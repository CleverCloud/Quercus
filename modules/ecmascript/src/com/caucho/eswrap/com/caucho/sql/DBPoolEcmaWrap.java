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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 *
 * $Id: DBPoolEcmaWrap.java,v 1.2 2004/09/29 00:13:04 cvs Exp $
 */

package com.caucho.eswrap.com.caucho.sql;

import com.caucho.es.ESException;
import com.caucho.sql.DBPool;
import com.caucho.util.Exit;
import com.caucho.util.ExitListener;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBPoolEcmaWrap {
  public static DBPool call(String name)
    throws Exception
  {
    Context cxt = (Context) new InitialContext().lookup("java:comp/env");
    
    return (DBPool) cxt.lookup(name);
  }

  public static Connection getConnection(DBPool pool)
    throws SQLException
  {
    Connection conn = pool.getConnection();
    
    // Resin automatically reclaims connections when the script ends
    Exit.addExit(exitConnection, conn);

    return conn;
  }

  public static Statement createStatement(DBPool pool)
    throws SQLException
  {
    Connection conn = getConnection(pool);
    
    Statement stmt = conn.createStatement();
    
    // Resin automatically reclaims statements when the script ends
    Exit.addExit(exitStatement, stmt);

    return stmt;
  }

  public static int executeUpdate(DBPool pool, String msg)
    throws SQLException
  {
    Connection conn = pool.getConnection();

    try {
      Statement stmt = conn.createStatement();

      try {
        return stmt.executeUpdate(msg);
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  public static Object executeQuery(DBPool pool, String msg)
    throws ESException, SQLException
  {
    Connection conn = pool.getConnection();

    try {
      Statement stmt = conn.createStatement();
      ArrayList array = new ArrayList();

      try {
        ResultSet rs = stmt.executeQuery(msg);

        try {
          int i = 0;
          while (rs.next()) {
            // array.add(ResultSetEcmaWrap.toObject(rs, null));
          }
          return array.toArray(new Object[array.size()]);
        } finally {
          rs.close();
        }
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  private static ExitListener exitConnection = new ExitListener() {
    public void handleExit(Object o)
    {
      Connection conn = (Connection) o;

      try {
        conn.close();
      } catch (SQLException e) {
      }
    }
  };

  private static ExitListener exitStatement = new ExitListener() {
    public void handleExit(Object o)
    {
      Statement stmt = (Statement) o;

      try {
        stmt.close();
      } catch (SQLException e) {
      }
    }
  };
}
