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
import com.caucho.xpath.ExprEnvironment;

import org.w3c.dom.Node;

/**
 * matches any node.  The 'any' axis is the root of a match pattern.
 */
public class FromAny extends Axis {
  public FromAny()
  {
    super(null);
  }

  /**
   * All nodes match
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true
   */
  public boolean match(Node node, ExprEnvironment env)
  {
    return true;
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
    Node owner = node.getOwnerDocument();
    if (owner == null)
      return node;
    else
      return owner;
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
   * The root is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    return false;
  }

  /*
   * Should be impossible to call position() with the any pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   */
  public int position(Node node, ExprEnvironment env, AbstractPattern pattern)
  {
    throw new RuntimeException();
  }

  /*
   * Should be impossible to call last() with the any pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   */
  public int count(Node node, ExprEnvironment env, AbstractPattern pattern)
  {
    throw new RuntimeException();
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    return b instanceof FromAny;
  }

  public String toString()
  {
    return "node()";
  }
}
