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
import java.util.concurrent.*;
import com.caucho.util.Utf8;

public class JsonSerializerFactory {
  private static final HashMap<Class,JsonSerializer> _staticSerMap
    = new HashMap<Class,JsonSerializer>();

  private final ConcurrentHashMap<Class,JsonSerializer> _serMap
    = new ConcurrentHashMap<Class,JsonSerializer>();

  private static final HashMap<Class,JsonDeserializer> _staticDeserMap
    = new HashMap<Class,JsonDeserializer>();

  private final ConcurrentHashMap<Class,JsonDeserializer> _deserMap
    = new ConcurrentHashMap<Class,JsonDeserializer>();

  //
  // serializers
  //

  public JsonSerializer getSerializer(Class cl)
  {
    JsonSerializer ser = _serMap.get(cl);

    if (ser == null) {
      ser = createSerializer(cl);

      _serMap.putIfAbsent(cl, ser);
    }

    return ser;
  }

  protected JsonSerializer createSerializer(Class cl)
  {
    JsonSerializer ser = _staticSerMap.get(cl);

    if (ser != null)
      return ser;

    if (Collection.class.isAssignableFrom(cl))
      return CollectionSerializer.SER;

    if (Map.class.isAssignableFrom(cl))
      return MapSerializer.SER;

    if (cl.isArray())
      return ObjectArraySerializer.SER;

    return new JavaSerializer(cl);
  }

  //
  // deserializers
  //

  public JsonDeserializer getDeserializer(Class cl)
  {
    JsonDeserializer deser = _deserMap.get(cl);

    if (deser == null) {
      deser = createDeserializer(cl);

      _deserMap.putIfAbsent(cl, deser);
    }

    return deser;
  }

  protected JsonDeserializer createDeserializer(Class cl)
  {
    JsonDeserializer deser = _staticDeserMap.get(cl);

    if (deser != null)
      return deser;

    return new JavaDeserializer(cl);
  }

  static {
    _staticSerMap.put(boolean.class, BooleanSerializer.SER);
    _staticSerMap.put(Boolean.class, BooleanSerializer.SER);

    _staticSerMap.put(char.class, CharSerializer.SER);
    _staticSerMap.put(Character.class, CharSerializer.SER);

    _staticSerMap.put(byte.class, LongSerializer.SER);
    _staticSerMap.put(Byte.class, LongSerializer.SER);

    _staticSerMap.put(short.class, LongSerializer.SER);
    _staticSerMap.put(Short.class, LongSerializer.SER);

    _staticSerMap.put(int.class, LongSerializer.SER);
    _staticSerMap.put(Integer.class, LongSerializer.SER);

    _staticSerMap.put(long.class, LongSerializer.SER);
    _staticSerMap.put(Long.class, LongSerializer.SER);

    _staticSerMap.put(float.class, DoubleSerializer.SER);
    _staticSerMap.put(Float.class, DoubleSerializer.SER);

    _staticSerMap.put(double.class, DoubleSerializer.SER);
    _staticSerMap.put(Double.class, DoubleSerializer.SER);

    _staticSerMap.put(String.class, StringSerializer.SER);

    _staticSerMap.put(boolean[].class, BooleanArraySerializer.SER);
    _staticSerMap.put(byte[].class, ByteArraySerializer.SER);
    _staticSerMap.put(char[].class, CharArraySerializer.SER);
    _staticSerMap.put(short[].class, ShortArraySerializer.SER);
    _staticSerMap.put(int[].class, IntArraySerializer.SER);
    _staticSerMap.put(long[].class, LongArraySerializer.SER);
    _staticSerMap.put(float[].class, FloatArraySerializer.SER);
    _staticSerMap.put(double[].class, DoubleArraySerializer.SER);

    _staticDeserMap.put(String.class, StringDeserializer.DESER);
  }
}
