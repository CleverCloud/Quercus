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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import org.w3c.dom.Node;

public class DOMNode<T extends Node>
  extends DOMWrapper<T>
{
  protected DOMNode(DOMImplementation impl, T delegate)
  {
    super(impl, delegate);
  }

  Node getDelegate()
  {
    return _delegate;
  }

  public DOMNode appendChild(DOMNode newChild)
    throws DOMException
  {
    try {
      return wrap(_delegate.appendChild(newChild.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode cloneNode(boolean deep)
  {
    return wrap(_delegate.cloneNode(deep));
  }

  public short compareDocumentPosition(DOMNode other)
    throws DOMException
  {
    try {
      return _delegate.compareDocumentPosition(other.getDelegate());
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNamedNodeMap getAttributes()
  {
    return wrap(_delegate.getAttributes());
  }

  public String getBaseURI()
  {
    return _delegate.getBaseURI();
  }

  public DOMNodeList getChildNodes()
  {
    return wrap(_delegate.getChildNodes());
  }

  public Object getFeature(String feature, String version)
  {
    return _delegate.getFeature(feature, version);
  }

  public DOMNode getFirstChild()
  {
    return wrap(_delegate.getFirstChild());
  }

  public DOMNode getLastChild()
  {
    return wrap(_delegate.getLastChild());
  }

  public String getLocalName()
  {
    return _delegate.getLocalName();
  }

  public String getNamespaceURI()
  {
    return _delegate.getNamespaceURI();
  }

  public DOMNode getNextSibling()
  {
    return wrap(_delegate.getNextSibling());
  }

  public String getNodeName()
  {
    return _delegate.getNodeName();
  }

  public short getNodeType()
  {
    return _delegate.getNodeType();
  }

  public String getNodeValue()
    throws DOMException
  {
    try {
      return _delegate.getNodeValue();
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMDocument getOwnerDocument()
  {
    return wrap(_delegate.getOwnerDocument());
  }

  public DOMNode getParentNode()
  {
    return wrap(_delegate.getParentNode());
  }

  public String getPrefix()
  {
    return _delegate.getPrefix();
  }

  public DOMNode getPreviousSibling()
  {
    return  wrap(_delegate.getPreviousSibling());
  }

  public String getTextContent()
    throws DOMException
  {
    try {
      return _delegate.getTextContent();
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public Object getUserData(String key)
  {
    return _delegate.getUserData(key);
  }

  public boolean hasAttributes()
  {
    return _delegate.hasAttributes();
  }

  public boolean hasChildNodes()
  {
    return _delegate.hasChildNodes();
  }

  public DOMNode insertBefore(DOMNode newChild, DOMNode refChild)
    throws DOMException
  {
    try {
      return wrap(_delegate.insertBefore(
          newChild.getDelegate(), refChild.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public boolean isDefaultNamespace(String namespaceURI)
  {
    return _delegate.isDefaultNamespace(namespaceURI);
  }

  public boolean isEqualNode(DOMNode arg)
  {
    return _delegate.isEqualNode(arg.getDelegate());
  }

  public boolean isSameNode(DOMNode other)
  {
    return _delegate.isSameNode(other.getDelegate());
  }

  public boolean isSupported(String feature, String version)
  {
    return _delegate.isSupported(feature, version);
  }

  public String lookupNamespaceURI(String prefix)
  {
    return _delegate.lookupNamespaceURI(prefix);
  }

  public String lookupPrefix(String namespaceURI)
  {
    return _delegate.lookupPrefix(namespaceURI);
  }

  public void normalize()
  {
    _delegate.normalize();
  }

  public DOMNode removeChild(DOMNode oldChild)
    throws DOMException
  {
    try {
      return wrap(_delegate.removeChild(oldChild.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMNode replaceChild(DOMNode newChild, DOMNode oldChild)
    throws DOMException
  {
    try {
      return wrap(_delegate.replaceChild(
          newChild.getDelegate(), oldChild.getDelegate()));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public void setNodeValue(String nodeValue)
    throws DOMException
  {
    try {
      _delegate.setNodeValue(nodeValue);
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public void setPrefix(String prefix)
    throws DOMException
  {
    try {
      _delegate.setPrefix(prefix);
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public void setTextContent(String textContent)
    throws DOMException
  {
    try {
      _delegate.setTextContent(textContent);
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public Object setUserData(String key, Object data)
  {
    return _delegate.setUserData(key, data, null);
  }

  public String toString()
  {
    return getClass().getSimpleName();
  }
}
