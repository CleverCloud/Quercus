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

package com.caucho.j2ee.deployclient;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

import com.caucho.VersionFactory;

/**
 * Factory for the implementation classes.
 */
public class DeploymentFactoryImpl implements DeploymentFactory {
  /**
   * Returns true if the deployment manager handles the URI.
   */
  public boolean handlesURI(String uri)
  {
    return uri.startsWith("resin:http");
  }

  /**
   * Returns a deployment manager for the URI.
   */
  public DeploymentManager getDeploymentManager(String uri,
                                                String username,
                                                String password)
    throws DeploymentManagerCreationException
  {
    try {
      DeploymentManagerImpl manager = createManager(uri);

      manager.connect(username, password);

      return manager;
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a deployment manager for the URI.
   */
  public DeploymentManager getDisconnectedDeploymentManager(String uri)
    throws DeploymentManagerCreationException
  {
    return createManager(uri);
  }

  /**
   * Creates the manager.
   */
  private DeploymentManagerImpl createManager(String uri)
    throws DeploymentManagerCreationException
  {
    if (! handlesURI(uri))
      throw new DeploymentManagerCreationException("'" + uri + "' is an unsupported deployment URI.");

    return new DeploymentManagerImpl(uri);
  }

  /**
   * Returns the name of the vendor's manager.
   */
  public String getDisplayName()
  {
    return "Resin J2EE DeploymentFactory";
  }

  /**
   * Returns a string of the version.
   */
  public String getProductVersion()
  {
    return VersionFactory.getFullVersion();
  }
}

