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
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.el.*;

public class MethodBindingAdapter extends MethodBinding implements StateHolder
{
  private MethodExpression _expr;
  private Class []_param;

  public MethodBindingAdapter()
  {
  }

  public MethodBindingAdapter(MethodExpression expr, Class []param)
  {
    _expr = expr;
    _param = param;
  }

  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }
  
  @Deprecated
  public Object invoke(FacesContext context, Object []param)
    throws EvaluationException, javax.faces.el.MethodNotFoundException
  {
    if (context == null)
      throw new NullPointerException();
    
    try {
      return _expr.invoke(context.getELContext(), param);
    } catch (javax.el.MethodNotFoundException e) {
      throw new javax.faces.el.MethodNotFoundException(e);
    } catch (ELException e) {
      if (e.getCause() != null)
        throw new EvaluationException(e.getCause());
      else
        throw new EvaluationException(e);
    } catch (Exception e) {
      throw new EvaluationException(e);
    }
  }

  @Deprecated
  public Class getType(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    try {
      MethodInfo info = _expr.getMethodInfo(context.getELContext());

      return info.getReturnType();
    } catch (javax.el.MethodNotFoundException e) {
      throw new javax.faces.el.MethodNotFoundException(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EvaluationException(e);
    }
  }
  
  public Object saveState(FacesContext context)
  {
    ELContext elContext = context.getELContext();
    
    String expr = _expr.getExpressionString();

    return new Object[] { expr, _param };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;
    
    Application app = context.getApplication();
    ELContext elContext = context.getELContext();
    ExpressionFactory factory = app.getExpressionFactory();

    String expr = (String) state[0];
    Class []param = (Class []) state[1];

    _expr = factory.createMethodExpression(context.getELContext(),
                                           expr, Object.class, param);
    _param = param;
  }

  public boolean isTransient()
  {
    return false;
  }

  public void setTransient(boolean isTransient)
  {
  }

  public String toString()
  {
    return "MethodBindingAdapter[" + _expr.getExpressionString() + "]";
  }
}
