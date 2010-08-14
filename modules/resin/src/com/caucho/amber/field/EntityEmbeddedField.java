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
import com.caucho.amber.expr.EmbeddedExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for a bean's embedded field
 */
public class EntityEmbeddedField extends AbstractField
{
  private static final L10N L = new L10N(EntityEmbeddedField.class);
  protected static final Logger log
    = Logger.getLogger(EntityEmbeddedField.class.getName());
  
  private EmbeddableType _embeddableType;

  private ArrayList<EmbeddedSubField> _subFields;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  public EntityEmbeddedField(EntityType ownerType,
                             EmbeddableType embeddableType,
                             String name)
    throws ConfigException
  {
    super(ownerType, name);

    setEmbeddableType(embeddableType);
  }

  public EntityEmbeddedField(EntityType ownerType,
                             EmbeddableType embeddableType)
  {
    super(ownerType);

    setEmbeddableType(embeddableType);
  }

  public EmbeddableType getEmbeddableType()
  {
    return _embeddableType;
  }

  /**
   * Sets the result type.
   */
  public void setEmbeddableType(EmbeddableType type)
  {
    _embeddableType = type;

    _subFields = new ArrayList<EmbeddedSubField>();

    ArrayList<AmberField> fields = type.getFields();
    for (int i = 0; i < fields.size(); i++) {
      _subFields.add(createSubField(fields.get(i), i));
    }
  }

  protected EmbeddedSubField createSubField(AmberField field, int index)
  {
    return new EmbeddedSubField(this, field, index);
  }

  /**
   * Sets the result type.
   */
  public AmberType getType()
  {
    return _embeddableType;
  }

  /**
   * Returns the subfields.
   */
  public ArrayList<EmbeddedSubField> getSubFields()
  {
    return _subFields;
  }

  /**
   * Returns true if the property is an @EmbeddedId.
   */
  public boolean isEmbeddedId()
  {
    return false;
  }

  /**
   * Set true if the property should be saved on an insert.
   */
  public void setInsert(boolean isInsert)
  {
    _isInsert = isInsert;
  }

  /**
   * Set true if the property should be saved on an update.
   */
  public void setUpdate(boolean isUpdate)
  {
    _isUpdate = isUpdate;
  }

  /**
   * Initializes the property.
   */
  public void init()
    throws ConfigException
  {
    super.init();
  }

  /**
   * Returns the null value.
   */
  public String generateNull()
  {
    return getType().generateNull();
  }

  /**
   * Generates the set property.
   */
  public void generateGetterMethod(JavaWriter out)
    throws IOException
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    out.println();
    out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    // XXX: must not load the entity to return the pk. Avoids StackOverflow.
    if (! (this instanceof EmbeddedIdField)) {
      out.println("if (__caucho_session != null)");
      out.println("  __caucho_load_" + getLoadGroupIndex() + "(__caucho_session);");
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
    if (! isFieldAccess() && (getGetterMethod() == null
                              || getSetterMethod() == null && ! isAbstract()))
      return;

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    if (! _isUpdate) {
      out.println("if (__caucho_session == null)");
      out.println("  " + generateSuperSetter("this", "v") + ";");
    }
    else {
      out.println(getJavaTypeName() + " oldValue = " + generateSuperGetter("this") + ";");

      int maskGroup = getLoadGroupIndex() / 64;
      String loadVar = "__caucho_loadMask_" + maskGroup;

      long mask = 1L << (getLoadGroupIndex() % 64);

      if (getJavaTypeName().equals("java.lang.String")) {
        out.println("if ((oldValue == v || v != null && v.equals(oldValue)) && (" + loadVar + " & " + mask + "L) != 0L)");
        out.println("  return;");
      }
      else {
        out.println("if (oldValue == v && (" + loadVar + " & " + mask + "L) != 0)");
        out.println("  return;");
      }

      out.println(generateSuperSetter("this", "v") + ";");

      int dirtyGroup = getIndex() / 64;
      String dirtyVar = "__caucho_dirtyMask_" + dirtyGroup;

      long dirtyMask = 1L << (getIndex() % 64);

      out.println();
      out.println("long oldMask = " + dirtyVar + ";");
      out.println(dirtyVar + " |= " + dirtyMask + "L;");
      out.println();
      out.println("if (__caucho_session != null && oldMask == 0)");
      out.println("  __caucho_session.update(this);");
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates code to copy to an object.
   */
  public void generateCopy(JavaWriter out,
                           String dest,
                           String source)
    throws IOException
  {
    // XXX: how to make a new instance?

    String value = generateGet(source);
    
    out.println(generateSet(dest, value) + ";");
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(AmberTable table, String id)
  {
    if (getTable() != table)
      return null;
    else
      return generateSelect(id);
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < _subFields.size(); i++) {
      if (i > 0)
        sb.append(", ");

      sb.append(_subFields.get(i).generateSelect(id));
    }

    return sb.toString();
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < _subFields.size(); i++) {
      if (i > 0)
        sb.append(" and ");

      sb.append(_subFields.get(i).generateWhere(id));
    }

    return sb.toString();
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert) {
      for (int i = 0; i < _subFields.size(); i++) {
        _subFields.get(i).generateInsertColumns(columns);
      }
    }
  }

