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

import java.io.IOException;

import javax.enterprise.context.ConversationScoped;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import com.caucho.config.inject.InjectManager;

/**
 * The conversation scope value
 */
public class ConversationJsfViewHandler extends ViewHandlerWrapper
{
  private ViewHandler _next;
  private InjectManager _cdiManager;
  private ConversationContext _conversation;

  public ConversationJsfViewHandler(ViewHandler next)
  {
    _next = next;
    
    _cdiManager = InjectManager.create();
    _conversation = (ConversationContext) _cdiManager.getContext(ConversationScoped.class);
  }

  /*
  @Override
  public void initView(FacesContext context)
    throws FacesException
  {
    //_conversation.initView(context);
    
    super.initView(context);
  }
  */

  @Override
  public void renderView(FacesContext context,
                         UIViewRoot viewToRender)
    throws IOException, FacesException
  {
    try {
      super.renderView(context, viewToRender);
    } finally {
      _conversation.destroy();
    }
  }
  
  @Override
  public ViewHandler getWrapped()
  {
    return _next;
  }
}
