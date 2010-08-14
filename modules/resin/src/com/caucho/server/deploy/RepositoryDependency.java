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

package com.caucho.server.deploy;

import java.util.logging.Logger;

import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryService;
import com.caucho.vfs.PersistentDependency;

/**
 * Class for keeping track of modifications.
 */
public class RepositoryDependency implements PersistentDependency {
  private String _tag;
  private String _sha1;

  private transient Repository _repository;

  /**
   * Create a new dependency with an already known modified time and length.
   *
   * @param source the source file
   */
  public RepositoryDependency(String tag, String sha1)
  {
    _tag = tag;
    _sha1 = sha1;

    _repository = RepositoryService.getCurrentRepository();
  }

  /**
   * If the source modified date changes at all, treat it as a modification.
   * This protects against the case where multiple computers have
   * misaligned dates and a '<' comparison may fail.
   */
  @Override
  public boolean isModified()
  {
    String value = getRepository().getTagContentHash(_tag);

    if (_sha1 != null && ! _sha1.equals(value))
      return true;
    else if (_sha1 == null && value != null)
      return true;
    else
      return false;
  }

  private Repository getRepository()
  {
    return _repository;
  }

  /**
   * Log the reason for modification
   */
  @Override
  public boolean logModified(Logger log)
  {
    if (! isModified())
      return false;

    log.info(_tag + " is modified.");

    return true;
  }
  
  /**
   * Returns true if the test Dependency has the same tag as
   * this dependency.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (! (obj instanceof RepositoryDependency))
      return false;

    RepositoryDependency depend = (RepositoryDependency) obj;

    return _tag.equals(depend._tag);
  }

  @Override
  public String getJavaCreateString()
  {
    return ("new " + getClass().getName()
            + "(\"" + _tag + "\", \"" + _sha1 + "\")");
  }

  /**
   * Returns a printable version of the dependency.
   */
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _tag + "," + _sha1 + "]");
  }
}
