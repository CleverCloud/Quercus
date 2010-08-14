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

package javax.enterprise.deploy.model;

import javax.enterprise.deploy.shared.ModuleType;

/**
 * Represents an EAR file.
 */
public interface J2eeApplicationObject extends DeployableObject {
  /**
   * Returns the DeployableObject with the given URI.
   */
  public DeployableObject getDeployableObject(String uri);

  /**
   * Returns all deployable objects of the given type.
   */
  public DeployableObject []getDeployableObjects(ModuleType type);

  /**
   * Returns the URIs for the given module type.
   */
  public String []getModuleUris(ModuleType type);

  /**
   * Returns the DDBean based on the XPath.
   */
  public DDBean []getChildBean(ModuleType type, String xpath);

  /**
   * Returns the text for the matching module types.
   */
  public String []getText(ModuleType type, String xpath);

  /**
   * Adds a listener for changes in the xpath.
   */
  public void addXpathListener(ModuleType type,
                               String xpath,
                               XpathListener xpl);
  
  /**
   * Removes a listener for changes in the xpath.
   */
  public void removeXpathListener(ModuleType type,
                                  String xpath,
                                  XpathListener xpl);
}

