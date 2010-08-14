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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.make.DependencyContainer;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.ToCharResponseAdapter;
import com.caucho.server.webapp.JspResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.QDate;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a compiled JSP page.
 */
abstract public class Page implements Servlet, ServletConfig, CauchoPage {
  protected static final Logger _caucho_log
    = Logger.getLogger(Page.class.getName());

  private ServletConfig _config;
  private WebApp _webApp;

  private DependencyContainer _depends = new DependencyContainer();
  private ArrayList<Depend> _cacheDepends;

  protected String _contentType;

  private PageManager.Entry _entry;
  private long _lastModified;
  private String _lastModifiedString;
  private String _etag;
  private QDate _calendar;

  private JspManager _jspManager;

  private boolean _isRecompiling = false;
  private int _useCount;
  private boolean _isDead = true;

  public void init(Path path)
    throws ServletException
  {
  }

  public void caucho_init(ServletConfig config)
    throws ServletException
  {
    init(config);
  }

  void _caucho_setContentType(String contentType)
  {
    _contentType = contentType;
  }

  void _caucho_setUpdateInterval(long updateInterval)
  {
    _depends.setCheckInterval(updateInterval);
  }

  void _caucho_setJspManager(JspManager manager)
  {
    _jspManager = manager;
  }

  void _caucho_unload()
  {
    if (_jspManager != null)
      _jspManager.unload(this);
  }

  void _caucho_setEntry(PageManager.Entry entry)
  {
    _entry = entry;
  }

  protected void _caucho_setContentType(String contentType, String encoding)
  {
    if (encoding != null && encoding.equals("ISO-8859-1"))
      encoding = null;

    _contentType = contentType;
  }

  /**
   * Marks the page as uncacheable.
   */
  void _caucho_setUncacheable()
  {
    _cacheDepends = null;
  }

  /**
   * When called treats the JSP page as always modified, i.e. always forcing
   * recompilation.
   */
  protected void _caucho_setAlwaysModified()
  {
    if (_cacheDepends == null) {
      _depends.setModified(true);
    }
  }

  /**
   * When called treats the JSP page as always modified, i.e. always forcing
   * recompilation.
   */
  protected void _caucho_setModified()
  {
    _depends.setModified(true);
  }

  /**
   * Set if the page is never modified.  Some users want to deploy
   * the JSP classes without the JSP source.
   */
  protected void _caucho_setNeverModified(boolean modified)
  {
    _depends.setCheckInterval(-1);
    _depends.setModified(false);
  }

  /**
   * Adds a dependency to the page.
   *
   * @param path the file the JSP page is dependent on.
   */
  protected void _caucho_addDepend(Path path)
  {
    PersistentDependency depend = path.createDepend();
    if (depend instanceof Depend)
      ((Depend) depend).setRequireSource(getRequireSource());

    _caucho_addDepend(depend);
  }

  /**
   * Adds a dependency to the page.
   *
   * @param path the file the JSP page is dependent on.
   */
  protected void _caucho_addDepend(PersistentDependency depend)
  {
    _depends.add(depend);
  }

  /**
   * Adds an array of dependencies to the page.
   */
  protected void _caucho_addDepend(ArrayList<PersistentDependency> dependList)
  {
    if (dependList == null)
      return;

    for (int i = 0; i < dependList.size(); i++)
      _caucho_addDepend(dependList.get(i));
  }

  /**
   * Adds a JSP source dependency.  If the source file changes, the JSP must
   * be recompiled.
   *
   * @param path the path to the file
   * @param lastModified the last modified time
   * @param length the length of the file
   */
  protected void _caucho_addDepend(Path path,
                                   long lastModified,
                                   long length)
  {
    Depend depend = new Depend(path, lastModified, length);
    depend.setRequireSource(getRequireSource());

    _caucho_addDepend(depend);
  }

  public ArrayList<Dependency> _caucho_getDependList()
  {
    return null;
  }

  /**
   * Marks the page as cacheable.
   */
  protected void _caucho_setCacheable()
  {
  }


