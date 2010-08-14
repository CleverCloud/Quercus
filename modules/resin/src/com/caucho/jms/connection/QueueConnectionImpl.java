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

package com.caucho.jms.connection;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.XAQueueConnection;
import javax.jms.QueueSession;
import javax.jms.XAQueueSession;
import javax.jms.ServerSessionPool;

/**
 * A sample queue connection factory.
 */
public class QueueConnectionImpl extends ConnectionImpl
  implements XAQueueConnection
{
  /**
   * Create a new queue connection.
   */
  public QueueConnectionImpl(ConnectionFactoryImpl factory, boolean isXA)
  {
    super(factory, isXA);
  }
  
  /**
   * Create a new queue connection.
   */
  public QueueConnectionImpl(ConnectionFactoryImpl factory)
  {
    super(factory);
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
  createConnectionConsumer(Queue queue, String messageSelector,
                           ServerSessionPool sessionPool, int maxMessages)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new connection session.
   */
  public QueueSession createQueueSession(boolean transacted,
                                         int acknowledgeMode)
    throws JMSException
  {
    checkOpen();

    assignClientID();
    
    return new QueueSessionImpl(this, transacted, acknowledgeMode, isXA());
  }

  /**
   * Creates a new connection session.
   */
  public XAQueueSession createXAQueueSession()
    throws JMSException
  {
    checkOpen();

    assignClientID();
    
    return new QueueSessionImpl(this, true, 0, true);
  }
}
