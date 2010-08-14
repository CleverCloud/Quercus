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

package com.caucho.db.table;

import com.caucho.db.Database;
import com.caucho.db.sql.Expr;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class TableFactory {
  private static final L10N L = new L10N(TableFactory.class);

  private Database _database;

  private String _name;
  private Row _row;

  private ArrayList<Constraint> _constraints = new ArrayList<Constraint>();

  public TableFactory(Database database)
  {
    _database = database;
  }

  /**
   * Returns the table name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the row.
   */
  public Row getRow()
  {
    return _row;
  }

  /**
   * Creates a table.
   */
  public void startTable(String name)
  {
    _name = name;
    _row = new Row();
  }

  /**
   * Adds a varchar
   */
  public Column addTinytext(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new StringColumn(_row, name, 256));
  }

  /**
   * Adds a varchar
   */
  public Column addVarchar(String name, int size)
  {
    _row.allocateColumn();

    if (size <= 128)
      return _row.addColumn(new StringColumn(_row, name, size));
    else
      return _row.addColumn(new BigStringColumn(_row, name, size));
  }

  /**
   * Adds a binary
   */
  public Column addVarbinary(String name, int size)
  {
    _row.allocateColumn();

    if (size <= 128)
      return _row.addColumn(new VarBinaryColumn(_row, name, size));
    else
      return _row.addColumn(new BlobColumn(_row, name));
  }

  /**
   * Adds a binary
   */
  public Column addBinary(String name, int size)
  {
    _row.allocateColumn();

    if (size <= 128)
      return _row.addColumn(new BinaryColumn(_row, name, size));
    else
      return _row.addColumn(new BlobColumn(_row, name));
  }

  /**
   * Adds a blob
   */
  public Column addBlob(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new BlobColumn(_row, name));
  }

  /**
   * Adds a short
   */
  public Column addShort(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new ShortColumn(_row, name));
  }

  /**
   * Adds an integer
   */
  public Column addInteger(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new IntColumn(_row, name));
  }

  /**
   * Adds a long
   */
  public Column addLong(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new LongColumn(_row, name));
  }

  /**
   * Adds the identity column
   */
  public Column addIdentity(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new IdentityColumn(_row, name));
  }

  /**
   * Adds a double
   */
  public Column addDouble(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new DoubleColumn(_row, name));
  }

  /**
   * Adds a datetime column
   */
  public Column addDateTime(String name)
  {
    _row.allocateColumn();

    return _row.addColumn(new DateColumn(_row, name));
  }

  /**
   * Adds a numeric
   */
  public Column addNumeric(String name, int precision, int scale)
  {
    _row.allocateColumn();

    return _row.addColumn(new NumericColumn(_row, name, precision, scale));
  }

  /**
   * Sets the named column as a primary key constraint.
   */
  public void setPrimaryKey(String name)
    throws SQLException
  {
    Column column = _row.getColumn(name);

    if (column == null) {
      throw new SQLException(L.l("'{0}' is not a valid column for primary key",
                                 name));
    }

    column.setUnique();
    column.setNotNull();

    //addConstraint(new PrimaryKeySingleColumnConstraint(column));
  }

  /**
   * Sets the named column as not null
   */
  public void setNotNull(String name)
    throws SQLException
  {
    Column column = _row.getColumn(name);

    if (column == null)
      throw new SQLException(L.l("'{0}' is not a valid column for NOT NULL",
                                 name));

    column.setNotNull();
  }

  /**
   * Sets the column default
   */
  public void setDefault(String name, Expr expr)
    throws SQLException
  {
    Column column = _row.getColumn(name);

    if (column == null)
      throw new SQLException(L.l("'{0}' is not a valid column for DEFAULT",
                                  name));

    column.setDefault(expr);
  }

  /**
   * Sets the named column as unique
   */
  public void setUnique(String name)
    throws SQLException
  {
    Column column = _row.getColumn(name);

    if (column == null)
      throw new SQLException(L.l("'{0}' is not a valid column for NOT NULL",
                                 name));

    column.setUnique();

    // already checked by unique
    //addConstraint(new UniqueSingleColumnConstraint(column));
  }

  /**
   * Sets the named column as auto-increment
   */
  public void setAutoIncrement(String name, int min)
    throws SQLException
  {
    Column column = _row.getColumn(name);

    if (column == null)
      throw new SQLException(L.l("'{0}' is not a valid column for auto_increment",
                                 name));

    column.setAutoIncrement(min);
  }


  /**
   * Sets the array of columns as unique
   */
  public void addUnique(ArrayList<String> names)
    throws SQLException
  {
    if (names.size() == 1) {
      setUnique(names.get(0));
      return;
    }

    ArrayList<Column> columns = new ArrayList<Column>();

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);

      Column column = _row.getColumn(name);

      if (column == null)
        throw new SQLException(L.l("`{0}' is not a valid column for UNIQUE",
                                   name));
    }

    Column []columnArray = new Column[columns.size()];
    columns.toArray(columnArray);

    if (columnArray.length == 1) {
      columnArray[0].setUnique();

      //addConstraint(new UniqueSingleColumnConstraint(columnArray[0]));
    }
    else
      addConstraint(new UniqueConstraint(columnArray));
  }

  /**
   * Sets the array of columns as the primary key
   */
  public void addPrimaryKey(ArrayList<String> names)
    throws SQLException
  {
    if (names.size() == 1) {
      setPrimaryKey(names.get(0));
      return;
    }

    ArrayList<Column> columns = new ArrayList<Column>();

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);

      Column column = _row.getColumn(name);

      if (column == null)
        throw new SQLException(L.l("`{0}' is not a valid column for PRIMARY KEY",
                                   name));
    }

    Column []columnArray = new Column[columns.size()];
    columns.toArray(columnArray);

    if (columnArray.length == 1) {
      columnArray[0].setPrimaryKey(true);
      //addConstraint(new PrimaryKeySingleColumnConstraint(columnArray[0]));
    }
    else
      addConstraint(new PrimaryKeyConstraint(columnArray));
  }

  /**
   * Adds a constraint.
   */
  public void addConstraint(Constraint constraint)
  {
    _constraints.add(constraint);
  }

  /**
   * Returns the constraints.
   */
  public Constraint []getConstraints()
  {
    Constraint []constraints = new Constraint[_constraints.size()];
    _constraints.toArray(constraints);

    return constraints;
  }

  /**
   * Creates the table.
   */
  public void create()
    throws java.sql.SQLException, IOException
  {
    Table table = new Table(_database, _name, _row, getConstraints());

    table.create();
  }
}
