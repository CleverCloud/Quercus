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

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The selector to grab a property.
 */
public class SpecialIdentifierSelector extends Selector  {
  final static int JMS_DELIVERY_MODE = 1;
  final static int JMS_PRIORITY = 2;
  final static int JMS_MESSAGE_ID = 3;
  final static int JMS_TIMESTAMP = 4;
  final static int JMS_CORRELATION_ID = 5;
  final static int JMS_TYPE = 6;
  
  private int _type;

  SpecialIdentifierSelector(int type)
  {
    _type = type;
  }

  /**
   * Returns false, since it's a known type.
   */
  boolean isUnknown()
  {
    return false;
  }

  /**
   * Returns true for the numeric values.
   */
  boolean isNumber()
  {
    switch (_type) {
    case JMS_PRIORITY:
    case JMS_TIMESTAMP:
      return true;
    default:
      return false;
    }
  }

  /**
   * Returns true for the string values.
   */
  boolean isString()
  {
    switch (_type) {
    case JMS_MESSAGE_ID:
    case JMS_CORRELATION_ID:
    case JMS_TYPE:
    case JMS_DELIVERY_MODE:
      return true;
    default:
      return false;
    }
  }

  /**
   * Evaluate the message.  The boolean literal selector returns
   * the value of the boolean.
   */
  Object evaluate(Message message)
    throws JMSException
  {
    switch (_type) {
    case JMS_DELIVERY_MODE:
      if (message.getJMSDeliveryMode() == DeliveryMode.PERSISTENT)
        return "PERSISTENT";
      else
        return "NON_PERSISTENT";
    case JMS_PRIORITY:
      return new Integer(message.getJMSPriority());
    case JMS_MESSAGE_ID:
      return message.getJMSMessageID();
    case JMS_TIMESTAMP:
      return new Long(message.getJMSTimestamp());
    case JMS_CORRELATION_ID:
      return message.getJMSCorrelationID();
    case JMS_TYPE:
      return message.getJMSType();
    default:
      throw new UnsupportedOperationException();
    }
  }

  public String toString()
  {
    switch (_type) {
    case JMS_DELIVERY_MODE:
      return "JMSDeliveryMode";
    case JMS_PRIORITY:
      return "JMSPriority";
    case JMS_MESSAGE_ID:
      return "JMSMessageID";
    case JMS_TIMESTAMP:
      return "JMSTimestamp";
    case JMS_CORRELATION_ID:
      return "JMSCorrelationID";
    case JMS_TYPE:
      return "JMSType";
    default:
      return "Special[" + _type + "]";
    }
  }
}
