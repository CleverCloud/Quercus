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

package com.caucho.amber.type;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.HashMap;

/**
 * The manages the types known to the Amber instnce.
 */
public class TypeManager {
  private static final L10N L = new L10N(TypeManager.class);

  private static HashMap<String,AmberType> _builtinTypes;
  private static HashMap<String,AmberType> _primitiveTypes;

  private HashMap<String,AmberType> _typeMap;

  public TypeManager()
  {
    _typeMap = new HashMap<String,AmberType>();

    _typeMap.putAll(_builtinTypes);
  }

  /**
   * Returns the type.
   */
  public AmberType create(String name)
    throws ConfigException
  {
    AmberType type = _typeMap.get(name);

    if (type != null)
      return type;

    throw new ConfigException(L.l("'{0}' is an unknown type", name));
  }

  /**
   * Returns the type.
   */
  public AmberType create(Class cl)
    throws ConfigException
  {
    AmberType type = _primitiveTypes.get(cl.getName());

    if (type != null)
      return type;

    type = _typeMap.get(cl.getName());

    return type;
  }

  /**
   * Returns the type.
   */
  public AmberType get(String name)
  {
    return _typeMap.get(name);
  }

  /**
   * Returns the type.
   */
  public EntityType getEntityByInstanceClass(String name)
  {
    for (AmberType type : _typeMap.values()) {
      if (type instanceof EntityType) {
        EntityType entityType = (EntityType) type;

        if (name.equals(entityType.getInstanceClassName()))
          return entityType;
      }
    }

    return null;
  }

  /**
   * Returns the type map.
   */
  public HashMap<String,AmberType> getTypeMap()
  {
    return _typeMap;
  }

  /**
   * Adds a type.
   */
  public void put(String name, AmberType type)
  {
    AmberType oldType = _typeMap.get(name);

    if (oldType != null && oldType != type)
      throw new IllegalStateException(L.l("'{0}' is a duplicate type",
                                          name));

    _typeMap.put(name, type);
  }

  static {
    _builtinTypes = new HashMap<String,AmberType>();

    _builtinTypes.put("boolean", BooleanType.create());
    _builtinTypes.put("java.lang.Boolean", BooleanType.create());
    _builtinTypes.put("yes_no", YesNoType.create());
    _builtinTypes.put("true_false", TrueFalseType.create());

    _builtinTypes.put("byte", ByteType.create());
    _builtinTypes.put("java.lang.Byte", ByteType.create());

    _builtinTypes.put("character", CharacterType.create());
    _builtinTypes.put("java.lang.Character", CharacterType.create());

    _builtinTypes.put("short", ShortType.create());
    _builtinTypes.put("java.lang.Short", ShortType.create());

    _builtinTypes.put("integer", IntegerType.create());
    _builtinTypes.put("java.lang.Integer", IntegerType.create());

    _builtinTypes.put("long", LongType.create());
    _builtinTypes.put("java.lang.Long", LongType.create());

    _builtinTypes.put("float", FloatType.create());
    _builtinTypes.put("java.lang.Float", FloatType.create());

    _builtinTypes.put("double", DoubleType.create());
    _builtinTypes.put("java.lang.Double", DoubleType.create());

    _builtinTypes.put("string", StringType.create());
    _builtinTypes.put("java.lang.String", StringType.create());

    _builtinTypes.put("date", SqlDateType.create());
    _builtinTypes.put("java.sql.Date", SqlDateType.create());

    _builtinTypes.put("time", SqlTimeType.create());
    _builtinTypes.put("java.sql.Time", SqlTimeType.create());

    _builtinTypes.put("timestamp", SqlTimestampType.create());
    _builtinTypes.put("java.sql.Timestamp", SqlTimestampType.create());

    _builtinTypes.put("java.util.Date", UtilDateType.create());
    _builtinTypes.put("java.util.Calendar", CalendarType.create());

    //XXX Need test case for timestamp
    _builtinTypes.put("timestamp", BigDecimalType.create());
    _builtinTypes.put("java.math.BigDecimal", BigDecimalType.create());
    _builtinTypes.put("java.math.BigInteger", BigIntegerType.create());

    _builtinTypes.put("blob", BlobType.create());
    _builtinTypes.put("java.sql.Blob", BlobType.create());
    _builtinTypes.put("clob", BlobType.create());
    _builtinTypes.put("java.sql.Clob", ClobType.create());

    _builtinTypes.put("[B", PrimitiveByteArrayType.create());
    _builtinTypes.put("[byte", PrimitiveByteArrayType.create());
    _builtinTypes.put("[java.lang.Byte", ByteArrayType.create());

    _builtinTypes.put("[char", PrimitiveCharArrayType.create());
    _builtinTypes.put("[java.lang.Character", CharacterArrayType.create());

    _builtinTypes.put("class", ClassType.create());
    _builtinTypes.put("java.lang.Class", ClassType.create());

    _primitiveTypes = new HashMap<String,AmberType>();

    _primitiveTypes.put("boolean", PrimitiveBooleanType.create());
    _primitiveTypes.put("char", PrimitiveCharType.create());
    _primitiveTypes.put("byte", PrimitiveByteType.create());
    _primitiveTypes.put("short", PrimitiveShortType.create());
    _primitiveTypes.put("int", PrimitiveIntType.create());
    _primitiveTypes.put("long", PrimitiveLongType.create());
    _primitiveTypes.put("float", PrimitiveFloatType.create());
    _primitiveTypes.put("double", PrimitiveDoubleType.create());
  }
}
