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

package com.caucho.server.dispatch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.config.ConfigException;
import com.caucho.make.DependencyContainer;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

/**
 * Manages dispatching: servlets and filters.
 */
public class ServletMapper {
  private static final Logger log = Logger.getLogger(ServletMapper.class.getName());
  private static final L10N L = new L10N(ServletMapper.class);

  private static final HashSet<String> _welcomeFileResourceMap
    = new HashSet<String>();

  private WebApp _webApp;

  private ServletManager _servletManager;

  private UrlMap<ServletMapping> _servletMap
    = new UrlMap<ServletMapping>();

  private ArrayList<String> _welcomeFileList = new ArrayList<String>();

  private HashMap<String,ServletMapping> _regexpMap
    = new HashMap<String,ServletMapping>();

  private ArrayList<String> _ignorePatterns = new ArrayList<String>();

  private String _defaultServlet;

  //Servlet 3.0 maps serletName to urlPattern
  private Map<String, Set<String>> _urlPatterns
    = new HashMap<String, Set<String>>();

  //Servlet 3.0 urlPattern to servletName
  private Map<String, String> _servletNamesMap
    = new HashMap<String, String>();

  public ServletMapper(WebApp webApp)
  {
    _webApp = webApp;
  }
  
  /**
   * Gets the servlet context.
   */
  public WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Returns the servlet manager.
   */
  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  /**
   * Sets the servlet manager.
   */
  public void setServletManager(ServletManager manager)
  {
    _servletManager = manager;
  }

  /**
   * Adds a servlet mapping
   */
  public void addUrlRegexp(String regexp,
                           String servletName,
                           ServletMapping mapping)
    throws ServletException
  {
    _servletMap.addRegexp(regexp, mapping);
    _regexpMap.put(servletName, mapping);
  }

