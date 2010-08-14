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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents a Quercus java value.
 */
public class JavaValue extends ObjectValue
  implements Serializable
{
  private static final Logger log
    = Logger.getLogger(JavaValue.class.getName());

  private JavaClassDef _classDef;
  protected Env _env;

  private Object _object;

  public JavaValue(Env env, Object object, JavaClassDef def)
  {
    super();

    setQuercusClass(env.createJavaQuercusClass(def));

    _classDef = def;
    _object = object;
  }

  public JavaValue(Object object, JavaClassDef def, QuercusClass qClass)
  {
    super();

    setQuercusClass(qClass);

    _classDef = def;
    _object = object;
  }

  /*
   * Returns the underlying Java class definition.
   */
  protected JavaClassDef getJavaClassDef()
  {
    return _classDef;
  }

  @Override
  public String getClassName()
  {
    return _classDef.getName();
  }

  /**
   * Converts to a double.
   */
  public long toLong()
  {
    return StringValue.parseLong(toString(Env.getInstance()));
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return toDouble(toString(Env.getInstance()).toString());
  }

  /**
   * Converts to a double.
   */
  public static double toDouble(String s)
  {
    int len = s.length();
    int i = 0;
    int ch = 0;

    if (i < len && ((ch = s.charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = s.charAt(i)) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i != len)
      return 1;
    else
      return Double.parseDouble(s);
  }

  @Override
  public StringValue toString(Env env)
  {
    StringValue value = _classDef.toString(env, this);

    if (value == null)
      value = env.createString(toString());

    return value;
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (_classDef.printRImpl(env, _object, out, depth, valueSet))
      return;

    Set<? extends Map.Entry<Value,Value>> entrySet = entrySet();

    if (entrySet == null) {
      out.print("resource(" + toString(env) + ")"); // XXX:
      return;
    }

    out.print(_classDef.getSimpleName());
    out.println(" Object");
    printRDepth(out, depth);
    out.print("(");

    for (Map.Entry<Value,Value> entry : entrySet) {
      out.println();
      printRDepth(out, depth);
      out.print("    [" + entry.getKey() + "] => ");

      entry.getValue().printRImpl(env, out, depth + 1, valueSet);
    }

    out.println();
    printRDepth(out, depth);
    out.println(")");
  }

  @Override
  protected void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    Value oldThis = env.setThis(this);

    try {
      if (! _classDef.varDumpImpl(env, this, _object, out, depth, valueSet))
        out.print("resource(" + toString(env) + ")"); // XXX:
    }
    finally {
      env.setThis(oldThis);
    }
  }

  //
  // field routines
  //

  /**
   * Returns the field value.
   */
  @Override
  public Value getField(Env env, StringValue name)
  {
    Value value = _classDef.getField(env, this, name);

    if (value != null)
      return value;
    else
      return UnsetValue.NULL;
  }

  /**
   * Sets the field value.
   */
  @Override
  public Value putField(Env env, StringValue name, Value value)
  {
    Value oldValue = _classDef.putField(env, this, name, value);

    if (oldValue != null)
      return oldValue;
    else
      return NullValue.NULL;
  }

  public Set<? extends Map.Entry<Value, Value>> entrySet()
  {
    return _classDef.entrySet(_object);
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    return new LongValue(System.identityHashCode(this));
  }

  @Override
  public int cmpObject(ObjectValue rValue)
  {
    // php/172z

    if (rValue == this)
      return 0;

    if (!(rValue instanceof JavaValue))
      return -1;

    Object rObject = rValue.toJavaObject();

    return _classDef.cmpObject(_object,
                               rObject,
                               ((JavaValue) rValue)._classDef);
  }

  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return true;
  }

  /*
   * Returns true for a resource.
   */
  @Override
  public boolean isResource()
  {
    return false;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "object";
  }

  /**
   * Returns the method.
   */
  /*
  @Override
  public AbstractFunction findFunction(String methodName)
  {
    return _classDef.findFunction(methodName);
  }
  */

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value []args)
  {
    return _classDef.callMethod(env, this, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash)
  {
    return _classDef.callMethod(env, this, methodName, hash);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1, Value a2)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, this, methodName, hash,
                                a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, this, methodName, hash,
                                a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value []args)
  {
    return _classDef.callMethod(env, this, methodName, hash, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash)
  {
    Value value = _classDef.callMethod(env, this, methodName, hash);

    return value;
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, this, methodName, hash, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, this, methodName, hash,
                                a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, this, methodName, hash,
                                a1, a2, a3, a4, a5);
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb, SerializeMap map)
  {
    String name = _classDef.getSimpleName();

    Set<? extends Map.Entry<Value,Value>> entrySet = entrySet();

    if (entrySet != null) {
      sb.append("O:");
      sb.append(name.length());
      sb.append(":\"");
      sb.append(name);
      sb.append("\":");
      sb.append(entrySet.size());
      sb.append(":{");

      for (Map.Entry<Value,Value> entry : entrySet) {
        entry.getKey().serialize(env, sb);
        entry.getValue().serialize(env, sb, map);
      }

      sb.append("}");
    }
    else {
      // php/121f
      sb.append("i:0;");
    }
  }

  /**
   * Encodes the value in JSON.
   */
  public void jsonEncode(Env env, StringValue sb)
  {
    if (_classDef.jsonEncode(env, _object, sb))
      return;
    else
      super.jsonEncode(env, sb);
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    //return toString(Env.getInstance()).toString();

    return String.valueOf(_object);
  }


  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return _object;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public final Object toJavaObject(Env env, Class type)
  {
    final Object object = _object;
    final Class objectClass = _object.getClass();

    if (type == objectClass || type.isAssignableFrom(objectClass)) {
      return object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
                      objectClass.getName(), type.getName()));

      return null;
    }
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    Class objClass = _object.getClass();

    if (objClass == type || type.isAssignableFrom(objClass)) {
      return _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
                      objClass.getName(), type.getName()));

      return null;
    }
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    if (type.isAssignableFrom(_object.getClass())) {
      return (Map) _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
                      _object.getClass().getName(), type.getName()));

      return null;
    }
  }

  /**
   * Converts to an object.
   */
  @Override
  public InputStream toInputStream()
  {
    if (_object instanceof InputStream)
      return (InputStream) _object;
    else if (_object instanceof File) {
      try {
        InputStream is = new FileInputStream((File) _object);

        Env.getCurrent().addCleanup(new EnvCloseable(is));

        return is;
      } catch (IOException e) {
        throw new QuercusException(e);
      }
    }
    else
      return super.toInputStream();
  }

  private static void printRDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < 8 * depth; i++)
      out.print(' ');
  }

  //
  // Java Serialization
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(_classDef.getType().getCanonicalName());

    out.writeObject(_object);
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _env = Env.getInstance();

    _classDef = _env.getJavaClassDefinition((String) in.readObject());

    int id = _env.getQuercus().getClassId(_classDef.getName());

    setQuercusClass(_env.createQuercusClass(id, _classDef, null));

    _object = in.readObject();
  }

  private static class EntryItem implements Map.Entry<Value,Value> {
    private Value _key;
    private Value _value;
    private boolean _isArray;

    EntryItem(Value key, Value value)
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
      return _value;
    }

    void addValue(Value value)
    {
      ArrayValue array = null;

      if (! _isArray) {
        _isArray = true;
        Value oldValue = _value;
        _value = new ArrayValueImpl();
        array = (ArrayValue) _value;
        array.append(oldValue);
      }
      else {
        array = (ArrayValue) _value;
      }

      array.append(value);
    }
  }
}

