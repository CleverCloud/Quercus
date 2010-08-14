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

public class ActionListenerTag extends TagSupport
{
  private static final L10N L = new L10N(ActionListenerTag.class);
  
  private ValueExpression _type;
  private ValueExpression _binding;

  public void setType(ValueExpression type)
  {
    _type = type;
  }

  public void setBinding(ValueExpression binding)
  {
    _binding = binding;
  }

  public int doStartTag()
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();
    
    UIComponentClassicTagBase parent;
    
    parent = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException(L.l("f:actionListener must be nested inside a UIComponent tag."));

    UIComponent parentComp = parent.getComponentInstance();
    
    if (! (parentComp instanceof ActionSource)) {
      throw new JspException(L.l("f:actionListener must be nested inside an ActionSource UIComponent tag."));
    }

    ActionSource actionComp = (ActionSource) parentComp;

    ActionListener listener = null;

    if (parent.getCreated()) {
      ValueExpression bindingExpr = null;

      if (_binding != null) {
        listener = (ActionListener) _binding.getValue(context.getELContext());
      }

      if (listener == null && _type != null) {
        String className = (String) _type.getValue(context.getELContext());

        if (className != null) {
          try {
            Thread thread = Thread.currentThread();
            ClassLoader loader = thread.getContextClassLoader();
            Class cl = Class.forName(className, false, loader);

            listener = (ActionListener) cl.newInstance();
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new JspException(e);
          }
        }
      }

      if (listener == null)
        throw new JspException(L.l("f:actionListener cannot create an actionListener"));

      actionComp.addActionListener(listener);
      
      if (bindingExpr != null)
        bindingExpr.setValue(context.getELContext(), listener);
    }
    
    return SKIP_BODY;
  }
}
