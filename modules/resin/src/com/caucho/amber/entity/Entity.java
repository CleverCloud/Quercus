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

package com.caucho.amber.entity;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.type.EntityType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An entity instance
 */
public interface Entity extends MappedSuperclass
{
  /**
   * Makes the entity persistent.
   */
  public boolean __caucho_makePersistent(AmberConnection aConn,
                                         EntityType entityType)
    throws SQLException;

  /**
   * Makes the entity persistent.
   */
  public void __caucho_makePersistent(AmberConnection aConn,
                                      EntityItem item)
    throws SQLException;

  /**
   * Pre-cascades the persist operation to child entities.
   */
  public void __caucho_cascadePrePersist(AmberConnection aConn)
    throws SQLException;

  /**
   * Pre-cascades the remove operation to child entities.
   */
  public void __caucho_cascadePreRemove(AmberConnection aConn)
    throws SQLException;

  /**
   * Post-cascades the persist operation to child entities.
   */
  public void __caucho_cascadePostPersist(AmberConnection aConn)
    throws SQLException;

  /**
   * Post-cascades the remove operation to child entities.
   */
  public void __caucho_cascadePostRemove(AmberConnection aConn)
    throws SQLException;

  /**
   * Detatch the entity
   */
  public void __caucho_detach();

  /**
   * Creates the entity in the database, making it persistent-new.
   */
  public boolean __caucho_create(AmberConnection aConn,
                                 EntityType entityType)
    throws SQLException;

  /**
   * Changes the entity state to P_PERSISTING, but does not flush to database.
   */
  public boolean __caucho_lazy_create(AmberConnection aConn,
                                      EntityType entityType)
    throws SQLException;

  /**
   * Sets the __caucho_item.
   */
  public void __caucho_setCacheItem(EntityItem item);

  /**
   * Set the primary key.
   */
  public void __caucho_setPrimaryKey(Object key);

  /**
   * Get the primary key.
   */
  public Object __caucho_getPrimaryKey();

  /**
   * Gets the corresponding cache entity referenced by __caucho_item.
   */
  public Entity __caucho_getCacheEntity();

  /**
   * Gets the __caucho_item.
   */
  public EntityItem __caucho_getCacheItem();

  /**
   * Get the entity type.
   */
  public EntityType __caucho_getEntityType();

  /**
   * Get the entity state.
   */
  public EntityState __caucho_getEntityState();

  /**
   * Sets the entity state.
   */
  public void __caucho_setEntityState(EntityState state);

  /**
   * Sets the connection.
   */
  public void __caucho_setConnection(AmberConnection aConn);

  /**
   * Returns the connection.
   */
  public AmberConnection __caucho_getConnection();

  /**
   * Returns true if the item is loaded
   */
  public boolean __caucho_isLoaded();

  /**
   * Returns true if the entity is dirty.
   */
  public boolean __caucho_isDirty();

  /**
   * Returns true if the entity matches.
   */
  public boolean __caucho_match(Class cl, Object key);

  /**
   * Loads the entity from the database.
   */
  public EntityItem __caucho_home_find(AmberConnection aConn,
                                       AmberEntityHome home,
                                       ResultSet rs, int index)
    throws SQLException;

  /**
   * Returns a new entity. In the case of inheritance, needs to query
   * the database.
   */
  public Entity __caucho_home_find(AmberConnection aConn,
                                   AmberEntityHome home,
                                   Object key);

  /**
   * Returns a new entity.
   */
  public Entity __caucho_home_new(AmberEntityHome home,
                                  Object key,
                                  AmberConnection aConn,
                                  EntityItem cacheItem);

  /**
   * Merges this entity state into an existing entity.
   */
  public void __caucho_mergeFrom(AmberConnection aConn,
                               Entity targetEntity);

  /**
   * Retrieves data from the data store, including the eager loads.
   */
  public void __caucho_retrieve_eager(AmberConnection aConn);

  /**
   * Retrieves data from the data store, only loading own fields.
   */
  public void __caucho_retrieve_self(AmberConnection aConn);

  /**
   * Loads the entity from the database and
   * returns the number of columns consumed
   * from the result set.
   */
  public int __caucho_load(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException;

  /**
   * Loads the entity key.
   */
  public Object __caucho_load_key(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException;

  /**
   * Loads the entity from the database.
   */
  public void __caucho_setKey(PreparedStatement pstmt, int index)
    throws SQLException;

  /**
   * Loads the entity from a native query
   */
  public void __caucho_load_native(ResultSet rs, String []columnNames)
    throws SQLException;

  /**
   * Expires data
   */
  public void __caucho_expire();

  /**
   * Deletes the entity from the database.
   */
  public void __caucho_delete();

  /**
   * Called when a foreign object is created/deleted.
   */
  public void __caucho_invalidate_foreign(String table, Object key);

  /**
   * Flushes changes to the backing store.
   */
  public boolean __caucho_flush()
    throws SQLException;

  /**
   * After a commit.
   */
  public void __caucho_afterCommit();

  /**
   * After a rollback.
   */
  public void __caucho_afterRollback();
}
