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

import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents the sub-field of an embedded type.
 */
public class KeyEmbeddedSubField extends EmbeddedSubField implements IdField
{
  public KeyEmbeddedSubField(EntityEmbeddedField embeddedField,
                             AmberField embeddableField,
                             int index)
    throws ConfigException
  {
    super(embeddedField, embeddableField, index);
  }
  /**
   * Returns the columns
   */
  public ArrayList<AmberColumn> getColumns()
  {
    ArrayList<AmberColumn> columns = new ArrayList<AmberColumn>();

    columns.add(getColumn());

    return columns;
  }

  /**
   * Returns the column type
   */
  public AmberType getType()
  {
    return getColumn().getType();
  }

  /**
   * Returns true for a generator.
   */
  public boolean isAutoGenerate()
  {
    // XXX: can an @EmbeddedId have a @Generated field?
    return false;
  }

  /**
   * Sets true if there are multiple keys.
   */
  public void setKeyField(boolean isKey)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    return getColumn().getType().getForeignTypeName();
  }

  /**
   * Returns the component count.
   */
  public int getComponentCount()
  {
    return 1;
  }

  /**
   * Returns the generator.
   */
  public String getGenerator()
  {
    return null;
  }

  /**
   * Generates code to copy to an object.
   */
  public void generateCopy(JavaWriter out,
                           String dest,
                           String source)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the setter for a key property
   */
  public String generateSetKeyProperty(String key, String value)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the getter for a key property
   */
  public String generateGetKeyProperty(String key)
    throws IOException
  {
    throw new UnsupportedOperationException();
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
   * Generates the set clause.
   */
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {

  }

  /**
   * Generates the set for an insert.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the set for an insert.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    // XXX: autogenerate stuff, see KeyPropertyField
    
    generateStatementSet(out, pstmt, index);
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the where code
   */
  public String generateRawWhere(String id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the key code
   */
  public String generateMatchArgWhere(String id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts from an object.
   */
  public String toValue(String value)
  {
    return getColumn().getType().generateCastFromObject(value);
  }
}
