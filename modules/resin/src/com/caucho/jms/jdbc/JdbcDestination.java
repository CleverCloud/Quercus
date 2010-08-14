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

package com.caucho.jms.jdbc;

import com.caucho.config.ConfigException;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.jms.queue.AbstractDestination;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC destination.
 */
abstract public class JdbcDestination extends AbstractDestination {
  static final Logger log = Logger.getLogger(JdbcDestination.class.getName());
  static final L10N L = new L10N(JdbcDestination.class);
  
  protected JdbcManager _jdbcManager = new JdbcManager();
  
  private String _name;
  
  private long _lastPurgeTime;

  public JdbcDestination()
  {
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true for a topic.
   */
  public boolean isTopic()
  {
    return false;
  }

  /**
   * Sets the jdbc manager
   */
  public void setJdbcManager(JdbcManager jdbcManager)
  {
    _jdbcManager = jdbcManager;
  }

  /**
   * Gets the JDBC manager.
   */
  public JdbcManager getJdbcManager()
  {
    return _jdbcManager;
  }
  
  /**
   * Sets the data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _jdbcManager.setDataSource(dataSource);
  }

  /**
   * Sets the tablespace for Oracle.
   */
  public void setTablespace(String tablespace)
  {
    _jdbcManager.setTablespace(tablespace);
  }

  /**
   * Initializes the JdbcDestination
   */
  @PostConstruct
  public void init()
    throws ConfigException, SQLException
  {
    _jdbcManager.init();
  }

  /**
   * Creates a queue.
   */
  protected int createDestination(String name, boolean isTopic)
    throws SQLException
  {
    Connection conn = _jdbcManager.getDataSource().getConnection();
    String destinationTable = _jdbcManager.getDestinationTable();
    String destinationSequence = _jdbcManager.getDestinationSequence();
    
    try {
      String sql = ("SELECT id FROM " + destinationTable +
                    " WHERE name=? AND is_topic=?");
      
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, name);
      pstmt.setInt(2, isTopic ? 1 : 0);

      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      }
      rs.close();

      if (destinationSequence != null) {
        JdbcMetaData metaData = _jdbcManager.getMetaData();
        sql = metaData.selectSequenceSQL(destinationSequence);
        int id = 0;

        pstmt = conn.prepareStatement(sql);

        rs = pstmt.executeQuery();
        if (rs.next())
          id = rs.getInt(1);
        else
          throw new RuntimeException("can't create sequence");

        sql = "INSERT INTO " + destinationTable + " (id,name,is_topic) VALUES(?,?,?)";

        pstmt = conn.prepareStatement(sql);

        pstmt.setInt(1, id);
        pstmt.setString(2, name);
        pstmt.setInt(3, isTopic ? 1 : 0);

        pstmt.executeUpdate();

        if (isTopic)
          log.fine("JMSTopic[" + name + "," + id + "] created");
        else
          log.fine("JMSQueue[" + name + "," + id + "] created");

        return id;
      }
      else {
        sql = "INSERT INTO " + destinationTable + " (name,is_topic) VALUES(?,?)";
        pstmt = conn.prepareStatement(sql,
                                      PreparedStatement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, name);
        pstmt.setInt(2, isTopic ? 1 : 0);

        pstmt.executeUpdate();

        rs = pstmt.getGeneratedKeys();

        if (rs.next()) {
          int id = rs.getInt(1);

          if (isTopic)
            log.fine("JMSTopic[" + name + "," + id + "] created");
          else
            log.fine("JMSQueue[" + name + "," + id + "] created");

          return id;
        }
        else
          throw new SQLException(L.l("can't generate destination for {0}",
                                     name));
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Purges expired messages.
   */
  protected void purgeExpiredMessages()
  {
    long purgeInterval = _jdbcManager.getPurgeInterval();
    long now = Alarm.getCurrentTime();

    if (now < _lastPurgeTime + purgeInterval)
      return;

    _lastPurgeTime = now;
    
    try {
      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
        String sql = ("DELETE FROM " + messageTable +
                      " WHERE expire < ? AND consumer IS NULL");

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setLong(1, Alarm.getCurrentTime());

        int count = pstmt.executeUpdate();

        if (count > 0)
          log.fine("JMSQueue[" + getName() + "] purged " + count + " expired mesages");

        pstmt.close();
      } finally {
        conn.close();
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}

