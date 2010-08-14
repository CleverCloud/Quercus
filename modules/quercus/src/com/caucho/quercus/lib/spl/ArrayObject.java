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

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class ArrayObject
  implements ArrayAccess,
             Countable,
             IteratorAggregate,
             Traversable
{
  private static L10N L = new L10N(ArrayObject.class);

  public static final int STD_PROP_LIST = 0x00000001;
  public static final int ARRAY_AS_PROPS = 0x00000002;

  private final Env _env;
  private Value _value;
  private int _flags;
  private QuercusClass _iteratorClass;

  @Name("__construct")
  public ArrayObject(Env env,
                     @Optional Value value,
                     @Optional int flags,
                     @Optional("ArrayIterator") String iteratorClassName)
  {
    if (value.isNull())
      value = new ArrayValueImpl();

    _env = env;
    _value = value.toValue();
    _flags = flags;

    QuercusClass iteratorClass = _env.findClass(iteratorClassName);

    if (iteratorClass == null || ! iteratorClass.isA("Iterator"))
      throw new IllegalArgumentException(
          L.l("A class that implements Iterator must be specified"));

    _iteratorClass = iteratorClass;
  }

  public void append(Value value)
  {
    _value.put(value);
  }

  public void asort(@Optional long sortFlag)
  {
    sortFlag = 0; // qa/4a46

    if (_value instanceof ArrayValue)
      ArrayModule.asort(_env, (ArrayValue) _value, sortFlag);
  }

  public int count()
  {
    return _value.getCount(_env);
  }

  public Value exchangeArray(ArrayValue array)
  {
    Value oldValue = _value;

    _value = array;

    return oldValue;
  }

  public Value getArrayCopy()
  {
    return _value.copy();
  }

  public int getFlags()
  {
    return _flags;
  }

  public ObjectValue getIterator()
  {
    Value[] args = new Value[] { _value, LongValue.create(_flags) };

    return (ObjectValue) _iteratorClass.callNew(_env, args);
  }

  public String getIteratorClass()
  {
    return _iteratorClass.getName();
  }

  public void ksort(@Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      ArrayModule.ksort(_env, (ArrayValue) _value, sortFlag);
  }

  public void natcasesort()
  {
    if (_value instanceof ArrayValue)
      ArrayModule.natcasesort(_env, _value);
  }

  public void natsort()
  {
    if (_value instanceof ArrayValue)
      ArrayModule.natsort(_env, _value);
  }

  public boolean offsetExists(Value offset)
  {
    return _value.get(offset).isset();
  }

  public Value offsetGet(Value offset)
  {
    return _value.get(offset);
  }

  public Value offsetSet(Value offset, Value value)
  {
    return _value.put(offset, value);
  }

  public Value offsetUnset(Value offset)
  {
    return _value.remove(offset);
  }

  public void setFlags(Value flags)
  {
    _flags = flags.toInt();
  }

  public void setIteratorClass(String iteratorClass)
  {
    _iteratorClass = _env.findClass(iteratorClass);
  }

  public void uasort(Callable func)
  {
    if (_value instanceof ArrayValue)
      ArrayModule.uasort(_env, (ArrayValue) _value, func,  0);
  }

  public void uksort(Callable func, @Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      ArrayModule.uksort(_env, (ArrayValue) _value, func, sortFlag);
  }

  public Value __getField(StringValue key)
  {
    if ((_flags & ARRAY_AS_PROPS) != 0)
      return _value.get(key);
    else
      return UnsetValue.UNSET;
  }

  static private void printDepth(WriteStream out, int depth)
    throws java.io.IOException
  {
    for (int i = depth; i > 0; i--)
      out.print(' ');
  }

  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException
  {

    if ((_flags & STD_PROP_LIST) != 0) {
      // XXX:
      out.print("ArrayObject");
      out.print(' ');
      out.println("Object");
      printDepth(out, 4 * depth);
      out.println("(");
      out.print(")");
    }
    else {
      out.print("ArrayObject");
      out.print(' ');
      out.println("Object");
      printDepth(out, 4 * depth);
      out.println("(");

      depth++;


      java.util.Iterator<Map.Entry<Value,Value>> iterator
        = _value.getIterator(env);

      while (iterator.hasNext()) {
        Map.Entry<Value, Value> entry = iterator.next();

        Value key = entry.getKey();
        Value value = entry.getValue();

        printDepth(out, 4 * depth);

        out.print("[" + key + "] => ");

        value.printR(env, out, depth + 1, valueSet);

        out.println();
      }

      depth--;

      printDepth(out, 4 * depth);
      out.println(")");
    }
  }

  public void varDumpImpl(Env env,
                          Value object,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    String name = object.getClassName();
    
    if ((_flags & STD_PROP_LIST) != 0) {
      // XXX:
      out.println("object(" + name + ") (0) {");
      out.print("}");

    }
    else {
      out.println("object(" + name + ") (" + _value.getSize() + ") {");

      depth++;

      java.util.Iterator<Map.Entry<Value,Value>> iterator
        = _value.getIterator(env);

      while (iterator.hasNext()) {
        Map.Entry<Value, Value> entry = iterator.next();

        Value key = entry.getKey();
        Value value = entry.getValue();

        printDepth(out, 2 * depth);

        out.print("[");

        if (key instanceof StringValue)
          out.print("\"" + key + "\"");
        else
          out.print(key);

        out.println("]=>");

        printDepth(out, 2 * depth);

        value.varDump(env, out, depth, valueSet);

        out.println();
      }

      depth--;

      printDepth(out, 2 * depth);

      out.print("}");
    }
  }
}
