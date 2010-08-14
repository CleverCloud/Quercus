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

package com.caucho.es.parser;

import com.caucho.es.*;
import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import java.io.CharConversionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * JavaScript lexer.
 */
class Lexer {
  private static final L10N L = new L10N(Lexer.class);
  
  final static int ERROR = -3;
  final static int START = -2;
  final static int EOF = -1;

  final static int RESERVED = 256;
  final static int LITERAL = RESERVED + 1;
  final static int REGEXP = LITERAL + 1;
  final static int IDENTIFIER = REGEXP + 1;
  final static int THIS = IDENTIFIER + 1;

  final static int HASH_DEF = THIS + 1;
  final static int HASH_REF = HASH_DEF + 1;

  final static int BIN_OP = HASH_REF + 1;
  final static int UNARY_OP = BIN_OP + 1;
  final static int BANDU_OP = UNARY_OP + 1;

  final static int RSHIFT = BANDU_OP + 1;
  final static int URSHIFT = RSHIFT + 1;
  final static int LSHIFT = URSHIFT + 1;
  final static int BITAND = LSHIFT + 1;
  final static int BITOR = BITAND + 1;

  final static int GEQ = BITOR + 1;
  final static int LEQ = GEQ + 1;
  final static int EQ = LEQ + 1;
  final static int NEQ = EQ + 1;

  final static int STRICT_EQ = NEQ + 1;
  final static int STRICT_NEQ = STRICT_EQ + 1;

  final static int AND = STRICT_NEQ + 1;
  final static int OR = AND + 1;

  final static int ASSIGN_OP = OR + 1;

  final static int PREFIX = ASSIGN_OP + 1;
  final static int POSTFIX = PREFIX + 1;
  final static int DELETE = POSTFIX + 1;
  final static int VOID = DELETE + 1;
  final static int TYPEOF = VOID + 1;

  final static int IF = TYPEOF + 1;
  final static int ELSE = IF + 1;

  final static int SWITCH = ELSE + 1;
  final static int CASE = SWITCH + 1;
  final static int DEFAULT = CASE + 1;

  final static int WHILE = DEFAULT + 1;
  final static int DO = WHILE + 1;
  final static int FOR = DO + 1;
  final static int IN = FOR + 1;
  final static int BREAK = IN + 1;
  final static int CONTINUE = BREAK + 1;

  final static int FUNCTION = CONTINUE + 1;
  final static int CONSTRUCTOR = FUNCTION;
  final static int RETURN = CONSTRUCTOR + 1;

  final static int NEW = RETURN + 1;
  final static int VAR = NEW + 1;
  final static int WITH = VAR + 1;

  final static int NULL = WITH + 1;
  final static int UNDEFINED = NULL + 1;
  final static int TRUE = UNDEFINED + 1;
  final static int FALSE = TRUE + 1;
  final static int EVAL = FALSE + 1;

  final static int CLASS = EVAL + 1;
  final static int EXTENDS = CLASS + 1;

  final static int SYNCHRONIZED = EXTENDS + 1;

  final static int TRY = SYNCHRONIZED + 1;
  final static int CATCH = TRY + 1;
  final static int FINALLY = CATCH + 1;
  final static int THROW = FINALLY + 1;

  final static int IMPORT = THROW + 1;
  final static int STATIC = IMPORT + 1;
  
  final static int LAST_LEXEME = STATIC;

  static HashMap ops;
  static HashMap reserved;
  
  Global resin;
  ReadStream is;
  int peek = -1;
  int peek2 = -1;

  ArrayList macros = new ArrayList();

  CharBuffer macroText;
  int macroIndex;
  int macroOldLine;

  int _flags;

  int state;
  int lbrace;
  int stringClose;
  boolean isRegexp;

  LineMap lineMap;
  String filename;
  String lastFilename;
  String beginFilename;

  int lastLine;
  int beginLine;
  int beginLineCh;
  int line;
  int lineCh;

  Op op;
  int lexeme;
  int lastLexeme;
  CharBuffer text;
  CharBuffer lineText = new CharBuffer();
  boolean isEof = false;
  ESId id;
  ESBase literal;
  int intValue;
  boolean hasLf;
  boolean regexpOk;
  String writeln;

  CharBuffer temp = new CharBuffer();

