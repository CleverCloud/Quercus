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

import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;

/**
 * JavaScript object
 */
class NativeString extends Native {
  static final int NEW = 1;
  static final int TO_STRING = NEW + 1;
  static final int FROM_CHAR_CODE = TO_STRING + 1;
  static final int VALUE_OF = FROM_CHAR_CODE + 1;
  static final int CHAR_AT = VALUE_OF + 1;
  static final int CHAR_CODE_AT = CHAR_AT + 1;
  static final int INDEX_OF = CHAR_CODE_AT + 1;
  static final int LAST_INDEX_OF = INDEX_OF + 1;
  static final int SPLIT = LAST_INDEX_OF + 1;
  static final int SUBSTRING = SPLIT + 1;
  static final int TO_UPPER_CASE = SUBSTRING + 1;
  static final int TO_LOWER_CASE = TO_UPPER_CASE + 1;

  // js1.2
  static final int CONCAT = TO_LOWER_CASE + 1;
  static final int MATCH = CONCAT + 1;
  static final int REPLACE = MATCH + 1;
  static final int SEARCH = REPLACE + 1;
  static final int SLICE = SEARCH + 1;
  static final int SUBSTR = SLICE + 1;

  // caucho
  static final int PRINTF = SUBSTR + 1;
  static final int CONTAINS = PRINTF + 1;
  static final int STARTS_WITH = CONTAINS + 1;
  static final int ENDS_WITH = STARTS_WITH + 1;
  static final int GET_BYTES = ENDS_WITH + 1;

  static final int REPLACE_WS = 0;
  static final int REPLACE_DIGIT = REPLACE_WS + 1;
  static final int REPLACE_ID = REPLACE_DIGIT + 1;

  private NativeString(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native String object
   */
  static ESObject create(Global resin)
  {
    NativeString nativeString = new NativeString("String", NEW, 1);
    ESObject stringProto = new ESWrapper("String", resin.objProto,
                                         ESString.NULL);
    NativeWrapper string;
    string = new NativeWrapper(resin, nativeString,
                               stringProto, ESThunk.STRING_THUNK);
    resin.stringProto = stringProto;

    stringProto.put("length", ESNumber.create(0), 
                    DONT_ENUM|DONT_DELETE|READ_ONLY);

    put(stringProto, "valueOf", VALUE_OF, 0);
    put(stringProto, "toString", TO_STRING, 0);
    put(stringProto, "charAt", CHAR_AT, 1);
    put(stringProto, "charCodeAt", CHAR_CODE_AT, 1);
    put(stringProto, "indexOf", INDEX_OF, 2);
    put(stringProto, "lastIndexOf", LAST_INDEX_OF, 2);
    put(stringProto, "split", SPLIT, 1);
    put(stringProto, "substring", SUBSTRING, 2);
    put(stringProto, "toUpperCase", TO_UPPER_CASE, 0);
    put(stringProto, "toLowerCase", TO_LOWER_CASE, 0);

    put(string, "fromCharCode", FROM_CHAR_CODE, 0);

    // js1.2
    put(stringProto, "concat", CONCAT, 1);
    put(stringProto, "match", MATCH, 1);
    put(stringProto, "replace", REPLACE, 2);
    put(stringProto, "search", SEARCH, 1);
    put(stringProto, "slice", SLICE, 2);
    put(stringProto, "substr", SUBSTR, 2);

    // caucho extensions
    put(string, "printf", PRINTF, 1);
    put(stringProto, "contains", CONTAINS, 1);
    put(stringProto, "startsWith", STARTS_WITH, 1);
    put(stringProto, "endsWith", ENDS_WITH, 1);
    put(stringProto, "getBytes", GET_BYTES, 1);

    stringProto.setClean();
    string.setClean();

    return string;
  }
  
  private static void put(ESObject obj, String name, int n, int len)
  {
    ESId id = ESId.intern(name);

    obj.put(id, new NativeString(name, n, len), DONT_ENUM);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case NEW:
      if (length == 0)
        return ESString.create("");
      else
        return eval.getArg(0).toStr();

    case FROM_CHAR_CODE:
      return fromCharCode(eval, length);

    case VALUE_OF:
    case TO_STRING:
      try {
        return (ESBase) ((ESWrapper) eval.getArg(-1)).value;
      } catch (ClassCastException e) {
        if (eval.getArg(-1) instanceof ESString)
          return eval.getArg(-1);

        if (eval.getArg(-1) instanceof ESThunk)
          return (ESBase) ((ESWrapper) ((ESThunk) eval.getArg(-1)).getObject()).value;

        throw new ESException("toString expects string object");
      }

    case CHAR_AT:
      return charAt(eval, length);

    case CHAR_CODE_AT:
      return charCodeAt(eval, length);

    case INDEX_OF:
      return indexOf(eval, length);

    case LAST_INDEX_OF:
      return lastIndexOf(eval, length);

    case SPLIT:
      return split(eval, length);

    case SUBSTRING:
      return substring(eval, length);

    case TO_UPPER_CASE:
      return eval.getArg(-1).toStr().toUpperCase();

    case TO_LOWER_CASE:
      return eval.getArg(-1).toStr().toLowerCase();

    case CONCAT:
      return concat(eval, length);

    case MATCH:
      return match(eval, length);

    case REPLACE:
      return replace(eval, length);

    case SEARCH:
      return search(eval, length);

    case SLICE:
      return slice(eval, length);

    case SUBSTR:
      return substr(eval, length);

    case PRINTF:
      return printf(eval, length);

    case CONTAINS:
      if (length < 1)
        return ESBoolean.FALSE;
      else
        return eval.getArg(-1).toStr().contains(eval.getArg(0));

    case STARTS_WITH:
      if (length < 1)
        return ESBoolean.FALSE;
      else
        return eval.getArg(-1).toStr().startsWith(eval.getArg(0));

    case ENDS_WITH:
      if (length < 1)
        return ESBoolean.FALSE;
      else
        return eval.getArg(-1).toStr().endsWith(eval.getArg(0));

    case GET_BYTES:
      if (length < 1)
        return eval.wrap(eval.getArgString(-1, length).getBytes());
      else
        return eval.wrap(eval.getArgString(-1, length).getBytes(eval.getArgString(0, length)));

    default:
      throw new ESException("Unknown object function");
    }
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (n != NEW)
      throw new ESException("Unknown object function");
 
    return (ESObject) create(eval, length);
  }

