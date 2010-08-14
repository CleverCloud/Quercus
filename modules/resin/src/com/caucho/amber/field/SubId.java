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

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
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
public class SubId extends Id {
  private static final L10N L = new L10N(SubId.class);
  private static final Logger log
    = Logger.getLogger(SubId.class.getName());

  private Id _parentId;
  private LinkColumns _link;

  public SubId(EntityType ownerType, EntityType rootType)
  {
    super(ownerType, new ArrayList<IdField>());

    _parentId = rootType.getId();
  }

  /**
   * Returns the parent keys.
   */
  public ArrayList<IdField> getParentKeys()
  {
    return _parentId.getKeys();
  }

  /**
   * Returns the keys.
   */
  public ArrayList<IdField> getKeys()
  {
    return _parentId.getKeys();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    return _parentId.getForeignTypeName();
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    return _parentId.generateLoadForeign(out, rs, indexVar, index);
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    return _parentId.generateLoadForeign(out, rs, indexVar, index, name);
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    ArrayList<IdField> keys = getParentKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(keys.get(i).generateSelect(id));
    }

    return cb.close();
  }

  /**
   * Generates the select clause.
   */
  @Override
  public String generateLoadSelect(String id)
  {
    return null;
  }

  /**
   * Returns the key for the value
   */
  @Override
  public String generateGet(String value)
  {
    return _parentId.generateGet(value);
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    _parentId.generateLoadFromObject(out, obj);
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateSet(JavaWriter out, String objThis, String value)
    throws IOException
  {
    _parentId.generateSet(out, objThis, value);
  }

  /**
   * Generates loading cache
   */
  @Override
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    _parentId.generateUpdateFromObject(out, obj);
  }

  /**
   * Generates the where clause.
   */
  @Override
  public String generateMatchArgWhere(String id)
  {
    ArrayList<IdField> keys = getParentKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      generateMatchArgWhere(cb, keys.get(i), id);
    }

    return cb.close();
  }


  /**
   * Generates the where clause.
   */
  private void generateMatchArgWhere(CharBuffer cb, IdField parentId, String id)
  {
    LinkColumns link = getOwnerType().getTable().getDependentIdLink();

    ArrayList<AmberColumn> columns = parentId.getColumns();

    for (int i = 0; i < columns.size(); i++) {
      AmberColumn column = columns.get(i);

      if (i != 0)
        cb.append(" and ");

      cb.append(id);
      cb.append('.');
      cb.append(link.getSourceColumn(column).getName());
      cb.append("=?");
    }
  }

  /**
   * Generates the where clause.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  public void generateSetKey(JavaWriter out, String pstmt,
                             String obj, String index)
    throws IOException
  {
    _parentId.generateSetKey(out, pstmt, obj, index);
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateSet(JavaWriter out, String pstmt,
                          String obj, String index)
    throws IOException
  {
    _parentId.generateSet(out, pstmt, obj, index);
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateStatementSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    _parentId.generateStatementSet(out, pstmt, index);
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    _parentId.generateSetInsert(out, pstmt, index);
  }

  /**
   * Generates code to convert to the type from the object.
   */
  @Override
  public String generateCastFromObject(String value)
  {
    return value;
  }

  /**
   * Generates code for a match.
   */
  @Override
  public void generateMatch(JavaWriter out, String key)
    throws IOException
  {
    // jpa/0l30
    out.println("return " + generateEquals("__caucho_getPrimaryKey()", key) + ";");
  }

  /**
   * Generates code to test the equals.
   */
  @Override
  public String generateEquals(String leftBase, String value)
  {
    return leftBase + ".equals(" + value + ")";
  }

  /**
   * Generates the set clause.
   */
  @Override
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates code to convert to the object.
   */
  @Override
  public String toObject(String value)
  {
    return value;
  }
}
