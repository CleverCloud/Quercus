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

import static javax.xml.XMLConstants.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.w3c.dom.*;
import static org.w3c.dom.Node.*;

/**
 * XML pull-parser interface.
 */
public class DOMSourceXMLStreamReaderImpl implements XMLStreamReader {
  private static final Logger log
    = Logger.getLogger(DOMSourceXMLStreamReaderImpl.class.getName());
  private static final L10N L = new L10N(DOMSourceXMLStreamReaderImpl.class);

  private NamespaceReaderContext _namespaceTracker;
  private Node _current;
  private boolean _ending = false;
  private String _systemId;
  private String _publicId;

  private String _version;
  private String _encoding;
  private boolean _standalone = false;

  private boolean _first = true;

  public DOMSourceXMLStreamReaderImpl(DOMSource source)
    throws XMLStreamException
  {
    this(source.getNode());
  }

  public DOMSourceXMLStreamReaderImpl(Node node)
    throws XMLStreamException
  {
    _current = node;

    if (node.getNodeType() == Node.ELEMENT_NODE)
      _first = false;

    init();
  }

  private void init()
    throws XMLStreamException
  {
    _namespaceTracker = new NamespaceReaderContext();
    declareNamespaces();

    if (_current instanceof Document) {
      Document document = (Document) _current;

      _encoding = document.getXmlEncoding();
      _version = document.getXmlVersion();
      _standalone = document.getXmlStandalone();
    }
  }

  private void declareNamespaces()
  {
    if (isStartElement()) {
      _namespaceTracker.push();

      Element element = (Element) _current;

      NamedNodeMap map = element.getAttributes();

      if (map == null)
        return;

      for (int i = 0; i < map.getLength(); i++) {
        Attr attr = (Attr) map.item(i);

        if (attr.getName().startsWith(XMLNS_ATTRIBUTE)) {
          int colon = attr.getName().indexOf(':');

          if (colon < 0)
            _namespaceTracker.declare(DEFAULT_NS_PREFIX, attr.getNodeValue());
          else {
            String prefix = attr.getName().substring(0, colon);
            _namespaceTracker.declare(prefix, attr.getNodeValue());
          }
        }
      }
    }
  }