  Lexer(ReadStream is, String filename, int line, LineMap lineMap)
  {
    this.filename = filename;
    this.line = line;
    this.lastFilename = filename;
    this.lastLine = line;
    this.lineMap = lineMap;
    this.is = is;
    peek = -1;
    peek2 = -1;
    text = new CharBuffer();
    lexeme = START;
    lastLexeme = START;
    regexpOk = true;
    macroText = null;
    macroIndex = 0;

    // Initialize the operator table
    if (ops == null) {
      ops = new HashMap();
      opsPut(".", '.', '.', Parser.PREC_DOT, false);
      opsPut("++", '+', POSTFIX, Parser.PREC_DOT, false);
      opsPut("--", '-', POSTFIX, Parser.PREC_DOT, false);

      opsPut("@", '@', '@', Parser.PREC_DOT, false);
      
      opsPut("~", '~', UNARY_OP, Parser.PREC_UMINUS, false);
      opsPut("!", '!', UNARY_OP, Parser.PREC_UMINUS, false);

      opsPut("*", '*', BIN_OP, Parser.PREC_TIMES, false);
      opsPut("/", '/', BIN_OP, Parser.PREC_TIMES, false);
      opsPut("%", '%', BIN_OP, Parser.PREC_TIMES, false);

      opsPut("+", '+', BANDU_OP, Parser.PREC_PLUS, false);
      opsPut("-", '-', BANDU_OP, Parser.PREC_PLUS, false);

      opsPut(">>", RSHIFT, BIN_OP, Parser.PREC_SHIFT, false);
      opsPut(">>>", URSHIFT, BIN_OP, Parser.PREC_SHIFT, false);
      opsPut("<<", LSHIFT, BIN_OP, Parser.PREC_SHIFT, false);

      opsPut(">", '>', BIN_OP, Parser.PREC_CMP, false);
      opsPut(">=", GEQ, BIN_OP, Parser.PREC_CMP, false);
      opsPut("<", '<', BIN_OP, Parser.PREC_CMP, false);
      opsPut("<=", LEQ, BIN_OP, Parser.PREC_CMP, false);
      opsPut("==", EQ, BIN_OP, Parser.PREC_CMP, false);
      opsPut("!=", NEQ, BIN_OP, Parser.PREC_CMP, false);
      opsPut("===", STRICT_EQ, BIN_OP, Parser.PREC_CMP, false);
      opsPut("!==", STRICT_NEQ, BIN_OP, Parser.PREC_CMP, false);

      opsPut("&", '&', BIN_OP, Parser.PREC_BITAND, false);
      opsPut("^", '^', BIN_OP, Parser.PREC_BITXOR, false);
      opsPut("|", '|', BIN_OP, Parser.PREC_BITOR, false);

      opsPut("&&", AND, BIN_OP, Parser.PREC_AND, false);
      opsPut("||", OR, BIN_OP, Parser.PREC_OR, false);

      opsPut("?", '?', '?', Parser.PREC_COND, false);

      opsPut("=", '=', '=', Parser.PREC_ASSIGN, true);
      opsPut("*=", '*', '=', Parser.PREC_ASSIGN, true);
      opsPut("/=", '/', '=', Parser.PREC_ASSIGN, true);
      opsPut("%=", '%', '=', Parser.PREC_ASSIGN, true);
      opsPut("+=", '+', '=', Parser.PREC_ASSIGN, true);
      opsPut("-=", '-', '=', Parser.PREC_ASSIGN, true);
      opsPut(">>=", RSHIFT, '=', Parser.PREC_ASSIGN, true);
      opsPut(">>>=", URSHIFT, '=', Parser.PREC_ASSIGN, true);
      opsPut("<<=", LSHIFT, '=', Parser.PREC_ASSIGN, true);
      opsPut("&=", '&', '=', Parser.PREC_ASSIGN, true);
      opsPut("^=", '^', '=', Parser.PREC_ASSIGN, true);
      opsPut("|=", '|', '=', Parser.PREC_ASSIGN, true);

      opsPut(",", ',', ',', Parser.PREC_COMMA, false);

      reserved = new HashMap();
      resPut("new", NEW);
      resPut("var", VAR);
      resPut("delete", DELETE);
      resPut("void", VOID);
      resPut("typeof", TYPEOF);

      resPut("if", IF);
      resPut("else", ELSE);
      resPut("switch", SWITCH);
      resPut("case", CASE);
      resPut("default", DEFAULT);

      resPut("while", WHILE);
      resPut("do", DO);
      resPut("for", FOR);
      resPut("in", IN);
      resPut("break", BREAK);
      resPut("continue", CONTINUE);

      resPut("null", NULL);
      resPut("undefined", UNDEFINED);
      resPut("true", TRUE);
      resPut("false", FALSE);
      resPut("this", THIS);
      resPut("eval", EVAL);

      resPut("function", FUNCTION);
      //resPut("constructor", CONSTRUCTOR);
      resPut("return", RETURN);

      resPut("with", WITH);

      resPut("class", CLASS);
      resPut("extends", EXTENDS);

      resPut("synchronized", SYNCHRONIZED);

      resPut("try", TRY);
      resPut("catch", CATCH);
      resPut("finally", FINALLY);
      resPut("throw", THROW);

      resPut("import", IMPORT);
      resPut("static", STATIC);
      
      resPut("const", RESERVED);
      resPut("debugger", RESERVED);
      resPut("enum", RESERVED);
      resPut("export", RESERVED);
      resPut("super", RESERVED);
/*
      resPut("boolean", RESERVED);
      resPut("byte", RESERVED);
      resPut("char", RESERVED);
      resPut("double", RESERVED);
      resPut("float", RESERVED);
      resPut("int", RESERVED);
      resPut("long", RESERVED);
      resPut("short", RESERVED);
*/    
      resPut("public", RESERVED);
      resPut("private", RESERVED);
      resPut("protected", RESERVED);
      resPut("throws", RESERVED);
    }
  }
  
