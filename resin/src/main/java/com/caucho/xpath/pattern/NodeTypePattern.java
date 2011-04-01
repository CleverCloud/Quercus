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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Matches a node without checking the name.  e.g. * or @*
 */
public class NodeTypePattern extends AbstractPattern {
  public final static int NODE = -2;
  public final static int ANY = -1;

  private AbstractPattern _match;
  private int _nodeType;

  private NodeTypePattern(AbstractPattern parent, int nodeType)
  {
    super(parent);

    _nodeType = nodeType;
  }

  public static AbstractPattern create(AbstractPattern parent, int nodeType)
  {
    if (nodeType == NODE
        && parent instanceof FromParent
        && parent._parent instanceof FromContext) {
      FromContext context = (FromContext) parent._parent;

      return new FromContext(context.getCount() + 1);
    }
    else
      return new NodeTypePattern(parent, nodeType);
  }

  /**
   * The node-type priority is less than nodes.
   */
  public double getPriority()
  {
    if (_parent instanceof Axis
        && _parent.getParent() != null
        && ! (_parent.getParent() instanceof FromRoot)
        && ! (_parent.getParent() instanceof FromAny))
      return 0.5;
    else
      return -0.5;
  } 

  /**
   * Returns the name of the matching node or '*' if many nodes match.
   *
   * <p>The Xsl package uses this to speed template matching.
   */
  public String getNodeName()
  {
    switch (_nodeType) {
    case Node.TEXT_NODE: // cdata, too?
      return "#text";
      
    case Node.DOCUMENT_NODE:
      return "#document";
      
    case Node.COMMENT_NODE:
      return "#comment";
      
    default:
      return "*";
    }
  }

  /**
   * Returns the matching node type.
   */
  public int getNodeType()
  {
    return _nodeType;
  }
  
  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    if (_parent != null)
      return _parent.isStrictlyAscending();
    else
      return true;
  }

  /**
   * Matches if the node type matches.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true if the node matches the node type
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;
    
    if (_nodeType == Node.ATTRIBUTE_NODE) {
      if (node.getNodeType() != Node.ATTRIBUTE_NODE)
        return false;
      else if (XMLNS.equals(node.getNamespaceURI()))
        return false;
    }
    else if (node.getNodeType() == _nodeType) {
    }
    else if (_nodeType == ANY) {
    }
    else if (_nodeType == NODE && ! (node instanceof Document)) {
    }
    else
      return false;
    
    return _parent == null || _parent.match(node, env);
  }

  /**
   * Copies the node matching portion of the pattern, i.e. the section
   * only applying to the current axis.
   */
  public AbstractPattern copyPosition()
  {
    if (_match == null) {
      AbstractPattern parent = null;
      if (_parent != null)
        parent = _parent.copyPosition();
      _match = new NodeTypePattern(parent, _nodeType);
    }
    
    return _match;
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof NodeTypePattern))
      return false;

    NodeTypePattern bPattern = (NodeTypePattern) b;
    
    return (_nodeType == bPattern._nodeType
            && (_parent == bPattern._parent
                || (_parent != null && _parent.equals(bPattern._parent))));
  }

  /**
   * Returns the printable representation of the pattern.
   */
  public String toString()
  {
    String prefix;
    
    if (_parent == null)
      prefix = "";
    else if (_parent instanceof FromChildren)
      prefix = _parent.getPrefix();
    else
      prefix = _parent.toString();
    
    switch (_nodeType) {
    case ANY: 
      if (! (_parent instanceof FromSelf))
        return prefix + "node()";
      else if (_parent._parent == null)
        return ".";
      else
        return  _parent.getPrefix() + ".";

    case NODE: 
      return prefix + "node()";

    case Node.PROCESSING_INSTRUCTION_NODE:
      return prefix + "pi()";
    case Node.ATTRIBUTE_NODE:
      return prefix + "*";
    case Node.ELEMENT_NODE:
      return prefix + "*";
    case Node.COMMENT_NODE:
      return prefix + "comment()";
    case Node.TEXT_NODE:
      return prefix + "text()";
    case Node.ENTITY_REFERENCE_NODE:
      return prefix + "er()";
    default:
      return super.toString();
    }
  }
}
