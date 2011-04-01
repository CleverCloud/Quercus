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

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.Vfs;
import com.caucho.util.L10N;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Logger;

public class XMLStreamWriterImpl implements XMLStreamWriter {
  private static final L10N L = new L10N(XMLStreamWriterImpl.class);
  private static final Logger log
    = Logger.getLogger(XMLStreamReaderImpl.class.getName());

  private WriteStream _out;
  private NamespaceWriterContext _tracker;

  private QName _pendingTagName = null;
  private boolean _shortTag = false;
  private boolean _repair = false;
  private ArrayList<QName> _pendingAttributeNames = new ArrayList<QName>();
  private ArrayList<String> _pendingAttributeValues = new ArrayList<String>();

  private int _indent = -1;
  private int _currentIndent;
  private boolean _flushed = true;

  public XMLStreamWriterImpl(WriteStream ws)
  {
    this(ws, false);
  }

  public XMLStreamWriterImpl(WriteStream ws, boolean repair)
  {
    _out = ws;
    _repair = repair;
    _tracker = new NamespaceWriterContext(repair);
  }

  public XMLStreamWriterImpl(Writer w, boolean repair)
  {
    this(Vfs.openWrite(w), repair);
  }

  public XMLStreamWriterImpl(OutputStream os, boolean repair)
  {
    this(Vfs.openWrite(os), repair);
  }

  public void setIndent(int indent)
  {
    _indent = indent;
  }

  public void setRepair(boolean repair)
  {
    _repair = repair;
    _tracker.setRepair(repair);
  }

  public void close() throws XMLStreamException
  {
    flushPending();
    flush();
    // DO NOT close _out!  
    // This will cause XFire/CXF and possibly others to blow up.
  }

  public void flush() throws XMLStreamException
  {
    try {
      _out.flush();
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
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
    if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name))
      return Boolean.valueOf(_repair);

