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


package com.caucho.netbeans;

import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;

import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import java.util.WeakHashMap;
import java.util.logging.*;

public class ResinDeploymentFactory
  implements DeploymentFactory
{
  private static final Logger log
    = Logger.getLogger(ResinDeploymentFactory.class.getName());
  private static final PluginL10N L = new PluginL10N(ResinDeploymentFactory.class);

  private static final String RESIN_PREFIX = "resin:"; // NOI18N
  private static final String DISCONNECTED_URI = "resin:virtual";

  private static DeploymentFactory _instance;

  private static final WeakHashMap<InstanceProperties, ResinDeploymentManager> _managerCache
    = new WeakHashMap<InstanceProperties, ResinDeploymentManager>();

  public static synchronized DeploymentFactory create()
  {
    if (_instance == null) {
      //log.fine("Create Resin Deployment Factory");
    
      _instance = new ResinDeploymentFactory();

      DeploymentFactoryManager.getInstance().registerDeploymentFactory(_instance);
    }

    return _instance;
  }

  public boolean handlesURI(String uri)
  {
    return (uri != null) && uri.startsWith(RESIN_PREFIX);
  }

  public DeploymentManager getDeploymentManager(String uri,
						String username,
						String password)
    throws DeploymentManagerCreationException
  {
    if (!handlesURI(uri))
      throw new DeploymentManagerCreationException(L.l("'{0}' is not a Resin URI",  uri));
    
    InstanceProperties ip = InstanceProperties.getInstanceProperties(uri);

    if (ip == null) {
      if (!DISCONNECTED_URI.equals(uri))
        throw new DeploymentManagerCreationException(L.l("Resin instance '{0}' is not registered.", uri));
    }
    
    ResinDeploymentManager manager = _managerCache.get(ip);

    if (manager == null) {
      try {
        manager = new ResinDeploymentManager(uri, ip);
        _managerCache.put(ip, manager);
      }
      catch (IllegalArgumentException e) {
        Exception t = new DeploymentManagerCreationException(L.l("Cannot create deployment manager for Resin instance: {0}", uri));

        throw (DeploymentManagerCreationException) t.initCause(e);
      }
    }
    

    return manager;
  }

  public DeploymentManager getDisconnectedDeploymentManager(String uri)
    throws DeploymentManagerCreationException
  {
    // called on initialization and configuration
    return getDeploymentManager(uri, null, null);
  }

  public String getDisplayName()
  {
    return "Resin 3.1";
  }

  public String getProductVersion()
  {
    return "3.1";
  }
}
