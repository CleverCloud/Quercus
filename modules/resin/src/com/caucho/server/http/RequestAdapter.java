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

package com.caucho.server.http;

import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * Any requests that depends on an underlying request, like
 * include() requests or adapters for other servlet engines.
 */
public class RequestAdapter extends RequestWrapper
  implements CauchoRequest {
  private static final L10N L = new L10N(RequestAdapter.class);

  static final int MAX_DEPTH = 64;
  
  public static String REQUEST_URI = "javax.servlet.include.request_uri";
  public static String CONTEXT_PATH = "javax.servlet.include.context_path";
  public static String SERVLET_PATH = "javax.servlet.include.servlet_path";
  public static String PATH_INFO = "javax.servlet.include.path_info";
  public static String QUERY_STRING = "javax.servlet.include.query_string";
  
  public static String STATUS_CODE = "javax.servlet.error.status_code";
  public static String EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  public static String MESSAGE = "javax.servlet.error.message";
  public static String EXCEPTION = "javax.servlet.error.exception";
  public static String ERROR_URI = "javax.servlet.error.request_uri";
  public static String SERVLET_NAME = "javax.servlet.error.servlet_name";
  
  public static String JSP_EXCEPTION = "javax.servlet.jsp.jspException";
  
  public static String SHUTDOWN = "com.caucho.shutdown";

  private static final FreeList<RequestAdapter> _freeList
    = new FreeList<RequestAdapter>(16);
  
  // for real adapters
  private WebApp _webApp;
  private HttpServletResponse _response;

  private HashMap<String,String> _roleMap;

  protected RequestAdapter()
  {
  }
  
  protected RequestAdapter(HttpServletRequest request, WebApp app)
  {
    super(request);
    
    _webApp = app;
  }

  /**
   * Creates a new RequestAdapter.
   */
  public static RequestAdapter create(HttpServletRequest request,
                                      WebApp app)
  {
    RequestAdapter reqAdapt = _freeList.allocate();

    if (reqAdapt == null)
      return new RequestAdapter(request, app);
    else {
      reqAdapt.setRequest(request);
      reqAdapt._webApp = app;

      return reqAdapt;
    }
  }

  /**
   * Creates a new RequestAdapter.
   */
  public static RequestAdapter create()
  {
    RequestAdapter reqAdapt = _freeList.allocate();

    if (reqAdapt != null)
      return reqAdapt;
    else
      return new RequestAdapter();
  }

  public void init(HttpServletRequest request,
                   HttpServletResponse response,
                   WebApp app)
    throws ServletException
  {
    setRequest(request);
    
    _response = response;
    _webApp = app;

    if (request == this
        || (request instanceof CauchoRequest
            && ((CauchoRequest) request).getRequestDepth(0) > MAX_DEPTH)) {
      throw new ServletException(L.l("too many servlet includes `{0}'",
                                     request.getRequestURI()));
    }
  }

  public boolean isTop()
  {
    return false;
  }
  
  public void setWebApp(WebApp app)
  {
    _webApp = app;
  }

  public AbstractHttpRequest getAbstractHttpRequest()
  {
    HttpServletRequest request = getRequest();

    if (request instanceof CauchoRequest)
      return ((CauchoRequest) request).getAbstractHttpRequest();
    else
      return null;
  }
  
  protected HttpServletResponse getResponse()
  {
    return _response;
  }

  public void setResponse(CauchoResponse response)
  {
    _response = response;
  }

  /**
   * Returns the underlying read stream.
   */
  public ReadStream getStream() throws IOException
  {
    if (getRequest() instanceof CauchoRequest)
      return ((CauchoRequest) getRequest()).getStream();
    else
      return null;
  }

  /**
   * Returns the URI for the current page: included or top-level.
   */
  public String getPageURI()
  {
    String uri = (String) getAttribute(REQUEST_URI);
    
    if (uri != null)
      return uri;
    else
      return getRequestURI();
  }

  public static String getPageURI(HttpServletRequest request)
  {
    String uri = (String) request.getAttribute(REQUEST_URI);
    
    if (uri != null)
      return uri;
    else
      return request.getRequestURI();
  }

  public String getPageContextPath()
  {
    String contextPath = (String) getAttribute(CONTEXT_PATH);
    
    if (contextPath != null)
      return contextPath;
    else
      return getContextPath();
  }

  public static String getPageContextPath(HttpServletRequest request)
  {
    String contextPath = (String) request.getAttribute(CONTEXT_PATH);
    
    if (contextPath != null)
      return contextPath;
    else
      return request.getContextPath();
  }
  
  /**
   * Returns the servlet-path for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public String getPageServletPath()
  {
    String servletPath = (String) getAttribute(SERVLET_PATH);
    
    if (servletPath != null)
      return servletPath;
    else
      return getServletPath();
  }
  
  /**
   * Returns the servlet-path for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public static String getPageServletPath(HttpServletRequest request)
  {
    String servletPath = (String) request.getAttribute(SERVLET_PATH);
    
    if (servletPath != null)
      return servletPath;
    else
      return request.getServletPath();
  }

  /**
   * Returns the path-info for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public String getPagePathInfo()
  {
    String uri = (String) getAttribute(REQUEST_URI);
    
    if (uri != null)
      return (String) getAttribute(PATH_INFO);
    else
      return getPathInfo();
  }

  /**
   * Returns the path-info for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public static String getPagePathInfo(HttpServletRequest request)
  {
    String uri = (String) request.getAttribute(REQUEST_URI);
    
    if (uri != null)
      return (String) request.getAttribute(PATH_INFO);
    else
      return request.getPathInfo();
  }
  
  /**
   * Returns the query-string for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public String getPageQueryString()
  {
    String uri = (String) getAttribute(REQUEST_URI);
    
    if (uri != null)
      return (String) getAttribute(QUERY_STRING);
    else
      return getQueryString();
  }
  
  /**
   * Returns the query-string for the current page, i.e. this will return the
   * url of the include page, not the original request.
   */
  public static String getPageQueryString(HttpServletRequest request)
  {
    String uri = (String) request.getAttribute(REQUEST_URI);
    
    if (uri != null)
      return (String) request.getAttribute(QUERY_STRING);
    else
      return request.getQueryString();
  }

  public int getRequestDepth(int depth)
  {
    if (depth > MAX_DEPTH)
      throw new RuntimeException(L.l("too many request dispatchers"));

    ServletRequest req = getRequest();
    while (req != null) {
      if (req instanceof CauchoRequest)
        return ((CauchoRequest) req).getRequestDepth(depth + 1);
      else if (req instanceof ServletRequestWrapper) {
        ServletRequestWrapper reqWrap = (ServletRequestWrapper) req;

        req = reqWrap.getRequest();
      }
      else
        break;
    }

    return depth + 2;
  }

  public void setHeader(String key, String value)
  {
  }

  public void setSyntheticCacheHeader(boolean isSynthetic)
  {
  }

  public boolean isSyntheticCacheHeader()
  {
    return false;
  }
  
  public WebApp getWebApp()
  {
    return _webApp;
  }

  public void setVaryCookie(String cookie)
  {
    // super.setVaryCookie(cookie);

    if (getRequest() instanceof CauchoRequest)
      ((CauchoRequest) getRequest()).setVaryCookie(cookie);
  }

  public boolean getVaryCookies()
  {
    // super.setVaryCookie(cookie);

    if (getRequest() instanceof CauchoRequest)
      return ((CauchoRequest) getRequest()).getVaryCookies();
    else
      return false;
  }

  public void setHasCookie()
  {
    // super.setHasCookie();

    if (getRequest() instanceof CauchoRequest)
      ((CauchoRequest) getRequest()).setHasCookie();
  }

  public boolean getHasCookie()
  {
    // super.setHasCookie();

    if (getRequest() instanceof CauchoRequest)
      return ((CauchoRequest) getRequest()).getHasCookie();
    else
      return false;
  }
      
  public HttpSession getMemorySession()
  {
    return getSession(false);
  }
  
  public HttpSession getSession(boolean create)
  {
    SessionManager manager = getSessionManager();
    
    setVaryCookie(getCookieName(manager));

    HttpSession session = super.getSession(create);

    if (session != null)
      setHasCookie();
    
    return session;
  }

  public String getRequestedSessionId()
  {
    SessionManager manager = getSessionManager();
    
    setVaryCookie(getCookieName(manager));

    String id = super.getRequestedSessionId();

    if (id != null)
      setHasCookie();

    return id;
  }

  public boolean isRequestedSessionIdValid()
  {
    SessionManager manager = getSessionManager();
    
    setVaryCookie(getCookieName(manager));

    boolean isValid = super.isRequestedSessionIdValid();

    if (isValid)
      setHasCookie();

    return isValid;
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    SessionManager manager = getSessionManager();
    
    setVaryCookie(getCookieName(manager));

    boolean isValid = super.isRequestedSessionIdFromCookie();
    if (isValid)
      setHasCookie();

    return isValid;
  }

  public boolean isRequestedSessionIdFromURL()
  {
    SessionManager manager = getSessionManager();

    setVaryCookie(getCookieName(manager));

    boolean isValid = super.isRequestedSessionIdFromURL();
    
    if (isValid)
      setHasCookie();

    return isValid;
  }

  public boolean isSessionIdFromCookie()
  {
    CauchoRequest cReq = getCauchoRequest();

    if (cReq != null)
      return cReq.isSessionIdFromCookie();
    else
      return isRequestedSessionIdFromCookie();
  }

  public String getSessionId()
  {
    CauchoRequest cReq = getCauchoRequest();

    if (cReq != null)
      return cReq.getSessionId();
    else
      return getRequestedSessionId();
  }

  public void setSessionId(String sessionId)
  {
    CauchoRequest cReq = getCauchoRequest();

    if (cReq != null)
      cReq.setSessionId(sessionId);
  }

  protected final SessionManager getSessionManager()
  {
    WebApp app = getWebApp();
    if (app != null)
      return app.getSessionManager();
    else
      return null;
  }

  protected final String getCookieName(SessionManager manager)
  {
    if (isSecure())
      return manager.getCookieName();
    else
      return manager.getSSLCookieName();
  }

  public Cookie []getCookies()
  {
    // page depends on any cookie
    setVaryCookie(null);
    
    Cookie []cookies = super.getCookies();
    if (cookies != null && cookies.length > 0)
      setHasCookie();

    return cookies;
  }

  public Cookie getCookie(String name)
  {
    // page depends on this cookie
    setVaryCookie(name);

    if (getRequest() instanceof CauchoRequest)
      return ((CauchoRequest) getRequest()).getCookie(name);

    Cookie []cookies = super.getCookies();
    for (int i = 0; i < cookies.length; i++) {
      if (cookies[i].getName().equals(name)) {
        setHasCookie();
        return cookies[i];
      }
    }

    return null;
  }

  public boolean isComet()
  {
    return false;
  }

  public boolean isDuplex()
  {
    return false;
  }
  
  public void killKeepalive()
  {
  }
  
  public boolean isKeepaliveAllowed()
  {
    return true;
  }

  public boolean isClientDisconnect()
  {
    return false;
  }

  public void clientDisconnect()
  {
  }

  /**
   * Sets the role map.
   */
  public HashMap<String,String> setRoleMap(HashMap<String,String> map)
  {
    HashMap<String,String> oldMap = _roleMap;
    _roleMap = map;

    return oldMap;
  }

  /**
   * Checks the isUserInRole.
   */
  public boolean isUserInRole(String role)
  {
    if (_roleMap != null) {
      String newRole = _roleMap.get(role);
      
      if (newRole != null)
        role = newRole;
    }

    return super.isUserInRole(role);
  }

  @Override
  public boolean isLoginRequested()
  {
    return false;
  }
  
  @Override
  public void requestLogin()
  {
  }
  
  @Override
  public boolean login(boolean isFail)
  {
    return true;
  }

  public boolean isSuspend()
  {
    return false;
  }

  public boolean hasRequest()
  {
    return false;
  }

  /**
   * @since Servlet 3.0
   */
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return getRequest().authenticate(response);
  }

  /**
   * @since Servlet 3.0
   */
  public Part getPart(String name)
    throws IOException, ServletException
  {
    return getRequest().getPart(name);
  }

  /**
   * @since Servlet 3.0
   */
  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    return getRequest().getParts();
  }

  /**
   * @since Servlet 3.0
   */
  public void login(String username, String password)
    throws ServletException
  {
    getRequest().login(username, password);
  }

  /**
   * @since Servlet 3.0
   */
  public void logout()
    throws ServletException
  {
    getRequest().logout();
  }
 
  public CauchoRequest getCauchoRequest()
  {
    return (CauchoRequest) getRequest();
  }

  /**
   * Frees the adapter for reuse.
   */
  public static void free(RequestAdapter reqAdapt)
  {
    reqAdapt.free();

    _freeList.free(reqAdapt);
  }

  /**
   * Clears the adapter.
   */
  protected void free()
  {
    super.free();
    
    _webApp = null;
    _response = null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '[' + _request + ']'; 
  }
}
