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

package com.caucho.quercus.mysql;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql column
 */
public class MysqlColumn {
  private static final int TYPE_DECIMAL = 0x0;
  private static final int TYPE_TINY = 0x1;
  private static final int TYPE_SHORT = 0x2;
  private static final int TYPE_INTEGER = 0x3;
  private static final int TYPE_FLOAT = 0x4;
  private static final int TYPE_DOUBLE = 0x5;
  private static final int TYPE_NULL = 0x6;
  private static final int TYPE_TIMESTAMP = 0x7;
  private static final int TYPE_BIGINT = 0x8;
  private static final int TYPE_INT24 = 0x9;
  private static final int TYPE_DATE = 0xa;
  private static final int TYPE_TIME = 0xb;
  private static final int TYPE_DATETIME = 0xc;
  private static final int TYPE_YEAR = 0xd;
  private static final int TYPE_NEWDATE = 0xe;
  private static final int TYPE_VARCHAR = 0xf;
  private static final int TYPE_BIT = 0x10;
  private static final int TYPE_NEWDECIMAL = 0xf6;
  private static final int TYPE_ENUM = 0xf7;
  private static final int TYPE_SET = 0xf8;
  private static final int TYPE_TINY_BLOB = 0xf9;
  private static final int TYPE_MEDIUM_BLOB = 0xfa;
  private static final int TYPE_LONG_BLOB = 0xfb;
  private static final int TYPE_BLOB = 0xfc;
  private static final int TYPE_VAR_STRING = 0xfd;
  private static final int TYPE_STRING = 0xfe;
  private static final int TYPE_GEOMETRY = 0xff;

  private static final int FLAG_NOT_NULL = 0x0001;
  private static final int FLAG_PRIMARY_KEY = 0x0002;
  private static final int FLAG_UNIQUE_KEY = 0x0004;
  private static final int FLAG_MULTIPLE_KEY = 0x0008;
  private static final int FLAG_BLOB = 0x0010;
  private static final int FLAG_UNSIGNED = 0x0020;
  private static final int FLAG_ZEROFILL = 0x0040;
  private static final int FLAG_BINARY = 0x0080;
  private static final int FLAG_ENUM = 0x0100;
  private static final int FLAG_AUTO_INCREMENT = 0x0200;
  private static final int FLAG_TIMESTAMP = 0x0400;
  private static final int FLAG_SET = 0x0800;

  private char []_catalogBuffer = new char[32];
  private int _catalogLength;
  private String _catalog;

  private char []_databaseBuffer = new char[32];
  private int _databaseLength;
  private String _database;

  private char []_tableBuffer = new char[32];
  private int _tableLength;
  private String _table;

  private char []_origTableBuffer = new char[32];
  private int _origTableLength;
  private String _origTable;

  private char []_nameBuffer = new char[32];
  private int _nameLength;
  private String _name;

  private char []_origNameBuffer = new char[32];
  private int _origNameLength;
  private String _origName;

  private int _charset;
  private int _length;
  private int _type;
  private int _flags;
  private int _decimals;
  private long _defaultValue;

  private int _rowOffset;
  private int _rowLength;

  char []startCatalog(int length)
  {
    if (_catalogBuffer.length < length) {
      _catalogBuffer = new char[length];
    }

    _catalog = null;
    _catalogLength = length;

    return _catalogBuffer;
  }

  String getCatalog()
  {
    if (_catalog == null)
      _catalog = new String(_catalogBuffer, 0, _catalogLength);

    return _catalog;
  }

  char []startDatabase(int length)
  {
    if (_databaseBuffer.length < length) {
      _databaseBuffer = new char[length];
    }

    _database = null;
    _databaseLength = length;

    return _databaseBuffer;
  }

  String getDatabase()
  {
    if (_database == null)
      _database = new String(_databaseBuffer, 0, _databaseLength);

    return _database;
  }

  char []startTable(int length)
  {
    if (_tableBuffer.length < length) {
      _tableBuffer = new char[length];
    }

    _table = null;
    _tableLength = length;

    return _tableBuffer;
  }

  String getTable()
  {
    if (_table == null)
      _table = new String(_tableBuffer, 0, _tableLength);

    return _table;
  }

  char []startOrigTable(int length)
  {
    if (_origTableBuffer.length < length) {
      _origTableBuffer = new char[length];
    }

    _origTable = null;
    _origTableLength = length;

    return _origTableBuffer;
  }

  String getOrigTable()
  {
    if (_origTable == null)
      _origTable = new String(_origTableBuffer, 0, _origTableLength);

    return _origTable;
  }

  char []startName(int length)
  {
    if (_nameBuffer.length < length) {
      _nameBuffer = new char[length];
    }

    _name = null;
    _nameLength = length;

    return _nameBuffer;
  }

  String getName()
  {
    if (_name == null)
      _name = new String(_nameBuffer, 0, _nameLength);

    return _name;
  }

  char []startOrigName(int length)
  {
    if (_origNameBuffer.length < length) {
      _origNameBuffer = new char[length];
    }

    _origName = null;
    _origNameLength = length;

    return _origNameBuffer;
  }

  String getOrigName()
  {
    if (_origName == null)
      _origName = new String(_origNameBuffer, 0, _origNameLength);

    return _origName;
  }

  public void setCharset(int charset)
  {
    _charset = charset;
  }

  public int getCharset()
  {
    return _charset;
  }

