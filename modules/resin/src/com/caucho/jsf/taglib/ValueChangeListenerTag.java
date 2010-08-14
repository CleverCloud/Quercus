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
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.el.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class ValueChangeListenerTag extends TagSupport
{
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
    UIComponentClassicTagBase parent;
    
    parent = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException("f:valueChangeListener must be nested inside a UIComponent tag.");

    UIComponent comp = parent.getComponentInstance();

    if (parent.getCreated()) {
      if (! (comp instanceof EditableValueHolder))
        throw new JspException("f:valueChangeListener parent of validator must be a EditableValueHolder.");

      EditableValueHolder valueHolder = (EditableValueHolder) comp;

      ValueChangeListener listener = createListener();

      valueHolder.addValueChangeListener(listener);
    }
    
    return SKIP_BODY;
  }

  protected ValueChangeListener createListener()
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();

    ValueChangeListener listener = null;

    ELContext elContext = context.getELContext();
      
    if (_binding != null)
      listener = (ValueChangeListener) _binding.getValue(elContext);

    if (listener == null && _type != null) {
      String type = (String) _type.getValue(elContext);
      
      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class cl = Class.forName(type, false, loader);

        listener = (ValueChangeListener) cl.newInstance();
      } catch (Exception e) {
        throw new JspException(e);
      }

      if (_binding != null)
        _binding.setValue(elContext, listener);
    }

    return listener;
  }
}