    throw new PropertyNotSupportedException(name);
  }

  public void setDefaultNamespace(String uri)
    throws XMLStreamException
  {
    _tracker.declare(XMLConstants.DEFAULT_NS_PREFIX, uri, _repair);
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
    _pendingAttributeNames.add(new QName(localName));
    _pendingAttributeValues.add(value);
  }

  public void writeAttribute(String namespaceURI, String localName,
                             String value)
    throws XMLStreamException
  {
    if (_repair) {
      String prefix = _tracker.declare(namespaceURI);

      if (prefix == null)
        _pendingAttributeNames.add(new QName(namespaceURI, localName));
      else
        _pendingAttributeNames.add(new QName(namespaceURI, localName, prefix));
    }
    else {
      String prefix = _tracker.getPrefix(namespaceURI);

      if (prefix == null)
        throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

      _pendingAttributeNames.add(new QName(namespaceURI, localName, prefix));
    }

    _pendingAttributeValues.add(value);
  }

  public void writeAttribute(String prefix, String namespaceURI,
                             String localName, String value)
    throws XMLStreamException
  {
    if (prefix == null || "".equals(prefix))
      throw new XMLStreamException(L.l("Attribute namespace prefixes cannot be null or empty"));

    if (_repair && _tracker.getPrefix(namespaceURI) == null)
      _tracker.declare(prefix, namespaceURI, true);
    else
      _tracker.declare(prefix, namespaceURI);

    _pendingAttributeNames.add(new QName(namespaceURI, localName, prefix));
    _pendingAttributeValues.add(value);
  }

  public void writeCData(String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _out.print("<![CDATA[");
      _out.print(data);
      _out.print("]]>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException
  {
    flushPending();
    try {
      Escapifier.escape(text, start, len, _out);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeCharacters(String text)
    throws XMLStreamException
  {
    flushPending();
    try {
      Escapifier.escape(text, _out);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeComment(String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _out.print("<!--");
      _out.print(data);
      _out.print("-->");
    }
    catch (IOException e) {
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
    flushPending();
    
    try {
      _out.print(dtd);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeElement(String localName, String contents)
    throws XMLStreamException
  {
    writeStartElement(localName);
    if (contents != null)
      writeCharacters(contents);
    writeEndElement();
  }

  public void writeEmptyElement(String localName)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(localName);
      pushContext(qname);
      _pendingTagName = qname;
      _shortTag = true;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    flushPending();

    try {
      QName qname = null;

      if (_repair) {
        // NOTE: We have to push before we declare because declare will
        // declare the namespace in the parent context if we don't
        flushContext();
        _tracker.push();

        String prefix = _tracker.declare(namespaceURI);

        if (prefix == null)
          qname = new QName(namespaceURI, localName);
        else
          qname = new QName(namespaceURI, localName, prefix);

        _tracker.setElementName(qname);
        _flushed = false;
      }
      else {
        String prefix = _tracker.getPrefix(namespaceURI);

        if (prefix == null)
          throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

        qname = new QName(namespaceURI, localName, prefix);
        pushContext(qname);
      }

      _pendingTagName = qname;
      _shortTag = true;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeEmptyElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(namespaceURI, localName, prefix);

      if (_repair && _tracker.getPrefix(namespaceURI) == null) {
        // NOTE: We have to push before we declare because declare will
        // declare the namespace in the parent context if we don't
        flushContext();
        _tracker.push();

        _tracker.declare(prefix, namespaceURI, true);

        _tracker.setElementName(qname);
        _flushed = false;
      }
      else
        pushContext(qname);

      _pendingTagName = qname;
      _shortTag = true;
    }
    catch (IOException e) {
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
    writeEndElement(null, null);
  }

  public void writeEndElement(String localName)
    throws XMLStreamException
  {
    writeEndElement(null, localName);
  }

  public void writeEndElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    flushPending();

    try {
      QName name = popContext();

      if ((localName != null && !localName.equals(name.getLocalPart()))
          || (namespaceURI != null && !namespaceURI.equals(name.getNamespaceURI())))
        throw new XMLStreamException(L.l("unbalanced close, expecting `{0}' not `{1}'",
                                         name, new QName(namespaceURI, localName)));

      _out.print("</");
      _out.print(printQName(name));
      _out.print(">");

      if (_indent >= 0) {
        _out.println();
        _currentIndent -= _indent;
      }
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private static String printQName(QName name) {

    if (name.getPrefix() == null || name.getPrefix().equals(""))
      return name.getLocalPart();

    return name.getPrefix() + ":" + name.getLocalPart();
  }

  public void writeEntityRef(String name)
    throws XMLStreamException
  {
    flushPending();
    try {
      _out.print("&");
      _out.print(name);
      _out.print(";");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException
  {
    if (_pendingTagName == null)
      throw new XMLStreamException("Namespace written before element");

    if (prefix == null || "".equals(prefix) || "xmlns".equals(prefix))
      writeDefaultNamespace(namespaceURI);
    else
      _tracker.declare(prefix, namespaceURI, true);
  }

  public void writeProcessingInstruction(String target)
    throws XMLStreamException
  {
    flushPending();
    try {
      _out.print("<?");
      _out.print(target);
      _out.print("?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeProcessingInstruction(String target, String data)
    throws XMLStreamException
  {
    flushPending();
    try {
      _out.print("<?");
      _out.print(target);
      _out.print(" ");
      _out.print(data);
      _out.print("?>");
    }
    catch (IOException e) {
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
    writeStartDocument("utf-8", version);
  }

  public void writeStartDocument(String encoding, String version)
    throws XMLStreamException
  {
    try {
      _out.print("<?xml version=\""+version+"\" encoding=\""+encoding+"\"?>");
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String localName)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(localName);
      pushContext(qname);
      _pendingTagName = qname;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = null;

      if (_repair) {
        // NOTE: We have to push before we declare because declare will
        // declare the namespace in the parent context if we don't
        flushContext();
        _tracker.push();

        String prefix = _tracker.declare(namespaceURI);

        if (prefix == null)
          qname = new QName(namespaceURI, localName);
        else
          qname = new QName(namespaceURI, localName, prefix);

        _tracker.setElementName(qname);
        _flushed = false;
      }
      else {
        String prefix = _tracker.getPrefix(namespaceURI);

        if (prefix == null)
          throw new XMLStreamException(L.l("No prefix defined for namespace {0}", namespaceURI));

        qname = new QName(namespaceURI, localName, prefix);
        pushContext(qname);
      }

      _pendingTagName = qname;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public void writeStartElement(String prefix, String localName,
                                String namespaceURI)
    throws XMLStreamException
  {
    flushPending();
    try {
      QName qname = new QName(namespaceURI, localName, prefix);

      if (_repair && _tracker.getPrefix(namespaceURI) == null) {
        // NOTE: We have to push before we declare because declare will
        // declare the namespace in the parent context if we don't
        flushContext();
        _tracker.push();

        _tracker.declare(prefix, namespaceURI, true);

        _tracker.setElementName(qname);
        _flushed = false;
      }
      else
        pushContext(qname);

      _pendingTagName = qname;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////

  private void pushContext(QName elementName)
    throws IOException
  {
    flushContext();
    _tracker.push();
    _tracker.setElementName(elementName);
    _flushed = false;
  }

  private QName popContext()
    throws IOException, XMLStreamException
  {
    flushContext();
    QName name = _tracker.getElementName();
    _tracker.pop();
    return name;
  }

  private void flushContext()
    throws IOException
  {
    if (_flushed)
      return;
    
    _tracker.emitDeclarations(_out);
    _flushed = true;
  }

  private void flushPending()
    throws XMLStreamException
  {
    try {
      if (_pendingTagName == null)
        return;

      _out.print("<");
      _out.print(printQName(_pendingTagName));
      
      for(int i = 0; i < _pendingAttributeNames.size(); i++) {
        _out.print(" ");
        _out.print(printQName(_pendingAttributeNames.get(i)));
        _out.print("=\"");
        Escapifier.escape(_pendingAttributeValues.get(i), _out);
        _out.print('"');
      }
      flushContext();

      if (_shortTag) {
        _out.print("/>");
        popContext();
      } else {
        _out.print(">");

        if (_indent > -1)
          _currentIndent += _indent;
      }
      
      _pendingTagName = null;
      _pendingAttributeNames.clear();
      _pendingAttributeValues.clear();
      _shortTag = false;
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }
}
