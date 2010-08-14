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

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

class HtmlOutputLabelRenderer
  extends BaseRenderer
{
  public static final Renderer RENDERER = new HtmlOutputLabelRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return false;
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
    String dir;
    String lang;
    String style;
    String styleClass;
    String title;

    String accesskey;
    String onfocus;
    String onblur;
    String onclick;
    String ondblclick;
    String onmousedown;
    String onmouseup;
    String onmouseover;
    String onmousemove;
    String onmouseout;
    String onkeypress;
    String onkeydown;
    String onkeyup;

    String forValue;
    String tabindex;

    Object value;

    if (component instanceof HtmlOutputLabel) {
      HtmlOutputLabel htmlOutput = (HtmlOutputLabel) component;

      dir = htmlOutput.getDir();
      lang = htmlOutput.getLang();
      style = htmlOutput.getStyle();
      styleClass = htmlOutput.getStyleClass();
      title = htmlOutput.getTitle();

      forValue = htmlOutput.getFor();

      tabindex = htmlOutput.getTabindex();

      accesskey = htmlOutput.getAccesskey();
      onfocus = htmlOutput.getOnfocus();
      onblur = htmlOutput.getOnblur();
      onclick = htmlOutput.getOnclick();
      ondblclick = htmlOutput.getOndblclick();
      onmousedown = htmlOutput.getOnmousedown();
      onmouseup = htmlOutput.getOnmouseup();
      onmouseover = htmlOutput.getOnmouseover();
      onmousemove = htmlOutput.getOnmousemove();
      onmouseout = htmlOutput.getOnmouseout();
      onkeypress = htmlOutput.getOnkeypress();
      onkeydown = htmlOutput.getOnkeydown();
      onkeyup = htmlOutput.getOnkeyup();

      value = htmlOutput.getValue();
    }
    else {
      Map<String, Object> attrMap = component.getAttributes();

      dir = (String) attrMap.get("dir");
      lang = (String) attrMap.get("lang");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");

      forValue = (String) attrMap.get("for");

      tabindex = (String) attrMap.get("tabindex");

      accesskey = (String) attrMap.get("accesskey");
      onfocus = (String) attrMap.get("onfocus");
      onblur = (String) attrMap.get("onblur");
      onclick = (String) attrMap.get("onclick");
      ondblclick = (String) attrMap.get("ondblclick");
      onmousedown = (String) attrMap.get("onmousedown");
      onmouseup = (String) attrMap.get("onmouseup");
      onmouseover = (String) attrMap.get("onmouseover");
      onmousemove = (String) attrMap.get("onmousemove");
      onmouseout = (String) attrMap.get("onmouseout");
      onkeypress = (String) attrMap.get("onkeypress");
      onkeydown = (String) attrMap.get("onkeydown");
      onkeyup = (String) attrMap.get("onkeyup");

      value = attrMap.get("value");
    }

    out.startElement("label", component);

    if (id != null && !id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    if (lang != null)
      out.writeAttribute("lang", lang, "dir");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (title != null)
      out.writeAttribute("title", title, "title");

    if (forValue != null) {
      UIComponent forComponent = component.findComponent(forValue);
      if (forComponent != null)
        out.writeAttribute("for", forComponent.getClientId(context), "for");
    }

    if (tabindex != null)
      out.writeAttribute("tabindex", tabindex, "tabindex");

    if (accesskey != null)
      out.writeAttribute("accesskey", accesskey, "accesskey");

    if (onfocus != null)
      out.writeAttribute("onfocus", onfocus, "onfocus");

    if (onblur != null)
      out.writeAttribute("onblur", onblur, "onblur");

    if (onclick != null)
      out.writeAttribute("onclick", onclick, "onclick");

    if (ondblclick != null)
      out.writeAttribute("ondblclick", ondblclick, "ondblclick");

    if (onmousedown != null)
      out.writeAttribute("onmousedown", onmousedown, "onmousedown");

    if (onmouseup != null)
      out.writeAttribute("onmouseup", onmouseup, "onmouseup");

    if (onmouseover != null)
      out.writeAttribute("onmouseover", onmouseover, "onmouseover");

    if (onmousemove != null)
      out.writeAttribute("onmousemove", onmousemove, "onmousemove");

    if (onmouseout != null)
      out.writeAttribute("onmouseout", onmouseout, "onmouseout");

    if (onkeypress != null)
      out.writeAttribute("onkeypress", onkeypress, "onkeypress");

    if (onkeydown != null)
      out.writeAttribute("onkeydown", onkeydown, "onkeydown");

    if (onkeyup != null)
      out.writeAttribute("onkeyup", onkeyup, "onkeyup");

    if (value != null)
      out.writeText(toString(context, component, value), "value");
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();
    out.endElement("label");
  }

  public String toString()
  {
    return "HtmlOutputLabelRenderer[]";
  }
}