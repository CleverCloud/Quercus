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
 * Implements the ancestor:: axis.
 */
public class FromAncestors extends Axis {
  private boolean _self;

  public FromAncestors(AbstractPattern parent, boolean self)
  {
    super(parent)
      ;
    _self = self;

    if (parent == null)
      throw new RuntimeException();
  }
  /**
   * Matches if a descendant matches the parent pattern.
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

    Node lastNode = lastDescendantNode(node);
    
    if (! _self)
      node = XmlUtil.getNext(node);
    
    for (;
         node != null && node != lastNode;
         node = XmlUtil.getNext(node)) {
      if (_parent.match(node, env))
        return true;
    }
    
    return false;
  }

  public boolean isAscending()
  {
    return false;
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
    if (_self)
      return node;
    else
      return node.getParentNode();
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param last the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node last)
  {
    return (node == null) ? null : node.getParentNode();
  }

  /**
   * The ancestor position is the number of matching nodes between it
   * and an axis-context.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int index = env.getPositionIndex();

    Node lastNode = lastDescendantNode(node);

    Node axis = _self ? node : XmlUtil.getNext(node);

    for (; index >= 0; index--) {
      for (; axis != lastNode && axis != null; axis = XmlUtil.getNext(axis)) {
        if (_parent.match(axis, env))
          break;
      }

      if (index > 0)
        axis = XmlUtil.getNext(axis);
    }

    if (axis == lastNode)
      return 0;

    Node next = XmlUtil.getNext(axis);
    for (; next != lastNode; next = XmlUtil.getNext(next)) {
      if (_parent.match(next, env)) {
        env.setMorePositions(true);
        break;
      }
    }

    Node a1 = axis;
    
    if (! _self && axis != null)
      axis = axis.getParentNode();

    int count = 0;
    for (; axis != null; axis = axis.getParentNode()) {
      if (pattern.match(axis, env))
        count++;

      if (node == axis)
        break;
    }

    return count;
  }

  /**
   * Returns the last node in the selection order.
   *
   * @param node the current node
   *
   * @return the last node
   */
  private Node lastDescendantNode(Node node)
  {
    Node last = node;

    for (;
         last != null && last.getNextSibling() == null;
         last = last.getParentNode()) {
    }

    return last != null ? last.getNextSibling() : null;
  }

  /**
   * counts the number of matching ancestors from the axis context
   */
  public int count(Node node, Env env, AbstractPattern pattern)
  {
    throw new RuntimeException();
  }

  public String toString()
  {
    return getPrefix() + (_self ? "ancestor-or-self::" : "ancestor::");
  }
}
