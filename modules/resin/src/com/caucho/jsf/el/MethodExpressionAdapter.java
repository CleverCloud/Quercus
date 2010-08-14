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

public class MethodExpressionAdapter extends MethodExpression
{
  private final MethodBinding _expr;
  private final Class []_params;

  public MethodExpressionAdapter(MethodBinding expr, Class []params)
  {
    _expr = expr;
    _params = params;
  }

  public MethodBinding getBinding()
  {
    return _expr;
  }
  
  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }

  public boolean isLiteralText()
  {
    return false;
  }
  
  public MethodInfo getMethodInfo(ELContext elContext)
  {
    throw new UnsupportedOperationException();
  }

  public Object invoke(ELContext elContext, Object []args)
  {
    FacesContext facesContext
      = (FacesContext) elContext.getContext(FacesContext.class);
    
    return _expr.invoke(facesContext, args);
  }

  public int hashCode()
  {
    return _expr.getExpressionString().hashCode();
  }

  public boolean equals(Object o)
  {
    return this == o;
  }

  public String toString()
  {
    return "MethodExpressionAdapter[" + _expr.getExpressionString() + "]";
  }
}
