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

import com.caucho.quercus.lib.db.QuercusConnection;
 import com.caucho.util.*;
 import com.caucho.vfs.*;

 import java.io.*;
 import java.net.*;
 import java.sql.*;
 import java.util.*;
 import java.util.logging.*;
 import javax.sql.*;

 /**
  * Special Quercus Mysql connection.
  */
public class MysqlConnectionImpl implements QuercusConnection {
  private static final Logger log
    = Logger.getLogger(MysqlConnectionImpl.class.getName());
  private static final L10N L = new L10N(MysqlConnectionImpl.class);

  private static final int CLIENT_LONG_PASSWORD = 1;
  private static final int CLIENT_FOUND_ROWS = 2;
  private static final int CLIENT_LONG_FLAG = 4;
  private static final int CLIENT_CONNECT_WITH_DB = 8;
  private static final int CLIENT_COMPRESS = 32;
  private static final int CLIENT_PROTOCOL_41 = 512;
  private static final int CLIENT_SSL = 2048;
  private static final int CLIENT_SECURE_CONNECTION = 32768;
  private static final int CLIENT_MULTI_STATEMENTS = 65536;
  private static final int CLIENT_MULTI_RESULTS = 131072;

  private static final int UTF8_MB3 = 33;

  private static final int COM_QUERY = 0x03;

  private QuercusMysqlDriver _driver;

  private String _host;
  private int _port;
  private String _database;

  private Socket _s;
  private MysqlReader _in;
  private MysqlWriter _out;

  private int _serverCapabilities;
  private int _serverLanguage;
  private byte []_scrambleBuf = new byte[8 + 13];
  private byte []_errorState = new byte[5];

  private String _catalog;

  private State _state = State.IDLE;

  enum State {
    IDLE,
    FIELD_HEADER,
    FIELD_DATA,
  };

  MysqlConnectionImpl(QuercusMysqlDriver driver,
                      String url,
                      Properties info)
    throws SQLException
  {
    if (driver == null)
      throw new NullPointerException();

    _driver = driver;

    _host = driver.getHost();
    _port = driver.getPort();
    _database = driver.getDatabase();
    _catalog = _database;

    connect();
  }

  private void connect()
    throws SQLException
  {
    Socket s = null;

    try {
      s = new Socket(_host, _port);

      InputStream is = s.getInputStream();
      OutputStream os = s.getOutputStream();

      ReadStream in = Vfs.openRead(is);
      WriteStream out = Vfs.openWrite(os);

      MysqlReader reader = new MysqlReader(this, in);
      MysqlWriter writer = new MysqlWriter(this, out);

      readHandshakeInit(reader);

      String user = "ferg";
      String password = "";

      writeClientAuth(writer, user, password, _database);

      readOk(reader);

      _s = s;
      _in = reader;
      _out = writer;

      s = null;
    } catch (IOException e) {
      throw new SQLTransientConnectionException(L.l("{0}:{1} is not an accessible host",
                                                    _host, _port));
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
    }
  }

  /**
   * JDBC api to create a new statement.  Any SQL exception thrown here
   * will make the connection invalid, i.e. it can't be put back into
   * the pool.
   *
   * @return a new JDBC statement.
   */
  public Statement createStatement()
    throws SQLException
  {
    return new MysqlStatementImpl(this);
  }

  public Statement createStatement(int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    return createStatement();
  }

  public String getCatalog()
    throws SQLException
  {
    return _catalog;
  }

