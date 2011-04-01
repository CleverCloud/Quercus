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

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Uses the axis to select new nodes.
 */
public class AttributeListIterator extends NodeIterator {
  protected NodeIterator _parentIter;

  protected NamedNodeMap _attributeMap;
  protected Node _node;
  protected int _index;
  protected AbstractPattern _match;
  
  protected AttributeListIterator(ExprEnvironment env)
  {
    super(env);
  }
  
  /**
   * Creates the new AxisIterator.
   *
   * @param parentIter the parent iterator
   * @param env the variable environment
   * @param match the node matching pattern
   */
  public AttributeListIterator(NodeIterator parentIter, 
                               ExprEnvironment env,
                               AbstractPattern match)
    throws XPathException
  {
    super(env);
    
    _parentIter = parentIter;
    _match = match;

    _node = findFirstMatchingNode();
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
    Node node = _node;

    _node = findFirstMatchingNode();

    return node;
  }

  /**
   * Finds the next matching node.
   */
  private Node findFirstMatchingNode()
    throws XPathException
  {
    Node node = null;
    
    while (true) {
      Node parent = null;
      

      if (node != null && (_match == null || _match.match(node, _env))) {
        _position++;
        return node;
      }
          
      if (_attributeMap != null && _index < _attributeMap.getLength())
        node = _attributeMap.item(_index++);
      else if (_parentIter == null 
               || (parent = _parentIter.nextNode()) == null) {
        return null;
      }
      else if (parent instanceof Element) {
        _position = 0;
        _size = 0;
        _index = 0;
        _attributeMap = ((Element) parent).getAttributes();
      }
    }
  }
  
  /**
   * Returns the number of nodes in the context list.
   */
  public int getContextSize()
  {
    if (_attributeMap == null)
      return 0;
    else
      return _attributeMap.getLength();
  }


  public Object clone()
  {
    AttributeListIterator iter = new AttributeListIterator(_env);

    iter.copy(this);

    if (_parentIter != null)
      iter._parentIter = (NodeIterator) _parentIter.clone();
    iter._node = _node;
    iter._index = _index;
    iter._attributeMap = _attributeMap;
    iter._match = _match;

    return iter;
  }

  public String toString()
  {
    return "AttributeListIterator[" + _match + "]";
  }
}
