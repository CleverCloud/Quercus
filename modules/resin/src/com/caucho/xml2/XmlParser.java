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

package com.caucho.xml2;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;
import com.caucho.xml2.readers.MacroReader;
import com.caucho.xml2.readers.Utf16Reader;
import com.caucho.xml2.readers.Utf8Reader;
import com.caucho.xml2.readers.XmlReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * A configurable XML parser.  Loose versions of XML and HTML are supported
 * by changing the Policy object.
 *
 * <p>Normally, applications will use Xml, LooseXml, Html, or LooseHtml.
 */
public class XmlParser extends AbstractParser {
  // Xerces uses the following
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
  public static final String XML = "http://www.w3.org/XML/1998/namespace";

  static final QName DOC_NAME = new QName("#document");
  static final QName TEXT_NAME = new QName("#text");
  static final QName WHITESPACE_NAME = new QName("#whitespace");
  
  private static final boolean []XML_NAME_CHAR;

  QAttributes _attributes;
  QAttributes _nullAttributes;

  CharBuffer _text;
  CharBuffer _eltName;
  CharBuffer _cb;
  CharBuffer _buf = new CharBuffer();
  String _textFilename;
  int _textLine;

  TempCharBuffer _tempInputBuffer;
  char []_inputBuffer;
  int _inputOffset;
  int _inputLength;

  char []_textBuffer = new char[1024];
  int _textLength;
  int _textCapacity = _textBuffer.length;
  boolean _isIgnorableWhitespace;
  
  char []_valueBuffer = _textBuffer;
  
  CharBuffer _name = new CharBuffer();
  CharBuffer _nameBuffer = new CharBuffer();
  
  MacroReader _macro = new MacroReader();
  int _macroIndex = 0;
  int _macroLength = 0;
  char []_macroBuffer;

  int []_elementLines = new int[64];
  int _elementTop;

  ArrayList<SaxIntern.Entry> _attrNames = new ArrayList<SaxIntern.Entry>();
  ArrayList<String> _attrValues = new ArrayList<String>();

  ReadStream _is;
  XmlReader _reader;
  
  String _extPublicId;
  String _extSystemId;

  NamespaceContextImpl _namespace = new NamespaceContextImpl();
  SaxIntern _intern = new SaxIntern(_namespace);;
  
  QName _activeNode;
  QName _topNamespaceNode;
  boolean _isTagStart;
  boolean _stopOnIncludeEnd;
  boolean _hasTopElement;
  boolean _hasDoctype;
  Locator _locator = new LocatorImpl(this);

  public XmlParser()
  {
  }

  /**
   * Creates a new parser with a given parsing policy and dtd.
   *
   * @param policy the parsing policy, handling optional tags.
   * @param dtd the parser's dtd.
   */
  XmlParser(QDocumentType dtd)
  {
    super(dtd);
  }

  /**
   * Initialize the parser.
   */
  void init()
  {
    super.init();
    
    _attributes = new QAttributes();
    _nullAttributes = new QAttributes();
    _eltName = new CharBuffer();
    _text = new CharBuffer();

    _textLength = 0;
    _isIgnorableWhitespace = true;
    _elementTop = 0;
    _elementLines[0] = 1;

    _line = 1;

    _dtd = null;
    _isTagStart = false;
    _stopOnIncludeEnd = false;

    _extPublicId = null;
    _extSystemId = null;

    _filename = null;
    _publicId = null;
    _systemId = null;
    
    _hasTopElement = false;
    _hasDoctype = false;

    _macroIndex = 0;
    _macroLength = 0;

    _reader = null;

    // _owner = null;
  }

  /**
   * Parse the document from a read stream.
   *
   * @param is read stream to parse from.
   *
   * @return The parsed document.
   */
  Document parseInt(ReadStream is)
    throws IOException, SAXException
  {
    _tempInputBuffer = TempCharBuffer.allocate();
    _inputBuffer = _tempInputBuffer.getBuffer();
    _inputLength = _inputOffset = 0;
    
    _is = is;

    if (_filename == null && _systemId != null)
      _filename = _systemId;
    else if (_filename == null)
      _filename = _is.getUserPath();

    if (_systemId == null) {
      _systemId = _is.getPath().getURL();
      if ("null:".equals(_systemId) || "string:".equals(_systemId))
        _systemId = "stream";
    }

    if (_filename == null)
      _filename = _systemId;

    if (_filename == null)
      _filename = "stream";

    if (_dtd != null)
      _dtd.setSystemId(_systemId);
    
    if (_builder != null) {
      if (! "string:".equals(_systemId) && ! "stream".equals(_systemId))
        _builder.setSystemId(_systemId);
      _builder.setFilename(_is.getPath().getURL());
    }

    if (_contentHandler == null)
      _contentHandler = new org.xml.sax.helpers.DefaultHandler();

    _contentHandler.setDocumentLocator(_locator);

    if (_owner == null)
      _owner = new QDocument();
    if (_defaultEncoding != null)
      _owner.setAttribute("encoding", _defaultEncoding);
    _owner.addDepend(is.getPath());
    
    _activeNode = DOC_NAME;
    
    _contentHandler.startDocument();
    
    parseXMLDeclaration(null);
    
    parseNode();

    /*
    if (dbg.canWrite()) {
      printDebugNode(dbg, doc, 0);
      dbg.flush();
    }
    */

    if (! _hasTopElement)
      throw error(L.l("XML file has no top-element.  All well-formed XML files have a single top-level element."));

    _contentHandler.endDocument();

    QDocument owner = _owner;
    _owner = null;
      
    return owner;
  }

