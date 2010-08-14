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
 * The HTML graphic/image renderer
 */
class HtmlGraphicImageRenderer extends Renderer
{
  public static final Renderer RENDERER = new HtmlGraphicImageRenderer();

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

    String alt;
    String dir;
    String height;
    boolean ismap = false;
    String lang;
    String longdesc;
    
    String onclick;
    String ondblclick;
    
    String onkeydown;
    String onkeypress;
    String onkeyup;
    
    String onmousedown;
    String onmousemove;
    String onmouseout;
    String onmouseover;
    String onmouseup;
    
    String style;
    String styleClass;
    String title;
    String usemap;
    String width;

    Object value;
    
    if (component instanceof HtmlGraphicImage) {
      HtmlGraphicImage html = (HtmlGraphicImage) component;

      alt = html.getAlt();
      dir = html.getDir();
      height = html.getHeight();
      ismap = html.isIsmap();
      lang = html.getLang();
      longdesc = html.getLongdesc();
      
      onclick = html.getOnclick();
      ondblclick = html.getOndblclick();
      
      onkeydown = html.getOnkeydown();
      onkeypress = html.getOnkeypress();
      onkeyup = html.getOnkeyup();
      
      onmousedown = html.getOnmousedown();
      onmousemove = html.getOnmousemove();
      onmouseout = html.getOnmouseout();
      onmouseover = html.getOnmouseover();
      onmouseup = html.getOnmouseup();
      
      style = html.getStyle();
      styleClass = html.getStyleClass();
      title = html.getTitle();
      usemap = html.getUsemap();
      width = html.getWidth();
      
      value = html.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      alt = (String) attrMap.get("alt");
      dir = (String) attrMap.get("dir");
      height = (String) attrMap.get("height");
      Boolean bValue = (Boolean) attrMap.get("ismap");
      if (bValue != null)
        ismap = bValue;
      lang = (String) attrMap.get("lang");
      longdesc = (String) attrMap.get("longdesc");
      
      onclick = (String) attrMap.get("onclick");
      ondblclick = (String) attrMap.get("ondblclick");
      
      onkeydown = (String) attrMap.get("onkeydown");
      onkeypress = (String) attrMap.get("onkeypress");
      onkeyup = (String) attrMap.get("onkeyup");
      
      onmousedown = (String) attrMap.get("onmousedown");
      onmousemove = (String) attrMap.get("onmousemove");
      onmouseout = (String) attrMap.get("onmouseout");
      onmouseover = (String) attrMap.get("onmouseover");
      onmouseup = (String) attrMap.get("onmouseup");
      
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      title = (String) attrMap.get("title");
      usemap = (String) attrMap.get("usemap");
      width = (String) attrMap.get("width");
      
      value = attrMap.get("value");
    }

    out.startElement("img", component);

    if (id != null && ! id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      out.writeAttribute("id", component.getClientId(context), "id");

    String src;
    if (value == null)
      src = "";
    else
      src = String.valueOf(value);

    ViewHandler view = context.getApplication().getViewHandler();
    ExternalContext extContext = context.getExternalContext();

    src = extContext.encodeResourceURL(view.getResourceURL(context, src));

    out.writeAttribute("src", src, "src");

    if (alt != null)
      out.writeAttribute("alt", alt, "alt");
      
    if (dir != null)
      out.writeAttribute("dir", dir, "dir");
      
    if (height != null)
      out.writeAttribute("height", height, "height");

    if (ismap)
      out.writeAttribute("ismap", "ismap", "ismap");
      
    if (lang != null)
      out.writeAttribute("lang", lang, "lang");

    if (longdesc != null)
      out.writeAttribute("longdesc", longdesc, "longdesc");

    if (onclick != null)
      out.writeAttribute("onclick", onclick, "onclick");

    if (ondblclick != null)
      out.writeAttribute("ondblclick", ondblclick, "ondblclick");

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

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (title != null)
      out.writeAttribute("title", title, "title");

    if (usemap != null)
      out.writeAttribute("usemap", usemap, "usemap");

    if (width != null)
      out.writeAttribute("width", width, "width");

    out.endElement("img");
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
    ResponseWriter out = context.getResponseWriter();
  }

  public String toString()
  {
    return "HtmlGraphicImageRenderer[]";
  }
}
