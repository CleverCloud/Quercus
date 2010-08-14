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

package com.caucho.jstl;

import javax.servlet.jsp.jstl.sql.Result;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Returns the result of a SQL query.
 */
public class ResultImpl implements Result {
  private ArrayList _rows = new ArrayList();
  private Object [][]_objectRows;
  private SortedMap []_sortedRows;
  private String []_columnNames;
  private boolean _isLimitedByMaxRows;

  public ResultImpl(ResultSet rs, int maxRows)
    throws SQLException
  {
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();

    _columnNames = new String[columnCount];
    for (int i = 0; i < _columnNames.length; i++) {
      _columnNames[i] = metaData.getColumnName(i + 1);
    }

    // _isLimitedByMaxRows = maxRows >= 0;

    if (maxRows < 0)
      maxRows = Integer.MAX_VALUE;

    for (; rs.next() && maxRows > 0; maxRows--) {
      Object []row = new Object[columnCount];

      for (int i = 0; i < columnCount; i++) {
        row[i] = rs.getObject(i + 1);
      }

      _rows.add(row);
    }
    
    if (maxRows == 0)
      _isLimitedByMaxRows = true;
  }

  public SortedMap[] getRows()
  {
    if (_sortedRows == null) {
      _sortedRows = new SortedMap[_rows.size()];

      for (int i = _rows.size() - 1; i >=0; i--) {
        SortedMap map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        _sortedRows[i] = map;
        Object []row = (Object []) _rows.get(i);

        for (int j = _columnNames.length - 1; j >= 0; j--)
          map.put(_columnNames[j], row[j]);
      }
    }

    return _sortedRows;
  }

  public Object[][] getRowsByIndex()
  {
    if (_objectRows == null) {
      _objectRows = new Object[_rows.size()][];
      _rows.toArray(_objectRows);
    }

    return _objectRows;
  }

  public String []getColumnNames()
  {
    return _columnNames;
  }

  public int getRowCount()
  {
    return _rows.size();
  }

  public boolean isLimitedByMaxRows()
  {
    return _isLimitedByMaxRows;
  }
}
