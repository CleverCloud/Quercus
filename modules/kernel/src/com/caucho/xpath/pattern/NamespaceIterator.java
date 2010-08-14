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
 * Selects namespace nodes.
 */
public class NamespaceIterator extends NodeIterator {
  protected NodeIterator _parentIter;
  protected AbstractPattern _match;

  protected NamespaceNode _node;
  protected NamespaceNode _next;
  
  protected NamespaceIterator(ExprEnvironment env)
  {
    super(env);
  }
  /**
   * Creates the new NamespaceIterator.
   *
   * @param node the initial node
   * @param parentIter the parent iterator
   * @param env the variable environment
   * @param match the node matching pattern
   */
  public NamespaceIterator(Node node,
                           NodeIterator parentIter,
                           ExprEnvironment env,
                           AbstractPattern match)
    throws XPathException
  {
    super(env);
    
    _parentIter = parentIter;
    _match = match;

    if (parentIter == null)
      _node = NamespaceNode.create(node);
    
    _node = findFirstMatchingNode();
    _next = _node;
  }
  
  /**
   * True if there's more data.
   */
  public boolean hasNext()
  {
    if (_next == null) {
      try {
        _next = (NamespaceNode) nextNode();
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
      _node = (NamespaceNode) _node.getNextSibling();
      _node = findFirstMatchingNode();
    }
    
    _next = null;

    return _node;
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

  /**
   * Finds the next matching node.
   */
  private NamespaceNode findFirstMatchingNode()
    throws XPathException
  {
    while (true) {
      Node parentNode;

      if (_node != null) {
        if (_match == null || _match.match(_node, _env)) {
          _position++;
          return _node;
        }
        else {
          _node = (NamespaceNode) _node.getNextSibling();
          continue;
        }
      }
      
      else if (_parentIter == null ||
               (parentNode = _parentIter.nextNode()) == null)
        return null;
      else {
        _position = 0;
        _size = 0;
        _node = NamespaceNode.create(parentNode);
      }
    }
  }
  
  /**
   * Returns the number of nodes in the context list.
   */
  public int getContextSize()
  {
    return _position;
  }

  public Object clone()
  {
    return null;
  }
  
  public String toString()
  {
    return "NamespaceIterator[" + _match + "]";
  }
}
