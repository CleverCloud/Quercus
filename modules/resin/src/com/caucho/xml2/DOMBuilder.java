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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xml2;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XMLWriter to create a DOM document.
 */
public class DOMBuilder implements XMLWriter, ContentHandler, ErrorHandler {
  static final Logger log
    = Logger.getLogger(DOMBuilder.class.getName());
  static final L10N L = new L10N(DOMBuilder.class);
  static final String XMLNS = XmlParser.XMLNS;
  
  private QDocument _doc;
  private Node _top;
  private Node _node;

  private String _singleText;
  private CharBuffer _text = new CharBuffer();
  
  private boolean _escapeText;
  private boolean _strictXml;

  // If true, text and cdata sections should be combined.
  private boolean _isCoalescing = true;
  // If true, ignorable whitespace is skipped
  private boolean _skipWhitespace = false;

  private ArrayList<QName> _prefixNames = new ArrayList<QName>();
  private ArrayList<String> _prefixValues = new ArrayList<String>();

  private Locator _locator;
  private ExtendedLocator _extLocator;
  private String _systemId;

  public DOMBuilder()
  {
  }

  public void init(Node top)
  {
    if (top instanceof QDocument)
      _doc = (QDocument) top;
    else
      _doc = (QDocument) top.getOwnerDocument();
    _top = top;
    _node = top;
    
    _singleText = null;

    _prefixNames.clear();
    _prefixValues.clear();
  }

  public void setSystemId(String systemId)
  {
    _systemId = systemId;
    
    if (systemId != null && _top instanceof Document) {
      Document tdoc = (Document) _top;
      DocumentType dtd = tdoc.getDoctype();
      if (tdoc instanceof QDocument && dtd == null) {
        dtd = new QDocumentType(null);
        ((QDocument) tdoc).setDoctype(dtd);
      }
      
      if (dtd instanceof QDocumentType && dtd.getSystemId() == null)
        ((QDocumentType) dtd).setSystemId(systemId);
    }

    if (_doc != null)
      _doc.setSystemId(systemId);
  }

  public String getSystemId()
  {
    return _systemId;
  }

  public void setFilename(String filename)
  {
    if (filename != null && _top instanceof QDocument) {
      _doc.setRootFilename(filename);
    }
  }

  /**
   * Set true if we're only handling strict xml.
   */
  public void setStrictXML(boolean isStrictXml)
  {
    _strictXml = isStrictXml;
  }

  /**
   * Set true if text and cdata nodes should be combined.
   */
  public void setCoalescing(boolean isCoalescing)
  {
    _isCoalescing = isCoalescing;
  }

  /**
   * Set true if ignorable whitespace should be skipped.
   */
  public void setSkipWhitespace(boolean skipWhitespace)
  {
    _skipWhitespace = skipWhitespace;
  }

  public void setDocumentLocator(Locator loc)
  {
    if (_doc == null) {
      _doc = new QDocument();
      _node = _doc;
      _top = _doc;
    }

    _locator = loc;
    
    if (loc instanceof ExtendedLocator)
      _extLocator = (ExtendedLocator) loc;

    if (_extLocator != null && _doc.getSystemId() == null)
      _doc.setLocation(_extLocator.getSystemId(),
                       _extLocator.getFilename(),
                       _extLocator.getLineNumber(),
                       _extLocator.getColumnNumber());
  }
  
  public void startPrefixMapping(String prefix, String url)
  {
    if (_node == null || _node == _top)
      _doc.addNamespace(prefix, url);

    if (prefix.equals("")) {
      _prefixNames.add(new QName(null, "xmlns", XmlParser.XMLNS));
      _prefixValues.add(url);
    }
    else {
      _prefixNames.add(new QName("xmlns", prefix, XmlParser.XMLNS));
      _prefixValues.add(url);
    }
  }
  
  public void endPrefixMapping(String prefix)
  {
  }

  public Node getNode()
  {
    return _top;
  }

  public void startDocument()
  {
    if (_doc == null) {
      _doc = new QDocument();
      _node = _doc;
      _top = _doc;
    }
  }

  public void endDocument()
    throws SAXException
  {
    popText();
  }
  
  public void setLocation(String filename, int line, int column)
  {
  }

