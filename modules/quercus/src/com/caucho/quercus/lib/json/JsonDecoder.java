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

package com.caucho.quercus.lib.json;

import com.caucho.quercus.env.*;
import com.caucho.util.L10N;

class JsonDecoder {
  private static final L10N L = new L10N(JsonDecoder.class);

  private StringValue _str;
  private int _len;
  private int _offset;

  private boolean _isAssociative;

  public Value jsonDecode(Env env,
                          StringValue s,
                          boolean assoc)
  {
    _str = s;
    _len = _str.length();
    _offset = 0;

    _isAssociative = assoc;

    Value val = jsonDecodeImpl(env, true);

    // Should now be at end of string or have only white spaces left.
    skipWhitespace();

    if (_offset < _len)
      return errorReturn(env, "expected no more input");

    return val;
  }

  /**
   * Entry point to decode a JSON value.
   *
   * @return decoded PHP value
   */
  private Value jsonDecodeImpl(Env env, boolean isTop)
  {
    skipWhitespace();

    if (_len <= _offset)
      return errorReturn(env);

    char ch = _str.charAt(_offset);

    switch (ch) {
    case '"': {
      _offset++;
      return decodeString(env, true);
    }

    case 'T':
    case 't': {
      if (isTop && _offset + 4 < _len)
        return decodeString(env, false);
      else if (_offset + 3 < _len) {
        char ch2 = _str.charAt(_offset + 1);
        char ch3 = _str.charAt(_offset + 2);
        char ch4 = _str.charAt(_offset + 3);

        if ((ch2 == 'r' || ch2 == 'R')
            && (ch3 == 'u' || ch3 == 'U')
            && (ch4 == 'e' || ch4 == 'E')) {
          if (_offset + 4 < _len
              && (ch = _str.charAt(_offset + 4)) != ','
              && ch != ']'
              && ch != '}'
              && ! Character.isWhitespace(ch))
            return errorReturn(env, "malformed 'true'");
          else {
            _offset += 4;
            return BooleanValue.TRUE;
          }
        }
      }

      if (isTop)
        return decodeString(env, false);
      else
        return errorReturn(env, "expected 'true'");
    }

    case 'F':
    case 'f': { // false
      if (isTop && _offset + 5 < _len)
        return decodeString(env, false);
      else if (_offset + 4 < _len) {
        char ch2 = _str.charAt(_offset + 1);
        char ch3 = _str.charAt(_offset + 2);
        char ch4 = _str.charAt(_offset + 3);
        char ch5 = _str.charAt(_offset + 4);

        if ((ch2 == 'a' || ch2 == 'A')
            && (ch3 == 'l' || ch3 == 'L')
            && (ch4 == 's' || ch4 == 'S')
            && (ch5 == 'e' || ch5 == 'E')) {
          if (_offset + 5 < _len
              && (ch = _str.charAt(_offset + 5)) != ','
              && ch != ']'
              && ch != '}'
              && ! Character.isWhitespace(ch))
            return errorReturn(env, "malformed 'false'");
          else {
            _offset += 5;
            return BooleanValue.FALSE;
          }
        }
      }

      if (isTop)
        return decodeString(env, false);
      else
        return errorReturn(env, "expected 'false'");
    }

    case 'N':
    case 'n': {
      if (isTop && _offset + 4 < _len)
        return decodeString(env, false);
      else if (_offset + 3 < _len) {
        char ch2 = _str.charAt(_offset + 1);
        char ch3 = _str.charAt(_offset + 2);
        char ch4 = _str.charAt(_offset + 3);

        if ((ch2 == 'u' || ch2 == 'U')
            && (ch3 == 'l' || ch3 == 'L')
            && (ch4 == 'l' || ch4 == 'L')) {
          if (_offset + 4 < _len
              && (ch = _str.charAt(_offset + 4)) != ','
              && ch != ']'
              && ch != '}'
              && ! Character.isWhitespace(ch))
            return errorReturn(env, "malformed 'null'");
          else {
            _offset += 4;
            return NullValue.NULL;
          }
        }
      }

      if (isTop)
        return decodeString(env, false);
      else
        return errorReturn(env, "expected 'null'");
    }

    case '[': { // ["foo", "bar", "baz"]
      return decodeArray(env);
    }

    case  '{': {
      return decodeObject(env);
    }

    case '-':
    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
      return decodeNumber(env);

    default:
      if (isTop)
        return decodeString(env, false);
      else
        return errorReturn(env);
    }
  }