  Lexer(ReadStream is, String filename, int line)
  {
    this(is, filename, line, null);
  }
  
  Lexer(ReadStream is, LineMap lineMap)
  {
    this(is, null, 1, lineMap);
  }

  void setLineMap(LineMap lineMap)
  {
    this.lineMap = lineMap;
  }

  private void opsPut(String name, int code, int lex, int prec, boolean flag)
  {
    ops.put(new CharBuffer(name), new Op(code, lex, prec, flag));
  }

  private void resPut(String name, int code)
  {
    reserved.put(new CharBuffer(name), new Integer(code));
  }

  int peek() throws ESParseException
  {
    try {
      if (lexeme == START) {
        lexeme = lex();
      }
      
      lastLexeme = lexeme;

      return lexeme;
    } catch (ESParseException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw error(e.toString());
    }
  }

  int next() throws ESParseException
  {
    try {
      int value = lexeme;

      if (value == START) {
        value = lex();
      }

      lastLexeme = value;
      lexeme = START;

      lastFilename = beginFilename;
      lastLine = beginLine;

      return value;
    } catch (ESParseException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw error(e == null ? "" : e.toString());
    }
  }

  int prev()
  {
    if (lastLexeme == START)
      throw new RuntimeException();

    lexeme = lastLexeme;

    lastLexeme = START;

    return lexeme;
  }

  int last()
  {
    if (lastLexeme == START)
      throw new RuntimeException();

    return lastLexeme;
  }

