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

package com.caucho.jdbc;

import java.sql.Types;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Abstract way of grabbing data from the JDBC connection.
 */
public class OracleMetaData extends GenericMetaData {
  private static final Logger log
    = Logger.getLogger(OracleMetaData.class.getName());

  protected OracleMetaData(DataSource ds)
  {
    super(ds);
  }

  /**
   * Returns the literal for FALSE.
   */
  public String getFalseLiteral()
  {
    return "0";
  }

  /**
   * True if blobs must be truncated on delete.
   */
  public boolean isTruncateBlobBeforeDelete()
  {
    return true;
  }

  /**
   * Returns the SQL for the table with the given SQL type.
   */
  public String getCreateColumnSQL(int sqlType, int length, int precision, int scale)
  {
    // the oracle metadata doesn't return a proper value for data/time
    switch (sqlType) {
    case Types.DATE:
    case Types.TIME:
      return "DATE";
    case Types.DOUBLE:
      return "DOUBLE PRECISION";
    }

    return super.getCreateColumnSQL(sqlType, length, precision, scale);
  }

  /**
   * Returns true if the POSITION function is supported.
   */
  public boolean supportsPositionFunction()
  {
    return false;
  }

  /**
   * Returns true if sequences are supported.
   */
  public boolean supportsSequences()
  {
    return true;
  }

  /**
   * Returns true if table alias name with UPDATE is supported.
   */
  public boolean supportsUpdateTableAlias()
  {
    return true;
  }

  /**
   * Returns a sequence select expression.
   */
  public String createSequenceSQL(String name, int size)
  {
    if (size > 1)
      return "CREATE SEQUENCE " + name + " INCREMENT BY " + size;
    else
      return "CREATE SEQUENCE " + name;
  }

  /**
   * Returns a sequence select expression.
   */
  public String selectSequenceSQL(String name)
  {
    return "SELECT " + name + ".nextval FROM DUAL";
  }
}