  /**
   * Adds a single cache dependency.  A cache dependency will cause
   * the page to be rerun, but will not force a recompilation of the JSP.
   *
   * @param path the path to the file
   * @param lastModified the last modified time
   * @param length the length of the file
   */
  protected void _caucho_addCacheDepend(Path path,
                                        long lastModified,
                                        long length)
  {
    if (_cacheDepends == null)
      _cacheDepends = new ArrayList<Depend>();

    Depend depend = new Depend(path, lastModified, length);

    if (! _cacheDepends.contains(depend))
      _cacheDepends.add(depend);
  }

  /**
   * Adds an array of dependencies which will change the results of
   * running the page.
   *
   * @param depends an array list of Depend
   */
  void _caucho_setCacheable(ArrayList<Path> depends)
  {
    _cacheDepends = new ArrayList<Depend>();
    for (int i = 0; i < depends.size(); i++) {
      Path path = depends.get(i);

      Depend depend = new Depend(path);
      depend.setRequireSource(getRequireSource());

      if (! _cacheDepends.contains(depend))
        _cacheDepends.add(depend);
    }
  }

  /**
   * Returns true if the underlying source has been modified.
   */
  @Override
  public boolean _caucho_isModified()
  {
    return _isDead || _depends.isModified();
  }

  /***
   * Returns true if the underlying source has been modified.
   */
  /*
  public final boolean cauchoIsModified()
  {
    // return (_isDead || _depends.isModified());
    return _caucho_isModified();
  }
  */

  protected HashMap<String,Method> _caucho_getFunctionMap()
  {
    return null;
  }

  /**
   * Returns true if deleting the underlying JSP will force a recompilation.
   */
  private boolean getRequireSource()
  {
    return false;
  }

  /**
   * Initialize the servlet.
   */
  public void init(ServletConfig config)
    throws ServletException
  {
    if (_config != null)
      return;

    _config = config;
    _isDead = false;

    _webApp = (WebApp) config.getServletContext();

    //cauchoIsModified();
    ArrayList<Dependency> depends = _caucho_getDependList();

    for (int i = 0; depends != null && i < depends.size(); i++)
      _depends.add(depends.get(i));

    if (! disableLog() && _caucho_log.isLoggable(Level.FINE))
      _caucho_log.fine(getClass().getName() + " init");
  }

  /**
   * Returns true if initializes.
   */
  public boolean isInit()
  {
    return _config != null;
  }

  /**
   * Returns the Resin webApp.
   */
  public WebApp _caucho_getApplication()
  {
    return _webApp;
  }

  public ServletContext getServletContext()
  {
    return _webApp;
  }

  public String getServletName()
  {
    return _config.getServletName();
  }

  public String getInitParameter(String name)
  {
    return _config.getInitParameter(name);
  }

  public Enumeration<String> getInitParameterNames()
  {
    return _config.getInitParameterNames();
  }

  public void log(String msg)
  {
    _webApp.log(getClass().getName() + ": " + msg);
  }

  public void log(String msg, Throwable cause)
  {
    _webApp.log(getClass().getName() + ": " + msg, cause);
  }

  public String getServletInfo()
  {
    return "A JSP Page";
  }

  boolean disableLog()
  {
    JspPropertyGroup jsp = _webApp.getJsp();

    if (jsp != null)
      return jsp.isDisableLog();

    return true;
  }

  /**
   * Returns this servlet's configuration.
   */
  public ServletConfig getServletConfig()
  {
    return _config;
  }

  /**
   * Initialize the response headers.
   */
  public void _caucho_init(HttpServletRequest req, HttpServletResponse res)
  {
    if (_contentType != null)
      res.setContentType(_contentType);
    else
      res.setContentType("text/html");
  }

  /**
   * Returns the Last-Modified time for use in caching.  If the result
   * is &lt;= 0, last-modified caching is disabled.
   *
   * @return the last modified time.
   */
  public long getLastModified(HttpServletRequest request)
  {
    return _caucho_lastModified();
  }

  /**
   * The default Last-Modified time is just the most recently modified file.
   * For JSP files, this is overwritten to always return 0.
   */
  public long _caucho_lastModified()
  {
    return 0;

    /*
    if (_cacheDepends == null) {
      return 0;
    }
    else {
      return calculateLastModified(_depends, _cacheDepends);
    }
    */
  }

