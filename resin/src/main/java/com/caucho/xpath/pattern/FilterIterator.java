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

import com.caucho.xpath.Expr;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

import java.util.logging.Level;

/**
 * Uses the axis to select new nodes.
 */
public class FilterIterator extends NodeIterator {
  private NodeIterator _parentIter;
  
  private Expr _expr;
  
  private Node _node;
  private Node _next;
  
  /**
   * Creates the new AxisIterator.
   *
   * @param parentIter the parent iterator
   * @param expr the filter expression
   * @param env the xpath environment
   * @param context the context node
   */
  public FilterIterator(NodeIterator parentIter, Expr expr,
                        ExprEnvironment env, Node context)
    throws XPathException
  {
    super(env);
    
    _parentIter = parentIter;
    _expr = expr;
    _env = env;
    
    _contextNode = context;

    _node = findFirstMatchingNode(parentIter);
    _position = 1;
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

    if (_node != null) {
      _node = findFirstMatchingNode(_parentIter);
      _position++;
    }
    _next = null;

    return _node;
  }

  /**
   * Finds the next matching node.
   */
  private Node findFirstMatchingNode(NodeIterator parentIter)
    throws XPathException
  {
    Node node = parentIter.nextNode();

    while (true) {
      if (node != null) {
        if (_expr.isNumber()) {
          double value = _expr.evalNumber(node, parentIter);
          
          if (value == parentIter.getContextPosition())
            return node;
        }
        else if (_expr.isBoolean()) {
          if (_expr.evalBoolean(node, parentIter))
            return node;
        }
        else {
          Object value = _expr.evalObject(node, parentIter);

          if (value instanceof Number) {
            if (Expr.toDouble(value) == parentIter.getContextPosition())
              return node;
          }
          else if (Expr.toBoolean(value))
            return node;
        }
      }

      if (parentIter == null || (node = parentIter.nextNode()) == null) {
        return null;
      }
      else if (parentIter.getContextPosition() == 1) {
        _position = 0;
        _size = 0;
      }
      _contextNode = node;
      parentIter.setContextNode(node);
    }
  }

  public Object clone()
  {
    FilterIterator clone = null;;

    try {
      if (_parentIter == null)
        clone = new FilterIterator(null, _expr, _env, _contextNode);
      else
        clone = new FilterIterator((NodeIterator) _parentIter.clone(),
                                   _expr, _env, _contextNode);

      clone._env = _env;
      clone._position = _position;
      clone._node = _node;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return clone;
  }

  public String toString()
  {
    return "FilterIterator[" + _expr + "]";
  }
}
