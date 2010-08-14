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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.caucho.config.scope.AbstractScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

/**
 * The conversation scope value
 */
@SuppressWarnings("serial")
public class ConversationContext extends AbstractScopeContext
  implements Conversation, java.io.Serializable
{
  private static final Logger log = Logger.getLogger(ConversationContext.class.getName());
  private static final L10N L = new L10N(ConversationContext.class);
  
  private static final long DEFAULT_TIMEOUT = 120000L;
  
  public ConversationContext()
  {
  }
  
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    return facesContext != null;
  }

  @Override
  public boolean isTransient()
  {
    Scope scope = getJsfScope();
    
    return scope == null || scope._extendedId == null;
  }

  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScope()
  {
    return ConversationScoped.class;
  }

  /**
   * Returns the current value of the component in the conversation scope.
   */
  @Override
  protected ContextContainer getContextContainer()
  {
    Scope scope = getJsfScope();
    
    if (scope != null)
      return scope._transientConversation;
    else
      return null;
  }

  /**
   * Returns the current value of the component in the conversation scope.
   */
  @Override
  protected ContextContainer createContextContainer()
  {
    Scope scope = createJsfScope();

    if (scope._transientConversation == null) {
      scope._transientConversation = scope._extendedConversation;
      
      if (scope._transientConversation == null)
        scope._transientConversation = new ContextContainer();
    }
    
    return scope._transientConversation;
  }

  //
  // Conversation API
  //

  /**
   * Begins an extended conversation
   */
  @Override
  public void begin()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(RandomUtil.getRandomLong() & 0x7fffffffffffffffL);

    begin(sb.toString());
  }
  
  @Override
  public void begin(String name)
  {
    Scope scope = createJsfScope();

    if (scope._extendedId != null)
      throw new IllegalStateException(L.l("Conversation begin() must only be called when a long-running conversation does not exist."));
    
    scope._extendedId = name;
    scope._extendedConversation = scope._transientConversation;
  }

  /**
   * Ends an extended conversation
   */
  @Override
  public void end()
  {
    Scope scope = getJsfScope();

    if (scope == null)
      return;
    
    if (scope._extendedId == null)
      throw new IllegalStateException(L.l("Conversation end() must only be called when a long-running conversation exists."));

    scope._extendedId = null;
    scope._extendedConversation = null;
  }

  @Override
  public String getId()
  {
    Scope scope = getJsfScope();
    
    if (scope != null)
      return scope._extendedId;
    else
      return null;
  }

  @Override
  public long getTimeout()
  {
    Scope scope = createJsfScope();
    
    return scope.getTimeout();
  }

  @Override
  public void setTimeout(long timeout)
  {
    try {
      Scope scope = createJsfScope();
    
      scope.setTimeout(timeout);
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  private Scope getJsfScope()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    
    if (facesContext == null)
      return null;
    
    return getJsfScope(facesContext, false);
  }
  
  private Scope createJsfScope()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    
    if (facesContext == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available because JSF is not active"));

    return getJsfScope(facesContext, true);
  }
    
  private Scope getJsfScope(FacesContext facesContext, boolean isCreate)
  {
    ExternalContext extContext = facesContext.getExternalContext();
    Map<String,Object> sessionMap = extContext.getSessionMap();

    Scope scope = (Scope) sessionMap.get("caucho.conversation");

    if (scope == null) {
      if (! isCreate)
        return null;
      
      scope = new Scope();
      sessionMap.put("caucho.conversation", scope);
    }
    
    if (scope._transientConversation == null)
      scope._transientConversation = scope._extendedConversation;

    return scope;
  }
  
  public void destroy()
  {
    Scope scope = getJsfScope();
    
    if (scope == null)
      return;
    
    ContextContainer context = scope._transientConversation;
    scope._transientConversation = null;
    
    if (scope._extendedConversation == null && context != null)
      context.close();
  }

  static class Scope implements java.io.Serializable, HttpSessionBindingListener {
    ContextContainer _transientConversation;
    
    String _extendedId;
    ContextContainer _extendedConversation;
    private long _timeout = DEFAULT_TIMEOUT;
    
    public long getTimeout()
    {
      return _timeout;
    }
    
    public void setTimeout(long timeout)
    {
      _timeout = timeout;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event)
    {
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event)
    {
      ContextContainer conversation = _extendedConversation;
      
      if (conversation != null)
        conversation.close();
      
      _extendedConversation = null;
    }
  }
}
