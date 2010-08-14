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

package com.caucho.server.resin;

import java.lang.management.MemoryUsage;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.MemoryMXBean;

/**
 * Facade for the JVM's memory statistics
 */
public class MemoryAdmin extends AbstractManagedObject
  implements MemoryMXBean
{
  private final MBeanServer _mbeanServer;

  private final ObjectName _codeCacheName;
  private final ObjectName _edenName;
  private final ObjectName _permGenName;
  private final ObjectName _survivorName;
  private final ObjectName _tenuredName;

  private MemoryAdmin()
  {
    _mbeanServer = Jmx.getGlobalMBeanServer();

    try {
      ObjectName query = new ObjectName("java.lang:type=MemoryPool,*");

      ObjectName codeCacheName
        = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
      ObjectName edenName
        = new ObjectName("java.lang:type=MemoryPool,name=Eden Space");
      ObjectName permGenName
        = new ObjectName("java.lang:type=MemoryPool,name=Perm Gen");
      ObjectName survivorName
        = new ObjectName("java.lang:type=MemoryPool,name=Survivor Space");
      ObjectName tenuredName
        = new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen");
      
      for (ObjectName objName : _mbeanServer.queryNames(query, null)) {
        String name = objName.getKeyProperty("name");

        if (name.toLowerCase().contains("code"))
          codeCacheName = objName;
        else if (name.toLowerCase().contains("eden"))
          edenName = objName;
        else if (name.toLowerCase().contains("perm"))
          permGenName = objName;
        else if (name.toLowerCase().contains("surv"))
          survivorName = objName;
        else if (name.toLowerCase().contains("tenured"))
          tenuredName = objName;
        else if (name.toLowerCase().contains("old"))
          tenuredName = objName;
      }
      
      _codeCacheName = codeCacheName;
      _edenName = edenName;
      _permGenName = permGenName;
      _survivorName = survivorName;
      _tenuredName = tenuredName;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    registerSelf();
  }

  static MemoryAdmin create()
  {
    return new MemoryAdmin();
  }

  @Override
  public String getName()
  {
    return null;
  }

  public long getCodeCacheCommitted()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheMax()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheUsed()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getCodeCacheFree()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_codeCacheName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenCommitted()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenMax()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenUsed()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getEdenFree()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_edenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenCommitted()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenMax()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenUsed()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getPermGenFree()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_permGenName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorCommitted()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorMax()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorUsed()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getSurvivorFree()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_survivorName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredCommitted()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getCommitted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredMax()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredUsed()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public long getTenuredFree()
  {
    try {
      CompositeData data
        = (CompositeData) _mbeanServer.getAttribute(_tenuredName, "Usage");

      MemoryUsage usage = MemoryUsage.from(data);

      return usage.getMax() - usage.getUsed();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
