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
import javax.faces.el.ValueBinding;

public class UISelectBoolean extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectBoolean";
  public static final String COMPONENT_TYPE = "javax.faces.SelectBoolean";

  public UISelectBoolean()
  {
    setRendererType("javax.faces.Checkbox");
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

  public boolean isSelected()
  {
    Object value = getValue();

    return Util.booleanValueOf(value);
  }

  public void setSelected(boolean value)
  {
    setValue(value);
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("selected".equals(name))
      return super.getValueExpression("value");
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("selected".equals(name))
      super.setValueExpression("value", expr);
    else
      super.setValueExpression(name, expr);
  }

  /**
   * Returns the value binding with the given name.
   */
  @Override
  public ValueBinding getValueBinding(String name)
  {
    if ("selected".equals(name))
      return super.getValueBinding("value");
    else
      return super.getValueBinding(name);
  }

  /**
   * Sets the value binding with the given name.
   */
  @Override
  public void setValueBinding(String name, ValueBinding expr)
  {
    if ("selected".equals(name))
      super.setValueBinding("value", expr);
    else
      super.setValueBinding(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return super.saveState(context);
  }

  public void restoreState(FacesContext context, Object value)
  {
    super.restoreState(context, value);
  }
}
