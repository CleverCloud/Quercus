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

package com.caucho.jcr.svn;

import com.caucho.util.L10N;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Subversion node class.
 */
public class SubversionNode {
  private final L10N L = new L10N(SubversionNode.class);
  private final Logger log
    = Logger.getLogger(SubversionNode.class.getName());;

  private String _name;
  private long _version = 1;
  private Date _lastModified;
  private String _user = "nobody";

  public SubversionNode(String name)
  {
    _name = name;
  }

  /**
   * Returns the node name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the version.
   */
  public long getVersion()
  {
    return _version;
  }

  /**
   * Sets the version.
   */
  public void setVersion(long version)
  {
    _version = version;
  }

  /**
   * Returns the user who updated the node.
   */
  public String getUser()
  {
    return _user;
  }

  /**
   * Sets the user who updated the node.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Returns the last-modified time.
   */
  public Date getLastModified()
  {
    return _lastModified;
  }

  /**
   * Sets teh last-modified date
   */
  public void setLastModified(Date lastModified)
  {
    _lastModified = lastModified;
  }

  public String toString()
  {
    return "SubversionNode[" + _name + ",rev=" + _version + "," + _user + "]";
  }
}