  public void setLength(int length)
  {
    _length = length;
  }

  public int getLength()
  {
    return _length;
  }

  public void setType(int type)
  {
    _type = type;
  }

  public int getType()
  {
    return _type;
  }

  public void setFlags(int flags)
  {
    _flags = flags;
  }

  public int getFlags()
  {
    return _flags;
  }

  public void setDecimals(int decimals)
  {
    _decimals = decimals;
  }

  public int getDecimals()
  {
    return _decimals;
  }

  public void setDefault(long value)
  {
    _defaultValue = value;
  }

  public long getDefault()
  {
    return _defaultValue;
  }

  public String getSchema()
  {
    return getTable();
  }

  public int getSQLType()
  {
    switch (_type) {
    case TYPE_DECIMAL:
      return Types.DECIMAL;

    case TYPE_TINY:
      return Types.TINYINT;

    case TYPE_SHORT:
      return Types.SMALLINT;

    case TYPE_INTEGER:
      return Types.INTEGER;

    case TYPE_FLOAT:
      return Types.FLOAT;

    case TYPE_DOUBLE:
      return Types.DOUBLE;

    case TYPE_NULL:
      return Types.NULL;

    case TYPE_TIMESTAMP:
      return Types.TIMESTAMP;

    case TYPE_BIGINT:
      return Types.BIGINT;

    case TYPE_INT24:
      return Types.INTEGER;

    case TYPE_DATE:
      return Types.DATE;

    case TYPE_TIME:
      return Types.TIME;

    case TYPE_DATETIME:
      return Types.TIMESTAMP;

    case TYPE_YEAR:
      return Types.DATE;

    case TYPE_NEWDATE:
      return Types.DATE;

    case TYPE_BIT:
      return Types.BIT;

    case TYPE_ENUM:
      return Types.DISTINCT;

    case TYPE_TINY_BLOB:
      return Types.VARCHAR;

    case TYPE_MEDIUM_BLOB:
      return Types.VARCHAR;

    case TYPE_LONG_BLOB:
      return Types.VARCHAR;

    case TYPE_BLOB:
      return Types.VARCHAR;

    case TYPE_VAR_STRING:
      return Types.VARCHAR;

    case TYPE_STRING:
      return Types.CHAR;

    default:
      return 0;
    }
  }

  public String getTypeName()
  {
    switch (_type) {
    case TYPE_DECIMAL:
      return "decimal";

    case TYPE_TINY:
      return "tinyint";

    case TYPE_SHORT:
      return "smallint";

    case TYPE_INTEGER:
      return "integer";

    case TYPE_FLOAT:
      return "float";

    case TYPE_DOUBLE:
      return "double";

    case TYPE_NULL:
      return "null";

    case TYPE_TIMESTAMP:
      return "timestamp";

    case TYPE_BIGINT:
      return "bigint";

    case TYPE_INT24:
      return "mediumint";

    case TYPE_DATE:
      return "date";

    case TYPE_TIME:
      return "time";

    case TYPE_DATETIME:
      return "datetime";

    case TYPE_YEAR:
      return "year";

    case TYPE_NEWDATE:
      return "newdate";

    case TYPE_BIT:
      return "bit";

    case TYPE_ENUM:
      return "enum";

    case TYPE_SET:
      return "set";

    case TYPE_TINY_BLOB:
      return "tinyblob";

    case TYPE_MEDIUM_BLOB:
      return "mediumblob";

    case TYPE_LONG_BLOB:
      return "longblob";

    case TYPE_BLOB:
      return "blob";

    case TYPE_VAR_STRING:
      return "varchar";

    case TYPE_STRING:
      return "string";

    default:
      return "unknown(" + _type + ")";
    }
  }

  public int getPrecision()
  {
    return getLength();
  }

  public int getScale()
  {
    return _decimals;
  }

  public boolean isNotNull()
  {
    return (_flags & FLAG_NOT_NULL) != 0;
  }

  public boolean isPrimaryKey()
  {
    return (_flags & FLAG_PRIMARY_KEY) != 0;
  }

  public boolean isUniqueKey()
  {
    return (_flags & FLAG_UNIQUE_KEY) != 0;
  }

  public boolean isMultipleKey()
  {
    return (_flags & FLAG_MULTIPLE_KEY) != 0;
  }

  public boolean isBlob()
  {
    return (_flags & FLAG_BLOB) != 0;
  }

  public boolean isUnsigned()
  {
    return (_flags & FLAG_UNSIGNED) != 0;
  }

  public boolean isZeroFill()
  {
    return (_flags & FLAG_ZEROFILL) != 0;
  }

  public boolean isBinary()
  {
    return (_flags & FLAG_BINARY) != 0;
  }

  public boolean isEnum()
  {
    return (_flags & FLAG_ENUM) != 0;
  }

  public boolean isAutoIncrement()
  {
    return (_flags & FLAG_AUTO_INCREMENT) != 0;
  }

  public boolean isTimestamp()
  {
    return (_flags & FLAG_TIMESTAMP) != 0;
  }

  public boolean isSet()
  {
    return (_flags & FLAG_SET) != 0;
  }

  //
  // row offsets
  //

  public int getRowOffset()
  {
    return _rowOffset;
  }

  public void setRowOffset(int offset)
  {
    _rowOffset = offset;
  }

  public int getRowLength()
  {
    return _rowLength;
  }

  public void setRowLength(int length)
  {
    _rowLength = length;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
