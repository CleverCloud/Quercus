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

import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;

import com.caucho.jms.queue.AbstractQueue;
import com.caucho.util.L10N;

/**
 * A basic message consumer.
 */
public class QueueReceiverImpl extends MessageConsumerImpl
  implements QueueReceiver
{
  private static final Logger log
    = Logger.getLogger(QueueReceiverImpl.class.getName());
  private static final L10N L = new L10N(QueueReceiverImpl.class);
  
  QueueReceiverImpl(JmsSession session,
                    AbstractQueue queue,
                    String messageSelector)
    throws JMSException
  {
    super(session, queue, messageSelector, false);
  }
  
  QueueReceiverImpl(JmsSession session,
      AbstractQueue queue,
      String messageSelector,
      boolean noLocal)
    throws JMSException
  {
    super(session, queue, messageSelector, noLocal);
  }

  public Queue getQueue()
    throws JMSException
  {
    return (Queue) getDestination();
  }
}

