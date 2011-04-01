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

import java.util.logging.Level;

/**
 * Uses the axis to select new nodes.
 */
public class AttributeIterator extends AxisIterator {
  private AttributeIterator(ExprEnvironment env)
  {
    super(env);
  }
  /**
   * Creates the new AxisIterator.
   *
   * @param parentIter the parent iterator
   * @param axis the owning axis
   * @param node the first node
   * @param env the xpath environment
   * @param context the context node
   * @param match the node matching pattern
   */
  public AttributeIterator(NodeIterator parentIter, AbstractPattern axis,
                           Node node, ExprEnvironment env,
                           AbstractPattern match)
    throws XPathException
  {
    super(parentIter, axis, node, env, match);
  }
  
  /**
   * Returns the next selected node.
   */
  public SelectedNode nextSelectedNode()
    throws XPathException
  {
    Node node = nextNode();

    return node == null ? null : new SelectedAttribute(node);
  }

  public Object clone()
  {
    try {
      AttributeIterator iter = new AttributeIterator(_env);

      iter._position = _position;
      if (_parentIter != null)
        iter._parentIter = (NodeIterator) _parentIter.clone();
      iter._axis = _axis;
      iter._node = _node;
      iter._env = _env; // clone?
      iter._match = _match;

      return iter;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  public String toString()
  {
    return "AttributeIterator[axis:" + _axis + ",match:" + _match + "]";
  }
}
