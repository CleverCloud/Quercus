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

import com.caucho.el.Expr;
import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.*;

public class ImplicitFieldExpr extends Expr {
  private int _index;

  private Expr _field;

  public ImplicitFieldExpr(int index, Expr field)
  {
    _index = index;
    _field = field;
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
    PageContext page = (PageContext) env.getContext(JspContext.class);

    if (page == null)
      return null;

    Object fieldValue = _field.evalObject(env);

    if (fieldValue == null)
      return null;

    String fieldString = toString(fieldValue, env);

    switch (_index) {
    case ImplicitObjectExpr.APPLICATION_SCOPE:
      return page.getServletContext().getAttribute(fieldString);

    case ImplicitObjectExpr.SESSION_SCOPE:
      {
        HttpSession session = page.getSession();

        if (session != null) {
          return session.getAttribute(fieldString);
        }
        else
          return null;
      }

    case ImplicitObjectExpr.REQUEST_SCOPE:
      {
        return page.getRequest().getAttribute(fieldString);
      }

    case ImplicitObjectExpr.PAGE_SCOPE:
      return page.getAttribute(fieldString);
      
    case ImplicitObjectExpr.PARAM:
      return page.getRequest().getParameter(fieldString);

    case ImplicitObjectExpr.PARAM_VALUES:
      return page.getRequest().getParameterValues(fieldString);
      
    case ImplicitObjectExpr.HEADER:
      return ((HttpServletRequest) page.getRequest()).getHeader(fieldString);

    case ImplicitObjectExpr.HEADER_VALUES: {
      Enumeration e =
        ((HttpServletRequest) page.getRequest()).getHeaders(fieldString);

      if (e == null)
        return null;

      if (! e.hasMoreElements())
        return null;
      
      ArrayList<String> list = new ArrayList<String>();

      while (e.hasMoreElements())
        list.add((String) e.nextElement());
      
      return list.toArray(new String[list.size()]);
    }
      
    case ImplicitObjectExpr.INIT_PARAM:
      return page.getServletContext().getInitParameter(fieldString);

    case ImplicitObjectExpr.COOKIE: {
      Cookie []cookies = ((HttpServletRequest) page.getRequest()).getCookies();
      if (cookies == null)
        return null;

      for (int i = 0; i < cookies.length; i++) {
        if (cookies[i].getName().equals(fieldString))
          return cookies[i];
      }

      return null;
    }
    }
      
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "implicit_" + _index + "[" + _field + "]";
  }
  
  /**
   * Prints the code to create an IdExpr.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.jsp.el.ImplicitFieldExpr(");
    os.print(_index);
    os.print(", ");
    _field.printCreate(os);
    os.print(")");
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
