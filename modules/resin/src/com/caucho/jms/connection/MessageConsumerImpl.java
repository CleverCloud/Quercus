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

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.message.ObjectMessageImpl;
import com.caucho.jms.message.TextMessageImpl;
import com.caucho.jms.queue.AbstractDestination;
import com.caucho.jms.queue.AbstractQueue;
import com.caucho.jms.queue.EntryCallback;
import com.caucho.jms.queue.MessageCallback;
import com.caucho.jms.queue.MessageException;
import com.caucho.jms.queue.QueueEntry;
import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * A basic message consumer.
 */
public class MessageConsumerImpl<E> implements MessageConsumer
{
  static final Logger log
    = Logger.getLogger(MessageConsumerImpl.class.getName());
  static final L10N L = new L10N(MessageConsumerImpl.class);

  private final Object _consumerLock = new Object();

  protected final JmsSession _session;

  private AbstractQueue<E> _queue;

  private MessageListener _messageListener;
  private ClassLoader _listenerClassLoader;

  private MessageConsumerCallback _messageCallback;
  private EntryCallback<E> _entryCallback;

  private String _messageSelector;
  protected Selector _selector;
  private boolean _noLocal;
  private boolean _isAutoAcknowledge;

  private volatile boolean _isClosed;

  MessageConsumerImpl(JmsSession session,
                      AbstractQueue<E> queue,
                      String messageSelector,
                      boolean noLocal)
    throws JMSException
  {
    _session = session;
    _queue = queue;
    _messageSelector = messageSelector;

    if (_messageSelector != null) {
      SelectorParser parser = new SelectorParser();
      _selector = parser.parse(messageSelector);
    }
    _noLocal = noLocal;

    // _queue.addMessageAvailableListener(this);

    switch (_session.getAcknowledgeMode()) {
    case Session.AUTO_ACKNOWLEDGE:
    case Session.DUPS_OK_ACKNOWLEDGE:
      _isAutoAcknowledge = true;
      break;

    default:
      _isAutoAcknowledge = false;
      break;
    }
  }

  /**
   * Returns the destination
   */
  protected AbstractDestination<E> getDestination()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDestination(): MessageConsumer is closed."));

