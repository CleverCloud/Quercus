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

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.caucho.env.thread.ThreadPool;
import com.caucho.network.listen.AsyncController;
import com.caucho.network.listen.CometHandler;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.webapp.AsyncRequest;
import com.caucho.server.webapp.RequestDispatcherImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.comet.CometController;
import com.caucho.util.L10N;

/**
 * Public API to control a comet connection.
 */
public class AsyncContextImpl
  implements AsyncContext, CometHandler {
  private static final L10N L = new L10N(ConnectionCometController.class);
  private static final Logger log = Logger
      .getLogger(ConnectionCometController.class.getName());

  private AsyncController _cometController;

  private final ServletRequest _request;
  private final ServletResponse _response;
  
  private boolean _isOriginal;

  private AsyncListenerNode _listenerNode;

  private WebApp _dispatchWebApp;
  private String _dispatchPath;

  public AsyncContextImpl(AbstractHttpRequest httpConn,
                          ServletRequest request,
                          ServletResponse response,
                          boolean isOriginal)
  {
    _request = request;
    _response = response;
    
    if (! (request instanceof HttpServletRequest)) {
      throw new IllegalStateException(L.l("startAsync requires a HttpServletRequest"));
    }
    
    _dispatchWebApp = (WebApp) request.getServletContext();
    
    HttpServletRequest req = (HttpServletRequest) request;

    String servletPath = req.getServletPath();
    String pathInfo = req.getPathInfo();
    
    if (pathInfo == null)
      _dispatchPath = servletPath;
    else if (servletPath == null)
      _dispatchPath = pathInfo;
    else
      _dispatchPath = servletPath + pathInfo;
    
    _cometController = httpConn.getConnection().toComet(this);
    
    /* XXX: tck
    if (_cometController == null)
      throw new NullPointerException();
      */

    _isOriginal = isOriginal;
  }

  /**
   * Returns the originating request for the async. 
   */
  @Override
  public ServletRequest getRequest()
  {
    return _request;
  }

  /**
   * Returns the originating request for the async. 
   */
  @Override
  public ServletResponse getResponse()
  {
    return _response;
  }

  @Override
  public boolean hasOriginalRequestAndResponse()
  {
    return _isOriginal;
  }

  /**
   * Sets the suspend/idle timeout for the async request.
   */
  @Override
  public void setTimeout(long idleTimeout)
  {
    _cometController.setMaxIdleTime(idleTimeout);
  }

  /**
   * Returns the suspend/idle timeout for the async request.
   */
  @Override
  public long getTimeout()
  {
    return _cometController.getMaxIdleTime();
  }

  /**
   * Tests if the controller is active.
   */
  private boolean isActive()
  {
    return _cometController != null;
  }
  
  //
  // dispatch values
  //

  ServletContext getDispatchContext()
  {
    return _dispatchWebApp;
  }

  String getDispatchPath()
  {
    return _dispatchPath;
  }
  
  //
  // AsyncListener
  //

  @Override
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

  @Override
  public <T extends AsyncListener> T createListener(Class<T> cl)
    throws ServletException
  {
    return _request.getServletContext().createListener(cl);
  }
  
  //
  // Async dispatching
  //

  @Override
  public void dispatch()
  {
    AsyncController cometController = _cometController;
    
    if (cometController == null)
      throw new IllegalStateException(L.l("dispatch is not valid when no AsyncContext is available"));
     
    cometController.wake();
  }

  @Override
  public void dispatch(String path)
  {
    _dispatchPath = path;

    dispatch();
  }

  @Override
  public void dispatch(ServletContext context, String path)
  {
    _dispatchWebApp = (WebApp) context;
    _dispatchPath = path;

    dispatch();
  }

  @Override
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
   * Completes the comet connection
   */
  @Override
  public void complete()
  {
    AsyncController cometController = _cometController;
    
    if (cometController == null)
      return;

    try {
      cometController.complete();
    } finally {
      _cometController = null;
    }
  }
  
  //
  // CometHandler callbacks
  //
  
  /**
   * CometHandler callback when the connection times out.
   */
  public void onStart(ServletContext webApp,
                      ServletRequest request,
                      ServletResponse response)
  {
    AsyncEvent event = new AsyncEvent(this, request, response);
    Thread.dumpStack();
    
    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onStart(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  /**
   * CometHandler callback when the connection times out.
   */
  public void onTimeout()
  {
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onTimeout(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  /**
   * CometHandler callback when the connection times out.
   */
  public void onError()
  {
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onError(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  public void onComplete()
  {
    AsyncEvent event = new AsyncEvent(this, _request, _response);
    
    for (AsyncListenerNode node = _listenerNode;
         node != null;
         node = node.getNext()) {
      try {
        node.onComplete(event);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cometController + "]";
  }
}
