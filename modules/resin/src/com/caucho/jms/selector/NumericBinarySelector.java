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
public class NumericBinarySelector extends Selector  {
  static L10N L = new L10N(Selector.class);

  private int _token;
  private Selector _left;
  private Selector _right;

  NumericBinarySelector(int token, Selector left, Selector right)
  {
    _token = token;
    _left = left;
    _right = right;
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  Object evaluate(Message message)
    throws JMSException
  {
    Object lobj = _left.evaluate(message);
    Object robj = _right.evaluate(message);

    if (! (lobj instanceof Number) || ! (robj instanceof Number))
      return NULL;

    if (isInteger(lobj) && isInteger(robj)) {
      long lvalue = toLong(lobj);
      long rvalue = toLong(robj);

      switch (_token) {
      case '+':
        return new Long(lvalue + rvalue);
      
      case '-':
        return new Long(lvalue - rvalue);
      
      case '*':
        return new Long(lvalue * rvalue);
      
      case '/':
        return new Long(lvalue / rvalue);
      
      default:
        throw new RuntimeException("Unknown expression");
      }
    }
    else {
      double lvalue = ((Number) lobj).doubleValue();
      double rvalue = ((Number) robj).doubleValue();

      switch (_token) {
      case '+':
        return new Double(lvalue + rvalue);
      
      case '-':
        return new Double(lvalue - rvalue);
      
      case '*':
        return new Double(lvalue * rvalue);
      
      case '/':
        return new Double(lvalue / rvalue);
      
      default:
        throw new RuntimeException("Unknown expression");
      }
    }
  }
}
