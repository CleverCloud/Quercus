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
 * @author Scott Ferguson
 */

package com.caucho.xml2;

import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class QDocumentType extends QNode implements DocumentType {
  String _name;
  HashMap<String,QElementDef> _elements = new HashMap<String,QElementDef>();
  HashMap<String,QEntity> _entities = new HashMap<String,QEntity>();
  HashMap<String,QNotation> _notations = new HashMap<String,QNotation>();
  HashMap<String,QEntity> _parameterEntities = new HashMap<String,QEntity>();
  HashMap<String,String> _ids = new HashMap<String,String>();
  String _systemId;
  String _publicId;

  boolean _hasAttributeDefaults;

  /**
   * Create a new document type.
   */
  public QDocumentType(String name)
  {
    this(name, null, null);
  }

  public QDocumentType(String name, String publicId, String systemId)
  {
    _name = name;

    _entities.put("amp", new QEntity("amp", "&"));
    _entities.put("lt", new QEntity("lt", "<"));
    _entities.put("gt", new QEntity("gt", ">"));
    _entities.put("quot", new QEntity("quot", "\""));
    _entities.put("apos", new QEntity("apos", "'"));

    _publicId = publicId;
    _systemId = systemId;
  }

  public String getNodeName() { return _name; }
  public String getTagName() { return "#documenttype"; }
  public short getNodeType() { return DOCUMENT_TYPE_NODE; }

  public String getPrefix() { return null; }
  public String getLocalName() { return null; }
  public String getNamespaceURI() { return null; }

  public String getName() { return _name; }
  public void setName(String name) { _name = name; }
  
  public NamedNodeMap getEntities()
  {
    return new QNamedNodeMap(_entities);
  }

  public NamedNodeMap getNotations()
  {
    return new QNamedNodeMap(_notations);
  }

  public void setLocation(String filename, int line, int col)
  {
  }

  Node importNode(QDocument owner, boolean deep) 
  {
    QDocumentType ref = new QDocumentType(_name);

    return ref;
  }

  void addNotation(QNotation notation)
  {
    _notations.put(notation._name, notation); 
  }

  public String getElementId(String element)
  {
    return _ids.get(element);
  }

  public Iterator getElementIdNames()
  {
    return _ids.keySet().iterator();
  }

  void setElementId(String element, String id)
  {
    _ids.put(element, id);
  }

  /**
   * Adds a new defined entity.
   */
  void addEntity(QEntity entity)
  {
    if (_entities.get(entity._name) == null)
      _entities.put(entity._name, entity); 
  }

  QEntity getEntity(String name)
  {
    return _entities.get(name);
  }

  void addParameterEntity(QEntity entity)
  {
    if (_parameterEntities.get(entity._name) == null)
      _parameterEntities.put(entity._name, entity); 
  }

  QEntity getParameterEntity(String name)
  {
    return _parameterEntities.get(name);
  }

  String getEntityValue(String name)
  {
    QEntity entity = _entities.get(name);

    if (entity == null)
      return null;
    else
      return entity._value;
  }

  public String getSystemId()
  {
    return _systemId;
  }

  protected void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  public String getPublicId()
  {
    return _publicId;
  }

  protected void setPublicId(String publicId)
  {
    _publicId = publicId;
  }

  public String getInternalSubset()
  {
    return null;
  }
  
  boolean isExternal()
  {
    return _systemId != null || _publicId != null;
  }

  public QElementDef getElement(String name)
  {
    return _elements.get(name);
  }

  QElementDef addElement(String name)
  {
    QElementDef def = _elements.get(name);
    if (def == null) {
      def = new QElementDef(name);
      def._dtd = this;
      def._owner = _owner;
      _elements.put(name, def);
      appendChild(def);
    }

    return def;
  }

  void setAttributeDefaults()
  {
    _hasAttributeDefaults = true;
  }

  boolean hasAttributeDefaults()
  {
    return _hasAttributeDefaults;
  }

  void fillDefaults(QElement element)
  {
    if (! _hasAttributeDefaults)
      return;

    QElementDef def = getElement(element.getNodeName());
    if (def != null)
      def.fillDefaults(element);
  }

  void print(XmlPrinter os) throws IOException
  {
    if (getName() == null)
      return;

    os.printHeader(getName());
    
    os.print("<!DOCTYPE ");
    os.print(getName());
    
    if (_publicId != null) {
      os.print(" PUBLIC \"");
      os.print(_publicId);
      os.print("\" \"");
      os.print(_systemId);
      os.print("\"");
    } else if (_systemId != null) {
      os.print(" SYSTEM \"");
      os.print(_systemId);
      os.print("\"");
    }

    if (_firstChild != null) {
      os.println(" [");

      for (QAbstractNode node = _firstChild; node != null; node = node._next) {
        node.print(os);
      }

      os.println("]>");
    } else
      os.println(">");
  }

  public String toString()
  {
    return "QDocumentType[" + _name + "]";
  }
}
