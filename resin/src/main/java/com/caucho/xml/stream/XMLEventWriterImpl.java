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
* @author Emil Ong
*/

package com.caucho.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import static javax.xml.stream.XMLStreamConstants.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.*;

import java.util.Iterator;

public class XMLEventWriterImpl implements XMLEventWriter {
  private XMLStreamWriter _out;

  public XMLEventWriterImpl(XMLStreamWriter out)
  {
    _out = out;
  }

  public XMLStreamWriter getXMLStreamWriter()
  {
    return _out;
  }

  public void add(XMLEvent event) throws XMLStreamException
  {
    // Order important: Namespace extends Attribute, so it comes first
    if (event instanceof Namespace) {
      Namespace namespace = (Namespace) event;
      
      if (namespace.isDefaultNamespaceDeclaration())
        _out.writeDefaultNamespace(namespace.getNamespaceURI());
      else
        _out.writeNamespace(namespace.getPrefix(), 
                            namespace.getNamespaceURI());

    }
    else if (event instanceof Attribute) {
      Attribute attribute = (Attribute) event;
      QName name = attribute.getName();

      if (name.getPrefix() != null && ! "".equals(name.getPrefix())) {
        _out.writeAttribute(name.getPrefix(), name.getNamespaceURI(),
                            name.getLocalPart(), attribute.getValue());
      }
      else if (name.getNamespaceURI() != null && 
               ! "".equals(name.getNamespaceURI())) {
        _out.writeAttribute(name.getNamespaceURI(), name.getLocalPart(), 
                            attribute.getValue());
      }
      else
        _out.writeAttribute(name.getLocalPart(), attribute.getValue());
    } 
    else if (event instanceof Characters) {
      Characters characters = (Characters) event;

      switch (characters.getEventType()) {
        case CDATA:
          _out.writeCData(characters.getData());
          break;

        case SPACE:
        case CHARACTERS: 
        default:
          _out.writeCharacters(characters.getData());
          break;
      }
    } 
    else if (event instanceof Comment) {
      Comment comment = (Comment) event;

      _out.writeComment(comment.getText());
    } 
    else if (event instanceof DTD) {
      DTD dtd = (DTD) event;

      _out.writeDTD(dtd.getDocumentTypeDeclaration());
    } 
    else if (event instanceof EndDocument) {
      _out.writeEndDocument();
    } 
    else if (event instanceof EndElement) {
      _out.writeEndElement();
    } 
    else if (event instanceof EntityDeclaration) {
      throw new UnsupportedOperationException();
    } 
    else if (event instanceof EntityReference) {
      throw new UnsupportedOperationException();
    } 
    else if (event instanceof NotationDeclaration) {
      throw new UnsupportedOperationException();
    } 
    else if (event instanceof ProcessingInstruction) {
      ProcessingInstruction pi = (ProcessingInstruction) event;

      if (pi.getData() == null || "".equals(pi.getData()))
        _out.writeProcessingInstruction(pi.getTarget());
      else
        _out.writeProcessingInstruction(pi.getTarget(), pi.getData());

    }
    else if (event instanceof StartDocument) {
      StartDocument startDocument = (StartDocument) event;

      if (startDocument.encodingSet()) {
        _out.writeStartDocument(startDocument.getCharacterEncodingScheme(),
                                startDocument.getVersion());
      }
      else if (startDocument.getVersion() != null &&
               ! "".equals(startDocument.getVersion())) {
        _out.writeStartDocument(startDocument.getVersion());
      }
      else
        _out.writeStartDocument();
    }
    else if (event instanceof StartElement) {
      StartElement startElement = (StartElement) event;
      QName name = startElement.getName();

      // Namespaces
      // We do namespaces first because the element itself may need one
      // xml/300w should catch this
      Iterator namespaces = startElement.getNamespaces();

      while (namespaces.hasNext())
        add((Namespace) namespaces.next());

      if (name.getPrefix() != null && ! "".equals(name.getPrefix())) {
        _out.writeStartElement(name.getPrefix(), 
                               name.getLocalPart(),
                               name.getNamespaceURI());
      }
      else if (name.getNamespaceURI() != null && 
               ! "".equals(name.getNamespaceURI())) {
        _out.writeStartElement(name.getNamespaceURI(), name.getLocalPart());
      }
      else 
        _out.writeStartElement(name.getLocalPart());

      // Attributes
      Iterator attributes = startElement.getAttributes();

      while (attributes.hasNext())
        add((Attribute) attributes.next());
    }
    else
      throw new XMLStreamException();
  }

  public void add(XMLEventReader reader) throws XMLStreamException
  {
    while (reader.hasNext())
      add(reader.nextEvent());
  }

  public void close() throws XMLStreamException
  {
    _out.close();
  }

  public void flush() throws XMLStreamException
  {
    _out.flush();
  }

  public NamespaceContext getNamespaceContext()
  {
    return _out.getNamespaceContext();
  }

  public String getPrefix(String uri) throws XMLStreamException
  {
    return _out.getPrefix(uri);
  }

  public void setDefaultNamespace(String uri) throws XMLStreamException
  {
    _out.setDefaultNamespace(uri);
  }

  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException
  {
    _out.setNamespaceContext(context);
  }

  public void setPrefix(String prefix, String uri) throws XMLStreamException
  {
    _out.setPrefix(prefix, uri);
  }

}
