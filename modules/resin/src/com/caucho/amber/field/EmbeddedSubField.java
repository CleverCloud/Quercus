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

import com.caucho.amber.expr.*;
import com.caucho.amber.manager.*;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents the sub-field of an embedded type.
 */
public class EmbeddedSubField implements AmberField {
  private static final L10N L = new L10N(EmbeddedSubField.class);
  protected static final Logger log
    = Logger.getLogger(EmbeddedSubField.class.getName());

  // The owning embedded field in the entity
  private EntityEmbeddedField _embeddedField;

  // The corresponding field of the embeddable type
  private AmberField _embeddableField;

  private AmberColumn _column;
  private boolean _isInsert;
  private boolean _isUpdate;

  private int _index;

  public EmbeddedSubField(EntityEmbeddedField embeddedField,
                          AmberField embeddableField,
                          int index)
    throws ConfigException
  {
    _embeddedField = embeddedField;
    _embeddableField = embeddableField;
    _index = index;

    AmberColumn embeddableColumn;

    if (embeddableField instanceof PropertyField) {
      embeddableColumn = ((PropertyField) embeddableField).getColumn();
    }
    else
      throw new IllegalStateException(L.l("'{0}' is an unknown field type of @Embeddable bean.",
                                          embeddableField.getClass().getName()));
    

    if (embeddableColumn == null)
      throw new IllegalStateException(embeddableField + " column is null");

    _column = new AmberColumn(_embeddedField.getTable(),
                         embeddableColumn.getName(),
                         embeddableColumn.getType());
  }
  
  /**
   * Returns the owning entity class.
   */
  public BeanType getSourceType()
  {
    return _embeddedField.getSourceType();
  }

  /**
   * Returns true if and only if this is a LAZY field.
   */
  public boolean isLazy()
  {
    return _embeddedField.isLazy();
  }

  /**
   * Returns the field name.
   */
  public String getName()
  {
    return _embeddableField.getName();
  }

  /**
   * Returns the table containing the value (or null)
   */
  public AmberTable getTable()
  {
    return getColumn().getTable();
  }

  public AmberColumn getColumn()
  {
    return _column;
  }

  /**
   * Returns the property index.
   */
  public int getIndex()
  {
    return _embeddedField.getIndex();
  }

  /**
   * Returns the property's group index.
   */
  public int getLoadGroupIndex()
  {
    return _embeddedField.getLoadGroupIndex();
  }

  /**
   * Returns the load group mask.
   */
  public long getCreateLoadMask(int group)
  {
    return _embeddedField.getCreateLoadMask(group);
  }

  /**
   * Returns the type of the field
   */
  public JType getJavaType()
  {
    return _embeddableField.getJavaType();
  }

  /**
   * Returns the type of the field
   */
  public Class getJavaClass()
  {
    return getJavaType().getRawType().getJavaClass();
  }

  /**
   * Returns the name of the java type.
   */
  public String getJavaTypeName()
  {
    return _embeddableField.getJavaTypeName();
  }

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isAbstract()
  {
    return false;
  }

  /**
   * Returns true if the field is cascadable.
   */
  public boolean isCascadable()
  {
    return false;
  }

  /**
   * Returns true for an updateable field.
   */
  public boolean isUpdateable()
  {
    return true;
  }

  /**
   * Links to the target.
   */
  public void setIndex(int index)
  {
  }

  //
  // getter/setter
  //

  /**
   * Returns the getter method.
   */
  public Method getGetterMethod()
  {
    return _embeddedField.getGetterMethod();
  }

  /**
   * Returns the getter name.
   */
  public String getGetterName()
  {
    return _embeddedField.getGetterName();
  }

  /**
   * Returns the setter method.
   */
  public Method getSetterMethod()
  {
    return _embeddedField.getSetterMethod();
  }

  /**
   * Returns the setter name.
   */
  public String getSetterName()
  {
    return _embeddedField.getSetterName();
  }

