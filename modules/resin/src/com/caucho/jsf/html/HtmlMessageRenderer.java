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

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML message renderer
 */
class HtmlMessageRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlMessageRenderer();

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

    String id;
    String dir;
    String lang;
    String errorClass;
    String errorStyle;
    String fatalClass;
    String fatalStyle;
    String forValue;
    String infoClass;
    String infoStyle;
    String style;
    String styleClass;
    String title;
    boolean tooltip;
    String warnClass;
    String warnStyle;
    boolean isShowSummary;
    boolean isShowDetail;

    id = component.getId();
    
    if (component instanceof HtmlMessage) {
      HtmlMessage htmlComp = (HtmlMessage) component;

      isShowSummary = htmlComp.isShowSummary();
      isShowDetail = htmlComp.isShowDetail();

      errorClass = htmlComp.getErrorClass();
      errorStyle = htmlComp.getErrorStyle();

      fatalClass = htmlComp.getFatalClass();
      fatalStyle = htmlComp.getFatalStyle();
      
      dir = htmlComp.getDir();

      forValue = htmlComp.getFor();

      infoClass = htmlComp.getInfoClass();
      infoStyle = htmlComp.getInfoStyle();
      
      lang = htmlComp.getLang();
      
      style = htmlComp.getStyle();
      styleClass = htmlComp.getStyleClass();
      title = htmlComp.getTitle();
      tooltip = htmlComp.isTooltip();

      warnClass = htmlComp.getWarnClass();
      warnStyle = htmlComp.getWarnStyle();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();

      isShowSummary = Boolean.TRUE.equals(attrMap.get("showSummary"));
      isShowDetail = Boolean.TRUE.equals(attrMap.get("showDetail"));

      dir = (String) attrMap.get("dir");
      
      forValue = (String) attrMap.get("for");
      
      errorClass = (String) attrMap.get("errorClass");
      errorStyle = (String) attrMap.get("errorStyle");
      fatalClass = (String) attrMap.get("fatalClass");
      fatalStyle = (String) attrMap.get("fatalStyle");
      infoClass = (String) attrMap.get("infoClass");
      infoStyle = (String) attrMap.get("infoStyle");
      warnClass = (String) attrMap.get("warnClass");
      warnStyle = (String) attrMap.get("warnStyle");
      
      lang = (String) attrMap.get("lang");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");
      tooltip = Boolean.TRUE.equals(attrMap.get("tooltip"));
    }
    
    Iterator<FacesMessage> iter;

    UIComponent forComponent = component.findComponent(forValue);

    if (forComponent != null) {
      iter = context.getMessages(forComponent.getClientId(context));

      if (iter.hasNext()) {
        FacesMessage msg = iter.next();

        if (FacesMessage.SEVERITY_ERROR.equals(msg.getSeverity())) {
          if (errorClass != null)
            styleClass = errorClass;

          if (errorStyle != null)
            style = errorStyle;
        }
        else if (FacesMessage.SEVERITY_FATAL.equals(msg.getSeverity())) {
          if (fatalClass != null)
            styleClass = fatalClass;

          if (fatalStyle != null)
            style = fatalStyle;
        }
        else if (FacesMessage.SEVERITY_INFO.equals(msg.getSeverity())) {
          if (infoClass != null)
            styleClass = infoClass;

          if (infoStyle != null)
            style = infoStyle;
        }
        else if (FacesMessage.SEVERITY_WARN.equals(msg.getSeverity())) {
          if (warnClass != null)
            styleClass = warnClass;

          if (warnStyle != null)
            style = warnStyle;
        }

        boolean hasSpan = (dir != null
                           || lang != null
                           || style != null
                           || styleClass != null
                           || title != null
                           || tooltip
                           || (id != null &&
                               ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX)));

        if (hasSpan) {
          out.startElement("span", component);

          if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
            out.writeAttribute("id", component.getClientId(context), "id");
        }

        if (dir != null)
          out.writeAttribute("dir", dir, "dir");

        if (lang != null)
          out.writeAttribute("lang", lang, "lang");

        if (style != null)
          out.writeAttribute("style", style, "style");

        if (styleClass != null)
          out.writeAttribute("class", styleClass, "styleClass");

        boolean summaryDone = false;
        if (isShowSummary) {
          if (tooltip && isShowDetail) {
            out.writeAttribute("title", msg.getSummary(), "title");
          }
          else if (title != null) {
            out.writeAttribute("title", title, "title");
            out.writeText(msg.getSummary(), "summary");
            summaryDone = true;
          }
          else {
            out.writeText(msg.getSummary(), "summary");
            summaryDone = true;
          }
        }

        if (isShowDetail) {
          if (summaryDone)
            out.writeText(" ", null);
          
          out.writeText(msg.getDetail(), "detail");
        }

        if (hasSpan)
          out.endElement("span");
      }
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
    return "HtmlMessageRenderer[]";
  }
}
