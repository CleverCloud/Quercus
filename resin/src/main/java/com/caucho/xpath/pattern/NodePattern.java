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

package com.caucho.xpath.pattern;

import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Matches a named node, like para or @id.
 */
public class NodePattern extends AbstractPattern {
  private NodePattern _match;
  
  private String _tag;
  private int _nodeType;

  /**
   * Creates a new node-matching pattern.
   */
  public NodePattern(AbstractPattern parent, String tag, int nodeType)
  {
    super(parent);

    _tag = tag.intern();
    _nodeType = nodeType;
  }

  /**
   * All priorities are based on the node priority.
   */
  public double getPriority()
  {
    if (_parent == null ||
        _parent instanceof FromChildren &&
        (_parent._parent instanceof FromAny ||
         _parent._parent instanceof FromContext))
      return 0;
    else
      return 0.5;
  } 

  /**
   * Returns the pattern's matching node name.
   */
  public String getNodeName()
  {
    return _tag;
  }

  /**
   * matches if the node type matches and the node name matches.
   *
   * @param node the node to test.
   * @param env the variable environment
   *
   * @return true if the node matches
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    if (node.getNodeType() != _nodeType)
      return false;
    else if (! node.getNodeName().equals(_tag))
      return false;
    else if (node.getNamespaceURI() != null
             && ! "".equals(node.getNamespaceURI())) {
      return false;
    }
    else if (_parent != null && ! _parent.match(node, env))
      return false;

    return true;
  }

  /**
   * Copies the position (non-axis) portion of the pattern.
   */
  public AbstractPattern copyPosition()
  {
    if (_match == null) {
      AbstractPattern parent = null;
      if (_parent != null)
        parent = _parent.copyPosition();
      _match = new NodePattern(parent, _tag, _nodeType);
    }
    
    return _match;
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof NodePattern))
      return false;

    NodePattern bPattern = (NodePattern) b;
    
    return (_nodeType == bPattern._nodeType
            && _tag.equals(bPattern._tag)
            && (_parent == bPattern._parent
                || (_parent != null && _parent.equals(bPattern._parent))));
  }

  /**
   * Converts the pattern back to its 
   */
  public String toString()
  {
    String prefix;
    
    if (_parent == null || _parent instanceof FromAny)
      prefix = "";
    else if (_parent instanceof FromChildren)
      prefix = _parent.getPrefix();
    else
      prefix = _parent.toString();

    switch (_nodeType) {
    case Node.PROCESSING_INSTRUCTION_NODE:
      return prefix + "pi('" + _tag + "')";
      
    case Node.ATTRIBUTE_NODE:
      return prefix + _tag;
      
    case Node.ELEMENT_NODE:
      return prefix + _tag;

    default:
      return super.toString();
    }
  }
}
