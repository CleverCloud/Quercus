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

package com.caucho.amber.field;

import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;
import java.sql.Timestamp;

/**
 * Configuration for a bean's field
 */
public class VersionField extends PropertyField {
  private static final L10N L = new L10N(VersionField.class);
  private static final Logger log
    = Logger.getLogger(VersionField.class.getName());

  public VersionField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);

    setLazy(false);
  }

  public VersionField(EntityType entityType)
  {
    super(entityType);

    setLazy(false);
  }

  /**
   * Generates the is null test.
   */
  public String generateIsNull()
  {
    String getter = generateSuperGetter("this");
    AmberType type = getColumn().getType();

    return type.generateIsNull(getter);
  }

  /**
   * Generates the increment version.
   */
  public void generateIncrementVersion(JavaWriter out)
    throws IOException
  {
    int dirtyGroup = getIndex() / 64;
    String dirtyVar = "__caucho_dirtyMask_" + dirtyGroup;

    long dirtyMask = 1L << (getIndex() % 64);

    String getter = generateSuperGetter("this");
    AmberType type = getColumn().getType();

    // jpa/0x02
    out.println();
    out.println("if (" + generateIsNull() + ")");

    if (type.getJavaTypeName().equals(Timestamp.class.getName()))
      out.println("  " + generateSuperSetter("this", "new java.sql.Timestamp(com.caucho.util.Alarm.getExactTime())") + ";");
    else
      out.println("  " + generateSuperSetter("this", "new Integer(1)") + ";");
      
    out.println("else");
    out.println("  " + generateSuperSetter("this", type.generateIncrementVersion(getter)) + ";");

    out.println();
    out.println("long oldMask = " + dirtyVar + ";");
    out.println(dirtyVar + " |= " + dirtyMask + "L;");
    out.println();
    out.println("if (__caucho_session != null && oldMask == 0)");
    out.println("  __caucho_session.update(this);");
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return getColumn().generateMatchArgWhere(id);
  }

  /**
   * Generates the post constructor initialization.
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
    // jpa/0x02

    String setter = getSetterName();
    String typeName = getJavaTypeName();
    Object initialValue = getColumn().getType().toObject(1);

    out.println("if (" + generateIsNull() + ");");
    out.println("  __caucho_increment_version();");
  }

  /**
   * Generates the set version clause.
   */
  public void generateSetVersion(JavaWriter out,
                                 String pstmt,
                                 String index)
    throws IOException
  {
    String value = generateGet("super");
    AmberType type = getColumn().getType();
    // jpa/0x02
    getColumn().generateSetVersion(out, pstmt, index, value); // type.generateIncrementVersion(value));
  }

  /**
   * Generates the update set clause
   */
  public void generateUpdate(CharBuffer sql)
  {
    sql.append(getColumn().generateUpdateSet());
  }

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out,
                             String maskVar,
                             String pstmt,
                             String index)
    throws IOException
  {
    // jpa/0x02
    //
    // int group = getIndex() / 64;
    //
    // out.println();
    // out.println("if (" + maskVar + "_" + group + " != 0L) {");
    // out.pushDepth();

    generateSetVersion(out, pstmt, index);

    // out.popDepth();
    // out.println("}");
  }
}
