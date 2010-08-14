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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;

/**
 * Represents a node type.
 */
public class BaseNodeType implements NodeType {
  public static final BaseNodeType MIX_REFERENCEABLE;
  
  public static final BaseNodeType NT_BASE;
  
  public static final BaseNodeType NT_HIERARCHY_NODE;
  public static final BaseNodeType NT_FILE;
  public static final BaseNodeType NT_FOLDER;
  public static final BaseNodeType NT_RESOURCE;
  
  private final String _name;
  
  private final NodeType []_declaredSuperTypes;
  
  private final NodeType []_superTypes;

  private boolean _isMixin;
  
  private boolean _hasOrderableChildNodes;

  private String _primaryItemName;
  
  private ArrayList<PropertyDefinition> _properties
    = new ArrayList<PropertyDefinition>();

  private ArrayList<NodeDefinition> _childNodes
    = new ArrayList<NodeDefinition>();

  public BaseNodeType(String name,
                      NodeType []declaredSuperTypes)
  {
    _name = name;
    _declaredSuperTypes = declaredSuperTypes;

    ArrayList<NodeType> superTypes = new ArrayList<NodeType>();

    for (NodeType type : declaredSuperTypes) {
      if (! superTypes.contains(type))
        superTypes.add(type);

      for (NodeType parentType : type.getSupertypes()) {
        if (! superTypes.contains(parentType))
          superTypes.add(parentType);
      }
    }

    _superTypes = new NodeType[superTypes.size()];
    superTypes.toArray(_superTypes);
  }

  public BaseNodeType(String name, NodeType superType)
  {
    this(name, new NodeType[] { superType });
  }
  
  /**
   * Returns the node type's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true for a mixin node type.
   */
  public boolean isMixin()
  {
    return _isMixin;
  }

  /**
   * Set true for a mixin node type.
   */
  public void setMixin(boolean isMixin)
  {
    _isMixin = isMixin;
  }

  /**
   * Returns true if this node type has orderable children.
   */
  public boolean hasOrderableChildNodes()
  {
    return _hasOrderableChildNodes;
  }

  /**
   * SEt true if this node type has orderable children.
   */
  public void setHasOrderableChildNodes(boolean hasOrder)
  {
    _hasOrderableChildNodes = hasOrder;
  }

  /**
   * Returns the main item name.
   */
  public String getPrimaryItemName()
  {
    return _primaryItemName;
  }

  /**
   * Returns the main item type.
   */
  public void setPrimaryItemName(String name)
  {
    _primaryItemName = name;
  }

  /**
   * Returns all supertypes of the node type.
   */
  public NodeType[] getSupertypes()
  {
    return _superTypes;
  }

  /**
   * Returns the immediate supertypes of the node type.
   */
  public NodeType[] getDeclaredSupertypes()
  {
    return _declaredSuperTypes;
  }

  /**
   * Returns true if the given node type is valid.
   */
  public boolean isNodeType(String nodeTypeName)
  {
    return false;
  }

  /**
   * Returns the properties defined for the node.
   */
  public PropertyDefinition[] getPropertyDefinitions()
  {
    return getDeclaredPropertyDefinitions();
  }
  
  /**
   * Returns the immediate properties defined for the node.
   */
  public PropertyDefinition[] getDeclaredPropertyDefinitions()
  {
    PropertyDefinition []props;

    props = new PropertyDefinition[_properties.size()];
    _properties.toArray(props);
    
    return props;
  }

  /**
   * Adds a property definition.
   */
  public void addProperty(PropertyDefinition prop)
  {
    _properties.add(prop);
  }
  
  /**
   * Returns the allowed children.
   */
  public NodeDefinition[] getChildNodeDefinitions()
  {
    return getDeclaredChildNodeDefinitions();
  }
  
  /**
   * Returns the direct children.
   */
  public NodeDefinition[] getDeclaredChildNodeDefinitions()
  {
    NodeDefinition []children;

    children = new NodeDefinition[_childNodes.size()];
    _childNodes.toArray(children);
    
    return children;
  }

  /**
   * Adds a child node.
   */
  public void addChildNode(NodeDefinition child)
  {
    _childNodes.add(child);
  }

  /**
   * Returns true if the given property can be set with the given value.
   */
  public boolean canSetProperty(String propertyName, Value value)
  {
    return false;
  }
  
