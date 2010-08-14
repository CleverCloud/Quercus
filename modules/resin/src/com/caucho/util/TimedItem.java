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

package com.caucho.util;

/**
 * A single item timed cache.  The item will remain valid until it expires.
 * TimedItem can simplify database caching.
 *
 * <pre><code>
 * TimedItem currentStories = new TimedItem(60000);
 *
 * public ArrayList getCurrentStories()
 * {
 *   ArrayList storyList = (ArrayList) currentStories.get();
 *
 *   if (storyList == null) {
 *     storyList = DB.queryStoryDatabase();
 *     currentStories.put(storyList);
 *   }
 *
 *   return storyList;
 * }
 * </code></pre>
 */
public class TimedItem {
  private long expireInterval;

  private long createTime;
  private Object value;

  /**
   * Create a new timed item with a specified update time
   *
   * @param expireInterval the time in milliseconds the item remains valid.
   */
  public TimedItem(long expireInterval)
  {
    this.expireInterval = expireInterval;
  }

  /**
   * Returns the expire time for this TimedItem.
   */
  public long getExpireInterval()
  {
    return expireInterval;
  }

  /**
   * Sets the expire time for this timedItem.
   */
  public void setExpireInterval(long expireInterval)
  {
    this.expireInterval = expireInterval;
  }

  /**
   * Sets the value.
   */
  public void put(Object value)
  {
    createTime = Alarm.getCurrentTime();
    this.value = value;
  }

  /**
   * Gets the cached value, returning null if expires.
   */
  public Object get()
  {
    if (Alarm.getCurrentTime() < createTime + expireInterval)
      return value;
    else {
      Object v = value;
      value = null;

      if (v instanceof CacheListener)
        ((CacheListener) v).removeEvent();

      return null;
    }
  }
}


