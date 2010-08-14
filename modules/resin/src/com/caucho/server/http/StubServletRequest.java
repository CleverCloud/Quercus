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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.JanusContext;
import com.caucho.servlet.JanusListener;
import com.caucho.util.NullEnumeration;
import com.caucho.vfs.ReadStream;

/**
 * Used when there isn't any actual request object, e.g. for calling
 * run-at servlets.
 */
public class StubServletRequest implements CauchoRequest {
  private static final Logger log
    = Logger.getLogger(StubServletRequest.class.getName());
  
  private HashMap _attributes;

  public StubServletRequest()
  {
  }

  public Object getAttribute(String name)
  {
    if (_attributes != null)
      return _attributes.get(name);
    else
      return null;
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if (_attributes != null)
      return Collections.enumeration(_attributes.keySet());
    else
      return (Enumeration) NullEnumeration.create();
  }
  
  public void setAttribute(String name, Object value)
  {
    if (_attributes == null)
      _attributes = new HashMap();

    _attributes.put(name, value);
  }
  
  public void removeAttribute(String name)
  {
    if (_attributes != null)
      _attributes.remove(name);
  }
    
  public boolean initStream(ReadStream rawStream, ReadStream realStream)
  {
    return false;
  }
  
  public String getCharacterEncoding() { return "UTF-8"; }
  public void setCharacterEncoding(String encoding) { }
  public int getContentLength() { return -1; }
  public String getContentType() { return "application/octet-stream"; }

  public String getParameter(String name) { return null; }
  public Enumeration<String> getParameterNames()
  {
    return (Enumeration) NullEnumeration.create();
  }
  public String []getParameterValues(String name) { return null; }
  public Map<String,String[]> getParameterMap() { return null; }
  public String getProtocol() { return "none"; }
  
  public String getRemoteAddr() { return "127.0.0.1"; }
  public String getRemoteHost() { return "127.0.0.1"; }
  public int getRemotePort() { return 6666; }
  public String getLocalAddr() { return "127.0.0.1"; }
  public String getLocalHost() { return "127.0.0.1"; }
  public String getLocalName() { return "127.0.0.1"; }
  public int getLocalPort() { return 6666; }
  public String getScheme() { return "cron"; }
  public String getServerName() { return "127.0.0.1"; }
  public int getServerPort() { return 0; }
    
  public String getRealPath(String path) { return null; }
  public Locale getLocale() { return null; }
  public Enumeration<Locale> getLocales()
  { return (Enumeration) NullEnumeration.create(); }
  public boolean isSecure() { return true; }
  public RequestDispatcher getRequestDispatcher(String uri) { return null; }

  public String getMethod() { return "GET"; }
  public String getServletPath() { return null; }
  public String getContextPath() { return null; }
  public String getPathInfo() { return null; }
  public String getPathTranslated() { return null; }
  public String getRequestURI () { return null; }
  public StringBuffer getRequestURL ()
  {
    return new StringBuffer("http://localhost");
  }
  public int getUriLength() { return 0; }
  public byte []getUriBuffer() { return null; }
  
  public String getQueryString() { return null; }
  
  public String getPageURI()
  {
    return getRequestURI();
  }
  
  public String getPageContextPath()
  {
    return getContextPath();
  }
  
  public String getPageServletPath()
  {
    return getServletPath();
  }
  
  public String getPagePathInfo()
  {
    return getPathInfo();
  }
  
  public String getPageQueryString()
  {
    return getQueryString();
  }

  public String getHeader(String header) { return null; }
  public int getIntHeader(String header) { return 0; }
  public long getDateHeader(String header) { return 0; }
  
  public Enumeration getHeaders(String header)
  {
    return (Enumeration) NullEnumeration.create();
  }
  
  public Enumeration<String> getHeaderNames()
  {
    return (Enumeration) NullEnumeration.create();
  }

  public String getAuthType() { return null; }
  public String getRemoteUser() { return null; }
  public java.security.Principal getUserPrincipal() { return null; }

  public boolean isUserInRole(String str) { return false; }
  
  public SocketLinkDuplexController upgradeProtocol(SocketLinkDuplexListener handler)
  {
    return null;
  }

  public AbstractHttpRequest getAbstractHttpRequest()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public ServletRequest getRequest()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public ServletResponse getServletResponse()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Authenticate the user.
   */
  public boolean login(boolean isFail)
  {
    return false;
  }

  public boolean isLoginRequested()
  {
    return false;
  }

  @Override
  public void requestLogin()
  {
  }
  
  public boolean isClientDisconnect()
  {
    return false;
  }
  
  public void clientDisconnect()
  {
  }

  public boolean isKeepaliveAllowed()
  {
    return false;
  }

  public boolean isDuplex()
  {
    return false;
  }

  public boolean isComet()
  {
    return false;
  }

  public boolean isSuspend()
  {
    return false;
  }

  public void killKeepalive()
  {
  }

  public void setHasCookie()
  {
  }

  public Cookie getCookie(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public HttpSession getMemorySession()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean hasRequest()
  {
    return true;
  }

  public boolean isTop()
  {
    return true;
  }

  public boolean getHasCookie()
  {
    return true;
  }

  public void setVaryCookie(String cookie)
  {
  }

  public boolean getVaryCookies()
  {
    return true;
  }

  public void setHeader(String k, String v)
  {
  }

  public int getRequestDepth(int i)
  {
    return i + 1;
  }
  
  public ReadStream getStream()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public WebApp getWebApp()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Part getPart(String name)
    throws IOException, ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean authenticate(HttpServletResponse response)
      throws IOException,ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void login(String username, String password)
    throws ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void logout()
    throws ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isRequestedSessionIdValid()
  {
    return false;
  }
  
  public boolean isRequestedSessionIdFromCookie()
  {
    return false;
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    return false;
  }
  
  public boolean isRequestedSessionIdFromUrl()
  {
    return false;
  }
  
  public String getRequestedSessionId()
  {
    return null;
  }

  public HttpSession getSession()
  {
    return getSession(true);
  }

  public HttpSession getSession(boolean create)
  {
    return null;
  }
  
  public Cookie []getCookies()
  {
    return null;
  }

  public boolean isSyntheticCacheHeader()
  {
    return false;
  }

  public void setSyntheticCacheHeader(boolean isSynthetic)
  {
  }

  public DispatcherType getDispatcherType()
  {
    return null;
  }
  
  public void addAsyncListener(AsyncListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void addAsyncListener(AsyncListener listener,
                               ServletRequest request,
                               ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AsyncContext getAsyncContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public ServletContext getServletContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isAsyncStarted()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isAsyncSupported()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setAsyncSupported(boolean isAsyncSupported) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AsyncContext startAsync()
    throws IllegalStateException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public JanusContext startWebSocket(JanusListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public ServletInputStream getInputStream()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isSessionIdFromCookie()
  {
    return true;
  }

  public String getSessionId()
  {
    return null;
  }

  public void setSessionId(String sessionId)
  {
  }
}
  
