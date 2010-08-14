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
 * @author Emil Ong
 */

package com.caucho.management.server;

import com.caucho.jmx.Description;

import java.sql.SQLException;

/**
 * MBean API to perform simple queries using a pooled JDBC driver.
 *
 * <pre>
 * resin:type=JdbcDriver,name=jdbc/resin,...
 * </pre>
 */
@Description("JDBC Query Access")
public interface JdbcQueryMXBean extends ManagedObjectMXBean {
  @Description("The driver URL")
  public String getUrl();

  @Description("performs a query")
  public JdbcQueryResult query(String sql)
    throws SQLException;

  @Description("lists the tables in the database")
  public String []listTables()
    throws SQLException;

  @Description("lists the columns in a table")
  public JdbcTableColumn []listColumns(String table)
    throws SQLException;
}
