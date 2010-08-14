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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext.expr;

/**
 * Parses a gettext plural expression.
 */
class PluralExprParser
{
  final static int INTEGER = 256;

  final static int EQ = 270;
  final static int NEQ = 271;
  final static int LE = 272;
  final static int GE = 273;

  final static int AND = 280;
  final static int OR = 281;

  final static int VARIABLE_N = 290;
  final static int UNKNOWN = 291;
  final static int UNSET = 292;

  private CharSequence _expr;
  private int _exprLength;
  private int _parseIndex;

  private Expr _npluralsExpr;
  private Expr _pluralExpr;

  private int _peekToken;
  private int _integer;

  private boolean _isError;
  private boolean _isInitialized;

  protected PluralExprParser(CharSequence expr)
  {
    _expr = expr;
    _exprLength = expr.length();

    _isInitialized = false;
  }

  public Expr getNpluralsExpr()
  {
    if (! _isInitialized)
      init();

    if (_isError)
      return null;

    return _npluralsExpr;
  }

  public Expr getPluralExpr()
  {
    if (! _isInitialized)
      init();

    if (_isError)
      return null;

    return _pluralExpr;
  }

  private void init()
  {
    _parseIndex = 0;
    _peekToken = UNSET;
    _isError = false;

    parseAssignExpr();
    parseAssignExpr();

    _isInitialized = true;
  }

  private void parseAssignExpr()
  {
    int ch = consumeWhiteSpace();

    boolean isNplurals;

    if (ch == 'n'
        && read() == 'p'
        && read() == 'l'
        && read() == 'u'
        && read() == 'r'
        && read() == 'a'
        && read() == 'l'
        && read() == 's') {
      isNplurals = true;
    }
    else if (ch == 'p'
            && read() == 'l'
            && read() == 'u'
            && read() == 'r'
            && read() == 'a'
            && read() == 'l') {
      isNplurals = false;
    }
    else
      return;

    ch = consumeWhiteSpace();

    if (ch != '=')
      return;

    if (isNplurals)
      _npluralsExpr = parseIfExpr();
    else
      _pluralExpr = parseIfExpr();

    // Read semicolon
    parseToken();
  }

  private Expr parseLiteralExpr()
  {
    int token = parseToken();

    if (token == INTEGER)
      return new LiteralExpr(_integer);
    else if (token == VARIABLE_N)
      return NExpr.N_EXPR;
    else
      return error("Expected INTEGER");
  }

  private Expr parseParenExpr()
  {
    int token = parseToken();

    if (token != '(') {
      _peekToken = token;
      return parseLiteralExpr();
    }

    Expr expr = parseIfExpr();
    if (parseToken() != ')')
      return error("Expected ')'");

    return expr;
  }

  private Expr parseMulExpr()
  {
    Expr expr = parseParenExpr();

    while (true) {
      int token = parseToken();
      switch (token) {
        case '%':
          expr = new ModExpr(expr, parseParenExpr());
          break;
        case '*':
          expr = new MulExpr(expr, parseParenExpr());
          break;
        case '/':
          expr = new DivExpr(expr, parseParenExpr());
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private Expr parseAddExpr()
  {
    Expr expr = parseMulExpr();

    while (true) {
      int token = parseToken();

      switch (token) {
        case '+':
          expr = new AddExpr(expr, parseMulExpr());
          break;
        case '-':
          expr = new SubExpr(expr, parseMulExpr());
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private Expr parseCmpExpr()
  {
    Expr expr = parseAddExpr();

    while (true) {
      int token = parseToken();
      switch (token) {
        case '>':
          expr = new GTExpr(expr, parseAddExpr());
          break;
        case '<':
          expr = new LTExpr(expr, parseAddExpr());
          break;
        case GE:
          expr = new GEExpr(expr, parseAddExpr());
          break;
        case LE:
          expr = new LEExpr(expr, parseAddExpr());
          break;
        case EQ:
          expr = new EQExpr(expr, parseAddExpr());
          break;
        case NEQ:
          expr = new NEQExpr(expr, parseAddExpr());
          break;
        default:
          _peekToken = token;
          return expr;
      }
    }
  }

  private Expr parseAndExpr()
  {
    Expr expr = parseCmpExpr();

    while (true) {
      int token = parseToken();
      if (token != AND) {
        _peekToken = token;
        return expr;
      }

      expr = new AndExpr(expr, parseCmpExpr());
    }
  }

  private Expr parseOrExpr()
  {
    Expr expr = parseAndExpr();

    while (true) {
      int token = parseToken();
      if (token != OR) {
        _peekToken = token;
        return expr;
      }

      expr = new OrExpr(expr, parseAndExpr());
    }
  }

  private Expr parseIfExpr()
  {
    Expr expr = parseOrExpr();

    int token = parseToken();
    if (token != '?') {
      _peekToken = token;
      return expr;
    }

    Expr trueExpr = parseIfExpr();

    token = parseToken();
    if (token != ':')
      return error("Expected ':'");

    Expr falseExpr = parseIfExpr();

    return new IfExpr(expr, trueExpr, falseExpr);
  }

  private int parseToken()
  {

    if (_peekToken != UNSET) {
      int toReturn = _peekToken;
      _peekToken = UNSET;
      return toReturn;
    }

    int ch = consumeWhiteSpace();

    switch (ch) {
      case '(':
      case ')':
      case '?':
      case ':':
      case ';':
      case '+':
      case '-':
      case '%':
      case '*':
      case '/':
        return ch;
      case '>':
        if (read() == '=')
          return GE;
        unread();
        return '>';
      case '<':
        if (read() == '=')
          return LE;
        unread();
        return '<';
      case '=':
        if (read() == '=')
          return EQ;
        return UNKNOWN;
      case '!':
        if (read() == '=')
          return NEQ;
        return UNKNOWN;
      case '&':
        if (read() == '&')
          return AND;
        return UNKNOWN;
      case '|':
        if (read() == '|')
          return OR;
        return UNKNOWN;
    }

    return parseIntegerToken(ch);
  }

  private int parseIntegerToken(int ch)
  {
    if ('0' <= ch && ch <= '9') {
      _integer = ch - '0';
      for (ch = read(); '0' <= ch && ch <= '9'; ch = read()) {
        _integer = _integer * 10 + ch - '0';
      }

      unread();
      return INTEGER;
    }

    else if (ch == 'n') {
      if (Character.isLetter(read()))
        return UNKNOWN;

      unread();
      return VARIABLE_N;
    }

    return UNKNOWN;
  }

  /**
   * Consumes whitespaces and returns the last non-whitespace character.
   */
  private int consumeWhiteSpace()
  {
    while (true) {
      int ch = read();
      switch (ch) {
        case ' ':
        case '\n':
        case '\t':
        case '\r':
          continue;
        default:
          return ch;
      }
    }
  }

  private int read()
  {
    if (_parseIndex < _exprLength)
      return _expr.charAt(_parseIndex++);
    else
      return -1;
  }

  private void unread()
  {
    if (_parseIndex > 0 && _parseIndex < _exprLength)
      _parseIndex--;
  }

  private Expr error(String message)
  {
    _isError = true;
    return NExpr.N_EXPR;
  }
}
