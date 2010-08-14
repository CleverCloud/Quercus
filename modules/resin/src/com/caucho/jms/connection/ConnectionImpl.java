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

import com.caucho.util.L10N;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.*;
import com.caucho.jms.memory.*;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A connection.
 */
public class ConnectionImpl implements XAConnection
{
  static final Logger log
    = Logger.getLogger(ConnectionImpl.class.getName());
  static final L10N L = new L10N(ConnectionImpl.class);

  private static int _clientIdGenerator;

  private ConnectionFactoryImpl _factory;
  private boolean _isXA;
  
  private String _clientId;
  private boolean _isClientIdSet;
  
  private ExceptionListener _exceptionListener;

  private ArrayList<JmsSession> _sessions = new ArrayList<JmsSession>();

  private HashMap<String,TopicSubscriber> _durableSubscriberMap
    = new HashMap<String,TopicSubscriber>();

  private HashMap<String,Queue> _dynamicQueueMap
    = new HashMap<String,Queue>();

  private HashMap<String,Topic> _dynamicTopicMap
    = new HashMap<String,Topic>();

  private final Lifecycle _lifecycle = new Lifecycle(log);

  public ConnectionImpl(ConnectionFactoryImpl factory, boolean isXA)
  {
    this(factory);

    _isXA = isXA;
  }

  public ConnectionImpl(ConnectionFactoryImpl factory)
  {
    _factory = factory;

    Environment.addCloseListener(this);
  }

  /**
   * Returns true for an XA connection.
   */
  public boolean isXA()
  {
    return _isXA;
  }

  /**
   * Returns the connection's client identifier.
   */
  public String getClientID()
    throws JMSException
  {
    checkOpen();
    
    return _clientId;
  }

  /**
   * Sets the connections client identifier.
   *
   * @param the new client identifier.
   */
  public void setClientID(String clientId)
    throws JMSException
  {
    checkOpen();
    
    if (_isClientIdSet)
      throw new IllegalStateException(L.l("Can't set client id '{0}' after the connection has been used.",
                                          clientId));

    ConnectionImpl oldConn = _factory.findByClientID(clientId);

    if (oldConn != null)
      throw new InvalidClientIDException(L.l("'{0}' is a duplicate client id.",
                                             clientId));
    
    _clientId = clientId;
    _isClientIdSet = true;
    _lifecycle.setName(toString());
    _lifecycle.setLevel(Level.FINER);
  }

  /**
   * Returns the connection factory.
   */
  public ConnectionFactoryImpl getConnectionFactory()
  {
    return _factory;
  }

  /**
   * Returns the connection's exception listener.
   */
  public ExceptionListener getExceptionListener()
    throws JMSException
  {
    checkOpen();
    
    return _exceptionListener;
  }

  /**
   * Returns the connection's exception listener.
   */
  public void setExceptionListener(ExceptionListener listener)
    throws JMSException
  {
    checkOpen();

    assignClientID();
    
    _exceptionListener = listener;
  }

  /**
   * Returns the connection's metadata.
   */
  public ConnectionMetaData getMetaData()
    throws JMSException
  {
    checkOpen();
    
    return new ConnectionMetaDataImpl();
  }

  /**
   * Start (or restart) a connection.
   */
  public void start()
    throws JMSException
  {
    checkOpen();
    assignClientID();

    if (! _lifecycle.toActive())
      return;
    
    synchronized (_sessions) {
      for (int i = 0; i < _sessions.size(); i++) {
        _sessions.get(i).start();
      }
    }
  }

  /**
   * Stops the connection temporarily.
   */
  public void stop()
    throws JMSException
  {
    checkOpen();

    if (! _lifecycle.toStopping())
      return;
    
    try {
      assignClientID();

      synchronized (_sessions) {
        for (int i = 0; i < _sessions.size(); i++) {
          try {
            _sessions.get(i).stop();
          } catch (Exception e) {
            log.log(Level.FINE, e.toString(), e);
          }
        }
      }
    } finally {
      _lifecycle.toStop();
    }
  }