  /**
   * Checks to see if there is a valid number per JSON Internet Draft.
   */
  private Value decodeNumber(Env env)
  {
    int startOffset = _offset;

    long value = 0;
    int sign = 1;

    char ch;

    // (-)?
    if ((ch = _str.charAt(_offset)) == '-') {
      sign = -1;

      _offset++;
    }

    if (_len <= _offset)
      return errorReturn(env, "expected 1-9");

    ch = _str.charAt(_offset++);

    // (0) | ([1-9] [0-9]*)
    if (ch == '0') {
    }
    else if ('1' <= ch && ch <= '9') {
      value = ch - '0';

      while (_offset < _len
             && '0' <= (ch = _str.charAt(_offset)) && ch <= '9') {
        _offset++;

        value = 10 * value + ch - '0';
      }
    }

    boolean isDouble = false;

    // ((decimalPoint) [0-9]+)?
    if (_offset < _len && (ch = _str.charAt(_offset)) == '.') {
      _offset++;

      isDouble = true;

      while (_offset < _len
             && '0' <= (ch = _str.charAt(_offset)) && ch <= '9') {
        _offset++;
      }
    }

    // ((e | E) (+ | -)? [0-9]+)
    if (_offset < _len && (ch = _str.charAt(_offset)) == 'e' || ch == 'E') {
      _offset++;

      isDouble = true;

      if (_offset < _len && (ch = _str.charAt(_offset)) == '+' || ch == '-') {
        _offset++;
      }

      while (_offset < _len
             && '0' <= (ch = _str.charAt(_offset)) && ch <= '9') {
        _offset++;
      }

      /*
      if (_offset < _len)
        return errorReturn(env,
                           L.l("expected 0-9 exponent at '{0}'", (char) ch));
      */
    }

    if (isDouble) {
      String strValue
        = _str.stringSubstring(startOffset, _offset);

      return DoubleValue.create(Double.parseDouble(strValue));
    }
    else
      return LongValue.create(sign * value);
  }

  /**
   * Returns a non-associative PHP array.
   */
  private Value decodeArray(Env env)
  {
    ArrayValueImpl array = new ArrayValueImpl();

    _offset++;

    while (true) {
      skipWhitespace();

      if (_offset >= _len)
        return errorReturn(env, "expected either ',' or ']'");

      if (_str.charAt(_offset) == ']') {
        _offset++;
        break;
      }

      array.append(jsonDecodeImpl(env, false));

      skipWhitespace();

      if (_offset >= _len)
        return errorReturn(env, "expected either ',' or ']'");

      char ch = _str.charAt(_offset++);

      if (ch == ',') {
      }
      else if (ch == ']')
        break;
      else
        return errorReturn(env, "expected either ',' or ']'");
    }

    return array;
  }

  private Value decodeObject(Env env)
  {
    if (_isAssociative)
      return decodeObjectToArray(env);
    else
      return decodeObjectToObject(env);
  }

  /**
   * Returns a PHP associative array of JSON object.
   */
  private Value decodeObjectToArray(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    _offset++;

    while (true) {
      skipWhitespace();

      if (_offset >= _len || _str.charAt(_offset) == '}') {
        _offset++;
        break;
      }

      Value name = jsonDecodeImpl(env, false);

      skipWhitespace();

      if (_offset >= _len || _str.charAt(_offset++) != ':')
        return errorReturn(env, "expected ':'");

      array.append(name, jsonDecodeImpl(env, false));

      skipWhitespace();

      char ch;

      if (_offset >= _len)
        return errorReturn(env, "expected either ',' or '}'");
      else if ((ch = _str.charAt(_offset++)) == ',') {
      }
      else if (ch == '}')
        break;
      else
        return errorReturn(env, "expected either ',' or '}'");
    }

    return array;
  }

