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

package com.caucho.jsf.application;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.render.*;

import javax.servlet.http.*;
import javax.servlet.jsp.jstl.core.*;

import com.caucho.jsf.context.*;
import com.caucho.util.*;

public class JspViewHandler extends ViewHandler
{
  private static final L10N L = new L10N(JspViewHandler.class);
  
  @Override
  public Locale calculateLocale(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    ExternalContext extContext = context.getExternalContext();

    Locale locale;

    ArrayList<Locale> supportedLocales = new ArrayList<Locale>();
    Iterator<Locale> iter = context.getApplication().getSupportedLocales();

    while (iter != null && iter.hasNext())
      supportedLocales.add(iter.next());
    
    iter = extContext.getRequestLocales();
    while (iter.hasNext()) {
      locale = iter.next();

      for (int i = 0; i < supportedLocales.size(); i++) {
        Locale supLocale = supportedLocales.get(i);

        if (supLocale.equals(locale))
          return supLocale;
        else if ("".equals(supLocale.getCountry())
                 && locale.getLanguage().equals(supLocale.getLanguage()))
          return supLocale;
      }
    }
    
    locale = context.getApplication().getDefaultLocale();

    if (locale != null)
      return locale;

    return Locale.getDefault();
  }

  @Override
  public String calculateCharacterEncoding(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    ExternalContext extContext = context.getExternalContext();

    HttpServletRequest req = (HttpServletRequest) extContext.getRequest();

    String contentType = req.getHeader("Content-Type");

    if (contentType != null) {
      int p = contentType.indexOf("charset=");

      if (p > 0) {
        int q = contentType.indexOf(';', p + 9);

        String charset;

        if (q > 0)
          charset = contentType.substring(p + 8, q).trim();
        else
          charset = contentType.substring(p + 8).trim();

        return charset;
      }
    }

    if (extContext.getSession(false) != null) {
      Map<String,Object> sessionMap = extContext.getSessionMap();

      Object value = sessionMap.get(CHARACTER_ENCODING_KEY);

      if (value != null)
        return value.toString();
    }
    
    return "utf-8";
  }

  @Override
  public String calculateRenderKitId(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    ExternalContext extContext = context.getExternalContext();
    Map requestMap = extContext.getRequestMap();

    String id;
    
    id = (String) requestMap.get(ResponseStateManager.RENDER_KIT_ID_PARAM);

    if (id != null)
      return id;

    Application app = context.getApplication();

    id = app.getDefaultRenderKitId();

    if (id != null)
      return id;
    
    return RenderKitFactory.HTML_BASIC_RENDER_KIT;
  }

  public UIViewRoot createView(FacesContext context,
                               String viewId)
  {
    if (context == null)
      throw new NullPointerException();

    if (viewId != null) {

      viewId = convertViewId(context, viewId);

      ExternalContext extContext = context.getExternalContext();
      String servletPath = extContext.getRequestServletPath();

      if (viewId.equals(servletPath)
          || (servletPath == null
              && viewId.equals(extContext.getRequestPathInfo()))) {
        try {
          extContext.redirect(extContext.getRequestContextPath());
        }
        catch (IOException e) {
          throw new FacesException(e);
        }

        context.renderResponse();
        context.responseComplete();

        return null;
      }
    }

    UIViewRoot viewRoot = (UIViewRoot) context.getApplication()
      .createComponent(UIViewRoot.COMPONENT_TYPE);

    viewRoot.setViewId(viewId);

    UIViewRoot oldView = context.getViewRoot();

    String renderKitId = null;

    if (oldView != null)
      renderKitId = oldView.getRenderKitId();
    
    if (renderKitId == null)
      renderKitId = calculateRenderKitId(context);

    viewRoot.setRenderKitId(renderKitId);
    
    Locale locale = null;

    if (oldView != null)
      locale = oldView.getLocale();

    if (locale == null)
      locale = calculateLocale(context);
    
    viewRoot.setLocale(locale);

    return viewRoot;
  }

  public static String createViewId(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();

    Map requestMap = extContext.getRequestMap();

    boolean isInclude
      = requestMap.containsKey("javax.servlet.include.request_uri");
    
    String pathInfo;

    if (isInclude)
      pathInfo = (String) requestMap.get("javax.servlet.include.path_info");
    else
      pathInfo = extContext.getRequestPathInfo();

    if (pathInfo != null)
      return pathInfo;

    String servletPath = extContext.getRequestServletPath();

    String path;
    int dot;

    if (servletPath != null
        && (dot = servletPath.lastIndexOf('.')) > 0
        && servletPath.lastIndexOf('/') < dot) {
      String suffix
        = extContext.getInitParameter(ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);

      if (suffix == null)
        suffix = ViewHandler.DEFAULT_SUFFIX;
      
      // /test/foo.jsp

      return servletPath.substring(0, dot) + suffix;
    }

    throw new FacesException(L.l("no view-id found"));
  }

