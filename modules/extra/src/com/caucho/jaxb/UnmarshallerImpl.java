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

package com.caucho.jaxb;

import com.caucho.jaxb.adapters.BeanAdapter;
import com.caucho.jaxb.skeleton.ClassSkeleton;
import com.caucho.jaxb.property.Property;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.adapters.*;
import javax.xml.bind.attachment.*;
import javax.xml.bind.helpers.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import javax.xml.validation.*;

import java.net.URL;
import java.io.*;
import java.util.*;

// XXX extends AbstractUnmarshallerImpl
public class UnmarshallerImpl implements Unmarshaller
{
  private static final L10N L = new L10N(UnmarshallerImpl.class);

  private JAXBContextImpl _context;
  
  protected boolean validating;

  private AttachmentUnmarshaller _attachmentUnmarshaller = null;
  private ValidationEventHandler _validationEventHandler = null;
  private Listener _listener = null;
  private HashMap<String,Object> _properties = new HashMap<String,Object>();
  private Schema _schema = null;
  private XMLReader _xmlreader = null;
  private XmlAdapter _adapter = null;
  private HashMap<Class,XmlAdapter> _adapters
    = new HashMap<Class,XmlAdapter>();

  UnmarshallerImpl(JAXBContextImpl context)
    throws JAXBException
  {
    _context = context;
    setEventHandler(JAXBContextImpl.DEFAULT_VALIDATION_EVENT_HANDLER);
  }

  //
  // unmarshallers.
  //

