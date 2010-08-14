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

package com.caucho.jcr.svn;

import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Subversion input class.
 */
public class SubversionInput {
  private final L10N L = new L10N(SubversionInput.class);
  private final Logger log
    = Logger.getLogger(SubversionInput.class.getName());

  private ReadStream _is;
  private int _peek;

  public SubversionInput(ReadStream is)
  {
    _is = is;
  }

  /**
   * Reads a string.
   */
  public String readString()
    throws IOException
  {
    skipWhitespace();

    long length = readLong();

    expect(':');

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < length; i++) {
      sb.append((char) read());
    }

    return sb.toString();
  }

  /**
   * Reads a string literal.
   */
  public String readLiteral()
    throws IOException
  {
    skipWhitespace();

    StringBuilder sb = new StringBuilder();

    int ch;

    while (isStringChar((ch = read()))) {
      sb.append((char) ch);
    }

    _peek = ch;

    return sb.toString();
  }

  /**
   * Reads a long.
   */
  public long readLong()
    throws IOException
  {
    skipWhitespace();

    int sign = 1;
    long value = 0;

    int ch = read();

    if (ch == '-') {
      sign = -1;
      ch = read();
    }
    else if (ch == '+') {
      sign = -1;
      ch = read();
    }

    if (! ('0' <= ch && ch <= '9'))
      throw error(L.l("expected digit (0-9) at '{0}' (0x{1})",
                      String.valueOf((char) ch),
                      Integer.toHexString(ch)));

    for (; '0' <= ch && ch <= '9'; ch = read()) {
      value = 10 * value + ch - '0';
    }

    _peek = ch;

    return sign * value;
  }

  /**
   * Reads a s-exp
   */
  public Object readSexp()
    throws IOException
  {
    int ch;

    while ((ch = read()) >= 0) {
      switch (ch) {
      case ' ': case '\t': case '\r': case '\n':
        break;

      case '(':
        {
          ArrayList array = new ArrayList();

          Object value;

          while ((value = readSexp()) != null) {
            array.add(value);
          }

          expect(')');

          return array;
        }
      case ')':
        _peek = ch;
        return null;

      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        {
          _peek = ch;

          long value = readLong();

          ch = read();

          if (ch == ':') {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < value; i++)
              sb.append((char) read());
            return sb.toString();
          }
          else {
            _peek = ch;

            return new Long(value);
          }
        }

      default:
        if (isStringChar((char) ch)) {
          StringBuilder sb = new StringBuilder();

          sb.append((char) ch);
          while (isStringChar(ch = read())) {
            sb.append((char) ch);
          }

          _peek = ch;

          return sb.toString();
        }
        else
          throw error(L.l("Unexpected character"));
      }
    }

    return null;
  }
    
  
  /**
   * Skips whitespace
   */
  public boolean skipWhitespace()
    throws IOException
  {
    int ch;

    while (Character.isWhitespace(ch = read())) {
    }

    _peek = ch;

    return ch >= 0;
  }

  /**
   * Reads until an open brace.
   */
  public void expect(char expect)
    throws IOException
  {
    int ch;
    
    while ((ch = read()) >= 0) {
      if (ch == expect)
        return;
      else if (Character.isWhitespace(ch)) {
      }
      else
        throw error(L.l("Expected '{0}' at '{1}' (0x{2})",
                        String.valueOf((char) expect),
                        String.valueOf((char) ch),
                        Integer.toHexString(ch)));
    }
    
    throw error(L.l("Expected '{0}' at end of file",
                    String.valueOf((char) expect)));
  }

  private boolean isStringChar(int ch)
  {
    switch (ch) {
    case ' ': case '\t': case '\n': case '\r':
      return false;
    case -1:
      return false;
    case '(': case ')':
      return false;
    default:
      return true;
    }
  }

  private IOException error(String msg)
  {
    return new IOException(msg);
  }

  public int read()
    throws IOException
  {
    if (_peek > 0) {
      int peek = _peek;
      _peek = 0;
      return peek;
    }

    int ch = _is.read();

    if (ch >= 0)
      System.out.print((char) ch);
    
    return ch;
  }

  public void close()
  {
    ReadStream is = _is;
    _is = null;

    if (is != null) {
      is.close();
    }
  }
}
