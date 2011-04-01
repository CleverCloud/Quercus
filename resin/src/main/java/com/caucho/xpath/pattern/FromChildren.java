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
 * matches child nodes.
 */
public class FromChildren extends Axis {
  public FromChildren(AbstractPattern parent)
  {
    super(parent);
  }

  /**
   * Matches all nodes except attributes and tests the parent pattern
   * with the parent node.
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true if the pattern matches
   */
  @Override
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    return _parent == null || _parent.match(node.getParentNode(), env);
  }
  
  /**
   * The position of the child is the count of previous siblings
   * matching the pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return the node's position.
   */
  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int count = 1;
    
    for (node = node.getPreviousSibling();
         node != null;
         node = node.getPreviousSibling()) {
      if (pattern == null || pattern.match(node, env))
        count++;
    }

    return count;
  }
  
  /**
   * Counts all siblings matching the pattern.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return the count of the node list.
   */
  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int count = 0;

    for (node = node.getParentNode().getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      if (pattern.match(node, env))
        count++;
    }

    return count;
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
    return node.getFirstChild();
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param node the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node lastNode)
  {
    return node.getNextSibling();
  }
  
  /**
   * Returns true if the pattern nodes on a single level.
   */
  boolean isSingleLevel()
  {
    return _parent == null || _parent.isSingleLevel();
  }

  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    if (_parent == null)
      return true;
    else {
      return _parent.isStrictlyAscending();
    }
  }

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    if (! (b instanceof FromChildren))
      return false;

    FromChildren bPattern = (FromChildren) b;
    
    return (_parent == bPattern._parent
            || (_parent != null && _parent.equals(bPattern._parent)));
  }

  public String toString()
  {
    return getPrefix() + "child::";
  }
}
