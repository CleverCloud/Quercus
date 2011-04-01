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

package com.caucho.xml;

import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.EnclosedWriteStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import org.w3c.dom.*;
import org.xml.sax.Locator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Controls printing of XML documents.
 *
 * Typical use:
 * <code><pre>
 * Node node = ...;
 *
 * OutputStream os = Vfs.openWrite("test.xml");
 * XmlPrinter printer = new XmlPrinter(os);
 *
 * printer.printXml(node);
 * </pre></code>
 */
public class XmlPrinter implements XMLWriter {
  static final Logger log
    = Logger.getLogger(XmlPrinter.class.getName());
  static final L10N L = new L10N(XmlPrinter.class);
  
  private static final int NO_PRETTY = 0;
  private static final int INLINE = 1;
  private static final int NO_INDENT = 2;
  private static final int PRE = 3;

  private static final char OMITTED_SPACE = 0;
  private static final char OMITTED_NEWLINE = 1;
  private static final char OMITTED = 2;
  private static final char NULL_SPACE = 3;
  private static final char SPACE = 4;
  private static final char NEWLINE = 5;
  private static final char WHITESPACE = 6;

  private static final int ALWAYS_EMPTY = 1;
  private static final int EMPTY_IF_EMPTY = 2;
  
  private static IntMap _empties;
  private static HashMap<String,String> _booleanAttrs;
  private static HashMap<String,String> _verbatimTags;
  private static IntMap _prettyMap;

  private WriteStream _os;
  private char []_buffer = new char[256];
  private int _capacity = _buffer.length;
  private int _length;

  boolean _isAutomaticMethod = true;
  boolean _isTop = true;
  boolean _isJsp = false;
  String _encoding;
  String _method;
  boolean _isText;
  boolean _isHtml;
  boolean _inHead;
  String _version;

  boolean _isAutomaticPretty = true;
  boolean _isPretty;
  int _indent;
  int _preCount;
  int _lastTextChar = NULL_SPACE;
  boolean _hasMetaContentType = false;
  boolean _includeContentType = true;

  boolean _printDeclaration;
  
  String _standalone;
  String _systemId;
  String _publicId;
  private ExtendedLocator _locator;
  
  boolean _escapeText = true;
  boolean _inVerbatim = false;
  
  private HashMap<String,String> _namespace;
  private HashMap<String,String> _cdataElements;
  private Entities _entities;
  private String _mimeType;

  private ArrayList<String> _prefixList;
  
  private ArrayList<String> _attributeNames = new ArrayList<String>();
  private ArrayList<String> _attributeValues = new ArrayList<String>();

  private char []_cbuf = new char[256];
  private char []_abuf = new char[256];
  
  private LineMap _lineMap;
  private int _line;
  private String _srcFilename;
  private int _srcLine;

  private String _currentElement;
  private Document _currentDocument;

  private boolean _isEnclosedStream;

  /**
   * Create an XmlPrinter.  Using this API, you'll need to use
   * printer.init(os) to assign an output stream.
   */
  public XmlPrinter()
  {
  }

  /**
   * Creates a new XmlPrinter writing to an output stream.
   *
   * @param os output stream serving as the destination
   */
  public XmlPrinter(OutputStream os)
  {
    if (os instanceof WriteStream)
      init((WriteStream) os);
    else if (os instanceof EnclosedWriteStream)
      init(((EnclosedWriteStream) os).getWriteStream());
    else {
      _isEnclosedStream = true;
      WriteStream ws = Vfs.openWrite(os);
      try {
        ws.setEncoding("UTF-8");
      } catch (UnsupportedEncodingException e) {
      }
      init(ws);
    }
  }

  /**
   * Creates a new XmlPrinter writing to a writer.
   *
   * @param writer destination of the serialized node
   */
  public XmlPrinter(Writer writer)
  {
    if (writer instanceof EnclosedWriteStream)
      init(((EnclosedWriteStream) writer).getWriteStream());
    else {
      _isEnclosedStream = true;
      WriteStream ws = Vfs.openWrite(writer);
      init(ws);
    }
  }

  /**
   * Initialize the XmlPrinter with the write stream.
   *
   * @param os WriteStream containing the results.
   */
  public void init(WriteStream os)
  {
    _os = os;
    init();
  }

  /**
   * Initialize the XmlWriter in preparation for serializing a new XML.
   */
  void init()
  {
    String encoding = null;

    if (_os != null)
      encoding = _os.getEncoding();

    _length = 0;

    if (encoding == null ||
        encoding.equals("US-ASCII") || encoding.equals("ISO-8859-1"))
      _entities = XmlLatin1Entities.create();
    else
      _entities = XmlEntities.create();
    _encoding = encoding;
    _namespace = new HashMap<String,String>();
    _line = 1;
    _isTop = true;
    _hasMetaContentType = false;
    _attributeNames.clear();
    _attributeValues.clear();
  }