  public void startElement(String uri, String localName, String qName)
    throws IOException
  {
    popText();
    
    Element elt;

    if (uri != null && ! uri.equals(""))
      elt = _doc.createElementNS(uri, qName);
    else if (! qName.equals(""))
      elt = _doc.createElement(qName);
    else
      elt = _doc.createElement(localName);
    
    if (_node == _doc) {
      if (_doc.getDocumentElement() == null)
        ((QDocument) _doc).setDocumentElement(elt);
    }
    
    _node.appendChild(elt);
    _node = elt;

    if (_extLocator != null && elt instanceof QElement) {
      ((QElement) elt).setLocation(_extLocator.getSystemId(),
                                   _extLocator.getFilename(),
                                   _extLocator.getLineNumber(),
                                   _extLocator.getColumnNumber());
    }
  }

  public void startElement(QName name, QAttributes attributes)
    throws SAXException
  {
    popText();

    QElement elt = (QElement) _doc.createElementByName(name);
    _node.appendChild(elt);
    _node = elt;
    
    if (_node == _doc) {
      if (_doc.getDocumentElement() == null)
        ((QDocument) _doc).setDocumentElement(elt);
    }

    for (int i = 0; i < _prefixNames.size(); i++) {
      QName attrName = _prefixNames.get(i);
      String value = _prefixValues.get(i);

      elt.setAttribute(attrName, value);
    }

    _prefixNames.clear();
    _prefixValues.clear();

    int length = attributes.getLength();
    for (int i = 0; i < length; i++) {
      QName attrName = attributes.getName(i);
      String value = attributes.getValue(i);

      elt.setAttribute(attrName, value);
    }

    if (_extLocator != null) {
      elt.setLocation(_extLocator.getSystemId(),
                      _extLocator.getFilename(),
                      _extLocator.getLineNumber(),
                      _extLocator.getColumnNumber());
    }
    
    QDocumentType dtd = (QDocumentType) _doc.getDoctype();
    if (dtd != null)
      dtd.fillDefaults(elt);
  }

  public void startElement(String uri, String localName, String qName,
                           Attributes attributes)
    throws SAXException
  {
    popText();
    
    Element elt;

    if (uri != null && ! uri.equals(""))
      elt = _doc.createElementNS(uri, qName);
    else if (! qName.equals(""))
      elt = _doc.createElement(qName);
    else
      elt = _doc.createElement(localName);
    
    if (_node == _doc) {
      if (_doc.getDocumentElement() == null)
        ((QDocument) _doc).setDocumentElement(elt);
      else if (_strictXml)
        throw error(L.l("expected a single top-level element at `{0}'", qName));
    }

    _node.appendChild(elt);
    _node = elt;

    int length = attributes.getLength();
    for (int i = 0; i < length; i++) {
      String attrUri = attributes.getURI(i);
      String attrQname = attributes.getQName(i);
      String value = attributes.getValue(i);

      Attr attr;

      if (attrUri != null && ! attrUri.equals(""))
        attr = _doc.createAttributeNS(attrUri, attrQname);
      else if (! attrQname.equals(""))
        attr = _doc.createAttribute(attrQname);
      else
        attr = _doc.createAttribute(attributes.getLocalName(i));
      
      attr.setNodeValue(value);

      ((Element) _node).setAttributeNode(attr);
    }

    if (_extLocator != null)
      ((QElement) elt).setLocation(_extLocator.getSystemId(),
                                   _extLocator.getFilename(),
                                   _extLocator.getLineNumber(),
                                   _extLocator.getColumnNumber());

    QDocumentType dtd = (QDocumentType) _doc.getDoctype();
    if (dtd != null) {
      dtd.fillDefaults((QElement) elt);
    }
  }

  public void dtd(QDocumentType dtd)
  {
    ((QDocument) _doc).setDoctype(dtd);
    
    ((QDocument) _doc).appendChild(dtd);
  }

  public void attribute(String uri, String localName, String qName,
                        String value)
    throws IOException
  {
    if (_node instanceof Element) {
      Attr attr = _doc.createAttributeNS(uri, qName);
      attr.setNodeValue(value);

      ((Element) _node).setAttributeNode(attr);
    }
    else
      ((QDocument) _node).setAttribute(qName, value);
  }

  public void endElement(String uri, String localName, String qName)
  {
    popText();

    if (_node != null) // XXX:
      _node = _node.getParentNode();
    if (_node == null)
      _node = _doc;
  }

  public void processingInstruction(String name, String data)
  {
    popText();
    
    ProcessingInstruction pi = _doc.createProcessingInstruction(name, data);

    _node.appendChild(pi);
  }

