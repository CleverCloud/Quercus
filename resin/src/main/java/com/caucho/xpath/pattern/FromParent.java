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

public class FromParent extends Axis {
  public FromParent(AbstractPattern parent)
  {
    super(parent);

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * Matches if a child matches the parent pattern.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true if the pattern matches
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    if (node.getNodeType() != node.ELEMENT_NODE &&
        node.getNodeType() != node.DOCUMENT_NODE)
      return false;

    for (node = node.getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      if (_parent.match(node, env))
        return true;
    }
    
    return false;
  }

  /**
   * The parent is strictly ascending if there's only one possible selection.
   */
  public boolean isStrictlyAscending()
  {
    return isSingleSelect();
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    return _parent == null ? true : _parent.isSingleSelect();
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
  {
    return node.getParentNode();
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param lastNode the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    return null;
  }

  public String toString()
  {
    return getPrefix() + "parent::";
  }
}