  /**
   * Prints the node as XML.  The destination stream has already been
   * set using init() or in the constructor.
   *
   * @param node source DOM node
   */
  public static void print(Path path, Node node)
    throws IOException
  {
    WriteStream os = path.openWrite();

    try {
      new XmlPrinter(os).printXml(node);
    } finally {
      os.close();
    }
  }

  /**
   * Prints the node as XML.  The destination stream has already been
   * set using init() or in the constructor.
   *
   * @param node source DOM node
   */
  public void printXml(Node node)
    throws IOException
  {
    _isAutomaticMethod = false;

    ((QAbstractNode) node).print(this);

    flush();
  }

  /**
   * Prints the node and children as HTML
   *
   * @param node the top node to print
   */
  public void printHtml(Node node)
    throws IOException
  {
    setMethod("html");
    setVersion("4.0");

    ((QAbstractNode) node).print(this);

    flush();
  }

  /**
   * Prints the node and children as XML, automatically indending
   *
   * @param node the top node to print
   */
  public void printPrettyXml(Node node)
    throws IOException
  {
    _isAutomaticMethod = false;
    setPretty(true);

    ((QAbstractNode) node).print(this);

    flush();
  }

  /**
   * Prints the node as XML to a string.
   *
   * @param node the source node
   * @return a string containing the XML.
   */
  public String printString(Node node)
    throws IOException
  {
    CharBuffer cb = CharBuffer.allocate();

    _os = Vfs.openWrite(cb);
    init(_os);
    try {
      ((QAbstractNode) node).print(this);
    } finally {
      flush();
      _os.close();
    }

    return cb.close();
  }

  /**
   * Sets to true if XML entities like &lt; should be escaped as &amp;lt;.
   * The default is true.
   *
   * @param escapeText set to true if entities should be escaped.
   */
  public void setEscaping(boolean escapeText)
  {
    if (! _isText)
      _escapeText = escapeText;
  }
  
  /**
   * Returns the current XML escaping.  If true, entities like &lt;
   * will be escaped as &amp;lt;.
   *
   * @return true if entities should be escaped.
   */
  public boolean getEscaping()
  {
    return _escapeText;
  }

  /**
   * Sets the output methods, like the XSL &lt;xsl:output method='method'/>.
   */
  public void setMethod(String method)
  {
    _method = method;

    if (method == null) {
      _isAutomaticMethod = true;
      _isHtml = false;
    } else if (method.equals("html")) {
      _isAutomaticMethod = false;
      _isHtml = true;
      if (_isAutomaticPretty)
        _isPretty = true;
    } else if (method.equals("text")) {
      _isAutomaticMethod = false;
      _isText = true;
      _escapeText = false;
    } else {
      _isAutomaticMethod = false;
      _isHtml = false;
    }
  }
  
  /**
   * Sets the XML/HTML version of the output file.
   *
   * @param version the output version
   */
  public void setVersion(String version)
  {
    _version = version;
  }
  
  /**
   * Sets the character set encoding for the output file.
   *
   * @param encoding the mime name of the output encoding
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
    try {
      if (encoding != null) {
        _os.setEncoding(encoding);

        if (encoding.equals("US-ASCII") || encoding.equals("ISO-8859-1"))
          _entities = XmlLatin1Entities.create();
        else
          _entities = XmlEntities.create();
      }
    } catch (Exception e) {
    }
  }

  public void setMimeType(String mimeType)
  {
    _mimeType = mimeType;
    if (_method == null && mimeType != null && mimeType.equals("text/html"))
      setMethod("html");
  }

  /**
   * Set true if this is JSP special cased.
   */
  public void setJSP(boolean isJsp)
  {
    _isJsp = isJsp;
  }

  /**
   * True if this is JSP special cased.
   */
  public boolean isJSP()
  {
    return _isJsp;
  }

  /**
   * Returns true if the printer is printing HTML.
   */
  boolean isHtml()
  {
    return _isHtml;
  }

  /**
   * Set to true if the printer should add whitespace to 'pretty-print'
   * the output.
   *
   * @param isPretty if true, add spaces for printing
   */
  public void setPretty(boolean isPretty)
  {
    _isPretty = isPretty;
    _isAutomaticPretty = false;
  }

  /**
   * Returns true if the printer is currently pretty-printing the output.
   */
  public boolean isPretty()
  {
    return _isPretty;
  }
  
  public void setPrintDeclaration(boolean printDeclaration)
  {
    _printDeclaration = printDeclaration;
  }
  
  boolean getPrintDeclaration()
  {
    return _printDeclaration;
  }
  
  public void setStandalone(String standalone)
  {
    _standalone = standalone;
  }
  
  String getStandalone()
  {
    return _standalone;
  }
  
  public void setSystemId(String id)
  {
    _systemId = id;
  }
  
  String getSystemId()
  {
    return _systemId;
  }

