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

package com.caucho.server.webbeans;

import java.lang.annotation.Annotation;

import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.caucho.config.scope.AbstractScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.inject.Module;
import com.caucho.server.dispatch.ServletInvocation;

/**
 * The session scope value
 */
@Module
public class SessionScope extends AbstractScopeContext {
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      HttpSession session = ((HttpServletRequest) request).getSession();

      return session != null;
    }

    return false;
  }

  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScope()
  {
    return SessionScoped.class;
  }

  @Override
  protected ContextContainer getContextContainer()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    HttpSession session = ((HttpServletRequest) request).getSession();

    if (session == null)
      return null;

    return (ContextContainer) session.getAttribute("resin.candi.scope");
  }

  @Override
  protected ContextContainer createContextContainer()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    HttpSession session = ((HttpServletRequest) request).getSession();

    if (session == null)
      return null;

    ContextContainer context
      = (ContextContainer) session.getAttribute("resin.candi.scope");
    
    if (context == null) {
      context = new SessionContextContainer();
      session.setAttribute("resin.candi.scope", context);
    }
    
    return context;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
