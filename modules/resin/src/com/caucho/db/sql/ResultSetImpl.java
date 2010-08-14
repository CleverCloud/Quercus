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

package com.caucho.db.sql;

import java.sql.SQLException;

abstract public class ResultSetImpl {
  abstract public boolean next()
    throws SQLException;

  abstract public int findColumnIndex(String name)
    throws SQLException;

  abstract public String getString(int index)
    throws SQLException;

  abstract public int getInt(int index)
    throws SQLException;
  
  abstract public long getLong(int index)
    throws SQLException;
  
  abstract public double getDouble(int index)
    throws SQLException;
  
  abstract public long getDate(int index)
    throws SQLException;
  
  abstract public boolean wasNull()
    throws SQLException;

  abstract public void close()
    throws SQLException;
}
