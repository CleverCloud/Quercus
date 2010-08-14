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

package com.caucho.vfs;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for keeping track of modifications.
 */
public class PathExistsDependency implements Dependency {
  private static final Logger log
    = Logger.getLogger(PathExistsDependency.class.getName());
  
  Path _source;
  boolean _exists;

  /**
   * Create a new dependency.
   *
   * @param source the source file
   */
  public PathExistsDependency(Path source)
  {
    if (source instanceof JarPath)
      source = ((JarPath) source).getContainer();
    
    _source = source;
    _exists = source.exists();
  }

  /**
   * Create a new dependency with an already known modified time and length.
   *
   * @param source the source file
   */
  public PathExistsDependency(Path source, boolean exists)
  {
    _source = source;
    _exists = exists;
  }

  /**
   * Returns the underlying source path.
   */
  public Path getPath()
  {
    return _source;
  }

  /**
   * If the source modified date changes at all, treat it as a modification.
   * This protects against the case where multiple computers have
   * misaligned dates and a '<' comparison may fail.
   */
  public boolean isModified()
  {
    boolean exists = _source.exists();

    if (exists == _exists)
      return false;
    else if (exists) {
      if (log.isLoggable(Level.FINE))
        log.fine(_source.getNativePath() + " has been created.");

      return true;
    }
    else {
      if (log.isLoggable(Level.FINE))
        log.fine(_source.getNativePath() + " has been deleted.");
      
      return true;
    }
  }

  /**
   * Log the reason for the modification.
   */
  public boolean logModified(Logger log)
  {
    boolean exists = _source.exists();

    if (exists == _exists)
      return false;
    else if (exists) {
      log.info(_source.getNativePath() + " has been created.");

      return true;
    }
    else {
      log.info(_source.getNativePath() + " has been deleted.");
      
      return true;
    }
  }

  /**
   * Returns true if the test Dependency has the same source path as
   * this dependency.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof PathExistsDependency))
      return false;

    PathExistsDependency depend = (PathExistsDependency) obj;

    return _source.equals(depend._source);
  }

  /**
   * Returns a printable version of the dependency.
   */
  public String toString()
  {
    return ("PathExistsDependency[" + _source + "]");
  }
}
