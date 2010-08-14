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

package javax.faces.component;

import java.util.*;

import javax.el.*;
import javax.faces.context.*;

public class UIParameter extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.Parameter";
  public static final String COMPONENT_TYPE = "javax.faces.Parameter";

  private String _name;
  private ValueExpression _nameExpr;

  private Object _value;
  private ValueExpression _valueExpr;

  public UIParameter()
  {
    setRendererType(null);
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // properties
  //

  public String getName()
  {
    if (_name != null)
      return _name;
    else if (_nameExpr != null)
      return Util.evalString(_nameExpr, getFacesContext());
    else
      return null;
  }

  public void setName(String value)
  {
    _name = value;
  }

  public Object getValue()
  {
    if (_value != null)
      return _value;
    else if (_valueExpr != null)
      return Util.eval(_valueExpr, getFacesContext());
    else
      return null;
  }

  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("name".equals(name))
      return _nameExpr;
    else if ("value".equals(name))
      return _valueExpr;
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("name".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _name = String.valueOf(expr.getValue(null));
        return;
      }
      else
        _nameExpr = expr;
    }
    else if ("value".equals(name)) {
      if (expr != null && expr.isLiteralText()) {
        _value = String.valueOf(expr.getValue(null));
        return;
      }
      else
        _valueExpr = expr;
    }

    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
    
      _name,
      _value,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _name = (String) state[1];
    _value = (String) state[2];
  }
}
