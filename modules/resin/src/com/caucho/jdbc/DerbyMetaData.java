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
 * @author Sam
 */

package com.caucho.jdbc;

import javax.sql.DataSource;

/**
 * Metadata for the Apache Derby database.
 */
public class DerbyMetaData
  extends GenericMetaData
{
  protected DerbyMetaData(DataSource ds)
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
   * Returns true if the POSITION function is supported.
   */
  public boolean supportsPositionFunction()
  {
    return false;
  }

  /**
   * True if the generated keys is supported
   */
  public boolean supportsGetGeneratedKeys()
  {
    return true;
  }

  public boolean supportsIdentity()
  {
    return true;
  }

  public String createIdentitySQL(String sqlType)
  {
    return sqlType + " GENERATED ALWAYS AS IDENTITY";
  }

  public boolean supportsSequences()
  {
    // http://issues.apache.org/jira/browse/DERBY-712
    return false;
  }

  public String selectSequenceSQL(String name)
  {
    throw new UnsupportedOperationException();
  }

  public String createSequenceSQL(String name, int size)
  {
    throw new UnsupportedOperationException();
  }
}