  static String convertViewId(FacesContext context, String viewId)
  {
    ExternalContext extContext = context.getExternalContext();

    Map requestMap = extContext.getRequestMap();

    boolean isInclude
      = requestMap.containsKey("javax.servlet.include.request_uri");
    
    String pathInfo;

    if (isInclude)
      pathInfo = (String) requestMap.get("javax.servlet.include.path_info");
    else
      pathInfo = extContext.getRequestPathInfo();

    if (pathInfo != null)
      return viewId;

    int dot;
    if ((dot = viewId.lastIndexOf('.')) > 0
        && viewId.lastIndexOf('/') < dot) {
      String suffix
        = extContext.getInitParameter(ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);

      if (suffix == null)
        suffix = ViewHandler.DEFAULT_SUFFIX;
      
      return viewId.substring(0, dot) + suffix;
    }
    else
      return viewId;
  }

  public String getActionURL(FacesContext context,
                             String viewId)
  {
    if (context == null || viewId == null)
      throw new NullPointerException();

    if (! viewId.startsWith("/"))
      throw new IllegalArgumentException();
    
    ExternalContext extContext = context.getExternalContext();

    HttpServletRequest request
      = (HttpServletRequest) extContext.getRequest();

    final String contextPath = request.getContextPath();
    
    final String servletPath = request.getServletPath();
    final String pathInfo = request.getPathInfo();

    if (pathInfo == null) /*suffix mapping*/ {
      final int lastDot = viewId.lastIndexOf('.');
      final int suffixDot = servletPath.lastIndexOf('.');

      return contextPath +
             (lastDot == -1 || suffixDot == -1
              ? viewId
              : viewId.substring(0, lastDot) +
                servletPath.substring(suffixDot));
    }
    else /*prefix mapping*/ {

      return contextPath + servletPath + viewId;
    }
  }

  public String getResourceURL(FacesContext context,
                               String path)
  {
    if (path.startsWith("/")) {
      ExternalContext extContext = context.getExternalContext();

      HttpServletRequest request
        = (HttpServletRequest) extContext.getRequest();
    
      return request.getContextPath() + path;
    }
    else
      return path;
  }

  public void renderView(FacesContext context,
                         UIViewRoot viewToRender)
    throws IOException, FacesException
  {
    if (! viewToRender.isRendered())
      return;
    
    String viewId;

    viewId = viewToRender.getViewId();

    ExternalContext extContext = context.getExternalContext();
    HttpServletResponse response
      = (javax.servlet.http.HttpServletResponse) extContext.getResponse();
    HttpServletRequest request
      = (javax.servlet.http.HttpServletRequest) extContext.getRequest();

    Config.set(request, Config.FMT_LOCALE, viewToRender);

    response.setContentType("text/html");

    RenderKitFactory renderKitFactory
      = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
    String renderKitId = viewToRender.getRenderKitId();

    if (renderKitId == null)
      renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
    
    RenderKit renderKit = renderKitFactory.getRenderKit(context, renderKitId);

    String encoding = request.getCharacterEncoding();
    
    ResponseWriter oldOut = context.getResponseWriter();

    ResponseWriter out;

    JspResponseWrapper resWrapper = new JspResponseWrapper();
    resWrapper.init(response);
    extContext.setResponse(resWrapper);

    extContext.dispatch(viewId);

    String tail = resWrapper.completeJsf();

    extContext.setResponse(response);
    
    out = renderKit.createResponseWriter(response.getWriter(),
                                         null,
                                         encoding);

    context.setResponseWriter(out);
    
    //context.getApplication().getViewHandler().writeState(context);

    // XXX: save view

    out.startDocument();

    viewToRender.encodeAll(context);

    if (tail != null)
      out.write(tail);
    
    out.endDocument();

    if (oldOut != null)
      context.setResponseWriter(oldOut);
  }

  @Override
  public UIViewRoot restoreView(FacesContext context,
                                String viewId)
    throws FacesException
  {
    if (context == null)
      throw new NullPointerException();

    if (viewId != null)
      viewId = convertViewId(context, viewId);
    else
      viewId = createViewId(context);

    String renderKitId = calculateRenderKitId(context);
    StateManager stateManager = context.getApplication().getStateManager();

    return stateManager.restoreView(context, viewId, renderKitId);
  }

  @Override
  public void writeState(FacesContext context)
    throws IOException
  {
    UIViewRoot viewRoot = context.getViewRoot();

    if (viewRoot != null) {
      StateManager stateManager = context.getApplication().getStateManager();

      Object state = stateManager.saveView(context);

      stateManager.writeState(context, state);
    }
  }

  public String toString()
  {
    return "JspViewHandler[]";
  }
}
