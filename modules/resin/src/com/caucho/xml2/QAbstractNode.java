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

import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.WriteStream;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;

/**
 * QAbstractNode is an abstract implementation for any DOM node.
 */
public abstract class QAbstractNode implements CauchoNode, java.io.Serializable {
  protected static L10N L = new L10N(QAbstractNode.class);
  
  QDocument _owner;

  QNode _parent;

  QAbstractNode _next;
  QAbstractNode _previous;

  String _systemId;
  String _filename;
  int _line;

  protected QAbstractNode()
  {
  }

  protected QAbstractNode(QDocument owner)
  {
    _owner = owner;
  }

  public void setLocation(String systemId, String filename,
                          int line, int column)
  {
    _systemId = systemId;
    _filename = filename; 
    _line = line;
  }

  /**
   * Returns the node's source filename.
   */
  public String getFilename()
  {
    if (_filename != null)
      return _filename;
    else if (_previous != null)
      return _previous.getFilename();
    else if (_parent != null)
      return _parent.getFilename();
    else
      return null;
  }

  /**
   * Returns the base URI of the node.
   */
  public String getBaseURI()
  {
    if (_systemId != null)
      return _systemId;
    else if (_previous != null)
      return _previous.getBaseURI();
    else if (_parent != null)
      return _parent.getBaseURI();
    else
      return getFilename();
  }

  /**
   * Returns the base URI
   */
  public static String baseURI(Node node)
  {
    if (node instanceof QAbstractNode)
      return ((QAbstractNode) node).getBaseURI();
    else
      return null;
  }

  /**
   * Returns the node's source line.
   */
  public int getLine()
  {
    if (_filename != null)
      return _line;
    else if (_previous != null)
      return _previous.getLine();
    else if (_parent != null)
      return _parent.getLine();
    else
      return 0;
  }

  public int getColumn()
  {
    return 0;
  }

  /**
   * Returns the owning document.
   */
  public Document getOwnerDocument()
  {
    return _owner;
  }

  public boolean isSupported(String feature, String version)
  {
    return _owner.getImplementation().hasFeature(feature, version);
  }

  /**
   * Returns a feature value.
   */
  public Object getFeature(String feature, String version)
  {
    return null;
  }

  /**
   * Sets a feature value.
   */
  public void setFeature(String feature, boolean value)
  {
  }

  /**
   * Compares the document position
   */
  public short compareDocumentPosition(Node node)
  {
    return 0;
  }

  /**
   * Looks up a prefix value.
   */
  public String lookupPrefix(String feature)
  {
    return null;
  }
  
  /**
   * Returns true if the node has attributes.
   */
  public boolean hasAttributes()
  {
    return false;
  }

  public String getPrefix()
  {
    return "";
  }

  public void setPrefix(String prefix)
  {
  }

  public Object setUserData(String key, Object value, UserDataHandler userData)
  {
    return null;
  }

  public Object getUserData(String data)
  {
    return null;
  }

  public String getCanonicalName()
  {
    return getNodeName();
  }

  public String getLocalName()
  {
    return getNodeName();
  }

  public String getNamespaceURI()
  {
    return "";
  }

  public QName getQName()
  {
    return new QName(getNodeName(), getNamespaceURI());
  }

  public String getNodeValue() { return null; }

  public void setNodeValue(String value) {}

  public Node getParentNode() { return _parent; }

  public NodeList getChildNodes() 
  { 
    return new QEmptyNodeList();
  }

  public Node getFirstChild() { return null; }

  public Node getLastChild() { return null; }

  public Node getPreviousSibling() { return _previous; }

  public Node getNextSibling() { return _next; }

  public NamedNodeMap getAttributes() { return null; }

  public Node insertBefore(Node newChild, Node refChild)
    throws DOMException
  { 
    throw new QDOMException(DOMException.HIERARCHY_REQUEST_ERR, "");
  }

  public Node replaceChild(Node newChild, Node refChild)
    throws DOMException
  { 
    throw new QDOMException(DOMException.HIERARCHY_REQUEST_ERR, "");
  }

  public Node removeChild(Node oldChild) throws DOMException
  { 
    throw new QDOMException(DOMException.HIERARCHY_REQUEST_ERR, "");
  }

  public Node appendChild(Node newNode) throws DOMException
  { 
    throw new QDOMException(DOMException.HIERARCHY_REQUEST_ERR, "");
  }

