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
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

public class UISelectMany extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectMany";
  public static final String COMPONENT_TYPE = "javax.faces.SelectMany";
  public static final String INVALID_MESSAGE_ID
    = "javax.faces.component.UISelectMany.INVALID";

  public UISelectMany()
  {
    setRendererType("javax.faces.Listbox");
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

  public Object []getSelectedValues()
  {
    Object value = super.getValue();

    if (value instanceof String)
      return new Object[] { value };
    else
      return (Object []) value;
  }

  public void setSelectedValues(Object []value)
  {
    super.setValue(value);
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("selectedValues".equals(name))
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
    if ("selectedValues".equals(name)) {
      super.setValueExpression("value", expr);
    }
    else
      super.setValueExpression(name, expr);
  }

  //
  // validate
  //

  protected void validateValue(FacesContext context, Object value)
  {
    super.validateValue(context, value);
    
    if (! isValid())
      return;

    boolean hasValue = false;
    
    ExpressionFactory exprFactory
      = context.getApplication().getExpressionFactory();

    if (value instanceof Object[]) {
      Object[] values = (Object[]) value;

      for (int i = 0; i < values.length; i++) {
        hasValue = UISelectOne.matchChildren(exprFactory, this, values[i]);

        if (!hasValue)
          break;
      }
    }

    if (! hasValue) {
      setValid(false);
      
      String summary = Util.l10n(context, INVALID_MESSAGE_ID,
                                 "{0}: Validation Error: UISelectMany value {1} is not valid.",
                                 Util.getLabel(context, this),
                                 value == null? null: Arrays.asList((Object[])value));

      String detail = summary;

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail);

      context.addMessage(getClientId(context), msg);
    }
  }

  @Override
  protected boolean compareValues(Object oldValue, Object newValue)
  {
    if (oldValue == newValue)
      return false;
    else if (oldValue == null || newValue == null)
      return true;

    Object []oldValues = (Object []) oldValue;
    Object []newValues = (Object []) newValue;

    if (oldValues.length != newValues.length)
      return true;

    for (int i = 0; i < oldValues.length; i++) {
      if (! oldValues[i].equals(newValues[i]))
        return true;
    }

    return false;
  }
}
