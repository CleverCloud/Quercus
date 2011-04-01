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

import com.caucho.util.CharBuffer;
import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;

import org.w3c.dom.Node;

/**
 * Matches the node context.
 */
public class FromContext extends Axis {
  // ancestor count from the specified context where this virtual context is
  int _ancestorCount;
  
  public FromContext()
  {
    super(null);
  }

  /**
   * Creates a new context pattern.  Using the ancestorCount,
   * FromContext collapses parent patterns into a single FromContext
   * pattern.  For example, context()/../.. is collapsed to FromContext(2).
   *
   * @param ancestorCount number of parents where the real context starts
   */
  public FromContext(int ancestorCount)
  {
    super(null);

    _ancestorCount = ancestorCount;
  }

  public int getCount()
  {
    return _ancestorCount;
  }

  /**
   * Matches the node context
   *
   * @param node the current node
   * @param env the variable environment
   *
   * @return true if the node is the context node.
   */
  public boolean match(Node node, ExprEnvironment env)
  {
    Node context = env.getContextNode();

    for (int i = 0; i < _ancestorCount && context != null; i++)
      context = context.getParentNode();
    
    return (node == context);
  }

  /**
   * The context is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    return true;
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    return true;
  }
  
  /**
   * There is only a single node in the context axis.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return 1
   */
  public int position(Node node, Env env, AbstractPattern pattern)
  {
    return 1;
  }
  
  /**
   * There is only a single node in the context axis.
   *
   * @param node the current node
   * @param env the variable environment
   * @param pattern the position pattern
   *
   * @return 1
   */
  public int count(Node node, Env env, AbstractPattern pattern)
  {
    return 1;
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   * @param env the current environment
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
  {
    for (int i = 0; node != null && i < _ancestorCount; i++)
      node = node.getParentNode();

    return node;
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

  /**
   * Returns true if the two patterns are equal.
   */
  public boolean equals(Object b)
  {
    return (b instanceof FromContext &&
            _ancestorCount == ((FromContext) b)._ancestorCount);
  }

  /**
   * Prints the pattern representing the context.
   */
  public String toString()
  {
    if (_ancestorCount == 0)
      return "context()";
    
    CharBuffer cb = CharBuffer.allocate();

    cb.append("..");

    for (int i = 1; i < _ancestorCount; i++) {
      cb.append("/..");
    }

    return cb.close();
  }
}
