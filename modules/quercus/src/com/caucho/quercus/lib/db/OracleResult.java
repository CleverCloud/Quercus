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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.util.L10N;

import com.caucho.quercus.env.Env;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.logging.Logger;


/**
 * oracle result set class (postgres has NO object oriented API)
 */
public class OracleResult extends JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(OracleResult.class.getName());
  private static final L10N L = new L10N(OracleResult.class);

  /**
   * Constructor for OracleResult
   *
   * @param stmt the corresponding statement
   * @param rs the corresponding result set
   * @param conn the corresponding connection
   */
  public OracleResult(Env env,
                      Statement stmt,
                      ResultSet rs,
                      Oracle conn)
  {
    super(env, stmt, rs, conn);
  }

  /**
   * Constructor for OracleResult
   *
   * @param metaData the corresponding result set meta data
   * @param conn the corresponding connection
   */
  public OracleResult(Env env,
                      ResultSetMetaData metaData,
                      Oracle conn)
  {
    super(env, metaData, conn);
  }
}
