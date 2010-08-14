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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * Definition for a node type.
 */
public class BaseNodeDefinition
  extends BaseItemDefinition
  implements NodeDefinition {

  private NodeType []_requiredPrimaryTypes = new NodeType[0];
  
  private NodeType _defaultPrimaryType;
  
  private boolean _allowsSameNameSiblings;
  
  public BaseNodeDefinition(String name, NodeType type)
  {
    super(name, type);
  }
  
  /**
   * Returns the node types required as children.
   */
  public NodeType[] getRequiredPrimaryTypes()
  {
    return _requiredPrimaryTypes;
  }
  
  /**
   * the node types required as children.
   */
  public void setRequiredPrimaryTypes(NodeType []types)
  {
    _requiredPrimaryTypes = types;
  }
  
  /**
   * Returns the default primary type.
   */
  public NodeType getDefaultPrimaryType()
  {
    return _defaultPrimaryType;
  }
  
  /**
   * Sets the default primary type.
   */
  public void setDefaultPrimaryType(NodeType type)
  {
    _defaultPrimaryType = type;
  }

  /**
   * Returns true if siblings of the same name are allowed.
   */
  public boolean allowsSameNameSiblings()
  {
    return _allowsSameNameSiblings;
  }

  /**
   * Set true if siblings of the same name are allowed.
   */
  public void setAllowsSameNameSiblings(boolean isAllowed)
  {
    _allowsSameNameSiblings = isAllowed;
  }
}