  private int peekCh() throws ESParseException
  {
    try {
      int ch = read();
      ungetc(ch);
      return (ch);
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Returns the next lexeme
   */
  private int lex() throws ESParseException
  {
    lastFilename = beginFilename;
    lastLine = beginLine;
    
    hasLf = false;

    while (true) {
      beginFilename = filename;
      beginLine = line;
      beginLineCh = lineCh;

      int ch = read();

      switch (ch) {
      case -1:
        isEof = true;
        return EOF;

      case ' ': case '\t': case '\f': case 0x0b: /* vertical tab */
        break;

      case '\n': 
        newline();
        hasLf = true;
        break;

      case '+': case '-': case '*': case '!': case ',': case '^':
      case '<': case '>': case '&': case '|': case '=': case '~':
      case '?':
        regexpOk = true; // exception ++/--
        return lexOp(ch);

      case ')': case ']': 
        regexpOk = false;
        return ch;

      case ':': case ';': case '(': 
      case '[': case '{': case '}':
        regexpOk = true;
        return ch;

      case '.':
        {
          int ch2 = read();

          if (ch2 >= '0' && ch2 <= '9') {
            regexpOk = false;
            return lexFloat(0, ch2);
          }
          else {
            regexpOk = true;
            ungetc(ch2);
            return lexOp(ch);
          }
        }

      case '/':
        {
          int ch2 = read();

          if (ch2 == '/') {
            for (ch2 = read();
                 ch2 > 0 && ch2 != '\n';
                 ch2 = read()) {
            }

            ungetc(ch2);
            break;
          }
          else if (ch2 == '*') {
            boolean seenStar = false;
            for (ch2 = read();
                 ch2 > 0 && (! seenStar || ch2 != '/');
                 ch2 = read()) {
              if (ch2 == '/') {
                ch2 = read();
                if (ch2 == '*')
                  throw error(L.l("comments can't nest"));
              }

              seenStar = ch2 == '*';

              if (ch2 == '\n') {
                newline();
                hasLf = true;
              }
            }
            break;
          }
          else if (regexpOk) {
            regexpOk = false;

            ungetc(ch2);
            lexString('/', null, true, false);

            readRegexpFlags();
            try {
              Pattern regexp = Pattern.compile(literal.toString(), _flags);
              // checking for errors
            } catch (Exception e) {
              // e.printStackTrace();
              throw error(String.valueOf(e));
            }

            return REGEXP;
          } else {
            ungetc(ch2);
            return lexOp(ch);
          }
        }

      case '0': case '1': case '2': case '3': case '4': 
      case '5': case '6': case '7': case '8': case '9':
        regexpOk = false;
        return lexNumber(ch);

      case '"': case '\'':
        regexpOk = false;
        return lexString((char) ch, null, false, false);

      case '@':
        {
          int ch2 = read();

          switch (ch2) {
          case '"':
            CharBuffer macro = new CharBuffer();
            macro.append('(');
            interpolate(macro, '"', null, "\"", "\"", false, false);
            macro.append(')');
            pushMacro(macro);
            break;

          case '\'':
            macro = new CharBuffer();
            macro.append('(');
            interpolate(macro, '\'', null, "\'", "\'", false, false);
            macro.append(')');
            pushMacro(macro);
            break;

          case '@':
            if ((ch2 = read()) < 0)
              throw error(L.l("unexpected end of file"));
            switch (ch2) {
            case '{': ch2 = '}'; break;
            case '<': ch2 = '>'; break;
            case '(': ch2 = ')'; break;
            case '[': ch2 = ']'; break;
            }

            return lexString((char) ch2, null, true, false);

          case '<':
            if ((ch2 = read()) != '<')
              throw error(L.l("illegal character at `@'"));
            if (scanMultiline())
              return LITERAL;
            break;

          case '/':
            macro = new CharBuffer();
            macro.append("new RegExp(");
            interpolate(macro, '/', null, "@@/", "/", true, false);
            macro.append(",");
            macro.append(readRegexpFlags());
            macro.append(")");
            pushMacro(macro);
            break;

          default:
            return lexOp('@');
          }
          break;
        }

      case '%':
        {
          int ch2 = read();

          regexpOk = true;
          ungetc(ch2);
          return lexOp(ch);
        }

      case '#':
        {
          int ch2 = read();
          if (line == 1 && lineCh == 2 && ch2 == '!') {
            for (; ch2 > 0 && ch2 != '\n'; ch2 = read()) {
            }

            ungetc(ch2);
            break;
          }

          if (ch2 >= 'a' && ch2 <= 'z' || ch2 >= 'A' && ch2 <= 'Z') {
            temp.clear();
            for (; ch2 >= 'a' && ch2 <= 'z' || ch2 >= 'A' && ch2 <= 'Z';
                 ch2 = read()) {
              temp.append((char) ch2);
            }

            if (temp.toString().equals("line"))
              scanLine(ch2);
            else if (temp.toString().equals("file"))
              scanFile(ch2);
            else
              throw error(L.l("expected pragma at `{0}'", temp));

            break;
          }

          if (ch2 < '0' || ch2 > '9')
            throw error(L.l("expected digit at {0}", badChar(ch2)));
          intValue = 0;

          for (; ch2 >= '0' && ch2 <= '9'; ch2 = read())
            intValue = 10 * intValue + ch2 - '0';

          if (ch2 == '=')
            return HASH_DEF;
          else if (ch2 == '#')
            return HASH_REF;
          else
            throw error(L.l("expected sharp variable at {0}", badChar(ch)));
        }

      default:
        if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' ||
            ch == '_' || ch == '$') {
          regexpOk = false;
          return lexId(ch);
        } else {
          throw error(L.l("illegal character at {0}", badChar(ch)));
        }
      }
    }
  }

  /**
   * Returns the text object for the lexeme.
   */
  CharBuffer getText() { return text; }

  boolean isEof() { return isEof; }

  /**
   * Used for error messages.
   */
  String getToken() 
  {
    return lineText.substring(beginLineCh, lineCh);
  }
  /**
   * Returns the Id
   */
  ESId getId() { return id; }
  /**
   * Returns true if seen linefeed since the last.
   */
  boolean seenLineFeed() { return hasLf; }

  ESParseException error(String text)
  {
    return new ESParseException(filename, beginLine, beginLineCh,
                                line, lineCh, text);
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
    if (ch >= 0x20 && ch <= 0x7f)
      return "`" + (char) ch + "'";
    else if (ch == '\n')
      return L.l("end of line");
    else if (ch == -1)
      return L.l("end of file");
    else
      return "`" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  String getFilename()
  {
    if (lineMap != null) {
      LineMap.Line map = lineMap.getLine(line);
      if (map != null)
        return map.getSourceFilename();
    }

    return filename;
  }

  long getLastModified()
  {
    if (is.getPath() == null)
      return 0;
    else
      return is.getPath().getLastModified();
  }
  
  int getLine()
  {
    if (lineMap != null) {
      LineMap.Line map = lineMap.getLine(line);
      if (map != null) {
        return map.getSourceLine(line);
      }
    }

    return line;
  }

  String getLastFilename()
  {
    if (lineMap != null) {
      LineMap.Line map = lineMap.getLine(lastLine);
      if (map != null)
        return map.getSourceFilename();
    }

    return lastFilename;
  }
  
  int getLastLine()
  {
    if (lineMap != null) {
      LineMap.Line map = lineMap.getLine(lastLine);
      if (map != null) {
        return map.getSourceLine(lastLine);
      }
    }

    return lastLine;
  }
  
  private void pushMacro(CharBuffer cb)
    throws ESParseException
  {
    if (peek >= 0)
      cb.append((char) read()); // Because of peek
    if (peek >= 0)
      cb.append((char) read()); // Because of peek
    if (macroText != null)
      macros.add(new Macro(macroText, macroIndex, macroOldLine));
    macroText = cb;
    macroIndex = 0;
    macroOldLine = line;
  }

  /**
   * Update variables to handle a newline.
   */
  private void newline()
  {
    line++;
    lineCh = 0;
    lineText.clear();
  }

  /**
   * Handles all the goodies for a floating point number after the
   * dot or 'e'
   */
  private int lexFloat(double value, int ch) throws ESParseException
  {
    int expt = 0;

    for (; ch >= '0' && ch <= '9'; ch = read()) {
      value = 10 * value + ch - '0';
      expt--;
    }

    if (ch == 'e' || ch == 'E') {
      ch = read();

      int sign = 1;
      if (ch == '-') {
        sign = -1;
        ch = read();
      } else if (ch == '+') {
        ch = read();
      }

      if (ch < '0' || ch > '9')
        throw error(L.l("expected exponent at {0}", badChar(ch)));

      int userExpt = 0;
      for (; ch >= '0' && ch <= '9'; ch = read()) {
        userExpt = 10 * userExpt + ch - '0';
      }

      expt += sign * userExpt;
    }
    
    ungetc(ch);
    if (expt >= 0)
      literal = ESNumber.create(value * Math.pow(10, expt));
    else
      literal = ESNumber.create(value / Math.pow(10, -expt));
    return LITERAL;
  }

  /**
   * Lexeme for a number
   */
  private int lexNumber(int ch) throws ESParseException
  {
    int radix = 10;
    double value = 0;
    boolean hasChar = true;

    if (ch == '0') {
      ch = read();
      if (ch >= '0' && ch <= '9')
        radix = 8;
      else if (ch == 'x' || ch == 'X') {
        hasChar = false;
        radix = 16;
        ch = read();
      }
    }

    for (; ch >= 0; ch = read()) {
      if (ch >= '0' && ch <= '9') {
        value = radix * value + ch - '0';
        hasChar = true;

        if (radix == 8 && ch >= '8')
          throw error(L.l("expected octal digit at {0}", badChar(ch)));
      } else if (radix == 16 && ch >= 'a' && ch <= 'f') {
        hasChar = true;
        value = radix * value + ch - 'a' + 10;
      }
      else if (radix == 16 && ch >= 'A' && ch <= 'F') {
        hasChar = true;
        value = radix * value + ch - 'A' + 10;
      }
      else
        break;
    }

    if (! hasChar)
      throw error(L.l("expected hex digit at {0}", badChar(ch)));

    if (radix == 10 && ch == '.') {
      ch = read();
      
      if (ch >= '0' && ch <= '9')
        return lexFloat(value, ch);
      else {
        ungetc(ch);
        literal = ESNumber.create(value);
        return LITERAL;
      }
    } else if (radix == 10 && (ch == 'e' || ch == 'E'))
      return lexFloat(value, ch);
    else {
      ungetc(ch);
      literal = ESNumber.create(value);
      return LITERAL;
    }
  }

  /**
   * Returns the number for a hex digit.
   */
  private int hexDigit(int ch) throws ESParseException
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      throw error(L.l("expected hex digit at {0}", badChar(ch)));
  }

  /**
   * Lexeme for a string.
   */
  private int lexString(char endCh,
                        String endTail,
                        boolean isRegexp,
                        boolean isMultiline)
    throws ESParseException
  {
    text.setLength(0);
    
    int ch = read();
    for (; ch >= 0; ch = read()) {
      if (ch == '\n') {
        if (isMultiline) {
        }
        else if (isRegexp)
          throw error(L.l("unexpected end of line in regular expression"));
        else
          throw error(L.l("unexpected end of line in string"));
        newline();
      }

      if (ch != endCh) {
      }
      else if (endTail == null) {
        literal = ESString.create(text.toString());
        return LITERAL;
      }
      else if (! text.endsWith(endTail)) {
      }
      else if (text.length() == endTail.length()) {
        literal = ESString.create("");
        return LITERAL;
      }
      else {
        char tailCh = text.charAt(text.length() - endTail.length() - 1);

        if (tailCh == '\n') {
          text.setLength(text.length() - endTail.length() - 1);
          literal = ESString.create(text.toString());
          return LITERAL;
        }
      }

      if (ch == '\\') {
        ch = read();
        switch (ch) {
        case -1:
          if (isRegexp)
            throw error(L.l("unexpected end of file in regular expression"));
          else
            throw error(L.l("unexpected end of file in string"));

        case '\n':
          if (isRegexp)
            throw error(L.l("unexpected end of line in regular expression"));
          else
            throw error(L.l("unexpected end of line in string"));

        case 'b':
          if (isRegexp)
            text.append("\\b");
          else
            text.append('\b');
          break;

        case 'e':
          text.append((char) 0x1b);
          break;

        case 'f':
          text.append('\f');
          break;

        case 'n':
          text.append('\n');
          break;

        case 'r':
          text.append('\r');
          break;

        case 't':
          text.append('\t');
          break;

        case 'v':
          text.append((char) 0xb);
          break;

        case 'c':
          {
            ch = read();
            if (ch >= 'a' && ch <= 'z')
              text.append((char) (ch - 'a' + 1));
            else if (ch >= 'A' && ch <= 'Z')
              text.append((char) (ch - 'A' + 1));
            else if (ch - '@' >= 0 && ch - '@' < ' ')
              text.append((char) (ch - '@'));
            else
              throw error(L.l("expected control character at {0}",
                              badChar(ch)));
          }
          break;

        case 'o':
          {
            int value = 0;
            while ((ch = read()) >= '0' && ch <= '8') {
              value = 8 * value + ch - '0';
            }
            ungetc(ch);
            text.append((char) value);
          }
          break;

        case 'x':
          {
            int value = 16 * hexDigit(read());
            value += hexDigit(read());
            text.append((char) value);
          }
          break;

        case 'u':
          {
            int value = 4096 * hexDigit(read());
            value += 256 * hexDigit(read());
            value += 16 * hexDigit(read());
            value += hexDigit(read());
            text.append((char) value);
          }
          break;

        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
          {
            int value = ch - '0';

            if (ch != '0' && isRegexp) {
              text.append('\\');
              text.append((char) ch);
              break;
            }

            if ((ch = read()) >= '0' && ch <= '7') {
              value = 8 * value + ch - '0';

              if (value >= 040) {
              }
              else if ((ch = read()) >= '0' && ch <= '7')
                value = 8 * value + ch - '0';
              else
                ungetc(ch);
            } else
              ungetc(ch);
            text.append((char) value);
          }
          break;

        default:
          if (isRegexp)
            text.append('\\');
          text.append((char) ch);
          break;
        }
      } else {
        text.append((char) ch);
      }
    }

    if (ch != -1) {
    }
    else if (isRegexp)
      throw error(L.l("unexpected end of file in regular expression"));
    else
      throw error(L.l("unexpected end of file in string"));

    literal = ESString.create(text.toString());

    return LITERAL;
  }

  private void scanMacroStatement(CharBuffer macro, int end,
                                  boolean isRegexp, boolean multiline)
   throws ESParseException
  {
    int ch;

    while ((ch = read()) >= 0 && ch != end) {
      macro.append((char) ch);

      switch (ch) {
      case '\\':
        ch = read();
        macro.append((char) ch);
        break;

      case '\'':
      case '"':
         int testch = ch;

        while ((ch = read()) >= 0) {
           if (ch == '\\') {
             macro.append((char) ch);
             ch = read();
           }
           else if (ch == testch) {
             macro.append((char) ch);
             break;
           } else if (ch == '\n') {
             if (! multiline)
               throw error("unexpected end of line in " +
                          (isRegexp ? "regular expression" : "string"));
             newline();
           }
  
           macro.append((char) ch);
         }
        break;

      case '(':
        scanMacroStatement(macro, ')', isRegexp, multiline);
        macro.append(')');
        break;

      case '{':
        scanMacroStatement(macro, '}', isRegexp, multiline);
        macro.append('}');
        break;

      case '\n':
        if (! multiline)
          throw error("unexpected end of line in " +
                      (isRegexp ? "regular expression" : "string"));
        newline();
        break;

      default:
        break;
      }
    }
  }

  private void interpolate(CharBuffer macro, int tail,
                           String matchText,
                           String beginStr, String endStr,
                           boolean isRegexp, boolean multiline)
    throws ESParseException
  {
    int ch = read();
    int ch1;

    macro.append(beginStr);
    int start = macro.length();
  loop:
    for (; ch >= 0; ch = read()) {
      switch (ch) {
      case '\\':
        macro.append((char) ch);
        ch = read();
        if (ch != -1)
          macro.append((char) ch);
        break;

      case '$':
        if ((ch = read()) == -1)
          break;

        if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' ||
            ch == '_' || ch == '$') {
          macro.append(endStr);
          macro.append("+(");
          macro.append((char) ch);

          while ((ch = read()) >= 0 &&
                 (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                 (ch >= '0' && ch <= '9') || ch == '_' || ch == '$') {
            macro.append((char) ch);
          }
          ungetc(ch);
          macro.append(")+");
          macro.append(beginStr);
        } else if (ch == '{') {
          macro.append(endStr);
          macro.append("+(");
          scanMacroStatement(macro, '}', isRegexp, multiline);
          macro.append(")+");
          macro.append(beginStr);
        } else if (ch == '(') {
          macro.append(endStr);
          macro.append("+(");
          scanMacroStatement(macro, ')', isRegexp, multiline);
          macro.append(")+");
          macro.append(beginStr);
        } else {
          ungetc(ch);
          macro.append('$');
        }
        break;

      default:
        if (ch == '\n') {
          newline();
          if (! multiline)
            throw error("unexpected end of line in " +
                        (isRegexp ? "regular expression" : "string"));
        }

        if (ch != tail) {
        }
        else if (matchText == null) {
          break loop;
        }
        else if (! macro.endsWith(matchText)) {
        }
        else if (macro.length() - start == matchText.length()) {
          macro.setLength(start);
          break loop;
        }
        else if (macro.charAt(macro.length() - matchText.length() - 1) == '\n') {
          macro.setLength(macro.length() - matchText.length() - 1);
          break loop;
        }

        macro.append((char) ch);

        break;
      }
    }

    macro.append(endStr);
  }

  private boolean scanMultiline() throws ESParseException
  {
    int ch;
    CharBuffer end = new CharBuffer();
    boolean interpolate = true;
    boolean endNewline = true;

    if ((ch = read()) >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' ||
        ch == '_' || ch == '$') {
      for (; ch >= 0 && ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' ||
             ch == '_' || ch == '$' || ch >= '0' && ch <= '9';
           ch = read()) {
        end.append((char) ch);
      }
    } else if (ch == '\'') {
      interpolate = false;
      for (ch = read();
           ch >= 0 && ch != '\'' && ch != '\n';
           ch = read()) {
        end.append((char) ch);
      }

      if (ch != '\'')
        throw error(L.l("multiline escape error at {0}", badChar(ch)));
      ch = read();
    } else if (ch == '`') {
      interpolate = false;
      for (ch = read();
           ch >= 0 && ch != '`' && ch != '\n';
           ch = read()) {
        end.append((char) ch);
      }

      if (ch != '`')
        throw error(L.l("multiline escape error at {0}", badChar(ch)));
      endNewline = false;
    } else if (ch == '\"') {
      for (ch = read();
           ch >= 0 && ch != '\"' && ch != '\n';
           ch = read()) {
        end.append((char) ch);
      }

      if (ch != '\"')
        throw error(L.l("multiline escape error at {0}", badChar(ch)));
      ch = read();
    }
    
    int oldLine = line;
    CharBuffer lineTail = null;

    if (endNewline) {
      lineTail = new CharBuffer();
      for (; ch >= 0 && ch != '\n'; ch = read()) {
        lineTail.append((char) ch);
      }
      if (ch == '\r') {
        lineTail.append((char) ch);
        ch = read();
      }
      if (ch == '\n') {
        newline();
        lineTail.append((char) ch);
      }
    }

    CharBuffer macro = null;
    String endString = end.toString();
    if (interpolate) {
      macro = new CharBuffer();
      macro.append('(');
      interpolate(macro, '\n', endString, "@<<`" + endString + "`", 
                  "\n" + endString + '\n', false, true);
      macro.append("+'\\n')");
    } else {
      if (endNewline) {
        lexString('\n', endString, false, true);
        text.append('\n');
        literal = ESString.create(text);
      } else {
        lexString('\n', endString, false, true);
        line -= 2;
      }
    }

    if (endNewline) {
      pushMacro(lineTail);
      line = oldLine;
    }

    if (interpolate) {
      pushMacro(macro);
      line++;
      return false;
    } else
      return true;
  }

  private int readRegexpFlags() throws ESParseException
  {
    int ch;
    while (true) {
      switch ((ch = read())) {
      case 'x':
        _flags |= Pattern.COMMENTS;
        break;
      case 'i':
        _flags |= Pattern.CASE_INSENSITIVE;
        break;
      case 'g':
        break;
      case 'm':
        _flags |= Pattern.MULTILINE;
        break;
      case 's':
        break;
      default:
        ungetc(ch);
        return _flags;
      }
    }
  }

  /**
   * Lexeme for an Id.  Reserved words are looked up in a
   * HashMap.
   */
  private int lexId(int ch) throws ESParseException
  {
    text.setLength(0);

    text.append((char) ch);

    while (true) {
      ch = read();

      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' ||
          ch == '_' || ch == '$' || ch >= '0' && ch <= '9') {
        text.append((char) ch);
      } else {
        ungetc(ch);

        break;
      }
    }

    Integer value = (Integer) reserved.get(text);
 
    if (value == null) {
      id = ESId.intern(text.toString());
      return IDENTIFIER;
    }
    else {
      int intValue = value.intValue();

      switch (intValue) {
      case NULL: 
        literal = ESBase.esNull;
        return LITERAL;

      case UNDEFINED: 
        literal = ESBase.esUndefined;
        return LITERAL;

      case FALSE: 
        literal = ESBoolean.create(false);
        return LITERAL;

      case TRUE: 
        literal = ESBoolean.create(true);
        return LITERAL;

      default: return value.intValue();
      }
    }
  }

