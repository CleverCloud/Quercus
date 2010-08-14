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
import com.caucho.amber.expr.KeyManyToOneExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class KeyManyToOneField extends ManyToOneField implements IdField {
  private static final L10N L = new L10N(KeyManyToOneField.class);
  protected static final Logger log
    = Logger.getLogger(KeyManyToOneField.class.getName());

  // fields
  private ArrayList<KeyPropertyField> _idFields =
    new ArrayList<KeyPropertyField>();

  // use field accessors to get key values.
  private boolean _isKeyField;

  public KeyManyToOneField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);
  }

  public KeyManyToOneField(EntityType entityType,
                           String name,
                           LinkColumns columns)
    throws ConfigException
  {
    super(entityType, name);

    setLinkColumns(columns);

    setSourceCascadeDelete(true);
  }

  /**
   * Gets the generator.
   */
  public String getGenerator()
  {
    return null;
  }

  /**
   * Returns the target type as entity (ejb 2.1)
   * See com.caucho.ejb.ql.Expr
   */
  public EntityType getEntityType()
  {
    return (EntityType) getEntityTargetType();
  }

  public AmberType getType()
  {
    return getEntityTargetType();
  }

  public AmberColumn getColumn()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true for a generator.
   */
  public boolean isAutoGenerate()
  {
    return false;
  }

  /**
   * Set true if key fields are accessed through fields.
   */
  public void setKeyField(boolean isKeyField)
  {
    _isKeyField = isKeyField;
  }

  /**
   * Returns the foreign type name.
   */
  public String getForeignTypeName()
  {
    return getJavaTypeName();
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public boolean isTargetCascadeDelete()
  {
    return false;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public boolean isSourceCascadeDelete()
  {
    return true;
  }

  /**
   * Initialize the field.
   */
  public void init()
    throws ConfigException
  {
    super.init();

    ArrayList<IdField> keys = getEntityTargetType().getId().getKeys();

    ArrayList<ForeignColumn> columns = getLinkColumns().getColumns();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);
      ForeignColumn column = columns.get(i);

      KeyPropertyField field;
      field = new IdentifyingKeyPropertyField(getEntitySourceType(), column);

      _idFields.add(field);
    }
  }

  /**
   * Returns the component count.
   */
  public int getComponentCount()
  {
    return getEntityTargetType().getId().getKeyCount();
  }

  /**
   * Returns columns
   */
  public ArrayList<AmberColumn> getColumns()
  {
    ArrayList<AmberColumn> columns = new ArrayList<AmberColumn>();

    columns.addAll(getLinkColumns().getColumns());

    return columns;
  }

  /**
   * Returns the identifying field matching the target's id.
   */
  public KeyPropertyField getIdField(IdField field)
  {
    ArrayList<IdField> keys = getEntityTargetType().getId().getKeys();

    if (_idFields.size() != keys.size()) {
      try {
        init();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    for (int i = 0; i < keys.size(); i++) {
      if (keys.get(i) == field)
        return _idFields.get(i);
    }

    throw new IllegalStateException(field.toString());
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new KeyManyToOneExpr(parent, this);
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return getLinkColumns().generateMatchArgSQL(id);
  }

  /**
   * Returns the where code
   */
  public String generateRawWhere(String id)
  {
    CharBuffer cb = new CharBuffer();

    String prefix = id + "." + getName();

    ArrayList<IdField> keys = getEntityTargetType().getId().getKeys();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      cb.append(keys.get(i).generateRawWhere(prefix));
    }

    return cb.toString();
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
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb, String table1, String table2)
  {
    cb.append(getLinkColumns().generateJoin(table1, table2));
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(AmberColumn key)
  {
    return getLinkColumns().getSourceColumn(key);
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
   * Returns the actual data.
   */
  public String generateSuperGetter(String objThis)
  {
    if (isAbstract() || getGetterMethod() == null)
      return getFieldName();
    else
      return getGetterMethod().getName() + "()";
  }

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String objThis, String value)
  {
    if (isAbstract() || getGetterMethod() == null || getSetterMethod() == null)
      return objThis + '.' + getFieldName() + " = " + value + ";";
    else
      return objThis + '.' + getSetterMethod().getName() + "(" + value + ")";
  }

  /**
   * Generates code to copy to an object.
   */
  public void generateCopy(JavaWriter out,
                           String dest,
                           String source)
    throws IOException
  {
    out.println(generateSet(dest, generateGet(source)) + ";");
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    out.print("(" + getForeignTypeName() + ") ");

    out.print("aConn.loadProxy(\"" + getEntityTargetType().getName() + "\", ");
    index = getEntityTargetType().getId().generateLoadForeign(out, rs, indexVar, index,
                                                              getName());

    out.println(");");

    return index;
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

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
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    ArrayList<ForeignColumn> columns = getLinkColumns().getColumns();

    Id id = getEntityTargetType().getId();
    ArrayList<IdField> keys = id.getKeys();

    String prop = value != null ? generateGet(value) : null;
    for (int i = 0; i < columns.size(); i++ ){
      IdField key = keys.get(i);
      ForeignColumn column = columns.get(i);

      column.generateSet(out, pstmt, index, key.generateGet(prop));
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    String var = getFieldName();

    Id id = getEntityTargetType().getId();
    ArrayList<IdField> keys = id.getKeys();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      key.getType().generateSet(out, pstmt, index, key.generateGet(var));
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    String value = generateSuperGetter("this");

    out.println("if (" + getEntityTargetType().generateIsNull(value) + ") {");
    out.pushDepth();

    getEntityTargetType().generateSetNull(out, pstmt, index);

    out.popDepth();
    out.println("} else {");
    out.pushDepth();

    generateStatementSet(out, pstmt, index);

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
    throws IOException
  {
    if (_isKeyField)
      return key + "." + getName();
    else
      return generateGet(key);
  }

  /**
   * Generates the set clause.
   */
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {
  }

  /**
   * Generates the set clause.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
    out.println("if (" + generateSuperGetter("this") + " == null)");
    out.println("  throw new com.caucho.amber.AmberException(\"primary key must not be null on creation.  " + getGetterName() + "() must not return null.\");");
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    return  "(" + value + " == null)";
  }

  /**
   * Converts from an object.
   */
  public String toValue(String value)
  {
    return "((" + getJavaTypeName() + ") " + value + ")";
  }
}
