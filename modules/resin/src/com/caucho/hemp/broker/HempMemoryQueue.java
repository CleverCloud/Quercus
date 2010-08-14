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

package com.caucho.hemp.broker;

import java.io.Closeable;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hemp.packet.Message;
import com.caucho.hemp.packet.MessageError;
import com.caucho.hemp.packet.Packet;
import com.caucho.hemp.packet.PacketQueue;
import com.caucho.hemp.packet.QueryError;
import com.caucho.hemp.packet.QueryGet;
import com.caucho.hemp.packet.QueryResult;
import com.caucho.hemp.packet.QuerySet;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.WaitQueue;

/**
 * Queue of hmtp packets
 */
public class HempMemoryQueue implements ActorStream, Runnable, Closeable
{
  private static final L10N L = new L10N(HempMemoryQueue.class);
  private static final Logger log
    = Logger.getLogger(HempMemoryQueue.class.getName());

  private static long _gid;

  // how long the thread should wait for a new request before exiting
  private long _queueIdleTimeout = 2000L;

  private final ThreadPool _threadPool = ThreadPool.getCurrent();
  private final ClassLoader _loader
    = Thread.currentThread().getContextClassLoader();

  private final String _name;
  private final ActorStream _linkStream;
  private final ActorStream _actorStream;

  private final int _threadMax;
  private final AtomicInteger _threadCount = new AtomicInteger();
  private final WaitQueue _wait = new WaitQueue();
  private final PacketQueue _queue;
  
  private final Lifecycle _lifecycle = new Lifecycle();

  private long _lastExitTime;

  public HempMemoryQueue(ActorStream actorStream,
                         ActorStream linkStream,
                         int threadMax)
  {
    if (linkStream == null)
      throw new NullPointerException();

    if (actorStream == null)
      throw new NullPointerException();

    _linkStream = linkStream;
    _actorStream = actorStream;

    _threadMax = threadMax;

    if (_actorStream.getJid() == null) {
      _name = _actorStream.getClass().getSimpleName();
    }
    else
      _name = _actorStream.getJid();

    int maxDiscardSize = -1;
    int maxBlockSize = 1024;
    long expireTimeout = -1;

    _queue = new PacketQueue(_name, maxDiscardSize, maxBlockSize, expireTimeout);
    
    _lifecycle.toActive();

    Environment.addCloseListener(this);
  }

  /**
   * Returns the actor's jid
   */
  @Override
  public String getJid()
  {
    return _actorStream.getJid();
  }

  /**
   * Returns true if a message is available.
   */
  public boolean isPacketAvailable()
  {
    return ! _queue.isEmpty();
  }
  
  /**
   * Returns the stream back to the link for error packets
   */
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  /**
   * Sends a message
   */
  @Override
  public void message(String to, String from, Serializable value)
  {
    enqueue(new Message(to, from, value));
  }

  /**
   * Sends a message
   */
  @Override
  public void messageError(String to,
                               String from,
                               Serializable value,
                               ActorError error)
  {
    enqueue(new MessageError(to, from, value, error));
  }

