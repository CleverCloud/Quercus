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

package com.caucho.inject;

import java.lang.annotation.Annotation;
import javax.enterprise.context.RequestScoped;

import com.caucho.config.scope.AbstractScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.util.L10N;

/**
 * Scope based on the current transaction.
 */
@Module
public class RequestContext extends AbstractScopeContext
{
  private static final L10N L = new L10N(RequestContext.class);
  
  private static final ThreadLocal<RequestContext> _threadLocal
    = new ThreadLocal<RequestContext>();
  
  private ContextContainer _context;
  private int _depth;
  
  public RequestContext()
  {
  }
  
  public static void begin()
  {
    RequestContext request = _threadLocal.get();
    
    if (request == null) {
      request = new RequestContext();
      _threadLocal.set(request);
    }
    
    request._depth++;
  }
  
  public static void end()
  {
    RequestContext request = _threadLocal.get();
    
    if (request == null)
      throw new IllegalStateException(L.l("end() requires a matching begin()"));
      
    if (--request._depth == 0) {
      ContextContainer context = request._context;
      request._context = null;
      
      if (context != null)
        context.close();
    }
  }
  
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    RequestContext request = _threadLocal.get();

    return request != null && request._depth > 0;
  }

  /**
   * Returns the scope annotation type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return RequestScoped.class;
  }

  @Override
  protected ContextContainer getContextContainer()
  {
    RequestContext request = _threadLocal.get();
    
    if (request != null)
      return request._context;
    else
      return null;
  }

  @Override
  protected ContextContainer createContextContainer()
  {
    RequestContext request = _threadLocal.get();

    if (request._context == null) {
      request._context = new ContextContainer();
    }
    
    return request._context;
  }
}
