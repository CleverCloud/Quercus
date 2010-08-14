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

import org.w3c.dom.Node;
import org.w3c.dom.Notation;

import java.io.IOException;

class QNotation extends QNode implements Notation {
  String _name;
  String _publicId;
  String _systemId;

  QNotation(String name, String publicId, String systemId)
  {
    _name = name;
    _publicId = publicId;
    _systemId = systemId;
  }

  public String getNodeName() { return _name; }
  public String getTagName() { return _name; }
  public short getNodeType() { return Node.NOTATION_NODE; }

  public String getPublicId() { return _publicId; }
  public void setPublicId(String arg) { _publicId = arg; }

  public String getSystemId() { return _systemId; }
  public void setSystemId(String arg) { _systemId = arg; }

  Node importNode(QDocument owner, boolean deep) 
  {
    QNotation notation = new QNotation(_name, _publicId, _systemId);

    return notation;
  }

  public void print(XmlPrinter os) throws IOException
  {
    os.print("<!NOTATION ");
    os.print(_name);
    if (_publicId != null) {
      os.print(" PUBLIC \"");
      os.print(_publicId);
      os.print("\"");
      if (_systemId != null) {
        os.print(" \"");
        os.print(_systemId);
        os.print("\"");
      }
    } else if (_systemId != null) {
      os.print(" SYSTEM \"");
      os.print(_systemId);
      os.print("\"");
    }
    os.println(">");
  }
}
