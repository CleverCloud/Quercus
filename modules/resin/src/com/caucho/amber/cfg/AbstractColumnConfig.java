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

package com.caucho.amber.cfg;

/**
 * Base class for <column> and <join-column> tag in the orm.xml
 */
abstract public class AbstractColumnConfig {
  // attributes
  private String _name = "";
  private boolean _isUnique;
  private boolean _isNullable = true;
  private boolean _isInsertable = true;
  private boolean _isUpdatable = true;
  private String _columnDefinition = "";
  private String _table = "";

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public boolean isUnique()
  {
    return _isUnique;
  }

  public boolean isNullable()
  {
    return _isNullable;
  }

  public boolean isInsertable()
  {
    return _isInsertable;
  }

  public boolean isUpdatable()
  {
    return _isUpdatable;
  }

  public void setUnique(boolean isUnique)
  {
    _isUnique = isUnique;
  }

  public void setNullable(boolean isNullable)
  {
    _isNullable = isNullable;
  }

  public void setInsertable(boolean isInsertable)
  {
    _isInsertable = isInsertable;
  }

  public void setUpdatable(boolean isUpdatable)
  {
    _isUpdatable = isUpdatable;
  }

  /**
   * Returns the column definition.
   */
  public String getColumnDefinition()
  {
    return _columnDefinition;
  }

  /**
   * Sets the column definition.
   */
  public void setColumnDefinition(String columnDefinition)
  {
    _columnDefinition = columnDefinition;
  }

  /**
   * Returns the table.
   */
  public String getTable()
  {
    return _table;
  }

  /**
   * Sets the table.
   */
  public void setTable(String table)
  {
    _table = table;
  }
}
