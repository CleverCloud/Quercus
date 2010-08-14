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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Configuration for a bean's field
 */
public class MaxGenerator extends Generator {
  private String _table;
  private String _column;

  private String _sql;

  /**
   * Sets the table.
   */
  public void setTable(String table)
  {
    _table = table;
  }

  /**
   * Sets the column.
   */
  public void setColumn(String column)
  {
    _column = column;
  }

  /**
   * initialize the sql.
   */
  public void init()
  {
    _sql = "SELECT MAX(" + _column + ") FROM " + _table;
  }
  
  /**
   * Generates the new id.
   */
  public long generate(Connection conn)
    throws SQLException
  {
    Statement stmt = conn.createStatement();

    try {
      ResultSet rs = stmt.executeQuery(_sql);

      rs.next();

      long value = rs.getLong(1);

      rs.close();

      return value;
    } finally {
      stmt.close();
    }
  }
}
