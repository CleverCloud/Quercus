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

package javax.servlet.jsp.el;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Variable resolution for JSP variables
 */
public class ImplicitObjectELResolver extends ELResolver {
  private static final HashMap<Object,Prop> _propMap
    = new HashMap<Object,Prop>();
  
  private static final ArrayList<FeatureDescriptor> _featureDescriptors
    = new ArrayList<FeatureDescriptor>();
  
  @Override
  public Class<String> getCommonPropertyType(ELContext context,
                                             Object base)
  {
    if (base == null)
      return String.class;
    else
      return null;
  }

  @Override
  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    if (base != null)
      return null;
    
    return _featureDescriptors.iterator();
  }

  @Override
    public Class getType(ELContext context,
                         Object base,
                         Object property)
  {
    if (base != null)
      return null;

    Prop prop = _propMap.get(property);
    if (prop == null)
      return null;

    context.setPropertyResolved(true);

    return null;
  }

  @Override
    public Object getValue(ELContext context,
                           Object base,
                           Object property)
  {
    if (base != null)
      return null;

    Prop prop = _propMap.get(property);
    if (prop == null)
      return null;

    context.setPropertyResolved(true);

    PageContext jspContext = (PageContext) context.getContext(JspContext.class);

    switch (prop) {
    case PAGE_CONTEXT:
      return jspContext;
      
    case PAGE_SCOPE:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        Enumeration e = jspContext.getAttributeNamesInScope(PageContext.PAGE_SCOPE);
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, jspContext.getAttribute(name));
        }

        return map;
      }
      
    case REQUEST_SCOPE:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        ServletRequest request = jspContext.getRequest();

        Enumeration e = request.getAttributeNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, request.getAttribute(name));
        }

        return map;
      }
      
    case SESSION_SCOPE:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        HttpSession session = jspContext.getSession();

        if (session == null)
          return null;

        Enumeration e = session.getAttributeNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, session.getAttribute(name));
        }

        return map;
      }
      
    case APPLICATION_SCOPE:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        ServletContext app = jspContext.getServletContext();

        Enumeration e = app.getAttributeNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, app.getAttribute(name));
        }

        return map;
      }
      
    case PARAM:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        ServletRequest request = jspContext.getRequest();

        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, request.getParameter(name));
        }

        return map;
      }
      
    case PARAM_VALUES:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        ServletRequest request = jspContext.getRequest();

        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, request.getParameterValues(name));
        }

        return map;
      }
      
    case HEADER:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        HttpServletRequest request = (HttpServletRequest) jspContext.getRequest();

        Enumeration e = request.getHeaderNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, request.getHeader(name));
        }

        return map;
      }
      
    case HEADER_VALUES:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        HttpServletRequest request = (HttpServletRequest) jspContext.getRequest();

        Enumeration e = request.getHeaderNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, request.getHeaders(name));
        }

        return map;
      }
      
    case COOKIE:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        HttpServletRequest request
          = (HttpServletRequest) jspContext.getRequest();

        Cookie []cookies = request.getCookies();

        if (cookies == null)
          return map;

        for (int i = cookies.length - 1; i >= 0; i--) {
          map.put(cookies[i].getName(), cookies[i].getValue());
        }

        return map;
      }
    
    case INIT_PARAM:
      {
        HashMap<String,Object> map = new HashMap<String,Object>();

        ServletContext app = jspContext.getServletContext();

        Enumeration e = app.getInitParameterNames();
        while (e.hasMoreElements()) {
          String name = (String) e.nextElement();

          map.put(name, app.getInitParameter(name));
        }

        return map;
      }
    }

    return null;
  }

  @Override
    public boolean isReadOnly(ELContext context,
                         Object base,
                         Object property)
  {
    if (base != null)
      return true;

    Prop prop = _propMap.get(property);
    if (prop == null)
      return true;

    context.setPropertyResolved(true);
    return true;
  }

  @Override
    public void setValue(ELContext context,
                         Object base,
                         Object property,
                         Object value)
  {
    if (base != null)
      return;

    Prop prop = _propMap.get(property);
    if (prop == null)
      return;

    context.setPropertyResolved(true);
    
    throw new PropertyNotWritableException(String.valueOf(value));
  }
  
  private enum Prop {
    PAGE_CONTEXT,
    PAGE_SCOPE,
    REQUEST_SCOPE,
    SESSION_SCOPE,
    APPLICATION_SCOPE,
    PARAM,
    PARAM_VALUES,
    HEADER,
    HEADER_VALUES,
    COOKIE,
    INIT_PARAM
  };

  static {
    _propMap.put("pageContext", Prop.PAGE_CONTEXT);
    _propMap.put("pageScope", Prop.PAGE_SCOPE);
    _propMap.put("requestScope", Prop.REQUEST_SCOPE);
    _propMap.put("sessionScope", Prop.SESSION_SCOPE);
    _propMap.put("applicationScope", Prop.APPLICATION_SCOPE);
    _propMap.put("param", Prop.PARAM);
    _propMap.put("paramValues", Prop.PARAM_VALUES);
    _propMap.put("header", Prop.HEADER);
    _propMap.put("headerValues", Prop.HEADER_VALUES);
    _propMap.put("cookie", Prop.COOKIE);
    _propMap.put("initParam", Prop.INIT_PARAM);

    for (Object key : _propMap.keySet()) {
      String name = String.valueOf(key);
      
      FeatureDescriptor desc = new FeatureDescriptor();
      desc.setName(name);
      desc.setDisplayName(name);
      desc.setShortDescription("");
      desc.setExpert(false);
      desc.setHidden(false);
      desc.setPreferred(true);

      desc.setValue(ELResolver.TYPE, Map.class);

      desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);

      _featureDescriptors.add(desc);
    }
  }
}
