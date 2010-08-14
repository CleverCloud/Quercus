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

package com.caucho.env.git;

import java.util.HashMap;
import java.util.Map;

/**
 * The Commit structure has four main data items:
 * <ul>
 * <li>The Tree which is the hash of the content root</li>
 * <li>The parent, which is the hash of the previous version's Commit</li>
 * <li>meta-data &lt;key, value> pairs</li>
 * <li>A text message describing the commit</li>
 * </ul>
 */
public class GitCommit {
  private String _parent;
  private String _tree;
  private String _message;
  
  private HashMap<String,String> _attributes = new HashMap<String,String>();

  public String getMessage()
  {
    return _message;
  }

  public void setMessage(String message)
  {
    _message = message;
  }

  /**
   * The hash of the previous version's Commit entry or null if
   * this Commit has no previous version.
   */
  public String getParent()
  {
    return _parent;
  }


  /**
   * The hash of the previous version's Commit entry or null if
   * this Commit has no previous version.
   */
  public void setParent(String parent)
  {
    _parent = parent;
  }

  /**
   * The hash of the directory Tree containing the committed content.
   */
  public String getTree()
  {
    return _tree;
  }

  /**
   * The hash of the directory Tree containing the committed content.
   */
  public void setTree(String tree)
  {
    _tree = tree;
  }

  /**
   * Adds a metadata value.
   */
  public void put(String key, String value)
  {
    _attributes.put(key, value);
  }
  
  /**
   * Returns the given metadata value.
   */
  public String get(String key)
  {
    return _attributes.get(key);
  }

  /**
   * Returns the meta-data map for the commit. 
   */
  public Map<String,String> getMetaData()
  {
    return _attributes;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[tree=" + _tree + "]");
  }
}
