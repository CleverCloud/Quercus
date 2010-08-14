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

import com.caucho.db.table.Table;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a table binding for a from.
 */
public class FromItem {
  private Table _table;
  private String _name;

  private FromItem _dependTable;

  FromItem(Table table, String name)
  {
    _table = table;
    _name = name;
  }

  /**
   * Returns the FROM item's table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns the FROM item's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets a depend table.
   */
  void setDependTable(FromItem table)
  {
    _dependTable = table;
  }

  /**
   * Returns true if the given table order is valid.
   */
  boolean isValid(ArrayList<FromItem> tables)
  {
    if (_dependTable == null)
      return true;
    else
      return ! tables.contains(this) || tables.contains(_dependTable);
  }

  /**
   * Printable version of the FROM item.
   */
  public String toString()
  {
    return "FromItem[" + getTable().getName() + " AS " + _name + "]";
  }
}
