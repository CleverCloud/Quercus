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

public class PageContextAttributeMap extends AbstractMap {
  private PageContext _pageContext;
  private int _scope;
    
  public PageContextAttributeMap(PageContext pageContext, int scope)
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

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("[");

    boolean isFirst = true;
    Iterator iter = entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      
      if (! isFirst)
        sb.append(", ");
      isFirst = false;

      sb.append("{");
      sb.append(entry.getKey());
      sb.append(", ");
      sb.append(entry.getValue());
      sb.append("}");
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}
