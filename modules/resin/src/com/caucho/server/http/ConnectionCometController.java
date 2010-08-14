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

package com.caucho.server.http;

import java.util.HashMap;

import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.env.thread.ThreadPool;
import com.caucho.network.listen.AsyncController;
import com.caucho.network.listen.CometHandler;
import com.caucho.network.listen.SocketLink;
import com.caucho.servlet.comet.CometController;
import com.caucho.util.L10N;

/**
 * Public API to control a comet connection.
 */
@SuppressWarnings("deprecation")
public class ConnectionCometController
  implements CometController, CometHandler {
  private static final L10N L = new L10N(ConnectionCometController.class);
  private final AsyncController _cometController;
  
  private HashMap<String, Object> _map;

  private ServletRequest _request;
  private ServletResponse _response;

  private AsyncListenerNode _listenerNode;

  private boolean _isTimeout;

  // private boolean _isInitial = true;

  private String _forwardPath;

  private long _maxIdleTime;

  public ConnectionCometController(SocketLink conn,
                                   boolean isTop,
                                   ServletRequest request,
                                   ServletResponse response)
  {
    _cometController = conn.toComet(this);

    _request = request;
    _response = response;
  }

  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      _maxIdleTime = Long.MAX_VALUE / 2;
  }

  /**
   * Gets the max idle time.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  /*
  public TcpConnection getConnection()
  {
    return (TcpConnection) _conn;
  }
  */

  /**
   * Returns true if the connection is the initial request
   */
  /*
  public final boolean isInitial()
  {
    return _isInitial;
  }
  */

  /**
   * Returns true if the connection should be suspended
   */
  /*
  public final boolean isSuspended()
  {
    TransportConnection conn = _conn;

    return conn != null && conn.isComet();
  }
  */

  /**
   * Returns true if the connection is complete.
   */
  public final boolean isComplete()
  {
    return _cometController.isCometComplete();
  }

  /**
   * Complete the connection
   */
  public final void complete()
  {
    _cometController.complete();
  }
  
  public void onTimeout()
  {
  }
  
  public void onComplete()
  {
  }

  /**
   * Suspend the connection on the next request
   */
  public final void startResume()
  {
    /*
    _isSuspended = false;
    _isInitial = false;
    */
  }

  /**
   * Wakes the connection.
   */
  public final boolean wake()
  {
    _cometController.wake();
    
    return false;
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return false;
  }

  /**
   * Sets the timeout.
   */
  public final void timeout()
  {
    // _conn.toCometComplete();

    wake();
  }

  /**
   * Return true if timed out
   */
  public final boolean isTimeout()
  {
    return _isTimeout;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isActive()
  {
    return  false; // _cometController.isActive();
  }

  /**
   * Returns true for an active comet connection.
   */
  public boolean isComet()
  {
    return false; // ! _cometController.isCometComplete();
  }

  /**
   * Sets the async listener
   */
  public void setAsyncListenerNode(AsyncListenerNode node)
  {
    _listenerNode = node;
  }

  public void addAsyncListener(AsyncListener listener,
                               ServletRequest request,
                               ServletResponse response)
  {
    _listenerNode
      = new AsyncListenerNode(listener, request, response, _listenerNode);
  }

  public void addListener(AsyncListener listener)
  {
    addListener(listener, _request, _response);
  }

  public void addListener(AsyncListener listener,
                          ServletRequest request,
                          ServletResponse response)
  {
    _listenerNode
      = new AsyncListenerNode(listener, request, response, _listenerNode);
  }

  public <T extends AsyncListener> T createListener(Class<T> cl)
    throws ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setTimeout(long timeout)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getTimeout()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets a request attribute.
   */
  public Object getAttribute(String name)
  {
    if (_map != null) {
      synchronized (_map) {
        return _map.get(name);
      }
    } else
      return null;
  }

  /**
   * Sets a request attribute.
   */
  public void setAttribute(String name, Object value)
  {
    if (_map == null) {
      synchronized (this) {
        if (_map == null)
          _map = new HashMap<String, Object>(8);
      }
    }

    synchronized (_map) {
      _map.put(name, value);
    }
  }

  /**
   * Remove a request attribute.
   */
  public void removeAttribute(String name)
  {
    if (_map != null) {
      synchronized (_map) {
        _map.remove(name);
      }
    }
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isClosed()
  {
    return _cometController.isCometComplete();
  }

  public ServletRequest getRequest()
  {
    return _request;
  }

  public ServletResponse getResponse()
  {
    return _response;
  }

  public boolean hasOriginalRequestAndResponse()
  {
    return true;
  }

  public String getForwardPath()
  {
    return _forwardPath;
  }

  public void dispatch()
  {
    _cometController.wake();
  }

  public void dispatch(String path)
  {
    _forwardPath = path;

    dispatch();
  }

  public void dispatch(ServletContext context, String path)
  {
    _forwardPath = path;

    dispatch();
  }

  public void start(Runnable task)
  {
    if (isActive()) {
      ThreadPool.getCurrent().schedule(task);
    }
    else
      throw new IllegalStateException(
          L.l("AsyncContext.start() is not allowed because the AsyncContext has been completed."));
  }


  /**
   * Closes the connection.
   */
  public void close()
  {
    complete();
  }

  public void closeImpl()
  {
    // complete();

    _cometController.complete();

    _request = null;
    _response = null;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cometController + "]";
  }
}
