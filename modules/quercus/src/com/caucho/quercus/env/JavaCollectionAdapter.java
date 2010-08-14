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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.ArrayValue.Entry;
import com.caucho.quercus.program.JavaClassDef;

import java.util.*;

/**
 * Represents a marshalled Collection argument.
 */
public class JavaCollectionAdapter extends JavaAdapter
{
  private Collection<Object> _collection;

  public JavaCollectionAdapter(Collection<Object> coll, JavaClassDef def)
  {
    super(coll, def);
    
    _collection = coll;
  }

  /**
   * Clears the array
   */
  @Override
  public void clear()
  {
    _collection.clear();
  }

  //
  // Conversions
  //

  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return new JavaCollectionAdapter(_collection, getClassDef());
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return new JavaCollectionAdapter(_collection, getClassDef());
  }

  /**
   * Returns the size.
   */
  @Override
  public int getSize()
  {
    return _collection.size();
  }

  /**
   * Creatse a tail index.
   */
  @Override
  public Value createTailKey()
  {
    return LongValue.create(getSize());
  }

  @Override
  public Value putImpl(Value key, Value value)
  {
    if (key.toInt() != getSize())
      throw new UnsupportedOperationException(
        "random assignment into Collection");

    _collection.add(value.toJavaObject());
    
    return value;
  }

  /**
   * Gets a new value.
   */
  @Override
  public Value get(Value key)
  {
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _collection) {
      if (pos-- > 0)
        continue;
      
      return wrapJava(obj);
    }
    
    return UnsetValue.UNSET;
  }
  
  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  { 
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _collection) {
      if (pos-- > 0)
        continue;
      
      Value val = wrapJava(obj);
      
      _collection.remove(obj);
      return val;
    }

    return UnsetValue.UNSET;
  }
  
  /**
   * Returns a set of all the of the entries.
   */
  @Override
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new CollectionValueSet();
  }

  /**
   * Returns a collection of the values.
   */
  @Override
  public Set<Map.Entry<Object,Object>> objectEntrySet()
  {
    return new CollectionSet();
  }
  
  /**
   * Returns a collection of the values.
   */
  @Override
  public Collection<Value> values()
  {
    return new ValueCollection();
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return new CollectionValueIterator();
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return new KeyIterator();
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return new ValueIterator();
  }

  public class CollectionSet
    extends AbstractSet<Map.Entry<Object,Object>>
  {
    CollectionSet()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Map.Entry<Object,Object>> iterator()
    {
      return new CollectionIterator();
    }
  }
  
  public class CollectionIterator
    implements Iterator<Map.Entry<Object,Object>>
  {
    private int _index;
    private Iterator _iterator;

    public CollectionIterator()
    {
      _index = 0;
      _iterator = _collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Object, Object> next()
    {
      return new CollectionEntry(_index++, _iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class CollectionEntry
    implements Map.Entry<Object,Object>
  {
    private final int _key;
    private Object _value;

    public CollectionEntry(int key, Object value)
    {
      _key = key;
      _value = value;
    }

    public Object getKey()
    {
      return _key;
    }

    public Object getValue()
    {
      return _value;
    }

    public Object setValue(Object value)
    {
      Object oldValue = _value;

      _value = value;

      return oldValue;
    }
  }

  public class CollectionValueSet
    extends AbstractSet<Map.Entry<Value,Value>>
  {
    CollectionValueSet()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new CollectionValueIterator();
    }
  }

  public class CollectionValueIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private int _index;
    private Iterator _iterator;

    public CollectionValueIterator()
    {
      _index = 0;
      _iterator = _collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
       Value val = wrapJava(_iterator.next());

       return new ArrayValue.Entry(LongValue.create(_index++), val);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class ValueCollection
    extends AbstractCollection<Value>
  {
    ValueCollection()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Value> iterator()
    {
      return new ValueIterator();
    }
  }

  public class KeyIterator
    implements Iterator<Value>
  {
    private int _index;
    private Iterator _iterator;

    public KeyIterator()
    {
      _index = 0;
      _iterator = _collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Value next()
    {
      _iterator.next();

      return LongValue.create(_index++);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public class ValueIterator
    implements Iterator<Value>
  {
    private Iterator _iterator;

    public ValueIterator()
    {
      _iterator = _collection.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Value next()
    {
      return wrapJava(_iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

}
