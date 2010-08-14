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

package com.caucho.jms.selector;

import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import java.util.logging.Logger;


/**
 * Parsing the selector.
 */
public class SelectorParser  {
  static final Logger log
    = Logger.getLogger(SelectorParser.class.getName());
  static final L10N L = new L10N(SelectorParser.class);
  
  static final int TRUE = 1;
  static final int FALSE = TRUE + 1;
  static final int NULL = FALSE + 1;
  
  static final int INTEGER = NULL + 1;
  static final int DOUBLE = INTEGER + 1;
  static final int LONG = DOUBLE + 1;
  static final int STRING = LONG + 1;
  static final int IDENTIFIER = STRING + 1;

  static final int EQ = IDENTIFIER + 1;
  static final int NE = EQ + 1;
  static final int LT = NE + 1;
  static final int LE = LT + 1;
  static final int GT = LE + 1;
  static final int GE = GT + 1;
  
  static final int NOT = GE + 1;
  static final int AND = NOT + 1;
  static final int OR = AND + 1;
  static final int BETWEEN = OR + 1;
  static final int LIKE = BETWEEN + 1;
  static final int ESCAPE = LIKE + 1;
  static final int IN = ESCAPE + 1;
  static final int IS = IN + 1;

  private static IntMap _reserved;

  private String _query;
  private int _parseIndex;
  private int _token = -1;
  private String _lexeme;
  private CharBuffer _cb = new CharBuffer();

  public Selector parse(String query)
    throws JMSException
  {
    _query = query;
    _parseIndex = 0;

    if (peekToken() == -1)
      return null;
    
    Selector selector = parseExpr();

    if (! selector.isUnknown() && ! selector.isBoolean())
      throw new InvalidSelectorException(L.l("selector '{0}' must be a boolean",
                                             selector));

    return selector;
  }

  private Selector parseExpr()
    throws JMSException
  {
    return parseOr();
  }
  
  private Selector parseOr()
    throws JMSException
  {
    Selector left = parseAnd();

    while (true) {
      int token = peekToken();

      switch (token) {
      case OR:
        scanToken();
        left = new OrSelector(left, parseAnd());
        break;
      
      default:
        return left;
      }
    }
  }
  
  private Selector parseAnd()
    throws JMSException
  {
    Selector left = parseCmp();

    while (true) {
      int token = peekToken();

      switch (token) {
      case AND:
        scanToken();
        left = new BooleanBinarySelector(token, left, parseCmp());
        break;
      
      default:
        return left;
      }
    }
  }
  