  private ESBase create(Call eval, int length) throws Throwable
  {
    ESBase value;
    if (length == 0)
      value = ESString.create("");
    else
      value = eval.getArg(0).toStr();

    return value.toObject();
  }

  private ESBase fromCharCode(Call eval, int length) throws Throwable
  {
    StringBuffer sbuf = new StringBuffer();

    for (int i = 0; i < length; i++) {
      int value = eval.getArg(i).toInt32() & 0xffff;

      sbuf.append((char) value);
    }

    return ESString.create(sbuf.toString());
  }

  private ESBase charAt(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return ESString.create("");

    int value = (int) eval.getArg(0).toNum();

    if (value < 0 || value >= string.length())
      return ESString.create("");
    else
      return ESString.create("" + (char) string.charAt(value));
  }

  private ESBase charCodeAt(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return ESNumber.NaN;

    int value = (int) eval.getArg(0).toNum();

    if (value < 0 || value >= string.length())
      return ESNumber.NaN;
    else
      return ESNumber.create(string.charAt(value));
  }

  private ESBase indexOf(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return ESNumber.create(-1);

    int pos = 0;
    if (length > 1)
      pos = (int) eval.getArg(1).toNum();

    ESString test = eval.getArg(0).toStr();

    return ESNumber.create(string.indexOf(test, pos));
  }

  private ESBase lastIndexOf(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return ESNumber.create(-1);

    int pos = string.length() + 1;
    if (length > 1)
      pos = (int) eval.getArg(1).toNum();

    ESString test = eval.getArg(0).toStr();

    return ESNumber.create(string.lastIndexOf(test, pos));
  }

