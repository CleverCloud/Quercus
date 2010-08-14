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

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML form renderer
 */
class HtmlFormRenderer extends BaseRenderer
{
  private static final Logger log
    = Logger.getLogger(HtmlFormRenderer.class.getName());
  
  public static final Renderer RENDERER = new HtmlFormRenderer();

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
    Map<String,String> paramMap = ext.getRequestParameterMap();

    String value = paramMap.get(clientId);

    ((UIForm) component).setSubmitted(value != null);

    if (log.isLoggable(Level.FINE) && value != null) {
      log.fine("JSF[" + context.getViewRoot().getViewId()
               + "] decoding form data for '" + clientId + "'");
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

    String accept;
    String acceptcharset;
    String dir;
    String enctype;
    String lang;
    
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
    
    String onreset;
    String onsubmit;
    
    String style;
    String styleClass;
    String tabindex;
    String target;
    String title;

    if (component instanceof HtmlForm) {
      HtmlForm htmlForm = (HtmlForm) component;

      accept = htmlForm.getAccept();
      acceptcharset = htmlForm.getAcceptcharset();
      dir = htmlForm.getDir();
      enctype = htmlForm.getEnctype();
      lang = htmlForm.getLang();
      
      onclick = htmlForm.getOnclick();
      ondblclick = htmlForm.getOndblclick();
      
      onkeydown = htmlForm.getOnkeydown();
      onkeypress = htmlForm.getOnkeypress();
      onkeyup = htmlForm.getOnkeyup();
      
      onmousedown = htmlForm.getOnmousedown();
      onmousemove = htmlForm.getOnmousemove();
      onmouseout = htmlForm.getOnmouseout();
      onmouseover = htmlForm.getOnmouseover();
      onmouseup = htmlForm.getOnmouseup();
      
      onreset = htmlForm.getOnreset();
      onsubmit = htmlForm.getOnsubmit();
      
      style = htmlForm.getStyle();
      styleClass = htmlForm.getStyleClass();
      target = htmlForm.getTarget();
      title = htmlForm.getTitle();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();
    
      accept = (String) attrMap.get("accept");
      acceptcharset = (String) attrMap.get("acceptcharset");
      dir = (String) attrMap.get("dir");
      enctype = (String) attrMap.get("enctype");
      lang = (String) attrMap.get("lang");
      
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
      
      onreset = (String) attrMap.get("onreset");
      onsubmit = (String) attrMap.get("onsubmit");
      
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      target = (String) attrMap.get("target");
      title = (String) attrMap.get("title");
    }

    out.startElement("form", component);

    String clientId = component.getClientId(context);

    out.writeAttribute("name", clientId, "name");
    out.writeAttribute("id", clientId, "id");

    out.writeAttribute("method", "post", "method");

    String viewId = context.getViewRoot().getViewId();

    Application app = context.getApplication();
    ExternalContext extContext = context.getExternalContext();

    ViewHandler view = app.getViewHandler();
    String action = view.getActionURL(context, viewId);
    String encodedAction = extContext.encodeActionURL(action);

    out.writeAttribute("action", encodedAction, "action");

    if (accept != null)
      out.writeAttribute("accept", accept, "accept");

    if (acceptcharset != null)
      out.writeAttribute("accept-charset", acceptcharset, "acceptcharset");

    if (dir != null)
      out.writeAttribute("dir", dir, "dir");

    if (enctype != null)
      out.writeAttribute("enctype", enctype, "enctype");
    else
      out.writeAttribute("enctype", "application/x-www-form-urlencoded", "enctype");

    if (lang != null)
      out.writeAttribute("lang", lang, "lang");

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

    if (onreset != null)
      out.writeAttribute("onreset", onreset, "onreset");

    if (onsubmit != null)
      out.writeAttribute("onsubmit", onsubmit, "onsubmit");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (target != null)
      out.writeAttribute("target", target, "target");

    if (title != null)
      out.writeAttribute("title", title, "title");
    
    out.write("\n");
    
    out.startElement("input", component);
    out.writeAttribute("type", "hidden", "type");
    out.writeAttribute("name", component.getClientId(context), "name");
    //out.writeAttribute("value", "true", "value");
    out.endElement("input");
    out.write("\n");
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
    ViewHandler handler = context.getApplication().getViewHandler();

    ResponseWriter out = context.getResponseWriter();

    Set<String> links = findCommandLinkHiddenFieldSet(context, component.getClientId(context),false);

    if (links != null) {
      for (String link : links) {
        out.startElement("input", component);
        out.writeAttribute("type", "hidden", "type");
        out.writeAttribute("name", link, "name");
        out.endElement("input");
      }
    }

    Set<String> params = findCommandLinkParamSet(context,
                                                 component.getClientId(context),
                                                 false);
    if (params != null) {
      for (String param : params) {
        out.startElement("input", component);

        out.writeAttribute("name", param, "name");
        out.writeAttribute("id", param, "id");
        out.writeAttribute("type", "hidden", "type");

        out.endElement("input");
      }
    }

    // jsf/1136 -- needed by facelets
    handler.writeState(context);

    out.endElement("form");
    out.write("\n");
  }

  static void addCommandLinkParam(FacesContext context,
                                  String formClientId,
                                  String name)
  {
    findCommandLinkParamSet(context, formClientId, true).add(name);
  }

  static void addCommandLinkHiddenField(FacesContext context,
                                        String formClientId,
                                        String name) {
    findCommandLinkHiddenFieldSet(context, formClientId, true).add(name);
  }

  private static Set<String> findCommandLinkParamSet(FacesContext context,
                                                     String formClientId,
                                                     boolean create)
  {
    final String setKey = "com.caucho.jsf.html.form." +
                          formClientId +
                          ".commandLinkParams";
    Map requestMap = context.getExternalContext().getRequestMap();
    Set<String> params = (Set<String>) requestMap.get(setKey);
    if (params == null && create) {
      params = new HashSet<String>();
      requestMap.put(setKey, params);
    }
    return params;
  }

  private static Set<String> findCommandLinkHiddenFieldSet(FacesContext context,
                                                     String formClientId,
                                                     boolean create)
  {
    final String setKey = "com.caucho.jsf.html.form." +
                          formClientId +
                          ".commandLinks";
    Map requestMap = context.getExternalContext().getRequestMap();
    Set<String> links = (Set<String>) requestMap.get(setKey);
    if (links == null && create) {
      links = new HashSet<String>();
      requestMap.put(setKey, links);
    }
    return links;
  }

}
