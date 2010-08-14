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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Represents any of the axis patterns, i.e. those patterns that
 * select many nodes from a single node.
 */
abstract class Axis extends AbstractPattern {
  Axis(AbstractPattern parent)
  {
    super(parent);
  }

  /**
   * Calculates the position of the node in its context.
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
    return 1;
  }

  /**
   * Counts the nodes within the axis matching the pattern.
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
    return 1;
  }

  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the variable environment
   * @param match the axis match pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env,
                                         AbstractPattern match)
    throws XPathException
  {
    if (_parent == null)
      return new AxisIterator(null, this, node, env, match);
    else if (_parent instanceof FromRoot) {
      if (node instanceof Document)
        return new AxisIterator(null, this, node, env, match);
      else if (node != null)
        return new AxisIterator(null, this, node.getOwnerDocument(),
                                env, match);
    }

    NodeIterator parentIter;
    parentIter = _parent.createNodeIterator(node, env, _parent.copyPosition());

    return new AxisIterator(parentIter, this, null, env, match);
  }

  /**
   * Returns the node itself for the axis.
   */
  public AbstractPattern copyAxis()
  {
    return this;
  }

  /**
   * Returns null since the axis isn't part of the position pattern.
   */
  public AbstractPattern copyPosition()
  {
    return null;
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    return false;
  }
  
  /**
   * Returns true if the pattern is strictly ascending.
   */
  public boolean isStrictlyAscending()
  {
    return isSingleLevel();
  }
  
  /**
   * Returns true if the pattern's selector returns unique nodes.
   */
  public boolean isUnique()
  {
    return isStrictlyAscending();
  }
}
