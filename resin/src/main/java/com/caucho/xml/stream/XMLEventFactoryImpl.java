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
* @author Adam Megacz
*/

package com.caucho.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.*;
import java.util.Iterator;
import java.util.HashMap;

import com.caucho.xml.stream.events.*;

public class XMLEventFactoryImpl extends XMLEventFactory {

  public XMLEventFactoryImpl()
  {
  }

  public Attribute createAttribute(QName name, String value)
  {
    return new AttributeImpl(name, value);
  }

  public Attribute createAttribute(String localName, String value)
  {
    return new AttributeImpl(new QName(localName), value);
  }

  public Attribute createAttribute(String prefix, String namespaceURI,
                                   String localName, String value)
  {
    return new AttributeImpl(new QName(namespaceURI, localName, prefix), value);
  }

  public Characters createCData(String content)
  {
    return new CharactersImpl(content, true, false, false);
  }

  public Characters createCharacters(String content)
  {
    return new CharactersImpl(content, false, false, false);
  }

  public Comment createComment(String text)
  {
    return new CommentImpl(text);
  }

  public DTD createDTD(String dtd)
  {
    return new DTDImpl(dtd);
  }

  public EndDocument createEndDocument()
  {
    return new EndDocumentImpl();
  }

  public EndElement createEndElement(QName name, Iterator namespaces)
  {
    return new EndElementImpl(name, namespaces);
  }

  public EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName)
  {
    return new EndElementImpl(new QName(namespaceUri, localName, prefix));
  }

  public EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName,
                                              Iterator namespaces)
  {
    return new EndElementImpl(new QName(namespaceUri, localName, prefix),
                              namespaces);
  }

  public EntityReference
    createEntityReference(String name, EntityDeclaration declaration)
  {
    return new EntityReferenceImpl(name, declaration);
  }

  public Characters createIgnorableSpace(String content)
  {
    return new CharactersImpl(content, false, true, false);
  }

  public Namespace createNamespace(String namespaceURI)
  {
    return new NamespaceImpl(namespaceURI, null);
  }

  public Namespace createNamespace(String prefix, String namespaceUri)
  {
    return new NamespaceImpl(namespaceUri, prefix);
  }

  public ProcessingInstruction
    createProcessingInstruction(String target, String data)
  {
    return new ProcessingInstructionImpl(target, data);
  }

  public Characters createSpace(String content)
  {
    return new CharactersImpl(content, false, false, true);
  }

  public StartDocument createStartDocument()
  {
    return new StartDocumentImpl();
  }

  public StartDocument createStartDocument(String encoding)
  {
    return new StartDocumentImpl(true, encoding, null, "1.0", false, false);
  }

  public StartDocument createStartDocument(String encoding,
                                                    String version)
  {
    return new StartDocumentImpl(true, encoding, null, version, false, false);
  }

  public StartDocument createStartDocument(String encoding,
                                                    String version,
                                                    boolean standalone)
  {
    return new StartDocumentImpl(true, encoding, 
                                 null, version, 
                                 standalone, true);
  }

  public StartElement createStartElement(QName name,
                                         Iterator attributes,
                                         Iterator namespaces)
  {
    return createStartElement(name, attributes, namespaces, null);
  }

  public StartElement createStartElement(String prefix,
                                         String namespaceUri,
                                         String localName)
  {
    return createStartElement(prefix, namespaceUri, localName, null, null);
  }

  public StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator attributes,
                                                  Iterator namespaces)
  {
    return createStartElement(prefix, namespaceUri, localName, 
                              attributes, namespaces, null);
  }

  public StartElement createStartElement(String prefix,
                                         String namespaceUri,
                                         String localName,
                                         Iterator attributes,
                                         Iterator namespaces,
                                         NamespaceContext context)
  {
    return createStartElement(new QName(namespaceUri, localName, prefix),
                              attributes, namespaces, context);
  }

  private StartElement createStartElement(QName name,
                                          Iterator attributes,
                                          Iterator namespaces,
                                          NamespaceContext context)
  {
    HashMap<QName, Attribute> attributeMap = new HashMap<QName, Attribute>();
    HashMap<String, Namespace> namespaceMap = new HashMap<String, Namespace>();

    if (attributes != null) {
      while (attributes.hasNext()) {
        Attribute attribute = (Attribute) attributes.next();
        attributeMap.put(attribute.getName(), attribute);
      }
    }

    if (namespaces != null) {
      while (namespaces.hasNext()) {
        Namespace namespace = (Namespace) namespaces.next();
        namespaceMap.put(namespace.getPrefix(), namespace);
      }
    }

    return new StartElementImpl(name, attributeMap, namespaceMap, context);
  }

  public void setLocation(Location location)
  {
    throw new UnsupportedOperationException();
  }

}

