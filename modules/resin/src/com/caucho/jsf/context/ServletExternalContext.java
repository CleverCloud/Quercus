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

package com.caucho.jsf.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caucho.server.http.CauchoRequest;
import com.caucho.util.L10N;

public class ServletExternalContext extends ExternalContext {
  private static final L10N L = new L10N(ServletExternalContext.class);

  private static final Logger log
       = Logger.getLogger(ServletFacesContextImpl.class.getName());


  private ServletContext _webApp;
  private HttpServletRequest _request;
  private HttpServletResponse _response;

  private Map<String,String> _requestParameterMap;
  private Map<String,Object> _applicationMap;
  private Map<String,Object> _requestMap;
  private Map<String,Object> _sessionMap;

  ServletExternalContext(ServletContext webApp,
                         HttpServletRequest request,
                         HttpServletResponse response)
  {
    _webApp = webApp;
    _request = request;
    _response = response;
  }
  
  public void dispatch(String path)
    throws IOException
  {
    try {
      //_request.getRequestDispatcher(path).include(_request, _response);
      RequestDispatcher rd = _request.getRequestDispatcher(path);

      if (rd == null)
        throw new FacesException(L.l("'{0}' is an unknown dispatcher.",
                                     path));

      
      rd.forward(_request, _response);
    } catch (ServletException e) {
      throw new FacesException(e);
    }
  }

  public String encodeActionURL(String url)
  {
    if (url == null)
      throw new NullPointerException();
    
    return _response.encodeURL(url);
  }

  public String encodeNamespace(String name)
  {
    if (name == null)
      throw new NullPointerException();
    
    return name;
  }

  public String encodeResourceURL(String url)
  {
    if (url == null)
      throw new NullPointerException();
    
    return _response.encodeURL(url);
  }

  public Map<String,Object> getApplicationMap()
  {
    if (_applicationMap == null)
      _applicationMap = new ApplicationMap();

    return _applicationMap;
  }

  public String getAuthType()
  {
    return _request.getAuthType();
  }

  public Object getContext()
  {
    return _webApp;
  }

  public String getInitParameter(String name)
  {
    if (name == null)
      throw new NullPointerException();
    
    return _webApp.getInitParameter(name);
  }

  public Map getInitParameterMap()
  {
    HashMap map = new HashMap();

    Enumeration e = _webApp.getInitParameterNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      map.put(name, _webApp.getInitParameter(name));
    }