  private String escapeRegexp(String arg)
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < arg.length(); i++) {
      int ch;
      switch ((ch = arg.charAt(i))) {
      case '\\': case '-': case '[': case ']': case '(': case ')': 
      case '$': case '^': case '|': case '?': case '*': case '{':
      case '}': case '.':
        cb.append('\\');
        cb.append((char) ch);
        break;

      default:
        cb.append((char) ch);
      }
    }

    return cb.toString();
  }

  private ESBase split(Call eval, int length) throws Throwable
  {
    Global resin = Global.getGlobalProto();
    ESString string = eval.getArg(-1).toStr();

    ESArray array = resin.createArray();

    if (length == 0) {
      array.setProperty(0, string);
      return array;
    }
    else if (eval.getArg(0) instanceof ESRegexp) {
      throw new UnsupportedOperationException();
      
      // splitter = (ESRegexp) eval.getArg(0);
    }
    else {
      String arg = eval.getArg(0).toString();

      String []values = string.toString().split(arg);

      for (int i = 0; i < values.length; i++) {
        array.setProperty(i, ESString.create(values[i]));
      }
      
      /*
      if (arg.length() == 1 && arg.charAt(0) == ' ') {
        splitter = new ESRegexp("\\s", "g");
      } else
        splitter = new ESRegexp(escapeRegexp(arg), "g");
      */
    }

    return array;
  }

  private ESBase substring(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return string;

    int start = (int) eval.getArg(0).toNum();
    int end = string.length();

    if (length > 1)
      end = (int) eval.getArg(1).toNum();

    if (start < 0)
      start = 0;
    if (end > string.length())
      end = string.length();
    if (start > end)
      return string.substring(end, start);
    else
      return string.substring(start, end);
  }

  private ESBase concat(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return string;

    CharBuffer cb = new CharBuffer();
    cb.append(string.toString());

    for (int i = 0; i < length; i++) {
      ESString next = eval.getArg(i).toStr();

      cb.append(next.toString());
    }

    return ESString.create(cb);
  }

  private ESBase match(Call eval, int length) throws Throwable
  {
    /*
    if (length == 0)
      return esNull;
    
    Global resin = Global.getGlobalProto();
    ESString string = eval.getArg(-1).toStr();

    ESBase arg = eval.getArg(0);
    ESRegexp regexp;

    if (arg instanceof ESRegexp)
      regexp = (ESRegexp) arg;
    else if (length > 1)
      regexp = new ESRegexp(arg.toStr(), eval.getArg(1).toStr());
    else
      regexp = new ESRegexp(arg.toStr(), ESString.NULL);

    IntArray results = new IntArray();

    resin.getRegexp().setRegexp(regexp);
    regexp.setLastIndex(0);
    if (! regexp.exec(string))
      return esNull;

    ESArray array = resin.createArray();

    if (! regexp.regexp.isGlobal()) {
      for (int i = 0; i < regexp.regexp.length(); i++) {
        array.setProperty(i, string.substring(regexp.regexp.getBegin(i),
                                              regexp.regexp.getEnd(i)));
      }

      return array;
    }

    int i = 0;
    do {
      array.setProperty(i, string.substring(regexp.regexp.getBegin(0), 
                                            regexp.regexp.getEnd(0)));
      i++;
    } while (regexp.exec(string));

    return array;
    */
    return esNull;
  }

  private void replaceFun(CharBuffer result, String pattern, 
                          ESRegexp regexp, ESBase fun)
    throws Throwable
  {
    /*
    Call call = Global.getGlobalProto().getCall();

    call.top = 1;
    call.setThis(regexp);
    for (int i = 0; i < regexp.regexp.length(); i++) {
      int begin = regexp.regexp.getBegin(i);
      int end = regexp.regexp.getEnd(i);
      call.setArg(i, ESString.create(pattern.substring(begin, end)));
    }

    ESBase value = fun.call(call, regexp.regexp.length());

    Global.getGlobalProto().freeCall(call);

    String string = value.toStr().toString();

    result.append(string);
    */
  }

  /*
  private void replaceString(CharBuffer result, String pattern, 
                             Pattern regexp, String replacement)
    throws Throwable
  {
    int len = replacement.length();

    for (int i = 0; i < len; i++) {
      char ch = replacement.charAt(i);
      
      if (ch == '$' && i + 1 < len) {
        i++;
        ch = replacement.charAt(i);

        if (ch >= '0' && ch <= '9') {
          int index = ch - '0';

          if (index < regexp.length()) {
            int begin = regexp.getBegin(index);
            int end = regexp.getEnd(index);
            result.append(pattern.substring(begin, end));
          }
        } else if (ch == '$')
          result.append('$');
        else if (ch == '&') {
          int begin = regexp.getBegin(0);
          int end = regexp.getEnd(0);
          result.append(pattern.substring(begin, end));
        } else if (ch == '+') {
          int begin = regexp.getBegin(regexp.length() - 1);
          int end = regexp.getEnd(regexp.length() - 1);
          result.append(pattern.substring(begin, end));
        } else if (ch == '`') {
          int begin = 0;
          int end = regexp.getBegin(0);
          result.append(pattern.substring(begin, end));
        } else if (ch == '\'') {
          int begin = regexp.getEnd(0);
          int end = pattern.length();
          result.append(pattern.substring(begin, end));
        } else {
          result.append('$');
          result.append(ch);
        }
      } else {
        result.append(ch);
      }
    }
  }
  */

  private ESBase replace(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length < 1)
      return string;

    Global resin = Global.getGlobalProto();
    ESBase arg = eval.getArg(0);
    ESRegexp regexp;

    if (arg instanceof ESRegexp)
      regexp = (ESRegexp) arg;
    else
      regexp = new ESRegexp(arg.toStr(), ESString.NULL);

    IntArray results = new IntArray();
    String pattern = string.toString();

    ESBase replace = null;
    String stringPattern = null;
    if (length > 1)
      replace = eval.getArg(1);

    /* XXX: convert to java.util.regex
    int last = 0;
    CharBuffer result = new CharBuffer();
    resin.getRegexp().setRegexp(regexp);
    if (regexp.regexp.isGlobal())
      regexp.setLastIndex(0);
    while (regexp.exec(string)) {
      result.append(pattern.substring(last, regexp.regexp.getBegin(0)));
      last = regexp.regexp.getEnd(0);

      if (replace instanceof ESClosure) {
        replaceFun(result, pattern, regexp, replace);
      } else {
        if (stringPattern == null)
          stringPattern = replace == null ? "" : replace.toString();

        replaceString(result, pattern, regexp.regexp, stringPattern);
      }

      if (! regexp.regexp.isGlobal())
        break;

    }
        result.append(pattern.substring(last));

    return ESString.create(result);
    */

    return null;
  }

  private ESBase search(Call eval, int length) throws Throwable
  {
    if (length == 0)
      return ESNumber.create(-1);
    
    return ESNumber.create(-1);

    /* XXX: convert to java.util.regex
    ESString string = eval.getArg(-1).toStr();

    ESBase arg = eval.getArg(0);
    ESRegexp regexp;

    if (arg instanceof ESRegexp)
      regexp = (ESRegexp) arg;
    else if (length > 1)
      regexp = new ESRegexp(arg.toStr(), eval.getArg(1).toStr());
    else
      regexp = new ESRegexp(arg.toStr(), ESString.NULL);

    Global.getGlobalProto().getRegexp().setRegexp(regexp);
    if (! regexp.exec(string, false))
      return ESNumber.create(-1);
    else
      return ESNumber.create(regexp.regexp.getBegin(0));
    */
  }

  private ESBase slice(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return string;

    int start = (int) eval.getArg(0).toNum();
    int end = string.length();

    if (length > 1)
      end = (int) eval.getArg(1).toNum();

    if (start < 0)
      start += string.length();
    if (end < 0)
      end += string.length();

    if (start < 0)
      start = 0;
    if (start > string.length())
      start = string.length();

    if (end < 0)
      end = 0;
    if (end > string.length())
      end = string.length();

    if (start <= end)
      return string.substring(start, end);
    else
      return ESString.NULL;
  }

  private ESBase substr(Call eval, int length) throws Throwable
  {
    ESString string = eval.getArg(-1).toStr();

    if (length == 0)
      return string;

    int start = (int) eval.getArg(0).toNum();
    int len = string.length();

    if (length > 1)
      len = (int) eval.getArg(1).toNum();

    if (start < 0)
      start += string.length();

    if (start < 0)
      start = 0;
    if (start > string.length())
      start = string.length();

    if (len <= 0)
      return ESString.NULL;

    int end = start + len;

    if (end > string.length())
      end = string.length();

    return string.substring(start, end);
  }

  private ESBase printf(Call eval, int length) throws Throwable
  {
    if (length == 0)
      return ESString.NULL;

    ESString format = eval.getArg(0).toStr();
    CharBuffer cb = new CharBuffer();

    Printf.printf(cb, format, eval, length);
    
    return ESString.create(cb.toString());
  }
}