  /**
   * Generates the update set clause
   */
  public void generateUpdate(CharBuffer sql)
  {
    if (_isUpdate) {
      boolean isFirst = true;
      
      for (int i = 0; i < _subFields.size(); i++) {
        if (i > 0)
          sql.append(", ");

        _subFields.get(i).generateUpdate(sql);
      }
    }
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_isInsert)
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
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_isUpdate)
      generateStatementSet(out, pstmt, index, obj);
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    for (int i = 0; i < _subFields.size(); i++) {
      
    }
    /*
    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();

      String getter = _fieldNameByColumn.get(column.getName());

      EmbeddableType embeddableType = getEmbeddableType();

      if (! getSourceType().isFieldAccess())
        getter = "get" + Character.toUpperCase(getter.charAt(0)) +
          getter.substring(1) + "()";

      out.println("if (" + generateGet(obj) + " == null) {");
      out.pushDepth();

      // embeddableType.generateSetNull(out, pstmt, "index++");
      column.generateStatementSet(out, pstmt, index, null);

      out.popDepth();
      out.println("} else");
      out.pushDepth();
      column.generateStatementSet(out, pstmt, index, generateGet(obj)+"."+getter);
      out.popDepth();
    }
    */
  }

  /**
   * Generates get property.
   */
  public void generateGetPrimaryKey(CharBuffer cb)
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    /*
    String thisGetter = generateGet("this");

    EmbeddableType embeddableType = getEmbeddableType();

    ArrayList<AmberField> fields = embeddableType.getFields();
    for (int i = 0; i < fields.size(); i++) {
      if (i != 0)
        cb.append(", ");

      AmberField field = fields.get(i);

      String getter = field.getName();

      if (! getSourceType().isFieldAccess()) {
        getter = "get" + Character.toUpperCase(getter.charAt(0)) +
          getter.substring(1) + "()";
      }

      cb.append(thisGetter + "." + getter);
    }
    */
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    /*
    String var = "amber_ld_embedded" + index;

    out.print(getJavaTypeName());
    out.println(" " + var + " = new "+getJavaTypeName()+"();");
    */

    // jpa/0w01
    String value = (_embeddableType.getJavaTypeName()
                    + ".__caucho_make(aConn, rs, "
                    + indexVar + " + " + index + ")");

    // XXX: should cound
    index += _subFields.size();

    out.println(generateSuperSetter("this", value) + ";");

    // out.println("__caucho_loadMask |= " + (1L << getIndex()) + "L;");

    return index;
  }

  /**
   * Creates the expression for the field.
   */

  @Override
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new EmbeddedExpr(parent, _embeddableType, _subFields);
  }
}