  /**
   * Returns the actual data.
   */
  public String generateSuperGetter(String objThis)
  {
    if (! getSourceType().isEmbeddable())
      return "__caucho_super_get_" + getName() + "()";
    else if (getSourceType().isFieldAccess())
      return "__caucho_super_get_" + getName() + "()";
    else
      return getGetterMethod().getName() + "()";
  }

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String objThis, String value)
  {
    /*
    if (! getSourceType().isEmbeddable())
      return objThis + "." + "__caucho_super_set_" + getName() + "(" + value + ")";
      else
   */
    if (getSourceType().isFieldAccess())
      return objThis + "." + getName() + " = " + value;
    else
      return objThis + "." + getSetterName() + "(" + value + ")";
  }

  /**
   * Generates loading cache
   */
  public void generateSet(JavaWriter out, String objThis, String value)
    throws IOException
  {
    _embeddedField.generateSet(out, objThis, value);
  }

  /**
   * Links to the target.
   */
  public void init()
    throws ConfigException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
  }

  /**
   * Generates the post constructor fixup
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out, String mask, String pstmt,
                             String index)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates loading code
   */
  public boolean hasLoadGroup(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int loadGroupIndex)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates loading code after the basic fields.
   */
  public int generatePostLoadSelect(JavaWriter out, int index)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates loading for a native query
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading for a native query
   */
  public void generateNativeColumnNames(ArrayList<String> names)
  {
  }

  /**
   * Generates loading cache
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public void generateGet(JavaWriter out, String value)
    throws IOException
  {
    out.print(generateGet(value));
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public String generateGet(String objThis)
  {
    String fieldType = getColumn().getType().getForeignType().getJavaTypeName();
    
    return ("((" + fieldType + ") "
            + "((Embeddable) "
            + _embeddedField.generateGet(objThis)
            + ").__caucho_get_field(" + _index + "))");
  }

  /**
   * Generates the field setter.
   *
   * @param value the non-null value
   */
  public String generateSet(String obj, String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the get property.
   */
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the set property.
   */
  public void generateSetterMethod(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the get property.
   */
  public void generateSuperGetterMethod(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the get property.
   */
  public void generateSuperSetterMethod(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Override the field
   */
  public AmberField override(BeanType entityType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the table create.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the JDBC preparedStatement set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    getColumn().generateSet(out, pstmt, index, generateGet("this"));
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Generates the set clause for the insert clause.
   */
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadIndex)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Updates the cached copy.
   */
  public void generateMergeFrom(JavaWriter out,
                                      String dst, String src)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks entity-relationships from an object.
   */
  public void generateDumpRelationships(JavaWriter out,
                                        int updateIndex)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
    getColumn().generateSet(out, pstmt, index, generateGet(obj));
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Links to the target.
   */
  public void link()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the delete foreign
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the delete foreign
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the expire code.
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deletes the children
   */
  public void childDelete(AmberConnection aConn, Serializable primaryKey)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates code to convert to the type from the object.
   */
  public String generateCastFromObject(String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value)
  {
    throw new UnsupportedOperationException();
  }

  //
  // SQL generation

  /**
   * Generates the select clause for an entity load.
   */
  public String generateLoadSelect(AmberTable table, String id)
  {
    if (getColumn().getTable() == table)
      return generateSelect(id);
    else
      return null;
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    return getColumn().generateSelect(id);
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert)
      columns.add(getColumn().getName());
  }

  /**
   * Generates the JPA QL select clause.
   */
  public String generateJavaSelect(String id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    return getColumn().generateSelect(id);
  }

  /**
   * Generates the where clause.
   */
  public void generateUpdate(CharBuffer sql)
  {
    if (_isUpdate)
      sql.append(getColumn().generateUpdateSet());
  }

  /**
   * Generates any code needed before a persist occurs
   */
  public void generatePrePersist(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates any code needed for detachment
   */
  public void generateDetach(JavaWriter out)
    throws IOException
  {
  }

  //
  // Query methods

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    throw new UnsupportedOperationException();
  }
}
