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

package com.caucho.xml2;

import com.caucho.util.CharBuffer;
import com.caucho.xpath.pattern.NodeListIterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Generic implementation of a node list.
 */
public class NodeListImpl implements NodeList {
  private ArrayList<Node> _nodeList = new ArrayList<Node>();

  /**
   * Creates an empty node list.
   */
  public NodeListImpl()
  {
  }

  /**
   * Creates a node list from a collection.
   */
  public NodeListImpl(Collection<Node> collection)
  {
    _nodeList.addAll(collection);
  }

  /**
   * Adds an item.
   */
  public void add(Node node)
  {
    _nodeList.add(node);
  }

  /**
   * Returns the item at the index.
   */
  public Node item(int index)
  {
    if (index < 0 || _nodeList.size() <= index)
      return null;

    return _nodeList.get(index);
  }

  /**
   * Returns the number of items in the list.
   */
  public int getLength()
  {
    return _nodeList.size();
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();

    cb.append("NodeListImpl[");
    for (int i = 0; i < getLength(); i++) {
      if (i != 0)
        cb.append(", ");
      cb.append(item(i));
    }
    cb.append("]");

    return cb.toString();
  }

  // for quercus
  public Iterator<Node> iterator()
  {
    return new NodeListIterator(null, this);
  }
}
