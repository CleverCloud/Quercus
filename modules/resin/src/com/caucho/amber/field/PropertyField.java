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

import com.caucho.amber.cfg.AbstractConfigIntrospector;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.ColumnExpr;
import com.caucho.amber.expr.EmbeddedExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class PropertyField extends AbstractField {
  private static final L10N L = new L10N(PropertyField.class);
  private static final Logger log
    = Logger.getLogger(PropertyField.class.getName());

  private AmberColumn _column;
  private AmberType _type;

  private KeyManyToOneField _aliasKey;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  public PropertyField(BeanType statefulType,
                       String name)
    throws ConfigException
  {
    super(statefulType, name);
  }

  public PropertyField(BeanType statefulType)
  {
    super(statefulType);
  }

  /**
   * Sets the result type.
   */
  public void setType(AmberType type)
  {
    _type = type;
  }

  /**
   * Sets the result type.
   */
  public AmberType getType()
  {
    return _type;
  }

  /**
   * Returns the source type as
   * entity or mapped-superclass.
   */
  public EntityType getRelatedSourceType()
  {
    return (EntityType) getSourceType();
  }

  /**
   * Returns the table containing the field's columns.
   */
  @Override
  public AmberTable getTable()
  {
    return getColumn().getTable();
  }

  /**
   * Sets the column.
   */
  @Override
  public void setColumn(AmberColumn column)
  {
    _column = column;
  }

  /**
   * Gets the column.
   */
  @Override
  public AmberColumn getColumn()
  {
    return _column;
  }

  /**
   * Set true if the property should be saved on an insert.
   */
  public void setInsert(boolean isInsert)
  {
    _isInsert = isInsert;
  }

  public boolean isInsert()
  {
    return _isInsert;
  }

  /**
   * Set true if the property should be saved on an update.
   */
  public void setUpdate(boolean isUpdate)
  {
    _isUpdate = isUpdate;
  }

  public boolean isUpdate()
  {
    return _isUpdate;
  }

  /**
   * Creates a copy of the field for a parent
   */
  @Override
  public AmberField override(BeanType type)
  {
    PropertyField field = new PropertyField(getSourceType(), getName());

    field.setOverride(true);
    field.setLazy(isLazy());
    field.setInsert(_isInsert);
    field.setUpdate(_isUpdate);

    return field;
  }

  /**
   * Initializes the property.
   */
  @Override
  public void init()
    throws ConfigException
  {
    super.init();

    if (getColumn() == null)
      throw new IllegalStateException(L.l("column must be set before init"));

    // Embedded types have no id.
    // Only entity or mapped-superclass types have id.
    if (! (getSourceType() instanceof EntityType))
      return;

    if (getRelatedSourceType().getId() != null) {
      // resolve any alias
      for (AmberField field : getRelatedSourceType().getId().getKeys()) {
        if (field instanceof KeyManyToOneField) {
          KeyManyToOneField key = (KeyManyToOneField) field;

          for (ForeignColumn column : key.getLinkColumns().getColumns()) {
            if (getColumn().getName().equals(column.getName()))
              _aliasKey = key;
          }
        }
      }
    }
  }

  //
  // getter/setter
  //


  /**
   * Generates the set property.
   */
  @Override
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    out.println();
    out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    if (! (getSourceType() instanceof EmbeddableType)) {
      out.println("if (__caucho_session != null)");
      out.println("  __caucho_load_select_" + getLoadGroupIndex() + "(__caucho_session);");
      out.println();
    }

    out.println("return " + generateSuperGetter("this") + ";");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  @Override
  public void generateSetterMethod(JavaWriter out)
    throws IOException
  {
    if (! isFieldAccess()
        && (getGetterMethod() == null
            || getSetterMethod() == null && ! isAbstract()))
      return;

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    int maskGroup = getLoadGroupIndex() / 64;
    String loadVar = "__caucho_loadMask_" + maskGroup;

    long mask = 1L << (getLoadGroupIndex() % 64);

    // jpa/0gh0
    if (! _isUpdate || getSourceType() instanceof EmbeddableType) {
      out.println(generateSuperSetter("this", "v") + ";");
      out.popDepth();
      out.println("}");
      return;
    }

    out.println("if (__caucho_session == null) {");
    out.println("  " + generateSuperSetter("this", "v") + ";");
    out.println("  return;");
    out.println("}");

    // jpa/0g06, jpa/0g0k, jpa/0j5f
    out.println();
    out.println("if ((" + loadVar + " & " + mask + "L) == 0) {");
    out.println("  __caucho_load_" + maskGroup + "(__caucho_session);");
    //out.println();
    //out.println("  if (__caucho_session.isActiveTransaction())");
    //out.println("    __caucho_session.makeTransactional((com.caucho.amber.entity.Entity) this);");
    out.println("}");
    
    out.println();
    out.println(getJavaTypeName() + " oldValue = " + generateSuperGetter("this") + ";");

    out.println();
    if (getJavaTypeName().equals("java.lang.String")) {
      out.println("if ((oldValue == v || v != null && v.equals(oldValue)) && (" + loadVar + " & " + mask + "L) != 0L)");
      out.println("  return;");
    }
    else {
      out.println("if (oldValue == v && (" + loadVar + " & " + mask + "L) != 0)");
      out.println("  return;");
    }

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println(generateSuperSetter("this", "v") + ";");

    out.popDepth();
    out.println("} catch (Exception e1) {");
    out.pushDepth();

    out.println("throw __caucho_session.rollback(e1);");

    out.popDepth();
    out.println("}");

    int dirtyGroup = getIndex() / 64;
    String dirtyVar = "__caucho_dirtyMask_" + dirtyGroup;

    long dirtyMask = 1L << (getIndex() % 64);

    out.println();
    out.println("long oldMask = " + dirtyVar + ";");
    out.println(dirtyVar + " |= " + dirtyMask + "L;");

    out.println();
    out.println("if (oldMask == 0)");
    out.println("  __caucho_session.update((com.caucho.amber.entity.Entity) this);");
    out.println();
    out.println("__caucho_increment_version();");

    out.popDepth();
    out.println("}");
  }
  

  /**
   * Returns the null value.
   */
  @Override
  public String generateNull()
  {
    return getType().generateNull();
  }

  /**
   * Returns the field name.
   */
  @Override
  protected String getFieldName()
  {
    if (isFieldAccess())
      return super.getFieldName();
    else if (getColumn() == null) {
      // jpa/0w01, jpa/0w10
      return "__amber_" + AbstractConfigIntrospector.toSqlName(getName());
    }
    else
      return getColumn().getFieldName();
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(AmberTable table, String id)
  {
    if (getColumn().getTable() != table) {
      // jpa/0l14 as a negative test
      if (getRelatedSourceType() instanceof EntityType)
        return null;
    }

    return generateSelect(id);
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateSelect(String id)
  {
    return getColumn().generateSelect(id);
  }

  /**
   * Generates the where clause.
   */
  @Override
  public String generateWhere(String id)
  {
    return getColumn().generateSelect(id);
  }

  /**
   * Generates the insert.
   */
  @Override
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert && _aliasKey == null)
      columns.add(getColumn().getName());
  }

  /**
   * Generates the update set clause
   */
  @Override
  public void generateUpdate(CharBuffer sql)
  {
    if (_isUpdate && _aliasKey == null)
      sql.append(getColumn().generateUpdateSet());
    /*
      sql.append(getColumn());
      sql.append("=?");
    */
  }

  /**
   * Generates the set clause for the insert clause.
   */
  @Override
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_aliasKey != null) {
    }
    else if (_isInsert)
      generateStatementSet(out, pstmt, index, obj);
    else if (getLoadGroupIndex() != 0) {
      int groupIndex = getLoadGroupIndex();
      int group = groupIndex / 64;
      long groupMask = 1L << (groupIndex % 64);
      out.println("__caucho_loadMask_" + group + " &= ~" + groupMask + "L;");
    }
  }

  /**
   * Generates the set clause for the insert clause.
   */
  @Override
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_isUpdate && _aliasKey == null)
      generateStatementSet(out, pstmt, index, obj);
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
    // XXX: need to find QA explaining the following test
    if (! isFieldAccess() && getGetterMethod() == null || _aliasKey != null)
      return;

    getColumn().generateSet(out, pstmt, index, generateGet(obj));
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (_aliasKey != null)
      return index;
    /*
      if (getSetterMethod() == null)
      return index;
    */

    String var = "amber_ld" + index;

    AmberType columnType;

    // jpa/0w24
    if (getColumn() == null)
      columnType = getType();
    else
      columnType = getColumn().getType();

    if (columnType instanceof ArrayType)
      out.print(((ArrayType) columnType).getPrimitiveArrayTypeName());
    else
      out.print(getJavaTypeName());
    out.print(" " + var + " = ");

    // jpa/0w24
    if (getColumn() == null)
      index = getType().generateLoad(out, rs, indexVar, index);
    else
      index = getColumn().generateLoad(out, rs, indexVar, index);

    out.println(";");

    // jpa/1417 as a negative test.
    if (columnType instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) columnType;
      String primitiveType = arrayType.getPrimitiveArrayTypeName();

      out.print(getJavaTypeName() + " " + var + "_temp = null;");
      out.println();

      // jpa/110d
      out.print("if (" + var + " != null) {");
      out.pushDepth();

      out.print(var + "_temp = new ");
      String instanceJavaType = arrayType.getJavaObjectTypeName();
      out.println(instanceJavaType + "[" + var + ".length];");
      out.println("for (int i=0; i < " + var + ".length; i++)");
      out.print("  " + var + "_temp[i] = new ");
      out.print(instanceJavaType);
      out.println("(" + var + "[i]);");

      out.popDepth();
      out.println("}");

      out.println();
      out.println(generateSuperSetter("this", var + "_temp") + ";");
    }
    else
      out.println(generateSuperSetter("this", var) + ";");

    // out.println("__caucho_loadMask |= " + (1L << getIndex()) + "L;");

    return index;
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    if (_aliasKey != null || getColumn() == null)
      return index;

    String var = "amber_ld" + index;

    AmberType columnType;

    columnType = getColumn().getType();

    out.print(getJavaTypeName());
    out.print(" " + var + " = ");

    // jpa/0w24
    index = getColumn().generateLoadNative(out, index);

    out.println(";");
    
    out.println(generateSuperSetter("this", var) + ";");

    return index;
  }

  /**
   * Generates loading code
   */
  @Override
  public void generateNativeColumnNames(ArrayList<String> names)
  {
    if (_aliasKey != null || getColumn() == null)
      return;
    
    getColumn().generateNativeColumnNames(names);
  }

  /**
   * Creates the expression for the field.
   */
  @Override
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    AmberColumn column;

    if (parent instanceof EmbeddedExpr) {
      column = ((EmbeddedExpr) parent).getColumnByFieldName(getName());
    }
    else
      column = getColumn();

    return new ColumnExpr(parent, column);
  }
}
