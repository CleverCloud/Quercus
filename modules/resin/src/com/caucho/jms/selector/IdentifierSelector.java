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

import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The selector to grab a property.
 */
public class IdentifierSelector extends Selector  {
  static L10N L = new L10N(Selector.class);

  static final IntMap _specialSelectors;

  private String _name;

  private IdentifierSelector(String name)
  {
    _name = name;
  }

  static Selector create(String name)
  {
    int type = _specialSelectors.get(name.toLowerCase());

    if (type >= 0)
      return new SpecialIdentifierSelector(type);

    return new IdentifierSelector(name);
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  Object evaluate(Message message)
    throws JMSException
  {
    return message.getObjectProperty(_name);
  }

  public String toString()
  {
    return _name;
  }

  static {
    _specialSelectors = new IntMap();

    _specialSelectors.put("jmsdeliverymode", SpecialIdentifierSelector.JMS_DELIVERY_MODE);
    _specialSelectors.put("jmspriority", SpecialIdentifierSelector.JMS_PRIORITY);
    _specialSelectors.put("jmsmessageid", SpecialIdentifierSelector.JMS_MESSAGE_ID);
    _specialSelectors.put("jmstimestamp", SpecialIdentifierSelector.JMS_TIMESTAMP);
    _specialSelectors.put("jmscorrelationid", SpecialIdentifierSelector.JMS_CORRELATION_ID);
    _specialSelectors.put("jmstype", SpecialIdentifierSelector.JMS_TYPE);
  }
}
