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
public class AxisIterator extends NodeIterator {
  protected NodeIterator _parentIter;
  protected AbstractPattern _axis;
  protected Node _node;
  protected Node _next;
  protected Node _lastNode;
  protected AbstractPattern _match;
  
  protected AxisIterator(ExprEnvironment env)
  {
    super(env);
  }
  
  /**
   * Creates the new AxisIterator.
   *
   * @param parentIter the parent iterator
   * @param axis the owning axis
   * @param node the first node
   * @param env the variable environment
   * @param match the node matching pattern
   */
  public AxisIterator(NodeIterator parentIter, AbstractPattern axis,
                      Node node, ExprEnvironment env,
                      AbstractPattern match)
    throws XPathException
  {
    super(env);
    
    _parentIter = parentIter;
    _axis = axis;
    _match = match;

    if (parentIter != null && parentIter.hasNext()) {
      node = parentIter.nextNode();
    }

    if (node != null) {
      _lastNode = axis.lastNode(node);
      _node = findFirstMatchingNode(axis.firstNode(node, _env));
    }

    _next = _node;
  }
  
  /**
   * True if there's more data.
   */
  public boolean hasNext()
  {
    if (_next == null) {
      try {
        _next = nextNode();
      } catch (XPathException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    return _next != null;
  }
  
  /**
   * Returns the next selected node.
   */
  public Node nextNode()
    throws XPathException
  {
    if (_next != null) {
      _node = _next;
      _next = null;
      
      return _node;
    }

    if (_node != null)
      _node = findFirstMatchingNode(_axis.nextNode(_node, _lastNode));
    
    _next = null;

    return _node;
  }

  /**
   * Finds the next matching node.
   */
  private Node findFirstMatchingNode(Node node)
    throws XPathException
  {
    while (true) {
      if (node != null) {
        if (_match == null || _match.match(node, _env)) {
          _position++;
          return node;
        }
        else {
          node = _axis.nextNode(node, _lastNode);
          continue;
        }
      }
      
      else if (_parentIter == null || (node = _parentIter.nextNode()) == null)
        return null;
      else {
        _position = 0;
        _size = 0;
        _lastNode = _axis.lastNode(node);
        node = _axis.firstNode(node, _env);
      }
    }
  }
  
  /**
   * Returns the number of nodes in the context list.
   */
  public int getContextSize()
  {
    if (_size == 0) {
      _size = _position;
      
      try {
        Node ptr = _node == null ? null : _axis.nextNode(_node, _lastNode);
        
        for (; ptr != null; ptr = _axis.nextNode(ptr, _lastNode)) {
          if (_match == null || _match.match(ptr, _env))
            _size++;
        }
      } catch (XPathException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    return _size;
  }


  public Object clone()
  {
    AxisIterator iter = new AxisIterator(_env);

    iter.copy(this);

    if (_parentIter != null)
      iter._parentIter = (NodeIterator) _parentIter.clone();
    
    iter._axis = _axis;
    iter._node = _node;
    iter._match = _match;

    return iter;
  }

  public String toString()
  {
    return "AxisIterator[axis:" + _axis + ",match:" + _match + "]";
  }
}