  /**
   * Handles the callback for a comment.
   *
   * @param data the content of the comment.
   */
  public void comment(char []buf, int offset, int length)
    throws SAXException
  {
    try {
      comment(new String(buf, offset, length));
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Handles the callback for a comment.
   *
   * @param data the content of the comment.
   */
  public void comment(String data)
    throws IOException
  {
    popText();
    
    Comment comment = _doc.createComment(data);

    _node.appendChild(comment);
  }

  public boolean getEscapeText()
  {
    return _escapeText;
  }

  public void setEscapeText(boolean isEscaped)
  {
    _escapeText = isEscaped;
  }

  public void text(String text)
    throws IOException
  {
    if (_singleText == null && _text.length() == 0) {
      if (! text.equals(""))
        _singleText = text;
    }
    else if (_singleText != null) {
      _text.append(_singleText);
      _text.append(text);
    }
    else
      _text.append(text);
    
    if (! _isCoalescing)
      popText();
  }

  public void text(char []buffer, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;
    
    if (_singleText != null) {
      _singleText = null;
      _text.append(_singleText);
    }
    _text.append(buffer, offset, length);
    
    if (! _isCoalescing)
      popText();
  }

  /**
   * Adds text characters to the current document.
   */
  public void characters(char []buffer, int offset, int length)
    throws SAXException
  {
    if (length == 0)
      return;
    
    if (_strictXml && _node == _doc) {
      if (_doc.getDocumentElement() == null) {
        while (length > 0 && XmlChar.isWhitespace(buffer[offset])) {
          offset++;
          length--;
        }

        for (int i = 0; i < length; i++) {
          if (buffer[offset + i] == '\n' || buffer[offset + i] == '\r') {
            length = i;
            break;
          }
        }
            
        if (length > 16)
          length = 16;

        if (length > 0)
          throw error(L.l("expected top element at `{0}'",
                          new String(buffer, offset, length)));
      }
    }
    
    _text.append(buffer, offset, length);
    
    /*
    if (! isCoalescing)
      popText();
    */
  }

  /**
   * Handles the callback for ignorable whitespace.
   *
   * @param buffer the character buffer containing the whitespace.
   * @param offset starting offset into the character buffer.
   * @param length number of characters in the buffer.
   */
  public void ignorableWhitespace(char []buffer, int offset, int length)
    throws SAXException
  {
    if (! _skipWhitespace)
      characters(buffer, offset, length);
  }

  public void entityReference(String name)
  {
    popText();
    
    QEntityReference er = new QEntityReference(name);
    er._owner = (QDocument) _doc;

    _node.appendChild(er);
  }

  public void skippedEntity(String s)
  {
    _text.append(s);
  }

  public void cdata(String text)
    throws IOException
  {
    popText();

    _node.appendChild(_doc.createCDATASection(text));
  }

  public void cdata(char []buffer, int offset, int length)
    throws IOException
  {
    cdata(new String(buffer, offset, length));
  }

  private void popText()
  {
    if (_singleText != null) {
      Node text = _doc.createTextNode(_singleText);
      _node.appendChild(text);

      _singleText = null;
      return;
    }
    
    if (_text.length() == 0)
      return;
    
    Node text = _doc.createTextNode(_text.toString());
    _text.clear();

    _node.appendChild(text);
  }
  
  public void fatalError(SAXParseException e)
    throws SAXException
  {
    log.log(Level.FINE, e.toString(), e);
    
    throw error(e.getMessage());
  }

  public void error(SAXParseException e)
    throws SAXException
  {
    log.log(Level.FINER, e.toString(), e);
    
    throw error(e.getMessage());
  }

  public void warning(SAXParseException e)
    throws SAXException
  {
    log.log(Level.FINER, e.toString(), e);
  }
    
  /**
   * Throws an appropriate error.
   */
  public SAXException createError(Exception e)
  {
    if (e instanceof SAXException)
      return (SAXException) e;
    else
      return new SAXException(e);
  }

  /**
   * Returns a new parse exception with filename and line if available.
   */
  XmlParseException error(String text)
  {
    if (_extLocator != null)
      return new XmlParseException(_extLocator.getFilename() + ":" +
                                   _extLocator.getLineNumber() + ": " + text);
    else if (_locator != null)
      return new XmlParseException(_locator.getSystemId() + ":" +
                                   _locator.getLineNumber() + ": " + text);
    else
      return new XmlParseException(text);
  }
}
