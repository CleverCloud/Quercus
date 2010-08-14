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

package com.caucho.server.dispatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.servlet.comet.CometController;
import com.caucho.servlet.comet.CometServlet;

/**
 * Represents the final servlet in a filter chain.
 */
public class CometServletFilterChain implements FilterChain {
  public static String SERVLET_NAME = "javax.servlet.error.servlet_name";

  // servlet config
  private ServletConfigImpl _config;
  // servlet
  private CometServlet _servlet;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the underlying servlet
   */
  public CometServletFilterChain(ServletConfigImpl config)
  {
    if (config == null)
      throw new NullPointerException();

    _config = config;
  }

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    return _config.getServletName();
  }

  /**
   * Returns the role map.
   */
  public HashMap<String,String> getRoleMap()
  {
    return _config.getRoleMap();
  }

  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @since Servlet 2.3
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    if (_servlet == null) {
      try {
        _servlet = (CometServlet) _config.createServlet(false);
      } catch (ServletException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }

    ServletCometController controller = null;

    try {
      controller = (ServletCometController) request.getAttribute("caucho.comet.controller");

      if (controller != null) {
        controller.suspend();
        
        if (_servlet.resume(request, response, controller)) {
          controller = null;
        }
      }
      else {
        controller = new ServletCometController(request, response);
        request.setAttribute("caucho.comet.controller", controller);
        controller.suspend();

        if (_servlet.service(request, response, controller)) {
          controller = null;
        }
      }
    } catch (UnavailableException e) {
      _servlet = null;
      _config.setInitException(e);
      _config.killServlet();
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (ServletException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (IOException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (RuntimeException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } finally {
      if (controller != null)
        controller.close();
    }
  }
  
  @SuppressWarnings("deprecation")
  static class ServletCometController implements CometController, AsyncListener {
    private HttpServletRequestImpl _request;
    private ServletResponse _response;
   
    private AsyncContext _context;
    private AtomicBoolean _isWake = new AtomicBoolean();
    
    ServletCometController(ServletRequest request, ServletResponse response)
    {
      _request = (HttpServletRequestImpl) request;
      _response = response;
    }
    
    void suspend()
    {
      /*
      if (_request == null)
        return;
        */
      
      _context = _request.startAsync();
      _context.addListener(this);
      
      _isWake.set(false);
    }

    @Override
    public long getMaxIdleTime()
    {
      return 0;
    }

    @Override
    public boolean isClosed()
    {
      HttpServletRequestImpl request = _request;
      
      return request == null || request.isClosed() || ! isActive();
    }

    @Override
    public Object getAttribute(String name)
    {
      ServletRequest request = _request;
      
      if (request != null) {
        synchronized (request) {
          return request.getAttribute("caucho.comet." + name);
        }
      }
      
      return null;
    }

    @Override
    public void removeAttribute(String name)
    {
      ServletRequest request = _request;
      
      if (request != null) {
        synchronized (request) {
          request.removeAttribute("caucho.comet." + name);
        }
      }
    }

    @Override
    public void setAttribute(String name, Object value)
    {
      ServletRequest request = _request;
      
      if (request != null) {
        synchronized (request) {
          request.setAttribute("caucho.comet." + name, value);
        }
      }
    }

    @Override
    public void setMaxIdleTime(long idleTime)
    {
      if (_context != null)
        _context.setTimeout(idleTime);
    }

    public boolean isActive()
    {
      return _context != null;
    }
    
    @Override
    public boolean wake()
    {
      if (! _isWake.getAndSet(true)) {
        AsyncContext context = _context;
        
        if (context != null)
          context.dispatch();
        
        return true;
      }
      
      return false;
    }

    @Override
    public void close()
    {
      AsyncContext context = _context;
      _context = null;
      
      if (context != null) {
        context.complete();
      }
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException
    {
      //_request = null;
      //_response = null;
      // _context = null;
    }

    @Override
    public void onError(AsyncEvent event) throws IOException
    {
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException
    {
      // TODO Auto-generated method stub
      
    }

    /* (non-Javadoc)
     * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
     */
    @Override
    public void onTimeout(AsyncEvent event) throws IOException
    {
      _request = null;
      _response = null;
      _context = null;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _context + "]";
    }
  }
}
