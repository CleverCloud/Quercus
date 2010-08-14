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

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.events.*;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * XML pull-parser interface.
 */
public class SAXSourceXMLEventReaderImpl implements XMLEventReader {
  private static final Logger log
    = Logger.getLogger(SAXSourceXMLEventReaderImpl.class.getName());
  private static final L10N L = new L10N(SAXSourceXMLEventReaderImpl.class);

  private static final XMLEventFactory EVENT_FACTORY
    = XMLEventFactory.newInstance();

  private static SAXParserFactory _saxParserFactory;
  private static final String NAMESPACE_FEATURE 
    = "http://xml.org/sax/features/namespaces";
  private static final String PREFIX_FEATURE 
    = "http://xml.org/sax/features/namespace-prefixes";

  private final ArrayList<XMLEvent> _events = new ArrayList<XMLEvent>();

  private final EventGeneratingContentHandler _contentHandler;

  public static SAXParserFactory getSAXParserFactory()
    throws ParserConfigurationException, SAXException
  {
    if (_saxParserFactory == null) {
      _saxParserFactory = SAXParserFactory.newInstance();
      _saxParserFactory.setFeature(NAMESPACE_FEATURE, true);
      _saxParserFactory.setFeature(PREFIX_FEATURE, true);
      _saxParserFactory.setNamespaceAware(true);
    }

    return _saxParserFactory;
  }

