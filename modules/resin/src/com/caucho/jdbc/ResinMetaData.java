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
 */

package com.caucho.jdbc;

import com.caucho.util.Log;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Metadata for the Resin database.
 */
public class ResinMetaData extends GenericMetaData {
  private static final Logger log = Log.open(ResinMetaData.class);

  protected ResinMetaData(DataSource ds)
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
   * Returns the blob type.
   */
  public String getBlobType()
  {
    return "BLOB";
  }

  /**
   * Returns the long type.
   */
  public String getLongType()
  {
    return "BIGINT";
  }

  /**
   * Returns true if identity is supported.
   */
  public boolean supportsIdentity()
  {
    return true;
  }

  /**
   * Returns true if the POSITION function is supported.
   */
  public boolean supportsPositionFunction()
  {
    return false;
  }

  /**
   * Returns the identity property
   */
  public String createIdentitySQL(String sqlType)
  {
    return sqlType + " auto_increment";
  }

  @Override
  public boolean isLimit()
  {
    return true;
  }

  @Override
  public boolean isLimitOffset()
  {
    return true;
  }

  /**
   * Returns a limit.
   */
  @Override
  public String limit(String sql, int offset, int limit)
  {
    if (limit < 0)
      return sql;
    else if (offset <= 0)
      return sql + " LIMIT " + limit;
    else
      return sql + " OFFSET " + offset + " LIMIT " + limit;
  }
}
