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

package com.caucho.json;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;

public class JavaDeserializer implements JsonDeserializer {
  private static final Logger log
    = Logger.getLogger(JavaDeserializer.class.getName());

  private Class _type;
  private Constructor _ctor;
  private HashMap<String,JsonField> _fieldMap
    = new HashMap<String,JsonField>();

  JavaDeserializer(Class type)
  {
    try {
      _type = type;
      _ctor = introspectConstructor(type);
      _ctor.setAccessible(true);

      introspect();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void introspect()
  {
    introspectConstructor(_type);
    introspectFields(_type);
  }

  private Constructor introspectConstructor(Class type)
  {
    for (Constructor ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
        return ctor;
    }

    throw new IllegalStateException(type + " needs a zero-arg constructor");
  }

  private void introspectFields(Class type)
  {
    if (type == null)
      return;

    introspectFields(type.getSuperclass());

    for (Field field : type.getDeclaredFields()) {
      if (Modifier.isTransient(field.getModifiers()))
        continue;
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      field.setAccessible(true);

      Class fieldType = field.getType();

      if (fieldType == int.class || fieldType == Integer.class)
        _fieldMap.put(field.getName(), new JsonIntField(field));
      else if (fieldType == long.class || fieldType == Long.class)
        _fieldMap.put(field.getName(), new JsonLongField(field));
      else if (fieldType == double.class || fieldType == Double.class)
        _fieldMap.put(field.getName(), new JsonDoubleField(field));
      else
        _fieldMap.put(field.getName(), new JsonObjectField(field));
    }
  }

  public Object read(JsonInput in)
    throws IOException
  {
    Object bean = create();

    in.parseBeanMap(bean, this);

    return bean;
  }

  public Object create()
  {
    try {
      return _ctor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void readField(JsonInput in, Object bean, String fieldName)
    throws IOException
  {
    JsonField jsonField = _fieldMap.get(fieldName);

    if (jsonField != null)
      jsonField.read(in, bean);
    else {
      // skip
      in.readObject();
    }
  }

  public Object complete(Object bean)
  {
    return bean;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }

  abstract static class JsonField {
    abstract void read(JsonInput in, Object bean)
      throws IOException;
  }

  static class JsonIntField extends JsonField {
    private final Field _field;

    JsonIntField(Field field)
    {
      _field = field;
    }

    void read(JsonInput in, Object bean)
      throws IOException
    {
      long value = in.readLong();

      try {
        _field.setInt(bean, (int) value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class JsonLongField extends JsonField {
    private final Field _field;

    JsonLongField(Field field)
    {
      _field = field;
    }

    void read(JsonInput in, Object bean)
      throws IOException
    {
      long value = in.readLong();

      try {
        _field.setLong(bean, value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class JsonDoubleField extends JsonField {
    private final Field _field;

    JsonDoubleField(Field field)
    {
      _field = field;
    }

    void read(JsonInput in, Object bean)
      throws IOException
    {
      double value = in.readDouble();

      try {
        _field.setDouble(bean, value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class JsonObjectField extends JsonField {
    private final Field _field;

    JsonObjectField(Field field)
    {
      _field = field;
    }

    void read(JsonInput in, Object bean)
      throws IOException
    {
      Object value = in.readObject(_field.getType());

      if (value == null)
        return;

      if (! _field.getType().isAssignableFrom(value.getClass()))
        throw new IOException(value.getClass() + " is an illegal value for " + _field);

      try {
        _field.set(bean, value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
