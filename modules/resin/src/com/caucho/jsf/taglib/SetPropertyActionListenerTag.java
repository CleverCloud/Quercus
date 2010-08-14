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
 * @author Alex Rojkov
 */

package com.caucho.jsf.taglib;

import com.caucho.jsf.event.SetPropertyActionListener;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.event.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public class SetPropertyActionListenerTag
  extends TagSupport
{
  private ValueExpression _value;
  private ValueExpression _target;

  public void setValue(ValueExpression value)
  {
    _value = value;
  }

  public void setTarget(ValueExpression target)
  {
    _target = target;
  }

  public int doStartTag()
    throws JspException
  {
    UIComponentClassicTagBase parent;

    parent
      = UIComponentClassicTagBase.getParentUIComponentClassicTagBase(this.pageContext);

    if (parent == null)
      throw new JspException(
        "f:setPropertyActionListener must be nested iside a UIComponent tag.");

    UIComponent comp = parent.getComponentInstance();

    if (parent.getCreated()) {
      if (!(comp instanceof ActionSource))
        throw new JspException(
          "f:valueChangeListener parent must be an ActionSource.");

      ActionSource actionSource = (ActionSource) comp;

      ActionListener actionListener = createListener();

      actionSource.addActionListener(actionListener);
    }

    return SKIP_BODY;
  }

  protected ActionListener createListener()
  {
    SetPropertyActionListener listener = new SetPropertyActionListener();

    listener.setValue(_value);
    listener.setTarget(_target);

    return listener;
  }
}