  /**
   * Adds a servlet mapping
   */
  void addUrlMapping(final String urlPattern,
                     String servletName,
                     ServletMapping mapping,
                     boolean ifAbsent)
    throws ServletException
  {
    try {
      boolean isIgnore = false;

      if (mapping.isInFragmentMode()
         && _servletMap.contains(new FragmentFilter(servletName)))
        return;

      if (servletName == null) {
        throw new ConfigException(L.l("servlets need a servlet-name."));
      }
      else if (servletName.equals("invoker")) {
        // special case
      }
      else if (servletName.equals("plugin_match")
               || servletName.equals("plugin-match")) {
        // special case
        isIgnore = true;
      }
      else if (servletName.equals("plugin_ignore")
               || servletName.equals("plugin-ignore")) {
        if (urlPattern != null)
          _ignorePatterns.add(urlPattern);

        return;
      }
      else if (mapping.getBean() != null) {
      }
      else if (_servletManager.getServlet(servletName) == null)
        throw new ConfigException(L.l("'{0}' is an unknown servlet-name.  servlet-mapping requires that the named servlet be defined in a <servlet> configuration before the <servlet-mapping>.", servletName));

      if ("/".equals(urlPattern)) {
        _defaultServlet = servletName;
      }
      else if (mapping.isStrictMapping()) {
        _servletMap.addStrictMap(urlPattern, null, mapping);
      }
      else
        _servletMap.addMap(urlPattern, mapping, isIgnore, ifAbsent);

      Set<String> patterns = _urlPatterns.get(servletName);

      if (patterns == null) {
        patterns = new HashSet<String>();

        _urlPatterns.put(servletName, patterns);
      }

      _servletNamesMap.put(urlPattern, servletName);

      patterns.add(urlPattern);

      log.config("servlet-mapping " + urlPattern + " -> " + servletName);
    } catch (ServletException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Set<String> getUrlPatterns(String servletName)
  {
    return _urlPatterns.get(servletName);
  }

  /**
   * Sets the default servlet.
   */
  public void setDefaultServlet(String servletName)
    throws ServletException
  {
    _defaultServlet = servletName;
  }

  /**
   * Adds a welcome-file
   */
  public void addWelcomeFile(String fileName)
  {
    ArrayList<String> welcomeFileList
      = new ArrayList<String>(_welcomeFileList);

    welcomeFileList.add(fileName);

    _welcomeFileList = welcomeFileList;
  }

  /**
   * Sets the welcome-file list
   */
  public void setWelcomeFileList(ArrayList<String> list)
  {
    _welcomeFileList = new ArrayList<String>(list);
  }

  public FilterChain mapServlet(ServletInvocation invocation)
    throws ServletException
  {
    String contextURI = invocation.getContextURI();

    String servletName = null;
    ArrayList<String> vars = new ArrayList<String>();

    invocation.setClassLoader(Thread.currentThread().getContextClassLoader());

    ServletConfigImpl config = null;

    if (_servletMap != null) {
      ServletMapping servletMap = _servletMap.map(contextURI, vars);

      if (servletMap != null && servletMap.isServletConfig())
        config = servletMap;

      ServletMapping servletRegexp = _regexpMap.get(servletName);

      if (servletRegexp != null) {
        servletName = servletRegexp.initRegexp(_webApp,
                                               _servletManager,
                                               vars);
      }
      else if (servletMap != null) {
        servletName = servletMap.getServletName();
      }
    }

    if (servletName == null) {
      try {
        InputStream is;
        is = _webApp.getResourceAsStream(contextURI);

        if (is != null) {
          is.close();

          servletName = _defaultServlet;
        }
      } catch (Exception e) {
      }
    }

    MatchResult matchResult = null;

    if (matchResult == null && contextURI.endsWith("j_security_check")) {
      servletName = "j_security_check";
    }

    if (servletName == null) {
      matchResult = matchWelcomeFileResource(invocation, vars);

      if (matchResult != null)
        servletName = matchResult.getServletName();

      if (matchResult != null 
          && ! contextURI.endsWith("/")
          && ! (invocation instanceof SubInvocation)) {
        String contextPath = invocation.getContextPath();

        return new RedirectFilterChain(contextPath + contextURI + "/");
      }

      if (matchResult != null && invocation instanceof Invocation) {
        Invocation inv = (Invocation) invocation;

        inv.setContextURI(matchResult.getContextUri());
        // server/10r9
        // inv.setRawURI(inv.getRawURI() + file);
      }
    }

    /*
    if (servletName == null && matchResult == null) {
      vars.clear();

      matchResult = matchWelcomeServlet(invocation, vars);

      if (matchResult != null)
        servletName = matchResult.getServletName();

      if (servletName != null && ! contextURI.endsWith("/")
          && ! (invocation instanceof SubInvocation)) {
        String contextPath = invocation.getContextPath();

        return new RedirectFilterChain(contextPath + contextURI + "/");
      }
    }
    */

    if (servletName == null) {
      servletName = _defaultServlet;
      vars.clear();

      if (matchResult != null)
        vars.add(matchResult.getContextUri());
      else
        vars.add(contextURI);

      addWelcomeFileDependency(invocation);
    }

    if (servletName == null) {
      log.fine(L.l("'{0}' has no default servlet defined", contextURI));

      return new ErrorFilterChain(404);
    }

    String servletPath = vars.get(0);

    invocation.setServletPath(servletPath);

    if (servletPath.length() < contextURI.length())
      invocation.setPathInfo(contextURI.substring(servletPath.length()));
    else
      invocation.setPathInfo(null);

    ServletMapping regexp = _regexpMap.get(servletName);
    if (regexp != null) {
      servletName = regexp.initRegexp(_webApp, _servletManager, vars);

      if (servletName == null) {
        log.fine(L.l("'{0}' has no matching servlet", contextURI));

        return new ErrorFilterChain(404);
      }

      if (regexp.isServletConfig())
        config = regexp;
    }

    if (servletName.equals("invoker"))
      servletName = handleInvoker(invocation);

    invocation.setServletName(servletName);

    if (log.isLoggable(Level.FINER)) {
      log.finer(_webApp + " map (uri:"
                + contextURI + " -> " + servletName + ")");
    }

    /*
    if (config == null)
      config = _servletManager.getServlet(servletName);
    */
    // server/13f1
    ServletConfigImpl newConfig = _servletManager.getServlet(servletName);
    if (newConfig != null)
      config = newConfig;

    if (config != null) {
      invocation.setSecurityRoleMap(config.getRoleMap());
    }

    FilterChain chain
      = _servletManager.createServletChain(servletName, config, invocation);

    if (chain instanceof PageFilterChain) {
      PageFilterChain pageChain = (PageFilterChain) chain;

      chain = PrecompilePageFilterChain.create(invocation, pageChain);
    }

    return chain;
  }

  private MatchResult matchWelcomeFileResource(ServletInvocation invocation,
                                               ArrayList<String> vars)
  {
    String contextURI = invocation.getContextURI();

    try {
      Path path = _webApp.getCauchoPath(contextURI);

      if (! path.exists())
        return null;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l(
                                 "can't match a welcome file path {0}",
                                 contextURI), e);

      return null;
    }

    // String servletName = null;

    ArrayList<String> welcomeFileList = _welcomeFileList;
    int size = welcomeFileList.size();

    for (int i = 0; i < size; i++) {
      String file = welcomeFileList.get(i);

      try {
        String welcomeURI;

        if (contextURI.endsWith("/"))
          welcomeURI = contextURI + file;
        else
          welcomeURI = contextURI + '/' + file;

        ServletMapping servletMap = _servletMap.map(welcomeURI, vars);

        String servletName = null;
        String servletClass = null;

        if (servletMap != null) {
          servletName = servletMap.getServletName();

          servletClass = servletMap.getServletClassName();
        }

        if (servletName == null && _defaultServlet == null)
          continue;

        if (servletClass == null) {
          ServletConfigImpl servlet = null;

          if (servletName != null)
            servlet = _servletManager.getServlet(servletName);
          else if (_defaultServlet != null)
            servlet = _servletManager.getServlet(_defaultServlet);

          if (servlet != null)
            servletClass = servlet.getServletClassName();
        }

        // server/100l
        if (servletClass != null && isWelcomeFileResource(servletClass)
            || servletName == null) {
          InputStream is;
          is = _webApp.getResourceAsStream(welcomeURI);

          if (is != null)
            is.close();

          if (is == null)
            continue;
        }

        if (servletName != null || _defaultServlet != null) {
          contextURI = welcomeURI;

          return new MatchResult(servletName, contextURI);
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return null;
  }

  private boolean isWelcomeFileResource(String servletName)
  {
    return _welcomeFileResourceMap.contains(servletName);
  }

  private void addWelcomeFileDependency(ServletInvocation servletInvocation)
  {
    if (! (servletInvocation instanceof Invocation))
      return;

    Invocation invocation = (Invocation) servletInvocation;

    String contextURI = invocation.getContextURI();

    DependencyContainer dependencyList = new DependencyContainer();

    WebApp app = (WebApp) _webApp;

    Path contextPath = app.getAppDir().lookup(app.getRealPath(contextURI));

    if (! contextPath.isDirectory())
      return;

    for (int i = 0; i < _welcomeFileList.size(); i++) {
      String file = _welcomeFileList.get(i);

      String realPath = app.getRealPath(contextURI + "/" + file);

      Path path = app.getAppDir().lookup(realPath);

      dependencyList.add(new Depend(path));
    }

    dependencyList.clearModified();

    invocation.setDependency(dependencyList);
  }

  private String handleInvoker(ServletInvocation invocation)
    throws ServletException
  {
    String tail;

    if (invocation.getPathInfo() != null)
      tail = invocation.getPathInfo();
    else
      tail = invocation.getServletPath();

      // XXX: this is really an unexpected, internal error that should never
      //      happen
    if (! tail.startsWith("/")) {
      throw new ConfigException("expected '/' starting " +
                                 " sp:" + invocation.getServletPath() +
                                 " pi:" + invocation.getPathInfo() +
                                 " sn:invocation" + invocation);
    }

    int next = tail.indexOf('/', 1);
    String servletName;

    if (next < 0)
      servletName = tail.substring(1);
    else
      servletName = tail.substring(1, next);

    // XXX: This should be generalized, possibly with invoker configuration
    if (servletName.startsWith("com.caucho")) {
      throw new ConfigException(L.l("servlet '{0}' forbidden from invoker. com.caucho.* classes must be defined explicitly in a <servlet> declaration.",
                                    servletName));
    }
    else if (servletName.equals("")) {
      throw new ConfigException(L.l("invoker needs a servlet name in URL '{0}'.",
                                    invocation.getContextURI()));
    }

    addServlet(servletName);

    String servletPath = invocation.getServletPath();
    if (invocation.getPathInfo() == null) {
    }
    else if (next < 0) {
      invocation.setServletPath(servletPath + tail);
      invocation.setPathInfo(null);
    }
    else if (next < tail.length()) {

      invocation.setServletPath(servletPath + tail.substring(0, next));
      invocation.setPathInfo(tail.substring(next));
    }
    else {
      invocation.setServletPath(servletPath + tail);
      invocation.setPathInfo(null);
    }

    return servletName;
  }

  public String getServletPattern(String uri)
  {
    ArrayList<String> vars = new ArrayList<String>();

    Object value = null;

    if (_servletMap != null)
      value = _servletMap.map(uri, vars);

    if (value == null)
      return null;
    else
      return uri;
  }

  /**
   * Returns the servlet matching patterns.
   */
  public ArrayList<String> getURLPatterns()
  {
    ArrayList<String> patterns = _servletMap.getURLPatterns();

    return patterns;
  }

  public String getServletName(String pattern)
  {
    return _servletNamesMap.get(pattern);
  }

  /**
   * Returns the servlet plugin_ignore patterns.
   */
  public ArrayList<String> getIgnorePatterns()
  {
    return _ignorePatterns;
  }

  private void addServlet(String servletName)
    throws ServletException
  {
    if (_servletManager.getServlet(servletName) != null)
      return;

    ServletConfigImpl config = new ServletConfigImpl();
    config.setServletContext(_webApp);
    config.setServletName(servletName);

    try {
      config.setServletClass(servletName);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }

    config.init();

    _servletManager.addServlet(config);
  }

  public void destroy()
  {
    _servletManager.destroy();
  }

  private class FragmentFilter implements UrlMap.Filter<ServletMapping> {
    private String _servletName;

    public FragmentFilter(String servletName)
    {
      _servletName = servletName;
    }

    @Override
    public boolean isMatch(ServletMapping item)
    {
      return _servletName.equals(item.getServletName());
    }
  }

  private static class MatchResult {
    String _servletName;
    String _contextUri;

    private MatchResult(String servletName, String contextUri)
    {
      _servletName = servletName;
      _contextUri = contextUri;
    }

    public String getServletName()
    {
      return _servletName;
    }

    public String getContextUri()
    {
      return _contextUri;
    }
  }

  static {
    _welcomeFileResourceMap.add("com.caucho.servlets.FileServlet");
    _welcomeFileResourceMap.add("com.caucho.jsp.JspServlet");
    _welcomeFileResourceMap.add("com.caucho.jsp.JspXmlServlet");
    _welcomeFileResourceMap.add("com.caucho.quercus.servlet.QuercusServlet");
    _welcomeFileResourceMap.add("com.caucho.jsp.XtpServlet");
  }
}


