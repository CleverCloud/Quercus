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
 * Matches a named node, like foo:para or @foo:id when the prefix
 * maps to a namespace.
 */
public class NSNamePattern extends AbstractPattern {
  private NSNamePattern _match;
  
  private String _namespace;
  private String _local;
  private int _nodeType;

  /**
   * Creates the namespace pattern.
   *
   * @param parent the parent pattern.
   * @param namespace the node's namespace URL.
   * @param local the node's local name.
   * @param nodeType the node type to match.
   */
  public NSNamePattern(AbstractPattern parent, String namespace, String local,
                       int nodeType)
  {
    super(parent);

    _nodeType = nodeType;

    if (namespace != null)
      _namespace = namespace.intern();
    else
      _namespace = "";
    
    _local = local.intern();
  }

  /**
   * Nodes have a higher default priority.
   */
  public double getPriority()
  {
    return 0;
  }

  /**
   * Matches if the namespace matches and the local name matches.
   *
   * @param node the current node
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

    String uri = node.getNamespaceURI();
    if (uri == null || ! node.getNamespaceURI().equals(_namespace))
      return false;
    
    else if (! node.getLocalName().equals(_local))
      return false;

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
      
      _match = new NSNamePattern(parent, _namespace, _local, _nodeType);
    }
    
    return _match;
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof NSNamePattern))
      return false;

    NSNamePattern bPattern = (NSNamePattern) b;
    
    return (_nodeType == bPattern._nodeType
            && _local.equals(bPattern._local)
            && _namespace.equals(bPattern._namespace)
            && (_parent == bPattern._parent
                || (_parent != null && _parent.equals(bPattern._parent))));
  }

  public String toString()
  {
    return _parent + "{" + _namespace + "}" + _local;
  }
}
