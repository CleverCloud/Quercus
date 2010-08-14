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

package javax.xml.stream;
import javax.xml.namespace.NamespaceContext;

public interface XMLStreamWriter {

  public void close() throws XMLStreamException;

  public void flush() throws XMLStreamException;

  public NamespaceContext getNamespaceContext();

  public String getPrefix(String uri)
    throws XMLStreamException;

  public Object getProperty(String name)
    throws IllegalArgumentException;

  public void setDefaultNamespace(String uri)
    throws XMLStreamException;

  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException;

  public void setPrefix(String prefix, String uri)
    throws XMLStreamException;

  public void writeAttribute(String localName, String value)
    throws XMLStreamException;

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException;

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException;

  public void writeCData(String data)
    throws XMLStreamException;

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException;

  public void writeCharacters(String text)
    throws XMLStreamException;

  public void writeComment(String data)
    throws XMLStreamException;

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException;

  public void writeDTD(String dtd)
    throws XMLStreamException;

  public void writeEmptyElement(String localName)
    throws XMLStreamException;

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException;

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException;

  public void writeEndDocument()
    throws XMLStreamException;

  public void writeEndElement()
    throws XMLStreamException;

  public void writeEntityRef(String name)
    throws XMLStreamException;

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException;

  public void writeProcessingInstruction(String target)
    throws XMLStreamException;

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException;

  public void writeStartDocument()
    throws XMLStreamException;

  public void writeStartDocument(String version)
    throws XMLStreamException;

  public void writeStartDocument(String encoding, String version)
    throws XMLStreamException;

  public void writeStartElement(String localName)
    throws XMLStreamException;

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException;

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException;
}

