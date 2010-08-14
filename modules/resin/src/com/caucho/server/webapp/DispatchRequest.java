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

package com.caucho.server.webapp;

import com.caucho.i18n.CharacterEncoding;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.Form;
import com.caucho.server.http.RequestAdapter;
import com.caucho.server.http.ServletInputStreamImpl;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.HashMapImpl;
import com.caucho.vfs.BufferedReaderAdapter;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.ReadStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/**
 * sub-request for a include() page
 */
class DispatchRequest extends RequestAdapter {
  protected static final Logger log
    = Logger.getLogger(DispatchRequest.class.getName());

  private static final FreeList<DispatchRequest> _freeList
    = new FreeList<DispatchRequest>(32);

  private WebApp _webApp;
  private WebApp _oldWebApp;
  private Invocation _invocation;
  private Form _formParser;
  private HashMapImpl<String,String[]> _form;
  protected ReadStream _readStream;
  protected ServletInputStreamImpl _is;
  // Reader for post contents
  private BufferedReaderAdapter _bufferedReader;
  private String _method;
  private String _uri;
  private String _servletPath;
  private String _pathInfo;
  private String _queryString;
  private String _addedQuery;
  private SessionImpl _session;

  private String _pageUri;
  private String _pageContextPath;
  private String _pageServletPath;
  private String _pagePathInfo;
  private String _pageQueryString;

  protected DispatchRequest()
  {
  }

  /**
   * Creates a dispatch request.
   */
  public static DispatchRequest createDispatch()
  {
    DispatchRequest req = _freeList.allocate();
    if (req == null)
      req = new DispatchRequest();

    return req;
  }

  void init(Invocation invocation,
            WebApp webApp,
            WebApp oldWebApp,
            HttpServletRequest request,
            HttpServletResponse response,
            String method, String uri,
            String servletPath, String pathInfo,
            String queryString, String addedQuery)
    throws ServletException
  {
    super.init(request, response, webApp);

    _invocation = invocation;
    _webApp = webApp;
    _oldWebApp = oldWebApp;

    _form = null;

    _readStream = null;
    _is = null;

    _bufferedReader = null;

    _method = method;
    _uri = uri;
    _servletPath = servletPath;
    _pathInfo = pathInfo;
    _queryString = queryString;
    _addedQuery = addedQuery;

    _pageUri = null;
    _pageContextPath = null;
    _pageServletPath = null;
    _pagePathInfo = null;
    _pageQueryString = null;

    _session = null;
  }

