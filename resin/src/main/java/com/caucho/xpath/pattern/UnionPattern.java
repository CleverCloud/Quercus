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
 * Implementation of the 'a|b' pattern.  It's made public 
 * so XSLT can separate the left and right sides.
 */
public class UnionPattern extends AbstractPattern {
  private AbstractPattern _left;
  private AbstractPattern _right;

  public UnionPattern(AbstractPattern left, AbstractPattern right)
  {
    super(null);

    if (right.getParent() instanceof FromAttributes &&
        left.getParent() instanceof FromChildren) {
      _left = right;
      _right = left;
    }
    else {
      _left = left;
      _right = right;
    }
  }

  /**
   * Match if either pattern matches.
   *
   * @param node the node to test
   * @param env the variable environment
   *
   * @return true if the pattern matches.
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    return (_left.match(node, env) || _right.match(node, env));
  }

  /**
   * Returns true if the nodes are ascending.
   */
  public boolean isStrictlyAscending()
  {
    // @*|node()
    if (_left.getParent() instanceof FromAttributes &&
        _right.getParent() instanceof FromChildren)
      return true;
    else
      return false;
  }


  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the xpath environment
   * @param match the axis match pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env, 
                                         AbstractPattern match)
    throws XPathException
  {
    NodeIterator leftIter = _left.createNodeIterator(node, env,
                                                     _left.copyPosition());
    NodeIterator rightIter = _right.createNodeIterator(node, env, 
                                                       _right.copyPosition());

    return new UnionIterator(env, leftIter, rightIter);
  }

  public int position(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    NodeIterator iter = select(node, env);

    int i = 1;
    while (iter.hasNext()) {
      if (iter.next() == node)
        return i;
      i++;
    }

    return 0;
  }

  public int count(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    NodeIterator iter = select(node, env);
    int count = 0;

    while (iter.hasNext()) {
      iter.next();
      count++;
    }

    return count;
  }

  /**
   * Return left node of the union.
   */
  public AbstractPattern getLeft()
  {
    return _left;
  }

  /**
   * Return right node of the union.
   */
  public AbstractPattern getRight()
  {
    return _right;
  }

  public String toString()
  {
    // XXX: '(' ?? ')'
    return _left.toString() + "|" + _right.toString();
  }
}
