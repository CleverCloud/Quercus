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

/**
 * Cache entry
 */
public class CacheItem implements java.io.Serializable
{
  private String _url;

  private boolean _isCacheable;
  private long _hitCount;
  private long _missCount;

  public CacheItem()
  {
  }

  public String getUrl()
  {
    return _url;
  }

  public void setUrl(String url)
  {
    _url = url;
  }
  
  public boolean isCacheable()
  {
    return _isCacheable;
  }
  
  public void setCacheable(boolean isCacheable)
  {
    _isCacheable = isCacheable;
  }

  public long getHitCount()
  {
    return _hitCount;
  }

  public void setHitCount(long hitCount)
  {
    _hitCount = hitCount;
  }

  public long getMissCount()
  {
    return _missCount;
  }

  public void setMissCount(long missCount)
  {
    _missCount = missCount;
  }

  public String toString()
  {
    return "CacheItem[" + _url + ",hit:" + _hitCount + ",miss:" + _missCount + "]";
  }
}
