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

import org.w3c.dom.Node;

/**
 * Represents a selected node.
 */
public class SelectedNode {
  protected Node _node;
  
  protected int _depth;
  protected int _level;

  /**
   * Creates new selected node, calculating the index.
   *
   * @param node the underlying DOM node.
   */
  public SelectedNode(Node node)
  {
    _node = node;

    _depth = 0;
    for (Node ptr = node; ptr != null; ptr = ptr.getParentNode()) {
      _depth++;
    }

    _level = 0;
    for (Node ptr = node; ptr != null; ptr = ptr.getPreviousSibling()) {
      _level++;
    }
  }

  /**
   * Returns the underlying DOM node.
   */
  public Node getNode()
  {
    return _node;
  }

  /**
   * Returns the node's index
   */
  public int compareTo(SelectedNode b)
  {
    int aDepth = _depth;
    int bDepth = b._depth;

    Node aPtr = getNode();
    Node bPtr = b.getNode();

    if (aDepth == bDepth && aPtr.getParentNode() == bPtr.getParentNode())
      return _level - b._level;

    return compareTo(aPtr, aDepth, bPtr, bDepth);
  }

  /**
   * Returns the node's index
   */
  static int compareTo(Node aPtr, int aDepth, Node bPtr, int bDepth)
  {
    for (int depth = aDepth; bDepth < depth; depth--)
      aPtr = aPtr.getParentNode();
    
    for (int depth = bDepth; aDepth < depth; depth--)
      bPtr = bPtr.getParentNode();

    Node aParent;
    Node bParent;
    while ((aParent = aPtr.getParentNode()) !=
           (bParent = bPtr.getParentNode())) {
      aPtr = aParent;
      bPtr = bParent;
    }

    if (aPtr == bPtr)
      return aDepth - bDepth;

    for (; aPtr != null; aPtr = aPtr.getPreviousSibling()) {
      if (aPtr == bPtr)
        return 1;
    }

    return -1;
  }
}

