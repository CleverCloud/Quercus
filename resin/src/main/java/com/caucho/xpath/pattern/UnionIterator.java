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
public class UnionIterator extends NodeIterator {
  private NodeIterator _leftIter;
  private NodeIterator _rightIter;

  private Node _node;
  
  /**
   * Creates the new AxisIterator.
   *
   * @param leftIter the left iterator
   * @param rightIter the right iterator
   */
  public UnionIterator(ExprEnvironment env,
                       NodeIterator leftIter, NodeIterator rightIter)
    throws XPathException
  {
    super(env);
    
    _leftIter = leftIter;
    _rightIter = rightIter;

    _node = leftIter.nextNode();
    if (_node == null) {
      _leftIter = null;
      _node = _rightIter.nextNode();
    }
  }
  
  /**
   * True if there's more data.
   */
  public boolean hasNext()
  {
    return _node != null;
  }
  
  /**
   * Returns the next selected node.
   */
  public Node nextNode()
    throws XPathException
  {
    Node next = _node;

    if (next == null)
      return null;

    if (_leftIter != null) {
      _node = _leftIter.nextNode();
      if (_node == null) {
        _leftIter = null;
        _node = _rightIter.nextNode();
      }
    }
    else
      _node = _rightIter.nextNode();
    
    return next;
  }

  /**
   * Returns a clone of the iterator.
   */
  public Object clone()
  {
    try {
      UnionIterator clone;
      clone = new UnionIterator(_env,
                                (NodeIterator) _leftIter.clone(),
                                (NodeIterator) _rightIter.clone());

      clone._node = _node;
      clone._position = _position;

      return clone;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
  }
}