  /**
   * Returns a PHP stdObject of JSON object.
   */
  private Value decodeObjectToObject(Env env)
  {
    ObjectValue object = env.createObject();

    _offset++;

    while (true) {
      skipWhitespace();

      if (_len <= _offset || _str.charAt(_offset) == '}') {
        _offset++;
        break;
      }

      Value name = jsonDecodeImpl(env, false);

      skipWhitespace();

      if (_len <= _offset || _str.charAt(_offset++) != ':')
        return errorReturn(env, "expected ':'");

      object.putField(env, name.toString(), jsonDecodeImpl(env, false));

      skipWhitespace();

      char ch;

      if (_offset >= _len)
        return errorReturn(env, "expected either ',' or '}'");
      else if ((ch = _str.charAt(_offset++)) == ',') {
      }
      else if (ch == '}')
        break;
      else
        return errorReturn(env, "expected either ',' or '}'");
    }

    return object;
  }

  /**
   * Returns a PHP string.
   */
  private Value decodeString(Env env, boolean isQuoted)
  {
    StringValue sb = env.createUnicodeBuilder();

    while (_offset < _len) {
      char ch = _str.charAt(_offset++);

      switch (ch) {

        // Escaped Characters
      case '\\':
        if (_offset >= _len)
          return errorReturn(env, "invalid escape character");

        ch = _str.charAt(_offset++);

        switch (ch) {
          case '"':
            sb.append('"');
            break;
          case '\\':
            sb.append('\\');
            break;
          case '/':
            sb.append('/');
            break;
          case 'b':
            sb.append('\b');
            break;
          case 'f':
            sb.append('\f');
            break;
          case 'n':
            sb.append('\n');
            break;
          case 'r':
            sb.append('\r');
            break;
          case 't':
            sb.append('\t');
            break;
          case 'u':
          case 'U':
            int hex = 0;

            for (int i = 0; _offset < _len && i < 4; i++) {
              hex = hex << 4;
              ch = _str.charAt(_offset++);

              if ('0' <= ch && ch <= '9')
                hex += ch - '0';
              else if (ch >= 'a' && ch <= 'f')
                hex += ch - 'a' + 10;
              else if (ch >= 'A' && ch <= 'F')
                hex += ch - 'A' + 10;
              else
                return errorReturn(env, "invalid escaped hex character");
            }

            if (hex < 0x80)
              sb.append((char)hex);
            else if (hex < 0x800) {
              sb.append((char) (0xc0 + (hex >> 6)));
              sb.append((char) (0x80 + (hex & 0x3f)));
            }
            else {
              sb.append((char) (0xe0 + (hex >> 12)));
              sb.append((char) (0x80 + ((hex >> 6) & 0x3f)));
              sb.append((char) (0x80 + (hex & 0x3f)));
            }
          }

        break;

        case '"':
          return sb;

        default:
          sb.append(ch);
      }
    }

    if (isQuoted)
      return errorReturn(env, "error decoding string");
    else
      return sb;
  }

  private Value errorReturn(Env env)
  {
    return errorReturn(env, null);
  }

  private Value errorReturn(Env env, String message)
  {
    int end = Math.min(_len, _offset + 1);

    String token = _str.substring(_offset, end).toString();

    if (message != null)
      env.warning(L.l("error parsing '{0}': {1}", token, message));
    else
      env.warning(L.l("error parsing '{0}'", token));

    return NullValue.NULL;
  }

  private void skipWhitespace()
  {
    while (_offset < _len) {
      char ch = _str.charAt(_offset);

      if (ch == ' '
          || ch == '\n'
          || ch == '\r'
          || ch == '\t') {
        _offset++;
      } else {
        break;
      }
    }
  }
}