  /**
   * Lexeme for an operation
   */
  private int lexOp(int ch) throws ESParseException
  {
    text.setLength(0);
    text.append((char) ch);

  loop:
    while ((ch = read()) >= 0) {
      switch (ch) {
      case '+': case '-': case '*': case '/': case '%': case '!':
      case '<': case '.': case '>': case '&': case '|': case '=':
      case '^': case '?':
        text.append((char) ch);

        op = (Op) ops.get(text);
        if (op == null) {
          text.setLength(text.length() - 1);
          ungetc(ch);
          break loop;
        }
        break;

      default:
        ungetc(ch);
        break loop;
      }
    }

    op = (Op) ops.get(text);

    // XXX: non-reachable
    if (op == null)
      throw error(L.l("expected operator at `{0}'", text.toString()));

    return op.lexeme;
  }

  /**
   * Return the operation for a lexeme.  Binary operations like '*' will
   * return BIN_OP as the lexeme.  Calling getOp() will get the actual
   * operation.
   */
  int getOp()
  {
    return op.op;
  }

  int getPrecedence()
  {
    return op.precedence;
  }

  boolean isRightAssoc()
  {
    return op.isRightAssoc;
  }

  ESBase getLiteral()
  {
    return literal;
  }

  int getFlags()
  {
    return _flags;
  }

