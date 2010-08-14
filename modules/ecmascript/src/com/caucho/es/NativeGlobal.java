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

package com.caucho.es;

import com.caucho.es.parser.Parser;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import java.io.IOException;

/**
 * JavaScript object
 */
class NativeGlobal extends Native {
  static final int EVAL = 2;
  static final int PARSE_INT = 3;
  static final int PARSE_FLOAT = 4;
  static final int ESCAPE = 5;
  static final int UNESCAPE = 6;
  static final int IS_NAN = 7;
  static final int IS_FINITE = 8;

  static final int PRINT = 9;
  static final int SYSTEM = PRINT + 1;

  /**
   * Create a new object based on a prototype
   */
  NativeGlobal(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Object object
   */
  static void create(Global resin)
  {
    put(resin, "eval", EVAL, 1);
    put(resin, "parseInt", PARSE_INT, 2);
    put(resin, "parseFloat", PARSE_FLOAT, 1);
    put(resin, "escape", ESCAPE, 1);
    put(resin, "unescape", UNESCAPE, 1);
    put(resin, "isNaN", IS_NAN, 1);
    put(resin, "isFinite", IS_FINITE, 1);

    put(resin, "print", PRINT, 1);
    put(resin, "system", SYSTEM, 1);
  }

  private static void put(Global resin, String name, int n, int len)
  {
    ESId id = ESId.intern(name);

    resin.addProperty(id, new NativeGlobal(name, n, len));
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case EVAL:
      return eval(eval, length);

    case PARSE_INT:
      return parseInt(eval, length);

    case PARSE_FLOAT:
      if (length < 1)
        return ESNumber.NaN;
      else {
        return ESNumber.create(ESString.parseFloat(eval.getArg(0).toStr()));
      }

    case ESCAPE:
      return escape(eval, length);

    case UNESCAPE:
      return unescape(eval, length);

    case IS_NAN:
      if (length < 1)
        return esUndefined;
      else
        return ESBoolean.create(Double.isNaN(eval.getArg(0).toNum()));

    case IS_FINITE:
      if (length < 1)
        return esUndefined;
      else {
        double value = eval.getArg(0).toNum();
        if (Double.isNaN(value))
          return ESBoolean.create(false);
        else if (value == 1.0/0.0)
          return ESBoolean.create(false);
        else if (value == -1.0/0.0)
          return ESBoolean.create(false);
        else
          return ESBoolean.create(true);
      }

    case PRINT:
      System.out.print(eval.getArg(0).toStr().toString());
      return esNull;

    case SYSTEM:
      String arg = eval.getArg(0).toStr().toString();
      String []args = new String[3];
      Process process;
      try {
        args[0] = "sh";
        args[1] = "-c";
        args[2] = arg;
        process = Runtime.getRuntime().exec(args);
        return ESNumber.create(process.waitFor());
      } catch (Exception e) {
        throw new ESWrapperException(e);
      }

    default:
      throw new ESException("Unknown object function");
    }
  }

  private ESBase eval(Call eval, int length) throws Throwable
  {
    if (length < 1)
      return esUndefined;

    ESBase arg = eval.getArg(0);
    if (! (arg instanceof ESString))
      return arg;

    String string = arg.toString();

    Global resin = Global.getGlobalProto();
    ESBase context = eval.getFunctionContext();
    Script script = null;
    ReadStream is = null;
    try {
      Parser parser = new Parser();
      is = Vfs.openString(string);
      script = parser.parseEval(is, "eval", 1);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (is != null)
        is.close();
    }

    ESCallable jsClass = script.initClass(resin, eval.getGlobal());
    
    return jsClass.call(2, eval.caller, 0);
  }

