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

package com.caucho.es.parser;

import com.caucho.es.ESBase;
import com.caucho.es.ESId;

import java.util.HashMap;

/**
 * Expr is an intermediate form representing an expression.
 */
class TypeExpr extends Expr {
  private static HashMap types;
  
  private static ESId CAUCHO = ESId.intern("caucho");
  private static ESId JAVA = ESId.intern("java");
  private static ESId PACKAGES = ESId.intern("Packages");

  private ESId id;
  
  protected String _typeName;

  TypeExpr(Block block, ESId id)
  {
    super(block);
    
    this.id = id;
    
    Type type = (Type) types.get(id);
    if (type != null) {
      this.type = type.jsType;
      _typeName = type._name;
      javaType = type.javaClass;
    }
    else {
      this.type = TYPE_ES;
      _typeName = "com.caucho.es.ESBase";
      javaType = ESBase.class;
    }
  }

  static TypeExpr create(Block block, ESId id)
  {
    if (id == CAUCHO)
      return new JavaTypeExpr(block, "com.caucho");
    else if (id == JAVA)
      return new JavaTypeExpr(block, "java");
    else if (id == PACKAGES)
      return new JavaTypeExpr(block, "");
    else
      return new TypeExpr(block, id);
  }

  String getTypeName()
  {
    return _typeName;
  }

  /**
   * Returns a debugging string.
   */
  public String toString()
  {
    return "TypeExpr[" + javaType + " " + type + "]";
  }

  /**
   * Representation of the primitive types.
   */
  static class Type {
    ESId id;
    int jsType;
    String _name;
    Class javaClass;

    Type(ESId id, int jsType, Class cl)
    {
      this.id = id;
      this.jsType = jsType;
      _name = cl.getName();
      this.javaClass = cl;
    }
  }

  static {
    types = new HashMap();
    types.put(ESId.intern("boolean"),
              new Type(ESId.intern("boolean"), TYPE_BOOLEAN, boolean.class));
    types.put(ESId.intern("byte"),
              new Type(ESId.intern("byte"), TYPE_INTEGER, byte.class));
    types.put(ESId.intern("short"),
              new Type(ESId.intern("short"), TYPE_INTEGER, short.class));
    types.put(ESId.intern("int"),
              new Type(ESId.intern("int"), TYPE_INTEGER, int.class));
    types.put(ESId.intern("long"),
              new Type(ESId.intern("long"), TYPE_LONG, long.class));
    types.put(ESId.intern("float"),
              new Type(ESId.intern("float"), TYPE_NUMBER, float.class));
    types.put(ESId.intern("double"),
              new Type(ESId.intern("double"), TYPE_NUMBER, double.class));
    
    types.put(ESId.intern("String"),
              new Type(ESId.intern("String"), TYPE_STRING, String.class));
  }
}
