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

package com.caucho.j2ee.deploytool;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.model.exceptions.DDBeanCreateException;
import javax.enterprise.deploy.shared.ModuleType;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * Represents a deployable object
 */
public class DeployableObjectImpl implements DeployableObject {
  /**
   * Returns the module type.
   */
  public ModuleType getType()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the deployment descriptor root.
   */
  public DDBeanRoot getDDBeanRoot()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the array of standard beans for the XML content based
   * on the XPath.
   */
  public DDBean []getChildBean(String xpath)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the XML content matching the xpath.
   */
  public String []getText(String xpath)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the class from the module.
   */
  public Class getClassFromScope(String className)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deprecated method to get the version.
   */
  public String getModuleDTDVersion()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the Deployment descriptor root.
   */
  public DDBeanRoot getDDBeanRoot(String filename)
    throws FileNotFoundException, DDBeanCreateException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an enumeration of the entries, which are filenames relative
   * to the module root.
   */
  public Enumeration entries()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the InputStream for a given entry.  The filename is relative
   * to the module root.
   */
  public InputStream getEntry(String name)
  {
    throw new UnsupportedOperationException();
  }
}

