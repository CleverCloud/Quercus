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
import java.util.logging.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.render.*;

/**
 * The HTML command/button renderer
 */
class HtmlCommandButtonRenderer extends Renderer
{
  private static final Logger log
    = Logger.getLogger(HtmlCommandButtonRenderer.class.getName());
  
  public static final Renderer RENDERER = new HtmlCommandButtonRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return false;
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

    if (value != null) {
      String type;

      if (component instanceof HtmlCommandButton) {
        HtmlCommandButton htmlComp = (HtmlCommandButton) component;

        type = htmlComp.getType();
      }
      else {
        Map<String,Object> attrMap = component.getAttributes();
    
        type = (String) attrMap.get("type");
      }

      if ("reset".equals(type))
        return;

      if (log.isLoggable(Level.FINE))
        log.fine(component + " action " + type);

      ActionEvent event = new ActionEvent(component);

      component.queueEvent(event);
    }
    else {
      String valueX = paramMap.get(clientId + ".x");
      String valueY = paramMap.get(clientId + ".y");
      
      if (valueX != null || valueY != null) {
        if (log.isLoggable(Level.FINE))
          log.fine(component + " action [" + valueX + "," + valueY + "]");

        ActionEvent event = new ActionEvent(component);

        component.queueEvent(event);
      }
    }
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
    String alt;
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
    String target;
    String title;
    String type;
    Object value;
    
    if (component instanceof HtmlCommandButton) {
      HtmlCommandButton htmlCommandButton = (HtmlCommandButton) component;

      accesskey = htmlCommandButton.getAccesskey();
      alt = htmlCommandButton.getAlt();
      dir = htmlCommandButton.getDir();
      disabled = htmlCommandButton.isDisabled();
      image = htmlCommandButton.getImage();
      lang = htmlCommandButton.getLang();
      
      onblur = htmlCommandButton.getOnblur();
      onchange = htmlCommandButton.getOnchange();
      onclick = htmlCommandButton.getOnclick();
      ondblclick = htmlCommandButton.getOndblclick();
      onfocus = htmlCommandButton.getOnfocus();
      
      onkeydown = htmlCommandButton.getOnkeydown();
      onkeypress = htmlCommandButton.getOnkeypress();
      onkeyup = htmlCommandButton.getOnkeyup();
      
      onmousedown = htmlCommandButton.getOnmousedown();
      onmousemove = htmlCommandButton.getOnmousemove();
      onmouseout = htmlCommandButton.getOnmouseout();
      onmouseover = htmlCommandButton.getOnmouseover();
      onmouseup = htmlCommandButton.getOnmouseup();
      
      onselect = htmlCommandButton.getOnselect();
      
      readonly = htmlCommandButton.isReadonly();
      style = htmlCommandButton.getStyle();
      styleClass = htmlCommandButton.getStyleClass();
      tabindex = htmlCommandButton.getTabindex();
      title = htmlCommandButton.getTitle();
      type = htmlCommandButton.getType();

      value = htmlCommandButton.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      accesskey = (String) attrMap.get("accesskey");
      alt = (String) attrMap.get("alt");
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
      type = (String) attrMap.get("type");
      
      value = attrMap.get("value");
    }

    out.startElement("input", component);
    if (image != null && ! "".equals(image)) {
      out.writeAttribute("type", "image", "type");

      ViewHandler view = context.getApplication().getViewHandler();
      
      String src = view.getResourceURL(context, image);
      
      ExternalContext extContext = context.getExternalContext();

      out.writeAttribute("src", extContext.encodeActionURL(src), "src");
    }
    else {
      if ("reset".equals(type))
        out.writeAttribute("type", "reset", "type");
      else
        out.writeAttribute("type", "submit", "type");

      if (value != null)
        out.writeAttribute("value", String.valueOf(value), "value");
    }
      
    out.writeAttribute("name", component.getClientId(context), "name");
    
    if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    if (accesskey != null)
      out.writeAttribute("accesskey", accesskey, "accesskey");

    if (alt != null)
      out.writeAttribute("alt", alt, "alt");

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
