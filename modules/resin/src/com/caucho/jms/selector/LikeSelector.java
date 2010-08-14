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

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.regex.Pattern;

/**
 * The like selector.
 */
public class LikeSelector extends Selector  {
  private Selector _expr;
  
  private String _likeString;
  private Pattern _pattern;

  LikeSelector(Selector expr, String likeString, char escape)
    throws JMSException
  {
    _expr = expr;

    _likeString = likeString;

    CharBuffer cb = new CharBuffer();
    cb.append("^");

    for (int i = 0; i < likeString.length(); i++) {
      char ch = likeString.charAt(i);

      if (ch == escape && i + 1 < likeString.length()) {
        ch = likeString.charAt(i + 1);

        switch (ch) {
        case '.': case '[': case ']': case '(': case ')': case '|':
        case '{': case '}':
        case '+': case '*': case '?': case '\\': case '$': case '^':
          cb.append('\\');
          cb.append(ch);
          break;

        default:
          cb.append(ch);
        }

        i++;
        continue;
      }

      switch (ch) {
      case '_':
        cb.append(".");
        break;
      case '%':
        cb.append(".*");
        break;
      case '.': case '[': case ']': case '(': case ')': case '|':
      case '{': case '}':
      case '+': case '*': case '?': case '\\': case '$': case '^':
        cb.append('\\');
        cb.append(ch);
        break;

      default:
        cb.append(ch);
        break;
      }
    }
    
    cb.append("$");

    _pattern = Pattern.compile(cb.toString());
  }

  /**
   * Returns true since the value is a boolean.
   */
  boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns false, since the type is known.
   */
  boolean isUnknown()
  {
    return false;
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  public Object evaluate(Message message)
    throws JMSException
  {
    Object value = _expr.evaluate(message);

    if (value == null)
      return null;

    // jms607l
    if (! (value instanceof String))
      return Boolean.FALSE;

    String s = (String) value;

    return _pattern.matcher(s).find() ? Boolean.TRUE : Boolean.FALSE;
  }

  public String toString()
  {
    return _expr + " LIKE " + _likeString;
  }
}
