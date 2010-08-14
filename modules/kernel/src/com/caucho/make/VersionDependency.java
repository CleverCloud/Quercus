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

package com.caucho.make;

import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.PersistentDependency;

import java.util.logging.Logger;

/**
 * Representing a Resin version.
 */
public class VersionDependency implements PersistentDependency {
  private final static Logger log
    = Logger.getLogger(VersionDependency.class.getName());
  
  private String _version;

  /**
   * Creates the version dependency.
   */
  public VersionDependency(String version)
  {
    _version = version;
  }

  /**
   * Creates the version dependency.
   */
  public VersionDependency()
  {
    _version = CauchoSystem.getFullVersion();
  }
  
  /**
   * Returns true if the underlying resource has changed.
   */
  public boolean isModified()
  {
    return ! CauchoSystem.getFullVersion().equals(_version);
  }
  
  /**
   * Returns true if the underlying resource has changed.
   */
  public boolean logModified(Logger log)
  {
    if (! CauchoSystem.getFullVersion().equals(_version)) {
      log.info("Resin version has changed to " + CauchoSystem.getFullVersion());
      return true;
    }
    else
      return false;
  }

  /**
   * Returns a string which will recreate the dependency.
   */
  public String getJavaCreateString()
  {
    return ("new com.caucho.make.VersionDependency(" +
            "\"" + _version + "\")");
  }
}
