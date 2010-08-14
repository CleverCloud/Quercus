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
import javax.faces.model.*;

public class UISelectItem extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectItem";
  public static final String COMPONENT_TYPE = "javax.faces.SelectItem";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _itemDescription;
  private ValueExpression _itemDescriptionExpr;

  private Boolean _itemDisabled;
  private ValueExpression _itemDisabledExpr;

  private Boolean _itemEscaped;
  private ValueExpression _itemEscapedExpr;

  private String _itemLabel;
  private ValueExpression _itemLabelExpr;

  private Object _itemValue;
  private ValueExpression _itemValueExpr;

  private Object _value;
  private ValueExpression _valueExpr;

  public UISelectItem()
  {
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

  public String getItemDescription()
  {
    if (_itemDescription != null)
      return _itemDescription;
    else if (_itemDescriptionExpr != null)
      return Util.evalString(_itemDescriptionExpr, getFacesContext());
    else
      return null;
  }

  public void setItemDescription(String value)
  {
    _itemDescription = value;
  }

  public boolean isItemDisabled()
  {
    if (_itemDisabled != null)
      return _itemDisabled;
    else if (_itemDisabledExpr != null)
      return Util.evalBoolean(_itemDisabledExpr, getFacesContext());
    else
      return false;
  }

  public void setItemDisabled(boolean value)
  {
    _itemDisabled = value;
  }

  public boolean isItemEscaped()
  {
    if (_itemEscaped != null)
      return _itemEscaped;
    else if (_itemEscapedExpr != null)
      return Util.evalBoolean(_itemEscapedExpr, getFacesContext());
    else
      return true;
  }

  public void setItemEscaped(boolean value)
  {
    _itemEscaped = value;
  }

  public String getItemLabel()
  {
    if (_itemLabel != null)
      return _itemLabel;
    else if (_itemLabelExpr != null)
      return Util.evalString(_itemLabelExpr, getFacesContext());
    else
      return null;
  }

  public void setItemLabel(String value)
  {
    _itemLabel = value;
  }

  public Object getItemValue()
  {
    if (_itemValue != null)
      return _itemValue;
    else if (_itemValueExpr != null)
      return Util.eval(_itemValueExpr, getFacesContext());
    else
      return null;
  }

  public void setItemValue(Object value)
  {
    _itemValue = value;
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
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case ITEM_DESCRIPTION:
        return _itemDescriptionExpr;
      case ITEM_DISABLED:
        return _itemDisabledExpr;
      case ITEM_ESCAPED:
        return _itemEscapedExpr;
      case ITEM_LABEL:
        return _itemLabelExpr;
      case ITEM_VALUE:
        return _itemValueExpr;
      case VALUE:
        return _valueExpr;
      }
    }
    
    return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case ITEM_DESCRIPTION:
        if (expr != null && expr.isLiteralText()) {
          _itemDescription = String.valueOf(expr.getValue(null));
          return;
        }
        else
          _itemDescriptionExpr = expr;
        break;

      case ITEM_DISABLED:
        if (expr != null && expr.isLiteralText()) {
          _itemDisabled = Util.booleanValueOf(expr.getValue(null));
          return;
        }
        else
          _itemDisabledExpr = expr;
        break;

      case ITEM_ESCAPED:
        if (expr != null && expr.isLiteralText()) {
          _itemEscaped = Util.booleanValueOf(expr.getValue(null));
          return;
        }
        else
          _itemEscapedExpr = expr;
        break;

      case ITEM_LABEL:
        if (expr != null && expr.isLiteralText()) {
          _itemLabel = String.valueOf(expr.getValue(null));
          return;
        }
        else
          _itemLabelExpr = expr;
        break;

      case ITEM_VALUE:
        if (expr != null && expr.isLiteralText()) {
          _itemValue = expr.getValue(null);
          return;
        }
        else
          _itemValueExpr = expr;
        break;

      case VALUE:
        if (expr != null && expr.isLiteralText()) {
          _value = expr.getValue(null);
          return;
        }
        else
          _valueExpr = expr;
        break;
      }
    }
    
    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    Object []state = new Object[] {
      super.saveState(context),
      _itemDescription,
      _itemDisabled,
      _itemEscaped,
      _itemLabel,
      _itemValue,
      _value
    };

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _itemDescription = (String) state[1];
    _itemDisabled = (Boolean) state[2];
    _itemEscaped = (Boolean) state[3];
    _itemLabel = (String) state[4];
    _itemValue = state[5];
    _value = state[6];
  }

  private enum PropEnum {
    ITEM_DESCRIPTION,
    ITEM_DISABLED,
    ITEM_ESCAPED,
    ITEM_LABEL,
    ITEM_VALUE,
    VALUE,
  }

  static {
    _propMap.put("itemDescription", PropEnum.ITEM_DESCRIPTION);
    _propMap.put("itemDisabled", PropEnum.ITEM_DISABLED);
    _propMap.put("itemEscaped", PropEnum.ITEM_ESCAPED);
    _propMap.put("itemLabel", PropEnum.ITEM_LABEL);
    _propMap.put("itemValue", PropEnum.ITEM_VALUE);
    _propMap.put("value", PropEnum.VALUE);
  }
}
