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

package com.caucho.bam;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.caucho.util.Alarm;

/**
 * QueryCallbackManager is used to generate query ids and to wait
 * for query callbacks.
 */
public class QueryManager {
  private final AtomicLong _qId = new AtomicLong();

  private final QueryMap _queryMap = new QueryMap();

  public QueryManager()
  {
  }

  public QueryManager(long seed)
  {
    _qId.set(seed);
  }

  /**
   * Generates a new unique query identifier.
   */
  public final long generateQueryId()
  {
    return _qId.incrementAndGet();
  }

  /**
   * Adds a query callback to handle a later message.
   *
   * @param id the unique query identifier
   * @param callback the application's callback for the result
   */
  public void addQueryCallback(long id, QueryCallback callback)
  {
    _queryMap.add(id, callback);
  }

  /**
   * Registers a callback future.
   */
  public QueryFuture addQueryFuture(long id, 
                                    String to,
                                    String from,
                                    Serializable payload,
                                    long timeout)
  {
    QueryFutureImpl future
    = new QueryFutureImpl(id, to, from, payload, timeout);

    _queryMap.add(id, future);

    return future;
  }

  //
  // callbacks and low-level routines
  //

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public final boolean onQueryResult(long id,
                                     String to,
                                     String from,
                                     Serializable payload)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null) {
      item.onQueryResult(to, from, payload);

      return true;
    }
    else
      return false;
  }

  /**
   * Callback from the ActorStream to handle a queryResult.  Returns true
   * if the client has a pending query, false otherwise.
   */
  public final boolean onQueryError(long id,
                                    String to,
                                    String from,
                                    Serializable payload,
                                    ActorError error)
  {
    QueryItem item = _queryMap.remove(id);

    if (item != null) {
      item.onQueryError(to, from, payload, error);

      return true;
    }
    else
      return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static final class QueryMap {
    private final QueryItem []_entries = new QueryItem[128];
    private final int _mask = _entries.length - 1;

    void add(long id, QueryCallback callback)
    {
      int hash = (int) (id & _mask);

      synchronized (_entries) {
        _entries[hash] = new QueryItem(id, callback, _entries[hash]);
      }
    }

    QueryItem remove(long id)
    {
      int hash = (int) (id & _mask);

      synchronized (_entries) {
        QueryItem prev = null;

        for (QueryItem ptr = _entries[hash];
             ptr != null;
             ptr = ptr.getNext()) {
          if (id == ptr.getId()) {
            if (prev != null)
              prev.setNext(ptr.getNext());
            else
              _entries[hash] = ptr.getNext();

            return ptr;
          }

          prev = ptr;
        }

        return null;
      }
    }
  }

  static final class QueryItem {
    private final long _id;
    private final QueryCallback _callback;

    private QueryItem _next;

    QueryItem(long id, QueryCallback callback, QueryItem next)
    {
      _id = id;
      _callback = callback;
      _next = next;
    }

    final long getId()
    {
      return _id;
    }

    final QueryItem getNext()
    {
      return _next;
    }

    final void setNext(QueryItem next)
    {
      _next = next;
    }

    void onQueryResult(String to, String from, Serializable value)
    {
      if (_callback != null)
        _callback.onQueryResult(to, from, value);
    }

    void onQueryError(String to,
                      String from,
                      Serializable value,
                      ActorError error)
    {
      if (_callback != null)
        _callback.onQueryError(to, from, value, error);
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _id + "," + _callback + "]";
    }
  }

  static final class QueryFutureImpl implements QueryCallback, QueryFuture {
    private final long _id;
    private final String _to;
    private final String _from;
    private final Serializable _payload;
    private final long _timeout;

    private volatile Serializable _result;
    private volatile ActorError _error;
    private final AtomicBoolean _isResult = new AtomicBoolean();
    private volatile Thread _thread;

    QueryFutureImpl(long id,
                    String to,
                    String from,
                    Serializable payload,
                    long timeout)
    {
      _id = id;
      _to = to;
      _from = from;
      _payload = payload;
      _timeout = timeout;
    }

    public Serializable getResult()
    {
      return _result;
    }

    public Serializable get()
      throws TimeoutException, ActorException
    {
      if (! waitFor(_timeout)) {
        throw new TimeoutException(this + " query timeout " + _payload
                                   + " {to:" + _to + "}");
      }
      else if (getError() != null)
        throw getError().createException();
      else
        return getResult();
    }

    public ActorError getError()
    {
      return _error;
    }

    boolean waitFor(long timeout)
    {
      _thread = Thread.currentThread();
      long now = Alarm.getCurrentTimeActual();
      long expires = now + timeout;

      while (! _isResult.get() && Alarm.getCurrentTimeActual() < expires) {
        try {
          Thread.interrupted();
          LockSupport.parkUntil(expires);
        } catch (Exception e) {
        }
      }

      _thread = null;

      return _isResult.get();
    }

    public void onQueryResult(String fromJid, String toJid,
                              Serializable payload)
    {
      _result = payload;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }

    public void onQueryError(String fromJid, String toJid,
                             Serializable payload, ActorError error)
    {
      _error = error;
      _isResult.set(true);

      Thread thread = _thread;
      if (thread != null)
        LockSupport.unpark(thread);
    }
  }
}