  private ESBase parseInt(Call eval, int length) throws Throwable
  {
    if (length < 1)
      return ESNumber.NaN;

    ESString string = eval.getArg(0).toStr();
    int len = string.length();

    int i = 0;
    for (; i < len && Character.isSpaceChar(string.charAt(i)); i++) {
    }

    int sign = 1;
    if (i < len && string.charAt(i) == '+') {
      i++;
    } else if (i < len && string.charAt(i) == '-') {
      sign = -1;
      i++;
    }

    int radix = 0;
    if (length > 1) {
      radix = eval.getArg(1).toInt32();
      if (radix == 0) {
      }
      else if (radix < 2 || radix > 36)
        return ESNumber.NaN;
      else if (radix == 16 && i + 1 < length &&
               string.charAt(i) == '0' &&
               (string.charAt(i + 1) == 'x' ||
                string.charAt(i + 1) == 'X'))
        i += 2;
    }
    
    if (radix != 0) {
    }
    else if (i >= len)
      radix = 10;
    else if (string.charAt(i) != '0')
      radix = 10;
    else if (i + 1 < len && 
             (string.charAt(i + 1) == 'x' || string.charAt(i + 1) == 'X')) {
      radix = 16;
      i += 2;
    } else {
      radix = 8;
    }

    long value = 0;
    boolean hasDigit = false;
    for (; i < len; i++) {
      int ch = string.charAt(i);

      if (radix <= 10 && ('0' <= ch && ch <= '0' + radix - 1)) {
        value = radix * value + string.charAt(i) - '0';
        hasDigit = true;
      } else if (radix > 10 && ('0' <= ch && ch <= '9')) {
        value = radix * value + string.charAt(i) - '0';
        hasDigit = true;
      } else if (radix > 10 && ('a' <= ch && ch <= 'a' + radix - 11)) {
        value = radix * value + string.charAt(i) - 'a' + 10;
        hasDigit = true;
      } else if (radix > 10 && ('A' <= ch && ch <= 'A' + radix - 11)) {
        value = radix * value + string.charAt(i) - 'A' + 10;
        hasDigit = true;
      } else
        break;
    }

    if (hasDigit)
      return ESNumber.create((double) sign * value);
    else
      return ESNumber.NaN;
  }

  static ESBase escape(Call eval, int length) throws Throwable
  {
    if (length < 1)
      return esUndefined;
    
    ESString string = eval.getArg(0).toStr();
    StringBuffer sbuf = new StringBuffer();

    for (int i = 0; i < string.length(); i++) {
      int ch = string.charAt(i);

      if (ch >= 'a' && ch <= 'z' ||
          ch >= 'A' && ch <= 'Z' ||
          ch >= '0' && ch <= '9' ||
          ch == '@' || ch == '*' || ch == '.' || ch == '_' ||
          ch == '+' || ch == '-' || ch == '/') {
        sbuf.append((char) ch);
      } else if (ch < 256) {
        sbuf.append('%');
        sbuf.append(Integer.toHexString(ch >> 4));
        sbuf.append(Integer.toHexString(ch & 0xf));
      } else {
        sbuf.append("%u");
        sbuf.append(Integer.toHexString(ch >> 12));
        sbuf.append(Integer.toHexString((ch >> 8) & 0xf));
        sbuf.append(Integer.toHexString((ch >> 4) & 0xf));
        sbuf.append(Integer.toHexString(ch & 0xf));
      }
    } 

    return ESString.create(sbuf.toString());
  }

  static ESBase unescape(Call eval, int length) throws Throwable
  {
    if (length < 1)
      return esUndefined;
    
    ESString string = eval.getArg(0).toStr();
    StringBuffer sbuf = new StringBuffer();

    for (int i = 0; i < string.length(); i++) {
      int ch = string.charAt(i);

      if (ch == '%' && i + 2 < string.length()) {
        int limit = 2;
        int start = 1;

        if (string.charAt(i + 1) == 'u') {
          limit = 4;
          start = 2;
        }

        int newCh = 0;
        int j = 0;
        for (; j < limit && i + j + start < string.length(); j++) {
          if ((ch = string.charAt(i + j + start)) >= '0' && ch <= '9')
            newCh = 16 * newCh + ch - '0';
          else if (ch >= 'a' && ch <= 'f')
            newCh = 16 * newCh + ch - 'a' + 10;
          else if (ch >= 'A' && ch <= 'F')
            newCh = 16 * newCh + ch - 'A' + 10;
          else
            break;
        }

        if (j != limit) {
          sbuf.append('%');
        } else {
          sbuf.append((char) newCh);
          i += limit + start - 1;
        }
      }
      else
        sbuf.append((char) ch);
    }

    return ESString.create(sbuf.toString());
  }
}
