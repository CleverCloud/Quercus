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
 * @author Alex Rojkov
 */

package com.caucho.jsf.event;

import javax.faces.event.ActionListener;
import javax.faces.event.ActionEvent;
import javax.faces.event.AbortProcessingException;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.el.ValueExpression;
import javax.el.ELContext;

public class SetPropertyActionListener
  implements ActionListener, StateHolder
{

  private boolean _transient;

  private ValueExpression _value;

  private ValueExpression _target;

  public ValueExpression getValue()
  {
    return _value;
  }

  public void setValue(ValueExpression value)
  {
    _value = value;
  }

  public ValueExpression getTarget()
  {
    return _target;
  }

  public void setTarget(ValueExpression target)
  {
    _target = target;
  }

  public void processAction(ActionEvent event)
    throws AbortProcessingException
  {
    FacesContext context = FacesContext.getCurrentInstance();

    ELContext elContext = context.getELContext();

    Object value = _value.getValue(elContext);

    _target.setValue(elContext, value);
  }

  public Object saveState(FacesContext context)
  {
    return new Object []{
      _value,
      _target,
    };
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object []) state;

    _value = (ValueExpression) values [0];
    _target = (ValueExpression) values [1];
  }

  public boolean isTransient()
  {
    return _transient;
  }

  public void setTransient(boolean isTransient)
  {
    _transient = isTransient;
  }
}
