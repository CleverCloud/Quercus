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
import javax.faces.model.*;

public class UISelectOne extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectOne";
  public static final String COMPONENT_TYPE = "javax.faces.SelectOne";
  public static final String INVALID_MESSAGE_ID
    = "javax.faces.component.UISelectOne.INVALID";

  public UISelectOne()
  {
    setRendererType("javax.faces.Menu");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  protected void validateValue(FacesContext context, Object value)
  {
    super.validateValue(context, value);

    if (! isValid() || value == null)
      return;

    boolean hasValue = matchChildren(context.getApplication().getExpressionFactory(), this, value);

    if (! hasValue) {
      String summary = Util.l10n(context, INVALID_MESSAGE_ID,
                                 "{0}: Validation Error: UISelectOne value '{1}' does not match a valid option.",
                                 Util.getLabel(context, this),
                                 value);

      String detail = summary;

      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail);

      context.addMessage(getClientId(context), msg);
      
      setValid(false);
    }
  }

  static boolean matchChildren(ExpressionFactory expressionFactory, UIComponent comp, Object value)
  {
    int count = comp.getChildCount();

    if (count <= 0)
      return false;

    List<UIComponent> children = comp.getChildren();

    Class type = value.getClass();

    for (int i = 0; i < count; i++) {
      UIComponent child = children.get(i);
      
      if (child instanceof UISelectItem) {
        UISelectItem item = (UISelectItem) child;

        SelectItem selectItem = (SelectItem) item.getValue();

        if (selectItem == null) {
          selectItem = new SelectItem(item.getItemValue());
        }
        
        Object optionValue;

        if (type != null) {
          optionValue = expressionFactory.coerceToType(selectItem.getValue(), type);
        }
        else {
          optionValue = selectItem.getValue();
        }
        
        if (value.equals(optionValue)) {
          return true;
        }
      }
      else if (child instanceof UISelectItems) {
        UISelectItems items = (UISelectItems) child;

        if (matchItems(expressionFactory, items.getValue(), value, type))
          return true;
      }
    }

    return false;
  }

  private static boolean matchItems(ExpressionFactory expressionFactory,
                                    Object selectValue, Object value,
                                    Class type)
  {
    if (selectValue instanceof SelectItemGroup) {
      SelectItem []items = ((SelectItemGroup) selectValue).getSelectItems();

      if (items != null) {
        for (int i = 0; i < items.length; i++) {
          if (matchItems(expressionFactory, items[i], value, type))
            return true;
        }
      }
    }
    else if (selectValue instanceof SelectItem) {
      SelectItem item = (SelectItem) selectValue;

      Object optionValue;

      if (type != null) {
        optionValue = expressionFactory.coerceToType(item.getValue(), type);
      }
      else {
        optionValue = item.getValue();
      }

      return value.equals(optionValue) && ! item.isDisabled();
    }
    else if (selectValue instanceof SelectItem[]) {
      SelectItem []item = (SelectItem[]) selectValue;

      for (int i = 0; i < item.length; i++) {

        Object optionValue;

        if (type != null) {
          optionValue = expressionFactory.coerceToType(item[i].getValue(), type);
        }
        else {
          optionValue = item[i].getValue();
        }

        if (value.equals(optionValue) && ! item[i].isDisabled())
          return true;
      }
    }
    else if (selectValue instanceof List) {
      List list = (List) selectValue;

      int size = list.size();
      for (int i = 0; i < size; i++) {
        if (matchItems(expressionFactory, list.get(i), value, type))
          return true;
      }
    }
    else if (selectValue instanceof Map) {
      Map map = (Map) selectValue;
      Collection collection = map.values();
      for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
        Object o = iterator.next();
        if (type != null) {
          o = expressionFactory.coerceToType(o, type);
        }
        if (value.equals(o)) return true;
      }
    }

    return false;
  }
}
