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

package com.caucho.jms.queue;

import java.util.logging.*;

import javax.jms.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.*;

import com.caucho.config.ConfigException;
import com.caucho.jms.JmsRuntimeException;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.*;

/**
 * Wrapper around a JMS destination
 */
public class JmsBlockingQueue extends java.util.AbstractQueue
  implements BlockingQueue
{
  private static final L10N L = new L10N(JmsBlockingQueue.class);
  private static final Logger log
    = Logger.getLogger(JmsBlockingQueue.class.getName());

  private static long _idRandom;
  private static long _idCount;
  
  // queue api
  private ConnectionFactory _factory;
  private Connection _conn;

  private Destination _destination;
  
  private Session _writeSession;
  private Session _readSession;

  private MessageProducer _producer;
  private MessageConsumer _consumer;

  private Object _readLock = new Object();
  private Object _writeLock = new Object();

  public JmsBlockingQueue()
  {
  }

  public JmsBlockingQueue(ConnectionFactory factory, Destination destination)
  {
    _factory = factory;
    _destination = destination;
  }

  public void setFactory(ConnectionFactory factory)
  {
    _factory = factory;
  }

  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  @PostConstruct
  public void init()
  {
    if (_factory == null)
      throw new ConfigException("JmsBlockingQueue requires a 'factory' with the JMS ConnectionFactory");
    
    if (_destination == null)
      throw new ConfigException("JmsBlockingQueue requires a 'destination' with the JMS Destination");
  }
  
  //
  // BlockingQueue api
  //

  public int size()
  {
    return 0;
  }
  
  public boolean contains(Object obj)
  {
    return false;
  }
  
  public boolean remove(Object obj)
  {
    return false;
  }

  public Iterator iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds the item to the queue, waiting if necessary
   */
  public boolean offer(Object value, long timeout, TimeUnit unit)
  {
    try {
      synchronized (_writeLock) {
        MessageProducer producer = getWriteProducer();

        Message msg;

        if (value instanceof Message)
          msg = (Message) value;
        else
          msg = _writeSession.createObjectMessage((Serializable) value);

        producer.send(_destination, msg, 0, 0, Integer.MAX_VALUE);

        return true;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JmsRuntimeException(e);
    }
  }

  public Object poll(long timeout, TimeUnit unit)
  {
    try {
      synchronized (_readLock) {
        MessageConsumer consumer = getReadConsumer();

        long msTimeout = unit.toMillis(timeout);

        Message msg = consumer.receive(msTimeout);

        if (msg instanceof ObjectMessage) {
          return ((ObjectMessage) msg).getObject();
        }
        else if (msg instanceof TextMessage) {
          return ((TextMessage) msg).getText();
        }
        else if (msg == null)
          return null;
        else
          throw new JmsRuntimeException(L.l("'{0}' is an unsupported message for the BlockingQueue API.",
                                            msg));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JmsRuntimeException(e);
    }
  }

  public boolean offer(Object value)
  {
    return offer(value, 0, TimeUnit.SECONDS);
  }

  public void put(Object value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public int remainingCapacity()
  {
    return Integer.MAX_VALUE;
  }

  public Object peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Object poll()
  {
    return poll(0, TimeUnit.MILLISECONDS);
  }

  public Object take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public int drainTo(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  public int drainTo(Collection c, int max)
  {
    throw new UnsupportedOperationException();
  }

  protected MessageProducer getWriteProducer()
    throws JMSException
  {
    synchronized (this) {
      if (_conn == null) {
        _conn = _factory.createConnection();
        _conn.start();
      }
    
      if (_writeSession == null) {
        _writeSession = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      }

      if (_producer == null) {
        _producer = _writeSession.createProducer(_destination);
      }
    }

    return _producer;
  }

  protected MessageConsumer getReadConsumer()
    throws JMSException
  {
    synchronized (this) {
      if (_conn == null) {
        _conn = _factory.createConnection();
        _conn.start();
      }
    
      if (_readSession == null) {
        _readSession = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      }
    
      if (_consumer == null) {
        _consumer = _readSession.createConsumer(_destination);
      }
    }

    return _consumer;
  }

  public void close()
  {
    MessageConsumer consumer = _consumer;
    _consumer = null;

    MessageProducer producer = _producer;
    _producer = null;

    Session readSession = _readSession;
    _readSession = null;

    Session writeSession = _writeSession;
    _writeSession = null;

    Connection conn = _conn;
    _conn = null;

    try {
      if (consumer != null)
        consumer.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (producer != null)
        producer.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (readSession != null)
        readSession.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (writeSession != null)
        writeSession.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    try {
      if (conn != null)
        conn.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _destination + "]";
  }
}

