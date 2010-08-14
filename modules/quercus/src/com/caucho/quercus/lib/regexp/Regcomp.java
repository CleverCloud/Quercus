/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

/*
 * XXX: anchored expressions should have flags for quick matching.
 */

package com.caucho.quercus.lib.regexp;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.caucho.quercus.env.ConstStringValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.util.*;

/**
 * Regular expression compilation.
 */
class Regcomp {
  private static final Logger log
    = Logger.getLogger(Regcomp.class.getName());
  private static final L10N L = new L10N(RegexpNode.class);

  // #2526, JIT issues with Integer.MAX_VALUE
  private static final int INTEGER_MAX = Integer.MAX_VALUE - 1;
  
  static final int MULTILINE = 0x1;
  static final int SINGLE_LINE = 0x2;
  static final int IGNORE_CASE = 0x4;
  static final int IGNORE_WS = 0x8;
  static final int GLOBAL = 0x10;

  static final int ANCHORED = 0x20;
  static final int END_ONLY = 0x40;
  static final int UNGREEDY = 0x80;
  static final int STRICT = 0x100;
  static final int UTF8 = 0x200;
  
  static final HashMap<String,Integer> _characterClassMap
    = new HashMap<String,Integer>();

  static final ConcurrentHashMap<String,RegexpSet> _unicodeBlockMap
    = new ConcurrentHashMap<String,RegexpSet>();

  private PeekStream _pattern;
  
  int _nGroup;
  int _nLoop;
  int _maxGroup;
  int _flags;

  HashMap<Integer,StringValue> _groupNameMap
    = new HashMap<Integer,StringValue>();

  HashMap<StringValue,Integer> _groupNameReverseMap
    = new HashMap<StringValue,Integer>();

  ArrayList<RegexpNode.Recursive> _recursiveList
    = new ArrayList<RegexpNode.Recursive>();

  RegexpNode _groupTail;
  
  boolean _isLookbehind;
  boolean _isOr;
  
  Regcomp(int flags)
  {
    _flags = flags;
  }

  boolean isGreedy()
  {
    return (_flags & UNGREEDY) != UNGREEDY;
  }

  boolean isIgnoreCase()
  {
    return (_flags & IGNORE_CASE) == IGNORE_CASE;
  }

  boolean isIgnoreWs()
  {
    return (_flags & IGNORE_WS) == IGNORE_WS;
  }

  boolean isMultiline()
  {
    return (_flags & MULTILINE) == MULTILINE;
  }

  boolean isDollarEndOnly()
  {
    return (_flags & END_ONLY) == END_ONLY;
  }

  int nextLoopIndex()
  {
    return _nLoop++;
  }

  RegexpNode parse(PeekStream pattern) throws IllegalRegexpException
  {
    _pattern = pattern;
    
    _nGroup = 1;

    RegexpNode begin = null;

    if ((_flags & ANCHORED) != 0)
      begin = RegexpNode.ANCHOR_BEGIN_RELATIVE;
    
    RegexpNode value = parseRec(pattern, begin);

    int ch;
    while ((ch = pattern.read()) == '|') {
      value = RegexpNode.Or.create(value, parseRec(pattern, begin));
    }
    
    value = value != null ? value.getHead() : RegexpNode.N_END;

    if (_maxGroup < _nGroup)
      _maxGroup = _nGroup;

    for (RegexpNode.Recursive rec : _recursiveList) {
      RegexpNode top = value;

      if (top instanceof RegexpNode.Concat) {
        RegexpNode.Concat topConcat = (RegexpNode.Concat) top;

        if (topConcat.getConcatHead() instanceof RegexpNode.AnchorBegin
            || topConcat.getConcatHead() instanceof RegexpNode
            .AnchorBeginRelative) {
          top = topConcat.getConcatNext();
        }
      }
      
      rec.setTop(top);
    }

    if (log.isLoggable(Level.FINEST))
      log.finest("regexp[] " + value);

    return value;
  }
  
