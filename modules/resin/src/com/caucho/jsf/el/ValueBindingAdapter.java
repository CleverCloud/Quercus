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

package com.caucho.jsf.el;

import javax.el.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.component.StateHolder;

public class ValueBindingAdapter extends ValueBinding implements StateHolder
{
  private ValueExpression _expr;
  private boolean _isTransient;

  public ValueBindingAdapter(ValueExpression expr)
  {
    _expr = expr;
  }

  public ValueBindingAdapter()
  {
  }

  @Deprecated
  public Object getValue(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    try {
      return _expr.getValue(context.getELContext());
    } catch (javax.el.PropertyNotFoundException e) {
      throw new javax.faces.el.PropertyNotFoundException(e);
    }
  }

  @Deprecated
  public void setValue(FacesContext context, Object value)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    try {
      _expr.setValue(context.getELContext(), value);
    } catch (javax.el.PropertyNotFoundException e) {
      throw new javax.faces.el.PropertyNotFoundException(e);
    }
  }

  @Deprecated
  public boolean isReadOnly(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    try {
      return _expr.isReadOnly(context.getELContext());
    } catch (javax.el.PropertyNotFoundException e) {
      throw new javax.faces.el.PropertyNotFoundException(e);
    }
  }

  @Deprecated
  public Class getType(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    try {
      return _expr.getType(context.getELContext());
    } catch (javax.el.PropertyNotFoundException e) {
      throw new javax.faces.el.PropertyNotFoundException(e);
    }
  }

  @Deprecated
  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }

  public Object saveState(FacesContext context)
  {
    return _expr;
  }

  public void restoreState(FacesContext context, Object state)
  {
    _expr = (ValueExpression) state;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public String toString()
  {
    return "ValueBindingAdapter[" + _expr.getExpressionString() + "]";
  }
}
