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

import java.util.ArrayList;


/**
 * <table-generator> tag in orm.xml
 */
public class TableGeneratorConfig extends AbstractGeneratorConfig {

  // attributes
  private String _table;
  private String _catalog;
  private String _schema;
  private String _pkColumnName;
  private String _valueColumnName;
  private String _pkColumnValue;

  // elements
  private ArrayList<UniqueConstraintConfig> _uniqueConstraintList
    = new ArrayList<UniqueConstraintConfig>();

  public String getTable()
  {
    return _table;
  }

  public void setTable(String table)
  {
    _table = table;
  }

  public String getCatalog()
  {
    return _catalog;
  }

  public void setCatalog(String catalog)
  {
    _catalog = catalog;
  }

  public String getSchema()
  {
    return _schema;
  }

  public void setSchema(String schema)
  {
    _schema = schema;
  }

  public String getPkColumnName(String pkColumnName)
  {
    return _pkColumnName;
  }

  public void setPkColumnName(String pkColumnName)
  {
    _pkColumnName = pkColumnName;
  }

  public String getValueColumnName(String valueColumnName)
  {
    return _valueColumnName;
  }

  public void setValueColumnName(String valueColumnName)
  {
    _valueColumnName = valueColumnName;
  }

  public String getPkColumnValue(String pkColumnValue)
  {
    return _pkColumnValue;
  }

  public void setPkColumnValue(String pkColumnValue)
  {
    _pkColumnValue = pkColumnValue;
  }

  public void addUniqueConstraint(UniqueConstraintConfig uniqueConstraint)
  {
    _uniqueConstraintList.add(uniqueConstraint);
  }

  public ArrayList<UniqueConstraintConfig> getUniqueConstraintList()
  {
    return _uniqueConstraintList;
  }
}