  public SAXSourceXMLEventReaderImpl(SAXSource source)
    throws XMLStreamException
  {
    XMLReader reader = source.getXMLReader();

    if (reader == null) {
      try {
        reader = XMLReaderFactory.createXMLReader();

        source.setXMLReader(reader);
      } 
      catch (SAXException e) {
        throw new XMLStreamException(e);
      }
    }

    _contentHandler = new EventGeneratingContentHandler();
    reader.setContentHandler(_contentHandler);

    try {
      reader.parse(source.getInputSource());
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public SAXSourceXMLEventReaderImpl()
  {
    _contentHandler = new EventGeneratingContentHandler();
  }

  public ContentHandler getContentHandler()
  {
    return _contentHandler;
  }

  public void close() 
    throws XMLStreamException
  {
  }

  public String getElementText() 
    throws XMLStreamException
  {
    // XXX check precondition that the current element is start element

    StringBuilder sb = new StringBuilder();
    XMLEvent event = null;

    for (event = peek(); ! event.isEndElement(); event = peek()) {
      if (! event.isCharacters())
        throw new XMLStreamException("Unexpected event: " + event);

      event = nextEvent();

      sb.append(((Characters) event).getData());
    }

    return sb.toString();
  }

  public Object getProperty(String name) 
    throws IllegalArgumentException
  {
    throw new IllegalArgumentException(name);
  }

  public boolean hasNext()
  {
    return _events.size() > 0;
  }

  public XMLEvent nextEvent() 
    throws XMLStreamException
  {
    try {
      return _events.remove(0);
    }
    catch (IndexOutOfBoundsException e) {
      throw new NoSuchElementException();
    }
  }

  public XMLEvent nextTag() 
    throws XMLStreamException
  {
    XMLEvent event = null;

    for (event = nextEvent(); 
         ! event.isStartElement() && ! event.isEndElement();
         event = nextEvent()) {
      if (event.getEventType() != XMLStreamConstants.SPACE)
        throw new XMLStreamException("Unexpected event: " + event);
    }

    return event;
  }

  public XMLEvent peek() throws XMLStreamException
  {
    try {
      return _events.get(0);
    }
    catch (IndexOutOfBoundsException e) {
      throw new NoSuchElementException();
    }
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }

  public XMLEvent next()
  {
    try {
      return nextEvent();
    }
    catch (XMLStreamException e) {
      return null;
    }
  }

  private class EventGeneratingContentHandler implements ContentHandler {
    private NamespaceReaderContext _context = new NamespaceReaderContext();
    private ArrayList<Namespace> _newMappings = new ArrayList<Namespace>();
    private ArrayList<Namespace> _oldMappings = new ArrayList<Namespace>();
    private QName _pendingEndName = null;

    public void characters(char[] ch, int start, int length)
      throws SAXException
    {
      checkForPendingEndElement();

      String s = new String(ch, start, length);
      _events.add(EVENT_FACTORY.createCharacters(s));
    }

    public void endDocument()
      throws SAXException
    {
      checkForPendingEndElement();

      _events.add(EVENT_FACTORY.createEndDocument());
    }

    public void endElement(String uri, String localName, String qName)
    {
      int colon = qName.indexOf(':');

      if (colon < 0) {
        if (uri == null || "".equals(uri))
          _pendingEndName = new QName(localName);
        else
          _pendingEndName = new QName(uri, localName);
      }
      else {
        String prefix = qName.substring(0, colon);
        _pendingEndName = new QName(uri, localName, prefix);
      }
    }

    public void endPrefixMapping(String prefix)
      throws SAXException
    {
      String uri = _context.getUri(prefix);

      if (uri == null)
        throw new SAXException("Unknown prefix: " + prefix);

      _oldMappings.add(EVENT_FACTORY.createNamespace(prefix, uri));
    }

    private void checkForPendingEndElement()
      throws SAXException
    {
      if (_pendingEndName != null) {
        Iterator iterator = _oldMappings.iterator();

        _events.add(EVENT_FACTORY.createEndElement(_pendingEndName, iterator));

        _pendingEndName = null;
        _oldMappings = new ArrayList<Namespace>();
        
        try {
          _context.pop();
        }
        catch (XMLStreamException e) {
          throw new SAXException(e);
        }
      }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException
    {
      checkForPendingEndElement();

      String s = new String(ch, start, length);
      _events.add(EVENT_FACTORY.createIgnorableSpace(s));
    }

    public void processingInstruction(String target, String data)
      throws SAXException
    {
      checkForPendingEndElement();

      _events.add(EVENT_FACTORY.createProcessingInstruction(target, data));
    }

    public void setDocumentLocator(Locator locator)
    {
      // XXX
    }

    public void skippedEntity(String name)
    {
      // XXX
    }

    public void startDocument()
    {
      _events.add(EVENT_FACTORY.createStartDocument());
    }

    public void startElement(String uri, String localName, String qName, 
                             Attributes atts)
      throws SAXException
    {
      checkForPendingEndElement();
      _context.push();

      Iterator attributeIterator = null;
      Iterator namespaceIterator = null;
      
      if (atts.getLength() > 0) {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        ArrayList<Namespace> namespaces = new ArrayList<Namespace>();

        namespaces.addAll(_newMappings);
        _newMappings.clear();

        for (int i = 0; i < atts.getLength(); i++) {
          Attribute attribute = null;
          Namespace namespace = null;

          String qualified = atts.getQName(i);
          String local = atts.getLocalName(i);
          String namespaceURI = atts.getURI(i);
          String value = atts.getValue(i);

          int colon = qualified.indexOf(':');

          if (colon < 0) {
            if (namespaceURI == null || "".equals(namespaceURI))
              attribute = EVENT_FACTORY.createAttribute(local, value);
            else {
              if (XMLConstants.XMLNS_ATTRIBUTE.equals(local))
                namespace = EVENT_FACTORY.createNamespace(value);
              else {
                QName name = new QName(namespaceURI, local);
                attribute = EVENT_FACTORY.createAttribute(name, value);
              }
            }
          }
          else {
            String prefix = qualified.substring(0, colon);

            if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
              namespace = EVENT_FACTORY.createNamespace(local, value);
            else {
              attribute = EVENT_FACTORY.createAttribute(prefix, namespaceURI, 
                                                        local, value);
            }
          }

          if (attribute != null)
            attributes.add(attribute);

          if (namespace != null) {
            _context.declare(namespace.getPrefix(), 
                             namespace.getNamespaceURI());
            
            namespaces.add(namespace);
          }
        }

        attributeIterator = attributes.iterator();
        namespaceIterator = namespaces.iterator();
      }

      QName name = null;

      int colon = qName.indexOf(':');

      if (colon < 0) {
        if (localName == null)
          localName = qName;

        if (uri == null || "".equals(uri))
          name = new QName(localName);
        else
          name = new QName(uri, localName);
      }
      else {
        String prefix = qName.substring(0, colon);
        name = new QName(uri, localName, prefix);
      }

      StartElement start = EVENT_FACTORY.createStartElement(name, 
                                                            attributeIterator, 
                                                            namespaceIterator);
      _events.add(start);
    }

    public void startPrefixMapping(String prefix, String uri)
      throws SAXException
    {
      checkForPendingEndElement();

      _newMappings.add(EVENT_FACTORY.createNamespace(prefix, uri));
      _context.declare(prefix, uri);
    }
  }
}
