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

import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * matches namespace nodes of an element.
 */
public class FromNamespace extends Axis {
  public FromNamespace(AbstractPattern parent)
  {
    super(parent);

    if (parent == null)
      throw new RuntimeException();
  }

  /**
   * matches if the node is a namespace node.
   */
  public boolean match(Node node, ExprEnvironment env)
    throws XPathException
  {
    if (node == null)
      return false;

    if (! (node instanceof NamespaceNode))
      return false;

    return _parent.match(node.getParentNode(), env);
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
    NodeIterator parentIter;
    parentIter = _parent.createNodeIterator(node, env, _parent.copyPosition());

    return new NamespaceIterator(node, parentIter, env, match);
  }

  public String toString()
  {
    return getPrefix() + "namespace::";
  }
}
