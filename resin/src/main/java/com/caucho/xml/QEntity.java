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

import org.w3c.dom.Entity;
import org.w3c.dom.Node;

import java.io.IOException;

class QEntity extends QNode implements Entity {
  String _name;
  String _value;
  String _publicId;
  String _systemId;
  String _ndata;
  boolean _isPe;
  boolean _isSpecial;

  QEntity(String name, String value)
  {
    _name = name;
    _value = value;
    _isSpecial = true;
    if (value != null)
      _firstChild = new QText(value);
  }

  QEntity(String name, String value, String publicId, String systemId)
  {
    _name = name;
    _value = value;
    _systemId = systemId;
    _publicId = publicId;
    if (value != null)
      _firstChild = new QText(value);
  }

  public String getNodeName() { return _name; }
  public String getTagName() { return _name; }
  public short getNodeType() { return Node.ENTITY_NODE; }

  public String getPublicId() { return _publicId; }
  public void setPublicId(String arg) { _publicId = arg; }

  public String getSystemId() { return _systemId; }
  public void setSystemId(String arg) { _systemId = arg; }

  public String getValue() { return _value; }
  public void setValue(String arg) { _value = arg; }

  public String getNotationName() { return _ndata; }
  public void setNotationName(String arg) { _ndata = arg; }

  Node importNode(QDocument owner, boolean deep) 
  {
    QEntity entity = new QEntity(_name, _value, _publicId, _systemId);

    return entity;
  }

  // DOM LEVEL 3
  
  public String getActualEncoding()
  {
    throw new UnsupportedOperationException();
  }
  
  public void setActualEncoding(String actualEncoding)
  {
    throw new UnsupportedOperationException();
  }

  public String getEncoding()
  {
    throw new UnsupportedOperationException();
  }
  
  public void setEncoding(String encoding)
  {
    throw new UnsupportedOperationException();
  }

  public String getVersion()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlVersion()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public String getInputEncoding()
  {
    throw new UnsupportedOperationException();
  }
  
  public void setVersion(String version)
  {
    throw new UnsupportedOperationException();
  }

  void print(XmlPrinter out) throws IOException
  {
    out.print("<!ENTITY ");
    if (_isPe)
      out.print("% ");
    out.print(_name);
    if (_publicId != null) {
      out.print(" PUBLIC \"");
      out.printDecl(_publicId);
      out.print("\"");
      if (_systemId != null) {
        out.print(" \"");
        out.printDecl(_systemId);
        out.print("\"");
      }

      if (_ndata != null) {
        out.print(" NDATA ");
        out.printDecl(_ndata);
      }
    } else if (_systemId != null) {
      out.print(" SYSTEM \"");
      out.printDecl(_systemId);
      out.print("\"");

      if (_ndata != null) {
        out.print(" NDATA ");
        out.printDecl(_ndata);
      }
    }
    else if (_value != null) {
      out.print(" \"");
      out.printDecl(_value);
      out.print("\"");
    }
    out.println(">");
  }

  public String toString()
  {
    if (_systemId != null)
      return "QEntity[" + _name + " SYSTEM \"" + _systemId + "\"]";
    else
      return "QEntity[" + _name + " \"" + _value + "\"]";
  }
}