  private void scanLine(int ch) throws ESParseException
  {
    for (; ch == ' ' || ch == '\t'; ch = read()) {
    }

    if (ch < '0' || ch > '9')
      throw error(L.l("expected digit at {0}", badChar(ch)));

    line = 0;
    for (; ch >= '0' && ch <= '9'; ch = read())
      line = 10 * line + ch - '0';

    for (; ch == ' ' || ch == '\t'; ch = read()) {
    }

    if (ch != '#')
      throw error(L.l("expected `#' at {0}", badChar(ch)));
  }

  private void scanFile(int ch) throws ESParseException
  {
    for (; ch == ' ' || ch == '\t'; ch = read()) {
    }

    temp.clear();
    for (; ch >= 0 && ch != ' ' && ch != '\t' && ch != '#'; ch = read())
      temp.append((char) ch);

    if (temp.length() == 0)
      throw error(L.l("expected filename at {0}", badChar(ch)));
    filename = temp.toString();

    for (; ch == ' ' || ch == '\t'; ch = read()) {
    }

    line = 0;
    for (; ch >= '0' && ch <= '9'; ch = read())
      line = 10 * line + ch - '0';

    if (line == 0)
      line = 1;

    for (; ch == ' ' || ch == '\t'; ch = read()) {
    }

    if (ch != '#')
      throw error(L.l("expected `#' at {0}", badChar(ch)));
  }

