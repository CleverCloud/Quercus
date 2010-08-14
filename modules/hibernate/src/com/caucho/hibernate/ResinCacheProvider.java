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

package com.caucho.hibernate;

import java.util.Properties;
import org.hibernate.cache.*;
import com.caucho.distcache.ClusterCache;

public class ResinCacheProvider implements CacheProvider {
  private static final long DAY = 24 * 3600 * 1000;

  /**
   * Creates a new ResinCache for hibernate.
   */
  public Cache buildCache(String regionName, Properties properties)
    throws CacheException
  {
    ClusterCache cache = new ClusterCache();
    cache.setName("hibernate:" + regionName);
    cache.setExpireTimeoutMillis(1 * DAY);
    cache.setLocalReadTimeoutMillis(1000L);
    cache.init();

    return new ResinCache(regionName, cache);
  }

  public long nextTimestamp()
  {
    return Timestamper.next();
  }

  public void start(Properties properties)
    throws CacheException
  {
  }

  public void stop()
  {
  }

  public boolean isMinimalPutsEnabledByDefault()
  {
    return true;
  }
}