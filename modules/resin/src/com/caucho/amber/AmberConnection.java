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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Main user interface to Amber.
 */
public interface AmberConnection {
  /**
   * Loads the entity specified by its class and primary key.
   */
  public Object load(Class cl, Object key)
    throws AmberException;
  
  /**
   * Loads the entity specified by its class and primary key.
   */
  public Object load(Class cl, long key)
    throws AmberException;
  
  /**
   * Loads the entity, returning the active entity
   */
  public Object makePersistent(Object entity)
    throws SQLException;

  /**
   * Creates a new entity given the values in the object.
   */
  public void create(Object obj)
    throws SQLException;
  
  /**
   * Returns true if the entitiy is managed.
   */
  public boolean contains(Object entity);

  /**
   * Deletes the specified entry.
   */
  public void delete(Object obj)
    throws SQLException;

  /**
   * Queries the database, returning a result set
   *
   * @param query the query
   *
   * @return the query result set.
   */
  public ResultSet query(String query)
    throws SQLException;

  /**
   * Queries the database, returning a result set
   *
   * @param query the query
   *
   * @return the query result set.
   */
  public int update(String query)
    throws SQLException;
  
  /**
   * Creates a query object from a query string.
   *
   * The query will load the default group values for any selected
   * entitites.
   *
   * @param query a query
   */
  public AmberQuery prepareQuery(String queryString)
    throws AmberException;

  /**
   * Starts a transaction.
   */
  public void beginTransaction()
    throws SQLException;

  /**
   * Commits a transaction.
   */
  public void commit()
    throws SQLException;

  /**
   * Rolls a transaction back.
   */
  public void rollback()
    throws SQLException;
  
  /**
   * Flushes data from the connection.
   */
  public void flush()
    throws SQLException;
  
  /**
   * Closes the connection.  Unlike JDBC, this will never throw an
   * exception (although it may log a warning.)
   */
  public void close();

  /**
   * Queries the database, returning a result set
   *
   * @param query the query
   *
   * @return the query result set.
   */
  public List find(String query)
    throws SQLException;
}
