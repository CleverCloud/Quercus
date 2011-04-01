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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class QAttributedNode extends QNode {
  QAttr _firstAttribute;

  /**
   * Returns a map of the attributes.
   */
  public NamedNodeMap getAttributes()
  {
    return new QAttributeMap(this);
  }

  /**
   * Returns true if the element has attributes.
   */
  public boolean hasAttributes()
  {
    return _firstAttribute != null;
  }

  /**
   * Returns the first attribute in the attribute list.
   */
  public Attr getFirstAttribute()
  {
    return _firstAttribute;
  }

  /**
   * Returns the named attribute.
   */
  public String getAttribute(String name) 
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      if (name.equals(attr.getNodeName()))
        return attr.getNodeValue();
    }

    return "";
  }

  /**
   * Returns the attribute specified by a namespace.
   */
  public String getAttributeNS(String namespaceURI, String local)
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      String attrURI = attr.getNamespaceURI();
      
      if (attr.getLocalName().equals(local) &&
          (attrURI == namespaceURI ||
           attrURI != null && attrURI.equals(namespaceURI)))
        return attr.getNodeValue();
    }

    return "";
  }

  public boolean hasAttribute(String name)
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      if (attr.getNodeName().equals(name))
        return true;
    }

    return false;
  }

  public boolean hasAttributeNS(String uri, String local)
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      String attrURI = attr.getNamespaceURI();
      
      if (attr.getLocalName().equals(local) &&
          (attrURI == uri || attrURI != null && attrURI.equals(uri)))
        return true;
    }

    return false;
  }

  /**
   * Returns the attribute specified by the name.
   */
  public Attr getAttributeNode(String name)
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      if (attr.getNodeName().equals(name))
        return (Attr) attr;
    }

    return null;
  }

  public Attr getAttributeNodeNS(String uri, String local)
  {
    for (QAbstractNode attr = _firstAttribute;
         attr != null;
         attr = attr._next) {
      String attrURI = attr.getNamespaceURI();
      
      if (attr.getLocalName().equals(local) &&
          (attrURI == uri ||
           attrURI != null && attrURI.equals(uri)))
        return (Attr) attr;
    }

    return null;
  }

  public void setAttribute(String name, String value) 
    throws DOMException
  {  
    if (! isNameValid(name))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR, 
                              "illegal attribute `" + name + "'");

    setAttributeNode(_owner.createAttribute(name, value));
  }

  public void setAttributeNS(String uri, String local, String value)
  {
    Attr attr = _owner.createAttributeNS(uri, local);
    attr.setNodeValue(value);
    
    setAttributeNodeNS(attr);
  }

  void setAttribute(QName name, String value) 
    throws DOMException
  {  
    setAttributeNode(_owner.createAttribute(name, value));
  }

  /**
   * Sets an attribute, specified by the object.
   */
  public void setIdAttribute(String name, boolean isId)
    throws DOMException
  {
  }

  /**
   * Sets an attribute, specified by the object.
   */
  public void setIdAttributeNS(String namespaceURI, String localName,
                               boolean isId)
    throws DOMException
  {
  }

  /**
   * Sets an attribute, specified by the object.
   */
  public void setIdAttributeNode(Attr attr, boolean isId)
    throws DOMException
  {
  }

  /**
   * Sets an attribute, specified by the object.
   */
  public Attr setAttributeNode(Attr attr)
    throws DOMException
  {
    QAttr qAttr = (QAttr) attr;

    if (qAttr._owner == null)
      qAttr._owner = _owner;
    else if (qAttr._owner != _owner)
      throw new QDOMException(DOMException.WRONG_DOCUMENT_ERR,
                              "attribute from wrong document");

    if (qAttr._parent != null)
      throw new QDOMException(DOMException.INUSE_ATTRIBUTE_ERR,
                              "attribute `" + attr.getNodeName() +
                              "' is in use");

    qAttr._parent = this;

    // remove any matching old attribute
    QAttr old = unlink(attr.getNodeName());

    QAttr ptr = _firstAttribute;

    if (ptr == null) {
      _firstAttribute = qAttr;
    }
    else {
      for (; ptr._next != null; ptr = (QAttr) ptr._next) {
      }

      ptr._next = qAttr;
    }
    
    return old;
  }
  
  public Attr setAttributeNodeNS(Attr attr)
    throws DOMException
  {
    QAttr qAttr = (QAttr) attr;

    if (qAttr._owner != _owner)
      throw new QDOMException(DOMException.WRONG_DOCUMENT_ERR,
                              "attribute from wrong document");

    if (qAttr._parent != null)
      throw new QDOMException(DOMException.INUSE_ATTRIBUTE_ERR,
                              "attribute `" + attr.getNodeName() +
                              "' is in use");

    // remove any matching old attribute
    QAttr old = unlink(qAttr.getNamespaceURI(), qAttr.getLocalName());
    
    qAttr._parent = this;

    qAttr._next = _firstAttribute;
    _firstAttribute = qAttr;

    return old;
  }

  /**
   * Removes the named attribute.
   */
  public void removeAttribute(String name) 
  {
    if (! isNameValid(name))
      throw new QDOMException(DOMException.INVALID_CHARACTER_ERR, 
                              "illegal attribute `" + name + "'");
    
    unlink(name);
  }

  /**
   * Removes the attribute specified by the localname and namespace.
   */
  public void removeAttributeNS(String uri, String name) 
  {
    unlink(uri, name);
  }

  /**
   * Removes the matching attribute.
   */
  public Attr removeAttributeNode(Attr attr)
  {
    return unlink(attr.getNodeName());
  }

  /**
   * Removes the matching attribute.
   */
  public Attr removeAttributeNodeNS(Attr attr)
  {
    return unlink(attr.getNamespaceURI(), attr.getLocalName());
  }

  /**
   * Unlinks an attribute, returning it.
   */
  QAttr unlink(String name)
  {
    QAttr prev = null;
    QAttr ptr;

    for (ptr = _firstAttribute;
         ptr != null && ! ptr.getNodeName().equals(name);
         ptr = (QAttr) ptr._next) {
      prev = ptr;
    }

    if (ptr == null)
      return null;

    if (prev == null)
      _firstAttribute = (QAttr) ptr._next;
    else
      prev._next = ptr._next;

    ptr._next = null;

    return ptr;
  }

  /**
   * Removes the attribute named by the URI and local name.
   */
  public QAttr unlink(String uri, String local)
  {
    if (local == null || uri == null)
      return null;
    
    QAttr prev = null;
    QAttr ptr;

    for (ptr = (QAttr) _firstAttribute;
         ptr != null && (! local.equals(ptr.getLocalName()) ||
                         ! uri.equals(ptr.getNamespaceURI()));
         ptr = (QAttr) ptr._next) {
      prev = ptr;
    }

    if (ptr == null)
      return null;

    if (prev == null)
      _firstAttribute = (QAttr) ptr._next;
    else
      prev._next = ptr._next;

    ptr._next = null;

    return ptr;
  }

  static class QAttributeMap implements NamedNodeMap {
    QAttributedNode _elt;
    int _i;
    QAttr _attr;

    QAttributeMap(QAttributedNode elt)
    {
      _elt = elt;
    }
  
    public Node getNamedItem(String name)
    {
      return _elt.getAttributeNode(name);
    }
  
    public Node getNamedItemNS(String uri, String localName)
    {
      return _elt.getAttributeNodeNS(uri, localName);
    }

    public Node setNamedItem(Node arg) throws DOMException
    {
      return _elt.setAttributeNode((Attr) arg);
    }
  
    public Node setNamedItemNS(Node arg)
    {
      return _elt.setAttributeNodeNS((Attr) arg);
    }

    public Node removeNamedItem(String name) throws DOMException
    {
      return _elt.unlink(name);
    }
  
    public Node removeNamedItemNS(String uri, String localName)
    {
      return _elt.getAttributeNodeNS(uri, localName);
    }

    public Node item(int index)
    {
      QAbstractNode attr = _elt._firstAttribute;

      while (index > 0 && attr != null) {
        attr = attr._next;
        index--;
      }

      return attr;
    }

    public int getLength()
    {
      int length = 0;

      for (QAbstractNode attr = _elt._firstAttribute;
           attr != null;
           attr = attr._next)
        length++;

      return length;
    }
  }
}