  /**
   * The main dispatch loop.
   *
   * @param node the current node
   * @param ch the next character
   */
  private void parseNode()
    throws IOException, SAXException
  {
    char []valueBuffer = _valueBuffer;
    int valueLength = valueBuffer.length;
    int valueOffset = 0;
    boolean isWhitespace = true;
    
    char []inputBuffer = _inputBuffer;
    int inputLength = _inputLength;
    int inputOffset = _inputOffset;

  loop:
    while (true) {
      int ch;

      if (inputOffset < inputLength)
        ch = inputBuffer[inputOffset++];
      else if (fillBuffer()) {
        inputBuffer = _inputBuffer;
        inputOffset = _inputOffset;
        inputLength = _inputLength;

        ch = inputBuffer[inputOffset++];
      }
      else {
        if (valueOffset > 0)
          addText(valueBuffer, 0, valueOffset, isWhitespace);

        _inputOffset = inputOffset;
        _inputLength = inputLength;

        close();
        return;
      }

      switch (ch) {
      case '\n':
        _line++;
        valueBuffer[valueOffset++] = (char) ch;
        break;

      case ' ': case '\t': case '\r':
        valueBuffer[valueOffset++] = (char) ch;
        break;

      case 0xffff:
        // marker for end of text for serialization (?)
        if (valueOffset > 0)
          addText(valueBuffer, 0, valueOffset, isWhitespace);

        _inputOffset = inputOffset;
        _inputLength = inputLength;
        return;

      case '&':
        if (valueOffset > 0)
          addText(valueBuffer, 0, valueOffset, isWhitespace);

        _inputOffset = inputOffset;
        _inputLength = inputLength;

        parseEntityReference();

        inputOffset = _inputOffset;
        inputLength = _inputOffset;
        break;

      case '<':
        if (valueOffset > 0)
          addText(valueBuffer, 0, valueOffset, isWhitespace);

        _inputOffset = inputOffset;
        _inputLength = inputLength;

        ch = read();

        if (ch == '/') {
          SaxIntern.Entry entry = parseName(0, false);

          ch = read();

          if (ch != '>') {
            throw error(L.l("'</{0}>' expected '>' at {1}.  Closing tags must close immediately after the tag name.",
                            entry.getName(), badChar(ch)));
          }

          _namespace.pop(entry);
        }
        // element: <tag attr=value ... attr=value> ...
        else if (XmlChar.isNameStart(ch)) {
          parseElement(ch);
          ch = read();
        }
        // <! ...
        else if (ch == '!') {
          // <![CDATA[ ... ]]>
          if ((ch = read()) == '[') {
            parseCdata();
            ch = read();
          }
          // <!-- ... -->
          else if (ch == '-') {
            parseComment();

            ch = read();
          }
          else if (XmlChar.isNameStart(ch)) {
            unread(ch);

            SaxIntern.Entry entry = parseName(0, false);

            String declName = entry.getName();
            if (declName.equals("DOCTYPE")) {
              parseDoctype();
              if (_contentHandler instanceof DOMBuilder)
                ((DOMBuilder) _contentHandler).dtd(_dtd);
            }
            else
              throw error(L.l("expected '<!DOCTYPE' declaration at {0}", declName));
          }
          else
            throw error(L.l("expected '<!DOCTYPE' declaration at {0}", badChar(ch)));
        }
        // PI: <?tag attr=value ... attr=value?>
        else if (ch == '?') {
          parsePI();
        }
        else {
          throw error(L.l("expected tag name after '<' at {0}.  Open tag names must immediately follow the open brace like '<foo ...>'", badChar(ch)));
        }

        inputOffset = _inputOffset;
        inputLength = _inputLength;
        break;

      default:
        isWhitespace = false;
        valueBuffer[valueOffset++] = (char) ch;
        break;
      }

      if (valueOffset == valueLength) {
        addText(valueBuffer, 0, valueOffset, isWhitespace);

        valueOffset = 0;
      }
    }
  }

  /**
   * Parses the &lt;!DOCTYPE> declaration.
   */
  private void parseDoctype()
    throws IOException, SAXException
  {
    if (_activeNode != DOC_NAME)
      throw error(L.l("<!DOCTYPE immediately follow the <?xml ...?> declaration."));
    
    int ch = skipWhitespace(read());
    ch = _reader.parseName(_nameBuffer, ch);
    String name = _nameBuffer.toString();
    ch = skipWhitespace(ch);

    if (_dtd == null)
      _dtd = new QDocumentType(name);

    _dtd.setName(name);

    if (XmlChar.isNameStart(ch)) {
      ch = parseExternalID(ch);
      ch = skipWhitespace(ch);

      _dtd._publicId = _extPublicId;
      _dtd._systemId = _extSystemId;
    }

    if (_dtd._systemId != null && ! _dtd._systemId.equals("")) {
      InputStream is = null;

      unread(ch);
      
      XmlReader oldReader = _reader;
      boolean hasInclude = false;

      try {
        pushInclude(_extPublicId, _extSystemId);
        hasInclude = true;
      } catch (Exception e) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINER, e.toString(), e);
        else
          log.finer(e.toString());
      }

      if (hasInclude) {
        _stopOnIncludeEnd = true;
        try {
          DtdParser dtdParser = new DtdParser(this, _dtd);
          ch = dtdParser.parseDoctypeDecl(_dtd);
        } catch (XmlParseException e) {
          if (_extSystemId != null &&
              _extSystemId.startsWith("http")) {
            log.log(Level.FINE, e.toString(), e);
          }
          else
            throw e;
        }
        _stopOnIncludeEnd = false;

        while (_reader != null && _reader != oldReader)
          popInclude();
      }