  /**
   * Calculate the last modified time for all the dependencies.  The
   * last modified time is the time of the most recently changed
   * cache or source file.
   *
   * @param depends list of the source file dependencies
   * @param cacheDepends list of the cache dependencies
   *
   * @return the last modified time in milliseconds
   */
  public static long calculateLastModified(ArrayList<PersistentDependency> depends,
                                           ArrayList<Depend> cacheDepends)
  {
      long lastModified = 0;

      for (int i = 0; i < depends.size(); i++) {
        PersistentDependency dependency = depends.get(i);

        if (dependency instanceof Depend) {
          Depend depend = (Depend) dependency;
          long modified = depend.getLastModified();
          if (lastModified < modified)
            lastModified = modified;
        }
      }

      for (int i = 0; cacheDepends != null && i < cacheDepends.size(); i++) {
        Depend depend = cacheDepends.get(i);
        long modified = depend.getLastModified();
        if (lastModified < modified)
          lastModified = modified;
      }

      return lastModified;
  }

  /**
   * The extended service method creates JavaScript global variables
   * from a property map.
   *
   * <p>This method only makes sense for JavaScript templates.  To pass
   * variables to Java templates, use the setAttribute() method of the
   * request.
   *
   * @param properties hashmap of objects to create as JavaScript globals.
   */
  public void pageservice(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PageManager.Entry entry = _entry;
    if (entry != null)
      entry.accessPage();

    long lastModified = getLastModified(req);

    if (lastModified > 0) {
      if (_calendar == null)
        _calendar = new QDate();

      String etag = _etag;
      if (lastModified != _lastModified) {
        CharBuffer cb = new CharBuffer();
        cb.append('"');
        Base64.encode(cb, lastModified);
        cb.append('"');
        etag = cb.close();
        _etag = etag;

        synchronized (_calendar) {
          _calendar.setGMTTime(lastModified);
          _lastModifiedString = _calendar.printDate();
        }

        _lastModified = lastModified;
      }

      String ifMatch = req.getHeader("If-None-Match");
      if (etag.equals(ifMatch)) {
        res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }

      String ifModifiedSince = req.getHeader("If-Modified-Since");
      if (_lastModifiedString.equals(ifModifiedSince)) {
        res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }

      res.setHeader("ETag", etag);
      res.setHeader("Last-Modified", _lastModifiedString);
    }

    // jsp/0510, jsp/17f?
    if (res instanceof CauchoResponse) {
      // JspResponse jspResponse = new JspResponse((CauchoResponse) res);

      // service(req, jspResponse);

      service(req, res);
    }
    else {
      // The wrapping is needed to handle the output stream.
      ToCharResponseAdapter resAdapt = ToCharResponseAdapter.create(res);
      // resAdapt.setRequest(req);
      //resAdapt.init(res);

      try {
        service(req, resAdapt);
      } finally {
        resAdapt.close();
        ToCharResponseAdapter.free(resAdapt);
      }
    }
  }

  protected void setDead()
  {
    _isDead = true;
  }

  public boolean isDead()
  {
    return _isDead;
  }

  /**
   * Starts recompiling.  Returns false if the recompiling has already
   * started or if the page has already been destroyed.
   */
  public boolean startRecompiling()
  {
    boolean allowRecompiling;

    synchronized (this) {
      allowRecompiling = _isDead || ! _isRecompiling;
      _isRecompiling = true;
    }

    return allowRecompiling;
  }

  String getErrorPage() { return null; }

  /**
   * Marks the page as used.
   */
  public void _caucho_use()
  {
    synchronized (this) {
      _useCount++;
    }
  }

  /**
   * Marks the page as free.
   */
  public void _caucho_free()
  {
    synchronized (this) {
      _useCount--;
    }

    if (_useCount <= 0)
      destroy();
  }

  public void destroy()
  {
    if (_isDead)
      return;

    _isDead = true;
    /*
    if (! _isDead)
      throw new IllegalStateException();
    */

    _entry = null;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClass().getClassLoader());

      _caucho_log.fine(getClass().getName() + " destroy");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
