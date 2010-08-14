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

import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The base selector.
 */
public class BooleanBinarySelector extends Selector  {
  private int _token;
  private Selector _left;
  private Selector _right;

  BooleanBinarySelector(int token, Selector left, Selector right)
    throws JMSException
  {
    _token = token;
    _left = left;
    _right = right;

    switch (_token) {
    case SelectorParser.LT:
    case SelectorParser.LE:
    case SelectorParser.GT:
    case SelectorParser.GE:
      if (! _left.isUnknown() && ! _left.isNumber())
        throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                               this));
      if (! _right.isUnknown() && ! _right.isNumber())
        throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                               this));
      break;
    case SelectorParser.EQ:
    case SelectorParser.NE:
      if (_left.isUnknown() || _right.isUnknown()) {
      }
      else if (_left.isNumber() != _right.isNumber()) {
        throw new InvalidSelectorException(L.l("'{0}' test must have matching types.",
                                               this));
      }
      break;
      
    case SelectorParser.AND:
    case SelectorParser.OR:
      if (! _left.isUnknown() && ! _left.isBoolean())
        throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                               this));
      if (! _right.isUnknown() && ! _right.isBoolean())
        throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                               this));
      break;
    }
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
    Object lvalue = _left.evaluate(message);
    Object rvalue = _right.evaluate(message);

    if (lvalue == null || rvalue == null)
      return NULL;

    switch (_token) {
    case SelectorParser.EQ:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(((Number) lvalue).doubleValue() ==
                         ((Number) rvalue).doubleValue());
      else
        return toBoolean(lvalue.equals(rvalue));
      
    case SelectorParser.NE:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(! (((Number) lvalue).doubleValue() ==
                            ((Number) rvalue).doubleValue()));
      else
        return toBoolean(! lvalue.equals(rvalue));
      
    case SelectorParser.LT:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(((Number) lvalue).doubleValue() <
                         ((Number) rvalue).doubleValue());
      else
        return Boolean.FALSE;
        
    case SelectorParser.LE:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(((Number) lvalue).doubleValue() <=
                         ((Number) rvalue).doubleValue());
      else
        return Boolean.FALSE;
        
    case SelectorParser.GT:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(((Number) lvalue).doubleValue() >
                         ((Number) rvalue).doubleValue());
      else
        return Boolean.FALSE;
        
    case SelectorParser.GE:
      if (lvalue instanceof Number && rvalue instanceof Number)
        return toBoolean(((Number) lvalue).doubleValue() >=
                         ((Number) rvalue).doubleValue());
      else
        return Boolean.FALSE;
        
    case SelectorParser.AND:
      if (! (lvalue instanceof Boolean))
        return NULL;
      else if (! ((Boolean) lvalue).booleanValue())
        return Boolean.FALSE;
      else if (! (rvalue instanceof Boolean))
        return NULL;
      else if (! ((Boolean) rvalue).booleanValue())
        return Boolean.FALSE;
      else
        return Boolean.TRUE;
        
    case SelectorParser.OR:
      if ((lvalue instanceof Boolean) && ((Boolean) lvalue).booleanValue())
        return Boolean.TRUE;
      else if ((rvalue instanceof Boolean) &&
               ((Boolean) rvalue).booleanValue())
        return Boolean.TRUE;
      else if (lvalue instanceof Boolean || rvalue instanceof Boolean)
        return Boolean.FALSE;
      else
        return NULL;
      
    default:
      throw new JMSException("NOTONE");
    }
  }

  public String toString()
  {
    switch (_token) {
    case SelectorParser.EQ:
      return _left + " = " + _right;
    case SelectorParser.NE:
      return _left + " <> " + _right;
    case SelectorParser.LT:
      return _left + " < " + _right;
    case SelectorParser.LE:
      return _left + " <= " + _right;
    case SelectorParser.GT:
      return _left + " > " + _right;
    case SelectorParser.GE:
      return _left + " >= " + _right;
    case SelectorParser.AND:
      return _left + " AND " + _right;
    case SelectorParser.OR:
      return _left + " OR " + _right;
    default:
      return super.toString();
    }
  }
}
