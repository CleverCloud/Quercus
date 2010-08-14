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

package com.caucho.util;

import java.util.Iterator;

public class Tree {
  private Tree parent;

  private Tree next;
  private Tree previous;

  private Tree first;
  private Tree last;

  private Object data;
  
  public Tree(Object data)
  {
    this.data = data;
  }

  public Object getData()
  {
    return data;
  }

  public void setData(Object data)
  {
    this.data = data;
  }

  public Tree getParent()
  {
    return parent;
  }

  public Tree getNext()
  {
    return next;
  }

  public Tree getNextPreorder()
  {
    if (first != null)
      return first;

    for (Tree ptr = this; ptr != null; ptr = ptr.parent) {
      if (ptr.next != null)
        return ptr.next;
    }

    return null;
  }

  public Tree getPreviousPreorder()
  {
    Tree ptr;

    if ((ptr = previous) != null) {
      for (; ptr.last != null; ptr = ptr.last) {
      }

      return ptr;
    }

    return parent;
  }

  public Tree getPrevious()
  {
    return previous;
  }

  public Tree getFirst()
  {
    return first;
  }

  public Tree getLast()
  {
    return last;
  }

  public Tree append(Object data)
  {
    Tree child = new Tree(data);

    child.parent = this;
    child.previous = last;
    if (last != null)
      last.next = child;
    else
      first = child;
    last = child;

    return last;
  }

  public void appendTree(Tree child)
  {
    Tree subChild = append(child.getData());
    
    for (child = child.getFirst(); child != null; child = child.getNext()) {
      subChild.appendTree(child);
    }
  }

  public Iterator children()
  {
    return new ChildIterator(first);
  }

  public Iterator dfs()
  {
    return new DfsIterator(first);
  }

  public Iterator iterator()
  {
    return new ChildDataIterator(first);
  }

  static class ChildIterator implements Iterator {
    private Tree node;

    ChildIterator(Tree child)
    {
      node = child;
    }

    public boolean hasNext()
    {
      return node != null;
    }

    public Object next()
    {
      Tree next = node;

      if (node != null)
        node = node.getNext();

      return next;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  static class ChildDataIterator implements Iterator {
    private Tree node;

    ChildDataIterator(Tree child)
    {
      node = child;
    }

    public boolean hasNext()
    {
      return node != null;
    }

    public Object next()
    {
      Tree next = node;

      if (node != null)
        node = node.getNext();

      return next == null ? null : next.data;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  static class DfsIterator implements Iterator {
    private Tree top;
    private Tree node;

    DfsIterator(Tree top)
    {
      this.top = top;
      node = top;
    }

    public boolean hasNext()
    {
      return node != null;
    }

    public Object next()
    {
      Tree next = node;

      if (node != null)
        node = node.getNextPreorder();

      return next;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
