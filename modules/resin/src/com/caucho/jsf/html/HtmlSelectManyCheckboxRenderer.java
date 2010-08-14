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

import java.io.*;
import java.util.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;
import javax.faces.model.SelectItemGroup;
import javax.faces.model.SelectItem;

/**
 * The HTML selectMany/checkbox renderer
 */
class HtmlSelectManyCheckboxRenderer
  extends SelectRenderer
{
  public static final Renderer RENDERER
    = new HtmlSelectManyCheckboxRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
  }

  /**
   * Decodes the data from the form.
   */
  @Override
  public void decode(FacesContext context, UIComponent component)
  {
    String clientId = component.getClientId(context);

    ExternalContext ext = context.getExternalContext();
    Map<String, String []> paramMap = ext.getRequestParameterValuesMap();

    String  []value = paramMap.get(clientId);

    if (value != null)
      ((EditableValueHolder) component).setSubmittedValue(value);
    else
      ((EditableValueHolder) component).setSubmittedValue(new String []{});
  }

  /**
   * Renders the open tag for the text.
   */
  @Override
  public void encodeBegin(FacesContext context, final UIComponent component)
    throws IOException
  {
    final ResponseWriter out = context.getResponseWriter();

    final int border;
    final boolean disabled;
    final String layout;

    final String style;
    final String styleClass;

    if (component instanceof HtmlSelectManyCheckbox) {
      HtmlSelectManyCheckbox htmlComponent
        = (HtmlSelectManyCheckbox) component;

      border = htmlComponent.getBorder();
      disabled = htmlComponent.isDisabled();
      layout = htmlComponent.getLayout();

      style = htmlComponent.getStyle();
      styleClass = htmlComponent.getStyleClass();

    }
    else {
      Map<String, Object> attrMap = component.getAttributes();

      Integer iValue = (Integer) attrMap.get("border");

      border = iValue != null ? iValue : 0;

      disabled = Boolean.TRUE.equals(attrMap.get("disabled"));
      layout = (String) attrMap.get("layout");

      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");

    }

    out.startElement("table", component);

    if (border > 0)
      out.writeAttribute("border", border, "border");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (disabled)
      out.writeAttribute("disabled", "disabled", "disabled");

    out.write("\n");

    if (!"pageDirection".equals(layout)) {
      out.startElement("tr", component);
      out.write("\n");
    }
  }

  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
    final ResponseWriter out = context.getResponseWriter();

    String id = component.getId();

    final String accesskey;
    final int border;
    final String dir;
    final boolean disabled;
    final String disabledClass;
    final String enabledClass;
    final String lang;
    final String layout;

    final String onblur;
    final String onchange;
    final String onclick;
    final String ondblclick;
    final String onfocus;

    final String onkeydown;
    final String onkeypress;
    final String onkeyup;

    final String onmousedown;
    final String onmousemove;
    final String onmouseout;
    final String onmouseover;
    final String onmouseup;

    final String onselect;

    final boolean readonly;
    final String style;
    final String styleClass;
    final String tabindex;
    final String title;
    final Object value;

    if (component instanceof HtmlSelectManyCheckbox) {
      HtmlSelectManyCheckbox htmlComponent
        = (HtmlSelectManyCheckbox) component;

      accesskey = htmlComponent.getAccesskey();
      border = htmlComponent.getBorder();
      dir = htmlComponent.getDir();
      disabled = htmlComponent.isDisabled();
      disabledClass = htmlComponent.getDisabledClass();
      enabledClass = htmlComponent.getEnabledClass();
      lang = htmlComponent.getLang();
      layout = htmlComponent.getLayout();

      onblur = htmlComponent.getOnblur();
      onchange = htmlComponent.getOnchange();
      onclick = htmlComponent.getOnclick();
      ondblclick = htmlComponent.getOndblclick();
      onfocus = htmlComponent.getOnfocus();

      onkeydown = htmlComponent.getOnkeydown();
      onkeypress = htmlComponent.getOnkeypress();
      onkeyup = htmlComponent.getOnkeyup();

      onmousedown = htmlComponent.getOnmousedown();
      onmousemove = htmlComponent.getOnmousemove();
      onmouseout = htmlComponent.getOnmouseout();
      onmouseover = htmlComponent.getOnmouseover();
      onmouseup = htmlComponent.getOnmouseup();

      onselect = htmlComponent.getOnselect();

      readonly = htmlComponent.isReadonly();
      style = htmlComponent.getStyle();
      styleClass = htmlComponent.getStyleClass();
      tabindex = htmlComponent.getTabindex();
      title = htmlComponent.getTitle();

      value = htmlComponent.getValue();
    }
    else {
      Map<String, Object> attrMap = component.getAttributes();

      Integer iValue;

      accesskey = (String) attrMap.get("accesskey");

      iValue = (Integer) attrMap.get("border");
      border = iValue != null ? iValue.intValue() : 0;

      dir = (String) attrMap.get("dir");
      disabled = Boolean.TRUE.equals(attrMap.get("disabled"));
      disabledClass = (String) attrMap.get("disabledClass");
      enabledClass = (String) attrMap.get("enabledClass");
      lang = (String) attrMap.get("lang");
      layout = (String) attrMap.get("layout");

      onblur = (String) attrMap.get("onblur");
      onchange = (String) attrMap.get("onchange");
      onclick = (String) attrMap.get("onclick");
      ondblclick = (String) attrMap.get("ondblclick");
      onfocus = (String) attrMap.get("onfocus");

      onkeydown = (String) attrMap.get("onkeydown");
      onkeypress = (String) attrMap.get("onkeypress");
      onkeyup = (String) attrMap.get("onkeyup");

      onmousedown = (String) attrMap.get("onmousedown");
      onmousemove = (String) attrMap.get("onmousemove");
      onmouseout = (String) attrMap.get("onmouseout");
      onmouseover = (String) attrMap.get("onmouseover");
      onmouseup = (String) attrMap.get("onmouseup");

      onselect = (String) attrMap.get("onselect");

      readonly = Boolean.TRUE.equals(attrMap.get("readonly"));
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      tabindex = (String) attrMap.get("tabindex");
      title = (String) attrMap.get("title");

      value = attrMap.get("value");
    }

    final String clientId = component.getClientId(context);

    List<SelectItem> list = super.accrueSelectItems(component);
    int counter = 0;
    for (int i = 0; i < list.size(); i++) {
      SelectItem selectItem = list.get(i);
      if (selectItem instanceof SelectItemGroup) {

        if ("pageDirection".equals(layout)) {
          out.startElement("tr", component);
          out.write("\n");
        }

        out.startElement("td", component);

        out.write("\n");

        out.startElement("table", component);

        if (border > 0)
          out.writeAttribute("border", border, "border");

        if (style != null)
          out.writeAttribute("style", style, "style");

        if (styleClass != null)
          out.writeAttribute("class", styleClass, "class");

        if (disabled)
          out.writeAttribute("disabled", "disabled", "disabled");

        out.write("\n");

        if (!"pageDirection".equals(layout)) {
          out.startElement("tr", component);
          out.write("\n");
        }

        SelectItem []items = ((SelectItemGroup) selectItem).getSelectItems();

        for (int j = 0; j < items.length; j++) {

          SelectItem item = items[j];

          encodeChild(out,
                      component,
                      value,
                      item,
                      clientId,
                      layout,
                      accesskey,
                      disabled,
                      dir,
                      lang,
                      onblur,
                      onchange,
                      onclick,
                      ondblclick,
                      onfocus,
                      onkeydown,
                      onkeypress,
                      onkeyup,
                      onmousedown,
                      onmousemove,
                      onmouseout,
                      onmouseover,
                      onmouseup,
                      onselect,
                      readonly,
                      tabindex,
                      title,
                      disabledClass,
                      enabledClass,
                      counter++);

        }

        if (!"pageDirection".equals(layout)) {
          out.endElement("tr");
          out.write("\n");
        }

        out.endElement("table");
        out.write("\n");

        out.endElement("td");
        out.write("\n");

        if ("pageDirection".equals(layout)) {
          out.endElement("tr");
          out.write("\n");
        }

      }
      else {
        encodeChild(out,
                    component,
                    value,
                    selectItem,
                    clientId,
                    layout,
                    accesskey,
                    disabled,
                    dir,
                    lang,
                    onblur,
                    onchange,
                    onclick,
                    ondblclick,
                    onfocus,
                    onkeydown,
                    onkeypress,
                    onkeyup,
                    onmousedown,
                    onmousemove,
                    onmouseout,
                    onmouseover,
                    onmouseup,
                    onselect,
                    readonly,
                    tabindex,
                    title,
                    disabledClass,
                    enabledClass,
                    counter++);
      }
    }
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
    final ResponseWriter out = context.getResponseWriter();

    final String layout;

    if (component instanceof HtmlSelectManyCheckbox) {
      layout = ((HtmlSelectManyCheckbox) component).getLayout();
    }
    else {
      layout = (String) component.getAttributes().get("layout");
    }
    if (!"pageDirection".equals(layout)) {
      out.endElement("tr");
      out.write("\n");
    }

    out.endElement("table");
    out.write("\n");

    for (UIComponent child : component.getChildren()) {
      if (child instanceof UIOutput)
        child.encodeAll(context);      
    }
  }

  private void encodeChild(ResponseWriter out,
                           UIComponent component,
                           Object value,
                           SelectItem selectItem,
                           String clientId,
                           String layout,
                           String accesskey,
                           boolean disabled,
                           String dir,
                           String lang,
                           String onblur,
                           String onchange,
                           String onclick,
                           String ondblclick,
                           String onfocus,
                           String onkeydown,
                           String onkeypress,
                           String onkeyup,
                           String onmousedown,
                           String onmousemove,
                           String onmouseout,
                           String onmouseover,
                           String onmouseup,
                           String onselect,
                           boolean readonly,
                           String tabindex,
                           String title,
                           String disabledClass,
                           String enabledClass,
                           int counter)
    throws IOException
  {
    String childId = clientId + ":" + counter;
    
    if ("pageDirection".equals(layout)) {
      out.startElement("tr", component);
      out.write("\n");
    }

    out.startElement("td", component);

    out.startElement("input", component);
    out.writeAttribute("id", childId, "id");
    out.writeAttribute("name", clientId, "name");
    out.writeAttribute("type", "checkbox", "type");

    if (selectItem.isDisabled() || disabled)
      out.writeAttribute("disabled", "disabled", "disabled");


    if (value instanceof Object []) {
      Object []values = (Object []) value;

      for (int j = 0; j < values.length; j++) {
        if (values[j].equals(selectItem.getValue())) {
          out.writeAttribute("checked", "checked", "value");
          break;
        }
      }
      
    }

    if (accesskey != null)
      out.writeAttribute("accesskey", accesskey, "accesskey");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    if (lang != null)
      out.writeAttribute("lang", lang, "lang");

    if (onblur != null)
      out.writeAttribute("onblur", onblur, "onblur");

    if (onchange != null)
      out.writeAttribute("onchange", onchange, "onchange");

    if (onclick != null)
      out.writeAttribute("onclick", onclick, "onclick");

    if (ondblclick != null)
      out.writeAttribute("ondblclick", ondblclick, "ondblclick");

    if (onfocus != null)
      out.writeAttribute("onfocus", onfocus, "onfocus");

    if (onkeydown != null)
      out.writeAttribute("onkeydown", onkeydown, "onkeydown");

    if (onkeypress != null)
      out.writeAttribute("onkeypress", onkeypress, "onkeypress");

    if (onkeyup != null)
      out.writeAttribute("onkeyup", onkeyup, "onkeyup");

    if (onmousedown != null)
      out.writeAttribute("onmousedown", onmousedown, "onmousedown");

    if (onmousemove != null)
      out.writeAttribute("onmousemove", onmousemove, "onmousemove");

    if (onmouseout != null)
      out.writeAttribute("onmouseout", onmouseout, "onmouseout");

    if (onmouseover != null)
      out.writeAttribute("onmouseover", onmouseover, "onmouseover");

    if (onmouseup != null)
      out.writeAttribute("onmouseup", onmouseup, "onmouseup");

    if (onselect != null)
      out.writeAttribute("onselect", onselect, "onselect");

    if (readonly)
      out.writeAttribute("readonly", "readonly", "readonly");

    if (tabindex != null)
      out.writeAttribute("tabindex", tabindex, "tabindex");

    if (title != null)
      out.writeAttribute("title", title, "title");

    Object itemValue = selectItem.getValue();
    if (itemValue != null)
      out.writeAttribute("value", String.valueOf(itemValue), "value");

    out.endElement("input");

    String itemLabel = selectItem.getLabel();

    if (itemLabel != null) {
      out.startElement("label", component);
      out.writeAttribute("for", childId, "for");

      if (selectItem.isDisabled() || disabled) {
        if (disabledClass != null)
          out.writeAttribute("class", disabledClass, "disabledClass");
      }
      else {
        if (enabledClass != null)
          out.writeAttribute("class", enabledClass, "enabledClass");
      }

      if (selectItem.isEscape())
        itemLabel = Html.escapeHtml(itemLabel);

      out.writeText(itemLabel, "itemLabel");

      out.endElement("label");
    }

    out.endElement("td");
    out.write("\n");

    if ("pageDirection".equals(layout)) {
      out.endElement("tr");
      out.write("\n");
    }
    
  }

  public String toString()
  {
    return "HtmlInputTextRenderer []";
  }
}
