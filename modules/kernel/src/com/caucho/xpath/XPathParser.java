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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xpath;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.xml.XmlChar;
import com.caucho.xpath.expr.*;
import com.caucho.xpath.functions.BaseURI;
import com.caucho.xpath.functions.ResolveURI;
import com.caucho.xpath.functions.Trace;
import com.caucho.xpath.pattern.*;

import org.w3c.dom.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * Parses an XPath expression.
 */
class XPathParser {
  private static final Logger log
    = Logger.getLogger(XPathParser.class.getName());
  private static final L10N L = new L10N(XPathParser.class);

  private final static int ANCESTOR_AXIS = 0;
  private final static int ANCESTOR_OR_SELF_AXIS = ANCESTOR_AXIS + 1;
  private final static int ATTRIBUTE_AXIS = ANCESTOR_OR_SELF_AXIS + 1;
  private final static int CHILD_AXIS = ATTRIBUTE_AXIS + 1;
  private final static int DESCENDANT_AXIS = CHILD_AXIS + 1;
  private final static int DESCENDANT_OR_SELF_AXIS = DESCENDANT_AXIS + 1;
  private final static int FOLLOWING_AXIS = DESCENDANT_OR_SELF_AXIS + 1;
  private final static int FOLLOWING_SIBLING_AXIS = FOLLOWING_AXIS + 1;
  private final static int NAMESPACE_AXIS = FOLLOWING_SIBLING_AXIS + 1;
  private final static int PARENT_AXIS = NAMESPACE_AXIS + 1;
  private final static int PRECEDING_AXIS = PARENT_AXIS + 1;
  private final static int PRECEDING_SIBLING_AXIS = PRECEDING_AXIS + 1;
  private final static int SELF_AXIS = PRECEDING_SIBLING_AXIS + 1;

  private final static int TEXT = Expr.LAST_FUN + 1;
  private final static int COMMENT = TEXT + 1;
  private final static int ER = COMMENT + 1;
  private final static int PI = ER + 1;
  private final static int NODE = PI + 1;
  private final static int CURRENT = NODE + 1;
  private final static int NODE_TEXT = CURRENT + 1;
  private final static int CONTEXT = NODE_TEXT + 1;

  private static IntMap exprFunctions;
  private static IntMap axisMap;

  private static HashMap<String,Constructor> _exprFunctions;
  
  private CharBuffer tag = new CharBuffer();

  private String _string;
  private int index;
  private int peek;

  private NamespaceContext _namespace;

  XPathParser(String string, NamespaceContext namespace)
  {
    _string = string;
    _namespace = namespace;
  }

  /**
   * Parse a select pattern, i.e. a path rooted in a context.
   */
  AbstractPattern parseSelect()
    throws XPathParseException
  {
    AbstractPattern top = new FromContext();

    AbstractPattern pattern = parseUnion(parseTop(top), top);

    if (index < _string.length())
      throw error(L.l("unexpected character at `{0}'", badChar(read())));

    return pattern;
  }

  /**
   * Parse a match pattern, i.e. a path with no root.
   */
  AbstractPattern parseMatch()
    throws XPathParseException
  {
    AbstractPattern root = new FromAny();
    AbstractPattern pattern = parseUnion(parseTop(root), root);

    if (index < _string.length())
      throw error(L.l("unexpected character at `{0}'", badChar(read())));

    return pattern;
  }


  /**
   * Parse an expression.
   */
  Expr parseExpr()
    throws XPathParseException
  {
    Expr expr = parseExpr(null, null);

    if (index < _string.length())
      throw error(L.l("unexpected character at `{0}'", badChar(read())));

    return expr;
  }

  private AbstractPattern parseStep(AbstractPattern root)
    throws XPathParseException
  {
    return parseUnion(parseTop(root), root);
  }

