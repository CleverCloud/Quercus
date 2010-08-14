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

import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;

class QElementDef extends QNode {
  String _name;
  Object _content;
  ArrayList<QAttributeDef> _attr;
  boolean _hasDefault;
  QDocumentType _dtd;

  QElementDef(String name)
  {
    _name = name;
  }

  public String getNodeName() { return "#element"; }
  public String getTagName() { return "#element"; }
  public short getNodeType() { return Node.ELEMENT_NODE; }

  Node importNode(QDocument owner, boolean deep) 
  {
    QElementDef def = new QElementDef(_name);

    return def;
  }

  public void addAttribute(String name, String type, ArrayList enumeration,
                           String qualifier, String deflt)
  {
    if (_attr == null)
      _attr = new ArrayList<QAttributeDef>();

    if (deflt != null) {
      _hasDefault = true;
      _dtd.setAttributeDefaults();
    }
    
    _attr.add(new QAttributeDef(name, type, enumeration, qualifier, deflt));
  }

  void fillDefaults(QElement element)
  {
    if (! _hasDefault)
      return;

    for (int i = 0; i < _attr.size(); i++) {
      QAttributeDef attrDef = _attr.get(i);
      if (attrDef._deflt != null && 
          element.getAttribute(attrDef._name).equals("")) {
        QAttr attr = (QAttr) element._owner.createAttribute(attrDef._name,
                                                            attrDef._deflt);
        attr._owner = element._owner;
        attr.setSpecified(false);
        element.setAttributeNode(attr);
      }
    }
  }

  void fillDefaults(QAttributes attributes)
  {
    if (! _hasDefault)
      return;

    for (int i = 0; i < _attr.size(); i++) {
      QAttributeDef attrDef = _attr.get(i);
      if (attrDef._deflt != null && 
          attributes.getIndex(attrDef._name) < 0) {
        attributes.add(new QName(null, attrDef._name, null), attrDef._deflt);
      }
    }
  }

  public void print(XmlPrinter os) throws IOException
  {
    if (_content != null) {
      os.print("<!ELEMENT ");
      os.print(_name);
      os.print(" ");
      if (_content instanceof QContentParticle)
        ((QContentParticle) _content).print(os);
      else
        os.print(String.valueOf(_content));
      os.println(">");
    }

    if (_attr != null) {
      os.print("<!ATTLIST ");
      os.print(_name);

      for (int i = 0; i < _attr.size(); i++) {
        QAttributeDef attribute = _attr.get(i);

        if (_attr.size() == 1)
          os.print(" ");
        else
          os.print("\n  ");
        os.print(attribute._name);
        if (attribute._type.equals("#ENUM")) {
          os.print(" (");
          for (int j = 0; j < attribute._enumeration.size(); j++) {
            String enumType = attribute._enumeration.get(j);

            if (j != 0)
              os.print(" | ");
            os.print(enumType);
          }
          os.print(")");
        } else if (attribute._type.equals("NOTATION")) {
          os.print(" NOTATION (");
          for (int j = 0; j < attribute._enumeration.size(); j++) {
            String enumType = attribute._enumeration.get(j);

            if (j != 0)
              os.print(" | ");
            os.print(enumType);
          }
          os.print(")");
        } else {
          os.print(" ");
          os.print(attribute._type);
        }

        if (attribute._qualifier != null) {
          os.print(" ");
          os.print(attribute._qualifier);
        }
        if (attribute._deflt != null) {
          os.print(" \"");
          os.print(attribute._deflt);
          os.print("\"");
        }
      }
      os.println(">");
    }
  }
}
