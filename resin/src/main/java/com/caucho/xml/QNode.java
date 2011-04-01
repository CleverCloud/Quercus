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

package com.caucho.xml;

// import com.caucho.xpath.pattern.NodeListIterator;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;

/**
 * QNode represents any node that can have children.
 */
public abstract class QNode extends QAbstractNode {
  protected QAbstractNode _firstChild;
  protected QAbstractNode _lastChild;

  protected QNode()
  {
  }

  protected QNode(QDocument owner)
  {
    super(owner);
  }

  public String getNodeValue() { return null; }

  public void setNodeValue(String value) {}

  /**
   * Returns a node list of the children.
   */
  public NodeList getChildNodes()
  {
    return new ChildNodeList();
  }

  public Node getFirstChild()
  {
    return _firstChild;
  }

  public Node getLastChild()
  {
    return _lastChild;
  }

  public Node getPreviousSibling()
  {
    return _previous;
  }

  public Node getNextSibling()
  {
    return _next;
  }

  public NamedNodeMap getAttributes() { return null; }

  public Node insertBefore(Node newChild, Node refChild)
    throws DOMException
  {
    QAbstractNode qNewChild = (QAbstractNode) newChild;
    QAbstractNode qRefChild = (QAbstractNode) refChild;

    if (qNewChild._owner != _owner && qNewChild._owner != this)
      throw new QDOMException(DOMException.WRONG_DOCUMENT_ERR,
                              "insertBefore new child from wrong document");

    qNewChild.remove();

    if (qRefChild != null && qRefChild._parent != this)
      throw new QDOMException(DOMException.NOT_FOUND_ERR, "insertBefore has no such child");

    if (qNewChild instanceof QDocumentFragment) {
      QDocumentFragment frag = (QDocumentFragment) qNewChild;
      QAbstractNode first = frag._firstChild;
      QAbstractNode next = null;
      for (QAbstractNode ptr = first; ptr != null; ptr = next) {
        next = ptr._next;
        insertBefore(ptr, refChild);
      }

      return first;
    }

    qNewChild._parent = this;

    if (refChild == null) {
      if (_firstChild == null) {
        qNewChild._previous = null;
        qNewChild._next = null;
        _firstChild = _lastChild = qNewChild;
      } else {
        _lastChild._next = qNewChild;
        qNewChild._previous = _lastChild;
        _lastChild = qNewChild;
        qNewChild._next = null;
      }

      return qNewChild;
    }

    qNewChild._previous = qRefChild._previous;
    qNewChild._next = qRefChild;
    if (qRefChild._previous == null)
      _firstChild = qNewChild;
    else
      qRefChild._previous._next = qNewChild;
    qRefChild._previous = qNewChild;

    return qNewChild;
  }

  public Node replaceChild(Node newChild, Node refChild)
    throws DOMException
  {
    QAbstractNode qNewChild = (QAbstractNode) newChild;
    QAbstractNode qRefChild = (QAbstractNode) refChild;

    if (qRefChild == null || qRefChild._parent != this)
      throw new QDOMException(DOMException.NOT_FOUND_ERR, "ref is not child");

    if (qNewChild == null || qNewChild._owner != _owner)
      throw new QDOMException(DOMException.WRONG_DOCUMENT_ERR,
                              "wrong document");

    if (_owner != null)
      _owner._changeCount++;

    qNewChild._previous = qRefChild._previous;
    qNewChild._next = qRefChild._next;
    qNewChild._parent = this;

    if (qNewChild._previous == null)
      _firstChild = qNewChild;
    else
      qNewChild._previous._next = qNewChild;

    if (qNewChild._next == null)
      _lastChild = qNewChild;
    else
      qNewChild._next._previous = qNewChild;

    qRefChild._previous = null;
    qRefChild._next = null;
    qRefChild._parent = null;

    return qRefChild;
  }

  public Node removeChild(Node oldChild) throws DOMException
  {
    QAbstractNode qOldChild = (QAbstractNode) oldChild;

    if (qOldChild != null && qOldChild._parent != this) {
      throw new QDOMException(DOMException.NOT_FOUND_ERR,
                              "removeChild has no such child");
    }

    if (_owner != null)
      _owner._changeCount++;

    if (qOldChild._previous == null)
      _firstChild = qOldChild._next;
    else
      qOldChild._previous._next = qOldChild._next;

    if (qOldChild._next == null)
      _lastChild = qOldChild._previous;
    else
      qOldChild._next._previous = qOldChild._previous;

    qOldChild._parent = null;
    qOldChild._next = null;
    qOldChild._previous = null;

    return qOldChild;
  }

