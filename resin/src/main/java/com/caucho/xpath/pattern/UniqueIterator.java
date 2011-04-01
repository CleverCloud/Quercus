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
 * The unique iterator.
 */
public class UniqueIterator extends NodeIterator {
  private NodeIterator _baseIterator;

  private Node _node;

  private Node []_oldNodes;
  private int _top;

  /**
   * Zero arg constructor.
   */
  public UniqueIterator(ExprEnvironment env)
  {
    super(env);
  }

  /**
   * Creates a merge iterator with a given base.
   */
  public UniqueIterator(ExprEnvironment env, NodeIterator baseIterator)
    throws XPathException
  {
    super(env);
    
    _baseIterator = baseIterator;
    _node = baseIterator.nextNode();
    _oldNodes = new Node[32];
  }

  /**
   * True if there are more nodes.
   */
  public boolean hasNext()
  {
    return _node != null;
  }

  /**
   * Returns the next node.
   */
  public Node nextNode()
    throws XPathException
  {
    Node next = _node;

    if (next == null)
      return null;

    if (_top == _oldNodes.length) {
      Node []newNodes = new Node[_oldNodes.length * 2];
      System.arraycopy(_oldNodes, 0, newNodes, 0, _oldNodes.length);
      _oldNodes = newNodes;
    }
    
    _oldNodes[_top++] = next;

    while (true) {
      _node = _baseIterator.nextNode();

      if (_node == null)
        return next;

      int i;
      for (i = _top - 1; i >= 0; i--)
        if (_oldNodes[i] == _node)
          break;

      if (i < 0)
        break;
    }

    return next;
  }

  /**
   * Returns a clone
   */
  public Object clone()
  {
    UniqueIterator clone = new UniqueIterator(_env);

    clone._node = _node;
    clone._oldNodes = new Node[_oldNodes.length];
    System.arraycopy(_oldNodes, 0, clone._oldNodes, 0, _oldNodes.length);
    clone._top = _top;

    return clone;
  }
}

