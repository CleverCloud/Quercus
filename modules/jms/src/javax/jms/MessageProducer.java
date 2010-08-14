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

package javax.jms;

/**
 * The main destination.
 */
public interface MessageProducer {
  public void setDisableMessageID(boolean value)
    throws JMSException;
  
  public boolean getDisableMessageID()
    throws JMSException;
  
  public void setDisableMessageTimestamp(boolean value)
    throws JMSException;
  
  public boolean getDisableMessageTimestamp()
    throws JMSException;
  
  public void setDeliveryMode(int deliveryMode)
    throws JMSException;
  
  public int getDeliveryMode()
    throws JMSException;
  
  public void setPriority(int defaultPriority)
    throws JMSException;
  
  public int getPriority()
    throws JMSException;
  
  public void setTimeToLive(long timeToLive)
    throws JMSException;
  
  public long getTimeToLive()
    throws JMSException;

  /**
   * @since 1.1
   */
  public Destination getDestination()
    throws JMSException;

  /**
   * @since 1.1
   */
  public void send(Message message)
    throws JMSException;

  /**
   * @since 1.1
   */
  public void send(Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException;

  /**
   * @since 1.1
   */
  public void send(Destination destination,
                   Message message)
    throws JMSException;

  /**
   * @since 1.1
   */
  public void send(Destination destination,
                   Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException;
  
  public void close()
    throws JMSException;
}
