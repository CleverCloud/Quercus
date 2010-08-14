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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;

import java.io.IOException;

public class QAttr extends QNode implements Attr {
  QName _name;
  private String _value;
  private boolean _specified = true;

  public QAttr(String name)
  {
    _name = new QName(name);
  }

  public QAttr(QName name)
  {
    _name = name;
  }

  protected QAttr(QName name, String value)
  { 
    _name = name; 
    _value = value;
  }

  protected QAttr(QDocument owner, QName name)
  {
    super(owner);

    _name = name;
  }

  public Element getOwnerElement()
  {
    return (Element) getParentNode();
  }

  public short getNodeType()
  {
    return ATTRIBUTE_NODE;
  }

  /**
   * Returns the full QName.
   */
  public QName getQName()
  {
    return _name;
  }

  public String getNodeName()
  {
    return _name.getName();
  }

  public boolean isId()
  {
    return false;
  }

  public String getName()
  {
    return _name.getName();
  }

  public String getPrefix()
  {
    return _name.getPrefix();
  }

  public String getLocalName()
  {
    return _name.getLocalName();
  }

  public String getCanonicalName()
  {
    return _name.getCanonicalName();
  }

  public String getNamespaceURI()
  {
    return _name.getNamespace();
  }

  public String getNodeValue()
  {
    return _value;
  }

  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  public void setNodeValue(String value)
  {
    _value = value;
  }

  public String getValue()
  {
    return _value;
  }

  public void setValue(String value)
  {
    _value = value;
  }

  public boolean getSpecified()
  {
    return _specified;
  }
  
  public void setSpecified(boolean specified)
  {
    _specified = specified;
  }

  Node importNode(QDocument owner, boolean deep) 
  {
    QNode node = new QAttr(_name, _value);
    node._owner = owner;
    return node;
  }

  public void print(XmlPrinter out) throws IOException
  {
    if (! _specified)
      return;

    out.attribute(getNamespaceURI(), getLocalName(),
                  getNodeName(), getNodeValue());
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }

  public String toString()
  {
    if (_value != null)
      return "Attr[" + _name + " " + _value + "]";
    else
      return "Attr[" + _name + "]";
  }
}
