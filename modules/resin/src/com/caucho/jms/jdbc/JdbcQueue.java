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
import com.caucho.jms.JmsExceptionWrapper;
import com.caucho.jms.connection.MessageConsumerImpl;
import com.caucho.jms.connection.JmsSession;
import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.queue.AbstractQueue;
import com.caucho.jms.queue.MessageException;
import com.caucho.jms.queue.PollingQueue;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.jdbc.*;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import java.sql.*;
import java.io.*;
import javax.sql.*;
import java.util.logging.*;

/**
 * A jdbc queue.
 */
public class JdbcQueue<E> extends PollingQueue<E> {
  static final Logger log = Logger.getLogger(JdbcQueue.class.getName());
  static final L10N L = new L10N(JdbcQueue.class);
  
  protected JdbcManager _jdbcManager = new JdbcManager();
  
  private String _name;

  private int _id;
  private int _consumerId;

  public JdbcQueue()
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
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return getName();
  }

  /**
   * Sets the queue's name.
   */
  public void setQueueName(String name)
  {
    setName(name);
  }

  /**
   * Returns the JDBC id for the queue.
   */
  public int getId()
  {
    return _id;
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
   * Initializes the JdbcQueue
   */
  public void init()
    throws ConfigException
  {
    try {
      if (_jdbcManager.getDataSource() == null)
        throw new ConfigException(L.l("JdbcQueue requires a <data-source> element."));
    
      if (getName() == null)
        throw new ConfigException(L.l("JdbcQueue requires a <queue-name> element."));

      _jdbcManager.init();

      _id = createDestination(getName(), false);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Creates a consumer.
   */
  /*
  public MessageConsumerImpl createConsumer(JmsSession session,
                                            String selector,
                                            boolean noWait)
    throws JMSException
  {
    return new JdbcQueueConsumer(session, selector, _jdbcManager, this);
  }
  */

  /**
   * Creates a browser.
   */
  /*
  public QueueBrowser createBrowser(SessionImpl session, String selector)
    throws JMSException
  {
    return new JdbcQueueBrowser(session, selector, this);
  }
  */

  /**
   * Sends the message to the queue.
   */
  @Override
  public void send(String msgId,
                   E payload,
                   int priority,
                   long expireTime)
    throws MessageException
  {
    // JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    //jdbcMessage.send(payload, _id, priority, expireTime);
  }

  /**
   * Receives a message from the queue.
   */
  /*
  @Override
  public MessageImpl receive(boolean isAutoAck)
    throws JMSException
  {
    try {
      long minId = -1;

      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
        String sql = ("SELECT m_id, msg_type, msg_id, delivered, body, header" +
                      " FROM " + messageTable
                      + " WHERE ?<m_id AND queue=?"
                      + "   AND consumer IS NULL AND ?<=expire"
                      + " ORDER BY m_id");

        PreparedStatement selectStmt = conn.prepareStatement(sql);

        try {
          selectStmt.setFetchSize(1);
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }

        if (isAutoAck) {
          sql = ("DELETE FROM " + messageTable +
                 " WHERE m_id=? AND consumer IS NULL");
        }
        else
          sql = ("UPDATE " + messageTable +
                 " SET consumer=?, delivered=1" +
                 " WHERE m_id=? AND consumer IS NULL");

        PreparedStatement updateStmt = conn.prepareStatement(sql);

        long id = -1;
        while (true) {
          id = -1;

          selectStmt.setLong(1, minId);
          selectStmt.setInt(2, getId());
          selectStmt.setLong(3, Alarm.getCurrentTime());

          MessageImpl msg = null;

          ResultSet rs = selectStmt.executeQuery();
          while (rs.next()) {
            id = rs.getLong(1);

            minId = id;

            msg = jdbcMessage.readMessage(rs);

            if (true)
              break;
          }

          rs.close();

          if (msg == null)
            return null;

          if (isAutoAck) {
            updateStmt.setLong(1, id);
          }
          else {
            updateStmt.setLong(1, _consumerId);
            updateStmt.setLong(2, id);
          }

          int updateCount = updateStmt.executeUpdate();

          if (updateCount == 1)
            return msg;
        }
      } finally {
        conn.close();
      }
    } catch (IOException e) {
      throw new JmsExceptionWrapper(e);
    } catch (SQLException e) {
      throw new JmsExceptionWrapper(e);
    }
  }
  */

  /**
   * Removes the first message matching the selector.
   */
  public void commit(int session)
    throws JMSException
  {
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
  }

  protected void pollImpl()
  {
    boolean hasValue = false;
    
    try {
      long minId = -1;

      DataSource dataSource = _jdbcManager.getDataSource();
      String messageTable = _jdbcManager.getMessageTable();
      JdbcMessage jdbcMessage = _jdbcManager.getJdbcMessage();
    
      Connection conn = dataSource.getConnection();
      try {
        String sql = ("SELECT m_id" +
                      " FROM " + messageTable +
                      " WHERE ?<m_id AND queue=?" +
                      "   AND consumer IS NULL AND ?<=expire" +
                      " ORDER BY m_id");

        PreparedStatement selectStmt = conn.prepareStatement(sql);

        try {
          selectStmt.setFetchSize(1);
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }

        selectStmt.setLong(1, minId);
        selectStmt.setInt(2, getId());
        selectStmt.setLong(3, Alarm.getCurrentTime());

        MessageImpl msg = null;

        ResultSet rs = selectStmt.executeQuery();
        if (rs.next()) {
          hasValue = true;
        }

        rs.close();
      } finally {
        conn.close();
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    /*
    if (hasValue)
      notifyMessageAvailable();
    */
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "JdbcQueue[" + getName() + "]";
  }
}