  /**
   * Parses a comparison expression.
   *
   * <pre>
   * cmp-expr ::= add-expr '=' add-expr
   *          ::= add-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Selector parseCmp()
    throws JMSException
  {
    int token = peekToken();
    boolean isNot = false;
    
    if (token == NOT) {
      scanToken();
      isNot = true;
      token = peekToken();
    }
    
    Selector left = parseAdd();

    token = peekToken();

    if (token == NOT) {
      isNot = ! isNot;
      scanToken();
      token = peekToken();
    }

    if (token >= EQ && token <= GE) {
      scanToken();
      
      left = new BooleanBinarySelector(token, left, parseAdd());
    }
    
    else if (token == BETWEEN) {
      scanToken();

      Selector low = parseAdd();
      token = scanToken();
      if (token != AND)
        throw error("BETWEEN needs AND");
      Selector high = parseAdd();

      left = new BetweenSelector(left, low, high);
    }

    else if (token == LIKE) {
      scanToken();

      token = scanToken();
      if (token != STRING)
        throw error("LIKE needs string pattern");

      String pattern = _lexeme;
      char escape = '\\';
      
      if (peekToken() == ESCAPE) {
        scanToken();

        token = scanToken();
        if (token != STRING)
          throw error("ESCAPE needs string pattern");

        if (_lexeme.length() > 0)
          escape = _lexeme.charAt(0);
      }

      left = new LikeSelector(left, pattern, escape);
    }

    else if (token == IN) {
      scanToken();

      InSelector inSelector = new InSelector(left);

      if (scanToken() != '(')
        throw error("IN needs `('");

      while ((token = scanToken()) == STRING) {
        inSelector.addValue(_lexeme);

        if (peekToken() == ',')
          scanToken();
      }

      if (token != ')')
        throw error("IN needs `)'");
      scanToken();

      left = inSelector;
    }

    else if (token == IS) {
      scanToken();

      if (peekToken() == NOT) {
        isNot = ! isNot;
        scanToken();
      }

      if ((token = scanToken()) != NULL)
        throw error("IS needs NULL");

      left = new UnarySelector(NULL, left);
    }
    
    if (isNot)
      return new UnarySelector(NOT, left);
    else
      return left;
  }
  
  private Selector parseAdd()
    throws JMSException
  {
    Selector left = parseMul();

    while (true) {
      int token = peekToken();

      switch (token) {
      case '+':
      case '-':
        scanToken();
        left = new NumericBinarySelector(token, left, parseMul());
        break;
      
      default:
        return left;
      }
    }
  }
  
  private Selector parseMul()
    throws JMSException
  {
    Selector left = parseUnary();

    while (true) {
      int token = peekToken();

      switch (token) {
      case '*':
      case '/':
        scanToken();
        left = new NumericBinarySelector(token, left, parseUnary());
        break;
      
      default:
        return left;
      }
    }
  }
  
  private Selector parseUnary()
    throws JMSException
  {
    int token = peekToken();

    switch (token) {
    case '+':
      scanToken();
      return new UnarySelector(token, parseUnary());
    case '-':
      scanToken();
      return new UnarySelector('-', parseUnary());
      
    default:
      return parseTerm(false);
    }
  }

  private Selector parseTerm(boolean hasSign)
    throws JMSException
  {
    Selector value = null;
    String prefix;
    int token = scanToken();

    switch (token) {
    case TRUE:
      value = new BooleanLiteralSelector(true);
      break;
    case FALSE:
      value = new BooleanLiteralSelector(false);
      break;
    case IDENTIFIER:
      value = IdentifierSelector.create(_lexeme);
      break;
    case STRING:
      value = new LiteralSelector(_lexeme);
      break;
    case INTEGER:
      if (hasSign)
        return new LiteralSelector(Long.decode("-" + _lexeme));
      else
        return new LiteralSelector(Long.decode(_lexeme));
    case LONG:
      if (hasSign)
        return new LiteralSelector(Long.decode("-" + _lexeme));
      else
        return new LiteralSelector(Long.decode(_lexeme));
    case DOUBLE:
      if (hasSign)
        return new LiteralSelector(new Double("-" + _lexeme));
      else
        return new LiteralSelector(new Double(_lexeme));

    case '(':
      value = parseExpr();
      if (scanToken() != ')')
        throw error("expected ')'");
      break;
      
    default:
      throw error("unknown token: " + token);
    }

    if (hasSign)
      return new UnarySelector('-', value);
    else
      return value;
  }

  /**
   * Peeks the next token
   *
   * @return integer code for the token
   */
  private int peekToken()
    throws JMSException
  {
    if (_token > 0)
      return _token;

    _token = scanToken();

    return _token;
  }
  
