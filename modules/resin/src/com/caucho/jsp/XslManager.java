/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.util.CauchoSystem;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.xsl.AbstractStylesheetFactory;
import com.caucho.xsl.CauchoStylesheet;
import com.caucho.xsl.StyleScript;
import com.caucho.xsl.StylesheetImpl;
import com.caucho.xsl.Xsl;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.xml.transform.Templates;
import java.lang.ref.SoftReference;
import java.util.logging.Logger;

class XslManager {
  private static final Logger log
    = Logger.getLogger(XslManager.class.getName());
  static final L10N L = new L10N(XslManager.class);

  private WebApp _webApp;
  private Path _workPath;
  private LruCache<String,SoftReference<Templates>> _xslCache =
    new LruCache<String,SoftReference<Templates>>(256);
  private boolean _strictXsl;
  private long _lastUpdate;

  public XslManager(ServletContext context)
  {
    _webApp = (WebApp) context;

    _workPath = CauchoSystem.getWorkPath();
  }

  public void setStrictXsl(boolean strictXsl)
  {
    _strictXsl = strictXsl;
  }

  public String getServletInfo()
  {
    return "Resin XTP";
  }

  Templates get(String href, CauchoRequest req)
    throws Exception
  {
    String servletPath = req.getPageServletPath();

    WebApp webApp = req.getWebApp();

    Path appDir = webApp.getAppDir();
    Path pwd = appDir.lookupNative(webApp.getRealPath(servletPath));
    pwd = pwd.getParent();
    
    String fullStyleSheet = pwd.toString() + "/" + href;

    Templates stylesheet = null;

    long now = Alarm.getCurrentTime();

    SoftReference<Templates> templateRef = _xslCache.get(fullStyleSheet);

    if (templateRef != null)
      stylesheet = templateRef.get();

    if (stylesheet instanceof StylesheetImpl
        && ! ((StylesheetImpl) stylesheet).isModified())
      return stylesheet;

    _lastUpdate = now;
    stylesheet = getStylesheet(req, href);

    if (stylesheet == null)
      throw new ServletException(L.l("can't find stylesheet `{0}'", href));

    _xslCache.put(fullStyleSheet, new SoftReference<Templates>(stylesheet));

    return stylesheet;
  }

  /**
   * Returns the stylesheet given by the references.
   */
  Templates getStylesheet(CauchoRequest req, String href)
    throws Exception
  {
    String servletPath = req.getPageServletPath();

    WebApp webApp = req.getWebApp();
    Path appDir = webApp.getAppDir();
    Path pwd = appDir.lookupNative(webApp.getRealPath(servletPath));
    pwd = pwd.getParent();

    DynamicClassLoader loader;
    loader = (DynamicClassLoader) webApp.getClassLoader();
    
    MergePath stylePath = new MergePath();
    stylePath.addMergePath(pwd);
    stylePath.addMergePath(appDir);

    String resourcePath = loader.getResourcePathSpecificFirst();
    stylePath.addClassPath(resourcePath);
    
    Path hrefPath = stylePath.lookup(href);

    if (hrefPath.canRead()) {
      DynamicClassLoader owningLoader = getStylesheetLoader(href, hrefPath);

      if (owningLoader != null) {
        loader = owningLoader;

        stylePath = new MergePath();
        stylePath.addMergePath(pwd);
        stylePath.addMergePath(appDir);
        resourcePath = loader.getResourcePathSpecificFirst();
        stylePath.addClassPath(resourcePath);
      }
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(loader);
    
      CauchoStylesheet xsl;

      AbstractStylesheetFactory factory;
    
      if (_strictXsl)
        factory = new Xsl();
      else
        factory = new StyleScript();

      factory.setStylePath(stylePath);
      factory.setClassLoader(loader);
      // factory.setWorkPath(_workPath);
    
      String className = "";

      if (pwd.lookup(href).canRead()) {
        int p = req.getServletPath().lastIndexOf('/');
        if (p >= 0)
          className += req.getServletPath().substring(0, p);
      }
      /*
        else if (href.startsWith("/"))
        href = href.substring(1);
      */
      
      className += "/" + href;

      factory.setClassName(className);

      // XXX: error here
      return factory.newTemplates(href);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private DynamicClassLoader getStylesheetLoader(String href, Path sourcePath)
  {
    DynamicClassLoader owningLoader = null;
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (! (loader instanceof DynamicClassLoader))
        continue;

      DynamicClassLoader dynLoader = (DynamicClassLoader) loader;
      
      MergePath mp = new MergePath();
      String resourcePath = dynLoader.getResourcePathSpecificFirst();
      mp.addClassPath(resourcePath);

      Path loaderPath = mp.lookup(href);

      if (loaderPath.getNativePath().equals(sourcePath.getNativePath()))
        owningLoader = dynLoader;
    }

    return owningLoader;
  }
}

