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

import com.caucho.vfs.WriteStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class XMLStreamWriterRepairingWrapper implements XMLStreamWriter {
  private XMLStreamWriter _out;

  public XMLStreamWriterRepairingWrapper(XMLStreamWriter out)
  {
    _out = out;
  }

  public void close()
    throws XMLStreamException
  {
    _out.close();
  }

  public void flush()
    throws XMLStreamException
  {
    _out.flush();
  }

  public NamespaceContext getNamespaceContext()
  {
    return _out.getNamespaceContext();
  }

  public String getPrefix(String uri)
    throws XMLStreamException
  {
    return _out.getPrefix(uri);
  }

  public Object getProperty(String name)
  {
    if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name))
      return true;

    return _out.getProperty(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _out.setDefaultNamespace(uri);
  }

  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException
  {
    _out.setNamespaceContext(context);
  }

  public void setPrefix(String prefix, String uri)
    throws XMLStreamException
  {
    _out.setPrefix(prefix, uri);
  }

  public void writeAttribute(String localName, String value)
    throws XMLStreamException
  {
    _out.writeAttribute(localName, value);
  }

  public void writeAttribute(String namespaceURI, 
                             String localName, 
                             String value)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, namespaceURI);

    _out.writeAttribute(namespaceURI, localName, value);
  }

  public void writeAttribute(String prefix, 
                             String namespaceURI, 
                             String localName, 
                             String value)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, prefix, namespaceURI);

    _out.writeAttribute(prefix, namespaceURI, localName, value);
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    _out.writeCData(data);
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    _out.writeCharacters(text, start, len);
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    _out.writeCharacters(text);
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    _out.writeComment(data);
  }

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException
  {
    _out.writeDefaultNamespace(namespaceURI);
  }

  public void writeDTD(String dtd)
    throws XMLStreamException
  {
    _out.writeDTD(dtd);
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    _out.writeEmptyElement(localName);
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, namespaceURI);

    _out.writeEmptyElement(namespaceURI, localName);
  }

  public void writeEmptyElement(String prefix, 
                                String localName, 
                                String namespaceURI)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, prefix, namespaceURI);

    _out.writeEmptyElement(prefix, localName, namespaceURI);
  }

  public void writeEndDocument()
    throws XMLStreamException
  {
    _out.writeEndDocument();
  }

  public void writeEndElement()
    throws XMLStreamException
  {
    _out.writeEndElement();
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    _out.writeEntityRef(name);
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    _out.writeNamespace(prefix, namespaceURI);
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    _out.writeProcessingInstruction(target);
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    _out.writeProcessingInstruction(target, data);
  }

  public void writeStartDocument()
    throws XMLStreamException
  {
    _out.writeStartDocument();
  }

  public void writeStartDocument(String version)
    throws XMLStreamException
  {
    _out.writeStartDocument(version);
  }

  public void writeStartDocument(String encoding, String version)
    throws XMLStreamException
  {
    _out.writeStartDocument(encoding, version);
  }

  public void writeStartElement(String localName)
    throws XMLStreamException
  {
    _out.writeStartElement(localName);
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, namespaceURI);

    _out.writeStartElement(namespaceURI, localName);
  }

  public void writeStartElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException
  {
    StaxUtil.repairNamespace(_out, prefix, namespaceURI);

    _out.writeStartElement(prefix, localName, namespaceURI);
  }
}