  /**
   * Unmarshall from a DOM node.
   */
  public Object unmarshal(Node node) throws JAXBException
  {
    try {
      XMLInputFactory factory = _context.getXMLInputFactory();
      
      return unmarshal(factory.createXMLStreamReader(new DOMSource(node)));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  /**
   * Unmarshall from an input source.
   */
  protected Object unmarshal(XMLReader reader, InputSource source)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Unmarshall from an event reader.
   */
  public Object unmarshal(XMLEventReader reader) throws JAXBException
  {
    throw new UnsupportedOperationException();

    /*
    try {
      XMLEvent event = null;

      while (reader.hasNext()) {
        event = reader.peek();

        if (event.isStartElement()) {
          StartElement start = (StartElement) event;

          ClassSkeleton skel = _context.getRootElement(start.getName());

          if (skel == null)
            throw new JAXBException(L.l("'{0}' is an unknown root element",
                  start.getName()));

          return skel.read(this, reader);
        }

        event = reader.nextEvent();
      }
      
      throw new JAXBException(L.l("Expected root element"));
    }
    catch (JAXBException e) {
      throw e;
    } 
    catch (Exception e) {
      throw new RuntimeException(e);
    }*/
  }

  public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader,
                                      Class<T> declaredType)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object unmarshal(XMLStreamReader reader)
    throws JAXBException
  {
    try {
      if (reader.nextTag() != XMLStreamReader.START_ELEMENT)
        throw new JAXBException(L.l("Expected root element"));

      ClassSkeleton skel = _context.getRootElement(reader.getName());

      if (skel == null)
        throw new JAXBException(L.l("'{0}' is an unknown root element",
              reader.getName()));

      return skel.read(this, reader);
    } 
    catch (JAXBException e) {
      throw e;
    } 
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parses the XML based on an InputStream
   */
  public Object unmarshal(InputStream is)
    throws JAXBException
  {
    try {
      XMLStreamReader reader = new XMLStreamReaderImpl(is);

      try {
        return unmarshal(reader);
      } 
      finally {
        reader.close();
      }
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <T> JAXBElement<T> unmarshal(XMLStreamReader reader,
                                      Class<T> declaredType)
      throws JAXBException
  {
    try {
      while (reader.getEventType() != XMLStreamReader.START_ELEMENT)
        reader.next();

      QName name = reader.getName();

      Property property = _context.createProperty(declaredType);

      T val = (T) property.read(this, reader, null);

      return new JAXBElement<T>(name, declaredType, val);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //
  // From AbstractUnmarshallerImpl
  //

  public UnmarshallerHandler getUnmarshallerHandler()
  {
    // The idea here is that we return a SAX ContentHandler which the
    // user writes to and we use those writes to construct an object.
    // The object is retrieved using UnmarshallerHandler.getResult().
    // This is a "reusable" operation, so we should return a new handler
    // (or at least a reset one) for each call of this function.

    return new UnmarshallerHandlerImpl();
  }

  protected UnmarshalException createUnmarshalException(SAXException e)
  {
    return new UnmarshalException(e);
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    A a = (A)_adapters.get(type);

    if (a == null)
      return (A)new BeanAdapter();

    return a;
  }

  public AttachmentUnmarshaller getAttachmentUnmarshaller()
  {
    return _attachmentUnmarshaller;
  }

  public ValidationEventHandler getEventHandler() throws JAXBException
  {
    return _validationEventHandler;
  }

  public Listener getListener()
  {
    return _listener;
  }

  public Object getProperty(String name) throws PropertyException
  {
    return _properties.get(name);
  }

  public Schema getSchema()
  {
    return _schema;
  }

  protected XMLReader getXMLReader() throws JAXBException
  {
    return _xmlreader;
  }

  public boolean isValidating() throws JAXBException
  {
    return validating;
  }

  public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter)
  {
    _adapters.put(type, adapter);
  }

  public void setAdapter(XmlAdapter adapter)
  {
    setAdapter((Class)adapter.getClass(), adapter);
  }

  public void setAttachmentUnmarshaller(AttachmentUnmarshaller au)
  {
    _attachmentUnmarshaller = au;
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    _validationEventHandler = handler;
  }

  public void setListener(Listener listener)
  {
    _listener = listener;
  }

  public void setProperty(String name, Object value) throws PropertyException
  {
    _properties.put(name, value);
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  public void setValidating(boolean validating) throws JAXBException
  {
    this.validating = validating;
  }

  public Object unmarshal(File f) throws JAXBException
  {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(f);
      XMLInputFactory factory = _context.getXMLInputFactory();
      return unmarshal(factory.createXMLStreamReader(f.getAbsolutePath(), fis));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
    finally {
      try {
        if (fis != null)
          fis.close();
      }
      catch (IOException e) {
        throw new JAXBException(e);
      }
    }
  }

  public Object unmarshal(InputSource inputSource) 
    throws JAXBException
  {
    try {
      XMLEventReader reader = 
        new SAXSourceXMLEventReaderImpl(new SAXSource(inputSource));

      return unmarshal(reader);
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(Reader reader) throws JAXBException
  {
    try {
      XMLInputFactory factory = _context.getXMLInputFactory();
      
      return unmarshal(factory.createXMLStreamReader(reader));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(Source source) throws JAXBException
  {
    try {
      XMLInputFactory factory = _context.getXMLInputFactory();
      
      return unmarshal(factory.createXMLEventReader(source));
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType)
      throws JAXBException
  {
    throw new UnsupportedOperationException("subclasses must override this");
  }

  public <T> JAXBElement<T> unmarshal(Source node, Class<T> declaredType)
      throws JAXBException
  {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      return unmarshal(factory.createXMLStreamReader(node), declaredType);
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(URL url) throws JAXBException
  {
    try {
      InputStream is = url.openStream();

      try {
        return unmarshal(is);
      } 
      finally {
        is.close();
      }
    } catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  private class UnmarshallerHandlerImpl implements UnmarshallerHandler {
    private ContentHandler _handler;
    private SAXSourceXMLEventReaderImpl _reader;
    private boolean _done = false;
    private Object _result = null;

    public UnmarshallerHandlerImpl()
    {
      _reader = new SAXSourceXMLEventReaderImpl();
      _handler = _reader.getContentHandler();
    }

    public Object getResult()
      throws JAXBException
    {
      if (! _done)
        throw new IllegalStateException();

      if (_result == null)
        _result = unmarshal(_reader);

      return _result;
    }

    public void characters(char[] ch, int start, int length)
      throws SAXException
    {
      _handler.characters(ch, start, length);
    }

    public void endDocument()
      throws SAXException
    {
      _handler.endDocument();
      _done = true;
    }

    public void endElement(String uri, String localName, String qName)
      throws SAXException
    {
      _handler.endElement(uri, localName, qName);
    }

    public void endPrefixMapping(String prefix)
      throws SAXException
    {
      _handler.endPrefixMapping(prefix);
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException
    {
      _handler.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data)
      throws SAXException
    {
      _handler.processingInstruction(target, data);
    }

    public void setDocumentLocator(Locator locator)
    {
      _handler.setDocumentLocator(locator);
    }

    public void skippedEntity(String name)
      throws SAXException
    {
      _handler.skippedEntity(name);
    }

    public void startDocument()
      throws SAXException
    {
      _handler.startDocument();
    }

    public void startElement(String uri, String localName, String qName, 
                             Attributes atts)
      throws SAXException
    {
      _handler.startElement(uri, localName, qName, atts);
    }

    public void startPrefixMapping(String prefix, String uri)
      throws SAXException
    {
      _handler.startPrefixMapping(prefix, uri);
    }
  }
}
