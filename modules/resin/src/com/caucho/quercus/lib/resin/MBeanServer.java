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
 * @author Sam
 */


package com.caucho.quercus.lib.resin;

import com.caucho.jmx.Jmx;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.server.resin.Resin;
import com.caucho.server.admin.RemoteMBeanConnectionFactory;
import com.caucho.util.L10N;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.*;

public class MBeanServer {
  private static final L10N L = new L10N(MBeanServer.class);
  private static final Logger log
    = Logger.getLogger(MBeanServer.class.getName());

  private static final Comparator<ObjectName> OBJECTNAME_COMPARATOR;

  private final MBeanServerConnection _server;

  /**
   * Create an MBeanServer that connects to a remote server.
   *
   * @param remoteUrl a url that connects to a
   * {@link com.caucho.services.jmx.JMXService} ussing the hessian protocol.
   */
  public MBeanServer(@Optional String serverId)
  {
    if (serverId == null
        || "".equals(serverId)
        || Server.getCurrent().getServerId().equals(serverId)) {
      _server = Jmx.getGlobalMBeanServer();
    }
    else {
      _server = RemoteMBeanConnectionFactory.create(serverId);
    }
  }

  /**
   * Perform a jmx lookup to retrieve an {@link MBean} object.
   *
   * If the optional name is not provided, the mbean for the current web-app
   * is returned.
   *
   * An unqualified name does not contain a `:' and is used to find an mbean
   * in the context of the current web-app.
   *
   * A fully qualified name contains a `:' and is used to find any mbean within the
   * server.
   *
   * @param name the name to lookup
   *
   * @return the mbean object, or null if it is not found.
   */
  public MBean lookup(Env env, @Optional String name)
  {
    try {
      if (name == null || name.length() == 0)
        return null;

      ObjectName objectName = Jmx.getObjectName(name);

      if (_server.isRegistered(objectName))
        return new MBean(_server, objectName);
      else
        return null;
    }
    catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(L.l("'{0}' is an invalid JMX name.\n{1}",
                                           name, e.getMessage()), e);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns an array of {@link MBean}s that match a JMX pattern.
   * If the name contains a ":", it is a query in the global jmx namespace.
   * If the name does not contain a ":", it is a search in the JMX namespace
   * of the current web application.
   */
  public ArrayValue query(Env env, String pattern)
  {
    try {
      ArrayValueImpl values = new ArrayValueImpl();

      ObjectName patternObjectName;

      patternObjectName = new ObjectName(pattern);

      Set<ObjectName> objectNames;

      objectNames = _server.queryNames(patternObjectName, null);

      if (objectNames == null)
        return values;

      TreeSet<ObjectName> sortedObjectNames
        = new TreeSet<ObjectName>(OBJECTNAME_COMPARATOR);

      sortedObjectNames.addAll(objectNames);

      for (ObjectName objectName : sortedObjectNames)
        values.put(env.wrapJava(new MBean(_server, objectName)));

      return values;
    }
    catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(e);
    }
    catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public String toString()
  {
    return "MBeanServer[" + _server + "]";
  }

  static {
    OBJECTNAME_COMPARATOR = new Comparator<ObjectName>() {
      public int compare(ObjectName o1, ObjectName o2)
      {
        if (o1 == null)
          return -1;

        if (o2 == null)
          return 1;

        return o1.getCanonicalName().compareTo(o2.getCanonicalName());
      }
    };
  }
}
