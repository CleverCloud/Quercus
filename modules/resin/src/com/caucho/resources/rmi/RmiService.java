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
import com.caucho.util.L10N;

import javax.resource.spi.ResourceAdapterInternalException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class RmiService {
  static private final Logger log
   = Logger.getLogger(RmiService.class.getName());
  static final L10N L = new L10N(RmiService.class);

  private RmiRegistry _registry;

  private String _serviceName;
  private String _serviceClass;
  private Class _serviceClassClass;

  private String _boundName;
  private Remote _boundObject;

  /**
   * The name of the service in the Registry, usually by convention this is the
   * same as the class name of the interface for the service.  RMI has a global
   * namespace, so this name must be unique within the JVM of the Registry,
   * even if the service happens to belong only to a particular web-app.
   */
  public void setServiceName(String name)

  {
    _serviceName = name;
  }

  public String getServiceName()
  {
    return _serviceName;
  }

  /**
   * The name of the class that implements the service.
   */
  public void setServiceClass(String serviceClass)
  {
    _serviceClass = serviceClass;
  }

  /**
   * The name of the class that implements the service.
   */
  public String getServiceClass()
  {
    return _serviceClass;
  }

  public void setParent(Object parent)
  {
    if (parent instanceof RmiRegistry)
      _registry = (RmiRegistry) parent;
  }

  public void init()
    throws ConfigException
  {
    if (_registry == null) 
      throw new ConfigException(L.l("{0} must be used as a child of {1}","RmiService","RmiRegistry"));

    if (_serviceClass == null) 
      throw new ConfigException(L.l("`{0}' is required","service-class"));
    if (_serviceName == null) 
      throw new ConfigException(L.l("`{0}' is required","service-name"));
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      if (loader != null)
        _serviceClassClass = Class.forName(_serviceClass, false, loader);
      else
        _serviceClassClass = Class.forName(_serviceClass);
    } catch (ClassNotFoundException ex) {
      throw new ConfigException(L.l("no class found with name `{0}'",_serviceClass));
    } 
  }


  /**
   * Bind the RMI service.
   */
  public void start()
    throws ResourceAdapterInternalException
  {
    String fullName = _registry.makeFullName(_serviceName);

    if (_boundName != null)
      throw new ResourceAdapterInternalException(L.l("cannot bind rmi service with name `{0}', already bound with name `{1}'", fullName, _boundName));

    try {
      _boundObject = (Remote) _serviceClassClass.newInstance();

      if (log.isLoggable(Level.FINE))
        log.fine(L.l("binding rmi name `{0}' to object `{1}'",fullName,_boundObject.getClass().getName()));

      Naming.rebind(fullName, _boundObject);
      _boundName = fullName;
    }
    catch (Exception ex) {
      throw new ResourceAdapterInternalException(L.l("error binding rmi service with name `{0}'", fullName),ex);
    }

  }

  /**
   * unbind and unexport
   */
  void stop()
  {
    if (_boundName != null) {
      try {
        if (log.isLoggable(Level.FINE))
          log.fine(L.l("unbinding rmi name `{0}'", _boundName));

        Naming.unbind(_boundName);

      } catch (Exception ex) {
        if (log.isLoggable(Level.INFO))
          log.log(Level.INFO,L.l("error unbinding rmi name `{0}'", _boundName),ex);
      }
      _boundName = null;
    }

    if (_boundObject != null) {
      try {
        if (log.isLoggable(Level.FINEST))
          log.finest(L.l("unexporting rmi object `{0}'", _boundObject.getClass().getName()));

        UnicastRemoteObject.unexportObject(_boundObject, true);
      } catch (Exception ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE,L.l("error unexporting rmi object `{0}'", _boundObject.getClass().getName()),ex);
      }
      _boundObject = null;
    }
  }
}