  /**
   * Returns true if the connection is started.
   */
  boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns true if the connection is stopping.
   */
  boolean isStopping()
  {
    return _lifecycle.isStopping();
  }

  /**
   * Creates a new connection session.
   */
  public Session createSession(boolean transacted, int acknowledgeMode)
    throws JMSException
  {
    checkOpen();
    
    assignClientID();
    
    return new JmsSession(this, transacted, acknowledgeMode, isXA());
  }

  /**
   * Creates a new connection session.
   */
  public XASession createXASession()
    throws JMSException
  {
    checkOpen();
    
    assignClientID();
    
    return new JmsSession(this, true, 0, true);
  }

  /**
   * Adds a session.
   */
  protected void addSession(JmsSession session)
  {
    _sessions.add(session);
    
    if (_lifecycle.isActive())
      session.start();
  }

  /**
   * Removes a session.
   */
  void removeSession(JmsSession session)
  {
    _sessions.remove(session);
  }

  /**
   * Creates a dynamic queue.
   */
  Queue createQueue(String name)
  {
    Queue queue = _dynamicQueueMap.get(name);

    if (queue != null)
      return queue;
    
    MemoryQueue memoryQueue = new MemoryQueue();
    memoryQueue.setName(name);
    _dynamicQueueMap.put(name, memoryQueue);

    return memoryQueue;
  }

  /**
   * Creates a dynamic topic.
   */
  Topic createTopic(String name)
  {
    Topic topic = _dynamicTopicMap.get(name);

    if (topic != null)
      return topic;
    
    MemoryTopic memoryTopic = new MemoryTopic();
    memoryTopic.setName(name);
    _dynamicTopicMap.put(name, memoryTopic);

    return memoryTopic;
  }

  /**
   * Gets a durable subscriber.
   */
  TopicSubscriber getDurableSubscriber(String name)
  {
    return _durableSubscriberMap.get(name);
  }

  /**
   * Adds a durable subscriber.
   */
  TopicSubscriber putDurableSubscriber(String name, TopicSubscriber subscriber)
  {
    return _durableSubscriberMap.put(name, subscriber);
  }

  /**
   * Removes a durable subscriber.
   */
  TopicSubscriber removeDurableSubscriber(String name)
  {
    return _durableSubscriberMap.remove(name);
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
    createConnectionConsumer(Destination destination,
                             String messageSelector,
                             ServerSessionPool sessionPool,
                             int maxMessages)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new consumer (optional)
   */
  public ConnectionConsumer
    createDurableConnectionConsumer(Topic topic, String name,
                                    String messageSelector,
                                    ServerSessionPool sessionPool,
                                    int maxMessages)
    throws JMSException
  {
    checkOpen();
    
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the connection.
   */
  public void close()
    throws JMSException
  {
    if (_lifecycle.isDestroyed())
      return;
    
    stop();

    if (! _lifecycle.toDestroy())
      return;

    _factory.removeConnection(this);

    ArrayList<JmsSession> sessions;

    synchronized (_sessions) {
      sessions = new ArrayList<JmsSession>(_sessions);
      _sessions.clear();
    }
    
    for (int i = 0; i < sessions.size(); i++) {
      try {
        sessions.get(i).close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Checks that the session is open.
   */
  protected void checkOpen()
    throws IllegalStateException
  {
    if (_lifecycle.isDestroyed())
      throw new IllegalStateException(L.l("connection is closed"));
  }

  /**
   * Assigns a random client id.
   *
   * XXX: possibly wrong, i.e. shouldn't assign, for durable subscriptions
   */
  protected void assignClientID()
  {
    if (_clientId == null)
      _clientId = "resin-temp-" + _clientIdGenerator++;
    _isClientIdSet = true;

    _lifecycle.setName(toString());
  }

  public String toString()
  {
    return "JmsConnection[" + _clientId + "]";
  }
}
