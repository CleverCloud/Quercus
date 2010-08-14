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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.TraversableDelegate;
import com.caucho.quercus.env.Value;

import java.util.Iterator;
import java.util.Map;

public class DOMNodeListDelegate
  implements TraversableDelegate
{
  public DOMNodeListDelegate()
  {
  }

  public Iterator<Value> getKeyIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListKeyIterator((DOMNodeList) obj.toJavaObject());
  }
  
  public Iterator<Value> getValueIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListValueIterator(env, (DOMNodeList) obj.toJavaObject());
  }
  
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env, ObjectValue obj)
  {
    return new DOMNodeListIterator(env, (DOMNodeList) obj.toJavaObject());
  }
  
  public class DOMNodeListKeyIterator
    implements Iterator<Value>
  {
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListKeyIterator(DOMNodeList list)
    {
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Value next()
    {
      return LongValue.create(_index++);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListValueIterator
    implements Iterator<Value>
  {
    private Env _env;
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListValueIterator(Env env, DOMNodeList list)
    {
      _env = env;
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Value next()
    {
      return _env.wrapJava(_list.item(_index++));
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListIterator
    implements Iterator<Map.Entry<Value, Value>>
  {
    private Env _env;
    private DOMNodeList _list;
    private int _index;

    public DOMNodeListIterator(Env env, DOMNodeList list)
    {
      _env = env;
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Map.Entry<Value, Value> next()
    {
      return new DOMNodeListEntry(_index, _env.wrapJava(_list.item(_index++)));
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNodeListEntry
    implements Map.Entry<Value,Value>
  {
    private int _key;
    private Value _value;
    
    public DOMNodeListEntry(int index, Value value)
    {
      _key = index;
      _value = value;
    }
    
    public Value getKey()
    {
      return LongValue.create(_key);
    }
    
    public Value getValue()
    {
      return _value;
    }
    
    public Value setValue(Value value)
    {
      throw new UnsupportedOperationException();
    }
  }
}
