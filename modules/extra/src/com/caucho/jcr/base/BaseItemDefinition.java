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

package com.caucho.jcr.base;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

/**
 * Definition for types of items.
 */
public class BaseItemDefinition implements ItemDefinition {
  private final NodeType _nodeType;
  private final String _name;

  private boolean _isAutoCreated;
  private boolean _isMandatory;
  private int _onParentVersion = OnParentVersionAction.COPY;
  private boolean _isProtected;

  public BaseItemDefinition(String name, NodeType nodeType)
  {
    _name = name;
    _nodeType = nodeType;
  }
  
  /**
   * Returns the declaring node type.
   */
  public NodeType getDeclaringNodeType()
  {
    return _nodeType;
  }

  /**
   * Returns the item definition name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true if this item is automatically created by the
   * repository.
   */
  public boolean isAutoCreated()
  {
    return _isAutoCreated;
  }

  /**
   * Set if the item is auto-created.
   */
  public void setAutoCreated(boolean isAutoCreated)
  {
    _isAutoCreated = isAutoCreated;
  }

  /**
   * Returns true if this item always exists.
   */
  public boolean isMandatory()
  {
    return _isMandatory;
  }

  /**
   * Set if the item is mandatory
   */
  public void setMandatory(boolean isMandatory)
  {
    _isMandatory = isMandatory;
  }

  /**
   * Returns the action when the parent is versioned.
   */
  public int getOnParentVersion()
  {
    return _onParentVersion;
  }

  /**
   * Set the action for the parent versioning.
   */
  public void setOnParentVersion(int onParentVersion)
  {
    _onParentVersion = onParentVersion;
  }

  /**
   * Returns true for a read-only item.
   */
  public boolean isProtected()
  {
    return _isProtected;
  }

  /**
   * Set true for a read-only item.
   */
  public void setProtected(boolean isProtected)
  {
    _isProtected = isProtected;
  }
}
