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
 * @author Sam
 */

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.TraversableDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.function.AbstractFunction;

import com.caucho.util.L10N;

import java.util.Iterator;
import java.util.Map;

/**
 * A delegate that intercepts requests for iterator's and calls methods on
 * target objects that implement
 * the {@link com.caucho.quercus.lib.spl.Iterator} interface.
 */
public class IteratorDelegate implements TraversableDelegate
{
  private static final L10N L = new L10N(IteratorDelegate.class);
  
  public Iterator<Map.Entry<Value, Value>>
    getIterator(Env env, ObjectValue qThis)
  {
    return new EntryIterator(env, qThis);
  }

  public Iterator<Value> getKeyIterator(Env env, ObjectValue qThis)
  {
    // doesn't belong here
    // php/4ar3
    //env.error(L.l("An iterator cannot be used with foreach by reference"));
    
    return new KeyIterator(env, qThis);
  }

  public Iterator<Value> getValueIterator(Env env, ObjectValue qThis)
  {
    return new ValueIterator(env, (ObjectValue) qThis);
  }

  abstract public static class AbstractIteratorImpl<T>
    implements Iterator<T>
  {
    protected final Env _env;
    protected final ObjectValue _obj;
    protected final QuercusClass _qClass;

    private final AbstractFunction _nextFun;
    private final AbstractFunction _currentFun;
    private final AbstractFunction _keyFun;
    private final AbstractFunction _rewindFun;
    private final AbstractFunction _validFun;
    private boolean _needNext;

    public AbstractIteratorImpl(Env env, ObjectValue obj)
    {
      _env = env;
      _qClass = obj.getQuercusClass();
      _obj = obj;

      if (! obj.isA("iterator"))
        throw new IllegalStateException(L.l("'{0}' is an invalid iterator",
                                            obj));
      _currentFun = _qClass.getFunction(env.createString("current"));
      _keyFun = _qClass.getFunction(env.createString("key"));
      _nextFun = _qClass.getFunction(env.createString("next"));
      _rewindFun = _qClass.getFunction(env.createString("rewind"));
      _validFun = _qClass.getFunction(env.createString("valid"));

      _rewindFun.callMethod(_env, _qClass, _obj);
      
      _needNext = false;
    }

    public boolean hasNext()
    {
      if (_needNext)
        _nextFun.callMethod(_env, _qClass, _obj);

      _needNext = true;

      return _validFun.callMethod(_env, _qClass, _obj).toBoolean();
    }

    public T next()
    {
      return getCurrent();
    }

    abstract protected T getCurrent();

    protected Value getCurrentKey()
    {
      return _keyFun.callMethod(_env, _qClass, _obj);
    }

    protected Value getCurrentValue()
    {
      return _currentFun.callMethod(_env, _qClass, _obj);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class EntryIterator<T>
    extends AbstractIteratorImpl<Map.Entry<Value, Value>>
  {
    public EntryIterator(Env env, ObjectValue obj)
    {
      super(env, obj);
    }

    @Override
    protected Map.Entry<Value, Value> getCurrent()
    {
      // php/4ar2
      Value value = getCurrentValue();
      Value key = getCurrentKey();

      return new EntryImpl(key, value);
    }
  }

  public static class EntryImpl
    implements Map.Entry<Value, Value>
  {
    private final Value _key;
    private final Value _value;

    public EntryImpl(Value key, Value value)
    {
      _key = key;
      _value = value;
    }

    public Value getKey()
    {
      return _key;
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

  public static class KeyIterator<T>
    extends AbstractIteratorImpl<Value>
  {
    public KeyIterator(Env env, ObjectValue obj)
    {
      super(env, obj);
    }

    @Override
    protected Value getCurrent()
    {
      return getCurrentKey();
    }
  }

  public static class ValueIterator<T>
    extends AbstractIteratorImpl<Value>
  {
    public ValueIterator(Env env, ObjectValue obj)
    {
      super(env, obj);
    }

    @Override
    protected Value getCurrent()
    {
      return getCurrentValue();
    }
  }
}