  /**
   * Returns true if the given property can be set with the given value.
   */
  public boolean canSetProperty(String propertyName, Value[] values)
  {
    return false;
  }
  
  /**
   * Returns true if this node type can add a child node.
   */
  public boolean canAddChildNode(String childNodeName)
  {
    return false;
  }
  
  /**
   * Returns true if this node type can add a child node with the given type..
   */
  public boolean canAddChildNode(String childNodeName, String nodeTypeName)
  {
    return false;
  }
  
  /**
   * Returns true if this node type can remove an item.
   */
  public boolean canRemoveItem(String itemName)
  {
    return false;
  }

  public int hashCode()
  {
    return getName().hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof BaseNodeType))
      return false;

    BaseNodeType nodeType = (BaseNodeType) o;

    return getName().equals(nodeType.getName());
  }

  public String toString()
  {
    return "BaseNodeType[" + getName() + "]";
  }

  static {
    BasePropertyDefinition prop;
    BaseNodeDefinition child;

    // mix:referenceable
    
    MIX_REFERENCEABLE = new BaseNodeType("mix:referenceable", new NodeType[0]);

    // nt:base

    NT_BASE = new BaseNodeType("nt:base", new NodeType[0]);

    prop = new BasePropertyDefinition("jcr:primaryType", NT_BASE,
                                      PropertyType.NAME);
    prop.setAutoCreated(true);
    prop.setMandatory(true);
    prop.setOnParentVersion(OnParentVersionAction.COMPUTE);
    prop.setProtected(true);
    NT_BASE.addProperty(prop);

    prop = new BasePropertyDefinition("jcr:mixinTypes", NT_BASE,
                                      PropertyType.NAME);
    prop.setOnParentVersion(OnParentVersionAction.COMPUTE);
    prop.setProtected(true);
    prop.setMultiple(true);
    NT_BASE.addProperty(prop);

    // nt:unstructured - XXX: skip

    // nt:hierarchyNode

    NT_HIERARCHY_NODE = new BaseNodeType("nt:hierarchyNode", NT_BASE);

    prop = new BasePropertyDefinition("jcr:created", NT_HIERARCHY_NODE,
                                      PropertyType.DATE);
    prop.setAutoCreated(true);
    prop.setOnParentVersion(OnParentVersionAction.INITIALIZE);
    prop.setProtected(true);
    NT_HIERARCHY_NODE.addProperty(prop);

    // nt:file

    NT_FILE = new BaseNodeType("nt:file", NT_HIERARCHY_NODE);
    NT_FILE.setPrimaryItemName("jcr:content");

    child = new BaseNodeDefinition("jcr:content", NT_FILE);
    child.setRequiredPrimaryTypes(new NodeType[] { NT_BASE });
    child.setMandatory(true);
    NT_FILE.addChildNode(child);

    // nt:linkedFile - XXX: skip

    // nt:folder

    NT_FOLDER = new BaseNodeType("nt:folder", NT_HIERARCHY_NODE);

    child = new BaseNodeDefinition("*", NT_FOLDER);
    child.setRequiredPrimaryTypes(new NodeType[] { NT_HIERARCHY_NODE });
    child.setOnParentVersion(OnParentVersionAction.VERSION);
    
    // nt:resource
    
    NT_RESOURCE = new BaseNodeType("nt:resource",
                                 new NodeType[] { NT_HIERARCHY_NODE,
                                                  MIX_REFERENCEABLE });
    NT_RESOURCE.setPrimaryItemName("jcr:data");

    prop = new BasePropertyDefinition("jcr:encoding", NT_RESOURCE,
                                      PropertyType.STRING);
    NT_FOLDER.addProperty(prop);

    prop = new BasePropertyDefinition("jcr:mimeType", NT_RESOURCE,
                                      PropertyType.STRING);
    prop.setMandatory(true);
    NT_FOLDER.addProperty(prop);

    prop = new BasePropertyDefinition("jcr:data", NT_RESOURCE,
                                      PropertyType.BINARY);
    prop.setMandatory(true);
    NT_FOLDER.addProperty(prop);

    prop = new BasePropertyDefinition("jcr:lastModified", NT_RESOURCE,
                                      PropertyType.DATE);
    prop.setMandatory(true);
    prop.setOnParentVersion(OnParentVersionAction.IGNORE);
    NT_FOLDER.addProperty(prop);
  }
}
