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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Status;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the next filter in a filter chain.  The final filter will
 * be the servlet itself.
 */
public class ContextFilterChain implements FilterChain {
  private static final Logger log
    = Logger.getLogger(ContextFilterChain.class.getName());
  
  // Next filter chain
  private FilterChain _next;
  
  // class loader
  private ClassLoader _classLoader;
  // transaction manager
  private TransactionManagerImpl _tm;
  // error page manager
  private ErrorPageManager _errorPageManager;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public ContextFilterChain(FilterChain next)
  {
    _next = next;
    
    _classLoader = Thread.currentThread().getContextClassLoader();

    try {
      _tm = TransactionManagerImpl.getInstance();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Sets the error page manager.
   */
  public void setErrorPageManager(ErrorPageManager errorPageManager)
  {
    _errorPageManager = errorPageManager;
  }
  
  /**
   * Invokes the next filter in the chain or the final servlet at
   * the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   * @since Servlet 2.3
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);
      _next.doFilter(request, response);
    } catch (ServletException e) {
      if (_errorPageManager != null)
        _errorPageManager.sendServletError(e, request, response);
      else
        throw e;
    } catch (IOException e) {
      if (_errorPageManager != null)
        _errorPageManager.sendServletError(e, request, response);
      else
        throw e;
    } catch (RuntimeException e) {
      if (_errorPageManager != null)
        _errorPageManager.sendServletError(e, request, response);
      else
        throw e;
    } finally {
      // needed for things like closing the session
      if (request instanceof AbstractHttpRequest)
        ((AbstractHttpRequest) request).finishInvocation();

      if (_tm != null) {
        try {
          TransactionImpl transaction = _tm.getCurrent();
          if (transaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
            log.warning("Transaction not properly closed for " + ((HttpServletRequest) request).getRequestURL());
          }
          transaction.close();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.getMessage(), e);
        }
      }
      
      if (response instanceof AbstractHttpResponse)
        ((AbstractHttpResponse) response).finishInvocation();

      thread.setContextClassLoader(oldLoader);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _next + "]";
  }    
}