  void setStream(ReadStream readStream)
  {
    _readStream = readStream;
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  public String getMethod()
  {
    return _method;
  }

  public String getRequestURI()
  {
    return _uri;
  }

  /**
   * Returns the URL for the request
   */
  public StringBuffer getRequestURL()
  {
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");
    sb.append(getServerName());
    if (getServerPort() > 0 &&
        getServerPort() != 80 &&
        getServerPort() != 443) {
      sb.append(":");
      sb.append(getServerPort());
    }
    sb.append(getRequestURI());

    return sb;
  }

  void setPageURI(String uri)
  {
    _pageUri = uri;
  }

  public String getPageURI()
  {
    return _pageUri;
  }

  /**
   * Returns the servlet context prefix of the webApp.
   */
  public String getContextPath()
  {
    if (_webApp != null)
      return _webApp.getContextPath();
    else
      return "/";
  }

  /**
   * Sets the servlet context prefix of page.
   */
  void setPageContextPath(String contextPath)
  {
    _pageContextPath = contextPath;
  }

  /**
   * Gets the servlet context prefix of page.
   */
  public String getPageContextPath()
  {
    return _pageContextPath;
  }

  public String getServletPath()
  {
    return _servletPath;
  }

  public String getPageServletPath()
  {
    return _pageServletPath;
  }

  void setPageServletPath(String servletPath)
  {
    _pageServletPath = servletPath;
  }

  public String getPathInfo()
  {
    return _pathInfo;
  }

  public String getPagePathInfo()
  {
    return _pagePathInfo;
  }

  void setPagePathInfo(String pathInfo)
  {
    _pagePathInfo = pathInfo;
  }

  public String getQueryString()
  {
    return _queryString;
  }

  void setPageQueryString(String queryString)
  {
    _pageQueryString = queryString;
  }

  public String getPageQueryString()
  {
    return _pageQueryString;
  }

  public Map getParameterMap()
  {
    if (_form == null)
      _form = parseQuery();

    return _form;
  }

  public Enumeration getParameterNames()
  {
    if (_form == null)
      _form = parseQuery();

    return Collections.enumeration(_form.keySet());
  }

  public String []getParameterValues(String name)
  {
    if (_form == null)
      _form = parseQuery();

    return _form.get(name);
  }

  public String getParameter(String name)
  {
    String []values = getParameterValues(name);
    if (values == null || values.length == 0)
      return null;

    return values[0];
  }

  private HashMapImpl<String,String[]> parseQuery()
  {
    HashMapImpl<String,String[]> table = new HashMapImpl<String,String[]>();

    String defaultEncoding = CharacterEncoding.getLocalEncoding();
    String charEncoding = getCharacterEncoding();
    if (charEncoding == null)
      charEncoding = defaultEncoding;
    String javaEncoding = Encoding.getJavaName(charEncoding);

    if (_addedQuery != null) {
      try {
        if (_formParser == null)
          _formParser = new Form();
        _formParser.parseQueryString(table, _addedQuery, javaEncoding, false);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    Enumeration en = super.getParameterNames();
    while (en.hasMoreElements()) {
      String key = (String) en.nextElement();
      String []oldValues = (String []) super.getParameterValues(key);
      String []newValues = (String []) table.get(key);

      if (oldValues == null) {
      }
      else if (newValues == null)
        table.put(key, oldValues);
      else {
        String []next = new String[oldValues.length + newValues.length];
        System.arraycopy(newValues, 0, next, 0, newValues.length);
        System.arraycopy(oldValues, 0, next, newValues.length,
                         oldValues.length);

        table.put(key, next);
      }
    }

    return table;
  }

  public String getRealPath(String path)
  {
    return _webApp.getRealPath(path);
  }

  public String getPathTranslated()
  {
    if (_pathInfo != null)
      return getRealPath(_pathInfo);
    else
      return null;
  }

  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path.startsWith("/"))
      return _webApp.getRequestDispatcher(path);
    else
      return _webApp.getRequestDispatcher(getPwd() + path);
  }

  public String getPwd()
  {
    CharBuffer cb = CharBuffer.allocate();

    String servletPath = getPageServletPath();
    if (servletPath != null)
      cb.append(servletPath);
    String pathInfo = getPagePathInfo();
    if (pathInfo != null)
      cb.append(pathInfo);

    int p = cb.lastIndexOf('/');
    if (p >= 0)
      cb.setLength(p);
    cb.append('/');

    return cb.close();
  }

  public ReadStream getStream()
    throws IOException
  {
    if (_readStream == null && getRequest() instanceof CauchoRequest)
      return ((CauchoRequest) getRequest()).getStream();
    else
      return _readStream;
  }

  public ServletInputStream getInputStream()
    throws IOException
  {
    if (_readStream == null)
      return super.getInputStream();

    if (_is == null)
      _is = new ServletInputStreamImpl();
    _is.init(_readStream);

    return _is;
  }

  public BufferedReader getReader()
    throws IOException
  {
    if (_readStream == null)
      return super.getReader();

    if (_bufferedReader == null)
      _bufferedReader = new BufferedReaderAdapter(getStream());

    // bufferedReader is just an adapter to get the signature right.
    _bufferedReader.init(getStream());

    return _bufferedReader;
  }

  /**
   * Returns the current session.
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  @Override
  public HttpSession getSession(boolean create)
  {
    SessionManager manager = null;

    if (_webApp != null)
      manager = _webApp.getSessionManager();

    if (manager != null)
      setVaryCookie(manager.getCookieName());

    if (_session != null && _session.isValid()) {
      setHasCookie();

      return _session;
    }

    if (_webApp == _oldWebApp) {
      HttpSession hSession = super.getSession(create);
      if (hSession != null)
        setHasCookie();

      return hSession;
    }
    else {
      SessionImpl oldSession = _session;
      _session = createSession(create, oldSession != null);
    }

    if (_session != null)
      setHasCookie();

    return _session;
  }

  /**
   * Returns the current session.
   *
   * XXX: duplicated in AbstractHttpRequest
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  private SessionImpl createSession(boolean create, boolean hasOldSession)
  {
    SessionManager manager = getSessionManager();

    String id = getRequestedSessionId();

    long now = Alarm.getCurrentTime();

    SessionImpl session;

    if (id != null && id.length() > 6) {
      session = manager.getSession(id, now, create,
                                   isRequestedSessionIdFromCookie());
      if (session == null) {
      }
      else if (session.isValid()) {
        if (session != null)
          setHasCookie();
        if (! session.getId().equals(id) && manager.enableSessionCookies()) {
          HttpServletResponse response = getResponse();

          if (response instanceof CauchoResponse)
            ((CauchoResponse) getResponse()).setSessionId(session.getId());
        }

        return session;
      }
    }
    else
      id = null;

    if (! create)
      return null;

    // Must accept old ids because different webApps in the same
    // server must share the same cookie
    //
    // But, if the session group doesn't match, then create a new
    // session.

    session = manager.createSession(id, now, this,
                                    isRequestedSessionIdFromCookie());

    if (session != null)
      setHasCookie();

    if (session.getId().equals(id))
      return session;

    if (manager.enableSessionCookies()) {
      HttpServletResponse response = getResponse();

      if (response instanceof CauchoResponse)
        ((CauchoResponse) getResponse()).setSessionId(session.getId());
    }

    return session;
  }

  public boolean isLoginRequested()
  {
    if (! (getRequest() instanceof CauchoRequest))
      return false;
    else
      return ((CauchoRequest) getRequest()).isLoginRequested();
  }

  public boolean login(boolean isFail)
  {
    if (! (getRequest() instanceof CauchoRequest))
      return false;
    else
      return ((CauchoRequest) getRequest()).login(isFail);
  }
  
  /**
   * Returns true if the user represented by the current request
   * plays the named role.
   *
   * @param role the named role to test.
   * @return true if the user plays the role.
   */
  public boolean isUserInRole(String role)
  {
    HashMap<String,String> roleMap = _invocation.getSecurityRoleMap();
    
    if (roleMap != null) {
      String linkRole = roleMap.get(role);
      
      if (linkRole != null)
        role = linkRole;
    }

    return super.isUserInRole(role);
  }

  /**
   * Cleans up at the end of the request
   */
  public void finishRequest()
    throws IOException
  {
    SessionImpl session = _session;
    _session = null;

    if (session != null)
      session.finishRequest();
  }

  /**
   * Frees the request.
   */
  public static void free(DispatchRequest req)
  {
    req.free();

    _freeList.free(req);
  }

  /**
   * Clears variables.
   */
  protected void free()
  {
    super.free();

    _session = null;
    _webApp = null;
    _oldWebApp = null;
    _readStream = null;
    _invocation = null;

    if (_is != null)
      _is.free();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getRequest() + "]";
  }
}