  /**
   *   Recursively compile a RegexpNode.
   *
   * first      -- The first node of this sub-RegexpNode
   * prev       -- The previous node of this sub-RegexpNode
   * last_begin -- When the last grouping began
   * last_end   -- When the last grouping ended
   *
   * head       ->  node
   *                 v -- rest
   *                ...
   *                 v -- rest
   *                node
   *
   * last       ->  node
   *                 v -- rest
   *                ...
   *                 v -- rest
   *                node
   */
  private RegexpNode parseRec(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();
    RegexpNode next;
    RegexpNode groupTail;

    switch (ch) {
    case -1:
      return tail != null ? tail.getHead() : null;

    case '?':
      if (tail == null)
        throw error(L.l("'?' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 0, 1);
      
      return parseRec(pattern, tail.getTail());

    case '*':
      if (tail == null)
        throw error(L.l("'*' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 0, INTEGER_MAX);
      
      return parseRec(pattern, tail.getTail());

    case '+':
      if (tail == null)
        throw error(L.l("'+' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 1, INTEGER_MAX);
      
      return parseRec(pattern, tail.getTail());

    case '{':
      if (tail == null || ! ('0' <= pattern.peek() && pattern.peek() <= '9')) {
        next = parseString('{', pattern);
      
        return concat(tail, parseRec(pattern, next));
      }

      return parseRec(pattern, parseBrace(pattern, tail).getTail());

    case '.':
      if ((_flags & SINGLE_LINE) == 0)
        next = RegexpNode.DOT;
      else
        next = RegexpNode.ANY_CHAR;

      return concat(tail, parseRec(pattern, next));

    case '|':
      pattern.ungetc(ch);

      if (_groupTail != null)
        return concat(tail, _groupTail);
      else
        return tail.getHead();

    case '(':
      {
        switch (pattern.peek()) {
        case '?':
          pattern.read();

          switch (pattern.peek()) {
          case ':':
            pattern.read();
            return parseGroup(pattern, tail, 0, _flags);

          case '#':
            parseCommentGroup(pattern);

            return parseRec(pattern, tail);

          case '(':
            return parseConditional(pattern, tail);

          case '=':
          case '!':
            ch = pattern.read();

            boolean isPositive = (ch == '=');

            groupTail = _groupTail;
            _groupTail = null;

            next = parseRec(pattern, null);

            while ((ch = pattern.read()) == '|') {
              RegexpNode nextHead = parseRec(pattern, null);
              next = next.createOr(nextHead);
            }

            if (isPositive)
              next = new RegexpNode.Lookahead(next);
            else
              next = new RegexpNode.NotLookahead(next);

            if (ch != ')')
              throw error(L.l("expected ')' at '{0}'",
                              String.valueOf((char) ch)));

            _groupTail = groupTail;

            return concat(tail, parseRec(pattern, next));

          case '<':
            pattern.read();

            switch (pattern.read()) {
            case '=':
              isPositive = true;
              break;
            case '!':
              isPositive = false;
              break;
            default:
              throw error(L.l("expected '=' or '!'"));
            }

            groupTail = _groupTail;
            _groupTail = null;

            next = parseRec(pattern, null);

            if (next == null) {
            }
            else if (isPositive)
              next = new RegexpNode.Lookbehind(next);
            else
              next = new RegexpNode.NotLookbehind(next);

            while ((ch = pattern.read()) == '|') {
              RegexpNode second = parseRec(pattern, null);

              if (second == null) {
              }
              else if (isPositive)
                second = new RegexpNode.Lookbehind(second);
              else
                second = new RegexpNode.NotLookbehind(second);

              if (second != null)
                next = next.createOr(second);
            }

            if (ch != ')')
              throw error(L.l("expected ')' at '{0}'",
                              String.valueOf((char) ch)));

            _groupTail = groupTail;

            return concat(tail, parseRec(pattern, next));

          // XXX: once-only subpatterns (mostly an optimization feature)
          case '>':
            pattern.read();
            return parseGroup(pattern, tail, 0, _flags);

          case 'P':
            pattern.read();
            return parseNamedGroup(pattern, tail);

          case 'R':
            pattern.read();
            RegexpNode.Recursive rec = new RegexpNode.Recursive();
            _recursiveList.add(rec);
            ch = pattern.read();
            if (ch != ')')
              throw error(L.l("expected ')' at '{0}'",
                              String.valueOf((char) ch)));

            return concat(tail, parseRec(pattern, rec));

          case 'm': case 's': case 'i': case 'x': case 'g':
          case 'U': case 'X':
            {
              int flags = _flags;

              while ((ch = pattern.read()) > 0 && ch != ')') {
                switch (ch) {
                case 'm': _flags |= MULTILINE; break;
                case 's': _flags |= SINGLE_LINE; break;
                case 'i': _flags |= IGNORE_CASE; break;
                case 'x': _flags |= IGNORE_WS; break;
                case 'g': _flags |= GLOBAL; break;
                case 'U': _flags |= UNGREEDY; break;
                case 'X': _flags |= STRICT; break;
                case ':':
                  {
                    return parseGroup(pattern, tail, 0, flags);
                  }
                default:
                  throw error(
                      L.l("'{0}' is an unknown (? code",
                          String.valueOf((char) ch)));
                }
              }

              if (ch != ')')
                throw error(L.l("expected ')' at '{0}'",
                                String.valueOf((char) ch)));

              RegexpNode node = parseRec(pattern, tail);

              _flags = flags;

              return node;
            }

          default:
            throw error(L.l("'{0}' is an unknown (? code",
                String.valueOf((char) pattern.peek())));
          }

        default:
          return parseGroup(pattern, tail, _nGroup++, _flags);
        }
      }

    case ')':
      pattern.ungetc(ch);

      if (_groupTail != null)
        return concat(tail, _groupTail);
      else
        return tail;

    case '[':
      next = parseSet(pattern);

      return concat(tail, parseRec(pattern, next));
      
    case '\\':
      next = parseSlash(pattern);
      
      return concat(tail, parseRec(pattern, next));
      
    case '^':
      if (isMultiline())
        next = RegexpNode.ANCHOR_BEGIN_OR_NEWLINE;
      else
        next = RegexpNode.ANCHOR_BEGIN;
      
      return concat(tail, parseRec(pattern, next));
      
    case '$':
      if (isMultiline())
        next = RegexpNode.ANCHOR_END_OR_NEWLINE;
      else if (isDollarEndOnly())
        next = RegexpNode.ANCHOR_END_ONLY;
      else
        next = RegexpNode.ANCHOR_END;
      
      return concat(tail, parseRec(pattern, next));

    case ' ': case '\n': case '\t': case '\r':
      if (isIgnoreWs()) {
        while (Character.isSpace((char) pattern.peek()))
          pattern.read();

        return parseRec(pattern, tail);
      }
      else {
        next = parseString(ch, pattern);
        
        return concat(tail, parseRec(pattern, next));
      }

    case '#':
      if (isIgnoreWs()) {
        while ((ch = pattern.read()) > 0 && ch != '\n') {
        }

        return parseRec(pattern, tail);
      }
      else {
        next = parseString(ch, pattern);
        
        return concat(tail, parseRec(pattern, next));
      }
      
    default:
      next = parseString(ch, pattern);
      
      return concat(tail, parseRec(pattern, next));
    }
  }

  private void parseCommentGroup(PeekStream pattern)
  {
    int ch;
    
    // (?#...) Comment
    while ((ch = pattern.read()) >= 0 && ch != ')') {
    }
  }
  
  private RegexpNode parseNamedGroup(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    if (ch == '=') {
      StringBuilder sb = new StringBuilder();

      while ((ch = pattern.read()) != ')' && ch >= 0) {
        sb.append((char) ch);
      }

      if (ch != ')')
        throw error(L.l("expected ')'"));

      String name = sb.toString();
      
      Integer v = _groupNameReverseMap.get(new ConstStringValue(name));

      if (v != null) {
        RegexpNode next = new RegexpNode.GroupRef(v);
      
        return concat(tail, parseRec(pattern, next));
      }
      else
        throw error(L.l("'{0}' is an unknown regexp group", name));
    }
    else if (ch == '<') {
      StringBuilder sb = new StringBuilder();

      while ((ch = pattern.read()) != '>' && ch >= 0) {
        sb.append((char) ch);
      }

      if (ch != '>')
        throw error(L.l("expected '>'"));

      String name = sb.toString();

      int group = _nGroup++;

      _groupNameMap.put(group, new StringBuilderValue(name));
      _groupNameReverseMap.put(new StringBuilderValue(name), group);

      return parseGroup(pattern, tail, group, _flags);
    }
    else
      throw error(L.l("Expected '(?:P=name' or '(?:P<name' for named group"));
  }

  private RegexpNode parseConditional(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    if (ch != '(')
      throw error(L.l("expected '('"));
    
    RegexpNode.ConditionalHead groupHead = null;;
    RegexpNode groupTail = null;

    if ('1' <= (ch = pattern.peek()) && ch <= '9') {
      int value = 0;

      while ('0' <= (ch = pattern.read()) && ch <= '9') {
        value = 10 * value + ch - '0';
      }

      if (ch != ')')
        throw error(L.l("expected ')'"));

      if (_nGroup <= value)
        throw error(L.l("conditional value less than number of groups"));

      groupHead = new RegexpNode.ConditionalHead(value);
      groupTail = groupHead.getTail();
    }
    else
      throw error(L.l("conditional requires number"));

    RegexpNode oldTail = _groupTail;

    _groupTail = groupTail;

    RegexpNode first = parseRec(pattern, null);
    RegexpNode second = null;

    if ((ch = pattern.read()) == '|') {
      second = parseRec(pattern, null);

      ch = pattern.read();
    }

    if (ch != ')')
      throw error(L.l("expected ')' at '{0}'", String.valueOf((char) ch)));

    _groupTail = oldTail;

    groupHead.setFirst(first);
    groupHead.setSecond(second);

    return concat(tail, parseRec(pattern, groupHead));
  }

  private RegexpNode parseGroup(PeekStream pattern, RegexpNode tail,
                                int group, int oldFlags)
    throws IllegalRegexpException
  {
    RegexpNode.GroupHead groupHead = new RegexpNode.GroupHead(group);
    RegexpNode groupTail = groupHead.getTail();

    RegexpNode oldTail = _groupTail;

    _groupTail = groupTail;

    RegexpNode body = parseRec(pattern, null);

    int ch;
    while ((ch = pattern.read()) == '|') {
      RegexpNode nextBody = parseRec(pattern, null);
      body = body.createOr(nextBody);
    }

    if (ch != ')')
      throw error(L.l("expected ')'"));

    _flags = oldFlags;

    _groupTail = oldTail;

    groupHead.setNode(body.getHead());

    return concat(tail, parseRec(pattern, groupTail).getHead());
  }

  private void expect(char expected, int value)
    throws IllegalRegexpException
  {
    if (expected != value)
      throw error(L.l("expected '{0}'", String.valueOf(expected)));
  }

  private IllegalRegexpException error(String msg)
  {
    return new IllegalRegexpException(msg + " " + _pattern.getPattern());
  }

  /**
   *   Parse the repetition construct.
   *
   *   {n}    -- exactly n
   *   {n,}   -- at least n
   *   {n,m}  -- from n to m
   *   {,m}   -- at most m
   */
  private RegexpNode parseBrace(PeekStream pattern, RegexpNode node)
    throws IllegalRegexpException
  {
    int ch;
    int min = 0;
    int max = INTEGER_MAX;

    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      min = 10 * min + ch - '0';
    }

    if (ch == ',') {
      while ('0' <= (ch = pattern.read()) && ch <= '9') {
        if (max == INTEGER_MAX)
          max = 0;

        max = 10 * max + ch - '0';
      }
    }
    else
      max = min;

    if (ch != '}')
      throw error(L.l("Expected '}'"));

    return createLoop(pattern, node, min, max);
  }

  private RegexpNode createLoop(PeekStream pattern, RegexpNode node,
                                int min, int max)
  {
    if (pattern.peek() == '+') {
      pattern.read();
      
      return node.createPossessiveLoop(min, max);
    }
    else if (pattern.peek() == '?') {
      pattern.read();

      if (isGreedy())
        return node.createLoopUngreedy(this, min, max);
      else
        return node.createLoop(this, min, max);
    }
    else {
      if (isGreedy())
        return node.createLoop(this, min, max);
      else
        return node.createLoopUngreedy(this, min, max);
    }
  }

  static RegexpNode concat(RegexpNode prev, RegexpNode next)
  {
    if (prev != null) {
      return prev.concat(next).getHead();
    }
    else
      return next;
  }

  private String hex(int value)
  {
    CharBuffer cb = new CharBuffer();

    for (int b = 3; b >= 0; b--) {
      int v = (value >> (4 * b)) & 0xf;
      if (v < 10)
        cb.append((char) (v + '0'));
      else
        cb.append((char) (v - 10 + 'a'));
    }

    return cb.toString();
  }

  private String badChar(int ch)
  {
    if (0x20 <= ch && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else if ((ch & 0xffff) == 0xffff)
      return "end of expression";
    else
      return "'" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  /**
   *   Collect the characters in a set, e.g. [a-z@@^!"]
   *
   * Variables:
   *
   *   last     -- Contains last read character.
   *   lastdash -- Contains character before dash or -1 if not after dash.
   */
  private RegexpNode parseSet(PeekStream pattern) 
    throws IllegalRegexpException
  {
    int first = pattern.peek();
    boolean isNot = false;

    if (first == '^') {
      pattern.read();
      isNot = true;
    }
    
    RegexpSet set = new RegexpSet();

    int last = -1;
    int lastdash = -1;
    int ch;

    int charRead = 0;
    
    ArrayList<RegexpNode> nodeList = null;
    
    while ((ch = pattern.read()) >= 0) {
      charRead++;

      // php/4e3o
      // first literal closing bracket need not be escaped
      if (ch == ']') {
        if (charRead == 1) {
          pattern.ungetc(ch);
          ch = '\\';
        }
        else
          break;
      }
      
      boolean isChar = true;
      boolean isDash = ch == '-';

      if (ch == '\\') {
        isChar = false;

        switch ((ch = pattern.read())) {
        case 's':
          set.mergeOr(RegexpSet.SPACE);
          break;

        case 'S':
          set.mergeOrInv(RegexpSet.SPACE);
          break;

        case 'd':
          set.mergeOr(RegexpSet.DIGIT);
          break;

        case 'D':
          set.mergeOrInv(RegexpSet.DIGIT);
          break;

        case 'w':
          set.mergeOr(RegexpSet.WORD);
          break;

        case 'W':
          set.mergeOrInv(RegexpSet.WORD);
          break;

        case 'p':
          int ch2 = pattern.read();

          if (ch2 != '{') {
            if (nodeList == null)
              nodeList = new ArrayList<RegexpNode>();

            nodeList.add(parseUnicodeProperty(ch2, false));
          }
          else {
            StringBuilder sb = new StringBuilder();

            int ch3;

            while ((ch3 = pattern.read()) >= 0 && ch3 != '}') {
              sb.append((char) ch3);
            }

            String name = sb.toString();

            if (ch3 != '}')
              throw new IllegalRegexpException(L.l("expected '}' at "
                               + badChar(ch3)));

            int len = name.length();

            if (len == 1) {
              if (nodeList == null)
                  nodeList = new ArrayList<RegexpNode>();

              nodeList.add(parseUnicodeProperty(name.charAt(0), false));
            }
            else if (len == 2) {
              if (nodeList == null)
                  nodeList = new ArrayList<RegexpNode>();

              nodeList.add(parseUnicodeProperty(name.charAt(0),
                                                name.charAt(1),
                                                false));
            }
            else {
              set.mergeOr(getUnicodeSet(name));
            }
          }
          break;

        case 'b':
          ch = '\b';
          isChar = true;
          break;
        case 'n':
          ch = '\n';
          isChar = true;
          break;
        case 't':
          ch = '\t';
          isChar = true;
          break;
        case 'r':
          ch = '\r';
          isChar = true;
          break;
        case 'f':
          ch = '\f';
          isChar = true;
          break;

        case 'x':
          ch = parseHex(pattern);
          isChar = true;
          break;

        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
          ch = parseOctal(ch, pattern);
          isChar = true;
          break;

        default:
          isChar = true;
        }
      }
      else if (ch == '[') {
        if (pattern.peek() == ':') {
          isChar = false;
          pattern.read();
          
          set.mergeOr(parseCharacterClass(pattern));
        }
      }

      if (isDash && last != -1 && lastdash == -1) {
        lastdash = last;
      }
      // c1-c2
      else if (isChar && lastdash != -1) {
        if (lastdash > ch)
          throw new IllegalRegexpException("expected increasing range at "
              + badChar(ch));

        setRange(set, lastdash, ch);

        last = -1;
        lastdash = -1;
      }
      else if (lastdash != -1) {
        setRange(set, lastdash, lastdash);
        setRange(set, '-', '-');

        last = -1;
        lastdash = -1;
      }
      else if (last != -1) {
        
        setRange(set, last, last);

        if (isChar)
          last = ch;
      }
      else if (isChar)
        last = ch;
    }

    // Dash at end of set: [a-z1-]
    if (lastdash != -1) {
      setRange(set, lastdash, lastdash);
      setRange(set, '-', '-');
    }
    else if (last != -1) {
      setRange(set, last, last);
    }
    
    if (ch != ']')
      throw error(L.l("Expected ']'"));

    if (nodeList == null) {
      if (isNot)
        return set.createNotNode();
      else
        return set.createNode();
    }
    else {
      RegexpNode setNode = set.createNode();

      for (RegexpNode node : nodeList) {
        setNode = setNode.createOr(node);
      }
        
      if (isNot)
        return setNode.createNot();
      else
        return setNode;
    }
  }

  private void setRange(RegexpSet set, int a, int b)
  {
    set.setRange(a, b);
    
    if (isIgnoreCase()) {
      if (Character.isLowerCase(a) && Character.isLowerCase(b)) {
        set.setRange(Character.toUpperCase(a), Character.toUpperCase(b));
      }

      if (Character.isUpperCase(a) && Character.isUpperCase(b)) {
        set.setRange(Character.toLowerCase(a), Character.toLowerCase(b));
      }
    }
  }

  private RegexpSet getUnicodeSet(String name)
    throws IllegalRegexpException
  {
    _flags |= UTF8;

    RegexpSet set = _unicodeBlockMap.get(name);

    if (set == null) {
      Character.UnicodeBlock block = Character.UnicodeBlock.forName(name);

      if (block == null)
        throw new IllegalRegexpException(
            L.l("'{0}' is an unknown unicode block", name));

      set = new RegexpSet();

      for (int ch = 0; ch < 65536; ch++) {
        if (Character.UnicodeBlock.of(ch) == block) {
          set.setRange(ch, ch);
        }
      }

      _unicodeBlockMap.put(name, set);
    }

    return set;
  }

  /**
   * Returns a node for sequences starting with a backslash.
   */
  private RegexpNode parseSlash(PeekStream pattern)
    throws IllegalRegexpException
  {
    int ch;
    switch (ch = pattern.read()) {
    case 's':
      return RegexpNode.SPACE;

    case 'S':
      return RegexpNode.NOT_SPACE;

    case 'd':
      return RegexpNode.DIGIT;

    case 'D':
      return RegexpNode.NOT_DIGIT;

    case 'w':
      return RegexpNode.S_WORD;

    case 'W':
      return RegexpNode.NOT_S_WORD;

    case 'b':
      return RegexpNode.WORD;

    case 'B':
      return RegexpNode.NOT_WORD;

    case 'A':
      return RegexpNode.STRING_BEGIN;

    case 'z':
      return RegexpNode.STRING_END;
      
    case 'Z':
      return RegexpNode.STRING_NEWLINE;

    case 'G':
      return RegexpNode.STRING_FIRST;

    case 'a':
      return parseString('\u0007', pattern);
    
    case 'c':
      ch = pattern.read();
      
      ch = Character.toUpperCase(ch);
      ch ^= 0x40;

      return parseString(ch, pattern);

    case 'e':
      return parseString('\u001B', pattern, true);
    case 'n':
      return parseString('\n', pattern, true);
    case 'r':
      return parseString('\r', pattern, true);
    case 'f':
      return parseString('\f', pattern, true);
    case 't':
      return parseString('\t', pattern, true);

    case 'x':
      int hex = parseHex(pattern);
      return parseString(hex, pattern, true);
    
    case '0':
      int oct = parseOctal(ch, pattern);
      return parseString(oct, pattern, true);

    case '1': case '2': case '3': case '4': 
    case '5': case '6': case '7': case '8': case '9':
      return parseBackReference(ch, pattern);

    case 'p':
      return parseUnicodeProperty(pattern, false);
    case 'P':
      return parseUnicodeProperty(pattern, true);
      
    case 'Q':
      return parseQuotedString(pattern);
      
    case '#':
      return parseString('#', pattern, true);

    default:
      if ((_flags & STRICT) != 0)
        throw new IllegalRegexpException("unrecognized escape at "
            + badChar(ch));
      return parseString(ch, pattern);
    }
  }
  
  /**
   * Returns a node for sequences starting with a '[:'.
   */
  private RegexpSet parseCharacterClass(PeekStream pattern)
    throws IllegalRegexpException
  {
    StringBuilder sb = new StringBuilder();
    
    int ch;
    while ((ch = pattern.read()) != ':' && ch >= 0) {
      sb.append((char)ch);
    }
    
    if (ch != ':') {
      throw new IllegalRegexpException(
          "expected character class closing colon ':' at " + badChar(ch));
    }  
     
    if ((ch = pattern.read()) != ']') {
      throw new IllegalRegexpException(
          "expected character class closing bracket ']' at " + badChar(ch));
    }

    String name = sb.toString();
    
    RegexpSet set = RegexpSet.CLASS_MAP.get(name);
    
    if (set == null) {
      throw new IllegalRegexpException("unrecognized POSIX character class "
          + name);
    }
 
    return set;
  }

  private int parseHex(PeekStream pattern)
    throws IllegalRegexpException
  {
    int ch = pattern.read();
    
    int hex = 0;
    
    StringBuilder sb = new StringBuilder();
    
    if (ch == '{') {
      while ((ch = pattern.read()) != '}') {
        if (ch < 0)
          throw new IllegalRegexpException("no more input; expected '}'");
        
        sb.append((char)ch);
      }
    }
    else {
      if (ch < 0)
        throw new IllegalRegexpException("expected hex digit at "
            + badChar(ch));
      
      sb.append((char)ch);
      ch = pattern.read();
      
      if (ch < 0) {
        throw new IllegalRegexpException("expected hex digit at "
            + badChar(ch));
      }

      sb.append((char)ch);
    }
    
    int len = sb.length();
    
    for (int i = 0; i < len; i++) {
      ch = sb.charAt(i);

      if ('0' <= ch && ch <= '9')
        hex = hex * 16 + ch - '0';
      else if ('a' <= ch && ch <= 'f')
        hex = hex * 16 + ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'F')
        hex = hex * 16 + ch - 'A' + 10;
      else
        throw new IllegalRegexpException("expected hex digit at "
            + badChar(ch));
    }
    
    return hex;
  }
  
  private RegexpNode parseBackReference(int ch, PeekStream pattern)
    throws IllegalRegexpException
  {
    int value = ch - '0';
    int ch2 = pattern.peek();
    
    if ('0' <= ch2 && ch2 <= '9') {
      pattern.read();
      value = value * 10 + ch2 - '0';
    }

    int ch3 = pattern.peek();
    
    if (value < 10 || value <= _nGroup && ! ('0' <= ch3 && ch3 <= '7')) {
      return new RegexpNode.GroupRef(value);
    }
    else if (! ('0' <= ch2 && ch2 <= '7')
             && ! ('0' <= ch3 && ch3 <= '7'))
      throw new IllegalRegexpException(
          "back referencing to a non-existent group: " + value);
    
    if (value > 10)
      pattern.ungetc(ch2);
    
    if (ch == '8' || ch == '9'
        || '0' <= ch3 && ch3 <= '9' && value * 10 + ch3 - '0' > 0xFF) {
      //out of byte range or not an octal,
      //need to parse backslash as the NULL character
      
      pattern.ungetc(ch);
      return parseString('\u0000', pattern);
    }
    
    int oct = parseOctal(ch, pattern);
    
    return parseString(oct, pattern, true);
    
    //return createString((char) oct);
  }

  private RegexpNode parseString(int ch,
                                                 PeekStream pattern)
    throws IllegalRegexpException
  {
    return parseString(ch, pattern, false);
  }
  
  /**
   * parseString
   */
  private RegexpNode parseString(int ch,
                                 PeekStream pattern,
                                 boolean isEscaped)
    throws IllegalRegexpException
  {
    CharBuffer cb = new CharBuffer();
    cb.append((char) ch);

    for (ch = pattern.read(); ch >= 0; ch = pattern.read()) {
      switch (ch) {
      case ' ': case '\t': case '\n': case '\r':
        if (! isIgnoreWs() || isEscaped)
          cb.append((char) ch);
        break;

      case '#':
        if (! isIgnoreWs() || isEscaped)
          cb.append((char) ch);
        else {
          while ((ch = pattern.read()) != '\n' && ch >= 0) {
          }
        }
        break;

      case '(': case ')': case '[':
      case '+': case '?': case '*': case '.':
      case '$': case '^': case '|':
        pattern.ungetc(ch);
        return createString(cb);

      case '{':
        if ('0' <= pattern.peek() && pattern.peek() <= '9') {
          pattern.ungetc(ch);
          return createString(cb);
        }
        cb.append('{');
        break;

      case '\\':
        ch = pattern.read();

        switch (ch) {
        case -1:
          cb.append('\\');
          return createString(cb);

        case 's': case 'S': case 'd': case 'D':
        case 'w': case 'W': case 'b': case 'B':
        case 'A': case 'z': case 'Z': case 'G':
        case 'p': case 'P':
          pattern.ungetc(ch);
          pattern.ungetc('\\');
          return createString(cb);

        case 'a':
          cb.append('\u0007');
          break;

        case 'c':
          ch = pattern.read();
      
          ch = Character.toUpperCase(ch);
          ch ^= 0x40;

          cb.append((char) ch);
          break;
        case 'e':
          cb.append('\u001b');
          break;
        case 't':
          cb.append('\t');
          break;
        case 'f':
          cb.append('\f');
          break;
        case 'n':
          cb.append('\n');
          break;
        case 'r':
          cb.append('\r');
          break;

        case 'x':
          int hex = parseHex(pattern);
          cb.append((char) hex);
          break;
      
        case 'Q':
          while ((ch = pattern.read()) >= 0) {
            if (ch == '\\' && pattern.peek() == 'E') {
              pattern.read();
              break;
            }

            cb.append((char) ch);
          }
          break;
    
        case '0':
          int oct = parseOctal(ch, pattern);
          cb.append((char) oct);
          break;

        case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
          if (ch - '0' <= _nGroup) {
            pattern.ungetc(ch);
            pattern.ungetc('\\');
            return createString(cb);
          }
          else {
            oct = parseOctal(ch, pattern);
            cb.append((char) oct);
          }
          break;
        case '#':
          cb.append('#');
          break;

        default:
          if ((_flags & STRICT) != 0)
            throw error(L.l("unrecognized escape at " + badChar(ch)));

          cb.append((char) ch);
          break;
        }
        break;

      default:
        cb.append((char) ch);
      }
    }

    return createString(cb);
  }
  
  private RegexpNode parseQuotedString(PeekStream pattern)
  {
    CharBuffer cb = new CharBuffer();
    
    int ch;
    
    while ((ch = pattern.read()) >= 0) {
      if (ch == '\\' && pattern.peek() == 'E') {
        pattern.read();
        break;
      }
      
      cb.append((char) ch);
    }
    
    return createString(cb);
  }

  private RegexpNode createString(CharBuffer cb)
  {
    if (isIgnoreCase())
      return new RegexpNode.StringIgnoreCase(cb);
    else
      return new RegexpNode.StringNode(cb);
  }
  
  private RegexpNode createString(char ch)
  {
    if (isIgnoreCase())
      return new RegexpNode.StringIgnoreCase(ch);
    else
      return new RegexpNode.StringNode(ch);
  }
  
  private int parseOctal(int ch, PeekStream pattern)
    throws IllegalRegexpException
  {
    if ('0' > ch || ch > '7')
      throw new IllegalRegexpException("expected octal digit at "
          + badChar(ch));
    
    int oct = ch - '0';
    
    int ch2 = pattern.peek();
    
    if ('0' <= ch2 && ch2 <= '7') {
      pattern.read();
      
      oct = oct * 8 + ch2 - '0';
      
      ch = pattern.peek();
      
      if ('0' <= ch && ch <= '7') {
        pattern.read();
        
        oct = oct * 8 + ch - '0';
      }
    }
    
    return oct;
  }
  
  private RegexpNode parseUnicodeProperty(PeekStream pattern,
                                          boolean isNegated)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    boolean isBraced = false;

    if (ch == '{') {
      isBraced = true;
      ch = pattern.read();
      
      if (ch == '^') {
        isNegated = ! isNegated;
        ch = pattern.read();
      }
    }
    
    RegexpNode node;
    
    if (isBraced) {
      int ch2 = pattern.read();
      
      if (ch2 == '}')
        node = parseUnicodeProperty(ch, isNegated);
      else {
        node = parseUnicodeProperty(ch, ch2, isNegated);
        
        expect('}', pattern.read());
      }
    }
    else
      node = parseUnicodeProperty(ch, isNegated);
    
    return node;
  }
  
  private RegexpNode parseUnicodeProperty(int ch, int ch2,
                                          boolean isNegated)
    throws IllegalRegexpException
  {
    byte category = 0;
    
    switch (ch) {
    case 'C':
      switch (ch2) {
      case 'c':
        return isNegated ? RegexpNode.PROP_NOT_Cc : RegexpNode.PROP_Cc;
      case 'f':
        return isNegated ? RegexpNode.PROP_NOT_Cf : RegexpNode.PROP_Cf;
      case 'n':
        return isNegated ? RegexpNode.PROP_NOT_Cn : RegexpNode.PROP_Cn;
      case 'o':
        return isNegated ? RegexpNode.PROP_NOT_Co : RegexpNode.PROP_Co;
      case 's':
        return isNegated ? RegexpNode.PROP_NOT_Cs : RegexpNode.PROP_Cs;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }

    case 'L':
      switch (ch2) {
      case 'l':
        return isNegated ? RegexpNode.PROP_NOT_Ll : RegexpNode.PROP_Ll;
      case 'm':
        return isNegated ? RegexpNode.PROP_NOT_Lm : RegexpNode.PROP_Lm;
      case 'o':
        return isNegated ? RegexpNode.PROP_NOT_Lo : RegexpNode.PROP_Lo;
      case 't':  
        return isNegated ? RegexpNode.PROP_NOT_Lt : RegexpNode.PROP_Lt;
      case 'u': 
        return isNegated ? RegexpNode.PROP_NOT_Lu : RegexpNode.PROP_Lu;

      case '}':
        return isNegated ? RegexpNode.PROP_NOT_L : RegexpNode.PROP_L;

      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }
    case 'M':
      switch (ch2) {
      case 'c':
        return isNegated ? RegexpNode.PROP_NOT_Mc : RegexpNode.PROP_Mc;
      case 'e':
        return isNegated ? RegexpNode.PROP_NOT_Me : RegexpNode.PROP_Me;
      case 'n':
        return isNegated ? RegexpNode.PROP_NOT_Mn : RegexpNode.PROP_Mn;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }

    case 'N':
      switch (ch2) {
      case 'd':
        return isNegated ? RegexpNode.PROP_NOT_Nd : RegexpNode.PROP_Nd;
      case 'l':
        return isNegated ? RegexpNode.PROP_NOT_Nl : RegexpNode.PROP_Nl;
      case 'o':
        return isNegated ? RegexpNode.PROP_NOT_No : RegexpNode.PROP_No;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }

    case 'P':
      switch (ch2) {
      case 'c':
        return isNegated ? RegexpNode.PROP_NOT_Pc : RegexpNode.PROP_Pc;
      case 'd':  
        return isNegated ? RegexpNode.PROP_NOT_Pd : RegexpNode.PROP_Pd;
      case 'e':
        return isNegated ? RegexpNode.PROP_NOT_Pe : RegexpNode.PROP_Pe;
      case 'f':
        return isNegated ? RegexpNode.PROP_NOT_Pf : RegexpNode.PROP_Pf;
      case 'i':     
        return isNegated ? RegexpNode.PROP_NOT_Pi : RegexpNode.PROP_Pi;
      case 'o':    
        return isNegated ? RegexpNode.PROP_NOT_Po : RegexpNode.PROP_Po;
      case 's':   
        return isNegated ? RegexpNode.PROP_NOT_Ps : RegexpNode.PROP_Ps;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }

    case 'S':
      switch (ch2) {
      case 'c': 
        return isNegated ? RegexpNode.PROP_NOT_Sc : RegexpNode.PROP_Sc;
      case 'k':
        return isNegated ? RegexpNode.PROP_NOT_Sk : RegexpNode.PROP_Sk;
      case 'm':  
        return isNegated ? RegexpNode.PROP_NOT_Sm : RegexpNode.PROP_Sm;
      case 'o':
        return isNegated ? RegexpNode.PROP_NOT_So : RegexpNode.PROP_So;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }

    case 'Z':
      switch (ch2) {
      case 'l':
        return isNegated ? RegexpNode.PROP_NOT_Zl : RegexpNode.PROP_Zl;
      case 'p':   
        return isNegated ? RegexpNode.PROP_NOT_Zp : RegexpNode.PROP_Zp;
      case 's':   
        return isNegated ? RegexpNode.PROP_NOT_Zs : RegexpNode.PROP_Zs;
      default:
        throw error(L.l("invalid Unicode category {0}{1}",
                        badChar(ch), badChar(ch2)));
      }
    }

    throw new UnsupportedOperationException();
  }
  
  private RegexpNode parseUnicodeProperty(int ch,
                                          boolean isNegated)
    throws IllegalRegexpException
  {
    switch (ch) {
      case 'C':
        return isNegated ? RegexpNode.PROP_NOT_C : RegexpNode.PROP_C;

      case 'L':
        return isNegated ? RegexpNode.PROP_NOT_L : RegexpNode.PROP_L;

      case 'M':
        return isNegated ? RegexpNode.PROP_NOT_M : RegexpNode.PROP_M;

      case 'N':
        return isNegated ? RegexpNode.PROP_NOT_N : RegexpNode.PROP_N;

      case 'P':
        return isNegated ? RegexpNode.PROP_NOT_P : RegexpNode.PROP_P;

      case 'S':
        return isNegated ? RegexpNode.PROP_NOT_S : RegexpNode.PROP_S;

      case 'Z':
        return isNegated ? RegexpNode.PROP_NOT_Z : RegexpNode.PROP_Z;

      default:
        throw new IllegalRegexpException("invalid Unicode property "
            + badChar(ch));
    }
  }
  
  /*
  static {
    _characterClassMap.put("alnum", RegexpNode.RC_ALNUM);
    _characterClassMap.put("alpha", RegexpNode.RC_ALPHA);
    _characterClassMap.put("blank", RegexpNode.RC_BLANK);
    _characterClassMap.put("cntrl", RegexpNode.RC_CNTRL);
    _characterClassMap.put("digit", RegexpNode.RC_DIGIT);
    _characterClassMap.put("graph", RegexpNode.RC_GRAPH);
    _characterClassMap.put("lower", RegexpNode.RC_LOWER);
    _characterClassMap.put("print", RegexpNode.RC_PRINT);
    _characterClassMap.put("punct", RegexpNode.RC_PUNCT);
    _characterClassMap.put("space", RegexpNode.RC_SPACE);
    _characterClassMap.put("upper", RegexpNode.RC_UPPER);
    _characterClassMap.put("xdigit", RegexpNode.RC_XDIGIT);
  }
  */
}
