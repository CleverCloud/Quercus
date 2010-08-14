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

package com.caucho.jsp.el;

import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.util.*;

import javax.el.ELContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Parses the expression.
 */
abstract public class ServletELContext extends ELContext
{
  abstract public ServletContext getApplication();
  
  abstract public Object getApplicationScope();
  
  public Object getCookie()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    Cookie []cookies = getRequest().getCookies();

    for (int i = 0; cookies != null && i < cookies.length; i++) {
      if (map.get(cookies[i].getName()) == null)
        map.put(cookies[i].getName(), cookies[i]);
    }
      
    return map;
  }
  
  public Object getHeader()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HashMap<String,String> map
      = new CaseInsensitiveHashMap<String>();
    Enumeration e = request.getHeaderNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      String value = request.getHeader(name);

      map.put(name, value);
    }
      
    return map;
  }
  
  public Object getHeader(String field)
  {
    HttpServletRequest request = getRequest();

    if (request != null)
      return request.getHeader(field);
    else
      return null;
  }
  
  public Object getHeaderValues()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HashMap<String,String[]> map
      = new CaseInsensitiveHashMap<String[]>();
    Enumeration e = request.getHeaderNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Enumeration values = request.getHeaders(name);
      
      ArrayList<String> list = new ArrayList<String>();

      while (values.hasMoreElements())
        list.add((String) values.nextElement());

      map.put(name, list.toArray(new String[list.size()]));
    }
      
    return map;
  }
  
  public Object getHeaderValues(String field)
  {
    HttpServletRequest request = getRequest();

    if (request != null) {
      Enumeration values = request.getHeaders(field);
      
      ArrayList<String> list = new ArrayList<String>();

      while (values != null && values.hasMoreElements())
        list.add((String) values.nextElement());

      return list.toArray(new String[list.size()]);
    }
    else
      return null;
  }
  
  public Object getParameter()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HashMap<String,String> map
      = new CaseInsensitiveHashMap<String>();
    Enumeration e = request.getParameterNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      String value = request.getParameter(name);

      map.put(name, value);
    }
      
    return map;
  }
  
  public Object getParameter(String field)
  {
    HttpServletRequest request = getRequest();

    if (request != null)
      return request.getParameter(field);
    else
      return null;
  }
  
  public Object getParameterValues()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HashMap<String,String[]> map
      = new CaseInsensitiveHashMap<String[]>();
    Enumeration e = request.getParameterNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      String []values = request.getParameterValues(name);

      map.put(name, values);
    }
      
    return map;
  }
  
  public Object getParameterValues(String field)
  {
    HttpServletRequest request = getRequest();

    if (request != null) {
      String []values = request.getParameterValues(field);

      return values;
    }
    else
      return null;
  }
  
  abstract public HttpServletRequest getRequest();
  
  abstract public Object getRequestScope();
  
  public Object getSessionScope()
  {
    HttpServletRequest request = getRequest();

    if (request == null)
      return null;

    HttpSession session = request.getSession(false);

    if (session == null)
      return null;

    HashMap<String,Object> map
      = new HashMap<String,Object>();
    
    Enumeration e = session.getAttributeNames();

    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Object value = request.getAttribute(name);

      map.put(name, value);
    }
      
    return map;
  }
}
