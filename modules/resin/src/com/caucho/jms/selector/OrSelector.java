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
public class OrSelector extends Selector  {
  private Selector _left;
  private Selector _right;

  OrSelector(Selector left, Selector right)
    throws JMSException
  {
    _left = left;
    _right = right;

    if (! _left.isUnknown() && ! _left.isBoolean())
      throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                             this));
    if (! _right.isUnknown() && ! _right.isBoolean())
      throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                             this));
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
    
    if ((lvalue instanceof Boolean) && ((Boolean) lvalue).booleanValue())
      return Boolean.TRUE;

    Object rvalue = _right.evaluate(message);

    if (rvalue == null)
      return NULL;

    if ((rvalue instanceof Boolean) && ((Boolean) rvalue).booleanValue())
      return Boolean.TRUE;
    else if (lvalue instanceof Boolean || rvalue instanceof Boolean)
      return Boolean.FALSE;
    else
      return NULL;
  }

  public String toString()
  {
    return _left + " OR " + _right;
  }
}
