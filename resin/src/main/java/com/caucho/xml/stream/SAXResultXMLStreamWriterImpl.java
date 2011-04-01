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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SAXResultXMLStreamWriterImpl implements XMLStreamWriter {
  private static final Logger log
    = Logger.getLogger(SAXResultXMLStreamWriterImpl.class.getName());
  private static final L10N L
    = new L10N(SAXResultXMLStreamWriterImpl.class);

  private LexicalHandler _lexicalHandler;
  private ContentHandler _contentHandler;

  // current element data
  private String _uri;
  private String _localName;
  private String _qName;
  private AttributesImpl _attributes = new AttributesImpl();
  private boolean _unwritten = false;

  private boolean _currentIsEmpty = false;
  private boolean _ended = false;

  private SimpleNamespaceContext _context = new SimpleNamespaceContext(null);

  public SAXResultXMLStreamWriterImpl(SAXResult result)
    throws XMLStreamException
  {
    _lexicalHandler = result.getLexicalHandler();
    _contentHandler = result.getHandler();
  }

  public void close() 
    throws XMLStreamException
  {
    try {
      if (! _ended)
        _contentHandler.endDocument();
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void flush() 
    throws XMLStreamException
  {
  }

  public NamespaceContext getNamespaceContext()
  {
    return _context;
  }

  public String getPrefix(String uri)
    throws XMLStreamException
  {
    return _context.getPrefix(uri);
  }

  public Object getProperty(String name)
    throws IllegalArgumentException
  {
    throw new PropertyNotSupportedException(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _context.declare("", uri);
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
    try {
      _context.declare(prefix, uri);
      _contentHandler.startPrefixMapping(prefix, uri);
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeAttribute(String localName, String value)
    throws XMLStreamException
  {
    if (! _unwritten)
      throw new IllegalStateException();

    // XXX other types?
    _attributes.addAttribute("", localName, localName, "CDATA", value);
  }

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException
  {
    if (! _unwritten)
      throw new IllegalStateException();

    _attributes.addAttribute(namespaceURI, localName, localName, 
                             "CDATA", value);
  }

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException
  {
    if (! _unwritten)
      throw new IllegalStateException();

    _attributes.addAttribute(namespaceURI, localName, prefix + ':' + localName, 
                             "CDATA", value);
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    // XXX
    throw new UnsupportedOperationException();
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    try {
      _contentHandler.characters(text, start, len);
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    char[] array = text.toCharArray();
    writeCharacters(array, 0, array.length);
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    try {
      if (_lexicalHandler != null) {
        char[] array = data.toCharArray();
        _lexicalHandler.comment(array, 0, array.length);
      }
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException
  {
    if (! _unwritten)
      throw new IllegalStateException();

    try {
      _context.declare("", namespaceURI);
      _contentHandler.startPrefixMapping("", namespaceURI);
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeDTD(String dtd)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    // XXX: lexicalHandler
    throw new UnsupportedOperationException();
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    handleUnwrittenStart();

    pushContext();

    _uri = null;
    _localName = localName;
    _qName = localName;
    _attributes = new AttributesImpl();

    _currentIsEmpty = true;
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    handleUnwrittenStart();

    pushContext();

    _uri = namespaceURI;
    _localName = localName;
    _qName = localName;
    _attributes = new AttributesImpl();

    _currentIsEmpty = true;
  }

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty)
      popContext();

    handleUnwrittenStart();

    pushContext();

    _uri = namespaceURI;
    _localName = localName;
    _qName = prefix + ':' + localName;
    _attributes = new AttributesImpl();

    _currentIsEmpty = true;
  }

  public void writeEndDocument()
    throws XMLStreamException
  {
    handleUnwrittenStart();

    try {
      _contentHandler.endDocument();
      _ended = true;
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEndElement()
    throws XMLStreamException
  {
    popContext();
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    // XXX
    throw new UnsupportedOperationException();
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    if (! _unwritten)
      throw new IllegalStateException();

    _context.declare(prefix, namespaceURI);
    _attributes.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, 
                             prefix, 
                             XMLConstants.XMLNS_ATTRIBUTE + ':' + prefix,
                             "CDATA", 
                             namespaceURI);
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    writeProcessingInstruction(target, null);
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    handleUnwrittenStart();

    try {
      _contentHandler.processingInstruction(target, data);
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartDocument()
    throws XMLStreamException
  {
    try {
      _contentHandler.startDocument();
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartDocument(String version)
    throws XMLStreamException
  {
    try {
      _contentHandler.startDocument();
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartDocument(String version, String encoding)
    throws XMLStreamException
  {
    try {
      _contentHandler.startDocument();
    }
    catch (SAXException e) {
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

    pushContext();

    _uri = null;
    _localName = localName;
    _qName = localName;
    _attributes = new AttributesImpl();
    _unwritten = true;
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    pushContext();

    _uri = namespaceURI;
    _localName = localName;
    _qName = localName;
    _attributes = new AttributesImpl();
    _unwritten = true;
  }

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    if (_currentIsEmpty) {
      popContext();
      _currentIsEmpty = false;
    }

    pushContext();

    _uri = namespaceURI;
    _localName = localName;
    _qName = prefix + ':' + localName;
    _attributes = new AttributesImpl();
    _unwritten = true;
  }

  //////////////////////////////////////////////////////////////////////////
  
  private void handleUnwrittenStart()
    throws XMLStreamException
  {
    try {
      if (_unwritten)
        _contentHandler.startElement(_uri, _localName, _qName, _attributes);

      _unwritten = false;
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  private void pushContext()
    throws XMLStreamException
  {
    _context = new SimpleNamespaceContext(_context);
  }

  private void popContext()
    throws XMLStreamException
  {
    try {
      if (_currentIsEmpty)
        _contentHandler.startElement(_uri, _localName, _qName, _attributes);

      for (String prefix : _context.getPrefixMap().keySet())
        _contentHandler.endPrefixMapping(prefix);

      _contentHandler.endElement(_uri, _localName, _qName);

      _context = _context.getParent();
    }
    catch (SAXException e) {
      throw new XMLStreamException(e);
    }
  }

  // XXX switch to NamespaceWriterContext
  private static class SimpleNamespaceContext implements NamespaceContext {
    private HashMap<String,String> _uris = new HashMap<String,String>();
    private HashMap<String,List<String>> _prefixes
      = new HashMap<String,List<String>>();
    private SimpleNamespaceContext _parent;
    private int _prefixCounter = 0;

    public SimpleNamespaceContext(SimpleNamespaceContext parent)
    {
      _parent = parent;
    }

    public String getNamespaceURI(String prefix)
    {
      return _uris.get(prefix);
    }

    public String getPrefix(String namespaceURI)
    {
      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null || prefixes.size() == 0)
        return null;

      return prefixes.get(0);
    }

    public Iterator getPrefixes(String namespaceURI)
    {
      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null) {
        prefixes = new ArrayList<String>();
        _prefixes.put(namespaceURI, prefixes);
      }

      return prefixes.iterator();
    }

    public HashMap<String,String> getPrefixMap()
    {
      return _uris;
    }

    public String declare(String namespaceURI)
    {
      String prefix = "ns" + _prefixCounter;
      declare(prefix, namespaceURI);
      _prefixCounter++;

      return prefix;
    }

    public void declare(String prefix, String namespaceURI)
    {
      _uris.put(prefix, namespaceURI);

      List<String> prefixes = _prefixes.get(namespaceURI);

      if (prefixes == null) {
        prefixes = new ArrayList<String>();
        _prefixes.put(namespaceURI, prefixes);
      }

      prefixes.add(prefix);
    }

    public SimpleNamespaceContext getParent()
    {
      return _parent;
    }
  }
}
