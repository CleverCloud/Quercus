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
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class CollectionField extends CascadableField {
  private static final L10N L = new L10N(CollectionField.class);
  private static final Logger log
    = Logger.getLogger(CollectionField.class.getName());

  private AmberType _targetType;

  private LinkColumns _linkColumns;

  private String _table;

  public CollectionField(EntityType relatedType,
                         String name,
                         CascadeType[] cascadeTypes)
    throws ConfigException
  {
    super(relatedType, name, cascadeTypes);
  }

  public CollectionField(EntityType relatedType)
  {
    super(relatedType);
  }

  /**
   * Sets the collection table.
   */
  public void setTable(String table)
  {
    _table = table;
  }

  /**
   * Gets the collection table.
   */
  public String getTableName()
  {
    return _table;
  }

  /**
   * Sets the target type.
   */
  public void setType(AmberType targetType)
  {
    _targetType = targetType;
  }

  /**
   * Returns the target type.
   */
  public AmberType getTargetType()
  {
    return _targetType;
  }

  /**
   * Sets the key columns.
   */
  public void setLinkColumns(LinkColumns linkColumns)
  {
    _linkColumns = linkColumns;
  }

  /**
   * Gets the key columns.
   */
  public LinkColumns getLinkColumns()
  {
    return _linkColumns;
  }

  /**
   * Generates the (pre) cascade operation from
   * parent to this child. This field will only
   * be cascaded first if the operation can be
   * performed with no risk to break FK constraints.
   */
  public void generatePreCascade(JavaWriter out,
                                 String aConn,
                                 CascadeType cascadeType)
    throws IOException
  {
    if (isCascade(cascadeType)) {

      String getter = "_caucho_field_" + getGetterName(); // generateSuperGetterMethod();

      out.println("if (" + getter + " != null) {");
      out.pushDepth();

      out.println("for (Object o : " + getter + ") {");
      out.pushDepth();

      // jpa/1622
      if (cascadeType == CascadeType.REMOVE) {
        // jpa/0i60
        out.println("com.caucho.amber.entity.Entity child = (com.caucho.amber.entity.Entity) o;");
        out.println();

        out.println("if (! child.__caucho_getEntityState().isTransactional())");
        out.println("  continue;");
        out.println();
      }
      // else {
      //   out.println("if (com.caucho.amber.entity.EntityState.P_DELETING.ordinal() <= child.__caucho_getEntityState().ordinal())");
      //   out.println("  continue;");
      // }

      out.print(aConn + ".");

      switch (cascadeType) {
      case PERSIST:
        out.print("persistFromCascade");
        break;

      case MERGE:
        out.print("merge");
        break;

      case REMOVE:
        out.print("remove");
        break;

      case REFRESH:
        out.print("refresh");
        break;
      }

      out.println("(o);");

      out.popDepth();
      out.println("}");

      out.popDepth();
      out.println("}");
    }
  }

  /**
   * Generates the (post) cascade operation from
   * parent to this child. This field will only
   * be cascaded first if the operation can be
   * performed with no risk to break FK constraints.
   */
  public void generatePostCascade(JavaWriter out,
                                  String aConn,
                                  CascadeType cascadeType)
    throws IOException
  {
  }

  /**
   * Generates the set clause.
   */
  public void generateStatementSet(JavaWriter out, String pstmt,
                          String obj, String index)
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
    String maskVar = mask + "_" + (getIndex() / 64);
    long maskValue = (1L << (getIndex() % 64));

    out.println();
    out.println("if ((" + maskVar + " & " + maskValue + "L) != 0) {");
    out.pushDepth();

    generateStatementSet(out, pstmt, index);

    out.popDepth();
    out.println("}");
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(String id)
  {
    return null;
  }

  /**
   * Generates the target select.
   */
  public String generateTargetSelect(String id)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the linking for a join
   */
  public String generateJoin(String sourceTable, String targetTable)
  {
    return _linkColumns.generateJoin(sourceTable, targetTable);
  }

  /**
   * Returns the source column for a given target key.
   */
  public ForeignColumn getSourceColumn(AmberColumn key)
  {
    return _linkColumns.getSourceColumn(key);
  }
}
