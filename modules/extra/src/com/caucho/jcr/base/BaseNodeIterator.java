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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jcr.base;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Iterates over an array array of base nodes.
 */
public class BaseNodeIterator implements NodeIterator {
  private BaseNode []_nodes;
  private int _index;

  public BaseNodeIterator(BaseNode []nodes)
  {
    _nodes = nodes;
  }
  /**
   * Returns the next node.
   */
  public Node nextNode()
  {
    if (_index < _nodes.length)
      return _nodes[_index++];
    else
      return null;
  }

  //
  // javax.jcr.RangeIterator
  //

  /**
   * Skips the next 'n' nodes.
   */
  public void skip(long skipNum)
  {
    _index += skipNum;
  }

  /**
   * Returns the total number of nodes.
   */
  public long getSize()
  {
    return _nodes.length;
  }

  /**
   * Returns the current position.
   */
  public long getPosition()
  {
    return _index;
  }

  //
  // java.util.iterator
  //

  /**
   * Returns the next node.
   */
  public Object next()
  {
    return nextNode();
  }

  /**
   * Returns true if there are more nodes.
   */
  public boolean hasNext()
  {
    return _index < _nodes.length;
  }

  /**
   * Removes the current item.
   */
  public void remove()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
