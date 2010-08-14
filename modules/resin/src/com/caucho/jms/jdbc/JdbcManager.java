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

package com.caucho.jms.jdbc;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.sql.DataSource;
import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages the JDBC configuration.
 */
public class JdbcManager {
  static final Logger log = Log.open(JdbcManager.class);
  static final L10N L = new L10N(JdbcManager.class);
  
  private DataSource _dataSource;

  private String _messageTable = "resin_jms_message";
  
  private String _destinationTable = "resin_jms_destination";
  private String _destinationSequence;
  
  private String _consumerTable = "resin_jms_consumer";
  private String _consumerSequence;
  
  private String _blob;
  private String _longType;
  
  private String _tablespace;  // oracle tablespace for blobs

  private boolean _isTruncateBlob;

  private long _purgeInterval = 60000L;
  private long _pollInterval = -1;

  private JdbcMessage _jdbcMessage;

  private volatile boolean _isInit;

  public JdbcManager()
  {
  }

  /**
   * Sets the data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Returns the message table
   */
  public String getMessageTable()
  {
    return _messageTable;
  }

  /**
   * Sets the message table
   */
  public void setMessageTable(String tableName)
  {
    _messageTable = tableName;
  }

  /**
   * Returns the destination table
   */
  public String getDestinationTable()
  {
    return _destinationTable;
  }

  /**
   * Sets the destination table
   */
  public void setDestinationTable(String tableName)
  {
    _destinationTable = tableName;
  }

  /**
   * Returns the destination sequence
   */
  public String getDestinationSequence()
  {
    return _destinationSequence;
  }

  /**
   * Returns the consumer table
   */
  public String getConsumerTable()
  {
    return _consumerTable;
  }

  /**
   * Sets the consumer table
   */
  public void setConsumerTable(String tableName)
  {
    _consumerTable = tableName;
  }

  /**
   * Returns the consumer sequence
   */
  public String getConsumerSequence()
  {
    return _consumerSequence;
  }

  /**
   * Returns the meta-data.
   */
  public JdbcMetaData getMetaData()
  {
    return JdbcMetaData.create(_dataSource);
  }

  /**
   * Returns the blob type.
   */
  public String getBlob()
  {
    if (_blob == null)
      _blob = getMetaData().getBlobType();

    return _blob;
  }

  /**
   * Sets the oracle tablespace.
   */
  public void setTablespace(String tablespace)
  {
    _tablespace = tablespace;
  }

  /**
   * Gets the oracle tablespace
   */
  public String getTablespace()
  {
    return _tablespace;
  }

  /**
   * Returns the blob type.
   */
  public String getLongType()
  {
    if (_longType == null)
      _longType = getMetaData().getLongType();

    return _longType;
  }

  /**
   * Sets the purge interval.
   */
  public void setPurgeInterval(Period period)
  {
    _purgeInterval = period.getPeriod();
  }

  /**
   * Gets the purge interval.
   */
  public long getPurgeInterval()
  {
    return _purgeInterval;
  }

  /**
   * Sets the poll interval for destinations that have a listener, default
   * is to do no polling.
   */
  public void setPollInterval(Period pollInterval)
  {
    _pollInterval = pollInterval.getPeriod();
  }

  /**
   * Returns the poll interval.
   */
  public long getPollInterval()
  {
    return _pollInterval;
  }

  /**
   * Returns the JDBC message manager.
   */
  public JdbcMessage getJdbcMessage()
  {
    return _jdbcMessage;
  }

  /**
   * Return true if blobs need to be truncated before deletion.
   */
  public boolean isTruncateBlob()
  {
      return _isTruncateBlob;
  }

  /**
   * Initializes the JdbcManager
   */
  @PostConstruct
  public void init()
    throws ConfigException, SQLException
  {
    if (_isInit)
      return;
    _isInit = true;
    
    if (_dataSource == null)
      throw new ConfigException(L.l("JdbcManager requires a <data-source> element."));

    _jdbcMessage = new JdbcMessage(this);

    _jdbcMessage.init();

    initDestinationTable();
    initConsumerTable();
    
    _isTruncateBlob = getMetaData().isTruncateBlobBeforeDelete();
  }

  /**
   * Initializes the destination table.
   */
  protected void initDestinationTable()
    throws SQLException
  {
    Connection conn = _dataSource.getConnection();
    
    if (! getMetaData().supportsIdentity()) {
      if (! getMetaData().supportsSequences())
        throw new ConfigException(L.l("JdbcManager requires a datasource that supports either identity or sequences"));

      _destinationSequence = _destinationTable + "_cseq";
    }

    try {
      Statement stmt = conn.createStatement();
      String sql = "SELECT 1 FROM " + _destinationTable + " WHERE 1=0";

      try {
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        rs.close();
        stmt.close();

        return;
      } catch (SQLException e) {
        log.finest(e.toString());
      }

      log.info(L.l("creating JMS destination table {0}", _destinationTable));

      String longType = getLongType();
      String identity = longType + " PRIMARY KEY";

      if (getMetaData().supportsIdentity())
        identity = getMetaData().createIdentitySQL(identity);

      sql = ("CREATE TABLE " + _destinationTable + " (" +
             "  id " + identity + "," +
             "  name VARCHAR(255)," +
             "  is_topic INTEGER" +
             ")");

      stmt.executeUpdate(sql);

      if (! getMetaData().supportsIdentity()) {
        _destinationSequence = _destinationTable + "_cseq";

        stmt.executeUpdate(getMetaData().createSequenceSQL(_destinationSequence, 1));
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Initializes the consumer table.
   */
  protected void initConsumerTable()
    throws SQLException
  {
    if (! getMetaData().supportsIdentity())
      _consumerSequence = _consumerTable + "_cseq";
      
    Connection conn = _dataSource.getConnection();
    try {
      Statement stmt = conn.createStatement();
      String sql = "SELECT 1 FROM " + _consumerTable + " WHERE 1=0";

      try {
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        rs.close();
        stmt.close();

        return;
      } catch (SQLException e) {
        log.finest(e.toString());
      }

      log.info(L.l("creating JMS consumer table {0}", _consumerTable));

      String longType = getLongType();
      String identity = longType + " PRIMARY KEY";

      if (getMetaData().supportsIdentity())
        identity = getMetaData().createIdentitySQL(identity);

      sql = ("CREATE TABLE " + _consumerTable + " (" +
             "  s_id " + identity + "," +
             "  queue " + longType + "," +
             "  client VARCHAR(255)," +
             "  name VARCHAR(255)," +
             "  expire " + longType + "," +
             "  read_id " + longType + "," +
             "  ack_id " + longType +
             ")");

      stmt.executeUpdate(sql);

      if (_consumerSequence != null)
        stmt.executeUpdate(getMetaData().createSequenceSQL(_consumerSequence, 1));
    } finally {
      conn.close();
    }
  }

  /**
   * Returns a hash code.
   */
  public int hashCode()
  {
    if (_dataSource == null)
      return 0;
    else
      return _dataSource.hashCode();
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof JdbcManager))
      return false;

    JdbcManager manager = (JdbcManager) o;

    return _dataSource != null && _dataSource.equals(manager._dataSource);
  }
}