  /**
   * Query an entity
   */
  @Override
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable query)
  {
    enqueue(new QueryGet(id, to, from, query));
  }

  /**
   * Query an entity
   */
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    enqueue(new QuerySet(id, to, from, payload));
  }

  /**
   * Query an entity
   */
  @Override
  public void queryResult(long id,
                              String to,
                              String from,
                              Serializable value)
  {
    enqueue(new QueryResult(id, to, from, value));
  }

  /**
   * Query an entity
   */
  @Override
  public void queryError(long id,
                             String to,
                             String from,
                             Serializable query,
                             ActorError error)
  {
    enqueue(new QueryError(id, to, from, query, error));
  }

  protected ActorStream getActorStream()
  {
    return _actorStream;
  }

  protected final void enqueue(Packet packet)
  {
    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("{0} cannot accept packets because it's no longer active",
                                          this));
    
    if (log.isLoggable(Level.FINEST)) {
      int size = _queue.getSize();
      log.finest(this + " enqueue(" + size + ") " + packet);
    }

    _queue.enqueue(packet);

    /*
    if (_dequeueCount.get() > 0)
      return;
    */

    wakeConsumer(packet);

    /*
    if (Alarm.isTest()) {
      // wait a millisecond for the dequeue to avoid spawing extra
      // processing threads
      packet.waitForDequeue(10);
    }
    */
  }

  private void wakeConsumer(Packet packet)
  {
    long lastExitTime = _lastExitTime;
    _lastExitTime = Alarm.getCurrentTime();

    if (_wait.wake()) {
      return;
    }

    while (! _lifecycle.isDestroying()) {
      if (_queue.isEmpty()) {
        // empty queue
        return;
      }

      int size = _queue.getSize();
      int threadCount = _threadCount.get();
      long now = Alarm.getCurrentTime();

      if (threadCount >= _threadMax) {
        // thread max
        return;
      }
      else if (threadCount >= 2 && size / 3 < threadCount) {
        // too little work to spawn a new thread
        return;
      }
      else if (threadCount > 0 && now <= lastExitTime + 10) {
        // last spawn too recent
        return;
      }

      if (isClosed()) {
        return;
      }
      else if (_threadCount.compareAndSet(threadCount, threadCount + 1)) {
        // 10ms start is primarily for QA
        if (! _threadPool.start(this, 100)) {
          _threadPool.schedule(this);
        }
        return;
      }
      else if (_threadCount.get() > 0) {
        // other thread already added
        return;
      }
    }
  }

  /**
   * Dispatches the packet to the stream
   */
  protected void dispatch(Packet packet, WaitQueue.Item item)
  {
    packet.dispatch(getActorStream(), _linkStream);
  }

  protected Packet dequeue(WaitQueue.Item item, long timeout)
  {
    item.startPark();

    try {
      Packet packet = _queue.dequeue();

      if (packet == null) {
        if (timeout <= 0)
          return null;

        item.park(timeout);

        packet = _queue.dequeue();
      }

      if (packet != null)
        packet.unparkDequeue();

      return packet;
    } finally {
      item.endPark();
    }
  }

  private void consumeQueue(WaitQueue.Item item)
  {
    while (! isClosed()) {
      try {
        Packet packet;

        // _dequeueCount.incrementAndGet();
        packet = _queue.dequeue();
        // _dequeueCount.decrementAndGet();

        if (packet != null) {
          // reset last exit with a new packet
          _lastExitTime = Alarm.getCurrentTime();

          if (log.isLoggable(Level.FINEST))
            log.finest(this + " dequeue " + packet);

          packet.unparkDequeue();

          dispatch(packet, item);
        }
        else if (! waitForQueue(item)) {
          return;
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  private boolean waitForQueue(WaitQueue.Item item)
  {
    long expires = Alarm.getCurrentTimeActual() + _queueIdleTimeout;
    int spinMax = 8;

    while (! isClosed()) {
      for (int i = spinMax; i >= 0; i--) {
        if (! _queue.isEmpty()) {
          // check for queue values
          return true;
        }
      }

      long now = Alarm.getCurrentTimeActual();

      if (now < expires) {
        _wait.parkUntil(item, expires);
      }
      else {
        return false;
      }
    }

    return true;
  }

  @Override
  public void run()
  {
    Thread thread = Thread.currentThread();

    String oldName = thread.getName();
    String name = _name;

    thread.setName(name + "-" + _gid++);

    thread.setContextClassLoader(_loader);

    WaitQueue.Item item = _wait.create();

    try {
      while (! _lifecycle.isDestroying()) {
        try {
          if (log.isLoggable(Level.FINEST)) {
            log.finest(this + " spawn {threadCount:" + _threadCount.get()
                       + ", queueSize:" + _queue.getSize() + "}");
          }

          consumeQueue(item);
        } finally {
          _threadCount.decrementAndGet();
        }

        if (_queue.isEmpty()) {
          // queue check after the _threadCount decrement for threading
          // timing issues
          return;
        }

        _threadCount.incrementAndGet();
      }
    } finally {
      item.remove();

      thread.setName(oldName);
    }
  }

  @Override
  public void close()
  {
    _lifecycle.toStop();

    _wait.wakeAll();
    
    long expires = Alarm.getCurrentTimeActual() + 2000;
    
    while (! _queue.isEmpty()
           && Alarm.getCurrentTimeActual() < expires) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        
      }
    }
    
    _lifecycle.toDestroy();
  }

  @Override
  public boolean isClosed()
  {
    return _lifecycle.isDestroying() || _linkStream.isClosed();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
