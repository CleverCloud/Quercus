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
import javax.faces.render.*;

/**
 * The HTML messages renderer
 */
class HtmlMessagesRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlMessagesRenderer();

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
    Iterator<FacesMessage> iter = context.getMessages();

    ResponseWriter out = context.getResponseWriter();

    String id;
    String dir;
    String lang;
    String layout;
    String errorClass;
    String errorStyle;
    String fatalClass;
    String fatalStyle;
    String infoClass;
    String infoStyle;
    String style;
    String styleClass;
    String title;
    String warnClass;
    String warnStyle;
    boolean isShowSummary;
    boolean isShowDetail;

    id = component.getId();
    
    if (component instanceof HtmlMessages) {
      HtmlMessages htmlComp = (HtmlMessages) component;

      isShowSummary = htmlComp.isShowSummary();
      isShowDetail = htmlComp.isShowDetail();

      errorClass = htmlComp.getErrorClass();
      errorStyle = htmlComp.getErrorStyle();

      fatalClass = htmlComp.getFatalClass();
      fatalStyle = htmlComp.getFatalStyle();

      dir = htmlComp.getDir();

      infoClass = htmlComp.getInfoClass();
      infoStyle = htmlComp.getInfoStyle();
      
      lang = htmlComp.getLang();
      layout = htmlComp.getLayout();
      
      style = htmlComp.getStyle();
      styleClass = htmlComp.getStyleClass();
      title = htmlComp.getTitle();

      warnClass = htmlComp.getWarnClass();
      warnStyle = htmlComp.getWarnStyle();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();

      isShowSummary = Boolean.TRUE.equals(attrMap.get("showSummary"));
      isShowDetail = Boolean.TRUE.equals(attrMap.get("showDetail"));

      dir = (String) attrMap.get("dir");
      
      errorClass = (String) attrMap.get("errorClass");
      errorStyle = (String) attrMap.get("errorStyle");
      fatalClass = (String) attrMap.get("fatalClass");
      fatalStyle = (String) attrMap.get("fatalStyle");
      infoClass = (String) attrMap.get("infoClass");
      infoStyle = (String) attrMap.get("infoStyle");
      warnClass = (String) attrMap.get("warnClass");
      warnStyle = (String) attrMap.get("warnStyle");
      
      lang = (String) attrMap.get("lang");
      layout = (String) attrMap.get("layout");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");
    }

    boolean isFirst = true;

    while (iter.hasNext()) {
      FacesMessage msg = iter.next();

      if (isFirst) {
        if ("table".equals(layout))
          out.startElement("table", component);
        else
          out.startElement("ul", component);

        if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
          out.writeAttribute("id", component.getClientId(context), "id");

        if (dir != null)
          out.writeAttribute("dir", dir, "dir");

        if (lang != null)
          out.writeAttribute("lang", lang, "lang");

        if (style != null)
          out.writeAttribute("style", style, "style");

        if (styleClass != null)
          out.writeAttribute("class", styleClass, "styleClass");

        if (title != null)
          out.writeAttribute("title", title, "title");
      }
      isFirst = false;
      
      if ("table".equals(layout)) {
        out.startElement("tr", component);
        out.startElement("td", component);
      }
      else
        out.startElement("li", component);

      if (FacesMessage.SEVERITY_ERROR.equals(msg.getSeverity())) {
        if (errorClass != null)
          out.writeAttribute("class", errorClass, "errorClass");

        if (errorStyle != null)
          out.writeAttribute("style", errorStyle, "errorStyle");
      }
      else if (FacesMessage.SEVERITY_FATAL.equals(msg.getSeverity())) {
        if (fatalClass != null)
          out.writeAttribute("class", fatalClass, "fatalClass");

        if (fatalStyle != null)
          out.writeAttribute("style", fatalStyle, "fatalStyle");
      }
      else if (FacesMessage.SEVERITY_INFO.equals(msg.getSeverity())) {
        if (infoClass != null)
          out.writeAttribute("class", infoClass, "infoClass");

        if (infoStyle != null)
          out.writeAttribute("style", infoStyle, "infoStyle");
      }
      else if (FacesMessage.SEVERITY_WARN.equals(msg.getSeverity())) {
        if (warnClass != null)
          out.writeAttribute("class", warnClass, "warnClass");

        if (warnStyle != null)
          out.writeAttribute("style", warnStyle, "warnStyle");
      }

      if (isShowSummary)
        out.writeText(msg.getSummary(), "summary");
      
      if (isShowDetail)
        out.writeText(msg.getDetail(), "detail");
      
      if ("table".equals(layout)) {
        out.endElement("td");
        out.endElement("tr");
      }
      else
        out.endElement("li");
    }
    
    if (! isFirst) {
      if ("table".equals(layout))
        out.endElement("table");
      else
        out.endElement("ul");
    }
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
    return "HtmlMessagesRenderer[]";
  }
}
