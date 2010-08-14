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

package javax.enterprise.deploy.spi;

import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Top-level configuration.
 */
public interface DeploymentConfiguration {
  /**
   * Returns an interface to the descriptor.
   */
  public DeployableObject getDeployableObject();
  
  /**
   * Returns an interface to the descriptor.
   */
  public DConfigBeanRoot getDConfigBeanRoot(DDBeanRoot bean)
    throws ConfigurationException;
  
  /**
   * Removes a config bean
   */
  public void removeDConfigBean(DConfigBeanRoot bean)
    throws BeanNotFoundException;
  
  /**
   * Restores a config bean
   */
  public DConfigBeanRoot restoreDConfigBean(InputStream archiveStream,
                                            DDBeanRoot bean)
    throws ConfigurationException;
  
  /**
   * Saves a config bean
   */
  public void saveDConfigBean(OutputStream archiveStream,
                              DDBeanRoot bean)
    throws ConfigurationException;
  
  /**
   * Restores the configuration.
   */
  public void restore(InputStream archiveStream)
    throws ConfigurationException;
  
  /**
   * Saves the configuration.
   */
  public void save(OutputStream archiveStream)
    throws ConfigurationException;
}