  private AbstractPattern parseUnion(AbstractPattern left, AbstractPattern root)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());

    while (ch >= 0) {
      if (ch == '|') {
        AbstractPattern tail = parseUnion(parseTop(root), root);
        left = new UnionPattern(left, tail);
      } else
        break;

      for (ch = read(); XmlChar.isWhitespace(ch); ch = read()) {
      }
    }

    unread();

    return left;
  }

  /**
   * Parses the top expression.
   *
   * <pre>
   *  top ::= (expr)
   *      ::= term
   * </pre>
   */
  private AbstractPattern parseTop(AbstractPattern pattern)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());
    unread();

    if (ch == '(') {
      Expr expr = parseTerm(pattern, pattern);

      // If the expression is really a pattern and the iterator is
      // ascending, then just unwrap it.
      if (expr instanceof NodeSetExpr) {
        AbstractPattern nodeSet = ((NodeSetExpr) expr).getPattern();

        if (nodeSet.isAscending())
          return nodeSet;
      }

      return new FromExpr(null, expr);
    }
    else
      return parseTerm(pattern, pattern).toNodeList();
  }

  private AbstractPattern parseBasisTop(AbstractPattern pattern)
    throws XPathParseException
  {
    int ch;

    ch = skipWhitespace(read());
    if (ch == '/') {
      ch = read();
      if (ch == '/') {
        pattern = new FromRoot();
        pattern = new FromDescendants(pattern, false);
        pattern = parseBasis(pattern);
        pattern = parseFilter(pattern);
        pattern = parsePath(pattern);

        return pattern;
      }
      pattern = new FromRoot();
      ch = skipWhitespace(ch);

      if (ch == -1)
        return pattern;
    }

    unread();

    if (pattern == null)
      pattern = new FromContext();

    pattern = parseBasis(pattern);
    pattern = parseFilter(pattern);

    return parsePath(pattern);
  }

  /**
   * path ::= top ('/' filter)*
   */
  private AbstractPattern parsePath(AbstractPattern pattern)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());

    while (ch == '/') {
      ch = read();

      if (ch == '/') {
        pattern = new FromDescendants(pattern, false);
        pattern = parseBasis(pattern);
        pattern = parseFilter(pattern);
      }
      else {
        unread();
        pattern = parseBasis(pattern);
        pattern = parseFilter(pattern);
      }

      ch = skipWhitespace(read());
    }

    unread();
    
    return pattern;
  }
  
  /**
   * filter ::= atom('[' expr ']')*
   */
  private AbstractPattern parseFilter(AbstractPattern pattern)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());
    while (ch == '[') {
      AbstractPattern context = new FromContext();
      Expr expr = parseExpr(context, pattern);
      pattern = new FilterPattern(pattern, expr);

      ch = skipWhitespace(read());
      if (ch !=  ']')
        throw error(L.l("expected `{0}' at {1}", "]", badChar(ch)));

      ch = skipWhitespace(read());
    }
    unread();

    return pattern;
  }

  /**
   * basis ::= name::node-test
   *         | node-test
   *         | @node-test
   *         | .
   *         | ..
   *         ;
   */
  private AbstractPattern parseBasis(AbstractPattern pattern)
    throws XPathParseException
  {
    boolean fromChildren = true;
    int ch = skipWhitespace(read());

    int nodeType = Node.ELEMENT_NODE;
    String namespace = null;
    tag.clear();

    if (ch == '@') {
      if (pattern instanceof FromDescendants)
        pattern = NodeTypePattern.create(pattern, NodeTypePattern.NODE);
      
      pattern = new FromAttributes(pattern);
      nodeType = Node.ATTRIBUTE_NODE;
      fromChildren = false;
      ch = read();
    }
    else if (ch == '.') {
      ch = read();
      if (ch == '.')
        return NodeTypePattern.create(new FromParent(pattern),
                                      NodeTypePattern.NODE);
      else {
        unread();
        if (pattern != null)
          return pattern;
        else
          return NodeTypePattern.create(new FromSelf(pattern), NodeTypePattern.ANY);
      }
    }
    else if (ch == '(') {
      // XXX: not strictly correct for counting
      Expr expr = parseExpr(null, null);

      if ((ch = read()) != ')')
        throw error(L.l("expected `{0}' at {1}", ")", badChar(ch)));

      return new FromExpr(pattern, expr);
    }

    if (ch == '*') {
      tag.append('*');
    }

    else if (XmlChar.isNameStart(ch)) {
      for (; XmlChar.isNameChar(ch); ch = read())
        tag.append((char) ch);

      if (ch == '*' && tag.endsWith(":"))
        tag.append('*');
      else
        unread();
    }
    else
      unread();

    String name = tag.toString();

    if (name.equals(""))
      throw error(L.l("expected name at {0}", badChar(ch)));

    return parseAxis(pattern, name, fromChildren, nodeType);
  }

  private AbstractPattern parseAxis(AbstractPattern pattern, String name,
                                    boolean fromChildren, int nodeType)
    throws XPathParseException
  {
    String axis = "";
    int axisIndex = name.indexOf("::");

    if (axisIndex >= 0 && nodeType != Node.ATTRIBUTE_NODE) {
      axis = name.substring(0, axisIndex);
      name = name.substring(axisIndex + 2);
    }

    if (pattern instanceof FromDescendants)
      return parseNodeTest(pattern, name, false, Node.ELEMENT_NODE);

    switch (axisMap.get(axis)) {
    case ANCESTOR_AXIS:
      return parseNodeTest(new FromAncestors(pattern, false),
                           name, false, Node.ELEMENT_NODE);

    case ANCESTOR_OR_SELF_AXIS:
      return parseNodeTest(new FromAncestors(pattern, true),
                           name, false, Node.ELEMENT_NODE);

    case ATTRIBUTE_AXIS:
      return parseNodeTest(new FromAttributes(pattern),
                           name, false, Node.ATTRIBUTE_NODE);

    case CHILD_AXIS:
      return parseNodeTest(new FromChildren(pattern),
                           name, false, Node.ELEMENT_NODE);

    case DESCENDANT_AXIS:
      return parseNodeTest(new FromDescendants(pattern, false),
                           name, false, Node.ELEMENT_NODE);

    case DESCENDANT_OR_SELF_AXIS:
      return parseNodeTest(new FromDescendants(pattern, true),
                           name, false, Node.ELEMENT_NODE);

    case FOLLOWING_AXIS:
      return parseNodeTest(new FromNext(pattern),
                           name, false, Node.ELEMENT_NODE);

    case FOLLOWING_SIBLING_AXIS:
      return parseNodeTest(new FromNextSibling(pattern),
                           name, false, Node.ELEMENT_NODE);

    case NAMESPACE_AXIS:
      return parseNodeTest(new FromNamespace(pattern),
                           name, false, Node.ATTRIBUTE_NODE);

    case PARENT_AXIS:
      return parseNodeTest(new FromParent(pattern),
                           name, false, Node.ELEMENT_NODE);

    case PRECEDING_AXIS:
      return parseNodeTest(new FromPrevious(pattern),
                           name, false, Node.ELEMENT_NODE);

    case PRECEDING_SIBLING_AXIS:
      return parseNodeTest(new FromPreviousSibling(pattern),
                           name, false, Node.ELEMENT_NODE);

    case SELF_AXIS:
      return parseNodeTest(new FromSelf(pattern),
                           name, false, Node.ELEMENT_NODE);

    default:
      return parseNodeTest(pattern, name, fromChildren, nodeType);
    }
  }

  /**
   * node-test ::= qname
   *             | *:name
   *             | *
   *             | name '(' args ')'
   *             ;
   */
  private AbstractPattern parseNodeTest(AbstractPattern pattern, String name,
                                        boolean fromChildren, int nodeType)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());

    AbstractPattern tagPattern;

    if (ch == '(') {
      Expr expr = parseFunction(pattern, pattern, name, fromChildren);
      return expr.toNodeList();
    }
    else if (ch == '{') {
      tag.clear();
      while ((ch = read()) >= 0 && ch != '}')
        tag.append((char) ch);
      String url = tag.toString();
      tag.clear();
      for (ch = read(); XmlChar.isNameChar(ch); ch = read())
        tag.append((char) ch);

      pattern = new NSNamePattern(pattern, url, tag.toString(), nodeType);
    }
    else {
      if (fromChildren)
        pattern = new FromChildren(pattern);

      if (name.equals("*"))
        pattern = NodeTypePattern.create(pattern, nodeType);
      else if (name.endsWith(":*")) {
        pattern = new NamespacePattern(pattern,
                                       name.substring(0, name.length() - 2),
                                       nodeType);
      }
      else {
        int p = name.indexOf(':');
        String ns = null;
        String local = name;

        if (p > 0) {
          String prefix = name.substring(0, p);
          ns = NamespaceContext.find(_namespace, prefix);
          local = name.substring(p + 1);
        }
        else if (nodeType != Node.ATTRIBUTE_NODE)
          ns = _namespace.find(_namespace, "");
        else
          ns = null; // _namespace.find(_namespace, "");

        if (ns == null)
          pattern = new NodePattern(pattern, name, nodeType);
        else
          pattern = new NSNamePattern(pattern, ns, local, nodeType);
      }
    }
    unread();

    return pattern;
  }

  /**
   * expr ::= or-expr
   */
  Expr parseExpr(AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    peek = -1;
    Expr left = parseTerm(parent, listParent);

    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.OR:
        left = parseOrExpr(token, left, parseTerm(parent, listParent),
                           parent, listParent);
        break;

      case Expr.AND:
        left = parseAndExpr(token, left, parseTerm(parent, listParent),
                            parent, listParent);
        break;

      case Expr.EQ: case Expr.NEQ: case Expr.LT:
      case Expr.LE: case Expr.GT: case Expr.GE:
        left = parseCmpExpr(token, left, parseTerm(parent, listParent),
                            parent, listParent);
        break;

      case Expr.ADD: case Expr.SUB:
        left = parseAddExpr(token, left, parseTerm(parent, listParent),
                            parent, listParent);
        break;

      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        left = parseMulExpr(token, left, parseTerm(parent, listParent),
                            parent, listParent);
        break;

      default:
        return left;
      }
    }
  }

  /**
   * or-expr ::= or-expr 'OR' expr
   *           | and-expr
   */
  private Expr parseOrExpr(int code, Expr left, Expr right,
                           AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.OR:
        left = new BooleanExpr(code, left, right);
        code = token;
        right = parseTerm(parent, listParent);
        break;

      case Expr.AND:
        right = parseAndExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.EQ: case Expr.NEQ: case Expr.LT:
      case Expr.LE: case Expr.GT: case Expr.GE:
        right = parseCmpExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      default:
        undoToken(token);
        return new BooleanExpr(code, left, right);
      }
    }
  }

  /**
   * and-expr ::= and-expr 'AND' expr
   *            | cmp-expr
   */
  private Expr parseAndExpr(int code, Expr left, Expr right,
                            AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.AND:
        left = new BooleanExpr(code, left, right);
        code = token;
        right = parseTerm(parent, listParent);
        break;

      case Expr.EQ: case Expr.NEQ: case Expr.LT:
      case Expr.LE: case Expr.GT: case Expr.GE:
        right = parseCmpExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      default:
        undoToken(token);
        return new BooleanExpr(code, left, right);
      }
    }
  }

  /**
   * cmp-expr ::= cmp-expr '<' expr
   *            | add-expr
   */
  private Expr parseCmpExpr(int code, Expr left, Expr right,
                            AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.EQ: case Expr.NEQ: case Expr.LT:
      case Expr.LE: case Expr.GT: case Expr.GE:
        left = new BooleanExpr(code, left, right);
        code = token;
        right = parseTerm(parent, listParent);
        break;

      case Expr.ADD: case Expr.SUB:
        right = parseAddExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      default:
        undoToken(token);
        return new BooleanExpr(code, left, right);
      }
    }
  }

  /**
   * add-expr ::= add-expr '+' expr
   *            | mul-expr
   */
  private Expr parseAddExpr(int code, Expr left, Expr right,
                            AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.ADD: case Expr.SUB:
        left = new NumericExpr(code, left, right);
        code = token;
        right = parseTerm(parent, listParent);
        break;

      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        right = parseMulExpr(token, right, parseTerm(parent, listParent),
                             parent, listParent);
        break;

      default:
        undoToken(token);
        return new NumericExpr(code, left, right);
      }
    }
  }

  /**
   * mul-expr ::= mul-expr '*' expr
   *            | term
   */
  private Expr parseMulExpr(int code, Expr left, Expr right,
                            AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    while (true) {
      int token = scanToken();
      switch (token) {
      case Expr.MUL: case Expr.DIV: case Expr.QUO: case Expr.MOD:
        left = new NumericExpr(code, left, right);
        code = token;
        right = parseTerm(parent, listParent);
        break;

      default:
        undoToken(token);
        return new NumericExpr(code, left, right);
      }
    }
  }

  /**
   * term ::= simple-term path?
   */
  private Expr parseTerm(AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());
    unread();
    Expr expr = parseSimpleTerm(parent, listParent);

    int nextCh = skipWhitespace(read());
    unread();
    if (nextCh == '/' || nextCh == '[') {
      AbstractPattern pattern = expr.toNodeList();

      if (ch == '(' && ! pattern.isStrictlyAscending())
        pattern = new FromExpr(null, expr);

      return NodeSetExpr.create(parseUnion(parsePath(parseFilter(pattern)),
                                           pattern));
    }
    else if (nextCh == '|') {
      AbstractPattern pattern = expr.toNodeList();

      return NodeSetExpr.create(parseUnion(parsePath(parseFilter(pattern)),
                                           listParent));
    }
    else
      return expr;
  }

  /**
   * simple-term ::= number
   *             ::= '(' expr ')'
   *             ::= '$' variable
   *             ::= '"' string '"'
   *             ::= node-set
   */
  private Expr parseSimpleTerm(AbstractPattern parent, AbstractPattern listParent)
    throws XPathParseException
  {
    int ch = read();
    
    ch = skipWhitespace(ch);

    switch (ch) {
    case '.':
      ch = read();
      unread();
      unread();
      if (! ('0' <= ch && ch <= '9')) {
        return NodeSetExpr.create(parseUnion(parseBasisTop(parent), parent));
      }
      ch = read();
      // fall through to parse the number

    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
      {
        long value = 0;
        double exp = 1;
        int digits = 0;
        for (; ch >= '0' && ch <= '9'; ch = read())
          value = 10 * value + ch - '0';
        if (ch == '.') {
          for (ch = read(); ch >= '0' && ch <= '9'; ch = read()) {
            value = 10 * value + ch - '0';
            exp *= 10;
            digits--;
          }
        }

        if (ch == 'e' || ch == 'E') {
          int sign = 1;
          int expValue = 0;
          
          ch = read();
          if (ch == '-') {
            sign = -1;
            ch = read();
          }
          else if (ch == '+')
            ch = read();
          
          for (; ch >= '0' && ch <= '9'; ch = read())
            expValue = 10 * expValue + ch - '0';

          exp = Math.pow(10, digits + sign * expValue);

          unread();
          
          return new NumericExpr((double) value * (double) exp);
        }
        
        unread();
        return new NumericExpr((double) value / (double) exp);
      }

    case '-':
      return new NumericExpr(Expr.NEG, parseTerm(parent, listParent));

    case '+':
      return parseTerm(parent, listParent);

    case '(':
      {
        Expr expr = parseExpr(parent, listParent);
        if ((ch = skipWhitespace(read())) != ')')
          throw error(L.l("expected `{0}' at {1}", ")", badChar(ch)));

        return expr;
      }

    case '/': case '@': case '*':
      unread();
      return NodeSetExpr.create(parseUnion(parseBasisTop(parent), parent));

    case '\'': case '"':
      {
        int end = ch;
        CharBuffer cb = new CharBuffer();
        for (ch = read(); ch >= 0; ch = read()) {
          if (ch != end)
            cb.append((char) ch);
          else if ((ch = read()) == end)
            cb.append((char) ch);
          else {
            unread();
            break;
          }
        }

        return new StringExpr(cb.toString());
      }

    case '$':
      {
        String name = readName(read());
        return new VarExpr(name);
      }

    default:
      if (! XmlChar.isNameStart(ch))
        throw error(L.l("unknown character at {0}", badChar(ch)));

      String name = readName(ch);

      ch = skipWhitespace(read());
      int axisIndex = name.indexOf("::");

      // make sure axis are treated as node sets
      if (ch == '(' && axisIndex < 0) {
        return parseFunction(parent, listParent, name, true);
      }
      else if (ch == '(') {
        String axis = name.substring(0, axisIndex);
        if (axisMap.get(axis) <= 0)
          return parseFunction(parent, listParent, name, true);
      }
      
      unread();

      if (parent == null)
        parent = new FromContext();
      return parseNodeSetExpr(parent, name, Node.ELEMENT_NODE);
    }
  }

  /**
   * function ::= name '(' args ')'
   *
   * The XPath library functions are hard-coded for a little extra
   * execution efficiency.
   */
  Expr parseFunction(AbstractPattern parent,
                     AbstractPattern listParent,
                     String name,
                     boolean fromChildren)
    throws XPathParseException
  {
    int ch = skipWhitespace(read());

    ArrayList<Expr> args = new ArrayList<Expr>();

    for (; ch >= 0 && ch != ')'; ch = skipWhitespace(read())) {
      if (ch != ',')
        unread();
      Expr expr = parseExpr(parent, listParent);

      if (expr == null)
        throw error(L.l("null expression"));
      
      args.add(expr);
    }

    int code = exprFunctions.get(name);
    switch (code) {
    case Expr.TRUE: case Expr.FALSE: case Expr.NOT: case Expr.BOOLEAN:
    case Expr.STARTS_WITH: case Expr.CONTAINS: case Expr.LANG:
    case Expr.FUNCTION_AVAILABLE:
      return new BooleanExpr(code, args);

    case Expr.NUMBER: case Expr.FLOOR: case Expr.CEILING: case Expr.ROUND:
    case Expr.STRING_LENGTH:
      return new NumericExpr(code, args);

    case Expr.POSITION: 
    case Expr.LAST:
      return new NumericExpr(code, listParent);

    case Expr.COUNT: case Expr.SUM:
      if (args.size() == 0)
        args.add(NodeSetExpr.create(new FromContext()));
      return new NumericExpr(code, ((Expr) args.get(0)).toNodeList());


    case Expr.CONCAT: 
    case Expr.SUBSTRING_BEFORE:
    case Expr.SUBSTRING: case Expr.SUBSTRING_AFTER:
    case Expr.TRANSLATE: 
    case Expr.SYSTEM_PROPERTY:
      return new StringExpr(code, args);

    case Expr.STRING: case Expr.NORMALIZE:
      return new StringExpr(code, args);

    case Expr.LOCAL_PART: case Expr.NAMESPACE:
    case Expr.QNAME: case Expr.GENERATE_ID:
      if (args.size() == 0)
        args.add(NodeSetExpr.create(new FromContext()));
      return new StringExpr(code, args);

    case Expr.ID:
      if (args.size() == 0) {
        args.add(NodeSetExpr.create(parent));
        return new IdExpr(args);
      }
      else
        return new IdExpr(args);

    case Expr.IF:
      if (args.size() != 3)
        throw error(L.l("`if' needs three args."));

      return new ObjectExpr(code, args);

    case Expr.BASE_URI:
      if (args.size() != 1)
        throw error(L.l("`base-uri' needs one args."));

      return new StringExpr(code, args.get(0));

    case TEXT:
      if (fromChildren)
        parent = new FromChildren(parent);
      AbstractPattern pattern = NodeTypePattern.create(parent, Node.TEXT_NODE);
      return NodeSetExpr.create(pattern);

    case COMMENT:
      if (fromChildren)
        parent = new FromChildren(parent);
      pattern = NodeTypePattern.create(parent, Node.COMMENT_NODE);
      return NodeSetExpr.create(pattern);

    case ER:
      if (fromChildren)
        parent = new FromChildren(parent);
      pattern = NodeTypePattern.create(parent, Node.ENTITY_REFERENCE_NODE);
      return NodeSetExpr.create(pattern);

    case PI:
      if (fromChildren)
        parent = new FromChildren(parent);
      if (args.size() == 1) {
        Expr expr = (Expr) args.get(0);
        String value = null;
        if (expr instanceof StringExpr)
          value = ((StringExpr) expr).getValue();
        if (value == null)
          throw error(L.l("processing-instruction expects string literal"));
        pattern = new NodePattern(parent, value,
                                  Node.PROCESSING_INSTRUCTION_NODE);
      }
      else
        pattern = NodeTypePattern.create(parent, Node.PROCESSING_INSTRUCTION_NODE);
      return NodeSetExpr.create(pattern);

    case NODE:
      if (fromChildren)
        parent = new FromChildren(parent);
      pattern = NodeTypePattern.create(parent, NodeTypePattern.NODE);
      return NodeSetExpr.create(pattern);

    case CURRENT:
      return NodeSetExpr.create(new CurrentPattern());

    case CONTEXT:
      return NodeSetExpr.create(new FromContext());

    default:
      Expr function = constructorFunction(name, args);

      if (function != null)
        return function;
      
      int p = name.lastIndexOf(':');
      String prefix;
      
      if (p > 0)
        prefix = name.substring(0, p);
      else
        prefix = "";
      
      String context = NamespaceContext.find(_namespace, prefix);

      if (context == null) {
      }
      else if (context.startsWith("java:"))
        name = context + "." + name.substring(p + 1);
      else if (context.indexOf(':') < 0)
        name = "java:" + context + "." + name.substring(p + 1);
      
      if (name.startsWith("java:")) {
        p = name.lastIndexOf('.');
        if (p < 0)
          throw error(L.l("`{0}' is an illegal extension function.  Java extension functions must look like java:mypkg.MyClass.mymethod.", name));

        String className = name.substring(5, p);
        String methodName = name.substring(p + 1);

        Class cl;
        try {
          cl = CauchoSystem.loadClass(className);
        } catch (ClassNotFoundException e) {
          throw error(L.l("`{0}' is an unknown Java class.  Java extension functions must use public classes.", className));
        }

        if (methodName.equals("new")) {
          Constructor []constructors = cl.getConstructors();
          
          for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].getParameterTypes().length == args.size())
              return new NewJavaExpr(constructors[i], args);
          }
            
          throw error(L.l("No matching public constructor in `{0}'",
                          className));
        }

        Method method = null;
        Method []methods = cl.getMethods();

        if (args.size() > 0) {
          for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(methodName) &&
                methods[i].getParameterTypes().length == args.size() - 1 &&
                ! Modifier.isStatic(methods[i].getModifiers())) {
              Expr objArg = (Expr) args.remove(0);

              return new ObjectJavaExpr(methods[i], objArg, args);
            }
          }
        }

        for (int i = 0; i < methods.length; i++) {
          if (methods[i].getName().equals(methodName) &&
              methods[i].getParameterTypes().length == args.size()) {
            method = methods[i];
            break;
          }
        }

        if (method == null)
          throw error(L.l("`{0}' does not match a public method in `{1}'",
                          methodName, className));

        if (! Modifier.isStatic(method.getModifiers()))
          throw error(L.l("`{0}' is not a static method in `{1}'",
                          methodName, className));

        return new StaticJavaExpr(method, args);
      }
      else if (name.equals(""))
        throw error(L.l("expected node-test at `{0}'", "("));

      return new FunExpr(name, parent, args);
    }
  }

  private Expr constructorFunction(String name, ArrayList<Expr> args)
    throws XPathParseException
  {
    Constructor constructor = _exprFunctions.get(name);

    if (constructor == null)
      return null;
    
    Class []params = constructor.getParameterTypes();

    if (params.length < args.size())
      throw error(L.l("`{0}' needs {1} arguments",
                      name, "" + params.length));

    Object []values = new Object[params.length];
    
    for (int i = 0; i < args.size(); i++)
      values[i] = args.get(i);

    try {
      return (Expr) constructor.newInstance(values);
    } catch (Throwable e) {
      throw new XPathParseException(e);
    }
  }

  private Expr parseNodeSetExpr(AbstractPattern parent, String name, int nodeType)
    throws XPathParseException
  {
    AbstractPattern top = parseAxis(parent, name, true, nodeType);
    top = parseFilter(top);

    return NodeSetExpr.create(parseUnion(parsePath(top), parent));
  }

  /**
   * Scans the next token.
   *
   * @return token code, expressed as an Expr enumeration.
   */
  private int scanToken()
    throws XPathParseException
  {
    if (peek >= 0) {
      int value = peek;
      peek = -1;
      return value;
    }

    int ch = skipWhitespace(read());

    switch (ch) {
    case '+': return Expr.ADD;
    case '-': return Expr.SUB;
    case '*': return Expr.MUL;
    case '=': return Expr.EQ;

    case '!':
      ch = read();
      if (ch == '=')
        return Expr.NEQ;
      else
        throw error(L.l("expected `{0}' at {1}", "=", badChar(ch)));

    case '<':
      ch = read();
      if (ch == '=')
        return Expr.LE;
      else {
        unread();
        return Expr.LT;
      }

    case '>':
      ch = read();
      if (ch == '=')
        return Expr.GE;
      else {
        unread();
        return Expr.GT;
      }

    default:
      if (XmlChar.isNameStart(ch)) {
        String name = readName(ch);

        if (name.equals("div"))
          return Expr.DIV;
        else if (name.equals("quo"))
          return Expr.QUO;
        else if (name.equals("mod"))
          return Expr.MOD;
        else if (name.equals("and"))
          return Expr.AND;
        else if (name.equals("or"))
          return Expr.OR;
        else
          throw error(L.l("expected binary operation at `{0}'", name));
      }

      unread();
      return -1;
    }
  }

  private String readName(int ch)
  {
    tag.clear();
    for (; XmlChar.isNameChar(ch); ch = read())
      tag.append((char) ch);
    
    if (ch == '*' && tag.endsWith(":"))
      tag.append((char) ch);
    else
      unread();

    return tag.toString();
  }

  private void undoToken(int token)
  {
    peek = token;
  }

  private int read()
  {
    if (index < _string.length()) {
      return _string.charAt(index++);
    }
    else {
      index++;
      return -1;
    }
  }

  private void unread()
  {
    index--;
  }
  
  private XPathParseException error(String message)
  {
    return new XPathParseException(message + " in " + _string);
  }
  
  private String badChar(int ch)
  {
    if (ch < 0)
      return L.l("end of file");
    else if (ch == '\n')
      return L.l("end of line");
    else
      return "`" + (char) ch + "'";
  }

  private int skipWhitespace(int ch)
    throws XPathParseException
  {
    for (; ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'; ch = read()) {
    }

    return ch;
  }

  private static void addFunction(String name, Class cl)
  {
    Constructor []constructors = cl.getConstructors();
    
    _exprFunctions.put(name, constructors[0]);
  }

  static {
    exprFunctions = new IntMap();
    exprFunctions.put("id", Expr.ID);

    exprFunctions.put("true", Expr.TRUE);
    exprFunctions.put("false", Expr.FALSE);
    exprFunctions.put("not", Expr.NOT);
    exprFunctions.put("boolean", Expr.BOOLEAN);
    exprFunctions.put("starts-with", Expr.STARTS_WITH);
    exprFunctions.put("contains", Expr.CONTAINS);
    exprFunctions.put("lang", Expr.LANG);

    exprFunctions.put("number", Expr.NUMBER);
    exprFunctions.put("sum", Expr.SUM);
    exprFunctions.put("floor", Expr.FLOOR);
    exprFunctions.put("ceiling", Expr.CEILING);
    exprFunctions.put("round", Expr.ROUND);
    exprFunctions.put("position", Expr.POSITION);
    exprFunctions.put("count", Expr.COUNT);
    exprFunctions.put("last", Expr.LAST);
    exprFunctions.put("string-length", Expr.STRING_LENGTH);

    exprFunctions.put("string", Expr.STRING);
    exprFunctions.put("concat", Expr.CONCAT);
    exprFunctions.put("substring", Expr.SUBSTRING);
    exprFunctions.put("substring-before", Expr.SUBSTRING_BEFORE);
    exprFunctions.put("substring-after", Expr.SUBSTRING_AFTER);
    exprFunctions.put("normalize-space", Expr.NORMALIZE);
    exprFunctions.put("translate", Expr.TRANSLATE);

    exprFunctions.put("local-name", Expr.LOCAL_PART);
    exprFunctions.put("local-part", Expr.LOCAL_PART);
    exprFunctions.put("namespace-uri", Expr.NAMESPACE);
    exprFunctions.put("name", Expr.QNAME);
    exprFunctions.put("generate-id", Expr.GENERATE_ID);

    exprFunctions.put("if", Expr.IF);
    
    exprFunctions.put("text", TEXT);
    exprFunctions.put("comment", COMMENT);
    exprFunctions.put("er", ER);
    exprFunctions.put("entity-reference", ER);
    exprFunctions.put("pi", PI);
    exprFunctions.put("processing-instruction", PI);
    exprFunctions.put("node", NODE);
    exprFunctions.put("current", CURRENT);
    exprFunctions.put("context", CONTEXT);

    axisMap = new IntMap();
    axisMap.put("ancestor", ANCESTOR_AXIS);
    axisMap.put("ancestor-or-self", ANCESTOR_OR_SELF_AXIS);
    axisMap.put("attribute", ATTRIBUTE_AXIS);
    axisMap.put("child", CHILD_AXIS);
    axisMap.put("descendant", DESCENDANT_AXIS);
    axisMap.put("descendant-or-self", DESCENDANT_OR_SELF_AXIS);
    axisMap.put("following", FOLLOWING_AXIS);
    axisMap.put("following-sibling", FOLLOWING_SIBLING_AXIS);
    axisMap.put("namespace", NAMESPACE_AXIS);
    axisMap.put("parent", PARENT_AXIS);
    axisMap.put("preceding", PRECEDING_AXIS);
    axisMap.put("preceding-sibling", PRECEDING_SIBLING_AXIS);
    axisMap.put("self", SELF_AXIS);

    _exprFunctions = new HashMap<String,Constructor>();
    addFunction("fn:base-uri", BaseURI.class);
    addFunction("fn:resolve-uri", ResolveURI.class);
    addFunction("fn:trace", Trace.class);
  }
}
