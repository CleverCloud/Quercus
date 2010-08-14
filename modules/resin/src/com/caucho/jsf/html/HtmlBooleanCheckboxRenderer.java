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

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.render.*;

/**
 * The HTML selectboolen/checkbox renderer
 */
class HtmlBooleanCheckboxRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlBooleanCheckboxRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return false;
  }

  /**
   * Converts from the string value to a boolean value.
   */
  public Object getConvertedValue(FacesContext context,
                                  UIComponent component,
                                  Object submittedValue)
    throws ConverterException
  {
    return Boolean.valueOf((String) submittedValue);
  }

  /**
   * Decodes the data from the form.
   */
  @Override
  public void decode(FacesContext context, UIComponent component)
  {
    String clientId = component.getClientId(context);

    ExternalContext ext = context.getExternalContext();
    Map<String,String> paramMap = ext.getRequestParameterMap();

    String value = paramMap.get(clientId);

    if ("on".equalsIgnoreCase(value)
        || "true".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value)) {
      ((EditableValueHolder) component).setSubmittedValue("true");
    }
    else
      ((EditableValueHolder) component).setSubmittedValue("false");
  }
  
  /**
   * Renders the open tag for the text.
   */
  @Override
  public void encodeBegin(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();

    String id = component.getId();

    String accesskey;
    String dir;
    boolean disabled;
    String image;
    String lang;
    
    String onblur;
    String onchange;
    String onclick;
    String ondblclick;
    String onfocus;
    
    String onkeydown;
    String onkeypress;
    String onkeyup;
    
    String onmousedown;
    String onmousemove;
    String onmouseout;
    String onmouseover;
    String onmouseup;
    
    String onselect;

    boolean readonly;
    String style;
    String styleClass;
    String tabindex;
    String title;
    
    boolean selected;

    if (component instanceof HtmlSelectBooleanCheckbox) {
      HtmlSelectBooleanCheckbox htmlComponent = (HtmlSelectBooleanCheckbox) component;

      accesskey = htmlComponent.getAccesskey();
      dir = htmlComponent.getDir();
      disabled = htmlComponent.isDisabled();
      lang = htmlComponent.getLang();
      
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

      selected = htmlComponent.isSelected();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      accesskey = (String) attrMap.get("accesskey");
      dir = (String) attrMap.get("dir");
      disabled = Boolean.TRUE.equals(attrMap.get("disabled"));
      image = (String) attrMap.get("image");
      lang = (String) attrMap.get("lang");
      
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

      Object selectedValue = attrMap.get("selected");

      selected = Boolean.TRUE.equals(selectedValue);
    }

    out.startElement("input", component);
    
    out.writeAttribute("type", "checkbox", "type");
      
    out.writeAttribute("name", component.getClientId(context), "name");
    
    if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    if (accesskey != null)
      out.writeAttribute("accesskey", accesskey, "accesskey");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    if (disabled)
      out.writeAttribute("disabled", "disabled", "disabled");

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

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (tabindex != null)
      out.writeAttribute("tabindex", tabindex, "tabindex");

    if (title != null)
      out.writeAttribute("title", title, "title");

    if (selected)
      out.writeAttribute("checked", "checked", "checked");

    out.endElement("input");
  }

  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  public String toString()
  {
    return "HtmlInputTextRenderer[]";
  }
}
