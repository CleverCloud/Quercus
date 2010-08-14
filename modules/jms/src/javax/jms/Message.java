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

package javax.jms;

import java.util.Enumeration;

/**
 * The main message.
 */
public interface Message {
  public static int DEFAULT_DELIVERY_MODE = DeliveryMode.PERSISTENT;
  public static int DEFAULT_PRIORITY = 4;
  public static long DEFAULT_TIME_TO_LIVE = 0;

  public String getJMSMessageID()
    throws JMSException;

  public void setJMSMessageID(String id)
    throws JMSException;

  public long getJMSTimestamp()
    throws JMSException;

  public void setJMSTimestamp(long timestamp)
    throws JMSException;

  public byte []getJMSCorrelationIDAsBytes()
    throws JMSException;

  public void setJMSCorrelationIDAsBytes(byte []value)
    throws JMSException;

  public String getJMSCorrelationID()
    throws JMSException;

  public void setJMSCorrelationID(String id)
    throws JMSException;

  public Destination getJMSReplyTo()
    throws JMSException;

  public void setJMSReplyTo(Destination replyTo)
    throws JMSException;

  public Destination getJMSDestination()
    throws JMSException;

  public void setJMSDestination(Destination replyTo)
    throws JMSException;

  public int getJMSDeliveryMode()
    throws JMSException;

  public void setJMSDeliveryMode(int deliveryMode)
    throws JMSException;

  public boolean getJMSRedelivered()
    throws JMSException;

  public void setJMSRedelivered(boolean isRedelivered)
    throws JMSException;

  public String getJMSType()
    throws JMSException;

  public void setJMSType(String type)
    throws JMSException;

  public long getJMSExpiration()
    throws JMSException;

  public void setJMSExpiration(long expiration)
    throws JMSException;

  public int getJMSPriority()
    throws JMSException;

  public void setJMSPriority(int expiration)
    throws JMSException;

  public void clearProperties()
    throws JMSException;

  public boolean propertyExists(String name)
    throws JMSException;

  public boolean getBooleanProperty(String name)
    throws JMSException;

  public byte getByteProperty(String name)
    throws JMSException;

  public short getShortProperty(String name)
    throws JMSException;

  public int getIntProperty(String name)
    throws JMSException;

  public long getLongProperty(String name)
    throws JMSException;

  public float getFloatProperty(String name)
    throws JMSException;

  public double getDoubleProperty(String name)
    throws JMSException;

  public String getStringProperty(String name)
    throws JMSException;

  public Object getObjectProperty(String name)
    throws JMSException;

  public Enumeration getPropertyNames()
    throws JMSException;

  public void setBooleanProperty(String name, boolean value)
    throws JMSException;

  public void setByteProperty(String name, byte value)
    throws JMSException;

  public void setShortProperty(String name, short value)
    throws JMSException;

  public void setIntProperty(String name, int value)
    throws JMSException;

  public void setLongProperty(String name, long value)
    throws JMSException;

  public void setFloatProperty(String name, float value)
    throws JMSException;

  public void setDoubleProperty(String name, double value)
    throws JMSException;

  public void setStringProperty(String name, String value)
    throws JMSException;

  public void setObjectProperty(String name, Object value)
    throws JMSException;

  public void acknowledge()
    throws JMSException;

  public void clearBody()
    throws JMSException;
}
