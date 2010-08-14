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
public class BetweenSelector extends Selector  {
  private Selector _left;
  private Selector _low;
  private Selector _high;

  BetweenSelector(Selector left, Selector low, Selector high)
    throws JMSException
  {
    _left = left;
    _low = low;
    _high = high;

    if (! _left.isUnknown() && ! _left.isNumber())
      throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                             this));
    if (! _low.isUnknown() && ! _low.isNumber())
      throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                             this));
    if (! _high.isUnknown() && ! _high.isNumber())
      throw new InvalidSelectorException(L.l("'{0}' must have a numeric value for comparison.",
                                             this));
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  Object evaluate(Message message)
    throws JMSException
  {
    Object lobj = _left.evaluate(message);

    if (! (lobj instanceof Number))
      return NULL;

    Object lowObj = _low.evaluate(message);
    Object highObj = _high.evaluate(message);

    if (! (lowObj instanceof Number) || ! (highObj instanceof Number))
      return NULL;

    if (isInteger(lobj) && isInteger(lowObj) && isInteger(highObj)) {
      long lvalue = toLong(lobj);
      long low = toLong(lowObj);
      long high = toLong(highObj);
      
      return toBoolean(low <= lvalue && lvalue <= high);
    }
    else {
      double lvalue = toDouble(lobj);
      double low = toDouble(lowObj);
      double high = toDouble(highObj);
      
      return toBoolean(low <= lvalue && lvalue <= high);
    }
  }
}
