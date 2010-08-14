/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.render.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.util.*;

public class PhaseListenerTag extends TagSupport
{
  private static final L10N L = new L10N(PhaseListenerTag.class);
  
  private ValueExpression _typeExpr;
  private ValueExpression _bindingExpr;

  public void setType(ValueExpression type)
  {
    _typeExpr = type;
  }

  public void setBinding(ValueExpression binding)
  {
    _bindingExpr = binding;
  }

  public int doStartTag()
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();
    
    UIComponentTagBase viewTag = findRootView();

    if (viewTag == null)
      throw new JspException(L.l("f:phaseListener must be nested inside a f:view tag."));

    if (viewTag.getCreated()) {
      UIViewRoot viewRoot = (UIViewRoot) viewTag.getComponentInstance();

      PhaseListener listener = null;

      ELContext elContext = context.getELContext();

      if (_bindingExpr != null)
        listener = (PhaseListener) _bindingExpr.getValue(elContext);

      if (listener == null && _typeExpr != null) {
        String type = (String) _typeExpr.getValue(elContext);

        try {
          ClassLoader loader = Thread.currentThread().getContextClassLoader();
          Class cl = Class.forName(type, false, loader);

          // XXX: messages

          listener = (PhaseListener) cl.newInstance();
        } catch (Exception e) {
          throw new JspException(e);
        }

        if (_bindingExpr != null)
          _bindingExpr.setValue(elContext, listener);
      }

      viewRoot.addPhaseListener(listener);
    }
    
    return SKIP_BODY;
  }

  private UIComponentTagBase findRootView()
  {
    UIComponentTagBase compTag = null;
    
    for (Tag tag = this; tag != null; tag = tag.getParent()) {
      if (! (tag instanceof UIComponentTagBase))
        continue;

      compTag = (UIComponentTagBase) tag;
    }

    return compTag;
  }
}
