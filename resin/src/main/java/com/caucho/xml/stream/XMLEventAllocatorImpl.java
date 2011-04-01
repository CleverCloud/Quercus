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

import com.caucho.xml.stream.events.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import static javax.xml.stream.XMLStreamConstants.*;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.*;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;

import java.util.HashMap;

public class XMLEventAllocatorImpl implements XMLEventAllocator {
  private static final XMLEventFactory EVENT_FACTORY
    = XMLEventFactory.newInstance();

  public XMLEvent allocate(XMLStreamReader reader)
    throws XMLStreamException
  {
    switch (reader.getEventType()) {
      case ATTRIBUTE: 
        // won't happen: our stream reader does not return attributes
        // independent of start elements/empty elements
        break;

      case CDATA:
        return EVENT_FACTORY.createCData(reader.getText());

      case CHARACTERS: 
        return EVENT_FACTORY.createCharacters(reader.getText());

      case COMMENT:
        return EVENT_FACTORY.createComment(reader.getText());

      case DTD:
        // XXX
        break;

      case END_DOCUMENT:
        return EVENT_FACTORY.createEndDocument();

      case END_ELEMENT:
        // XXX namespaces?
        return EVENT_FACTORY.createEndElement(reader.getName(), null);

      case ENTITY_DECLARATION:
        // XXX
        break;

      case ENTITY_REFERENCE:
        // XXX
        break;

      case NAMESPACE:
        // won't happen: our stream reader does not return attributes
        // independent of start elements/empty elements
        break;

      case NOTATION_DECLARATION:
        // XXX
        break;

      case PROCESSING_INSTRUCTION:
        return EVENT_FACTORY.createProcessingInstruction(reader.getPITarget(),
                                                         reader.getPIData());

      case SPACE:
        NamespaceContextImpl context = 
          (NamespaceContextImpl) reader.getNamespaceContext();
        
        if (context.getDepth() == 0)
          return EVENT_FACTORY.createIgnorableSpace(reader.getText());

        return EVENT_FACTORY.createSpace(reader.getText());

      case START_DOCUMENT:
        boolean encodingSet = true;
        String encoding = reader.getCharacterEncodingScheme();

        if (encoding == null) {
          encoding = "utf-8"; // XXX
          encodingSet = false;
        }

        return new StartDocumentImpl(encodingSet, encoding, 
                                     null /* XXX: system id */, 
                                     reader.getVersion(), 
                                     reader.isStandalone(), 
                                     reader.standaloneSet());

      case START_ELEMENT:
        HashMap<QName,Attribute> attributes = new HashMap<QName,Attribute>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
          Attribute attribute = new AttributeImpl(reader.getAttributeName(i),
                                                  reader.getAttributeValue(i));
          attributes.put(reader.getAttributeName(i), attribute);
        }

        HashMap<String,Namespace> namespaces= new HashMap<String,Namespace>();

        for (int i = 0; i < reader.getNamespaceCount(); i++) {
          String prefix = reader.getNamespacePrefix(i);

          if (prefix == null)
            prefix = XMLConstants.DEFAULT_NS_PREFIX;

          Namespace namespace = 
            new NamespaceImpl(reader.getNamespaceURI(i), prefix);
                            
          namespaces.put(prefix, namespace);
        }

        // bypass factory
        return new StartElementImpl(reader.getName(), attributes, namespaces,
                                    reader.getNamespaceContext());
    }

    throw new XMLStreamException("Event type = " + reader.getEventType());
  }

  public void allocate(XMLStreamReader reader, XMLEventConsumer consumer)
    throws XMLStreamException
  {
    consumer.add(allocate(reader));
  }

  public XMLEventAllocator newInstance()
  {
    return new XMLEventAllocatorImpl();
  }
}
