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

package com.caucho.server.distcache;

import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.config.ConfigException;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.Sha256OutputStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.cache.CacheLoader;

/**
 * Manages the distributed cache
 */
public class AdminPersistentStore extends AbstractManagedObject 
  implements PersistentStoreMXBean
{
  private AbstractDataCacheManager _manager;
  
  AdminPersistentStore(AbstractDataCacheManager manager)
  {
    _manager = manager;
    
    registerSelf();
  }
  
  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public long getObjectCount()
  {
    return 0;
  }

  @Override
  public long getMnodeCount()
  {
    return _manager.getMnodeStore().getCount();
  }

  @Override
  public long getDataCount()
  {
    return _manager.getDataStore().getCount();
  }

  @Override
  public long getLoadCountTotal()
  {
    return 0;
  }

  @Override
  public long getLoadFailCountTotal()
  {
    return 0;
  }

  @Override
  public long getSaveCountTotal()
  {
    return 0;
  }

  @Override
  public long getSaveFailCountTotal()
  {
    return 0;
  }

  @Override
  public String getStoreType()
  {
    return null;
  }
}
