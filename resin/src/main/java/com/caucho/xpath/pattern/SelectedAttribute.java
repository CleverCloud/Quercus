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
public class SelectedAttribute extends SelectedNode {
  /**
   * Creates new selected node, calculating the index.
   *
   * @param node the underlying DOM node.
   */
  public SelectedAttribute(Node node)
  {
    super(node);

    _level = 0;
    for (Node ptr = node; ptr != null; ptr = ptr.getPreviousSibling())
      _level--;
  }

  /**
   * Returns the node's index
   */
  public int compareTo(SelectedNode b)
  {
    Node aPtr = getNode();
    Node bPtr = b.getNode();

    int aDepth = _depth;
    int bDepth = b._depth;

    if (aPtr == bPtr)
      return 0;
    
    if (b instanceof SelectedAttribute) {
      if (aPtr.getParentNode() == bPtr.getParentNode()) {
        // Using getNextSibling() because QAttr doesn't
        // implement getPreviousSibling
        for (; bPtr != null; bPtr = bPtr.getNextSibling()) {
          if (aPtr == bPtr)
            return 1;
        }

        return -1;
      }
      else
        return compareTo(aPtr.getParentNode(), aDepth - 1,
                         bPtr.getParentNode(), bDepth - 1);
    }
    else if (bPtr.getParentNode() == aPtr.getParentNode())
      return -1;

    return compareTo(aPtr, aDepth, bPtr, bDepth);
  }
}
