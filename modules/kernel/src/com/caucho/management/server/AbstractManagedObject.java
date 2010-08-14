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
import com.caucho.jmx.Jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parent mbean of all Resin's managed objects.
 */
abstract public class AbstractManagedObject implements ManagedObjectMXBean {
  private static final Logger log
    = Logger.getLogger(AbstractManagedObject.class.getName());
  
  private ClassLoader _classLoader;
  private ObjectName _objectName;

  protected AbstractManagedObject()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  protected AbstractManagedObject(ClassLoader loader)
  {
    _classLoader = loader;
  }
  
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @Description("The JMX ObjectName for the MBean")
  public ObjectName getObjectName()
  {
    if (_objectName == null) {

      try {
        Map<String,String> props = Jmx.copyContextProperties(_classLoader);

        props.put("type", getType());

        String name = getName();
        if (name != null) {
          if (name.indexOf(':') >= 0)
            name = ObjectName.quote(name);

          props.put("name", name);
        }

        addObjectNameProperties(props);

        _objectName = Jmx.getObjectName("resin", props);
      } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
      }
    }

    return _objectName;
  }

  protected void addObjectNameProperties(Map<String,String> props)
    throws MalformedObjectNameException
  {
  }

  /**
   * The JMX name property of the mbean.
   */
  abstract public String getName();

  /**
   * The JMX type of this MBean, defaults to the prefix of the FooMXBean..
   */
  public String getType()
  {
    Class []interfaces = getClass().getInterfaces();

    for (int i = 0; i < interfaces.length; i++) {
      String className = interfaces[i].getName();
      
      if (className.endsWith("MXBean")) {
        int p = className.lastIndexOf('.');
        int q = className.indexOf("MXBean");

        return className.substring(p + 1, q);
      }
    }

    int p = getClass().getName().lastIndexOf('.');

    return getClass().getName().substring(p + 1);
  }

  /**
   * Registers the object with JMX.
   */
  protected boolean registerSelf()
  {
    try {
      Jmx.register(this, getObjectName(), _classLoader);

      return true;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  /**
   * Unregisters the object with JMX.
   */
  protected boolean unregisterSelf()
  {
    try {
      Jmx.unregister(getObjectName(), _classLoader);

      return true;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
