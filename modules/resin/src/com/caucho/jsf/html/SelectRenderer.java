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

package com.caucho.jsf.html;

import com.caucho.util.Html;

import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.model.*;
import javax.faces.convert.*;
import javax.el.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * The base renderer
 */
abstract class SelectRenderer extends BaseRenderer
{
  private static Map<Class, Class> _primitiveTypeMap
    = new HashMap<Class, Class>();

  @Override
  public Object getConvertedValue(FacesContext context,
                                  UIComponent component,
                                  Object submittedValue)
    throws ConverterException
  {
    if (component instanceof UISelectMany) {
      return getConvertedValue(context, (UISelectMany)component, submittedValue);
    }
    return super.getConvertedValue(context, component, submittedValue);
  }

  private Object getConvertedValue(FacesContext context,
                                   UISelectMany uiSelectMany,
                                   Object submittedValue)
    throws ConverterException
  {
    String []strValues = (String[]) submittedValue;

    Converter converter = uiSelectMany.getConverter();

    Object []values = null;

    ValueExpression valueExpr = uiSelectMany.getValueExpression("value");

    if (valueExpr != null) {
      Class cl;
      cl = valueExpr.getType(context.getELContext());
      if (cl.isArray()) {
        cl = cl.getComponentType();
        if (cl.isPrimitive()) {
          cl = _primitiveTypeMap.get(cl);
        }
        converter = context.getApplication().createConverter(cl);
        values = (Object []) Array.newInstance(cl, strValues.length);
      }
      else if (java.util.List.class.isAssignableFrom(cl)) {
        values = strValues;

        return values;
      }
      else {
        //todo should never happen as per spec. should an exception be thrown?
      }
    }
    else {
      values = new Object[strValues.length];
    }

    for (int i = 0; i < strValues.length; i++) {
      if (converter != null) {
        values[i] = converter.getAsObject(context, uiSelectMany, strValues[i]);
      }
      else {
        values[i] = strValues[i];
      }
    }

    return values;
  }

  public List<SelectItem> accrueSelectItems(UIComponent component)
  {
    ArrayList<SelectItem> itemList = new ArrayList<SelectItem>();

    int count = component.getChildCount();
    if (count == 0)
      return itemList;

    List<UIComponent> children = component.getChildren();

    for (int i = 0; i < count; i++) {
      UIComponent child = children.get(i);

      if (child instanceof UISelectItem) {
        UISelectItem uiSelectItem = (UISelectItem) child;

        SelectItem item = (SelectItem) uiSelectItem.getValue();

        if (item == null) {
          item = new SelectItem(uiSelectItem.getItemValue(),
                                uiSelectItem.getItemLabel(),
                                uiSelectItem.getItemDescription(),
                                uiSelectItem.isItemDisabled(),
                                uiSelectItem.isItemEscaped());
        }

        itemList.add(item);
      }
      else if (child instanceof UISelectItems) {
        UISelectItems selectedItems = (UISelectItems) child;

        Object value = selectedItems.getValue();

        if (value instanceof SelectItem) {
          itemList.add((SelectItem) value);
        }
        else if (value instanceof SelectItem []) {
          SelectItem []items = (SelectItem []) value;

          itemList.ensureCapacity(itemList.size() + items.length);

          for (SelectItem item : items) {
            itemList.add(item);
          }
        }
        else if (value instanceof Collection) {
          Collection items = (Collection) value;

          itemList.ensureCapacity(itemList.size() + items.size());

          itemList.addAll(items);
        }
        else if (value instanceof Map) {
          Map map = (Map) value;

          itemList.ensureCapacity(itemList.size() + map.size());

          Set<Map.Entry> entries = map.entrySet();
          for (Map.Entry entry : entries) {
            itemList.add(new SelectItem(entry.getValue(),
                                        String.valueOf(entry.getKey())));
          }
        }
      }
    }
    return itemList;
  }

  protected void encodeChildren(ResponseWriter out,
                              FacesContext context,
                              UIComponent component,
                              Object []values,
                              String enabledClass,
                              String disabledClass)
    throws IOException
  {

    List<SelectItem> list = accrueSelectItems(component);
    
    for (int i = 0; i < list.size(); i++) {
      SelectItem selectItem = list.get(i);

      out.startElement("option", component);

      // jsf/31c4
      /*
      out.writeAttribute("id", childId, "id");
      //out.writeAttribute("name", child.getClientId(context), "name");
      */

      if (selectItem.isDisabled()) {
        out.writeAttribute("disabled", "disabled", "disabled");

        if (disabledClass != null)
          out.writeAttribute("class", disabledClass, "disabledClass");
      }
      else {
        if (enabledClass != null)
          out.writeAttribute("class", enabledClass, "enabledClass");
      }

      if (values != null) {
        for (int j = 0; j < values.length; j++) {
          if (values[j].equals(selectItem.getValue())) {
            out.writeAttribute("selected", "selected", "selected");
            break;
          }
        }
      }

      out.writeAttribute("value",
                         String.valueOf(selectItem.getValue()),
                         "value");

      String label = selectItem.getLabel();


      if (label != null) {
        if (selectItem.isEscape())
          label = Html.escapeHtml(label);

        out.writeText(label, "label");
      }

      out.endElement("option");
      out.write("\n");

    }
  }

  protected void encodeOneChildren(ResponseWriter out,
                                   FacesContext context,
                                   UIComponent component,
                                   Object value,
                                   String enabledClass,
                                   String disabledClass)
    throws IOException
  {
    String clientId = component.getClientId(context);

    ValueExpression ve = component.getValueExpression("value");

    Class type = null;
    if (ve != null) {
      type = ve.getType(context.getELContext());
    }

    List<SelectItem> items = accrueSelectItems(component);
    for (int i = 0; i < items.size(); i++) {
      String childId = clientId + ":" + i;

      SelectItem selectItem = items.get(i);

      String itemLabel = selectItem.getLabel();
      Object itemValue = selectItem.getValue();

      out.startElement("option", component);

      // jsf/31c4
      /*
      out.writeAttribute("id", childId, "id");
      //out.writeAttribute("name", child.getClientId(context), "name");
      */

      Object optionValue;
       if (type != null) {
          optionValue = context.getApplication()
            .getExpressionFactory()
            .coerceToType(itemValue, type);
        }
        else {
          optionValue = selectItem.getValue();
        }

      if (value != null && value.equals(optionValue))
        out.writeAttribute("selected", "selected", "selected");

      if (selectItem.isDisabled()) {
        out.writeAttribute("disabled", "disabled", "disabled");

        if (disabledClass != null)
          out.writeAttribute("class", disabledClass, "disabledClass");
      }
      else {
        if (enabledClass != null)
          out.writeAttribute("class", enabledClass, "enabledClass");
      }

      String itemValueString = toString(context, component, itemValue);
      out.writeAttribute("value", itemValueString, "value");

      if (itemLabel == null)
        itemLabel = itemValueString;

      if (selectItem.isEscape())
        itemLabel = Html.escapeHtml(itemLabel);

      out.writeText(itemLabel, "label");

      out.endElement("option");
      out.write("\n");
    }
  }

  static {
    _primitiveTypeMap.put(boolean.class, Boolean.class);
    _primitiveTypeMap.put(byte.class, Byte.class);
    _primitiveTypeMap.put(char.class, Character.class);
    _primitiveTypeMap.put(short.class, Short.class);
    _primitiveTypeMap.put(int.class, Integer.class);
    _primitiveTypeMap.put(long.class, Long.class);
    _primitiveTypeMap.put(float.class, Float.class);
    _primitiveTypeMap.put(double.class, Double.class);
  }
}