  public boolean hasChildNodes() { return false; }

  public boolean equals(Node arg, boolean deep)
  {
    return this == arg;
  }

  void remove()
  {
    if (_owner != null)
      _owner._changeCount++;
    
    if (_previous != null)
      _previous._next = _next;
    else if (_parent != null)
      _parent._firstChild = _next;

    if (_next != null)
      _next._previous = _previous;
    else if (_parent != null)
      _parent._lastChild = _previous;

    _previous = null;
    _next = null;
    _parent = null;
  }

  public QAbstractNode getNextPreorder()
  {
    if (_next != null)
      return _next;

    for (QNode ptr = _parent; ptr != null; ptr = ptr._parent) {
      if (ptr._next != null)
        return ptr._next;
    }

    return null;
  } 

  public boolean hasContent() { return false; }

  public QAbstractNode getNextContent() 
  {
    for (QAbstractNode node = _next; node != null; node = node._next) {
      if (node.hasContent())
        return node;
    }

    return null;
  }

  public QAbstractNode getPreviousContent() 
  {
    for (QAbstractNode node = _previous; node != null; node = node._previous) {
      if (node.hasContent())
        return node;
    }

    return null;
  }

  public String getTextValue()
  {
    return getNodeValue();
  }

  /**
   * Support the same and the implementation
   */
  public boolean supports(String feature, String version)
  {
    return _owner._implementation.hasFeature(feature, version);
  }

  public void normalize()
  {
    
  }

  public Node cloneNode(boolean deep)
  {
    return _owner.importNode(this, deep);
  }

  // DOM level 3

  public short compareTreePosition(Node other)
  {
    throw new UnsupportedOperationException();
  }

  public String getTextContent()
    throws DOMException
  {
    return XmlUtil.textValue(this);
  }
  
  public void setTextContent(String textContent)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSameNode(Node other)
  {
    return this == other;
  }

  public String lookupNamespacePrefix(String namespaceURI, 
                                      boolean useDefault)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isDefaultNamespace(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isEqualNode(Node arg)
  {
    return equals(arg);
  }

  public Node getInterface(String feature)
  {
    throw new UnsupportedOperationException();
  }

  /*
  public Object setUserData(String key, 
                            Object data, 
                            UserDataHandler handler)
  {
    throw new UnsupportedOperationException();
  }
  
  public Object getUserData(String key)
  {
    throw new UnsupportedOperationException();
  }
  */

  // Caucho stuff

  public ArrayList<Depend> getDependencyList()
  {
    if (_owner != null)
      return _owner.getDependencyList();
    else
      return null;
  }
  
  boolean isNameValid(String name)
  {
    if (name == null || name.length() == 0)
      return false;

    if (! XmlChar.isNameStart(name.charAt(0)))
      return false;
    
    for (int i = 1; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (! XmlChar.isNameChar(ch))
        return false;
    }

    return true;
  }

  public boolean checkValid()
    throws Exception
  {
    if (_parent == null) {
      if (_next != null || _previous != null)
        throw new Exception("null bad: " + this);
      else
        return true;
    }

    if (_parent._owner != _owner && _owner != _parent)
      throw new Exception("owner bad: " + this);

    QAbstractNode ptr = _parent._firstChild;
    for (; ptr != null && ptr != this; ptr = ptr._next) {
    }
    if (ptr == null)
      throw new Exception("not in parent: " + this);

    ptr = _parent._lastChild;
    for (; ptr != null && ptr != this; ptr = ptr._previous) {
    }
    if (ptr == null)
      throw new Exception("not in parent: " + this);

    if (_next == null && _parent._lastChild != this)
      throw new Exception("bad tail: " + this);

    else if (_next != null && _next._previous != this)
      throw new Exception("bad link: " + this);

    if (_previous == null && _parent._firstChild != this)
      throw new Exception("bad head: " + this);
    else if (_previous != null && _previous._next != this)
      throw new Exception("bad link: " + this);

    return true;
  }

  void print(XmlPrinter out) throws IOException
  {
  }

  public void print(WriteStream out) throws IOException
  {
    new XmlPrinter(out).printXml(this);
  }

  public void printPretty(WriteStream out) throws IOException
  {
    new XmlPrinter(out).printPrettyXml(this);
  }

  public void printHtml(WriteStream out) throws IOException
  {
    new XmlPrinter(out).printHtml(this);
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }
}
