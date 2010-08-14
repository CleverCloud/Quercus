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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.inject.Module;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.CauchoRequestWrapper;
import com.caucho.server.http.Form;
import com.caucho.util.HashMapImpl;
import com.caucho.util.IntMap;
import com.caucho.vfs.Encoding;

@Module
public class IncludeRequest extends CauchoRequestWrapper {
  private static final IntMap _includeAttributeMap = new IntMap();

  private static final String REQUEST_URI
    = "javax.servlet.include.request_uri";
  private static final String CONTEXT_PATH
    = "javax.servlet.include.context_path";
  private static final String SERVLET_PATH
    = "javax.servlet.include.servlet_path";
  private static final String PATH_INFO
    = "javax.servlet.include.path_info";
  private static final String QUERY_STRING
    = "javax.servlet.include.query_string";

  private static Enumeration<String> _emptyEnum;

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;

  // the wrapped request
  private Invocation _invocation;

  private IncludeResponse _response;

  private HashMapImpl<String,String[]> _filledForm;
  private ArrayList<String> _headerNames;
  
  public IncludeRequest()
  {
    _response = new IncludeResponse(this);
  }
  
  public IncludeRequest(HttpServletRequest request,
                        HttpServletResponse response,
                        Invocation invocation)
  {
    super(request);

    _response = new IncludeResponse(this, response);
    setResponse(_response);

    _invocation = invocation;
  }

  public IncludeResponse getResponse()
  {
    return _response;
  }

  public ServletContext getServletContext()
  {
    return _invocation.getWebApp();
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.INCLUDE;
  }

  //
  // CauchoRequest
  //
  
  public String getPageURI()
  {
    return _invocation.getURI();
  }

  @Override
  public String getContextPath()
  {
    return _invocation.getContextPath();
  }

  public String getPageContextPath()
  {
    return _invocation.getContextPath();
  }
  
  public String getPageServletPath()
  {
    return _invocation.getServletPath();
  }
  
  public String getPagePathInfo()
  {
    return _invocation.getPathInfo();
  }
  
  public String getPageQueryString()
  {
    return _invocation.getQueryString();
  }

  public String getMethod()
  {
    String method = getRequest().getMethod();

    // server/10jk
    if ("POST".equalsIgnoreCase(method))
      return method;
    else
      return "GET";
  }
  
  public WebApp getWebApp()
  {
    return _invocation.getWebApp();
  }

  @Override
  public boolean isSyntheticCacheHeader()
  {
    // server/137b
    
    return true;
  }
  
  @Override
  public void setHeader(String name, String value)
  {
    // server/13r4
  }

  @Override
  public String getHeader(String name) {
    if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name))
      return null;

    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name))
      return _emptyEnum;

    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    // jsp/17eh jsp/17ek
    if (_headerNames == null) {
      _headerNames = new ArrayList<String>();

      Enumeration<String> names = super.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = (String) names.nextElement();
        if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name)) {
        } else {
          _headerNames.add(name);
        }
      }
    }

    return Collections.enumeration(_headerNames);
  }

  /*
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  */

  //
  // parameters
  //


  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  public Map<String,String[]> getParameterMap()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return (String []) _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  public String getParameter(String name)
  {
    String []values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  private HashMapImpl<String,String[]> parseQuery()
  {
    String javaEncoding = Encoding.getJavaName(getCharacterEncoding());

    HashMapImpl<String,String[]> form = new HashMapImpl<String,String[]>();

    form.putAll(getRequest().getParameterMap());
    
    Form formParser = Form.allocate();

    try {
      String queryString = _invocation.getQueryString();
      
      if (queryString != null) {
        formParser.parseQueryString(form, queryString, javaEncoding, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return form;
  }
  

  //
  // attributes
  //

  public Object getAttribute(String name)
  {
    switch (_includeAttributeMap.get(name)) {
    case REQUEST_URI_CODE:
      return _invocation.getURI();
      
    case CONTEXT_PATH_CODE:
      return _invocation.getContextPath();
      
    case SERVLET_PATH_CODE:
      return _invocation.getServletPath();
      
    case PATH_INFO_CODE:
      return _invocation.getPathInfo();
      
    case QUERY_STRING_CODE:
      return _invocation.getQueryString();
      
    default:
      return super.getAttribute(name);
    }
  }

  @Override
  public Enumeration<String> getAttributeNames()
  {
    ArrayList<String> list = new ArrayList<String>();

    Enumeration<String> e = super.getAttributeNames();
    
    while (e.hasMoreElements()) {
      list.add(e.nextElement());
    }

    if (! list.contains(REQUEST_URI)) {
      list.add(REQUEST_URI);
      list.add(CONTEXT_PATH);
      list.add(SERVLET_PATH);
      list.add(PATH_INFO);
      list.add(QUERY_STRING);
    }

    return Collections.enumeration(list);
  }

  //
  // lifecycle
  //

  /**
   * Starts the request
   */
  void startRequest()
  {
    _response.startRequest();
  }

  @Override
  protected void finishRequest()
    throws IOException
  {
    super.finishRequest();
    
    _response.finishRequest();
  }

  static {
    _includeAttributeMap.put(REQUEST_URI, REQUEST_URI_CODE);
    _includeAttributeMap.put(CONTEXT_PATH, CONTEXT_PATH_CODE);
    _includeAttributeMap.put(SERVLET_PATH, SERVLET_PATH_CODE);
    _includeAttributeMap.put(PATH_INFO, PATH_INFO_CODE);
    _includeAttributeMap.put(QUERY_STRING, QUERY_STRING_CODE);

    _emptyEnum = new Enumeration<String>() {
      public boolean hasMoreElements() {
        return false;
      }

      public String nextElement() {
        throw new NoSuchElementException();
      }
    };
  }
}
