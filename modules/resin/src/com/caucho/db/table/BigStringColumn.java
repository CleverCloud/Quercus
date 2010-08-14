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

class BigStringColumn extends BlobColumn {
  private int _size;
  /**
   * Creates an inode column.
   *
   * @param columnOffset the offset within the row
   * @param maxLength the maximum length of the string
   */
  BigStringColumn(Row row, String name, int size)
  {
    super(row, name);

    _size = size;
  }

  /**
   * Returns the type code for the column.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.VARCHAR;
  }

  /**
   * Returns the java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return java.sql.Blob.class;
  }

  /**
   * Returns the declaration size
   */
  @Override
  public int getDeclarationSize()
  {
    return _size;
  }

  /**
   * Returns the column's size.
   */
  @Override
  public int getLength()
  {
    return 128;
  }

  public String toString()
  {
    return "BigStringColumn[" + getName() + "]";
  }
}