    return _queue;
  }

  /**
   * Returns true if local messages are not sent.
   */
  public boolean getNoLocal()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getNoLocal(): MessageConsumer is closed."));

    return _noLocal;
  }

  /**
   * Returns the message listener
   */
  public MessageListener getMessageListener()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getNoLocal(): MessageConsumer is closed."));

    return _messageListener;
  }

  /**
   * Sets the message listener
   */
  public void setMessageListener(MessageListener listener)
    throws JMSException
  {
    setMessageListener(listener, -1);
  }

  /**
   * Sets the message listener with a poll interval
   */
  public void setMessageListener(MessageListener listener, long pollInterval)
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setMessageListener(): MessageConsumer is closed."));

    _messageListener = listener;
    _messageCallback = new MessageConsumerCallback(listener);

    _listenerClassLoader = Thread.currentThread().getContextClassLoader();

    // if Consumer has already been started then register the message Call back.
    if (isActive()) {
      addMessageCallback();
    }

  }

  /**
   * Returns the message consumer's selector.
   */
  public String getMessageSelector()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getMessageSelector(): MessageConsumer is closed."));

    return _messageSelector;
  }

  /**
   * Returns the parsed selector.
   */
  public Selector getSelector()
  {
    return _selector;
  }

  /**
   * Returns true if active
   */
  public boolean isActive()
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("isActive(): MessageConsumer is closed."));

    return _session.isActive() && ! _isClosed;
  }

  /**
   * Returns true if closed
   */
  public boolean isClosed()
  {
    return _isClosed || _session.isClosed();
  }

  /**
   * Receives the next message, blocking until a message is available.
   */
  @Override
  public Message receive()
    throws JMSException
  {
    long timeout = Long.MAX_VALUE / 2;
    
    if (Alarm.isTest())
      timeout = 600000L;
    
    return receiveImpl(timeout);
  }

  /**
   * Receives a message from the queue.
   */
  @Override
  public Message receiveNoWait()
    throws JMSException
  {
    return receiveImpl(0);
  }

  /**
   * Receives a message from the queue.
   */
  @Override
  public Message receive(long timeout)
    throws JMSException
  {
    Message msg = receiveImpl(timeout);

    if (msg != null && log.isLoggable(Level.FINE))
      log.fine(_queue + " receive message " + msg);

    return msg;
  }

  /**
   * Receives a message from the queue.
   */
  protected Message receiveImpl(long timeout)
    throws JMSException
  {
    if (_isClosed || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("receiveNoWait(): MessageConsumer is closed."));

    if (Long.MAX_VALUE / 2 < timeout || timeout < 0)
      timeout = Long.MAX_VALUE / 2;

    long now = Alarm.getCurrentTime();
    long expireTime = timeout > 0 ? now + timeout : 0;

    while (_session.isActive()) {
      QueueEntry<E> entry
        = _queue.receiveEntry(expireTime, _isAutoAcknowledge, _selector);

      if (entry == null)
        return null;

      E payload = entry.getPayload();

      if (payload == null)
        return null;
      
      MessageImpl msg = null;

      if (payload instanceof MessageImpl) {
        msg = (MessageImpl) payload;
      }
      else if (payload instanceof String) {
        msg = new TextMessageImpl((String) payload);
        msg.setJMSMessageID(entry.getMsgId());
      }
      else {
        msg = new ObjectMessageImpl((Serializable) payload);
        msg.setJMSMessageID(entry.getMsgId());
      }

      msg.setReceive();
      
      /*if (_selector != null && ! _selector.isMatch(msg)) {
        _queue.acknowledge(msg.getJMSMessageID());
        continue;
      }*/

      //else {
      if (log.isLoggable(Level.FINE))
        log.fine(_queue + " receiving message " + msg);

      if (! _isAutoAcknowledge)
        _session.addTransactedReceive(_queue, msg);

      return msg;
      //}
    }

    return null;
  }

  /**
   * Notifies that a message is available.
   */
  public boolean notifyMessageAvailable()
  {
    synchronized (_consumerLock) {
      _consumerLock.notifyAll();
    }

    return _session.notifyMessageAvailable();
  }

  /**
   * Called with the session's thread to handle any messages
   */
  boolean handleMessage(MessageListener listener)
  {
    if (_messageListener != null)
      listener = _messageListener;

    if (listener == null)
      return false;

    MessageImpl msg = null;
    try {
      MessageCallback<E> callback = _messageCallback;
      
      // XXX: not correct with new model

      // _queue.listen(callback);

      /*
      if (msg == null)
        System.out.println(_queue + " NOMESSAGE:");
      */

      if (msg != null) {
        if (log.isLoggable(Level.FINE)) {
          log.fine(_queue + " deliver " + msg + " to listener " + listener);
        }

        msg.setSession(_session);

        // XXX: ejb30/bb/mdb/activationconfig/queue/selectorauto/annotated/negativeTest1
        if (_selector == null || _selector.isMatch(msg)) {
          _session.addTransactedReceive(_queue, msg);

          Thread thread = Thread.currentThread();
          ClassLoader oldLoader = thread.getContextClassLoader();
          try {
            thread.setContextClassLoader(_listenerClassLoader);

            listener.onMessage(msg);
          } finally {
            thread.setContextClassLoader(oldLoader);
          }
        }

        if (_session.getTransacted())
          _session.commit();
        else
          msg.acknowledge();

        return true;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, L.l("{0}: message listener '{1}' failed for message '{2}' with exception\n{3}",
                                 this, listener, msg, e.toString()),
              e);

      _queue.addListenerException(e);
    }

    return false;
  }
  
  public void addMessageCallback() 
  {
    MessageConsumerCallback callback = _messageCallback;

    if (callback != null) {
      boolean isAutoAcknowledge = _isAutoAcknowledge;
      
      _entryCallback = _queue.addMessageCallback(callback, isAutoAcknowledge);
    }    
  }

  /**
   * Starts the consumer
   */
  public void start()
  {
    addMessageCallback();
  }  
  

  /**
   * Stops the consumer.
   */
  public void stop()
    throws JMSException
  {
    EntryCallback callback = _entryCallback;
    _entryCallback = null;

    if (callback != null)
      _queue.removeMessageCallback(callback);

    /*
    synchronized (_consumerLock) {
      _consumerLock.notifyAll();
    }
    */
  }

  /**
   * Closes the consumer.
   */
  public void close()
    throws JMSException
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
    }

    if (_queue instanceof TemporaryQueueImpl) {    
      ((TemporaryQueueImpl)_queue).removeMessageConsumer();
    }
    
    // _queue.removeMessageAvailableListener(this);
    _session.removeConsumer(this);    
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _queue + "]";
  }

  class MessageConsumerCallback implements MessageCallback<E> {
    private final MessageListener _listener;
    private final ClassLoader _classLoader;
    
    MessageConsumerCallback(MessageListener listener)
    {
      _listener = listener;
      _classLoader = Thread.currentThread().getContextClassLoader();
    }
    
    public void messageReceived(String msgId, E payload)
    {
      MessageImpl message = null;

      try {
        if (payload instanceof MessageImpl)
          message = (MessageImpl) payload;
        else if (payload instanceof String) {
          message = new TextMessageImpl((String) payload);
          message.setJMSMessageID(msgId);
        }
        else {
          message = new ObjectMessageImpl((Serializable) payload);
          message.setJMSMessageID(msgId);
        }

        if (_selector == null || _selector.isMatch(message)) {
          // XXX: only if XA
          //if (! _isAutoAcknowledge) {
          _session.addTransactedReceive(_queue, message);
          //}

          Thread thread = Thread.currentThread();
          ClassLoader oldLoader = thread.getContextClassLoader();
          try {
            thread.setContextClassLoader(_classLoader);

            _listener.onMessage(message);
          } finally {
            thread.setContextClassLoader(oldLoader);

            // XXX: commit/rollback?
            if (_session.getTransacted())
              _session.commit();
            else
              _session.acknowledge();
          }
        }
      } catch (JMSException e) {
        throw new MessageException(e);
      }
    }
  }
}
