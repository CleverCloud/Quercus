/**
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.distcache;

import javax.cache.Cache;

/**
 * Provides the means to request items from a persistent cache as of
 * specific time or version.
 */
public interface StorageCache extends Cache {


  /**
   * Returns the value for a specific version of the stored entry.
   *
   * @param key under which values have been stored
   * @param version of the value
   * @return the value or null when there is no matching entry
   */
  public Object getAsOfVersion(Object key, int version);

  /**
   * Returns the value for a stored entry that was current at the
   * time.
   *
   * @param key under which values have been stored
   * @param time when the value returned was the current entry.
   * @return
   */
  public Object getAsOfTime(Object key, long time);

}