  public int getAttributeCount()
  {
    if (isStartElement()) {
      Element element = (Element) _current;

      NamedNodeMap map = element.getAttributes();

      if (map == null)
        return 0;

      int attributeCount = 0;

      // count the non-namespace prefix attributes
      for (int i = 0; i < map.getLength(); i++) {
        Attr attr = (Attr) map.item(i);

        if (! attr.getName().startsWith(XMLNS_ATTRIBUTE))
          attributeCount++;
      }

      return attributeCount;
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  private Attr getAttribute(int index)
  {
    if (isStartElement()) {
      Element element = (Element) _current;

      NamedNodeMap map = element.getAttributes();

      if (map == null)
        throw new NoSuchElementException();

      int attributeCount = 0;

      for (int i = 0; i < map.getLength(); i++) {
        Attr attr = (Attr) map.item(i);

        if (! attr.getName().startsWith(XMLNS_ATTRIBUTE)) {
          if (attributeCount == index)
            return attr;

          attributeCount++;
        }
      }

      throw new NoSuchElementException();
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public String getAttributeLocalName(int index)
  {
    Attr attr = getAttribute(index);

    return attr.getLocalName();
  }

  public QName getAttributeName(int index)
  {
    Attr attr = getAttribute(index);

    int colon = attr.getName().indexOf(':');

    if (colon < 0) {
      if (attr.getNamespaceURI() == null || "".equals(attr.getNamespaceURI()))
        return new QName(attr.getNamespaceURI(), attr.getLocalName());
      else
        return new QName(attr.getLocalName());
    }

    String prefix = attr.getName().substring(0, colon);

    return new QName(attr.getNamespaceURI(), attr.getLocalName(), prefix);
  }

  public String getAttributeNamespace(int index)
  {
    Attr attr = getAttribute(index);

    return attr.getNamespaceURI();
  }

  public String getAttributePrefix(int index)
  {
    Attr attr = getAttribute(index);

    int colon = attr.getName().indexOf(':');

    if (colon < 0)
      return DEFAULT_NS_PREFIX;

    String prefix = attr.getName().substring(0, colon);

    return prefix;
  }

  public String getAttributeType(int index)
  {
    return "CDATA";
  }

  public String getAttributeValue(int index)
  {
    Attr attr = getAttribute(index);

    return attr.getNodeValue();
  }

  public boolean isAttributeSpecified(int index)
  {
    return index < getAttributeCount();
  }

  public String getAttributeValue(String namespaceURI, String localName)
  {
    if (isStartElement()) {
      Element element = (Element) _current;

      NamedNodeMap map = element.getAttributes();

      Attr attr = (Attr) map.getNamedItemNS(namespaceURI, localName);

      if (attr == null)
        return null;

      return attr.getNodeValue();
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public String getCharacterEncodingScheme()
  {
    return _encoding;
  }

  public String getElementText() throws XMLStreamException
  {
    return _current.getTextContent();
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public int getEventType()
  {
    if (_current == null)
      return END_DOCUMENT;

    switch (_current.getNodeType()) {
      case ATTRIBUTE_NODE:
        return ATTRIBUTE;
      case CDATA_SECTION_NODE:
        return CDATA;
      case COMMENT_NODE:
        return COMMENT;
      case DOCUMENT_FRAGMENT_NODE:
        throw new IllegalStateException();
      case DOCUMENT_NODE:
        return _ending ? END_DOCUMENT : START_DOCUMENT;
      case DOCUMENT_TYPE_NODE:
        return DTD;
      case ELEMENT_NODE:
        return _ending ? END_ELEMENT : START_ELEMENT;
      case ENTITY_NODE:
        return ENTITY_DECLARATION;
      case ENTITY_REFERENCE_NODE:
        return ENTITY_REFERENCE;
      case NOTATION_NODE:
        return NOTATION_DECLARATION;
      case PROCESSING_INSTRUCTION_NODE:
        return PROCESSING_INSTRUCTION;
      case TEXT_NODE:
        return CHARACTERS;
    }

    return -1;
  }

  public Location getLocation()
  {
    return new UnknownLocation();
  }

  public String getLocalName()
  {
    if (_current instanceof Element) {
      Element element = (Element) _current;

      return element.getLocalName();
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public String getNamespaceURI()
  {
    if (isStartElement()) {
      Element element = (Element) _current;

      return element.getNamespaceURI();
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public QName getName()
  {
    if (_current.getNodeType() == ELEMENT_NODE) {
      Element element = (Element) _current;

      String name = element.getNodeName();

      int colon = name.indexOf(':');

      if (colon < 0) {
        if (element.getNamespaceURI() == null || 
            "".equals(element.getNamespaceURI()))
          return new QName(element.getLocalName());
        else 
          return new QName(element.getNamespaceURI(), element.getLocalName());
      }

      String prefix = name.substring(0, colon);

      return new QName(element.getNamespaceURI(), 
                       element.getLocalName(), 
                       prefix);
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public NamespaceContext getNamespaceContext()
  {
    return _namespaceTracker;
  }

  public int getNamespaceCount()
  {
    return _namespaceTracker.getNumDecls();
  }

  public String getNamespacePrefix(int index)
  {
    String prefix = _namespaceTracker.getPrefix(index);

    // The API specifies that this function return a different value for
    // the default namespace, null, than any other function, which all return
    // the constant defined in XMLConstants.
    if (DEFAULT_NS_PREFIX.equals(prefix))
      return null;
    else
      return prefix;
  }

  public String getNamespaceURI(int index)
  {
    return _namespaceTracker.getUri(index);
  }

  public String getNamespaceURI(String prefix)
  {
    return _namespaceTracker.getUri(prefix);
  }

  public String getPIData()
  {
    if (_current.getNodeType() != PROCESSING_INSTRUCTION_NODE)
      return null;

    ProcessingInstruction pi = (ProcessingInstruction) _current;

    return pi.getData();
  }

  public String getPITarget()
  {
    if (_current.getNodeType() != PROCESSING_INSTRUCTION_NODE)
      return null;

    ProcessingInstruction pi = (ProcessingInstruction) _current;

    return pi.getTarget();
  }

  public String getPrefix()
  {
    if (isStartElement()) {
      Element element = (Element) _current;

      String name = element.getNodeName();

      int colon = name.indexOf(':');

      if (colon < 0)
        return DEFAULT_NS_PREFIX;

      return name.substring(0, colon);
    }
    else {
      throw new IllegalStateException(L.l("Current event not a start or end element"));
    }
  }

  public Object getProperty(String name) throws IllegalArgumentException
  {
    if ("javax.xml.stream.notations".equals(name)) {
      throw new UnsupportedOperationException(getClass().getName());
    }
    else if ("javax.xml.stream.entities".equals(name)) {
      throw new UnsupportedOperationException(getClass().getName());
    }
    else {
      throw
        new IllegalArgumentException("property \""+name+"+\" not supported");
    }
  }

  /**
   * Returns the current text string.
   */
  public String getText()
  {
    if (_current instanceof CharacterData) {
      CharacterData data = (CharacterData) _current;

      return data.getData();
    }

    throw new IllegalStateException("Not a text node");
  }

  /**
   * Returns a character buffer for the current text.
   */
  public char[] getTextCharacters()
  {
    return getText().toCharArray();
  }

  /**
   * Reads the current text into a buffer.
   */
  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    char[] source = getTextCharacters();

    int ret = Math.min(source.length - sourceStart, length);

    System.arraycopy(source, sourceStart, target, targetStart, ret);

    return ret;
  }

  /**
   * Returns the length of the current text.
   */
  public int getTextLength()
  {
    return getText().length();
  }

  /**
   * Returns the offset of the current text.
   */
  public int getTextStart()
  {
    return 0;
  }

  public String getVersion()
  {
    return _version;
  }

  public boolean hasName()
  {
    return _current.getNodeType() == ELEMENT_NODE;
  }

  public boolean hasText()
  {
    switch (_current.getNodeType()) {
      case TEXT_NODE:
      case DOCUMENT_TYPE_NODE:
      case ENTITY_REFERENCE_NODE:
      case COMMENT_NODE:
        return true;
      default:
        return false;
    }
  }

  public boolean isCharacters()
  {
    return _current.getNodeType() == TEXT_NODE;
  }

  public boolean isEndElement()
  {
    return _ending;
  }

  public boolean isStandalone()
  {
    return _standalone;
  }

  public boolean isStartElement()
  {
    return _current != null &&
           _current.getNodeType() == ELEMENT_NODE && 
           ! _ending;
  }

  public boolean isWhiteSpace()
  {
    if (! isCharacters())
      return false;

    String text = getText();

    for (int i = 0; i < text.length(); i++) {
      if (! Character.isWhitespace(text.charAt(i)))
        return false;
    }

    return true;
  }

  /**
   * Skips until the next START_ELEMENT or END_ELEMENT
   */
  public int nextTag() throws XMLStreamException
  {
    while (true) {
      int tag = next();

      if (tag < 0
          || tag == START_ELEMENT
          || tag == END_ELEMENT) {
        return tag;
      }
    }
  }

  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    if (type != getEventType())
      throw new XMLStreamException("expected " + constantToString(type) + ", "+
                                   "found " + constantToString(getEventType()) +
                                   " at " + getLocation());

    if (localName != null && ! localName.equals(getLocalName()))
      throw new XMLStreamException("expected <"+localName+">, found " +
                                   "<"+getLocalName()+"> at " + getLocation());

    if (namespaceURI != null && ! namespaceURI.equals(getNamespaceURI()))
      throw new XMLStreamException("expected xmlns="+namespaceURI+
                                   ", found xmlns="+getNamespaceURI() +
                                   " at " + getLocation());
  }

  public boolean standaloneSet()
  {
    return isStandalone();
  }

  public boolean hasNext() throws XMLStreamException
  {
    return (_current != null && getEventType() != END_DOCUMENT);
  }

  public int next() throws XMLStreamException
  {
    if (_current == null)
      throw new NoSuchElementException();

    if (_first) {
      _first = false;
      return getEventType();
    }

    if (_ending) {
      if (_current.getNodeType() == ELEMENT_NODE)
        _namespaceTracker.pop();

      if (_current.getNextSibling() != null) {
        _current = _current.getNextSibling();
        _ending = false;
      }
      else 
        _current = _current.getParentNode();
    }
    else if (_current.getFirstChild() != null) {
      _current = _current.getFirstChild();
    }
    else if (isStartElement()) {
      _ending = true;
    }
    else if (_current.getNextSibling() != null) {
      _current = _current.getNextSibling();
    }
    else {
      _current = _current.getParentNode();
      _ending = true;
    }

    declareNamespaces();

    return getEventType();
  }

  public void close() throws XMLStreamException
  {
  }

  private class UnknownLocation implements Location {
    public int getCharacterOffset()
    {
      return -1;
    }

    public int getColumnNumber()
    {
      return -1;
    }

    public int getLineNumber()
    {
      return -1;
    }

    public String getPublicId()
    {
      return null;
    }

    public String getSystemId()
    {
      return null;
    }
  }

  private static String constantToString(int constant) {
    switch(constant) {
      case ATTRIBUTE: return "ATTRIBUTE";
      case CDATA: return "CDATA";
      case CHARACTERS: return "CHARACTERS";
      case COMMENT: return "COMMENT";
      case DTD: return "DTD";
      case END_DOCUMENT: return "END_DOCUMENT";
      case END_ELEMENT: return "END_ELEMENT";
      case ENTITY_DECLARATION: return "ENTITY_DECLARATION";
      case ENTITY_REFERENCE: return "ENTITY_REFERENCE";
      case NAMESPACE: return "NAMESPACE";
      case NOTATION_DECLARATION: return "NOTATION_DECLARATION";
      case PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
      case SPACE: return "SPACE";
      case START_DOCUMENT: return "START_DOCUMENT";
      case START_ELEMENT: return "START_ELEMENT";
      default:
        throw new RuntimeException("constantToString("+constant+") unknown");
    }
  }
}
