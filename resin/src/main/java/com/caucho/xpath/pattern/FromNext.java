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

import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Matches nodes following the current node in the current document.
 */
public class FromNext extends Axis {
  public FromNext(AbstractPattern parent)
  {
    super(parent);

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * Matches if there is a previous node matching the parent pattern.
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

    return getAxisContext(node, env) != null;
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
    for (; node != null; node = node.getParentNode())
      if (node.getNextSibling() != null)
        return node.getNextSibling();
    
    return null;
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
    return XmlUtil.getNext(node);
  }

  /**
   * Calculates position by counting previous nodes matching the pattern.
   *
   * The axis is a previous node matching the parent pattern.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    int count = 1;
    for (Node ptr = XmlUtil.getPrevious(node);
         ptr != null;
         ptr = XmlUtil.getPrevious(ptr)) {
      if (_parent.match(ptr, env)) {
        boolean isParent = false;
        
        for (Node n = node; n != null; n = n.getParentNode()) {
          if (n == ptr) {
            isParent = true;
            break;
          }
        }
        if (! isParent && --index < 0) {
          for (; ptr != null; ptr = XmlUtil.getPrevious(ptr)) {
            if (_parent.match(ptr, env)) {
              env.setMorePositions(true);
              break;
            }
          }
          return count;
        }
      }
      if (pattern.match(ptr, env))
        count++;
    }

    return count;
  }

  /**
   * An axis context is a previous node matching the parent pattern.
   */
  private Node getAxisContext(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return null;

    for (Node prev = node;
         prev != null;
         prev = XmlUtil.getPrevious(prev)) {
      if (! isDescendant(node, prev) && _parent.match(prev, env))
        return prev;
    }
    
    return null;
  }

  /**
   * Returns true if descendant is a descendant of node.
   */
  private boolean isDescendant(Node descendant, Node node)
  {
    for (;
         descendant != node && descendant != null;
         descendant = descendant.getParentNode()) {
    }

    return descendant != null;
  }

  public String toString()
  {
    return getPrefix() + "following::";
  }
}
