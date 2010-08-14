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
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.EmbeddableType;
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
public class EmbeddedIdField extends EntityEmbeddedField implements IdField
{
  private static final L10N L = new L10N(EmbeddedIdField.class);
  private static final Logger log
    = Logger.getLogger(EmbeddedIdField.class.getName());

  boolean _isKeyField;

  public EmbeddedIdField(EntityType ownerType,
                         EmbeddableType embeddableType)
  {
    super(ownerType, embeddableType);
  }

  public EmbeddedIdField(EntityType ownerType,
                         EmbeddableType embeddableType,
                         String name)
    throws ConfigException
  {
    super(ownerType, embeddableType, name);
  }

  @Override
  protected EmbeddedSubField createSubField(AmberField field, int index)
  {
    return new KeyEmbeddedSubField(this, field, index);
  }

  /**
   * Returns the columns
   */
  public ArrayList<AmberColumn> getColumns()
  {
    return null;
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
   * Returns the component count.
   */
  public int getComponentCount()
  {
    return 1;
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    return null;
  }

  /**
   * Returns the generator.
   */
  public String getGenerator()
  {
    return null;
  }

  /**
   * Generates the setter for a key property
   */
  public String generateSetKeyProperty(String key, String value)
    throws IOException
  {
    return null;
  }

  /**
   * Generates the getter for a key property
   */
  public String generateGetKeyProperty(String key)
    throws IOException
  {
    return null;
  }

  /**
   * Generates the property getter for an EJB proxy
   *
   * @param value the non-null value
   */
  public String generateGetProxyProperty(String value)
  {
    return null;
  }

  /**
   * Generates any prologue.
   */
  @Override
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
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return generateWhere(id);
  }

  /**
   * Returns the where code
   */
  public String generateRawWhere(String id)
  {
    return id + "." + getName() + "=?";
  }

  /**
   * Generates loading code
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    return 0;
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
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(AmberTable table, String id)
  {
    return null;
  }

  /**
   * Generates loading cache
   */
  public String generateSetNull(String obj)
    throws IOException
  {
    return null;
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    return  null;
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
    return 0;
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    super.generateStatementSet(out, pstmt, index, value);
  }

  /**
   * Generates code for a match.
   */
  public void generateMatch(JavaWriter out, String key)
    throws IOException
  {
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String left, String right)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    generateStatementSet(out, pstmt, index);
  }

  /**
   * Generates the set clause.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return null;
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return null;
  }

  /**
   * Converts from an object.
   */
  public String toValue(String value)
  {
    return null;
  }
}
