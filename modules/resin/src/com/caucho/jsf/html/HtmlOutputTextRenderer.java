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
 * The HTML text renderer
 */
class HtmlOutputTextRenderer extends HtmlRenderer
{
  public static final Renderer RENDERER = new HtmlOutputTextRenderer();

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
    String title;
    boolean escape;
    Object value;
    
    if (component instanceof HtmlOutputText) {
      HtmlOutputText htmlOutput = (HtmlOutputText) component;

      dir = htmlOutput.getDir();
      lang = htmlOutput.getLang();
      style = htmlOutput.getStyle();
      styleClass = htmlOutput.getStyleClass();
      title = htmlOutput.getTitle();

      escape = htmlOutput.isEscape();
      value = htmlOutput.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      dir = (String) attrMap.get("dir");
      lang = (String) attrMap.get("lang");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");

      Boolean escapeValue = (Boolean) attrMap.get("escape");

      if (escapeValue != null)
        escape = escapeValue;
      else
        escape = true;

      if (component instanceof UIOutput)
        value = ((UIOutput) component).getValue();
      else
        value = attrMap.get("value");
    }

    boolean hasSpan = false;

    if (dir != null
        || lang != null
        || style != null
        || styleClass != null
        || (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))) {
      hasSpan = true;
      
      out.startElement("span", component);

      if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
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
    }
    
    String string = toString(context, component, value);

    if (escape)
      escapeText(out, string, "value");
    else
      out.write(string);

    if (hasSpan)
      out.endElement("span");
  }

  public String toString()
  {
    return "HtmlOutputTextRenderer[]";
  }
}
