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

package com.caucho.management.server;

import com.caucho.jmx.Description;

/**
 * Management interface for the block manager used by the proxy cache
 * and persistent sessions.
 *
 * <pre>
 * resin:type=BlockManager
 * </pre>
 */
@Description("Resin's backing store block manager")
public interface BlockManagerMXBean extends ManagedObjectMXBean {
  
  /**
   * Returns the number of blocks in the block manager
   */
  @Description("The number of blocks in the block manager")
  public long getBlockCapacity();
  
  //
  // Statistics
  //
  
  /**
   * Returns the block read count.
   */
  @Description("The total blocks read from the backing")
  public long getBlockReadCountTotal();
  
  /**
   * Returns the block write count.
   */
  @Description("The total blocks written to the backing")
  public long getBlockWriteCountTotal();
  
  /**
   * Returns the block LRU cache hit count.
   */
  @Description("The hit count is the number of block accesses found in"
               + " the cache.")
  public long getHitCountTotal();

  /**
   * Returns the proxy cache miss count.
   */
  @Description("The hit count is the number of block accesses missing in"
               + " the cache.")
  public long getMissCountTotal();
}
