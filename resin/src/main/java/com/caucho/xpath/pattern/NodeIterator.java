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

import com.caucho.xml.XmlUtil;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.StylesheetEnv;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.XPathFun;
import com.caucho.xpath.expr.Var;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Iterates through matching nodes.
 */
public abstract class NodeIterator implements ExprEnvironment, Iterator<Node> {
  protected static final Logger log
    = Logger.getLogger(NodeIterator.class.getName());
  
  protected ExprEnvironment _env;

  protected Node _contextNode;
  protected int _position;
  protected int _size;

  protected NodeIterator(ExprEnvironment env)
  {
    /** XXX: children of NodeList implement iterator() for quercus
     if (env == null)
     throw new NullPointerException();
     */
    
    _env = env;
  }
  
  /**
   * True if there's more data.
   */
  public abstract boolean hasNext();
  
  /**
   * Returns the next node.
   */
  public abstract Node nextNode()
    throws XPathException;

  /**
   * Iterator interface.
   */
  public Node next()
  {
    Node value = null;

    try {
      value = nextNode();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return value;
  }
  
  /**
   * Returns the next selected node.
   */
  public SelectedNode nextSelectedNode()
    throws XPathException
  {
    Node node = nextNode();

    if (node == null)
      return null;
    else if (node instanceof Attr)
      return new SelectedAttribute(node);
    else
      return new SelectedNode(node);
  }

  /**
   * Sets the current node.
   */
  public Node getCurrentNode()
  {
    return _env.getCurrentNode();
  }

  /**
   * Gets the env node.
   */
  public Node getContextNode()
  {
    if (_contextNode != null)
      return _contextNode;
    else
      return _env.getContextNode();
  }

  /**
   * Sets the env node.
   */
  public Node setContextNode(Node node)
  {
    Node oldNode = _contextNode;
    _contextNode = node;
    return oldNode;
  }

  /**
   * Returns the position of the context node.
   */
  public int getContextPosition()
  {
    return _position;
  }

  /**
   * Returns the number of nodes in the context list.
   */
  public int getContextSize()
  {
    if (_size == 0) {
      _size = _position;

      NodeIterator clone = (NodeIterator) clone();
      try {
        while (clone != null && clone.nextNode() != null)
          _size++;
      } catch (XPathException e) {
      }
    }
    
    return _size;
  }

  /**
   * Returns a document for creating nodes.
   */
  public Document getOwnerDocument()
  {
    return _env.getOwnerDocument();
  }

  /**
   * Returns the given variable
   */
  public Var getVar(String name)
  {
    return _env.getVar(name);
  }

  /**
   * Returns the given variable
   */
  public XPathFun getFunction(String name)
  {
    return _env.getFunction(name);
  }
  /**
   * Returns the stylesheet environment.
   */
  public StylesheetEnv getStylesheetEnv()
  {
    return _env.getStylesheetEnv();
  }

  /**
   * Returns the given system property.
   */
  public Object systemProperty(String namespaceURI, String localName)
  {
    return _env.getOwnerDocument();
  }

  /**
   * Returns the string-value of the ndoe.
   */
  public String stringValue(Node node)
  {
    return XmlUtil.textValue(node);
  }

  /**
   * Returns the position index count.
   */
  public int getPositionIndex()
  {
    return 0;
  }
  
  /**
   * Set true if should test more positions.
   */
  public void setMorePositions(boolean more)
  {
  }

  /**
   * clones the iterator
   */
  public abstract Object clone();

  /**
   * copies the iterator.
   */
  public void copy(NodeIterator src)
  {
    _env = src._env;
    _position = src._position;
    _size = src._size;
  }
  
  /**
   * remove is unsupported
   */
  public void remove()
    throws UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }
}
