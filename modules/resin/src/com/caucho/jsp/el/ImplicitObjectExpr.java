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

package com.caucho.jsp.el;

import com.caucho.el.Expr;
import com.caucho.jsp.PageContextImpl;
import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.*;

public class ImplicitObjectExpr extends Expr {
  final static int PAGE_CONTEXT = 1;
  
  final static int APPLICATION_SCOPE = PAGE_CONTEXT + 1;
  final static int SESSION_SCOPE = APPLICATION_SCOPE + 1;
  final static int REQUEST_SCOPE = SESSION_SCOPE + 1;
  final static int PAGE_SCOPE = REQUEST_SCOPE + 1;
  
  final static int PARAM = PAGE_SCOPE + 1;
  final static int PARAM_VALUES = PARAM + 1;
  
  final static int INIT_PARAM = PARAM_VALUES + 1;
  
  final static int HEADER = INIT_PARAM + 1;
  final static int HEADER_VALUES = HEADER + 1;
  
  final static int COOKIE = HEADER_VALUES + 1;

  private String _id;
  private int _index;

  public ImplicitObjectExpr(String id)
  {
    _id = id;

    if ("pageContext".equals(id))
      _index = PAGE_CONTEXT;
    else if ("applicationScope".equals(id))
      _index = APPLICATION_SCOPE;
    else if ("sessionScope".equals(id))
      _index = SESSION_SCOPE;
    else if ("requestScope".equals(id))
      _index = REQUEST_SCOPE;
    else if ("pageScope".equals(id))
      _index = PAGE_SCOPE;
    else if ("param".equals(id))
      _index = PARAM;
    else if ("paramValues".equals(id))
      _index = PARAM_VALUES;
    else if ("initParam".equals(id))
      _index = INIT_PARAM;
    else if ("header".equals(id))
      _index = HEADER;
    else if ("headerValues".equals(id))
      _index = HEADER_VALUES;
    else if ("cookie".equals(id))
      _index = COOKIE;
    else
      throw new IllegalArgumentException();
  }

  public Expr createField(Expr field)
  {
    switch (_index) {
    case APPLICATION_SCOPE:
    case SESSION_SCOPE:
    case REQUEST_SCOPE:
    case PAGE_SCOPE:
    case PARAM:
    case PARAM_VALUES:
    case HEADER:
    case HEADER_VALUES:
    case COOKIE:
    case INIT_PARAM:
      return new ImplicitFieldExpr(_index, field);
      
    default:
      return super.createField(field);
    }
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the page context
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    if (! (env instanceof PageContextImpl.PageELContext))
      return null;
    
    PageContextImpl page
      = ((PageContextImpl.PageELContext) env).getPageContext();
    
    switch (_index) {
    case PAGE_CONTEXT:
      return page;

    case APPLICATION_SCOPE:
      return new AttributeMap(page, PageContext.APPLICATION_SCOPE);

    case SESSION_SCOPE:
      return new AttributeMap(page, PageContext.SESSION_SCOPE);

    case REQUEST_SCOPE:
      return new AttributeMap(page, PageContext.REQUEST_SCOPE);

    case PAGE_SCOPE:
      return new AttributeMap(page, PageContext.PAGE_SCOPE);

    case PARAM_VALUES:
      return page.getRequest().getParameterMap();

    case PARAM: {
      HashMap<String,String> map = new HashMap<String,String>();
      Map pMap = page.getRequest().getParameterMap();
      Iterator iter = pMap.entrySet().iterator();
      
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        String key = (String) entry.getKey();
        String []value = (String []) entry.getValue();
        map.put(key, value[0]);
      }
      
      return map;
    }

    case INIT_PARAM:
    {
      ServletContext app = page.getServletContext();
      HashMap<String,String> map = new HashMap<String,String>();
      Enumeration e = app.getInitParameterNames();

      while (e.hasMoreElements()) {
        String name = (String) e.nextElement();

        map.put(name, app.getInitParameter(name));
      }
      
      return map;
    }

    case HEADER:
    {
      HttpServletRequest req = (HttpServletRequest) page.getRequest();
      HashMap<String,String> map = new HashMap<String,String>();
      Enumeration e = req.getHeaderNames();

      while (e.hasMoreElements()) {
        String name = (String) e.nextElement();

        map.put(name, req.getHeader(name));
      }
      
      return map;
    }

    case HEADER_VALUES:
    {
      HttpServletRequest req = (HttpServletRequest) page.getRequest();
      HashMap<String,String[]> map = new HashMap<String,String[]>();
      Enumeration e = req.getHeaderNames();

      while (e.hasMoreElements()) {
        String name = (String) e.nextElement();
        Enumeration values = req.getHeaders(name);
      
        ArrayList<String> list = new ArrayList<String>();

        while (values.hasMoreElements())
          list.add((String) values.nextElement());

        map.put(name, list.toArray(new String[list.size()]));
      }
      
      return map;
    }

    case COOKIE:
    {
      HashMap<String,Object> map = new HashMap<String,Object>();
      Cookie []cookies = ((HttpServletRequest) page.getRequest()).getCookies();

      for (int i = 0; cookies != null && i < cookies.length; i++) {
        if (map.get(cookies[i].getName()) == null)
          map.put(cookies[i].getName(), cookies[i]);
      }
      
      return map;
    }
    }

    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return _id;
  }

  /**
   * Prints the code to create an IdExpr.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.jsp.el.ImplicitObjectExpr(\"");
    printEscapedString(os, _id);
    os.print("\")");
  }

  public static class AttributeMap extends AbstractMap {
    private PageContext _pageContext;
    private int _scope;
    
    AttributeMap(PageContext pageContext, int scope)
    {
      _pageContext = pageContext;
      _scope = scope;
    }

    public Object get(Object key)
    {
      return _pageContext.getAttribute((String) key, _scope);
    }

    public Object put(Object key, Object value)
    {
      _pageContext.setAttribute((String) key, value, _scope);

      return null;
    }

    private EntrySet _entrySet;
    
    public Set entrySet()
    {
      if (_entrySet == null)
        _entrySet = new EntrySet();
      
      return _entrySet;
    }

    public class EntrySet extends AbstractSet {
      public int size()
      {
        Enumeration e = _pageContext.getAttributeNamesInScope(_scope);
        int i = 0;
        while (e.hasMoreElements()) {
          e.nextElement();
          i++;
        }

        return i;
      }
      
      public Iterator iterator()
      {
        return new EntryIterator();
      }
    }

    public class EntryIterator implements Iterator, Map.Entry {
      Enumeration _e;
      String _name;
      Object _value;

      EntryIterator()
      {
        _e = _pageContext.getAttributeNamesInScope(_scope);
      }

      public boolean hasNext()
      {
        return _e.hasMoreElements();
      }

      public Object next()
      {
        _name = (String) _e.nextElement();
        _value = _pageContext.getAttribute(_name, _scope);

        return this;
      }

      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      public Object getKey()
      {
        return _name;
      }

      public Object getValue()
      {
        return _value;
      }

      public Object setValue(Object value)
      {
        _pageContext.setAttribute(_name, value, _scope);
        
        Object oldValue = _value;
        _value = value;
        
        return oldValue;
      }

      public int hashCode()
      {
        return _name.hashCode();
      }

      public boolean equals(Object obj)
      {
        if (! (obj instanceof EntryIterator))
          return false;
        
        EntryIterator entry = (EntryIterator) obj;

        return (_name.equals(entry._name) &&
                (_value == null && entry._value == null ||
                 _value != null && _value.equals(entry._value)));
      }
    }
  }
}
