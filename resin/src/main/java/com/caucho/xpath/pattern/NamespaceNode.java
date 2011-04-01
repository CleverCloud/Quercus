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

import com.caucho.vfs.WriteStream;
import com.caucho.xml.CauchoElement;
import com.caucho.xml.CauchoNode;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QAttr;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.HashMap;

/**
 * A pseudo-node for handling the namespace:: axis.
 */
public class NamespaceNode extends QAbstractNode implements CauchoNode {
  Node _parent;

  NamespaceNode _next;
  NamespaceNode _prev;

  String _local;
  String _name;
  String _url;

  /**
   * Creates a new namespace node.
   */
  public NamespaceNode(Node parent, NamespaceNode next,
                       String prefix, String url)
  {
    _parent = parent;
    _next = next;
    if (next != null)
      next._prev = this;
    _local = prefix;
    if (prefix == null || prefix.equals(""))
      _name = "xmlns";
    else
      _name = ("xmlns:" + prefix).intern();
    _url = url;
  }

  /**
   * Creates a list of namespace nodes based on the current element.
   * The list is the list of namespaces active for that element.
   */
  static NamespaceNode create(Node node)
  {
    Node top = node;
    NamespaceNode nodes = null;
    HashMap<String,String> map = new HashMap<String,String>();

    for (; node instanceof CauchoElement; node = node.getParentNode()) {
      CauchoElement elt = (CauchoElement) node;

      String prefix = elt.getPrefix();
      String url = elt.getNamespaceURI();
      if (url == null)
        url = "";
      
      if (map.get(prefix) == null) {
        map.put(prefix, url);
        if (! url.equals(""))
          nodes = new NamespaceNode(top, nodes, prefix, url);
      }

      QAttr attr = (QAttr) elt.getFirstAttribute();
      for (; attr != null; attr = (QAttr) attr.getNextSibling()) {
        String name = attr.getNodeName();
        prefix = null;
        url = "";

        if (name.startsWith("xmlns:")) {
          prefix = name.substring(6);
          url = attr.getNodeValue();
        }
        else if (name.equals("xmlns")) {
          prefix = "";
          url = attr.getNodeValue();
        }
        else {
          prefix = attr.getPrefix();
          url = attr.getNamespaceURI();
        }

        if (url == null)
          url = "";
      
        if (map.get(prefix) == null) {
          map.put(prefix, url);
          if (! url.equals(""))
            nodes = new NamespaceNode(top, nodes, prefix, url);
        }
      }
    }

    return nodes;
  }

  public short getNodeType()
  {
    return ATTRIBUTE_NODE;
  }

  public String getNodeName()
  {
    return _name;
  }

  public String getPrefix()
  {
    return "xmlns";
  }

  public void setPrefix(String prefix)
  {
  }

  public boolean supports(String feature, String version)
  {
    return false;
  }
  
  public String getCanonicalName()
  {
    return "";
  }

  public String getLocalName()
  {
    return _local;
  }

  public String getNamespaceURI()
  {
    return null;
  }

  public String getNodeValue()
  {
    return _url;
  }

  public Node getParentNode()
  {
    return _parent;
  }

  public Node getPreviousSibling()
  {
    return _prev;
  }

  public Node getNextSibling()
  {
    return _next;
  }

  // The following are just stubs to conform to the api

  public void setLocation(String filename, int line, int column)
  { 
  }

  public String getFilename()
  {
    return null;
  }

  public int getLine()
  {
    return 0;
  }

  public int getColumn() { return 0; }

  public Document getOwnerDocument() { return null; }

  public void setNodeValue(String value) {}

  public NodeList getChildNodes() { return null; }

  public Node getFirstChild() { return null; }

  public Node getLastChild() { return null; }

  public NamedNodeMap getAttributes() { return null; }

  public Node insertBefore(Node newChild, Node refChild)
  { 
    return null;
  }

  public Node replaceChild(Node newChild, Node refChild)
  { 
    return null;
  }

  public Node removeChild(Node oldChild) throws DOMException
  { 
    return null;
  }

  public Node appendChild(Node newNode) throws DOMException
  { 
    return null;
  }

  public boolean hasChildNodes() { return false; }

  public boolean equals(Node arg, boolean deep)
  {
    return this == arg;
  }

  public Node cloneNode(boolean deep)
  {
    return null;
  }

  public void normalize()
  {
  }

  public String getTextValue() { return getNodeValue(); }
  public boolean checkValid() { return false; }

  public void print(WriteStream out) throws IOException
  {
  }

  public void printPretty(WriteStream out) throws IOException
  {
  }

  public void printHtml(WriteStream out) throws IOException
  {
  }

  public boolean isSupported(String feature, String version)
  {
    return false;
  }

  public boolean hasAttributes()
  {
    return false;
  }

  public String toString()
  {
    return "NamespaceNode[" + _name + " " + _url + "]";
  }
}
