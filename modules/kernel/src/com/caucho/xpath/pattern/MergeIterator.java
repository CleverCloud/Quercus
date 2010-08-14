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
 * The merge iterator.
 */
public class MergeIterator extends NodeIterator {
  private NodeIterator _baseIterator;

  private SelectedNode []_nodes = new SelectedNode[32];
  private int _head;
  private int _tail;

  /**
   * Creates a merge iterator with a given base.
   */
  public MergeIterator(ExprEnvironment env, NodeIterator baseIterator)
    throws XPathException
  {
    super(env);

    _baseIterator = baseIterator;

    SelectedNode node;
    loop:
    while ((node = baseIterator.nextSelectedNode()) != null) {
      if (_tail == _nodes.length) {
        SelectedNode []newNodes = new SelectedNode[2 * _nodes.length];
        System.arraycopy(_nodes, 0, newNodes, 0, _nodes.length);
        _nodes = newNodes;
      }

      int index = _tail;
      for (; index > 0; index--) {
        SelectedNode oldNode = _nodes[index - 1];

        int cmp = node.compareTo(oldNode);

        if (cmp > 0)
          break;
        else if (cmp == 0)
          continue loop;
      }

      for (int ptr = _tail++; index < ptr; ptr--)
        _nodes[ptr] = _nodes[ptr - 1];
          
      _nodes[index] = node;
    }
  }

  /**
   * True if there are more nodes.
   */
  public boolean hasNext()
  {
    return _head < _tail;
  }

  /**
   * Returns the next node.
   */
  public Node nextNode()
  {
    if (_head < _tail) {
      _position = _head + 1;
      return _nodes[_head++].getNode();
    }
    else
      return null;
  }

  /**
   * Returns the next selected.
   */
  public SelectedNode nextSelectedNode()
  {
    if (_head < _tail) {
      _position = _head + 1;
      return _nodes[_head++];
    }
    else
      return null;
  }

  /**
   * Returns a clone
   */
  public Object clone()
  {
    try {
      MergeIterator clone = new MergeIterator(_env, _baseIterator);
      clone._head = _head;
      clone._tail = _tail;

      if (_nodes.length != clone._nodes.length)
        clone._nodes = new SelectedNode[_nodes.length];

      System.arraycopy(_nodes, 0, clone._nodes, 0, _tail);

      return clone;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }
}