  /**
   * Sets the JDBC catalog.
   */
  public void setCatalog(String catalog)
    throws SQLException
  {
    if (_catalog == catalog || _catalog != null && _catalog.equals(catalog))
      return;

    _catalog = catalog;

    if (catalog == null)
      return;

    try {
      writeQuery("use " + catalog);
      readOk(_in);
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  /**
   * Gets the connection's metadata.
   */
  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    return new MysqlDatabaseMetaData(this);
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void clearWarnings()
    throws SQLException
  {
  }

  public boolean getAutoCommit()
    throws SQLException
  {
    return false;
  }

  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
  }

  public void commit()
    throws SQLException
  {
  }

  public void rollback()
    throws SQLException
  {
  }

  /**
   * Returns true if the connection is closed.
   */
  public boolean isClosed()
    throws SQLException
  {
    return false;
  }

  /**
   * Reset the connection and return the underlying JDBC connection to
   * the pool.
   */
  public void close() throws SQLException
  {
    Socket s = _s;
    _s = null;

    MysqlWriter out = _out;
    _out = null;

    MysqlReader in = _in;
    _in = null;
  }

  //
  // packet methods
  //

  private void readHandshakeInit(MysqlReader in)
    throws IOException, SQLException
  {
    in.readPacket();
    int protocolVersion = in.readByte();
    String serverVersion = in.readNullTermString();
    int threadId = in.readInt();

    in.readAll(_scrambleBuf, 0, 8);
    in.skip(1);

    _serverCapabilities = in.readShort();
    _serverLanguage = in.readByte();
    int serverStatus = in.readShort();

    in.readAll(_scrambleBuf, 8, 13);
    in.readAll(_scrambleBuf, 8, 13);
  }

  private String readOk(MysqlReader in)
    throws IOException, SQLException
  {
    in.readPacket();

    int fieldCount = in.readByte();

    if (fieldCount == 0xff)
      return readError(in);

    if (fieldCount == 0x00) {
      long rows = in.readLengthCodedBinary();
      long insertId = in.readLengthCodedBinary();
      int status = in.readShort();
      int warningCount = in.readShort();
      String message = in.readTailString();

      in.endPacket();

      return message;
    }
    else
      throw new SQLException("unexpected field");

  }

  String readResult(MysqlResultImpl result)
    throws SQLException
  {
    try {
      MysqlReader in = _in;

      in.readPacket();

      int fieldCount = in.readByte();

      if (fieldCount == 0xff)
        return readError(in);

      if (fieldCount == 0x00) {
        result.setResultSet(false);

        result.setUpdateCount((int) in.readLengthCodedBinary());
        result.setInsertId(in.readLengthCodedBinary());
        int status = in.readShort();
        int warningCount = in.readShort();
        String message = in.readTailString();
      }
      else if (fieldCount == 0xfe) {
        result.setResultSet(false);
      }
      else {
        result.setResultSet(true);

        fieldCount = (int) in.readLengthCodedBinary(fieldCount);

        readResultFields(result, fieldCount);
      }

      return null;
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  private void readResultFields(MysqlResultImpl result, int fieldCount)
    throws IOException, SQLException
  {
    MysqlReader in = _in;

    result.setColumnCount(fieldCount);

    int index = 0;

    while (true) {
      in.readPacket();

      int count = in.readByte();

      if (count == 0xfe || count < 0) { // EOF
        break;
      }

      MysqlColumn column = result.getColumn(index++);

      if (count == 251) {
        System.out.println("NULL:");
        continue;
      }

      int len;
      char []buffer;

      len = (int) in.readLengthCodedBinary(count);
      buffer = column.startCatalog(len);
      in.readAll(buffer, 0, len);

      len = (int) in.readLengthCodedBinary();
      buffer = column.startDatabase(len);
      in.readAll(buffer, 0, len);

      len = (int) in.readLengthCodedBinary();
      buffer = column.startTable(len);
      in.readAll(buffer, 0, len);

      len = (int) in.readLengthCodedBinary();
      buffer = column.startOrigTable(len);
      in.readAll(buffer, 0, len);

      len = (int) in.readLengthCodedBinary();
      buffer = column.startName(len);
      in.readAll(buffer, 0, len);

      len = (int) in.readLengthCodedBinary();
      buffer = column.startOrigName(len);
      in.readAll(buffer, 0, len);

      in.readByte();

      column.setCharset(in.readShort());
      column.setLength(in.readInt());
      column.setType(in.readByte());
      column.setFlags(in.readShort());
      column.setDecimals(in.readByte());

      int filler = in.readShort();
      // only for show tables
      // column.setDefault(in.readLengthCodedBinary());
    }

    int warningCount = in.readShort();
    int statusFlags = in.readShort();

    result.setRowAvailable(true);
    _state = State.FIELD_DATA;
  }

  private void skipRowData()
    throws SQLException
  {
    if (_state != State.FIELD_DATA)
      return;

    try {
      MysqlReader in = _in;
      
      while (true) {
        in.readPacket();

        int count = in.readByte();

        if (count == 0xfe || count < 0) { // EOF
          _state = State.IDLE;
          return;
        }
      }
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  boolean readRow(MysqlResultImpl result)
    throws SQLException
  {
    assert(_state == State.FIELD_DATA);
    
    try {
      MysqlReader in = _in;

      in.readPacket();

      int count = in.readByte();

      if (count == 0xfe || count < 0) { // EOF
        _state = State.IDLE;
        return false;
      }

      int fieldCount = result.getColumnCount();

      TempOutputStream resultStream = result.getResultStream();

      if (resultStream == null)
        throw new NullPointerException();

      int offset = 0;
      int length = 0;
      int index = 0;
      MysqlColumn column = result.getColumn(index++);

      length = (int) in.readLengthCodedBinary(count);

      column.setRowOffset(offset);
      column.setRowLength(length);
      in.readData(resultStream, length);
      offset += length;

      for (fieldCount--; fieldCount > 0; fieldCount--) {
        length = (int) in.readLengthCodedBinary();

        column = result.getColumn(index++);
        column.setRowOffset(offset);
        column.setRowLength(length);
        in.readData(resultStream, length);
        offset += length;
      }

      return true;
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  private String readError(MysqlReader in)
    throws IOException, SQLException
  {
    int errno = in.readShort();
    int marker = in.readByte();
    StringBuilder sb = new StringBuilder();

    if (marker == '#') {
      in.readAll(_errorState, 0, 5);
    }
    else
      sb.append((char) marker);

    int len = in.getPacketLength() - in.getPacketOffset();
    for (int i = 0; i < len; i++) {
      int ch = in.readByte();

      if (ch > 0)
        sb.append((char) ch);
    }

    System.out.println("ERROR: errno=" + errno + " stat:" + new String(_errorState) + " " + sb);

    throw new SQLException(sb.toString());
  }

  private void writeClientAuth(MysqlWriter out,
                               String user,
                               String password,
                               String database)
    throws IOException, SQLException
  {
    out.startVariablePacket();

    int clientFlags = 0;

    clientFlags |= CLIENT_PROTOCOL_41;
    clientFlags |= CLIENT_LONG_PASSWORD;
    clientFlags |= CLIENT_LONG_FLAG;

    // clientFlags = 0x03a685;

    clientFlags = 0x03a685;
    clientFlags &= ~CLIENT_COMPRESS;
    clientFlags &= ~CLIENT_SSL;

    if (_database != null)
      clientFlags |= CLIENT_CONNECT_WITH_DB;

    out.writeInt(clientFlags);

    int maxPacketSize = 1 << 24;

    out.writeInt(maxPacketSize);

    // int charsetNumber = UTF8_MB3;
    int charsetNumber = 8;

    out.writeByte(charsetNumber);
    out.writeZero(23);

    out.writeNullTermString(user);

    byte []hash = new byte[0];

    // out.writeNullTermString("");
    // out.write(hash, 0, hash.length);
    out.writeByte(0);

    if (_database != null)
      out.writeNullTermString(_database);

    out.endVariablePacket();
    out.flush();
  }

  void writeQuery(String query)
    throws SQLException
  {
    if (_state != State.IDLE)
      skipRowData();
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " query '" + query + "'");

    try {
      MysqlWriter out = _out;

      int len = query.length() + 1;

      out.writeByte(len);
      out.writeByte(len >> 8);
      out.writeByte(len >> 16);
      out.writeByte(0); // id

      out.writeByte(COM_QUERY);
      out.write(query);
      out.flush();
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  //
  // stub methods - methods not used by Quercus mysql
  //

  public Statement createStatement(int resultSetType,
                                   int resultSetConcurrency,
                                   int resultSetHoldability)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql,
                                            int []columnIndexes)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public PreparedStatement prepareStatement(String sql,
                                            String []columnNames)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public CallableStatement prepareCall(String sql,
                                       int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Map getTypeMap()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setTypeMap(Map<String,Class<?>> map)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String nativeSQL(String sql)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public int getTransactionIsolation()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setTransactionIsolation(int isolation)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isReadOnly()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }


  public void setHoldability(int hold)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public int getHoldability()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Savepoint setSavepoint()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Savepoint setSavepoint(String name)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void releaseSavepoint(Savepoint savepoint)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void rollback(Savepoint savepoint)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Clob createClob()
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Blob createBlob()
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public NClob createNClob()
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public SQLXML createSQLXML()
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean isValid(int timeout)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setClientInfo(String name, String value)
    throws SQLClientInfoException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setClientInfo(Properties properties)
    throws SQLClientInfoException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public String getClientInfo(String name)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Properties getClientInfo()
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Array createArrayOf(String typeName, Object[] elements)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Struct createStruct(String typeName, Object[] attributes)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return false;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _host + ":" + _port + "]";
  }
}
