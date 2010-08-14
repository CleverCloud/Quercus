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

package com.caucho.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.db.block.BlockManager;
import com.caucho.db.block.BlockStore;
import com.caucho.db.lock.Lock;
import com.caucho.db.sql.Parser;
import com.caucho.db.sql.Query;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableFactory;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

/**
 * Manager for a basic Java-based database.
 */
public class Database
{
  private static final Logger log
    = Logger.getLogger(Database.class.getName());
  private static final L10N L = new L10N(Database.class);

  private Path _dir;

  private BlockManager _blockManager;
  private HashMap<String,Table> _tables = new HashMap<String,Table>();

  private LruCache<String,Query> _cachedQueries = new LruCache<String,Query>(128);

  private Lock _databaseLock = new Lock("db");

  private boolean _removeOnError;

  private final Lifecycle _lifecycle = new Lifecycle(log, null, Level.FINER);

  /**
   * Creates the database.
   */
  public Database()
  {
    this(null);
  }

  /**
   * Creates the database.
   */
  public Database(Path dir)
  {
    Environment.addClassLoaderListener(new CloseListener(this));

    _lifecycle.setName(toString());

    if (dir != null)
      setPath(dir);

    _blockManager = BlockManager.create();
  }

  /**
   * Sets the directory.
   */
  public void setPath(Path dir)
  {
    _dir = dir;
  }

  /**
   * Set if error tables should be removed.
   */
  public void setRemoveOnError(boolean remove)
  {
    _removeOnError = remove;
  }

  /**
   * Ensure a minimum memory size.
   *
   * @param minCapacity the minimum capacity in bytes
   */
  public void ensureMemoryCapacity(long minCapacity)
  {
    int minBlocks = (int) ((minCapacity + BlockStore.BLOCK_SIZE - 1) /
                           BlockStore.BLOCK_SIZE);

    _blockManager.ensureCapacity(minBlocks);
  }

  /**
   * Initializes the database.  All *.db files in the database directory
   * are read for stored tables.
   */
  @PostConstruct
  public void init()
    throws SQLException
  {
    if (! _lifecycle.toActive())
      return;
  }

  /**
   * Returns the path.
   */
  public Path getPath()
  {
    return _dir;
  }

  /**
   * Returns the block manager.
   */
  public BlockManager getBlockManager()
  {
    return _blockManager;
  }

  /**
   * Returns the database lock.
   */
  public Lock getDatabaseLock()
  {
    return _databaseLock;
  }

  /**
   * Creates a table factory.
   */
  public TableFactory createTableFactory()
  {
    return new TableFactory(this);
  }

  /**
   * Adds a table.
   */
  public void addTable(Table table)
    throws IOException
  {
    log.fine("adding table " + table.getName());

    table.init();

    _tables.put(table.getName(), table);
  }

  /**
   * Gets a table.
   */
  public Table getTable(String name)
  {
    synchronized (_tables) {
      Table table = _tables.get(name);

      if (table != null)
        return table;
      
      Path dir = _dir;
    
      if (dir == null)
        return null;

      try {
        table = Table.loadFromFile(this, name);

        if (table == null)
          return null;

        table.init();

        _tables.put(name, table);

        return table;
      } catch (Exception e) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.toString(), e);

        if (_removeOnError) {
          if (log.isLoggable(Level.FINER))
            log.log(Level.FINER, e.toString(), e);
          else
            log.warning(e.toString());

          try {
            dir.lookup(name + ".db").remove();
          } catch (IOException e1) {
            log.log(Level.FINEST, e.toString(), e);
          }
        }
      }
    }

    return null;
  }

  /**
   * Drops a table.
   */
  public void dropTable(String name)
    throws SQLException
  {
    Table table = null;

    synchronized (this) {
      table = getTable(name);
      
      if (table == null)
        throw new SQLException(L.l("Table {0} does not exist.  DROP TABLE can only drop an existing table.",
                                   name));

      _tables.remove(name);

      _cachedQueries.clear();
    }

    table.remove();
  }

  /**
   * Updates the database.
   */
  /*
  public void update(String sql, Transaction xa)
    throws SQLException
  {
    Query query = parseQuery(sql);

    query.execute(xa);
  }
  */

  /**
   * Queries the database.
   */
  /*
  public ResultSetImpl query(String sql, Transaction xa)
    throws SQLException
  {
    Query query = parseQuery(sql);

    return query.execute(xa);
  }
  */

  /**
   * Parses a query.
   */
  public Query parseQuery(String sql)
    throws SQLException
  {
    // XXX: currently, can't cache because the params are improperly shared
    /*
    Query query = _cachedQueries.get(sql);

    if (query == null) {
      query = Parser.parse(this, sql);
      _cachedQueries.put(sql, query);
    }
    */
    if (log.isLoggable(Level.FINER))
      log.finer(this + ": " + sql);
    
    Query query = Parser.parse(this, sql);

    return query;
  }

  /*
  public void lockRead(Transaction xa, long lockId)
    throws SQLException
  {
    Lock databaseLock = _databaseLock;

    try {
      synchronized (databaseLock) {
        if (xa.hasReadLock(databaseLock))
          return;

        databaseLock.lockRead(xa, _timeout);

        xa.addReadLock(databaseLock);
      }
    } catch (SQLException e) {
      xa.setRollbackOnly(e);
    }
  }
  */

  /**
   * Closes the database.
   */
  public void close()
  {
    if (! _lifecycle.toDestroy())
      return;

    for (Table table : _tables.values()) {
      try {
        table.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _dir + "]";
  }
}