  /**
   * Set true if the printer should automatically add the
   * &lt;meta content-type> to HTML.
   */
  public void setIncludeContentType(boolean include)
  {
    _includeContentType = include;
  }
  
  /**
   * Return true if the printer should automatically add the
   * &lt;meta content-type> to HTML.
   */
  public boolean getIncludeContentType()
  {
    return _includeContentType;
  }
  
  public void setPublicId(String id)
  {
    _publicId = id;
  }
  
  String getPublicId()
  {
    return _publicId;
  }

  public Path getPath()
  {
    if (_os instanceof WriteStream)
      return ((WriteStream) _os).getPath();
    else
      return null;
  }

  /**
   * Creates a new line map.
   */
  public void setLineMap(String filename)
  {
    _lineMap = new LineMap(filename);
  }

  public LineMap getLineMap()
  {
    return _lineMap;
  }

  public void addCdataElement(String elt)
  {
    if (_cdataElements == null)
      _cdataElements = new HashMap<String,String>();
    _cdataElements.put(elt, "");
  }

  public void print(Node node)
    throws IOException
  {
    if (node instanceof QAbstractNode)
      ((QAbstractNode) node).print(this);
    else {
      printNode(node);
    }
    
    if (_isEnclosedStream)
      _os.flush();
  }

  public void printNode(Node node)
    throws IOException
  {
    if (node == null)
      return;

    switch (node.getNodeType()) {
    case Node.DOCUMENT_NODE:
      startDocument((Document) node);
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling())
        printNode(child);
      endDocument();
      break;
      
    case Node.ELEMENT_NODE: {
      Element elt = (Element) node;
      
      startElement(elt.getNamespaceURI(),
                   elt.getLocalName(),
                   elt.getNodeName());

      NamedNodeMap attrs = elt.getAttributes();
      int len = attrs.getLength();
      for (int i = 0; i < len; i++) {
        Attr attr = (Attr) attrs.item(i);

        attribute(attr.getNamespaceURI(),
                  attr.getLocalName(),
                  attr.getNodeName(),
                  attr.getNodeValue());
      }
      
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        printNode(child);
      }
      endElement(elt.getNamespaceURI(), elt.getLocalName(), elt.getNodeName());
      break;
    }
    
    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
    {
      CharacterData text = (CharacterData) node;
      text(text.getData());
      break;
    }
    
    case Node.COMMENT_NODE:
    {
      Comment comment = (Comment) node;
      comment(comment.getData());
      break;
    }
    
