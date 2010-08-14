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

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.BeanType;
import com.caucho.bytecode.JType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Configuration for a bean's property
 */
public interface AmberField {
  /**
   * Returns the owning entity class.
   */
  public BeanType getSourceType();

  /**
   * Returns the field name.
   */
  public String getName();

  /**
   * Returns true if and only if this is a LAZY field.
   */
  public boolean isLazy();

  /**
   * Returns the table containing the value (or null)
   */
  public AmberTable getTable();

  /**
   * Returns the column for the field.
   */
  public AmberColumn getColumn();

  /**
   * Returns the property index.
   */
  public int getIndex();

  /**
   * Returns the property's group index.
   */
  public int getLoadGroupIndex();

  /**
   * Returns the load group mask.
   */
  public long getCreateLoadMask(int group);

  /**
   * Returns the class of the field
   */
  public Class getJavaClass();
  
  /**
   * Returns the type of the field
   */
  public JType getJavaType();

  /**
   * Returns the name of the java type.
   */
  public String getJavaTypeName();

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isAbstract();

  /**
   * Returns true if the field is cascadable.
   */
  public boolean isCascadable();

  /**
   * Returns true for an updateable field.
   */
  public boolean isUpdateable();

  /**
   * Links to the target.
   */
  public void setIndex(int index);

  /**
   * Override the field
   */
  public AmberField override(BeanType entityType);

  /**
   * Links to the target.
   */
  public void init()
    throws ConfigException;

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException;

  //
  // getter/setter
  //

  /**
   * Returns the getter method.
   */
  public Method getGetterMethod();

  /**
   * Returns the setter method.
   */
  public Method getSetterMethod();

  /**
   * Returns the getter name.
   */
  public String getGetterName();

  /**
   * Returns the setter name.
   */
  public String getSetterName();
  
  /**
   * Returns the actual data.
   */
  public String generateSuperGetter(String objThis);

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String objThis, String value);

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public String generateGet(String objThis);

  /**
   * Generates the field setter.
   *
   * @param value the non-null value
   */
  public String generateSet(String objThis, String value);

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public void generateGet(JavaWriter out, String objThis)
    throws IOException;

  /**
   * Generates loading cache
   */
  public void generateSet(JavaWriter out, String objThis, String value)
    throws IOException;

  /**
   * Generates the get property
   */
  public void generateSuperGetterMethod(JavaWriter out)
    throws IOException;

  /**
   * Generates the get property
   */
  public void generateSuperSetterMethod(JavaWriter out)
    throws IOException;

  /**
   * Generates the get property implementation.
   */
  public void generateGetterMethod(JavaWriter out)
    throws IOException;

  /**
   * Generates the set property implementation.
   */
  public void generateSetterMethod(JavaWriter out)
    throws IOException;

  /**
   * Generates detachment code
   */
  public void generateDetach(JavaWriter out)
    throws IOException;
  
  //
  // constructor code
  //

  /**
   * Generates the post constructor fixup
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException;

  /**
   * Generates the select clause for an entity load.
   */
  public String generateLoadSelect(AmberTable table, String id);

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id);

  /**
   * Generates the JPA QL select clause.
   */
  public String generateJavaSelect(String id);

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id);

  /**
   * Generates the where clause.
   */
  public void generateUpdate(CharBuffer sql);

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out, String mask, String pstmt,
                             String index)
    throws IOException;

  /**
   * Generates loading code
   */
  public boolean hasLoadGroup(int index);

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int loadGroupIndex)
    throws IOException;

  /**
   * Generates loading code after the basic fields.
   */
  public int generatePostLoadSelect(JavaWriter out, int index)
    throws IOException;

  /**
   * Generates loading cache
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException;

  /**
   * Generates loading code
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException;

  /**
   * Generates loading code
   */
  public void generateNativeColumnNames(ArrayList<String> names);

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException;

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns);

  /**
   * Generates the table create.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager);

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException;

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException;

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException;

  /**
   * Generates any code needed before a persist occurs
   */
  public void generatePrePersist(JavaWriter out)
    throws IOException;

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException;

  /**
   * Updates the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadIndex)
    throws IOException;

  /**
   * Updates the cached copy.
   */
  public void generateMergeFrom(JavaWriter out,
                                String dst, String src)
    throws IOException;

  /**
   * Checks entity-relationships from an object.
   */
  public void generateDumpRelationships(JavaWriter out,
                                        int updateIndex)
    throws IOException;

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException;

  /**
   * Converts to an object.
   */
  public String toObject(String value);

  /**
   * Links to the target.
   */
  public void link();

  /**
   * Generates the delete foreign
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException;

  /**
   * Generates the delete foreign
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException;

  /**
   * Generates the expire code.
   */
  public void generateExpire(JavaWriter out)
    throws IOException;

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException;

  /**
   * Deletes the children
   */
  public void childDelete(AmberConnection aConn, Serializable primaryKey)
    throws SQLException;

  /**
   * Generates code to convert to the type from the object.
   */
  public String generateCastFromObject(String value);

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value);

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent);
}
