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

package com.caucho.bayeux;

import java.util.ArrayList;
import java.util.HashMap;
import com.caucho.util.L10N;

class JsonDecoder {
  private static final L10N L = new L10N(JsonDecoder.class);

  private String _string;
  private int _len;
  private int _offset;

  private JsonDecoder(String string)
  {
    _string = string;
    _len = _string.length();
    _offset = 0;
  }

  public static JsonObject decode(String string)
    throws JsonDecodingException
  {
    return new JsonDecoder(string).decode();
  }
  
  private JsonObject decode()
    throws JsonDecodingException
  {
    JsonObject object = decodeObject();

    // Should now be at end of string or have only white spaces left.
    if (skipWhitespace() >= 0)
      error("expected no more input");

    return object;
  }

  /**
   * Entry point to decode a JSON value.
   *
   * @return decoded PHP value 
   */
  private JsonObject decodeObject()
    throws JsonDecodingException
  {
    for (int ch = skipWhitespace(); ch >= 0; ch = read()) {

      switch (ch) {
        case '"':
          return decodeString();

        case 't':
          if (read() == 'r' &&
              read() == 'u' &&
              read() == 'e')
            return JsonBoolean.TRUE;
          else
            error("expected 'true'");

        case 'f':
          if (read() == 'a' &&
              read() == 'l' &&
              read() == 's' &&
              read() == 'e')
            return JsonBoolean.FALSE;
          else
            error("expected 'false'");

        case 'n':
          if (read() == 'u' &&
              read() == 'l' &&
              read() == 'l')
            return JsonNull.NULL;
          else
            error("expected 'null'");

        case '[':
          return decodeArray();

        case '{':
          return decodeMap();

        default:
          if (ch == '-' || ('0' <= ch && ch <= '9')) {
            unread();

            return decodeNumber();
          }
          else
            error();
      }
    }

    error();

    return null;
  }

  /**
   * Checks to see if there is a valid number per JSON Internet Draft.
   */
  private JsonNumber decodeNumber()
    throws JsonDecodingException
  {
    StringBuilder sb = new StringBuilder();

    int ch = read();

    // (-)?
    if (ch == '-') {
      sb.append((char)ch);
      ch = read();
    }

    // (0) | ([1-9] [0-9]+)
    if (ch >= 0) {
      if (ch == '0') {
        sb.append((char)ch);
        ch = read();
      }
      else if ('1' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();

        while ('0' <= ch && ch <= '9') {
          sb.append((char)ch);
          ch = read();
        }
      }
      else
        error("expected 1-9");
    }

    int integerEnd = sb.length();

    // ((decimalPoint) [0-9]+)?
    if (ch == '.') {
      sb.append((char)ch);
      ch = read();

      while ('0' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();
      }
    }

    // ((e | E) (+ | -)? [0-9]+)
    if (ch == 'e' || ch == 'E') {
      sb.append((char)ch);
      ch = read();

      if (ch == '+' || ch == '-') {
        sb.append((char)ch);
        ch = read();
      }

      if ('0' <= ch && ch <= '9') {
        sb.append((char)ch);
        ch = read();

        while ('0' <= ch && ch <= '9') {
          sb.append((char)ch);
          ch = read();
        }
      }
      else
        error("expected 0-9 exponent");
    }

    unread();

    if (integerEnd != sb.length())
      return JsonDouble.valueOf(Double.parseDouble(sb.toString()));
    else
      return JsonLong.valueOf(Long.parseLong(sb.toString()));
  }

  /**
   * Returns an array list.
   */
  private JsonArray decodeArray()
    throws JsonDecodingException
  {
    JsonArray array = new JsonArray();

    while (true) {
      int ch = skipWhitespace();

      if (ch == ']')
        break;

      unread();

      array.add(decodeObject());

      ch = skipWhitespace();

      if (ch == ',') {
      }
      else if (ch == ']')
        break;
      else
        error("expected either ',' or ']'");
    }

    return array;
  }

  /**
   * Returns a map version of a JSON object.
   */
  private JsonMap decodeMap()
    throws JsonDecodingException
  {
    JsonMap map = new JsonMap();

    while (true) {
      int ch = skipWhitespace();

      if (ch == '}')
        break;

      unread();

      JsonObject name = decodeObject();

      if (name == null)
        error("field names must not be null");

      if (! (name instanceof JsonString))
        error("field names must be strings");

      ch = skipWhitespace();

      if (ch != ':')
        error("expected ':'");

      map.put(name.toString(), decodeObject());

      ch = skipWhitespace();

      if (ch == ',') {
      }
      else if (ch == '}')
        break;
      else
        error("expected either ',' or '}'");
    }

    return map;
  }

  /**
   * Returns a string.
   */
  private JsonString decodeString()
    throws JsonDecodingException
  {
    StringBuilder sb = new StringBuilder();

    for (int ch = read(); ch >= 0; ch = read()) {

      switch (ch) {
        // Escaped Characters
        case '\\':
          ch = read();
          if (ch < 0)
            error("invalid escape character");

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

              for (int i = 0; i < 4; i++) {
                hex = hex << 4;
                ch = read();

                if ('0' <= ch && ch <= '9')
                  hex += ch - '0';
                else if (ch >= 'a' && ch <= 'f')
                  hex += ch - 'a' + 10;
                else if (ch >= 'A' && ch <= 'F')
                  hex += ch - 'A' + 10;
                else
                  error("invalid escaped hex character");
              }

              sb.append((char)hex);

          }
          break;

        case '"':
          return JsonString.valueOf(sb.toString());

        default:
          sb.append((char)ch);
      }
    }

    error("error decoding string");

    return null;
  }

  private void error()
    throws JsonDecodingException
  {
    error(null);
  }

  private void error(String message)
    throws JsonDecodingException
  {
    int start;
    int end;

    if (_offset < _len) {
      start = _offset - 1;
      end = _offset;
    }
    else {
      start = _len - 1;
      end = _len;
    }

    String token = _string.substring(start, end).toString();

    if (message != null)
      throw new JsonDecodingException(L.l("error parsing '{0}': {1}", 
                                          token, message));
    else
      throw new JsonDecodingException(L.l("error parsing '{0}'", token));
  }

  private void unread()
  {
    if (_offset > 0)
      _offset--;
  }

  private int peek(int index)
  {
    if (0 <= index && index < _len)
      return _string.charAt(index);
    else
      return -1;
  }

  private int read()
  {
    if (_offset < _len)
      return _string.charAt(_offset++);
    else
      return -1;
  }

  private int skipWhitespace()
  {
    int ch = read();
    for (; ch >= 0; ch = read()) {
      if (ch != ' ' &&
          ch != '\n' &&
          ch != '\r' &&
          ch != '\t')
        break;
    }

    return ch;
  }
}
