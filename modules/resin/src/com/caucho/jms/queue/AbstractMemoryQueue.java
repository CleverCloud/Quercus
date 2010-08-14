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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.ThreadPool;
import com.caucho.util.Alarm;

/**
 * Provides abstract implementation for a memory queue.
 * 
 */
@SuppressWarnings("serial")
public abstract class AbstractMemoryQueue<E,QE extends QueueEntry<E>>
  extends AbstractQueue<E>
{
  private static final Logger log
    = Logger.getLogger(AbstractMemoryQueue.class.getName());
  
  private int _queueSizeMax = Integer.MAX_VALUE / 2;
  
  private final Object _queueLock = new Object();

  private ArrayList<EntryCallback<E>> _callbackList
    = new ArrayList<EntryCallback<E>>();

  private QE []_head = (QE []) new QueueEntry[10];
  private QE []_tail = (QE []) new QueueEntry[10];

  private ThreadPool _threadPool = ThreadPool.getThreadPool();
  
  private final AtomicLong _readSequenceGenerator = new AtomicLong();
  
  private final AtomicInteger _queueSize = new AtomicInteger();
  
  private final AtomicBoolean _isQueueThrottle = new AtomicBoolean();
  
  // stats
  private AtomicInteger _receiverCount = new AtomicInteger();
  private AtomicInteger _listenerCount = new AtomicInteger();
  
  //
  // configuration
  //
  
  public void setQueueSizeMax(int max)
  {
    if (max <= 0 || Integer.MAX_VALUE / 2 < max)
      _queueSizeMax = Integer.MAX_VALUE / 2;
    else
      _queueSizeMax = max;
  }
  
  public int getQueueSizeMax()
  {
    return _queueSizeMax;
  }

  //
  // Abstract/stub methods to be implemented by the Queue
  //
  /**
   * Sends a message to the queue
   */
  @Override
  public void send(String msgId,
                   E payload,
                   int priority,
                   long expireTime)
    throws MessageException
  {
    QE entry = writeEntry(msgId, payload, priority, expireTime);
      
    addQueueEntry(entry, expireTime);
  }

  //
  // send implementation
  //

  abstract protected QE writeEntry(String msg,
                                   E payload,
                                   int priority,
                                   long expires);

  protected void addQueueEntry(QE entry, long expires)
  {
    addEntry(entry, expires);

    dispatchMessage();
  }

  //
  // receive implementation
  //

  /**
   * Primary message receiving, registers a callback for any new
   * message.
   */
  public QE receiveEntry(long expireTime, boolean isAutoAck) 
     throws MessageException
  {
    return receiveEntry(expireTime, isAutoAck, null);
  }
  
  public QE receiveEntry(long expireTime, boolean isAutoAck, 
                         QueueEntrySelector selector) throws MessageException
  {
    _receiverCount.incrementAndGet();
    
    try {
      QE entry = null;

      synchronized (_queueLock) {
        if (_callbackList.size() == 0) {
          entry = readEntry(selector);
        }
      }

      if (entry != null) {
        readPayload(entry);
  
        if (isAutoAck)
          acknowledge(entry.getMsgId());
          
        return entry;
      }
  
      if (expireTime <= Alarm.getCurrentTimeActual()) {              
        return null;
      }
  
      ReceiveEntryCallback callback = new ReceiveEntryCallback(isAutoAck);

      return (QE) callback.waitForEntry(expireTime);      
    } finally {
      _receiverCount.decrementAndGet();  
    }
  }
  
  public EntryCallback<E> addMessageCallback(MessageCallback<E> callback,
                                             boolean isAutoAck)
  {
    _listenerCount.incrementAndGet();
    
    ListenEntryCallback entryCallback
      = new ListenEntryCallback(callback, isAutoAck);

    listen(entryCallback);

    return entryCallback;
  }

  public void removeMessageCallback(EntryCallback<E> callback)
  {
    ListenEntryCallback listenerCallback
      = (ListenEntryCallback) callback;

    listenerCallback.close();

    synchronized (_queueLock) {
      _callbackList.remove(listenerCallback);
    }
    
    _listenerCount.decrementAndGet();
  }

  //
  // abstract receive stubs
  //

  protected void acknowledge(QE entry)
  {
  }

  protected void readPayload(QE entry)
  {
  }
  
  public void removeMessageCallback(MessageCallback callback)
  {
    /*
    synchronized (_callbackList) {
      _callbackList.remove(callback);
    }
    */
  }
    
  /**
   * Acknowledges the receipt of a message
   */
  @Override
  public void acknowledge(String msgId)
  {
    QE entry = removeEntry(msgId);

    if (entry != null)
      acknowledge(entry);
  }

  public boolean listen(EntryCallback<E> callback)
    throws MessageException
  {
    QE entry = null;

    synchronized (_queueLock) {
      if (_callbackList.size() > 0 || (entry = readEntry()) == null) {
        _callbackList.add(callback);
        return false;
      }
    }

    readPayload(entry);

    if (callback.entryReceived(entry)) {
      acknowledge(entry.getMsgId());
    }

    return true;
  }

  protected void dispatchMessage()
  {
    while (true) {
      QE entry = null;
      EntryCallback<E> callback = null;
      
      synchronized (_queueLock) {
        if (_callbackList.size() == 0 || (entry = readEntry()) == null) {
          return;
        }

        callback = _callbackList.remove(0);
      }

      readPayload(entry);

      if (callback.entryReceived(entry)) {
        acknowledge(entry.getMsgId());
      }
    }
  }
  
  //
  // Queue statistics JMX
  //

  /**
   * Returns the queue size
   */
  public int getQueueSize()
  {
    int count = 0;

    for (int i = 0; i < _head.length; i++) {
      for (QueueEntry<E> entry = _head[i];
           entry != null;
           entry = entry._next) {
        count++;
      }
    }

    return count;
  }
  
  /**
   * Returns true if a message is available.
   */
  @Override
  public boolean hasMessage()
  {
    return getQueueSize() > 0;
  }
  
  @Override  
  public int getConsumerCount()
  {
    return _listenerCount.get();
  }

  @Override  
  public int getReceiverCount()
  {
    return _receiverCount.get();
  }
  
  //
  // queue management
  //

  /**
   * Add an entry to the queue
   */
  private QE addEntry(QE entry, long expires)
  {
    int priority = entry.getPriority();
    
    synchronized (_queueLock) {
      if (_tail[priority] != null)
        _tail[priority]._next = entry;
      else
        _head[priority] = entry;

      _tail[priority] = entry;
    }
    
    int size = _queueSize.incrementAndGet();
    
    if (_queueSizeMax < size) {
      long timeout = 100;
      
      waitForQueueThrottle(timeout);
    }

    return entry;
  }
  
  private void waitForQueueThrottle(long timeout)
  {
    _isQueueThrottle.set(true);
    
    synchronized (_isQueueThrottle) {
      try {
        if (_isQueueThrottle.get()) {
          // long timeout = expires - Alarm.getCurrentTimeActual();
          
          if (timeout > 1000)
            timeout = 1000;
          
          if (timeout > 0) {
            _isQueueThrottle.wait(timeout);
          }
        }
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
  
  private void wakeQueueThrottle()
  {
   int size = _queueSize.get();
    
    if (size <= _queueSizeMax) {
      if (_isQueueThrottle.compareAndSet(true, false)) {
        synchronized (_isQueueThrottle) {
          _isQueueThrottle.notifyAll();
        }
      }
    }
  }
  
  /**
   * Returns the next entry from the queue
   */
  protected QE readEntry()
  {
    return readEntry(null);
  }
  /**
   * Returns the next entry from the queue
   */
  protected QE readEntry(QueueEntrySelector selector)
  {
    for (int i = _head.length - 1; i >= 0; i--) {
      for (QE entry = _head[i];
           entry != null;
           entry = (QE) entry._next) {

        if (! entry.isLease()) {
          continue;
        }

        if (entry.isRead()) {
          continue;
        }
        
        readPayload((QE) entry);
        if ((selector != null) && (! selector.isMatch(entry))) {
          continue;
        }
          
        entry.setReadSequence(_readSequenceGenerator.incrementAndGet());

        return entry;
      }
    }

    return null;
  }

  /**
   * 
   * @param selector
   * @return          Entries present in the Queue.
   */
  public ArrayList<QE> getBrowserList()
  {
    ArrayList<QE> entries = new ArrayList<QE>();
    for (int i = _head.length - 1; i >= 0; i--) {
      for (QE entry = _head[i];
           entry != null;
           entry = (QE) entry._next) {

        if (! entry.isLease()) {
          continue;
        }

        if (entry.isRead()) {
          continue;
        }
        
        readPayload(entry);

        entries.add(entry);
      }
    }
    
    return entries.size() > 0 ? entries : null;
  }
  
  /**
   * Removes message.
   */
  protected QE removeEntry(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
        QE prev = null;
        QE entry = _head[i];

        while (entry != null) {
          QE next = (QE) entry._next;
          
          if (msgId.equals(entry.getMsgId())) {
            if (prev != null)
              prev._next = entry._next;
            else
              _head[i] = (QE) entry._next;

            if (_tail[i] == entry)
              _tail[i] = prev;
            
            _queueSize.decrementAndGet();
            
            if (_isQueueThrottle.get()) {
              wakeQueueThrottle();
            }
            
            return entry;
          }

          prev = entry;
          entry = next;
        }
      }
    }

    return null;
  }
    
  
  /**
   * Rolls back the receipt of a message
   */
  @Override
  public void rollback(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
        for (QueueEntry<E> entry = _head[i];
             entry != null;
             entry = entry._next) {
          if (msgId.equals(entry.getMsgId())) {
            if (entry.isRead()) {
              entry.setReadSequence(0);

              /*
              MessageImpl msg = (MessageImpl) getPayload(entry);
        
              if (msg != null)
                msg.setJMSRedelivered(true);
              */
            }
            
            return;
          }
        }
      }
    }
  }
  
  public ArrayList<String> getMessageIds()
  {
    ArrayList<String> browserList = new ArrayList<String>();

    synchronized (_queueLock) {
      for (int i = 0; i < _head.length; i++) {
        for (QueueEntry<E> entry = _head[i];
             entry != null;
             entry = entry._next) {
          browserList.add(entry.getMsgId());
        }
      }
    }

    return browserList;    
  }

  /**
   * Synchronous timeout receive
   */
  class ReceiveEntryCallback implements EntryCallback<E> {
    private boolean _isAutoAck;
    
    private Thread _thread;
    private volatile QueueEntry<E> _entry;

    ReceiveEntryCallback(boolean isAutoAck)
    {
      _isAutoAck = isAutoAck;
      _thread = Thread.currentThread();
    }
    
    public boolean entryReceived(QueueEntry<E> entry)
    {
      _entry = entry;

      LockSupport.unpark(_thread);

      return _isAutoAck;
    }

    public QueueEntry<E> waitForEntry(long expireTime)
    {
      listen(this);
      
      while (_entry == null
             && (Alarm.getCurrentTimeActual() < expireTime)) {
        LockSupport.parkUntil(expireTime);
      }

      if (_entry == null) {
        synchronized (_queueLock) {
          _callbackList.remove(this);
        }
      }
      
      return _entry;
    }
  }

  /**
   * Async listen receive
   */
  class ListenEntryCallback implements EntryCallback<E>, Runnable {
    private MessageCallback<E> _callback;
    private ClassLoader _classLoader;

    private boolean _isClosed;
    
    private volatile QueueEntry<E> _entry;

    ListenEntryCallback(MessageCallback<E> callback, boolean isAutoAck)
    {
      _callback = callback;
      _classLoader = Thread.currentThread().getContextClassLoader();
    }
    
    public boolean entryReceived(QueueEntry<E> entry)
    {
      _entry = entry;

      _threadPool.schedule(this);

      return false;
    }

    public void run()
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      boolean isValid = false;
      long readSequence = _entry.getReadSequence();
      
      try {
        thread.setContextClassLoader(_classLoader);

        _callback.messageReceived(_entry.getMsgId(), _entry.getPayload());
        isValid = true;
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
        isValid = true;
      } catch (Throwable t) {
        log.log(Level.SEVERE, t.toString(), t);
      } finally {
        thread.setContextClassLoader(oldLoader);
        
        if (readSequence == _entry.getReadSequence()){
          acknowledge(_entry.getMsgId());
        }
      }

      if (! _isClosed && isValid)
        listen(this);
    }

    public void close()
    {
      _isClosed = true;
    }
  }
}