      if (_reader != null)
        ch = skipWhitespace(read());
    }
    
    if (ch == '[') {
      DtdParser dtdParser = new DtdParser(this, _dtd);
      ch = dtdParser.parseDoctypeDecl(_dtd);
    }

    ch = skipWhitespace(ch);

    if (ch != '>')
      throw error(L.l("expected '>' in <!DOCTYPE at {0}",
                      badChar(ch)));
  }

  /**
   * Parses an element.
   *
   * @param ch the current character
   */
  private void parseElement(int ch)
    throws IOException, SAXException
  {
    unread(ch);

    SaxIntern.Entry entry = parseName(0, false);
    
    _namespace.push(entry);

    ch = read();
    
    if (ch != '>' && ch != '/') {
      ch = parseAttributes(ch, true);
    }
    else
      _attributes.clear();

    QName qName = entry.getQName();

    if (_isValidating && _dtd != null) {
      QElementDef elementDef = _dtd.getElement(qName.getLocalPart());
      
      if (elementDef != null)
        elementDef.fillDefaults(_attributes);
    }
    
    _contentHandler.startElement(entry.getUri(),
                                 entry.getLocalName(),
                                 entry.getName(),
                                 _attributes);

    _hasTopElement = true;

    if (ch == '/') {
      // empty tag: <foo/>
      if ((ch = read()) == '>') {
        _contentHandler.endElement(entry.getUri(),
                                   entry.getLocalName(),
                                   entry.getName());

        _namespace.pop(entry);
      }
      // short tag: </foo/some text here/>
      else {
        throw error(L.l("unexpected character {0} after '/', expected '/>'",
                      badChar(ch), entry.getName()));
      }
    }
    else if (ch != '>') {
      throw error(L.l("unexpected character {0} while parsing '{1}' attributes.  Expected an attribute name or '>' or '/>'.  XML element syntax is:\n  <name attr-1=\"value-1\" ... attr-n=\"value-n\">",
                      badChar(ch), entry.getName()));
    }
  }

  /**
   * Parses the attributes in an element.
   *
   * @param ch the next character to reader.read.
   *
   * @return the next character to read.
   */
  private int parseAttributes(int ch, boolean isElement)
    throws IOException, SAXException
  {
    _attributes.clear();

    _attrNames.clear();
    _attrValues.clear();

    while (ch != -1) {
      boolean hasWhitespace = false;

      while (ch <= 0x20
             && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) {
        hasWhitespace = true;
        ch = read();
      }

      if (! XmlChar.isNameStart(ch)) {
        break;
      }

      if (! hasWhitespace)
        throw error(L.l("attributes must be separated by whitespace"));

      hasWhitespace = false;

      unread(ch);

      SaxIntern.Entry entry = parseName(0, true);

      ch = read();


      while (ch <= 0x20
             && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) {
        ch = read();
      }

      String value = null;

      if (ch != '=') {
        throw error(L.l("attribute '{0}' expects value at {1}.  XML requires attributes to have explicit values.",
                        entry.getName(), badChar(ch)));
      }

      ch = read();
      
      while (ch <= 0x20
             && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) {
        ch = read();
      }

      value = parseValue(ch);

      ch = read();

      if (entry.isXmlns()) {
        String prefix;

        if (entry.getPrefix() != null)
          prefix = entry.getLocalName();
        else
          prefix = "";

        String uri = value;

        if (_isXmlnsPrefix) {
          _contentHandler.startPrefixMapping(prefix, uri);
        }

        // needed for xml/032e
        if (isElement && _isXmlnsAttribute) {
          _attributes.add(entry.getQName(), uri);
        }
      }
      else {
        _attrNames.add(entry);
        _attrValues.add(value);
      }
    }

    int len = _attrNames.size();
    for (int i = 0; i < len; i++) {
      SaxIntern.Entry attrEntry = _attrNames.get(i);
      String value = _attrValues.get(i);

      QName name = attrEntry.getQName();

      _attributes.add(name, value);
    }
    
    return ch;
  }

  /**
   * Parses an entity reference:
   *
   * <pre>
   * er ::= &#d+;
   *    ::= &name;
   * </pre>
   */
  private int parseEntityReference()
    throws IOException, SAXException
  {
    int ch;

    ch = read();

    // character reference
    if (ch == '#') {
      addText((char) parseCharacterReference());

      return read();
    } 
    // entity reference
    else if (XmlChar.isNameStart(ch)) {
      ch = _reader.parseName(_buf, ch);

      if (ch != ';' && _strictXml)
        throw error(L.l("'&{0};' expected ';' at {0}.  Entity references have a '&name;' syntax.", _buf, badChar(ch)));
      else if (ch != ';') {
        addText('&');
        addText(_buf.toString());
        return ch;
      }

      addEntityReference(_buf.toString());

      ch = read();

      return ch;
    } else if (_strictXml) {
      throw error(L.l("expected name at {0}", badChar(ch)));
    } else {
      addText('&');
      return ch;
    }
  }

  private int parseCharacterReference()
    throws IOException, SAXException
  {
    int ch = read();

    int radix = 10;
    if (ch == 'x') {
      radix = 16;
      ch = read();
    }

    int value = 0;
    for (; ch != ';'; ch = read()) {
      if (ch >= '0' && ch <= '9')
        value = radix * value + ch - '0';
      else if (radix == 16 && ch >= 'a' && ch <= 'f')
        value = radix * value + ch - 'a' + 10;
      else if (radix == 16 && ch >= 'A' && ch <= 'F')
        value = radix * value + ch - 'A' + 10;
      else
        throw error(L.l("malformed entity ref at {0}", badChar(ch)));
    }

    if (value > 0xffff)
      throw error(L.l("malformed entity ref at {0}", "" + value));

    // xml/0072
    if (_strictCharacters && ! isChar(value))
      throw error(L.l("illegal character ref at {0}", badChar(value)));

    return value;
  }

  /**
   * Looks up a named entity reference, filling the text.
   */
  private void addEntityReference(String name)
    throws IOException, SAXException
  {
    boolean expand = ! _entitiesAsText || _hasDoctype;
    // XXX: not quite the right logic.  There should be a soft expandEntities

    if (! expand) {
      addText("&" + name + ";");
      return;
    }
    
    int ch = _entities.getEntity(name);
    if (ch >= 0 && ch <= 0xffff) {
      addText((char) ch);
      return;
    }

    QEntity entity = _dtd == null ? null : _dtd.getEntity(name);

    if (! _expandEntities) {
      addText("&" + name + ";");
      return;
    }

    if (entity == null && (_dtd == null || _dtd.getName() == null ||
                           ! _dtd.isExternal())) {
      throw error(L.l("'&{0};' is an unknown entity.  XML predefines only '&lt;', '&amp;', '&gt;', '&apos;' and  '&quot;'. All other entities must be defined in an &lt;!ENTITY> definition in the DTD.", name));
    }
    else if (entity != null) {
      if (entity._isSpecial && entity._value != null)
        addText(entity._value);
      else if (entity.getSystemId() != null) {
        if (pushSystemEntity(entity)) {
        }
        /* XXX:??
        else if (strictXml) {
          throw error(L.l("can't open external entity at '&{0};'", name));
        }
        */
        else if (_contentHandler instanceof DOMBuilder) {
          ((DOMBuilder) _contentHandler).entityReference(name);
        }
        else
          addText("&" + name + ";");
      }
      else if (expand && entity._value != null)
        setMacro(entity._value);
      else
        addText("&" + name + ";");
    }
    else {
      if (_contentHandler instanceof DOMBuilder) {
        ((DOMBuilder) _contentHandler).entityReference(name);
      }
      else // XXX: error?
        addText("&" + name + ";");
    }
  }

  private boolean pushSystemEntity(QEntity entity)
    throws IOException, SAXException
  {
    String publicId = entity.getPublicId();
    String systemId = entity.getSystemId();
    String value = null;
    InputSource source = null;
    ReadStream is = null;

    if (_entityResolver != null)
      source = _entityResolver.resolveEntity(publicId, systemId);

    if (source != null && source.getByteStream() != null)
      is = Vfs.openRead(source.getByteStream());
    else if (source != null && source.getCharacterStream() != null)
      is = Vfs.openRead(source.getCharacterStream());
    else if (source != null && source.getSystemId() != null &&
             _searchPath.lookup(source.getSystemId()).isFile()) {
      _owner.addDepend(_searchPath.lookup(source.getSystemId()));
      is = _searchPath.lookup(source.getSystemId()).openRead();
    }
    else if (systemId != null && ! systemId.equals("")) {
      String path = systemId;
      if (path.startsWith("file:"))
        path = path.substring(5);
      if (_searchPath.lookup(path).isFile()) {
        _owner.addDepend(_searchPath.lookup(path));
        is = _searchPath.lookup(path).openRead();
      }
    }

    if (is == null)
      return false;

    _filename = systemId;
    _systemId = systemId;

    Path oldSearchPath = _searchPath;
    Path path = is.getPath();
    if (path != null) {
      _owner.addDepend(path);
      
      if (_searchPath != null) {
        _searchPath = path.getParent();
        _reader.setSearchPath(oldSearchPath);
      }
    }

    _is = is;
    _line = 1;
    
    XmlReader oldReader = _reader;
    _reader = null;

    parseXMLDeclaration(oldReader);

    return true;
  }

  private boolean isAttributeChar(int ch)
  {
    switch (ch) {
    case ' ': case '\t': case '\n': case '\r':
      return false;
    case '<': case '>': case '\'':case '"': case '=':
      return false;
    default:
      return true;
    }
  }

  private int parsePI()
    throws IOException, SAXException
  {
    int ch;

    ch = read();
    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name after '<?' at {0}.  Processing instructions expect a name like <?foo ... ?>", badChar(ch)));
    ch = _reader.parseName(_text, ch);

    String piName = _text.toString();
    if (! piName.equals("xml"))
      return parsePITail(piName, ch);
    else {
      throw error(L.l("<?xml ... ?> occurs after content.  The <?xml ... ?> prolog must be at the document start."));

    }
  }

  private int parsePITail(String piName, int ch)
    throws IOException, SAXException
  {
    ch = skipWhitespace(ch);

    _text.clear();
    while (ch != -1) {
      if (ch == '?') {
        if ((ch = read()) == '>')
          break;
        else
          _text.append('?');
      } else {
        _text.append((char) ch);
        ch = read();
      }
    }

    _contentHandler.processingInstruction(piName, _text.toString());

    return read();
  }

  /**
   * Parses a comment.  The "&lt;!--" has already been read.
   */
  private void parseComment()
    throws IOException, SAXException
  {
    int ch = read();

    if (ch != '-')
      throw error(L.l("expected comment at {0}", badChar(ch)));

    ch = read();

    if (! _skipComments)
      _buf.clear();

  comment:
    while (ch != -1) {
      if (ch == '-') {
        ch = read();

        while (ch == '-') {
          if ((ch = read()) == '>')
            break comment;
          else if (_strictComments)
            throw error(L.l("XML forbids '--' in comments"));
          else if (ch == '-') {
            if (! _skipComments)
              _buf.append('-');
          }
          else {
            if (! _skipComments)
              _buf.append("--");
            break;
          }
        }

        _buf.append('-');
      } else if (! XmlChar.isChar(ch)) {
        throw error(L.l("bad character {0}", hex(ch)));
      } else {
        _buf.append((char) ch);
        ch = read();
      }
    }

    if (_skipComments) {
    }
    else if (_contentHandler instanceof XMLWriter && ! _skipComments) {
      ((XMLWriter) _contentHandler).comment(_buf.toString());
      _isIgnorableWhitespace = true;
    }
    else if (_lexicalHandler != null) {
      _lexicalHandler.comment(_buf.getBuffer(), 0, _buf.getLength());
      _isIgnorableWhitespace = true;
    }
  }

  /**
   * Parses the contents of a cdata section.
   *
   * <pre>
   * cdata ::= &lt;![CDATA[ ... ]]>
   * </pre>
   */
  private void parseCdata()
    throws IOException, SAXException
  {
    int ch;

    if ((ch = read()) != 'C' ||
        (ch = read()) != 'D' ||
        (ch = read()) != 'A' ||
        (ch = read()) != 'T' ||
        (ch = read()) != 'A' ||
        (ch = read()) != '[') {
      throw error(L.l("expected '<![CDATA[' at {0}", badChar(ch)));
    }

    ch = read();

    if (_lexicalHandler != null) {
      _lexicalHandler.startCDATA();
    }

  cdata:
    while (ch != -1) {
      if (ch == ']') {
        ch = read();

        while (ch == ']') {
          if ((ch = read()) == '>')
            break cdata;
          else if (ch == ']')
            addText(']');
          else {
            addText(']');
            break;
          }
        }

        addText(']');
      } else if (_strictCharacters && ! isChar(ch)) {
        throw error(L.l("expected character in cdata at {0}", badChar(ch)));
      } else {
        addText((char) ch);
        ch = read();
      }
    }
    
    if (_lexicalHandler != null) {
      _lexicalHandler.endCDATA();
    }
  }

  /**
   * Expands the macro value of a PE reference.
   */
  private void addPEReference(CharBuffer value, String name)
    throws IOException, SAXException
  {
    QEntity entity = _dtd.getParameterEntity(name);

    if (entity == null && ! _dtd.isExternal())
      throw error(L.l("'%{0};' is an unknown parameter entity.  Parameter entities must be defined in an <!ENTITY> declaration before use.", name));
    else if (entity != null && entity._value != null) {
      setMacro(entity._value);
    }
    else if (entity != null && entity.getSystemId() != null) {
      pushInclude(entity.getPublicId(), entity.getSystemId());
    }
    else {
      value.append("%");
      value.append(name);
      value.append(";");
    }
  }

  private static String toAttrDefault(CharBuffer text)
  {
    for (int i = 0; i < text.length(); i++) {
      int ch = text.charAt(i);

      if (ch == '"') {
        text.delete(i, i + 1);
        text.insert(i, "&#34;");
        i--;
      } else if (ch == '\'') {
        text.delete(i, i + 1);
        text.insert(i, "&#39;");
        i--;
      }
    }

    return text.toString();
  }

  /**
   * externalID ::= PUBLIC publicId systemId
   *            ::= SYSTEM systemId
   */
  private int parseExternalID(int ch)
    throws IOException, SAXException
  {
    ch = _reader.parseName(_text, ch);
    String key = _text.toString();
    ch = skipWhitespace(ch);

    _extSystemId = null;
    _extPublicId = null;
    if (key.equals("PUBLIC")) {
      _extPublicId = parseValue(ch);
      ch = skipWhitespace(read());

      if (_extPublicId.indexOf('&') > 0)
        throw error(L.l("Illegal character '&' in PUBLIC identifier '{0}'",
                        _extPublicId));

      _extSystemId = parseValue(ch);
      ch = skipWhitespace(read());
    }
    else if (key.equals("SYSTEM")) {
      _extSystemId = parseValue(ch);
      ch = read();
    }
    else
      throw error(L.l("expected PUBLIC or SYSTEM at '{0}'", key));

    return ch;
  }

  /**
   * Parses an attribute value.
   *
   * <pre>
   * value ::= '[^']*'
   *       ::= "[^"]*"
   *       ::= [^ />]*
   * </pre>
   *
   * @param value the CharBuffer which will contain the value.
   * @param ch the next character from the input stream.
   * @param isGeneral true if general entities are allowed.
   *
   * @return the following character from the input stream
   */
  private String parseValue(int ch)
    throws IOException, SAXException
  {
    int end = ch;

    char []valueBuffer = _valueBuffer;
    int valueLength = 0;

    if (end != '\'' && end != '"') {
      valueBuffer[valueLength++] = (char) end;
      for (ch = read();
           ch >= 0 && XmlChar.isNameChar(ch);
           ch = read()) {
        valueBuffer[valueLength++] = (char) ch;
      }
      
      String value = new String(valueBuffer, 0, valueLength);
      
      throw error(L.l("XML attribute value must be quoted at '{0}'.  XML attribute syntax is either attr=\"value\" or attr='value'.",
                      value));
    }
    
    ch = read();

    while (ch >= 0 && ch != end) {
      if (ch == '&') {
        if ((ch = read()) == '#') {
          valueBuffer[valueLength++] = (char) parseCharacterReference();
        }
        else if (XmlChar.isNameStart(ch)) {
          ch = _reader.parseName(_buf, ch);
          String name = _buf.toString();

          if (ch != ';')
            throw error(L.l("expected '{0}' at {1}", ";", badChar(ch)));
          else {
            int lookup = _entities.getEntity(name);

            if (lookup >= 0 && lookup <= 0xffff) {
              ch = read();
              valueBuffer[valueLength++] = (char) lookup;
              continue;
            }
            
            QEntity entity = _dtd == null ? null : _dtd.getEntity(name);
            if (entity != null && entity._value != null)
              setMacroAttr(entity._value);
            else
              throw error(L.l("expected local reference at '&{0};'", name));
          }
        }
      }
      else {
        if (ch == '\r') {
          ch = read();
          if (ch != '\n') {
            valueBuffer[valueLength++] = '\n';
            continue;
          }
        }

        valueBuffer[valueLength++] = (char) ch;
      }

      ch = read();
    }

    return new String(valueBuffer, 0, valueLength);
  }

  private boolean isWhitespace(int ch)
  {
    return ch <= 0x20 && (ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd);
  }

  private boolean isChar(int ch)
  {
    return (ch >= 0x20 && ch <= 0xd7ff ||
            ch == 0x9 ||
            ch == 0xa ||
            ch == 0xd ||
            ch >= 0xe000 && ch <= 0xfffd);
  }

  /**
   * Returns the hex representation of a byte.
   */
  private static String hex(int value)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int b = 3; b >= 0; b--) {
      int v = (value >> (4 * b)) & 0xf;
      if (v < 10)
        cb.append((char) (v + '0'));
      else
        cb.append((char) (v - 10 + 'a'));
    }

    return cb.close();
  }

  /**
   * Returns the current filename.
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Returns the current line.
   */
  public int getLine()
  {
    return _line;
  }
  
  /**
   * Returns the current column.
   */
  int getColumn()
  {
    return -1;
  }

  /**
   * Returns the opening line of the current node.
   */
  int getNodeLine()
  {
    if (_elementTop > 0)
      return _elementLines[_elementTop - 1];
    else
      return 1;
  }

  /**
   * Returns the current public id being read.
   */
  public String getPublicId()
  {
    if (_reader != null)
      return _reader.getPublicId();
    else
      return _publicId;
  }
  
  /**
   * Returns the current system id being read.
   */
  public String getSystemId()
  {
    if (_reader != null)
      return _reader.getSystemId();
    else if (_systemId != null)
      return _systemId;
    else
      return _filename;
  }

  public void setLine(int line)
  {
    _line = line;
  }
  
  public int getLineNumber() { return getLine(); }
  public int getColumnNumber() { return getColumn(); }

  /**
   * Adds a string to the current text buffer.
   */
  private void addText(String s)
    throws IOException, SAXException
  {
    int len = s.length();
    
    for (int i = 0; i < len; i++)
      addText(s.charAt(i));
  }
  
  /**
   * Adds a character to the current text buffer.
   */
  private void addText(char ch)
    throws IOException, SAXException
  {
    if (_textLength > 0 && _textBuffer[_textLength - 1] == '\r') {
      _textBuffer[_textLength - 1] = '\n';
      if (ch == '\n')
        return;
    }
    
    if (_isIgnorableWhitespace && ! XmlChar.isWhitespace(ch))
      _isIgnorableWhitespace = false;
    
    _textBuffer[_textLength++] = ch;
  }

  /**
   * Flushes the text buffer to the SAX callback.
   */
  private void addText(char []buffer, int offset, int length,
                       boolean isWhitespace)
    throws IOException, SAXException
  {
    if (length <= 0)
      return;
    
    if (_namespace.getDepth() == 1) {
      if (! isWhitespace) {
        throw error(L.l("expected top element at '{0}'",
                        new String(buffer, offset, length)));
      }
      else {
        _contentHandler.ignorableWhitespace(buffer, offset, length);
      }
    }
    else
      _contentHandler.characters(buffer, offset, length);
  }

  /**
   * Parses a name.
   */
  private SaxIntern.Entry parseName(int offset, boolean isAttribute)
    throws IOException
  {
    char []inputBuf = _inputBuffer;
    int inputLength = _inputLength;
    int inputOffset = _inputOffset;
    
    char []valueBuf = _valueBuffer;
    int valueLength = offset;

    int colon = 0;

    while (true) {
      if (inputOffset < inputLength) {
        char ch = inputBuf[inputOffset++];

        if (XML_NAME_CHAR[ch]) {
          valueBuf[valueLength++] = ch;
        }
        else if (ch == ':') {
          if (colon <= 0)
            colon = valueLength;

          valueBuf[valueLength++] = ch;
        }
        else {
          _inputOffset = inputOffset - 1;

          return _intern.add(valueBuf, offset, valueLength - offset,
                             colon, isAttribute);
        }
      }
      else if (fillBuffer()) {
        inputLength = _inputLength;
        inputOffset = 0;
      }
      else {
        return _intern.add(valueBuf, offset, valueLength - offset,
                           colon, isAttribute);
      }
    }
  }
  
  final int skipWhitespace(int ch)
    throws IOException, SAXException
  {
    while (ch <= 0x20 && (ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd)) {
      ch = read();
    }

    return ch;
  }


  public void setReader(XmlReader reader)
  {
    _reader = reader;
  }

  /**
   * Adds text to the macro, escaping attribute values.
   */
  void setMacroAttr(String text)
    throws IOException, SAXException
  {
    if (_reader != _macro) {
      _macro.init(this, _reader);
      _reader = _macro;
    }

    int j = _macroIndex;
    for (int i = 0; i < text.length(); i++) {
      int ch = text.charAt(i);

      if (ch == '\'')
        _macro.add("&#39;");
      else if (ch == '"')
        _macro.add("&#34;");
      else
        _macro.add((char) ch);
    }
  }

  void pushInclude(String systemId)
    throws IOException, SAXException
  {
    pushInclude(null, systemId);
  }
  /**
   * Pushes the named file as a lexical include.
   *
   * @param systemId the name of the file to include.
   */
  void pushInclude(String publicId, String systemId)
    throws IOException, SAXException
  {
    InputStream stream = openStream(systemId, publicId);
    if (stream == null)
      throw new FileNotFoundException(systemId);
    _is = Vfs.openRead(stream);
    Path oldSearchPath = _searchPath;
    Path path = _is.getPath();
    if (path != null) {
      _owner.addDepend(path);
      
      if (_searchPath != null) {
        _searchPath = path.getParent();
        _reader.setSearchPath(oldSearchPath);
      }
    }

    _filename = systemId;
    /*
    XmlReader nextReader;
    if (_reader instanceof Utf8Reader)
      nextReader = new Utf8Reader(this, _is);
    else {
      _is.setEncoding(_reader.getReadStream().getEncoding());
      nextReader = new XmlReader(this, _is);
    }
    _reader = nextReader;
    */

    XmlReader oldReader = _reader;
    _reader = null;
    
    _line = 1;
    parseXMLDeclaration(oldReader);
    int ch = read();

    XmlReader reader = _reader;

    if (reader instanceof MacroReader)
      reader = reader.getNext();
    
    reader.setSystemId(systemId);
    reader.setFilename(systemId);
    reader.setPublicId(publicId);
    reader.setNext(oldReader);

    unread(ch);
  }

  private void popInclude()
    throws IOException, SAXException
  {
    XmlReader oldReader = _reader;
    _reader = _reader.getNext();
    oldReader.setNext(null);
    _filename = _reader.getFilename();
    _line = _reader.getLine();
    _is = _reader.getReadStream();
    if (_reader.getSearchPath() != null)
      _searchPath = _reader.getSearchPath();
  }

  void setMacro(String text)
    throws IOException, SAXException
  {
    if (_reader == _macro) {
    }
    else if (_macro.getNext() == null) {
      _macro.init(this, _reader);
      _reader = _macro;
    }
    else {
      _macro = new MacroReader();
      _macro.init(this, _reader);
      _reader = _macro;
    }
    
    _macro.add(text);
  }

  protected final int read()
    throws IOException, SAXException
  {
    int inputOffset = _inputOffset;
    
    if (inputOffset < _inputLength) {
      char ch = _inputBuffer[inputOffset];

      _inputOffset = inputOffset + 1;
      
      return ch;
    }
    else if (fillBuffer()) {
      return _inputBuffer[_inputOffset++];
    }
    else
      return -1;
  }

  public final void unread(int ch)
  {
    if (ch < 0 || _inputOffset <= 0)
      return;

    _inputOffset--;
  }
    
  protected boolean fillBuffer()
    throws IOException
  {
    int len = _is.read(_inputBuffer, 0, _inputBuffer.length);

    if (len >= 0) {
      _inputLength = len;
      _inputOffset = 0;
      
      return true;
    }
    else {
      _inputLength = 0;
      _inputOffset = 0;

      return false;
    }
  }

  private void parseXMLDeclaration(XmlReader oldReader)
    throws IOException, SAXException
  {
    int startOffset = _is.getOffset();
    boolean isEBCDIC = false;
    int ch = _is.read();

    XmlReader reader = null;
    
    // utf-16 starts with \xfe \xff
    if (ch == 0xfe) {
      ch = _is.read();
      if (ch == 0xff) {
        _owner.setAttribute("encoding", "UTF-16");
        _is.setEncoding("utf-16");

        reader = new Utf16Reader(this, _is);
        
        ch = reader.read();
      }
    }
    // utf-16 rev starts with \xff \xfe
    else if (ch == 0xff) {
      ch = _is.read();
      if (ch == 0xfe) {
        _owner.setAttribute("encoding", "UTF-16");
        _is.setEncoding("utf-16");

        reader = new Utf16Reader(this, _is);
        ((Utf16Reader) reader).setReverse(true);
        
        ch = reader.read();
      }
    }
    // utf-16 can also start with \x00 <
    else if (ch == 0x00) {
      ch = _is.read();
      _owner.setAttribute("encoding", "UTF-16");
      _is.setEncoding("utf-16");
      
      reader = new Utf16Reader(this, _is);
    }
    // utf-8 BOM is \xef \xbb \xbf
    else if (ch == 0xef) {
      ch = _is.read();
      if (ch == 0xbb) {
        ch = _is.read();

        if (ch == 0xbf) {
          ch = _is.read();

          _owner.setAttribute("encoding", "UTF-8");
          _is.setEncoding("utf-8");
      
          reader = new Utf8Reader(this, _is);
        }
      }
    }
    else if (ch == 0x4c) {
      // ebcdic
      // xml/00l1
      _is.unread();
      // _is.setEncoding("cp037");
      _is.setEncoding("cp500");

      isEBCDIC = true;

      reader = new XmlReader(this, _is);

      ch = reader.read();
    }
    else {
      int ch2 = _is.read();

      if (ch2 == 0x00) {
        _owner.setAttribute("encoding", "UTF-16LE");
        _is.setEncoding("utf-16le");

        reader = new Utf16Reader(this, _is);
        ((Utf16Reader) reader).setReverse(true);
      }
      else if (ch2 > 0)
        _is.unread();
    }

    if (reader != null && reader != oldReader) {
    }
    else if (_is.getSource() instanceof ReaderWriterStream) {
      reader = new XmlReader(this, _is);
    }
    else {
      reader = new Utf8Reader(this, _is);
    }

    if (ch == '\n')
      reader.setLine(2);

    reader.setSystemId(_systemId);
    if (_systemId == null)
      reader.setSystemId(_filename);
    reader.setFilename(_filename);
    reader.setPublicId(_publicId);

    reader.setNext(oldReader);

    _reader = reader;

    /* XXX: this might be too strict. */
    /*
    if (! strictXml) {
      for (; XmlChar.isWhitespace(ch); ch = reader.read()) {
      }
    }
    */

    if (ch != '<') {
      unreadByte(ch);
      return;
    }

    if (parseXMLDecl(_reader) && isEBCDIC) {
      // EBCDIC requires a re-read
      _is.setOffset(startOffset);

      ch = read();
      if (ch != '<')
        throw new IllegalStateException();
      
      parseXMLDecl(_reader);
    }
  }

  private boolean parseXMLDecl(XmlReader reader)
    throws IOException, SAXException
  {
    int ch = readByte();
    if (ch != '?') {
      unreadByte((char) ch);
      unreadByte('<');
      return false;
    }

    ch = read();
    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name after '<?' at {0}.  Processing instructions expect a name like <?foo ... ?>", badChar(ch)));
    ch = _reader.parseName(_text, ch);

    String piName = _text.toString();
    if (! piName.equals("xml")) {
      ch = parsePITail(piName, ch);
      unreadByte(ch);
      return false;
    }
          
    ch = parseAttributes(ch, false);
      
    if (ch != '?')
      throw error(L.l("expected '?' at {0}.  Processing instructions end with '?>' like <?foo ... ?>", badChar(ch)));
    if ((ch = read()) != '>')
      throw error(L.l("expected '>' at {0}.  Processing instructions end with '?>' like <?foo ... ?>", ">", badChar(ch)));

    for (int i = 0; i < _attributes.getLength(); i++) {
      QName name = _attributes.getName(i);
      String value = _attributes.getValue(i);

      if (_owner != null)
        _owner.setAttribute(name.getLocalPart(), value);

      if (name.getLocalPart().equals("encoding")) { // xml/00hb // && ! _inDtd) {
        String encoding = value;

        if (! _isStaticEncoding &&
            ! encoding.equalsIgnoreCase("UTF-8") &&
            ! encoding.equalsIgnoreCase("UTF-16") &&
            ! (_is.getSource() instanceof ReaderWriterStream)) {
          _is.setEncoding(encoding);

          XmlReader oldReader = _reader;

          _reader = new XmlReader(this, _is);
          // _reader.setNext(oldReader);

          _reader.setLine(oldReader.getLine());

          _reader.setSystemId(_filename);
          _reader.setPublicId(null);
        }
      }
    }

    return true;
  }

  protected int readByte()
    throws IOException
  {
    return _is.read();
  }

  protected void unreadByte(int ch)
  {
    _is.unread();
  }

  /**
   * Returns an error including the current line.
   *
   * @param text the error message text.
   */
  XmlParseException error(String text)
  {
    if (_errorHandler != null) {
      SAXParseException e = new SAXParseException(text, _locator);

      try {
        _errorHandler.fatalError(e);
      } catch (SAXException e1) {
      }
    }
    
    return new XmlParseException(_filename + ":" + _line + ": " + text);
  }

  public void free()
  {
  }

  int parseName(CharBuffer cb, int ch)
    throws IOException, SAXException
  {
    return _reader.parseName(cb, ch);
  }

  /**
   * Returns a user-readable string for an error character.
   */
  static String badChar(int ch)
  {
    if (ch < 0 || ch == 0xffff)
      return L.l("end of file");
    else if (ch == '\n' || ch == '\r')
      return L.l("end of line");
    else if (ch >= 0x20 && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else
      return "'" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  private void printDebugNode(WriteStream s, Node node, int depth)
    throws IOException
  {
    if (node == null)
      return;

    for (int i = 0; i < depth; i++)
      s.print(' ');

    if (node.getFirstChild() != null) {
      s.println("<" + node.getNodeName() + ">");
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        printDebugNode(s, child, depth + 2);
      }
      for (int i = 0; i < depth; i++)
        s.print(' ');
      s.println("</" + node.getNodeName() + ">");
    }
    else
      s.println("<" + node.getNodeName() + "/>");
  }

  public void close()
  {
    TempCharBuffer tempInputBuffer = _tempInputBuffer;
    _tempInputBuffer = null;

    _inputBuffer = null;

    if (tempInputBuffer != null)
      TempCharBuffer.free(tempInputBuffer);
  }

  public static class LocatorImpl implements ExtendedLocator {
    XmlParser _parser;

    LocatorImpl(XmlParser parser)
    {
      _parser = parser;
    }
    
    public String getSystemId()
    {
      if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else
        return null;
    }
    
    public String getFilename()
    {
      if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else
        return null;
    }
    
    public String getPublicId()
    {
      if (_parser._reader != null)
        return _parser._reader.getPublicId();
      else
        return _parser.getPublicId();
    }

    public int getLineNumber()
    {
      if (_parser._reader != null)
        return _parser._reader.getLine();
      else
        return _parser.getLineNumber();
    }

    public int getColumnNumber()
    {
      return _parser.getColumnNumber();
    }
  }

  static {
    XML_NAME_CHAR = new boolean[65536];

    for (int i = 0; i < 65536; i++) {
      XML_NAME_CHAR[i] = XmlChar.isNameChar(i) && i != ':';
    }
  }
}
