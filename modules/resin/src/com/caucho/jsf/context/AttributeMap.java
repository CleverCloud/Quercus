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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

abstract class AttributeMap extends AbstractMap<String,Object>
{
  public abstract Enumeration getNames();

  abstract public Object get(String key);

  abstract public Object put(String key, Object value);
  
  abstract public Object remove(String key);
  
  public Set<Map.Entry<String,Object>> entrySet()
  {
    return new EntrySet();
  }

  class EntrySet extends AbstractSet<Map.Entry<String,Object>> {
    public int size()
    {
      int count = 0;

      Enumeration e = getNames();
      while (e.hasMoreElements()) {
        count++;

        e.nextElement();
      }

      return count;
    }
    
    public Iterator<Map.Entry<String,Object>> iterator()
    {
      return new EntryIterator();
    }
  }

  class EntryIterator implements Iterator<Map.Entry<String,Object>> {
    private Enumeration _e;
    private String _lastName;

    EntryIterator()
    {
      _e = getNames();
    }

    public boolean hasNext()
    {
      return _e.hasMoreElements();
    }

    public Map.Entry<String,Object> next()
    {
      _lastName = (String) _e.nextElement();
      return new AttrEntry(_lastName);
    }

    public void remove()
    {
      AttributeMap.this.remove(_lastName);
    }
  }

  class AttrEntry implements Map.Entry<String,Object> {
    String _key;
      
    AttrEntry(String key)
    {
      _key = key;
    }
      
    public String getKey()
    {
      return _key;
    }

    public Object getValue()
    {
      return get(_key);
    }
    
    public Object setValue(Object value)
    {
      return put(_key, value);
    }

    public int hashCode()
    {
      return _key.hashCode();
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof AttrEntry))
        return false;

      AttrEntry entry = (AttrEntry) o;

      return _key.equals(entry._key);
    }
  }
}
