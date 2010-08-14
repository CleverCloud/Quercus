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
 * Matches nodes preceding the current node, not counting descendants.
 */
public class FromPrevious extends Axis {
  public FromPrevious(AbstractPattern parent)
  {
    super(parent);

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * matches if we can find a following node matching the parent pattern.
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    return getAxisContext(node, env, node) != null;
  }

  /**
   * The iterator is in reverse document order.
   */
  public boolean isAscending()
  {
    return false;
  }

  /**
   * Calculates position by counting next nodes matching the pattern.
   *
   * The axis is a previous node matching the parent pattern.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    boolean hasMatch = true;
    Node axis = XmlUtil.getNext(node);
    
    for (; axis != null; axis = XmlUtil.getNext(axis)) {
      if (hasMatch && _parent.match(axis, env)) {
        boolean hasParent = false;
        for (Node ptr = node.getParentNode();
             ptr != null;
             ptr = ptr.getParentNode()) {
          if (ptr == axis) {
            hasParent = true;
            break;
          }
        }
        
        if (! hasParent && --index < 0) {
          hasMatch = false;
          break;
        }
      }
      
      if (! hasMatch && pattern.match(axis, env))
        hasMatch = true;
    }
    
    int count = 1;
    Node ptr;
    for (ptr = axis;
         ptr != null && ptr.getNextSibling() == null;
         ptr = ptr.getParentNode()) {
    }
    
    for (ptr = XmlUtil.getPrevious(axis);
         ptr != null && ptr != node;
         ptr = XmlUtil.getPrevious(ptr)) {
      if (pattern.match(ptr, env)) {
        boolean hasParent = false;
        for (Node n = node; n != null; n = n.getParentNode()) {
          if (n == ptr) {
            hasParent = true;
            break;
          }
        }

        if (! hasParent)
          count++;
      }
    }

    for (; axis != null; axis = XmlUtil.getNext(axis)) {
      if (_parent.match(axis, env)) {
        env.setMorePositions(true);
        break;
      }
    }

    return count;
  }

  /**
   * counts matching nodes preceding the axis context.
   *
   * count() walks backwards from the axis context.
   */
  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    Node axis = getAxisContext(node, env, node);
    for (; index > 0; index--)
      axis = getAxisContext(axis, env, node);

    if (getAxisContext(axis, env, node) != null)
      env.setMorePositions(true);

    int count = 0;
    for (Node ptr = axis;
         ptr != null;
         ptr = XmlUtil.getPrevious(ptr)) {
      if (pattern.match(ptr, env) && ! isDescendant(ptr, axis))
        count++;
    }

    return count;
  }
  
  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isUnique()
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
    return XmlUtil.getPrevious(node);
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param lastNode the original node (for checking ancestors)
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    loop:
    while (node != null) {
      node = XmlUtil.getPrevious(node);

      for (Node ptr = lastNode; ptr != null; ptr = ptr.getParentNode()) {
        if (ptr == node)
          continue loop;
      }

      return node;
    }

    return null;
  }

  /**
   * Returns the last node in the selection order.
   *
   * @param node the current node
   *
   * @return the last node
   */
  public Node lastNode(Node node)
  {
    return node;
  }

  /**
   * Returns the next AxisContext.  The axis context is the first following
   * node matching the parent pattern.
   */
  private Node getAxisContext(Node axis, ExprEnvironment env, Node node)
    throws XPathException
  {
    if (axis == null)
      return null;

    while ((axis = XmlUtil.getNext(axis)) != null) {
      if (! isDescendant(axis, node) && _parent.match(axis, env))
        return axis;
    }
    
    return null;
  }

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
    return getPrefix() + "preceding::";
  }
}