  /**
   * Scan the next token.  If the lexeme is a string, its string
   * representation is in "lexeme".
   *
   * @return integer code for the token
   */
  private int scanToken()
    throws JMSException
  {
    if (_token > 0) {
      int value = _token;
      _token = -1;
      return value;
    }

    int sign = 1;
    int ch;

    for (ch = read(); Character.isWhitespace((char) ch); ch = read()) {
    }

    switch (ch) {
    case -1:
    case '(':
    case ')':
    case '*':
    case '/':
    case '+':
    case '-':
    case ',':
      return ch;
      
    case '=':
      return EQ;

    case '<':
      if ((ch = read()) == '=')
        return LE;
      else if (ch == '>')
        return NE;
      else {
        unread(ch);
        return LT;
      }

    case '>':
      if ((ch = read()) == '=')
        return GE;
      else {
        unread(ch);
        return GT;
      }
    }

    if (Character.isJavaIdentifierStart((char) ch)) {
      _cb.clear();

      for (; ch > 0 && Character.isJavaIdentifierPart((char) ch); ch = read())
        _cb.append((char) ch);

      unread(ch);

      _lexeme = _cb.toString();
      String lower = _lexeme.toLowerCase();

      int token = _reserved.get(lower);

      if (token > 0)
        return token;
      else
        return IDENTIFIER; 
    }
    else if (ch >= '0' && ch <= '9' || ch == '.') {
      _cb.clear();

      int type = INTEGER;
      
      if (sign < 0)
        _cb.append('-');

      for (; '0' <= ch && ch <= '9'; ch = read())
        _cb.append((char) ch);

      if ((ch == 'x' || ch == 'X')
          && _cb.length() == 1 && _cb.charAt(0) == '0') {

        _cb.append('x');
        for (ch = read();
             '0' <= ch && ch <= '9'
               || 'a' <= ch && ch <= 'f'
               || 'A' <= ch && ch <= 'F';
             ch = read()) {
          _cb.append((char) ch);
        }

        _lexeme = _cb.toString();

        if (ch == 'l' || ch == 'L')
          return LONG;
        else {
          unread(ch);
          return INTEGER;
        }
      }

      if (ch == '.') {
        type = DOUBLE;
        
        _cb.append('.');
        for (ch = read(); ch >= '0' && ch <= '9'; ch = read())
          _cb.append((char) ch);
      }

      if (ch == 'e' || ch == 'E') {
        type = DOUBLE;

        _cb.append('e');
        if ((ch = read()) == '+' || ch == '-') {
          _cb.append((char) ch);
          ch = read();
        }
        
        if (! (ch >= '0' && ch <= '9'))
          throw error(L.l("exponent needs digits at {0}",
                          charName(ch)));
          
        for (; ch >= '0' && ch <= '9'; ch = read())
          _cb.append((char) ch);
      }

      if (ch == 'F' || ch == 'D' || ch == 'f' || ch == 'd')
        type = DOUBLE;
      else if (ch == 'L' || ch == 'l') {
        type = LONG;
      }
      else
        unread(ch);

      _lexeme = _cb.toString();

      return type;
    }
    // else if (ch == '\'' || ch == '\"') {
    else if (ch == '\'') {
      int end = ch;
      _cb.clear();

      for (ch = read(); ch >= 0; ch = read()) {
        if (ch == end) {
          int ch1;
          if ((ch1 = read()) == end)
            _cb.append((char) end);
          else {
            unread(ch1);
            break;
          }
        }
        else
          _cb.append((char) ch);
      }

      if (ch < 0)
        throw error(L.l("unexpected end of selector"));

      _lexeme = _cb.toString();

      return STRING;
    }

    throw error(L.l("unexpected char at {0}", "" + (char) ch));
  }

  /**
   * Returns the next character.
   */
  private int read()
  {
    if (_parseIndex < _query.length())
      return _query.charAt(_parseIndex++);
    else
      return -1;
  }

  /**
   * Unread the last character.
   */
  private void unread(int ch)
  {
    if (ch >= 0)
      _parseIndex--;
  }

  /**
   * Creates an error.
   */
  public JMSException error(String msg)
  {
    msg += "\nin \"" + _query + "\"";
    
    return new InvalidSelectorException(msg);
  }

  /**
   * Returns the name for a character
   */
  private String charName(int ch)
  {
    if (ch < 0)
      return L.l("end of query");
    else
      return String.valueOf((char) ch);
  }
  
  static {
    _reserved = new IntMap();
    _reserved.put("true", TRUE);
    _reserved.put("false", FALSE);
    _reserved.put("and", AND);
    _reserved.put("or", OR);
    _reserved.put("not", NOT);
    _reserved.put("null", NULL);
    _reserved.put("is", IS);
    _reserved.put("in", IN);
    _reserved.put("like", LIKE);
    _reserved.put("escape", ESCAPE);
    _reserved.put("between", BETWEEN);
  }
}