    return Collections.unmodifiableMap(map);
  }

  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }

  public Object getRequest()
  {
    return _request;
  }

  /**
   * @Since 1.2
   */
  public void setRequest(Object request)
  {
    //hack: trinidad passes an instance of ServerRequest
    // for an internal check that does not seem to affect
    // further operation
    if (request instanceof HttpServletRequest)
      _request = (HttpServletRequest) request;
    else
       log.warning("parameter request is not HttpServletRequest");
  }

  /**
   * @Since 1.2
   */
  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  public String getRequestContextPath()
  {
    return _request.getContextPath();
  }

  public Map<String,Object> getRequestCookieMap()
  {
    HashMap<String,Object> map = new HashMap<String,Object>();

    Cookie []cookies = _request.getCookies();

    if (cookies == null)
      return map;

    for (int i = 0; i < cookies.length; i++) {
      map.put(cookies[i].getName(), cookies[i]);
    }

    return Collections.unmodifiableMap(map);
  }

  public Map<String,String> getRequestHeaderMap()
  {
    HashMap<String,String> map = new HashMap<String,String>();

    Enumeration e = _request.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      
      map.put(name, _request.getHeader(name));
    }

    return Collections.unmodifiableMap(map);
  }

  public Map<String,String[]> getRequestHeaderValuesMap()
  {
    HashMap<String,String[]> map = new HashMap<String,String[]>();

    Enumeration e = _request.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      Enumeration e1 = _request.getHeaders(name);
      ArrayList<String> values = new ArrayList<String>();
      while (e1.hasMoreElements()) {
        values.add((String) e1.nextElement());
      }
      
      map.put(name, values.toArray(new String[values.size()]));
    }

    return Collections.unmodifiableMap(map);
  }

  public Locale getRequestLocale()
  {
    return _request.getLocale();
  }

  public Iterator<Locale> getRequestLocales()
  {
    ArrayList<Locale> locales = new ArrayList<Locale>();

    Enumeration e = _request.getLocales();
    while (e.hasMoreElements())
      locales.add((Locale) e.nextElement());

    return locales.iterator();
  }

  public Map<String,Object> getRequestMap()
  {
    if (_requestMap == null)
      _requestMap = new RequestMap();

    return _requestMap;
  }

  public Map<String,String> getRequestParameterMap()
  {
    if (_requestParameterMap == null) {
      HashMap<String,String> map = new HashMap<String,String>();

      Enumeration e = _request.getParameterNames();
      while (e.hasMoreElements()) {
        String name = (String) e.nextElement();
      
        map.put(name, _request.getParameter(name));
      }

      _requestParameterMap = Collections.unmodifiableMap(map);
    }

    return _requestParameterMap;
  }

  public Iterator<String> getRequestParameterNames()
  {
    return getRequestParameterMap().keySet().iterator();
  }

  public Map<String,String[]> getRequestParameterValuesMap()
  {
    HashMap<String,String[]> map = new HashMap<String,String[]>();

    Enumeration e = _request.getParameterNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      
      map.put(name, _request.getParameterValues(name));
    }

    return Collections.unmodifiableMap(map);
  }

  public String getRequestPathInfo()
  {
    if (_request instanceof CauchoRequest) {
      return ((CauchoRequest) _request).getPagePathInfo();
    }
    else {
      // XXX: include
      
      return _request.getPathInfo();
    }
  }

  public String getRequestServletPath()
  {
    if (_request instanceof CauchoRequest) {
      return ((CauchoRequest) _request).getPageServletPath();
    }
    else {
      // XXX: include
      
      return _request.getServletPath();
    }
  }

  /**
   * @Since 1.2
   */
  public String getRequestCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }

  /**
   * @Since 1.2
   */
  public String getRequestContentType()
  {
    return _request.getContentType();
  }

  /**
   * @Since 1.2
   */
  public String getResponseCharacterEncoding()
  {
    return _response.getCharacterEncoding();
  }

  /**
   * @Since 1.2
   */
  public String getResponseContentType()
  {
    return _response.getContentType();
  }

  public URL getResource(String path)
    throws MalformedURLException
  {
    return _webApp.getResource(path);
  }

  public InputStream getResourceAsStream(String path)
  {
    return _webApp.getResourceAsStream(path);
  }

  public Set<String> getResourcePaths(String path)
  {
    return _webApp.getResourcePaths(path);
  }

  public Object getResponse()
  {
    return _response;
  }

  /**
   * @Since 1.2
   */
  public void setResponse(Object response)
  {
    _response = (HttpServletResponse) response;
  }

  /**
   * @Since 1.2
   */
  public void setResponseCharacterEncoding(String encoding)
  {
    _response.setCharacterEncoding(encoding);
  }

  public Object getSession(boolean create)
  {
    return _request.getSession(create);
  }

  public Map<String,Object> getSessionMap()
  {
    if (_sessionMap == null)
      _sessionMap = new SessionMap(_request.getSession(true));

    return _sessionMap;
  }

  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }

  public boolean isUserInRole(String role)
  {
    return _request.isUserInRole(role);
  }

  public void log(String message)
  {
    _webApp.log(message);
  }

  public void log(String message, Throwable exn)
  {
    _webApp.log(message, exn);
  }

  public void redirect(String url)
    throws IOException
  {
    _response.sendRedirect(url);

    FacesContext.getCurrentInstance().responseComplete();
  }

  class ApplicationMap extends AttributeMap {
    public Object get(String key)
    {
      return _webApp.getAttribute(key);
    }
    
    public Object put(String key, Object value)
    {
      Object oldValue = _webApp.getAttribute(key);

      _webApp.setAttribute(key, value);

      return oldValue;
    }
    
    public Object remove(String key)
    {
      Object value = _webApp.getAttribute(key);

      _webApp.removeAttribute(key);

      return value;
    }

    public Enumeration getNames()
    {
      return _webApp.getAttributeNames();
    }
  }

  class SessionMap extends AttributeMap {
    private HttpSession _session;

    SessionMap(HttpSession session)
    {
      _session = session;
    }
    
    public Object get(String key)
    {
      return _session.getAttribute(key);
    }
    
    public Object put(String key, Object value)
    {
      Object oldValue = _session.getAttribute(key);

      _session.setAttribute(key, value);

      return oldValue;
    }
    
    public Object remove(String key)
    {
      Object value = _session.getAttribute(key);

      _session.removeAttribute(key);

      return value;
    }

    public Enumeration getNames()
    {
      return _session.getAttributeNames();
    }
  }

  class RequestMap extends AttributeMap {
    public Object get(String key)
    {
      return _request.getAttribute(key);
    }
    
    public Object put(String key, Object value)
    {
      Object oldValue = _request.getAttribute(key);

      _request.setAttribute(key, value);

      return oldValue;
    }
    
    public Object remove(String key)
    {
      Object value = _request.getAttribute(key);

      _request.removeAttribute(key);

      return value;
    }

    public Enumeration getNames()
    {
      return _request.getAttributeNames();
    }
  }
}

