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

package com.caucho.jsf.taglib;

import java.io.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.event.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * The h:commandLink tag
 */
public class HtmlCommandLinkTag extends HtmlStyleBaseTag {
  private MethodExpression _action;
  
  private ActionListener _actionListener;

  private ValueExpression _immediate;

  public String getComponentType()
  {
    return HtmlCommandLink.COMPONENT_TYPE;
  }
  
  public String getRendererType()
  {
    return "javax.faces.Link";
  }

  public void setAction(MethodExpression expr)
  {
    _action = expr;
  }

  public MethodExpression getAction()
  {
    return _action;
  }

  public ValueExpression getImmediate()
  {
    return _immediate;
  }

  public void setImmediate(ValueExpression immediate)
  {
    _immediate = immediate;
  }

  public void setActionListener(MethodExpression expr)
  {
    _actionListener = new MethodExpressionActionListener(expr);
  }
  
  /**
   * Sets the overridden properties of the tag
   */
  @Override
  protected void setProperties(UIComponent component)
  {
    super.setProperties(component);

    UICommand command = (UICommand) component;

    if (_action != null)
      command.setActionExpression(_action);

    if (_immediate != null)
      component.setValueExpression("immediate", _immediate);

    if (_actionListener != null) {
      ActionListener actionListener = null;

      for (ActionListener listener: command.getActionListeners()) {
         if (_actionListener.equals(listener)) {
           actionListener = listener;

           break;
         }
      }

      if (actionListener == null)
        command.addActionListener(_actionListener);
    }
  }

  public void release()
  {

    this._actionListener = null;
    this._action = null;
    this._immediate = null;

    super.release();
  }
}