    case Node.PROCESSING_INSTRUCTION_NODE:
    {
      ProcessingInstruction pi = (ProcessingInstruction) node;
      processingInstruction(pi.getNodeName(), pi.getData());
      break;
    }
    }
  }

  WriteStream getStream()
  {
    return _os;
  }

  public void startDocument(Document document)
    throws IOException
  {
    _currentDocument = document;

    startDocument();
  }

  /**
   * Callback when the document starts printing.
   */
  public void startDocument()
    throws IOException
  {
    _isTop = true;
  }

  /**
   * Callback when the document completes
   */
  public void endDocument()
    throws IOException
  {
    if (_isPretty && _lastTextChar < SPACE)
      println();

    flush();
  }

  /**
   * Sets the locator.
   */
  public void setDocumentLocator(Locator locator)
  {
    _locator = (ExtendedLocator) locator;
  }
  
  /**
   * Sets the current location.
   *
   * @param filename the source filename
   * @param line the source line
   * @param column the source column
   */
  public void setLocation(String filename, int line, int column)
  {
    _srcFilename = filename;
    _srcLine = line;
  }

  /**
   * Called at the start of a new element.
   *
   * @param url the namespace url
   * @param localName the local name
   * @param qName the qualified name
   */
  public void startElement(String url, String localName, String qName)
    throws IOException
  {
    if (_isText)
      return;
    
    if (_isAutomaticMethod) {
      _isHtml = (qName.equalsIgnoreCase("html") &&
                 (url == null || url.equals("")));
      
      if (_isAutomaticPretty)
        _isPretty = _isHtml;

      _isAutomaticMethod = false;
    }

    if (_isTop)
      printHeader(qName);

    if (_currentElement != null)
      completeOpenTag();

    _attributeNames.clear();
    _attributeValues.clear();

    if (_isHtml && _verbatimTags.get(qName.toLowerCase()) != null)
      _inVerbatim = true;

    if (_isPretty && _preCount <= 0)
      printPrettyStart(qName);

    if (_lineMap == null) {
    }
    else if (_locator != null) {
      _lineMap.add(_locator.getFilename(), _locator.getLineNumber(), _line);
    }
    else if (_srcFilename != null)
      _lineMap.add(_srcFilename, _srcLine, _line);
    
    print('<');
    print(qName);
    _currentElement = qName;
    _lastTextChar = NULL_SPACE;
  }

  /**
   * Prints the header, if necessary.
   *
   * @param top name of the top element
   */
  public void printHeader(String top)
    throws IOException
  {
    if (! _isTop)
      return;
    
    _isTop = false;
    
    String encoding = _encoding;

    if (encoding != null && encoding.equalsIgnoreCase("UTF-16"))
      print('\ufeff');
    
    if (_isHtml) {
      double dVersion = 4.0;
      
      if (_version == null || _version.compareTo("4.0") >= 0) {
      }
      else {
        dVersion = 3.2;
      }

      if (_systemId != null || _publicId != null)
        printDoctype("html");

      if (encoding == null || encoding.equalsIgnoreCase("ISO-8859-1"))
         // _entities = Latin1Entities.create(dVersion);
        _entities = HtmlEntities.create(dVersion);
      else if (encoding.equalsIgnoreCase("US-ASCII"))
        _entities = HtmlEntities.create(dVersion);
      else
        _entities = OtherEntities.create(dVersion);
    }
    else {
      if (_printDeclaration) {
        String version = _version;

        if (version == null)
          version = "1.0";

        print("<?xml version=\"");
        print(version);
        print("\"");

        if (encoding == null ||
            encoding.equals("") ||
            encoding.equalsIgnoreCase("US-ASCII")) {
        }
        else
          print(" encoding=\"" + encoding + "\"");

        if (_standalone != null &&
            (_standalone.equals("true") || _standalone.equals("yes")))
          print(" standalone=\"yes\"");

        println("?>");
      }

      printDoctype(top);

      if (encoding == null ||
          encoding.equalsIgnoreCase("US-ASCII") ||
          encoding.equalsIgnoreCase("ISO-8859-1"))
        _entities = XmlLatin1Entities.create();
      else
        _entities = XmlEntities.create();
    }
    
    _lastTextChar = NEWLINE;
  }

  /**
   * Prints the doctype declaration
   *
   * @param topElt name of the top element
   */
  private void printDoctype(String topElt)
    throws IOException
  {
    if (_publicId != null && _systemId != null)
      println("<!DOCTYPE " + topElt + " PUBLIC \"" + _publicId + "\" \"" +
              _systemId + "\">");
    else if (_publicId != null)
      println("<!DOCTYPE " + topElt + " PUBLIC \"" + _publicId + "\">");
    else if (_systemId != null)
      println("<!DOCTYPE " + topElt + " SYSTEM \"" + _systemId + "\">");
    else if (_currentDocument instanceof QDocument) {
      QDocumentType dtd = (QDocumentType) _currentDocument.getDoctype();

      if (dtd != null && dtd.getName() != null && dtd.getParentNode() == null) {
        dtd.print(this);
        println();
      }
    }
  }

  public void startPrefixMapping(String prefix, String uri)
    throws IOException
  {
  }
  
  public void endPrefixMapping(String prefix)
    throws IOException
  {
  }

  /**
   * Pretty printing for a start tag.
   *
   * @param qName the name of the element
   */
  private void printPrettyStart(String qName)
    throws IOException
  {
    int code = _isHtml ? _prettyMap.get(qName.toLowerCase()) : -1;

    if (code == NO_PRETTY) {
      if (_lastTextChar == OMITTED_NEWLINE)
        println();
      else if (_lastTextChar == OMITTED_SPACE)
        print(' ');
    }
    else if (code != INLINE && _lastTextChar < WHITESPACE) {
      if (_lastTextChar != NEWLINE)
        println();
      for (int i = 0; i < _indent; i++)
        print(' ');
    }
    else if (code == INLINE && _lastTextChar < WHITESPACE) {
      if (_lastTextChar == OMITTED_NEWLINE)
        println();
      else if (_lastTextChar == OMITTED_SPACE)
        print(' ');
    }

    if (! _isHtml || code < 0) {
      _indent += 2;
    }

    if (code == PRE) {
      _preCount++;
      _lastTextChar = 'a';
    }
    else if (code == NO_PRETTY || code == INLINE)
      _lastTextChar = 'a';
    else
      _lastTextChar = NULL_SPACE;
  }

  /**
   * Prints an attribute
   *
   * @param uri namespace uri
   * @param localName localname of the attribute
   * @param qName qualified name of the attribute
   * @param value value of the attribute.
   */
  public void attribute(String uri, String localName, String qName,
                        String value)
    throws IOException
  {
    if (_isText)
      return;

    if (_currentElement != null) {
    }
    else if (qName.equals("encoding")) {
      _encoding = value;
      return;
    }
    else if (qName.startsWith("xmlns")) {
    }
    else
      throw new IOException(L.l("attribute `{0}' outside element.", qName));
    
    qName = qName.intern();

    if (qName.startsWith("xmlns")) {
      if (localName == null)
        localName = "";

      if (_isHtml && localName.equals("") && value.equals(""))
        return;

      _namespace.put(localName, value);
      if (_prefixList == null)
        _prefixList = new ArrayList<String>();
      if (! _prefixList.contains(localName))
        _prefixList.add(localName);
      return;
    }
    else if (qName.equals("xtp:jsp-attribute")) {
      _attributeNames.add("<%= " + value + "%>");
      _attributeValues.add(null);
      return;
    }
    
    if (_isHtml && ! _hasMetaContentType &&
        _currentElement.equals("meta") &&
        qName.equalsIgnoreCase("http-equiv") &&
        value.equalsIgnoreCase("content-type")) {
      _hasMetaContentType = true;
    }

    for (int i = 0; i < _attributeNames.size(); i++) {
      String oldName = _attributeNames.get(i);

      if (oldName == qName) {
        _attributeValues.set(i, value);
        return;
      }
    }

    if (qName == null || qName.equals(""))
      throw new NullPointerException();

    _attributeNames.add(qName);
    _attributeValues.add(value);
  }

  /**
   * Complete printing of the attributes when the open tag completes.
   */
  public boolean finishAttributes()
    throws IOException
  {
    if (_currentElement == null)
      return false;

    for (int i = 0; i < _attributeNames.size(); i++) {
      String qName = _attributeNames.get(i);
      String value = _attributeValues.get(i);
      
      if (_isHtml &&
          _booleanAttrs.get(qName.toLowerCase()) != null &&
          (value == null || value.equals("") || value.equals(qName))) {
        print(' ');
        print(qName);
      }
      else {
        print(' ');
        print(qName);

        if (value != null) {
          print("=\"");

          if (! _escapeText || _inVerbatim)
            print(value);
          /*
          else if (isHtml && isURIAttribute(currentElement, qName)) {
            int len = value.length();
            int offset = 0;

            while (len > abuf.length) {
              value.getChars(offset, offset + abuf.length, abuf, 0);
              entities.printURIAttr(this, abuf, 0, abuf.length);
              len -= abuf.length;
              offset += abuf.length;
            }
            
            value.getChars(offset, offset + len, abuf, 0);
            entities.printURIAttr(this, abuf, 0, len);
          }
          */
          else {
            int len = value.length();
            int offset = 0;

            while (len > _abuf.length) {
              value.getChars(offset, offset + _abuf.length, _abuf, 0);
              _entities.printText(this, _abuf, 0, _abuf.length, true);
              len -= _abuf.length;
              offset += _abuf.length;
            }
            
            value.getChars(offset, offset + len, _abuf, 0);
            _entities.printText(this, _abuf, 0, len, true);
          }
          print('\"');
        }
        else if (! _isHtml) {
          print("=\"\"");
        }
      }
    }

    if (_prefixList != null && _prefixList.size() > 0) {
      for (int i = 0; i < _prefixList.size(); i++) {
        String prefix = _prefixList.get(i);
        String url = _namespace.get(prefix);

        if (prefix.equals("")) {
          print(" xmlns=\"");
          print(url);
          print('\"');
        }
        else if (prefix.startsWith("xmlns")) {
          print(" ");
          print(prefix);
          print("=\"");
          print(url);
          print('\"');
        }
        else {
          print(" xmlns:");
          print(prefix);
          print("=\"");
          print(url);
          print('\"');
        }
      }
      
      _prefixList.clear();
      _namespace.clear();
    }
    
    _currentElement = null;
    // lastTextChar = NULL_SPACE;
    
    return true;
  }

  /**
   * Prints the end tag of an element
   *
   * @param uri the namespace uri of the element
   * @param localName the localname of the element tag
   * @param qName qualified name of the element
   */
  public void endElement(String uri, String localName, String qName)
    throws IOException
  {
    if (_isText)
      return;

    String normalName = _isHtml ? qName.toLowerCase() : qName;
    
    if (_isHtml && _verbatimTags.get(normalName) != null)
      _inVerbatim = false;

    int prevTextChar = _lastTextChar;
    boolean isEmpty = _currentElement != null;
    if (_currentElement != null)
      finishAttributes();
      
    if (! _isHtml || _hasMetaContentType) {
    }
    else if (normalName.equals("head")) {
      if (isEmpty)
        print(">");
      isEmpty = false;
      printMetaContentType();
      _currentElement = null;
    }
      
    if (isEmpty) {
      if (_isHtml && _empties.get(normalName) >= 0)
        print(">");
      else if (prevTextChar <= OMITTED) {
        print(">");
        printPrettyEnd(qName);
        print("</");
        print(qName);
        print(">");
        return;
      }
      else if (_isHtml) {
        print("></");
        print(qName);
        print(">");
      }
      else {
        print("/>");
      }

      if (_isPretty)
        closePretty(qName);
    }
    else if (_isHtml && _empties.get(normalName) >= 0 &&
             ! normalName.equals("p")) {
      if (_isPretty)
        closePretty(qName);
    }
    else if (_isPretty) {
      printPrettyEnd(qName);
      print("</");
      print(qName);
      print(">");
    }
    else {
      print("</");
      print(qName);
      print(">");
    }
    
    _currentElement = null;
  }

  /**
   * Handle pretty printing at an end tag.
   */
  private void printPrettyEnd(String qName)
    throws IOException
  {
    int code = _isHtml ? _prettyMap.get(qName.toLowerCase()) : -1;

    if (code == PRE) {
      _preCount--;
      _lastTextChar = NULL_SPACE;
      return;
    }
    else if (_preCount > 0) {
      return;
    }
    else if (code == NO_PRETTY) {
      if (_lastTextChar <= OMITTED)
        println();
      _lastTextChar = 'a';
      // indent -= 2;
      return;
    }
    else if (code == INLINE) {
      _lastTextChar = NULL_SPACE;
      return;
    }
    
    if (! _isHtml || code < 0) {
      _indent -= 2;
    }

    if (_lastTextChar <= WHITESPACE) {
      if (_lastTextChar != NEWLINE)
        println();
      for (int i = 0; i < _indent; i++)
        print(' ');
    }
    _lastTextChar = NULL_SPACE;
  }

  /**
   * Handle the pretty printing after the closing of a tag.
   */
  private void closePretty(String qName)
  {
    int code = _isHtml ? _prettyMap.get(qName.toLowerCase()) : -1;

    if (code == PRE) {
      _preCount--;
      _lastTextChar = NULL_SPACE;
      return;
    }
    if (_preCount > 0)
      return;
    
    if (! _isHtml || code < 0) {
      _indent -= 2;
    }
    
    if (code != NO_PRETTY)
      _lastTextChar = NULL_SPACE;
    else
      _lastTextChar = 'a';
  }

  /**
   * Prints a processing instruction
   *
   * @param name the name of the processing instruction
   * @param data the processing instruction data
   */
  public void processingInstruction(String name, String data)
    throws IOException
  {
    if (_isText)
      return;
    
    if (_currentElement != null)
      completeOpenTag();

    if (_isTop && ! _isHtml && ! _isAutomaticMethod) {
      printHeader(null);
      _isTop = false;
    }

    print("<?");
    print(name);

    if (data != null && data.length() > 0) {
      print(" ");
      print(data);
    }

    if (isHtml())
      print(">");
    else
      print("?>");
    
    _lastTextChar = NULL_SPACE;
  }

  /**
   * Prints a comment
   *
   * @param data the comment data
   */
  public void comment(String data)
    throws IOException
  {
    if (_isText)
      return;

    int textChar = _lastTextChar;
    
    if (_currentElement != null)
      completeOpenTag();

    if (_isPretty && _preCount <= 0 &&
        (textChar == OMITTED_NEWLINE || textChar == NULL_SPACE)) {
      println();
      
      for (int i = 0; i < _indent; i++)
        print(' ');
    }

    print("<!--");
    print(data);
    print("-->");
    
    _lastTextChar = NULL_SPACE;
  }

  /**
   * Returns true if the text is currently being escaped
   */
  public boolean getEscapeText()
  {
    return _escapeText;
  }

  /**
   * Sets true if the text should be escaped, else it will be printed
   * verbatim.
   */
  public void setEscapeText(boolean isEscaped)
  {
    _escapeText = isEscaped;
  }
  
  /**
   * Prints text.  If the text is escaped, codes like &lt; will be printed as
   * &amp;lt;.
   */
  public void text(String text)
    throws IOException
  {
    int length = text.length();
    
    for (int offset = 0; offset < length; offset += _cbuf.length) {
      int sublen = length - offset;
      if (sublen > _cbuf.length)
        sublen = _cbuf.length;

      text.getChars(offset, offset + sublen, _cbuf, 0);
      text(_cbuf, 0, sublen);
    }
  }

  /**
   * Prints text.  If the text is escaped, codes like &lt; will be printed as
   * &amp;lt;.
   */
  public void text(char []buffer, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    int prevTextChar = _lastTextChar;
    if ((_isPretty && _preCount <= 0 || _isTop) && ! _isText && 
        trimPrettyWhitespace(buffer, offset, length)) {
      if (prevTextChar <= WHITESPACE)
        return;
      if (_lastTextChar == OMITTED_SPACE)
        _lastTextChar = SPACE;
      if (_lastTextChar == OMITTED_NEWLINE)
        _lastTextChar = NEWLINE;
    }

    int nextTextChar = _lastTextChar;
    if (_currentElement != null) {
      completeOpenTag();
      
      if (_isPretty && _preCount <= 0 && prevTextChar <= OMITTED)
        println();
    }

    _lastTextChar = nextTextChar;

    if (! _isTop) {
    }
    else if (! _isJsp) {
      _isTop = false;
    }
    else if (_isAutomaticMethod) {
    }
    else if (! _isHtml) {
      printHeader(null);
      _isTop = false;
    }
    
    if (_isHtml && ! _hasMetaContentType && ! _inHead) {
      int textChar = _lastTextChar;
      
      if (_isPretty && _preCount <= 0 && prevTextChar <= OMITTED)
        println();
      
      // printHeadContentType();
      _lastTextChar = textChar;
      prevTextChar = 'a';
    }
    
    if (! _isPretty || _preCount > 0) {
    }
    else if (prevTextChar == OMITTED_NEWLINE) {
      if (buffer[offset] != '\n')
        println();
    }
    else if (prevTextChar == OMITTED_SPACE) {
      char ch = buffer[offset];
      
      if (ch != ' ' && ch != '\n')
        print(' ');
    }

    if (_lineMap == null) {
    }
    else if (_locator != null) {
      _lineMap.add(_locator.getFilename(), _locator.getLineNumber(), _line);
    }
    else if (_srcFilename != null)
      _lineMap.add(_srcFilename, _srcLine, _line);

    if (! _escapeText || _inVerbatim || _entities == null)
      print(buffer, offset, length);
    else
      _entities.printText(this, buffer, offset, length, false);
  }

  /**
   * If the text is completely whitespace, skip it.
   */
  boolean trimPrettyWhitespace(char []buffer, int offset, int length)
  {
    char textChar = 'a';
    int i = length - 1;

    for (; i >= 0; i--) {
      char ch = buffer[offset + i];

      if (ch == '\r' || ch == '\n') {
        if (textChar != NEWLINE)
          textChar = OMITTED_NEWLINE;
      }
      else if (ch == ' ' || ch == '\t') {
        if (textChar == 'a' || textChar == NULL_SPACE)
          textChar = OMITTED_SPACE;
      }
      else if (textChar == OMITTED_NEWLINE) {
        textChar = NEWLINE;
        break;
      }
      else if (textChar == OMITTED_SPACE) {
        textChar = SPACE;
        break;
      }
      else
        break;
    }

    _lastTextChar = textChar;

    return (i < 0 && textChar <= WHITESPACE);
  }

  public void cdata(String text)
    throws IOException
  {
    if (text.length() == 0)
      return;
    
    _isTop = false;
    
    if (_currentElement != null)
      completeOpenTag();

    if (_lineMap != null && _srcFilename != null)
      _lineMap.add(_srcFilename, _srcLine, _line);

    print("<![CDATA[");

    print(text);
    
    print("]]>");
    
    _lastTextChar = NEWLINE;
  }

  private void completeOpenTag()
    throws IOException
  {
    boolean isHead = (_isHtml && ! _hasMetaContentType &&
                      _currentElement.equalsIgnoreCase("head"));
    
    finishAttributes();
    print(">");

    if (isHead)
      printHeadContentType();
  }

  public void cdata(char []buffer, int offset, int length)
    throws IOException
  {
    cdata(new String(buffer, offset, length));
  }

  private void printHeadContentType()
    throws IOException
  {
    printMetaContentType();
  }

  private void printMetaContentType()
    throws IOException
  {
    if (! _includeContentType)
      return;
    
    _hasMetaContentType = true;
    if (_lastTextChar != NEWLINE)
      println();

    if (_encoding == null ||  _encoding.equals("US-ASCII"))
      _encoding = "ISO-8859-1";
    String mimeType = _mimeType;
    if (mimeType == null)
      mimeType = "text/html";

    println("  <meta http-equiv=\"Content-Type\" content=\"" +
            mimeType + "; charset=" + _encoding + "\">");
    _lastTextChar = NEWLINE;
  }

  void printDecl(String text) throws IOException
  {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);

      switch (ch) {
      case '&': 
        if (i + 1 < text.length() && text.charAt(i + 1) == '#')
          print("&#38;");
        else
          print(ch);
        break;

      case '"': 
        print("&#34;");
        break;

      case '\'': 
        print("&#39;");
        break;

      case '\n':
        print("\n");
        break;

      default:
        print(ch);
      }
    }
  }

  /**
   * Prints a newline to the output stream.
   */
  void println() throws IOException
  {
    print('\n');
  }

  void println(String text) throws IOException
  {
    print(text);
    println();
  }

  /**
   * Prints a char buffer.
   */
  void print(char []buf)
    throws IOException
  {
    print(buf, 0, buf.length);
  }
  
  /**
   * Prints a char buffer.
   */
  void print(char []buf, int off, int len)
    throws IOException
  {
    for (int i = 0; i < len; i++)
      print(buf[off + i]);
  }

  /**
   * Prints a chunk of text.
   */
  void print(String text) throws IOException
  {
    int len = text.length();

    for (int i = 0; i < len; i++) {
      char ch = text.charAt(i);
      print(ch);
    }
  }

  /**
   * Prints a character.
   */
  void print(char ch) throws IOException
  {
    if (_capacity <= _length) {
      _os.print(_buffer, 0, _length);
      _length = 0;
    }

    _buffer[_length++] = ch;
    if (ch == '\n')
      _line++;
  }

  /**
   * Prints an integer to the output stream.
   */
  void print(int i) throws IOException
  {
    if (i < 0) {
    }
    else if (i < 10) {
      print((char) ('0' + i));
      return;
    }
    else if (i < 100) {
      print((char) ('0' + i / 10));
      print((char) ('0' + i % 10));
      return;
    }
    
    if (_length >= 0) {
      _os.print(_buffer, 0, _length);
      _length = 0;
    }

    _os.print(i);
  }

  private void flush() throws IOException
  {
    if (_length >= 0) {
      _os.print(_buffer, 0, _length);
      _length = 0;
    }

    if (_isEnclosedStream)
      _os.flush();
  }

  private void close() throws IOException
  {
    flush();
    
    if (_isEnclosedStream)
      _os.close();
  }
    

  static void add(IntMap map, String name, int value)
  {
    map.put(name, value);
    map.put(name.toUpperCase(), value);
  }

  static void add(HashMap<String,String> map, String name)
  {
    map.put(name, name);
    map.put(name.toUpperCase(), name);
  }

  static {
    _empties = new IntMap();
    add(_empties, "basefont", ALWAYS_EMPTY);
    add(_empties, "br", ALWAYS_EMPTY);
    add(_empties, "area", ALWAYS_EMPTY);
    add(_empties, "link", ALWAYS_EMPTY);
    add(_empties, "img", ALWAYS_EMPTY);
    add(_empties, "param", ALWAYS_EMPTY);
    add(_empties, "hr", ALWAYS_EMPTY);
    add(_empties, "input", ALWAYS_EMPTY);
    add(_empties, "col", ALWAYS_EMPTY);
    add(_empties, "frame", ALWAYS_EMPTY);
    add(_empties, "isindex", ALWAYS_EMPTY);
    add(_empties, "base", ALWAYS_EMPTY);
    add(_empties, "meta", ALWAYS_EMPTY);
    
    add(_empties, "p", ALWAYS_EMPTY);
    add(_empties, "li", ALWAYS_EMPTY);
    
    add(_empties, "option", EMPTY_IF_EMPTY);
    
    _booleanAttrs = new HashMap<String,String>();
    // input
    add(_booleanAttrs, "checked");
    // dir, menu, dl, ol, ul
    add(_booleanAttrs, "compact");
    // object
    add(_booleanAttrs, "declare");
    // script
    add(_booleanAttrs, "defer");
    // button, input, optgroup, option, select, textarea
    add(_booleanAttrs, "disabled");
    // img
    add(_booleanAttrs, "ismap");
    // select
    add(_booleanAttrs, "multiple");
    // area
    add(_booleanAttrs, "nohref");
    // frame
    add(_booleanAttrs, "noresize");
    // hr
    add(_booleanAttrs, "noshade");
    // td, th
    add(_booleanAttrs, "nowrap");
    // textarea, input
    add(_booleanAttrs, "readonly");
    // option
    add(_booleanAttrs, "selected");
    
    _prettyMap = new IntMap();
    // next two break browsers
    add(_prettyMap, "img", NO_PRETTY);
    add(_prettyMap, "a", NO_PRETTY);
    add(_prettyMap, "embed", NO_PRETTY);
    add(_prettyMap, "th", NO_PRETTY);
    add(_prettyMap, "td", NO_PRETTY);
    // inline tags look better without the indent
    add(_prettyMap, "tt", INLINE);
    add(_prettyMap, "i", INLINE);
    add(_prettyMap, "b", INLINE);
    add(_prettyMap, "big", INLINE);
    add(_prettyMap, "em", INLINE);
    add(_prettyMap, "string", INLINE);
    add(_prettyMap, "dfn", INLINE);
    add(_prettyMap, "code", INLINE);
    add(_prettyMap, "samp", INLINE);
    add(_prettyMap, "kbd", INLINE);
    add(_prettyMap, "var", INLINE);
    add(_prettyMap, "cite", INLINE);
    add(_prettyMap, "abbr", INLINE);
    add(_prettyMap, "acronym", INLINE);
    add(_prettyMap, "object", INLINE);
    add(_prettyMap, "q", INLINE);
    add(_prettyMap, "sub", INLINE);
    add(_prettyMap, "sup", INLINE);
    add(_prettyMap, "font", INLINE);
    add(_prettyMap, "small", INLINE);
    add(_prettyMap, "span", INLINE);
    add(_prettyMap, "bdo", INLINE);
    add(_prettyMap, "jsp:expression", INLINE);
    
    add(_prettyMap, "textarea", PRE);
    add(_prettyMap, "pre", PRE);
    
    add(_prettyMap, "html", NO_INDENT);
    add(_prettyMap, "body", NO_INDENT);
    add(_prettyMap, "ul", NO_INDENT);
    add(_prettyMap, "table", NO_INDENT);
    add(_prettyMap, "frameset", NO_INDENT);
    
    _verbatimTags = new HashMap<String,String>();
    add(_verbatimTags, "script");
    add(_verbatimTags, "style");
  }
}
