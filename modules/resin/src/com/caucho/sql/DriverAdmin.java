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


package com.caucho.sql;

import javax.management.*;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.JdbcDriverMXBean;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.beans.*;
import java.util.logging.*;

public class DriverAdmin extends AbstractManagedObject
  implements JdbcDriverMXBean
{
  private static final Logger log
    = Logger.getLogger(DriverAdmin.class.getName());
  
  private DriverConfig _driver;

  public DriverAdmin(DriverConfig driver)
  {
    _driver = driver;
  }

  @Override
  public String getName()
  {
    return _driver.getDBPool().getName();
  }

  public String getClassName()
  {
    return _driver.getType();
  }

  public String getUrl()
  {
    Properties props = getProperties();

    String url = (String) props.get("url");

    if (url != null)
      return url;

    return _driver.getURL();
  }

  public Properties getProperties()
  {
    try {
      Properties props = new Properties();
      
      Object driverObject = _driver.getDriverObject();

      if (driverObject == null)
        return props;

      BeanInfo info = Introspector.getBeanInfo(driverObject.getClass());

      for (PropertyDescriptor property : info.getPropertyDescriptors()) {
        String name = property.getName();

        if (name.equalsIgnoreCase("url"))
          name = "url";

        Method getter = property.getReadMethod();

        if (getter == null || property.getWriteMethod() == null)
          continue;

        try {
          Object value = getter.invoke(driverObject);

          if (name.equalsIgnoreCase("password"))
            value = "****";
          
          if (value != null)
            props.put(name, String.valueOf(value));
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }

      return props;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void addObjectNameProperties(Map<String,String> props)
    throws MalformedObjectNameException
  {
    String url = getUrl();
    
    if (url != null) {
      if (url.indexOf(':') >= 0)
        url = ObjectName.quote(url);

      props.put("url", url);
    }
  }

  //
  // state
  //

  public String getState()
  {
    return _driver.getLifecycle().getStateName();
  }

  //
  // statistics
  //
  
  public long getConnectionCountTotal()
  {
    return _driver.getConnectionCountTotal();
  }
  
  public long getConnectionFailCountTotal()
  {
    return _driver.getConnectionFailCountTotal();
  }
  
  public Date getLastFailTime()
  {
    return new Date(_driver.getLastFailTime());
  }

  //
  // Operations
  //
  
  /**
   * Enable the port, letting it listening to new requests.
   */
  public boolean start()
  {
    return _driver.start();
  }
  
  /**
   * Disable the port, stopping it from listening to new requests.
   */
  public boolean stop()
  {
    return _driver.stop();
  }

  void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return "JdbcDriverAdmin[" + getObjectName() + "]";
  }

}
