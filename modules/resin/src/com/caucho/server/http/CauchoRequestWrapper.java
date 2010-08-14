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

package com.caucho.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;

public class CauchoRequestWrapper extends AbstractCauchoRequest {
  // the wrapped request
  private HttpServletRequest _request;
  private CauchoResponse _response;

  //
  // ServletRequest
  //
  
  public CauchoRequestWrapper()
  {
  }
  
  public CauchoRequestWrapper(HttpServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  public void setRequest(HttpServletRequest request)
  {
    if (request == null || request == this)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  @Override
  public HttpServletRequest getRequest()
  {
    return _request;
  }
  
  public void setResponse(CauchoResponse response)
  {
    _response = response;
  }
  
  @Override
  public CauchoResponse getResponse()
  {
    return _response;
  }
  
  @Override
  public String getProtocol()
  {
    return _request.getProtocol();
  }
  
  public String getScheme()
  {
    return _request.getScheme();
  }
  
  public String getServerName()
  {
    return _request.getServerName();
  }
  
  public int getServerPort()
  {
    return _request.getServerPort();
  }
  
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }
  
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }
  
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }
  
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }
  
  public String getLocalName()
  {
    return _request.getLocalName();
  }
  
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }
  
  public String getParameter(String name)
  {
    return _request.getParameter(name);
  }
  
  public Map getParameterMap()
  {
    return _request.getParameterMap();
  }
  
  @Override
  public String []getParameterValues(String name)
  {
    return _request.getParameterValues(name);
  }
  
  @Override
  public Enumeration<String> getParameterNames()
  {
    return _request.getParameterNames();
  }
  
  @Override
  public ServletInputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }
  
  @Override
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }
  
  @Override
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }
  
  @Override
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }
  
  @Override
  public int getContentLength()
  {
    return _request.getContentLength();
  }
  
  @Override
  public String getContentType()
  {
    return _request.getContentType();
  }
  
  @Override
  public Locale getLocale()
  {
    return _request.getLocale();
  }
  
  public Enumeration getLocales()
  {
    return _request.getLocales();
  }
  
  public boolean isSecure()
  {
    return _request.isSecure();
  }
  
  public Object getAttribute(String name)
  {
    return _request.getAttribute(name);
  }
  
  public void setAttribute(String name, Object o)
  {
    _request.setAttribute(name, o);
  }
  
  @Override
  public Enumeration<String> getAttributeNames()
  {
    return _request.getAttributeNames();
  }
  
  @Override
  public void removeAttribute(String name)
  {
    _request.removeAttribute(name);
  }
  
  @Override
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null || path.length() == 0)
      return null;
    else if (path.charAt(0) == '/')
      return getWebApp().getRequestDispatcher(path);
    else {
      CharBuffer cb = new CharBuffer();

      WebApp webApp = getWebApp();

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
      cb.append(path);

      if (webApp != null)
        return webApp.getRequestDispatcher(cb.toString());

      return null;
    }
  }

  /*
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }
  */

  public ServletContext getServletContext()
  {
    return getWebApp();
  }

  public AsyncContext startAsync()
    throws IllegalStateException
  {
    return _request.startAsync();
  }

  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException
  {
    return _request.startAsync(servletRequest, servletResponse);
  }

  public AsyncContext getAsyncContext()
  {
    return _request.getAsyncContext();
  }

  public boolean isAsyncStarted()
  {
    return _request.isAsyncStarted();
  }

  public boolean isAsyncSupported()
  {
    return _request.isAsyncSupported();
  }

  public boolean isWrapperFor(ServletRequest wrapped)
  {
    return _request == wrapped;
  }

  public boolean isWrapperFor(Class<?> wrappedType)
  {
    return wrappedType.isAssignableFrom(_request.getClass());
  }

  public DispatcherType getDispatcherType()
  {
    return _request.getDispatcherType();
  }

  //
  // HttpServletRequest
  //
  
  public String getMethod()
  {
    return _request.getMethod();
  }
  
  public String getRequestURI()
  {
    return _request.getRequestURI();
  }

  /**
   * Returns the URL for the request
   */
  @Override
  public StringBuffer getRequestURL()
  {
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");

    sb.append(getServerName());
    int port = getServerPort();

    if (port > 0 &&
        port != 80 &&
        port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }
  
  @Override
  public String getContextPath()
  {
    return _request.getContextPath();
  }
  
  @Override
  public String getServletPath()
  {
    return _request.getServletPath();
  }
  
  @Override
  public String getPathInfo()
  {
    return _request.getPathInfo();
  }

  /**
   * Returns the real path of pathInfo.
   */
  @Override
  public String getPathTranslated()
  {
    // server/106w
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }
  
  @Override
  public String getQueryString()
  {
    return _request.getQueryString();
  }
  
  @Override
  public String getHeader(String name)
  {
    return _request.getHeader(name);
  }
  
  @Override
  public Enumeration<String> getHeaders(String name)
  {
    return _request.getHeaders(name);
  }
  
  @Override
  public Enumeration<String> getHeaderNames()
  {
    return _request.getHeaderNames();
  }
  
  @Override
  public int getIntHeader(String name)
  {
    return _request.getIntHeader(name);
  }
  
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }
  
  public Cookie []getCookies()
  {
    return _request.getCookies();
  }

  public String getRequestedSessionId()
  {
    return _request.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return _request.isRequestedSessionIdValid();
  }
  
  public boolean isRequestedSessionIdFromCookie()
  {
    return _request.isRequestedSessionIdFromCookie();
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    return _request.isRequestedSessionIdFromURL();
  }

  @Override
  public void setSessionId(String sessionId)
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setSessionId(sessionId);
  }

  @Override
  public String getSessionId()
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getSessionId();
    else
      return null;
  }

  @Override
  public boolean isSessionIdFromCookie()
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isSessionIdFromCookie();
    else
      return ! _request.isRequestedSessionIdFromURL();
  }
  
  public String getAuthType()
  {
    return _request.getAuthType();
  }
  
  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }
  
  /*
  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }
  */
  
  @Override
  public boolean isRequestedSessionIdFromUrl()
  {
    return _request.isRequestedSessionIdFromUrl();
  }

  /*
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return _request.authenticate(response);
  }
  */

  public Part getPart(String name)
    throws IOException, ServletException
  {
    return _request.getPart(name);
  }

  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    return _request.getParts();
  }
  
  /*
  public void login(String username, String password)
    throws ServletException
  {
    _request.login(username, password);
  }
  */

  public void logout()
    throws ServletException
  {
    _request.logout();
  }
  
  //
  // CauchoRequest
  //
  
  public String getPageURI()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageURI();
  }
  
  public String getPageContextPath()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageContextPath();
  }
  
  public String getPageServletPath()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageServletPath();
  }
  
  public String getPagePathInfo()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPagePathInfo();
  }
  
  public String getPageQueryString()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageQueryString();
  }
  
  public WebApp getWebApp()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getWebApp();
  }
  
  public ReadStream getStream() throws IOException
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getStream();
  }
  
  public int getRequestDepth(int depth)
  {
    if (_request instanceof CauchoRequest) {
      CauchoRequest cRequest = (CauchoRequest) _request;

      return cRequest.getRequestDepth(depth + 1);
    }
    else
      return 0;
  }
  
  public void setHeader(String key, String value)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setHeader(key, value);
  }
  
  public boolean isSyntheticCacheHeader()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isSyntheticCacheHeader();
  }
  
  public void setSyntheticCacheHeader(boolean isSynthetic)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setSyntheticCacheHeader(isSynthetic);
  }
  
  public boolean getVaryCookies()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getVaryCookies();
  }
  
  public void setVaryCookie(String cookie)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setVaryCookie(cookie);
  }
  
  public boolean getHasCookie()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getHasCookie();
  }
  

  public boolean isTop()
  {
    return false;
  }
  

  public boolean hasRequest()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.hasRequest();
  }
  
  
  public HttpSession getMemorySession()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getMemorySession();
  }
  
  public Cookie getCookie(String name)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getCookie(name);
  }
  
  public void setHasCookie()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setHasCookie();
  }
  
  public void killKeepalive()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.killKeepalive();
  }
  
  public boolean isSuspend()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isSuspend();
  }
  
  public boolean isComet()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isComet();
  }
  
  public boolean isDuplex()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isDuplex();
  }
  
  public boolean isKeepaliveAllowed()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isKeepaliveAllowed();
  }
  
  public boolean isClientDisconnect()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isClientDisconnect();
  }
  
  public void clientDisconnect()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.clientDisconnect();
  }

  @Override
  public boolean isLoginRequested()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isLoginRequested();
  }

  @Override
  public void requestLogin()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    if (cRequest != null)
      cRequest.requestLogin();
  }
 
  /*
  public boolean login(boolean isFail)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.login(isFail);
  }
  */
  
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  
  public AbstractHttpRequest getAbstractHttpRequest()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getAbstractHttpRequest();
  }

  private CauchoRequest getCauchoRequest()
  {
    ServletRequest request = _request;
    
    while (request instanceof ServletRequestWrapper) {
      if (request instanceof CauchoRequest)
        return (CauchoRequest) request;

      request = ((ServletRequestWrapper) request).getRequest();
    }

    if (request instanceof CauchoRequest)
      return (CauchoRequest) request;

    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }
}
