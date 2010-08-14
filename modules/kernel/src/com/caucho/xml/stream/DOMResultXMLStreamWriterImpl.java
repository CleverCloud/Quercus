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

import com.caucho.util.L10N;
import com.caucho.xml.QDocument;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DOMResultXMLStreamWriterImpl implements XMLStreamWriter {
  private static final Logger log
    = Logger.getLogger(DOMResultXMLStreamWriterImpl.class.getName());
  private static final L10N L
    = new L10N(DOMResultXMLStreamWriterImpl.class);

  private DOMResult _result;
  private Document _document;
  private Node _current;
  private boolean _currentIsEmpty = false;
  private boolean _repair = false;

  private NamespaceWriterContext _tracker;

  public DOMResultXMLStreamWriterImpl(DOMResult result)
    throws XMLStreamException
  {
    this(result, false);
  }

  public DOMResultXMLStreamWriterImpl(DOMResult result, boolean repair)
    throws XMLStreamException
  {
    _result = result;
    _repair = repair;
    _tracker = new NamespaceWriterContext(repair);

    _current = result.getNode();

    if (_current == null) {
      _current = _document = new QDocument();
      result.setNode(_document);
    }
    else {
      if (_current.getNodeType() == Node.DOCUMENT_NODE)
        _document = (Document) _current;
      else 
        _document = _current.getOwnerDocument();
    }
  }

  public void close() 
    throws XMLStreamException
  {
    writeEndDocument();
  }

  public void flush() 
    throws XMLStreamException
  {
  }

  public NamespaceContext getNamespaceContext()
  {
    return _tracker;
  }

  public String getPrefix(String uri)
    throws XMLStreamException
  {
    return _tracker.getPrefix(uri);
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new PropertyNotSupportedException(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _tracker.declare(DEFAULT_NS_PREFIX, uri, _repair);
  }

  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException
  {
    String message = "please do not set the NamespaceContext";
    throw new UnsupportedOperationException(message);
  }

  public void setPrefix(String prefix, String uri)
    throws XMLStreamException
  {
    _tracker.declare(prefix, uri);
  }

  public void writeAttribute(String localName, String value)
    throws XMLStreamException
  {
    try {
      ((Element) _current).setAttribute(localName, value);
    }
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException
  {
    if (_repair) {
      String prefix = _tracker.declare(namespaceURI);

      if (prefix == null)
        ((Element) _current).setAttributeNS(namespaceURI, localName, value);
      else {
        String qname = prefix + ':' + localName;
        ((Element) _current).setAttributeNS(namespaceURI, qname, value);
      }
    }
    else {
      String prefix = _tracker.getPrefix(namespaceURI);

      if (prefix == null)
        throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

      String qname = prefix + ':' + localName;
      
      ((Element) _current).setAttributeNS(namespaceURI, qname, value);
    }
  }

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException
  {
    try {
      if (_repair && _tracker.getPrefix(namespaceURI) == null)
        _tracker.declare(prefix, namespaceURI, true);
      else
        _tracker.declare(prefix, namespaceURI);

      String qname = prefix + ':' + localName;
      ((Element) _current).setAttributeNS(namespaceURI, qname, value);
    }
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createCDATASection(data));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    writeCharacters(new String(text, start, len));
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createTextNode(text));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createComment(data));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException
  {
    _tracker.declare("", namespaceURI, true);
  }

  public void writeDTD(String dtd)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    try {
      Node parent = _current;
      _current = _document.createElement(localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = true;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    try {
      String qname = localName;

      if (_repair) {
        String prefix = _tracker.declare(namespaceURI);

        if (prefix != null)
          qname = prefix + ':' + localName;
      }
      else {
        String prefix = _tracker.getPrefix(namespaceURI);

        if (prefix == null)
          throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

        qname = prefix + ':' + localName;
      }

      Node parent = _current;
      _current = _document.createElementNS(namespaceURI, qname);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = true;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    try {
      if (_repair && _tracker.getPrefix(namespaceURI) == null) 
        _tracker.declare(prefix, namespaceURI, true);

      Node parent = _current;
      String qname = prefix + ':' + localName;
      _current = _document.createElementNS(namespaceURI, qname);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = true;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEndDocument()
    throws XMLStreamException
  {
  }

  public void writeEndElement()
    throws XMLStreamException
  {
    try {
      popContext();

      if (_currentIsEmpty)
        popContext();

      _currentIsEmpty = false;
    } 
    catch (ClassCastException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createEntityReference(name));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    if (prefix == null || "".equals(prefix) || "xmlns".equals(prefix))
      writeDefaultNamespace(namespaceURI);

    else {
      _tracker.declare(prefix, namespaceURI, true);

      if (! (_current instanceof Element))
        throw new XMLStreamException(L.l("Cannot write namespace without an element"));

      String qname = XMLNS_ATTRIBUTE + ':' + prefix;

      ((Element) _current).setAttributeNS(XML_NS_URI, qname, namespaceURI);
    }
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    writeProcessingInstruction(target, "");
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      _current.appendChild(_document.createProcessingInstruction(target, data));
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartDocument()
    throws XMLStreamException
  {
    writeStartDocument("1.0");
  }

  public void writeStartDocument(String version)
    throws XMLStreamException
  {
    writeStartDocument(version, "utf-8");
  }

  public void writeStartDocument(String version, String encoding)
    throws XMLStreamException
  {
    try {
      _document.setXmlVersion(version);
      // XXX encoding
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      Node parent = _current;
      _current = _document.createElement(localName);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = false;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      String qname = localName;

      if (_repair) {
        String prefix = _tracker.declare(namespaceURI);

        if (prefix != null)
          qname = prefix + ':' + localName;
      }
      else {
        String prefix = _tracker.getPrefix(namespaceURI);

        if (prefix == null)
          throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

        qname = prefix + ':' + localName;
      }

      Node parent = _current;
      _current = _document.createElementNS(namespaceURI, qname);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = false;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    try {
      if (_repair && _tracker.getPrefix(namespaceURI) == null)
        _tracker.declare(prefix, namespaceURI, true);

      Node parent = _current;
      String qname = prefix + ':' + localName;
      _current = _document.createElementNS(namespaceURI, qname);
      parent.appendChild(_current);

      if (! (parent instanceof Document))
        pushContext();

      _currentIsEmpty = false;
    }
    catch (DOMException e) {
      throw new XMLStreamException(e);
    }
  }

  //////////////////////////////////////////////////////////////////////////

  private boolean _flushed = true;

  private void pushContext()
    throws DOMException
  {
    _tracker.push();
    _flushed = false;
  }

  private void popContext()
    throws DOMException, XMLStreamException
  {
    _tracker.pop();
    _current = _current.getParentNode();
  }
}
