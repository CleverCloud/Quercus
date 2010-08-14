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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.field;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.KeyColumnExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class KeyPropertyField extends PropertyField implements IdField {
  private static final L10N L = new L10N(KeyPropertyField.class);
  private static final Logger log
    = Logger.getLogger(KeyPropertyField.class.getName());

  private AmberColumn _column;
  private boolean _isKeyField;
  private String _generator;

  public KeyPropertyField(EntityType tableType)
  {
    super(tableType);
  }

  public KeyPropertyField(EntityType tableType,
                          String name)
    throws ConfigException
  {
    super(tableType, name);
  }

  public KeyPropertyField(EntityType entityType,
                          String name,
                          AmberColumn column)
    throws ConfigException
  {
    super(entityType, name);

    _column = column;
    _column.setPrimaryKey(true);
  }

  /**
   * Returns true for a key
   */
  @Override
  public boolean isKey()
  {
    return true;
  }

  /**
   * Returns true if key fields are accessed through fields.
   */
  public boolean isKeyField()
  {
    return _isKeyField;
  }

  /**
   * Set true if key fields are accessed through fields.
   */
  public void setKeyField(boolean isKeyField)
  {
    _isKeyField = isKeyField;
  }

  /**
   * Sets the generator.
   */
  public void setGenerator(String generator)
  {
    _generator = generator;

    if (_column != null)
      _column.setGeneratorType(generator);
  }

  /**
   * Gets the generator.
   */
  public String getGenerator()
  {
    return _generator;
  }

  /**
   * Returns true for a generator.
   */
  public boolean isAutoGenerate()
  {
    return _generator != null;
  }

  /**
   * Sets the column
   */
  @Override
  public void setColumn(AmberColumn column)
  {
    _column = column;
    _column.setPrimaryKey(true);
  }

  /**
   * Returns column
   */
  @Override
  public AmberColumn getColumn()
  {
    return _column;
  }

  /**
   * Returns columns
   */
  public ArrayList<AmberColumn> getColumns()
  {
    ArrayList<AmberColumn> columns = new ArrayList<AmberColumn>();

    columns.add(_column);

    return columns;
  }

  /**
   * Returns the component count.
   */
  public int getComponentCount()
  {
    return 1;
  }

  /**
   * Returns type
   */
  @Override
  public AmberType getType()
  {
    return _column.getType();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    // jpa/0gg2
    if (_column == null)
      return "";

    return _column.getType().getForeignTypeName();
  }

  /**
   * Generates any prologue.
   */
  @Override
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    /*
    if (isAbstract()) {
      out.println();

      out.println();
      out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
      out.println("{");
      out.println("  return " + getFieldName() + ";");
      out.println("}");

      out.println();
      out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
      out.println("{");
      out.println("  " + getFieldName() + " = v;");
      out.println("}");
    }
    */
  }

  //
  // getter/setter generation
  //

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  @Override
  public String generateGet(String objThis)
  {
    if (objThis == null)
      return generateNull();

    if ("super".equals(objThis))
      return generateSuperGetter("this");
    else
      return generateSuperGetter(objThis);
  }

  /**
   * Generates the field setter.
   *
   * @param value the non-null value
   */
  @Override
  public String generateSet(String objThis, String value)
  {
    if ("super".equals(objThis))
      objThis = "this";
    
    if (isFieldAccess()) {
      // jpa/0l05
      return generateSuperSetter(objThis, value);
    }
    else
      return objThis + "." + getSetterName() + "(" + value + ")";
  }

  //
  // copy
  //

  /**
   * Keys are not merged
   */
  @Override
  public void generateMergeFrom(JavaWriter out,
                                String dst, String src)
    throws IOException
  {
  }

  /**
   * Generates code to copy to an object.
   */
  public void generateCopy(JavaWriter out,
                           String dest,
                           String source)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a copy of the field for a parent
   */
  @Override
  public AmberField override(BeanType type)
  {
    KeyPropertyField field
      = new KeyPropertyField((EntityType) getSourceType(), getName());

    field.setOverride(true);
    field.setLazy(isLazy());
    field.setInsert(isInsert());
    field.setUpdate(isUpdate());

    return field;
  }

  /**
   * Returns the select code
   */
  @Override
  public String generateSelect(String id)
  {
    return _column.generateSelect(id);
  }

  /**
   * Returns the JPA QL select code
   */
  @Override
  public String generateJavaSelect(String id)
  {
    String select = getName();

    if (id != null)
      select = id + "." + select;

    return select;
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return _column.generateMatchArgWhere(id);
  }

  /**
   * Returns the where code
   */
  public String generateRawWhere(String id)
  {
    return id + "." + getName() + "=?";
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    return generateLoadForeign(out, rs, indexVar, index,
                               getForeignTypeName().replace('.', '_'));
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    out.println(generateSuperSetter("this", generateGet(obj)) + ";");
  }

  /**
   * Generates loading cache
   */
  public String generateSetNull(String obj)
    throws IOException
  {
    return generateSet(obj, getColumn().getType().generateNull());
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    return  "(" + getType().generateIsNull(generateSuperGetter("this")) + ")";
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    // XXX: 0 == null
    return _column.getType().generateLoadForeign(out, rs, indexVar, index);
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    if (value == null)
      _column.getType().generateSetNull(out, pstmt, index);
    else
      _column.getType().generateSet(out, pstmt, index,
                                    generateGet(value));
  }

  /**
   * Generates code for a match.
   */
  public void generateMatch(JavaWriter out, String key)
    throws IOException
  {
    out.println("return " + generateEquals("super", key) + ";");
  }

  /**
   * Generates code to test the equals.
   */
  @Override
  public String generateEquals(String left, String right)
  {
    return _column.getType().generateEquals(left, right);
  }

  /**
   * Generates the set clause.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    String value = generateSuperGetter("this");

    if (isAutoGenerate()) {
      out.println("if (! (" + getType().generateIsNull(value) + ")) {");
      out.pushDepth();

      generateStatementSet(out, pstmt, index);

      out.popDepth();

      out.println("} else if (! __caucho_home.isIdentityGenerator()) {");
      out.pushDepth();

      getType().generateSetNull(out, pstmt, index);

      out.popDepth();
      out.println("}");
    }
    else
      generateStatementSet(out, pstmt, index);
  }

  /**
   * Generates the set clause.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
    /*
      out.println("insertSql.append(\"" + getName() + "\");");
      out.println("insertValues.append(\"?\");");
    */

    if ("identity".equals(_generator))
      return;
    else if (_generator != null) {
      // XXX: logic change since the class needs to be enhanced before
      // the driver
      /*
      if (getEntitySourceType().getGenerator(getName()) == null)
        throw new IllegalStateException("no sequence generator for " + getName());
      */

      out.println("if (" + getType().generateIsNull(generateSuperGetter("this")) + " && home.isSequenceGenerator()) {");
      out.pushDepth();

      String id = "home.nextGeneratorId(aConn, \"" + getName() + "\")";

      String javaType = getType().getJavaTypeName();

      if ("long".equals(javaType))
        id = "(" + javaType + ") " + id;
      else if ("int".equals(javaType))
        id = "(" + javaType + ") " + id;
      else if ("short".equals(javaType))
        id = "(" + javaType + ") " + id;
      else if ("java.lang.Long".equals(javaType))
        id = "new Long(" + id + ")";
      else if ("java.lang.Integer".equals(javaType))
        id = "new Integer((int) " + id + ")";
      else if ("java.lang.Short".equals(javaType))
        id = "new Short((short) " + id + ")";
      else if ("java.lang.Byte".equals(javaType))
        id = "new Byte((byte) " + id + ")";
      else
        throw new UnsupportedOperationException(L.l("{0} is an unsupported generated key type.",
                                                    javaType));

      out.println(generateSuperSetter("this", id) + ";");

      out.popDepth();
      out.println("}");

      return;
    }

    if (! getJavaType().isPrimitive()) {
      out.println("if (" + getType().generateIsNull(generateSuperGetter("this")) + ")");
      out.println("  throw new com.caucho.amber.AmberException(\"primary key must not be null on creation.  " + getGetterName() + "() must not return null.\");");
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {
    if (! ("identity".equals(_generator) || "auto".equals(_generator)))
      return;

    out.print("if (");
    out.println(getType().generateIsNull(generateSuperGetter("this")));
    out.println("    && __caucho_home.isIdentityGenerator()) {");
    out.pushDepth();

    String var = "__caucho_rs_" + out.generateId();

    out.println("java.sql.ResultSet " + var + " = " + pstmt + ".getGeneratedKeys();");
    out.println("if (" + var + ".next()) {");
    out.pushDepth();

    out.print(getType().getName() + " v1 = ");
    getType().generateLoad(out, var, "", 1);
    out.println(";");

    out.println(generateSuperSetter("this", "v1") + ";");

    out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINER))");
    out.println("  __caucho_log.finer(\"create with new primaryKey \" + " + generateSuperGetter("this") + ");");

    out.popDepth();
    out.println("}");
    out.println("else throw new java.sql.SQLException();");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the setter for a key property
   */
  public String generateSetKeyProperty(String key, String value)
    throws IOException
  {
    if (_isKeyField)
      return key + "." + getName() + " = " + value;
    else
      return generateSet(key, value);
  }

  /**
   * Generates the getter for a key property
   */
  public String generateGetKeyProperty(String key)
  {
    if (_isKeyField)
      return key + "." + getName();
    else
      return generateGet(key);
  }

  /**
   * Generates the property getter for an EJB proxy
   *
   * @param value the non-null value
   */
  public String generateGetProxyProperty(String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates the expression for the field.
   */
  @Override
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new KeyColumnExpr(parent, getColumn());
  }

  /**
   * Converts to an object.
   */
  @Override
  public String toObject(String value)
  {
    return getColumn().getType().toObject(value);
  }

  /**
   * Converts from an object.
   */
  public String toValue(String value)
  {
    return getColumn().getType().generateCastFromObject(value);
  }
}
