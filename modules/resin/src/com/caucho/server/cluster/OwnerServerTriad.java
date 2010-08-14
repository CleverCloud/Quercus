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

package com.caucho.server.cluster;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.network.balance.ClientSocketFactory;

/**
 * The primary,secondary,tertiary for a ClusterTriad.Owner
 */
public final class OwnerServerTriad
{
  private final ClusterServer _primary;
  private final ClusterServer _secondary;
  private final ClusterServer _tertiary;

  /**
   * Creates the server triad for a ClusterTriad.Owner
   */
  public OwnerServerTriad(ClusterServer primary,
                          ClusterServer secondary,
                          ClusterServer tertiary)
  {
    _primary = primary;
    _secondary = secondary;
    _tertiary = tertiary;
  }

  /**
   * Returns the primary for this ownership triad.
   */
  public final ClusterServer getPrimary()
  {
    return _primary;
  }

  /**
   * Returns the secondary for this ownership triad.
   */
  public final ClusterServer getSecondary()
  {
    return _secondary;
  }

  /**
   * Returns the tertiary for this ownership triad.
   */
  public final ClusterServer getTertiary()
  {
    return _tertiary;
  }

  /**
   * Returns the primary if it is remote, i.e. not this server itself.
   */
  public final ClusterServer getPrimaryIfRemote()
  {
    ClusterServer primary = _primary;

    if (primary.getServerPool() != null)
      return primary;
    else
      return null;
  }

  /**
   * Returns the secondary if it is remote, i.e. not this server itself.
   */
  public final ClusterServer getSecondaryIfRemote()
  {
    ClusterServer secondary = _secondary;

    if (secondary == null)
      return null;
    else if (secondary.getServerPool() != null)
      return secondary;
    else
      return null;
  }

  /**
   * Returns the tertiary if it is remote, i.e. not this server itself.
   */
  public final ClusterServer getTertiaryIfRemote()
  {
    ClusterServer tertiary = _tertiary;

    if (tertiary == null)
      return null;
    else if (tertiary.getServerPool() != null)
      return tertiary;
    else
      return null;
  }

  /**
   * Returns the primary if it is active, i.e. not this server itself
   * and not stopped
   */
  public final ClusterServer getPrimaryIfActiveRemote()
  {
    ClusterServer primary = _primary;

    if (primary.isActiveRemote())
      return primary;
    else
      return null;
  }

  /**
   * Returns the secondary if it is remote, i.e. not this server itself.
   */
  public final ClusterServer getSecondaryIfActiveRemote()
  {
    ClusterServer secondary = _secondary;

    if (secondary == null)
      return null;
    else if (secondary.isActiveRemote())
      return secondary;
    else
      return null;
  }

  /**
   * Returns the tertiary if it is remote, i.e. not this server itself.
   */
  public final ClusterServer getTertiaryIfActiveRemote()
  {
    ClusterServer tertiary = _tertiary;

    if (tertiary == null)
      return null;
    else if (tertiary.isActiveRemote())
      return tertiary;
    else
      return null;
  }

  /**
   * Returns the best primary or secondary triad server.
   */
  public ClusterServer getActiveServer(ClusterServer oldServer)
  {
    ClusterServer server;
    ClientSocketFactory pool;

    server = _primary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    server = _secondary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    server = _tertiary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    // force the send.  Server must be active, but pool may have a failure

    server = _primary;
    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && server != oldServer)
        return server;
    }

    server = _secondary;
    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && server != oldServer)
        return server;
    }

    server = _tertiary;
    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && server != oldServer)
        return server;
    }

    return null;
  }

  /**
   * Returns the best primary or secondary triad server.
   */
  public ClusterServer getActiveOrSelfServer(ClusterServer oldServer)
  {
    ClusterServer server;
    ClientSocketFactory pool;

    server = _primary;

    if (server != null) {
      pool = server.getServerPool();
    
    if (pool == null || pool.isActive() && server != oldServer)
      return server;
    }

    server = _secondary;
    
    if (server != null) {
      pool = server.getServerPool();
    
      if (pool == null || pool.isActive() && server != oldServer)
        return server;
    }

    server = _tertiary;

    if (server != null) {
      pool = server.getServerPool();
    
      if (pool == null || pool.isActive() && server != oldServer)
      return server;
    }

    // force the send

    server = _primary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = _secondary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = _tertiary;
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    return null;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[primary=" + _primary
            + ",seconary=" + _secondary
            + ",tertiary=" + _tertiary
            + "]");
  }
}