  private static void setOwner(QAbstractNode node, QDocument owner)
  {
    if (node._owner == null) {
      node._owner = owner;

      String namespace = node.getNamespaceURI();

      if (namespace != "")
        owner.addNamespace(node.getPrefix(), namespace);

      for (QAbstractNode child = (QAbstractNode) node.getFirstChild();
           child != null;
           child = (QAbstractNode) child.getNextSibling())
      {
        setOwner(child, owner);
      }
    }
  }

  public Node appendChild(Node newNode) throws DOMException
  {
    QAbstractNode qNewNode = (QAbstractNode) newNode;

    setOwner(qNewNode, _owner);

    if (qNewNode._owner != _owner && qNewNode._owner != this) {
      throw new QDOMException(DOMException.WRONG_DOCUMENT_ERR,
                              "can't appendChild from different document");
    }

    qNewNode.remove();

    if (qNewNode instanceof QDocumentFragment) {
      QDocumentFragment frag = (QDocumentFragment) qNewNode;
      QAbstractNode first = frag._firstChild;
      QAbstractNode next = null;
      for (QAbstractNode ptr = first; ptr != null; ptr = next) {
        next = ptr._next;
        appendChild(ptr);
      }

      return first;
    }

    qNewNode._parent = this;
    qNewNode._next = null;
    qNewNode._previous = _lastChild;
    if (_lastChild == null) {
      _lastChild = qNewNode;
      _firstChild = qNewNode;
    }
    else {
      _lastChild._next = qNewNode;
      _lastChild = qNewNode;
    }

    return qNewNode;
  }

  public boolean hasChildNodes()
  {
    return _firstChild != null;
  }

  public void setTextContent(String content)
  {
    QText text = new QText(content);
    text._owner = _owner;

    _firstChild = _lastChild = text;
  }

  public void normalize()
  {
  }

  public boolean checkValid()
    throws Exception
  {
    if (! super.checkValid())
      throw new Exception("super bad: " + this);

    if (_firstChild != null && _firstChild._previous != null)
      throw new Exception("first child bad: " + this);

    if (_lastChild != null && _lastChild._next != null)
      throw new Exception("last child bad:" + this);

    QAbstractNode ptr = _firstChild;
    for (; ptr != null; ptr = ptr._next) {
      if (ptr._parent != this)
        throw new Exception("child parent bad:" + this + " " + ptr);
      if (ptr._owner != _owner && ptr._owner != this)
        throw new Exception("child owner bad:" + this + " " + ptr + " " +
                            ptr._owner + " " + _owner);
      if (ptr._next != null && ptr._next._previous != ptr)
        throw new Exception("child links bad:" + this + " " + ptr);
    }

    ptr = _lastChild;
    for (; ptr != null; ptr = ptr._previous) {
      if (ptr._parent != this)
        throw new Exception("child parent bad:" + this + " " + ptr);
      if (ptr._owner != _owner && ptr._owner != this)
        throw new Exception("child owner bad:" + this + " " + ptr);
      if (ptr._previous != null && ptr._previous._next != ptr)
        throw new Exception("child links bad:" + this + " " + ptr);
    }

    return true;
  }

  public QAbstractNode getNextPreorder()
  {
    if (_firstChild != null)
      return _firstChild;
    else if (_next != null)
      return _next;

    for (QNode ptr = _parent; ptr != null; ptr = ptr._parent) {
      if (ptr._next != null)
        return ptr._next;
    }

    return null;
  }

  public boolean equals(Object arg)
  {
    return this == arg;
  }

  public boolean equals(Node arg, boolean deep)
  {
    return this == arg;
  }

  // NodeList methods

  public class ChildNodeList implements NodeList {
    /**
     * Returns the child with the given index.
     */
    public Node item(int index)
    {
      QAbstractNode ptr = _firstChild;
      for (; ptr != null && index > 0; index--) {
        ptr = ptr._next;
      }

      return ptr;
    }

    /**
     * Returns the number of children.
     */
    public int getLength()
    {
      int index = 0;
      for (QAbstractNode ptr = _firstChild; ptr != null; ptr = ptr._next)
        index++;

      return index;
    }

    /*
    // for quercus
    public Iterator<Node> iterator()
    {
      return new NodeListIterator(null, this);
    }
    */
  }
}
