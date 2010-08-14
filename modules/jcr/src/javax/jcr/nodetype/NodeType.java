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

package javax.jcr.nodetype;

import javax.jcr.Value;

/**
 * Represents the type of a node.
 */
public interface NodeType {
  /**
   * Returns the node type's name.
   */
  public String getName();

  /**
   * Returns true for a mixing node type.
   */
  public boolean isMixin();

  /**
   * Returns true if this node type has orderable children.
   */
  public boolean hasOrderableChildNodes();

  /**
   * Returns the main item type.
   */
  public String getPrimaryItemName();

  /**
   * Returns all supertypes of the node type.
   */
  public NodeType[] getSupertypes();

  /**
   * Returns the immediate supertypes of the node type.
   */
  public NodeType[] getDeclaredSupertypes();

  /**
   * Returns true if the given node type is valid.
   */
  public boolean isNodeType(String nodeTypeName);

  /**
   * Returns the properties defined for the node.
   */
  public PropertyDefinition[] getPropertyDefinitions();
  
  /**
   * Returns the immediate properties defined for the node.
   */
  public PropertyDefinition[] getDeclaredPropertyDefinitions();
  
  /**
   * Returns the allowed children.
   */
  public NodeDefinition[] getChildNodeDefinitions();
  
  /**
   * Returns the direct children.
   */
  public NodeDefinition[] getDeclaredChildNodeDefinitions();

  /**
   * Returns true if the given property can be set with the given value.
   */
  public boolean canSetProperty(String propertyName, Value value);
  
  /**
   * Returns true if the given property can be set with the given value.
   */
  public boolean canSetProperty(String propertyName, Value[] values);
  
  /**
   * Returns true if this node type can add a child node.
   */
  public boolean canAddChildNode(String childNodeName);
  
  /**
   * Returns true if this node type can add a child node with the given type..
   */
  public boolean canAddChildNode(String childNodeName, String nodeTypeName);
  
  /**
   * Returns true if this node type can remove an item.
   */
  public boolean canRemoveItem(String itemName);
}
