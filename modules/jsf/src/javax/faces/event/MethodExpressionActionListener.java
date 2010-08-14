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

package javax.faces.event;

import javax.el.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;

public class MethodExpressionActionListener
  implements ActionListener, StateHolder
{
  private MethodExpression _expr;
  private boolean _isTransient;

  public MethodExpressionActionListener()
  {
  }

  public MethodExpressionActionListener(MethodExpression expr)
  {
    _expr = expr;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public void processAction(ActionEvent actionEvent)
  {
    FacesContext context = FacesContext.getCurrentInstance();

    _expr.invoke(context.getELContext(), new Object[] { actionEvent });
  }

  public Object saveState(FacesContext context)
  {
    if (_expr != null)
      return _expr.getExpressionString();
    else
      return null;
  }

  public void restoreState(FacesContext context, Object state)
  {
    if (state != null) {
      String expr = (String) state;
      
      Application app = context.getApplication();
      ExpressionFactory factory = app.getExpressionFactory();

      _expr = factory.createMethodExpression(context.getELContext(),
                                             expr,
                                             void.class,
                                             new Class[] { ActionEvent.class });
    }
  }
}
