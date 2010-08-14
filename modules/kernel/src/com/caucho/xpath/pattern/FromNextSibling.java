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

import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * matches following siblings
 */
public class FromNextSibling extends Axis {
  public FromNextSibling(AbstractPattern parent)
  {
    super(parent);

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * matches if we can find a previous sibling that matches the parent.
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

    for (node = node.getPreviousSibling();
         node != null;
         node = node.getPreviousSibling()) {
      if (_parent.match(node, env))
        return true;
    }

    return false;
  }

  /**
   * The count of nodes between the test-node and the axis.
   *
   * @param node the current node
   * @param env the variable environment
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    int count = 1;
    while ((node = node.getPreviousSibling()) != null) {
      if (_parent.match(node, env)) {
        if (--index <= 0) {
          // Test if there are more possible roots.
          for (node = node.getPreviousSibling();
               node != null;
               node = node.getPreviousSibling()) {
            if (_parent.match(node, env)) {
              env.setMorePositions(true);
              break;
            }
          }
          return count;
        }
      }

      if (pattern.match(node, env))
        count++;
    }

    return count;
  }

  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    if (_parent == null)
      return true;
    else
      return _parent.isSingleSelect();
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
    return node.getNextSibling();
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
    return node.getNextSibling();
  }

  public String toString()
  {
    return getPrefix() + "following-sibling::";
  }
}
