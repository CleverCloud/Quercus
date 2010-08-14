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
 * @author Sam 
 */

package com.caucho.resources.rmi;

import com.caucho.config.ConfigException;
import com.caucho.jca.ra.AbstractResourceAdapter;
import com.caucho.util.L10N;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapterInternalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * An RMI registry and its services.  This resource is used to register
 * services with an RMI Registry.  The Registry is either on localhost, in
 * which case it is created in the local JVM unless it already exists, or the
 * Registry is on a remote server, in which case it is assumed that the
 * Registry has already been started.
 */ 

public class RmiRegistry extends AbstractResourceAdapter 
{
  static private final Logger log 
    = Logger.getLogger(RmiRegistry.class.getName());
  static final L10N L = new L10N(RmiRegistry.class);

  private String _server = "localhost";
  private int _port = 1099;

  private LinkedList<RmiService> _services = new LinkedList<RmiService>();

  private String _namePrefix;

  /**
   * The server that runs the RMI registry.   If this is `localhost' (the
   * default), then the Registry is created on the localhost if it does not
   * already exist.  If the server is not localhost, then it is assumed that
   * the remote Registry already exists.
   */ 
  public void setServer(String server)
  {
    _server = server;
  }

  public String getServer()
  {
    return _server;
  }

  /**
   * The port for the Registry, default is 1099.
   */ 
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * The port for the Registry.
   */ 
  public int getPort()
  {
    return _port;
  }

  /**
   * Add an RMI service to register with this Registry.
   */ 
  public void addRmiService(RmiService service)
    throws ConfigException
  {
    _services.add(service);
    
  }

  @javax.annotation.PostConstruct
  public void init() throws ConfigException
  {
    if (System.getSecurityManager() == null)
      throw new ConfigException("RMI requires a SecurityManager - add a <security-manager/> element to <resin>");

    _namePrefix =("//" + _server + ':' + _port + '/');
  }

  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
    if (_server.equals("localhost"))
        startRegistry();
    else {
      log.config(L.l("using remote RMI Registry `{0}:{1}'",_server,String.valueOf(_port)));
    }

    // start (export and bind) all services
    for (Iterator<RmiService> i = _services.iterator(); i.hasNext(); ) {
      RmiService s = i.next();

      s.start();
    }
  }

  /** 
   * Make a full name for a service with this registry and
   * the given serviceName.
   */ 
  String makeFullName(String serviceName)
  {
    return _namePrefix + serviceName;
  }

  /**
   * Start the RMI registry in the local JVM, if it has not been started
   * already.
   */
  private void startRegistry()
    throws ResourceAdapterInternalException
  {
    /**
     * Some tricks are required here, the RMI Registry needs to be
     * started from the system classloader.
     */ 
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

      try {
        Registry reg = LocateRegistry.getRegistry(_port);
        reg.list();  // Verify it's alive and well
        if (log.isLoggable(Level.CONFIG))
          log.config(L.l("found RMI Registry on port `{0}'",
                         _port));
      }
      catch (Exception e) {
        // couldn't find a valid registry so create one
        if (log.isLoggable(Level.CONFIG))
          log.config(L.l("creating RMI Registry on port `{0}'", _port));

        LocateRegistry.createRegistry(_port);
      }
    } catch (Exception ex)  {
      throw new ResourceAdapterInternalException(ex);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
      
  /**
   * stop (unbind and unexport) all services
   */
  public void stop()
  {
    // unbind and unexport all services
    for (Iterator<RmiService> i = _services.iterator(); i.hasNext(); ) {
      RmiService s = i.next();
      s.stop();
    }
  }
}
