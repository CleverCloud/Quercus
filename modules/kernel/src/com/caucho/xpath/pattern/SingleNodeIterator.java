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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Iterates through matching nodes.
 */
public class SingleNodeIterator extends NodeIterator implements NodeList {
  protected Node _node;

  public SingleNodeIterator(ExprEnvironment env, Node node)
  {
    super(env);
    
    _node = node;
  }
  
  /**
   * Returns the current position.
   */
  public int getPosition()
  {
    return _node == null ? 1 : 0;
  }
  /**
   * True if there's more data.
   */
  public boolean hasNext()
  {
    return _node != null;
  }
  
  /**
   * Returns the next node.
   */
  public Node nextNode()
  {
    if (_node != null) {
      Node next = _node;
      _node = null;
      return next;
    }
    else
      return null;
  }

  /**
   * Returns the NodeList length.
   */
  public int getLength()
  {
    return _node != null ? 1 : 0;
  }

  /**
   * Returns the NodeList item.
   */
  public Node item(int i)
  {
    return i == 0 ? _node : null;
  }

  /**
   * clones the iterator
   */
  public Object clone()
  {
    return new SingleNodeIterator(_env, _node);
  }
}
