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
 * The HTML panel/group renderer
 */
class HtmlPanelGroupRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlPanelGroupRenderer();

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

    String id = null;
    String layout = null;
    String style = null;
    String styleClass = null;
    
    if (component instanceof HtmlPanelGroup) {
      HtmlPanelGroup html = (HtmlPanelGroup) component;

      id = html.getId();
      layout = html.getLayout();
      style = html.getStyle();
      styleClass = html.getStyleClass();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();

      id = (String) attrMap.get("id");
      layout = (String) attrMap.get("layout");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
    }

    boolean isDiv = "block".equals(layout);

    if (isDiv)
      out.startElement("div", component);
    else
      out.startElement("span", component);

    if (id != null)
      out.writeAttribute("id", id, "id");

    if (layout != null)
      out.writeAttribute("layout", layout, "layout");
    
    if (style != null)
      out.writeAttribute("style", style, "style");
    
    if (styleClass != null)
      out.writeAttribute("class", styleClass, "styleClass");

    int childCount = component.getChildCount();
    if (childCount > 0) {
      List<UIComponent> children = component.getChildren();

      for (int i = 0; i < childCount; i++) {
        UIComponent child = children.get(i);

        if (child.isRendered()) {
          child.encodeBegin(context);
          child.encodeChildren(context);
          child.encodeEnd(context);
        }
      }
    }
    
    if (isDiv)
      out.endElement("div");
    else
      out.endElement("span");
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
    return "HtmlPanelGroupRenderer[]";
  }
}
