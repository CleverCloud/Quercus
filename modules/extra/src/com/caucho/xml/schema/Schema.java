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
 * @author Emil Ong
 */

package com.caucho.xml.schema;

import java.io.*;
import java.util.*;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import javax.xml.namespace.QName;

/**
 * JAXB annotated Schema data structure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="schema", namespace=W3C_XML_SCHEMA_NS_URI)
public class Schema {
  @XmlElement(name="element", namespace=W3C_XML_SCHEMA_NS_URI)
  private List<Element> _elements;

  @XmlElements({
    @XmlElement(name="complexType", 
                namespace=W3C_XML_SCHEMA_NS_URI,
                type=com.caucho.xml.schema.ComplexType.class)})
  private List<Type> _types;

  @XmlElement(name="import", namespace=W3C_XML_SCHEMA_NS_URI)
  private List<Import> _imports;

  @XmlAttribute(name="targetNamespace")
  private String _targetNamespace;

  @XmlAttribute(name="version")
  private String _version;

  @XmlAttribute(name="elementFormDefault")
  private String _elementFormDefault;

  public String getElementFormDefault()
  {
    return _elementFormDefault;
  }

  public String getTargetNamespace()
  {
    return _targetNamespace;
  }

  public String getVersion()
  {
    return _version;
  }

  public void afterUnmarshal(Unmarshaller u, Object parent)
  {
    if (_types != null) {
      for (Type type : _types)
        type.setSchema(this);
    }

    if (_elements != null) {
      for (Element element : _elements)
        element.setSchema(this);
    }
  }

  public void resolveImports(Unmarshaller u)
    throws JAXBException
  {
    if (_imports != null) {
      for (int i = 0; i < _imports.size(); i++) {
        Import imp = _imports.get(i);
        imp.resolve(u);

        if (imp.getSchema() != null)
          imp.getSchema().resolveImports(u);
      }
    }
  }

  public void writeJAXBClasses(File outputDirectory, String pkg)
    throws IOException
  {
    if (_types != null) {
      for (Type type : _types)
        type.writeJava(outputDirectory, pkg);
    }

    if (_imports != null) {
      for (int i = 0; i < _imports.size(); i++) {
        Import imp = _imports.get(i);

        if (imp.getSchema() != null)
          imp.getSchema().writeJAXBClasses(outputDirectory, pkg);
      }
    }

    // XXX Elements -> ObjectFactory 
  }

  public Type getType(QName typeName)
  {
    if (typeName.getNamespaceURI() == null ||
        typeName.getNamespaceURI().equals(_targetNamespace)) {
      // look at the immediate children types
      if (_types != null) {
        for (int i = 0; i < _types.size(); i++) {
          Type type = _types.get(i);

          if (type.getName().equals(typeName.getLocalPart()))
            return type;
        }
      }

      if (_imports != null) {
        for (int i = 0; i < _imports.size(); i++) {
          Import imp = _imports.get(i);
          Schema schema = imp.getSchema();

          if (schema != null && 
              (schema.getTargetNamespace() == null ||
               schema.getTargetNamespace().equals(getTargetNamespace()))) {
            Type type = schema.getType(typeName);

            if (type != null)
              return type;
          }
        }
      }
    }
    else {
      // look for the children in imported/included schema
      if (_imports != null) {
        for (int i = 0; i < _imports.size(); i++) {
          Import imp = _imports.get(i);
          Schema schema = imp.getSchema();

          if (schema != null && 
              schema.getTargetNamespace().equals(typeName.getNamespaceURI())) {
            Type type = schema.getType(typeName);

            if (type != null)
              return type;
          }
        }
      }
    }

    return null;
  }
}
