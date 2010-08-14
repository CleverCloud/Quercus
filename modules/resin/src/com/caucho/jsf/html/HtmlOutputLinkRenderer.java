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
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML link renderer
 */
class HtmlOutputLinkRenderer extends BaseRenderer
{
  public static final Renderer RENDERER = new HtmlOutputLinkRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
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
    String target;
    String title;

    Object value;
    
    if (component instanceof HtmlOutputLink) {
      HtmlOutputLink htmlOutput = (HtmlOutputLink) component;

      dir = htmlOutput.getDir();
      lang = htmlOutput.getLang();
      target = htmlOutput.getTarget();
      style = htmlOutput.getStyle();
      styleClass = htmlOutput.getStyleClass();
      title = htmlOutput.getTitle();

      value = htmlOutput.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      dir = (String) attrMap.get("dir");
      lang = (String) attrMap.get("lang");
      target = (String) attrMap.get("target");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");

      value = attrMap.get("value");
    }

    out.startElement("a", component);

    if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    if (lang != null)
      out.writeAttribute("lang", lang, "lang");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (target != null)
      out.writeAttribute("target", lang, "target");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (title != null)
      out.writeAttribute("title", title, "title");

    int childCount = component.getChildCount();

    String href = toString(context, component, value);

    StringBuilder sb = null;
    for (int i = 0; i < childCount; i++) {
      UIComponent child = component.getChildren().get(i);

      if (child instanceof UIParameter) {
        if (sb == null) {
          sb = new StringBuilder().append(href);

          if (href.indexOf('?') < 0)
            sb.append('?');
          else
            sb.append('&');
        }
        else
          sb.append('&');

        UIParameter param = (UIParameter) child;

        String name = param.getName();
        Object paramValue = param.getValue();

        if (name != null) {
          sb.append(name);
          sb.append('=');
        }

        sb.append(toString(context, param, paramValue));
      }
    }

    if (sb != null)
      out.writeAttribute("href", sb.toString(), "href");
    else
      out.writeAttribute("href", href, "href");

    if (childCount > 0) {
      List<UIComponent> children = component.getChildren();

      for (int i = 0; i < childCount; i++) {
        UIComponent child = children.get(i);

        if (child instanceof UIParameter)
          continue;
      
        if (child.isRendered()) {
          child.encodeBegin(context);
          child.encodeChildren(context);
          child.encodeEnd(context);
        }
      }
    }
    
    out.endElement("a");
  }

  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
    ResponseWriter out = context.getResponseWriter();
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
    return "HtmlOutputTextRenderer[]";
  }
}
