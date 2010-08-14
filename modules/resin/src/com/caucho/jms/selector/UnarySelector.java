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

import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The base selector.
 */
public class UnarySelector extends Selector  {
  static L10N L = new L10N(Selector.class);

  private int _token;
  private Selector _expr;

  UnarySelector(int token, Selector expr)
  {
    _token = token;
    _expr = expr;
  }

  boolean isBoolean()
  {
    switch (_token) {
    case SelectorParser.NOT:
    case SelectorParser.NULL:
      return true;
    }
    
    return false;
  }

  boolean isNumber()
  {
    switch (_token) {
    case '-':
    case '+':
      return true;
    }
    
    return false;
  }

  boolean isUnknown()
  {
    return false;
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  Object evaluate(Message message)
    throws JMSException
  {
    Object value = _expr.evaluate(message);
    
    switch (_token) {
    case SelectorParser.NOT:
      if (! (value instanceof Boolean))
        return NULL;
      else
        return toBoolean(! ((Boolean) value).booleanValue());
      
    case SelectorParser.NULL:
      return toBoolean(value == null);
      
    case '+':
      if (! (value instanceof Number))
        return NULL;
      else
        return value;
      
    case '-':
      if (! (value instanceof Number))
        return NULL;
      else if (isInteger(value))
        return new Long(- toLong(value));
      else
        return new Double(- ((Number) value).doubleValue());
      
    default:
      throw new JMSException("NOTONE");
    }
  }

  public String toString()
  {
    switch (_token) {
    case SelectorParser.NOT:
      return "not(" + _expr + ")";
    case SelectorParser.NULL:
      return "(" + _expr + " is null)";
    case '+':
      return "+ (" + _expr + ")";
    case '-':
      return "- (" + _expr + ")";
    default:
      return super.toString();
    }
  }
}