  /**
   * Reads the next character.
   */
  private int read() throws ESParseException
  {
    lineCh++;
    if (peek >= 0) {
      int ch = peek;
      peek = peek2;
      peek2 = -1;
      return ch;
    } 

    while (macroText != null) {
      if (macroIndex < macroText.length()) {
        int ch = macroText.charAt(macroIndex++);
        lineText.append((char) ch);
        return ch;
      }

      line = macroOldLine;

      if (macros.size() == 0)
        macroText = null;
      else {
        Macro macro = (Macro) macros.remove(macros.size() - 1);
        macroText = macro.text;
        macroIndex = macro.index;
        macroOldLine = macro.oldLine;
      }
    }

    try {
      int ch = is.readChar();

      if (ch == '\r') {
        ch = is.readChar();
        if (ch != '\n') {
          if (ch == '\r')
            peek = '\n';
          else
            peek = ch;
        }
        ch = '\n';
      }
      lineText.append((char) ch);

      return ch;
    } catch (CharConversionException e1) {
      throw error(L.l("expected {0} encoded character", is.getEncoding()));
    } catch (IOException e1) {
      throw new ESParseException(e1);
    }
  }

  private void ungetc(int ch)
  {
    peek2 = peek;
    peek = ch;
    if (lineCh > 0)
      lineCh--;

    /*
    if (ch == '\n')
      line--;
    */
  }

  static class Op {
    int op;
    int lexeme;
    int precedence;
    boolean isRightAssoc;

    Op(int op, int lexeme, int precedence, boolean isRightAssoc)
    {
      this.op = op;
      this.lexeme = lexeme;
      this.precedence = precedence;
      this.isRightAssoc = isRightAssoc;
    }
  };

  class Macro {
    CharBuffer text;
    int index;
    int oldLine;

    void clear()
    {
      text.clear();
      index = 0;
    }
    
    Macro(CharBuffer cb, int index, int oldLine)
    {
      this.text = cb;
      this.index = index;
      this.oldLine = oldLine;
    }
  }
}

