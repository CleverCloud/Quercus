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
 * @author Scott Ferguson, Adam Megacz
 */

package com.caucho.xml.stream;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Logger;

/**
 * XML pull-parser interface.
 */
public class XMLStreamReaderImpl implements XMLStreamReader {
  private static final Logger log
    = Logger.getLogger(XMLStreamReaderImpl.class.getName());
  private static final L10N L = new L10N(XMLStreamReaderImpl.class);

  private static final boolean []IS_XML_NAME = new boolean[65536];

  private StaxIntern _intern;

  private ReadStream _is;
  private Reader _reader;

  private int _lastCol = 1;
  private int _col = 1;
  private int _row = 1;
  private int _offset = 1;

  private NamespaceReaderContext _namespaceTracker;

  private String _version = "1.0";
  private String _encoding = "utf-8";
  private String _encodingScheme;

  private String _publicId;
  private String _systemId;

  private boolean _seenDocumentStart = false;
  private int _current;
  private int _state;
  private boolean _isShortTag;
  private boolean _isWhitespace = false;

  private boolean _eofEncountered = false;

  private String _processingInstructionTarget;
  private String _processingInstructionData;

  private RawName _rawTagName = new RawName();
  private QName _name;

  private StaxIntern.Entry []_attrRawNames = new StaxIntern.Entry[16];
  private QName []_attrNames = new QName[16];
  private String []_attrValues = new String[16];
  private int _attrCount;
  private final StringBuilder _sb = new StringBuilder();
  
  private TempCharBuffer _tempInputBuffer;
  private char []_inputBuf;
  private int _inputOffset;
  private int _inputLength;

  private TempCharBuffer _tempCharBuffer;
  private char []_cBuf;
  private int _cBufLength;

  public XMLStreamReaderImpl(InputStream is)
    throws XMLStreamException
  {
    this(Vfs.openRead(is));
  }

  public XMLStreamReaderImpl(Reader r)
    throws XMLStreamException
  {
    _reader = r;
    init();
  }

  public XMLStreamReaderImpl(InputStream is, String systemId)
    throws XMLStreamException
  {
    this(Vfs.openRead(is));
    
    _systemId = systemId;
  }

  public XMLStreamReaderImpl(Reader reader, String systemId)
    throws XMLStreamException
  {
    this(reader);
    
    _systemId = systemId;
  }

  public XMLStreamReaderImpl(ReadStream is)
    throws XMLStreamException
  {
    _is = is;
    
    init();
  }

  public void init()
    throws XMLStreamException
  {
    _namespaceTracker = new NamespaceReaderContext();
    _intern = new StaxIntern(_namespaceTracker);
    
    _tempCharBuffer = TempCharBuffer.allocate();
    _cBuf = _tempCharBuffer.getBuffer();

    _tempInputBuffer = TempCharBuffer.allocate();
    _inputBuf = _tempInputBuffer.getBuffer();
    _inputOffset = _inputLength = 0;

    readHeader();

    _current = START_DOCUMENT;
  }
  
  public int available()
  {
    return _inputLength - _inputOffset;
  }

  public int getAttributeCount()
  {
    return _attrCount;
  }

  public String getAttributeLocalName(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
                                                                                                                                        _attrCount, index));

    return _attrNames[index].getLocalPart();
  }

  public QName getAttributeName(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
                                             _attrCount, index));

    return _attrNames[index];
  }

  public String getAttributeNamespace(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
                                             _attrCount, index));

    String ret = _attrNames[index].getNamespaceURI();

    // API quirk
    if ("".equals(ret))
      return null;

    return ret;
  }

  public String getAttributePrefix(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
                                             _attrCount, index));

    String ret = _attrNames[index].getPrefix();

    return ret;
  }

  public String getAttributeType(int index)
  {
    return "CDATA";
  }

  public String getAttributeValue(int index)
  {
    if (_attrCount <= index)
      throw new IllegalArgumentException(L.l("element only has {0} attributes, given index {1}",
                                             _attrCount, index));

    return _attrValues[index];
  }

  public boolean isAttributeSpecified(int index)
  {
    return index < _attrCount;
  }

  public String getAttributeValue(String namespaceURI, String localName)
  {
    for (int i = _attrCount - 1; i >= 0; i--) {
      QName name = _attrNames[i];

      // namespaceURI == null means ignore namespace
      if (namespaceURI == null) {
        if (name.getLocalPart().equals(localName)) 
          return _attrValues[i];
      }
      else if (name.getLocalPart().equals(localName)
               && name.getNamespaceURI().equals(namespaceURI))
        return _attrValues[i];
    }

    return null;
  }

  public String getCharacterEncodingScheme()
  {
    return _encodingScheme;
  }

  public String getElementText() throws XMLStreamException
  {
    if (_current != START_ELEMENT)
      throw new XMLStreamException(L.l("START_ELEMENT expected when calling getElementText()"));

    StringBuilder sb = new StringBuilder();

    for (int eventType = next();
         eventType != END_ELEMENT;
         eventType = next()) {
      switch (eventType) {
        case CHARACTERS:
        case CDATA:
        case SPACE:
        case ENTITY_REFERENCE:
          sb.append(_cBuf, 0, _cBufLength);
          break;

        case PROCESSING_INSTRUCTION:
        case COMMENT:
          break;

        case END_DOCUMENT:
          throw new XMLStreamException(L.l("Document ended unexpectedly while reading element text"));

        case START_ELEMENT:
          throw new XMLStreamException(L.l("getElementText() encountered a START_ELEMENT; text only element expected"));

        default:
          throw new XMLStreamException(L.l("Unexpected event during getElementText(): {0}", eventType));
      }
    }

    return sb.toString();
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public int getEventType()
  {
    return _current;
  }

  public Location getLocation()
  {
    return new StreamReaderLocation(_offset, _row, _col);
  }

  public String getLocalName()
  {
    if (_name == null)
      throw new IllegalStateException();

    return _name.getLocalPart();
  }

  public String getNamespaceURI()
  {
    if (_name == null)
      return null;

    String uri = _name.getNamespaceURI();

    if ("".equals(uri))
      return null;
    else
      // .intern() for WSS4J compatibility
      // xml/3028
      return uri.intern();
  }

  public QName getName()
  {
    return _name;
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
    if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))
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
    if (_current != PROCESSING_INSTRUCTION)
      return null;

    return _processingInstructionData;
  }

  public String getPITarget()
  {
    if (_current != PROCESSING_INSTRUCTION)
      return null;

    return _processingInstructionTarget;
  }

  public String getPrefix()
  {
    if (_name == null)
      return null;

    String prefix = _name.getPrefix();

    // xml/3000, xml/3009
    if ("" == prefix && "" == _name.getNamespaceURI())
      return null;

    return prefix;
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
    return new String(_cBuf, 0, _cBufLength);
  }

  /**
   * Returns a character buffer for the current text.
   */
  public char[] getTextCharacters()
  {
    return _cBuf;
  }

  /**
   * Reads the current text into a buffer.
   */
  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    int sublen = _cBufLength - sourceStart;
    
    if (length < sublen)
      sublen = length;
    
    System.arraycopy(_cBuf, sourceStart, target, targetStart, sublen);
    
    return sublen;
  }

  /**
   * Returns the length of the current text.
   */
  public int getTextLength()
  {
    return _cBufLength;
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
    return _name != null;
  }

  public boolean hasText()
  {
    switch(getEventType()) {
    case CHARACTERS:
    case DTD:
    case ENTITY_REFERENCE:
    case COMMENT:
    case SPACE:
      return true;
    default:
      return false;
    }
  }

  public boolean isCharacters()
  {
    return _current == CHARACTERS;
  }

  public boolean isEndElement()
  {
    if (_current == END_ELEMENT)
      return true;

    // php/4618
    if (_current == START_ELEMENT && _isShortTag)
      return true;

    return false;
  }

  public boolean isStandalone()
  {
    return false;
  }

  public boolean isStartElement()
  {
    return _current == START_ELEMENT;
  }

  public boolean isWhiteSpace()
  {
    return (_isWhitespace
            && (_current == CHARACTERS || _current == SPACE));
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
    if (type != _current) {
      StringBuilder sb = new StringBuilder();
      sb.append("expected ");
      sb.append(StaxUtil.constantToString(type));

      if (type == START_ELEMENT || type == END_ELEMENT) {
        sb.append('(');
        sb.append(localName);
        sb.append(')');
      }

      sb.append(", found ");
      sb.append(StaxUtil.constantToString(_current));

      if (_current == START_ELEMENT || _current == END_ELEMENT) {
        sb.append('(');
        sb.append(getLocalName());
        sb.append(')');
      }

      sb.append(" at ");
      sb.append(getLocation());

      throw new XMLStreamException(sb.toString());
    }

    if (localName != null && !localName.equals(getLocalName())) {
      if (type == START_ELEMENT) {
        throw new XMLStreamException("expected <" + localName + ">, found " +
                                     "<" + getLocalName() + "> at " +
                                     getLocation());
      }
      else if (type == END_ELEMENT) {
        throw new XMLStreamException("expected </" + localName + ">, found " +
                                     "</" + getLocalName() + "> at " +
                                     getLocation());
      }
    }

    if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI()))
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
    if (_is == null && _reader == null)
      return false;

    return _current != END_DOCUMENT;
  }

  public int next() throws XMLStreamException
  {
    try {
      _current = readNext();
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }

    if (_current > 0)
      return _current;
    else {
      if (_eofEncountered) {
        _current = -1;
      }
      else {
        _eofEncountered = true;
      
        _current = END_DOCUMENT;
      }

      return _current;
    }
  }

  private int readNext()
    throws IOException, XMLStreamException
  {
    _cBufLength = 0;

    // we pop the namespace context when the user is finished
    // working with the END_ELEMENT event
    if (_current == END_ELEMENT)
      _namespaceTracker.pop();
    
    if (_isShortTag) {
      _isShortTag = false;
      return END_ELEMENT;
    }

    _name = null;

    int ch = read();

    if (ch == '<') {
      ch = read();

      switch (ch) {
      case '/':
        _name = readName(false).getQName();

        expect('>');

        return END_ELEMENT;

      case '!':
        expect('-');
        expect('-');
        return readComment();

      case '?':
        readProcessingDirective();
        return PROCESSING_INSTRUCTION;

      case -1:
        close();
        return -1;

      default:
        unread();

        readElementBegin();

        return START_ELEMENT;
      }
    }
    else if (ch < 0) {
      close();
      
      return -1;
    }
    else {
      unread();

      return readData();
    }
  }

  private void readElementBegin()
    throws IOException, XMLStreamException
  {
    _namespaceTracker.push();

    StaxIntern.Entry eltName = readName(false);

    _isShortTag = false;

    int ch = readAttributes();

    if (ch == '>') {
    }
    else if (ch == '/') {
      _isShortTag = true;

      expect('>');
    }
    else if (ch < 0) {
      // #2989, xml/3033
      close();
      return;
    }
    else
      throw error(L.l("Expected {0} at {1}", ">", charName(ch)));

    for (int i = _attrCount - 1; i >= 0; i--)
      _attrNames[i] = _attrRawNames[i].getQName();

    _name = eltName.getQName();
  }

  private int readAttributes()
    throws IOException, XMLStreamException
  {
    int ch;
    int attrCount = 0;

    while ((ch = skipWhitespace()) >= 0 && IS_XML_NAME[ch]) {
      unread();

      if (_attrRawNames.length <= attrCount)
        extendAttrs();

      StaxIntern.Entry rawName = readName(true);

      ch = skipWhitespace();

      if (ch != '=')
        throw error(L.l("attribute expects '=' at {0}", charName(ch)));

      ch = skipWhitespace();

      if (ch == '\'' || ch == '"') {
        if ("xmlns".equals(rawName.getPrefix())) {
          _namespaceTracker.declare(rawName.getLocalName(), readValue(ch));
        }
        else if ("xmlns".equals(rawName.getLocalName())) {
          _namespaceTracker.declare(XMLConstants.DEFAULT_NS_PREFIX, 
                                    readValue(ch));
        }
        else {
          _attrRawNames[attrCount] = rawName;
          _attrValues[attrCount++] = readValue(ch);
        }
      }
      else
        throw error(L.l("attribute expects value at {0}", charName(ch)));
    }

    _attrCount = attrCount;

    return ch;
  }

  private String readValue(int end)
    throws XMLStreamException
  {
    char []valueBuffer = _cBuf;
    int valueIndex = 0;

    while (true) {
      int ch = read();

      switch (ch) {
        case -1:
          return new String(valueBuffer, 0, valueIndex);

        case '"': case '\'':
          if (ch == end)
            return new String(valueBuffer, 0, valueIndex);
          else
            valueBuffer[valueIndex++] = (char) ch;
          break;

        case '&':
          valueBuffer[valueIndex++] = (char) ch;
          break;

        default:
          valueBuffer[valueIndex++] = (char) ch;
          break;
      }
    }
  }

  private void extendAttrs()
  {
    int length = _attrRawNames.length;

    StaxIntern.Entry []attrRawNames = new StaxIntern.Entry[length + 16];
    System.arraycopy(_attrRawNames, 0, attrRawNames, 0, length);
    _attrRawNames = attrRawNames;

    QName []attrNames = new QName[length + 16];
    System.arraycopy(_attrNames, 0, attrNames, 0, length);
    _attrNames = attrNames;

    String []attrValues = new String[length + 16];
    System.arraycopy(_attrValues, 0, attrValues, 0, length);
    _attrValues = attrValues;
  }

  private int readData()
    throws IOException, XMLStreamException
  {
    int ch = 0;
    _isWhitespace = true;

    int index = 0;
    char []cBuf = _cBuf;
    int length = cBuf.length;
    int entity = -1;
    
    loop:
    for (; index < length && (ch = read()) >= 0; index++) {
      switch (ch) {
      case '<':
        unread();
        break loop;

      case '&':
        if (cBuf.length <= index + 256) {
          unread();
          break loop;
        }
        cBuf[index] = (char) ch;
        entity = index;
        break;

      case '\r':
        ch = read();
        if (ch != '\n') { ch = '\r'; unread(); }

      case ' ': case '\t': case '\n':
        cBuf[index] = (char) ch;
        break;

      case ';':
        if (entity >= 0) {
          String unresolved = new String(cBuf, entity + 1, index - entity - 1);
          String resolved = resolveEntity(unresolved);
          // the loop will advance index + 1
          index = entity + resolved.length() - 1;
          resolved.getChars(0, resolved.length(), cBuf, entity);
          entity = -1;
          break;
        }
      default:
        _isWhitespace = false;
        cBuf[index] = (char) ch;
        break;
      }
    }

    if (entity > 0)
      throw new XMLStreamException("XXX: unclosed entity at end of file");

    _cBufLength = index;

    if (ch < 0 && _isWhitespace)
      return -1;

    // whitespace surrounding the root element is "ignorable" per the XML spec
    boolean isIgnorableWhitespace
      = _isWhitespace && _namespaceTracker.getDepth() == 0;

    return isIgnorableWhitespace ? SPACE : CHARACTERS;
  }

  private String resolveEntity(String s)
    throws XMLStreamException
  {
    if ("amp".equals(s))    return "&";
    if ("apos".equals(s))   return "\'";
    if ("quot".equals(s))   return "\"";
    if ("lt".equals(s))     return "<";
    if ("gt".equals(s))     return ">";
    if (s.startsWith("#x"))
      return ""+((char)Integer.parseInt(s.substring(2), 16));
    if (s.startsWith("#"))
      return ""+((char)Integer.parseInt(s.substring(1)));

    throw new XMLStreamException("unknown entity: \"" + s + "\"");
  }

  private void readProcessingDirective()
    throws XMLStreamException
  {
    CharBuffer target = new CharBuffer();
    CharBuffer data   = null;

    while(true) {
      int ch = read();

      if (ch == -1)
        return;  /* XXX: error? */

      if (ch == '?') {
        int next = read();
        if (next == '>') {
          _processingInstructionTarget = target.toString();
          _processingInstructionData = data == null ? null : data.toString();
          return;
        }
        unread();
      }

      if (data == null && (ch == ' ' || ch == '\r' || ch == '\n')) {
        data = new CharBuffer();
        continue;
      }

      if (data != null)
        data.append((char)ch);
      else
        target.append((char)ch);
    }
  }


  private int readComment()
    throws XMLStreamException
  {
    int ch = 0;
    int index = 0;
    char []cBuf = _cBuf;
    int length = cBuf.length;
    loop:
    for (; index < length && (ch = read()) >= 0; index++) {
      cBuf[index] = (char) ch;
      if (index > 3
          && cBuf[index-2] == '-'
          && cBuf[index-1] == '-'
          && cBuf[index-0] == '>') {
        index -= 2;
        break;
      }
    }

    _cBufLength = index;

    return COMMENT;
  }

  private void readRawName(RawName name)
    throws IOException, XMLStreamException
  {
    int length = 0;
    char []nameBuffer = name._buffer;
    int bufferLength = nameBuffer.length;
    int prefix = -1;

    int ch;

    while ((ch = read()) >= 0 && IS_XML_NAME[ch]) {
      if (bufferLength <= length) {
        name.expandCapacity();
        nameBuffer = name._buffer;
        bufferLength = nameBuffer.length;
      }

      if (ch == ':' && prefix < 0)
        prefix = length;

      nameBuffer[length++] = (char) ch;
    }
    unread();

    name._length = length;
    name._prefix = prefix;
  }

  /**
   * Parses a name.
   */
  private StaxIntern.Entry readName(boolean isAttribute)
    throws XMLStreamException
  {
    char []inputBuf = _inputBuf;
    int inputLength = _inputLength;
    int inputOffset = _inputOffset;
    
    char []valueBuf = _cBuf;
    int valueOffset = 0;

    int colon = 0;

    while (true) {
      if (inputOffset < inputLength) {
        char ch = inputBuf[inputOffset++];

        if (IS_XML_NAME[ch]) {
          valueBuf[valueOffset++] = ch;
        }
        else if (ch == ':') {
          if (colon <= 0)
            colon = valueOffset;

          valueBuf[valueOffset++] = ch;
        }
        else {
          _inputOffset = inputOffset - 1;

          return _intern.add(valueBuf, 0, valueOffset, colon, isAttribute);
        }
      }
      else if (fillBuffer()) {
        inputLength = _inputLength;
        inputOffset = _inputOffset;
      }
      else {
        return _intern.add(valueBuf, 0, valueOffset, colon, isAttribute);
      }
    }
  }

  private static boolean isXmlName(int ch)
  {
    return ('a' <= ch && ch <= 'z'
            || 'A' <= ch && ch <= 'Z'
            || '0' <= ch && ch <= '9'
            || ch == ':'
            || ch == '+'
            || ch == '_'
            || ch == '-');
  }

  private int skipWhitespace()
    throws XMLStreamException
  {
    int ch;

    while ((ch = read()) == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
    }

    return ch;
  }

  /**
   * Reads the <?xml ... ?> declaraction
   */
  private void readHeader()
    throws XMLStreamException
  {
    // The reading at this point must use the underlying stream because
    // the encoding is not determined until the end of the declaration

    int ch;

    ch = readByte();

    if (ch == (char)0xFE) {
      if (readByte() != (char)0xFF)
        throw new XMLStreamException("found unrecognized BOM");
      
      ch = readByte();
    } 
    else if (ch == (char)0xFF) {
      if (readByte() != (char)0xFE)
        throw new UnsupportedOperationException("found byte-swapped BOM");
      else
        throw new XMLStreamException("found unrecognized BOM");
    }

    if (ch != '<') {
      unreadByte();
    }
    else if ((ch = readByte()) != '?') {
      unreadByte();
      unreadByte();
    }
    else if ((ch = readByte()) != 'x') {
      unreadByte();
      unreadByte();
      unreadByte();
    }
    else if ((ch = readByte()) != 'm') {
      unreadByte();
      unreadByte();
      unreadByte();
      unreadByte();
    }
    else if ((ch = readByte()) != 'l') {
      unreadByte();
      unreadByte();
      unreadByte();
      unreadByte();
      unreadByte();
    }
    else {
      CharBuffer directive = new CharBuffer();

      while ((ch = readByte()) >= 0 && ch != '?')
        directive.append((char)ch);

      String data = directive.toString().trim();

      if (data.startsWith("version")) {
        data = data.substring(7).trim();
        data = data.substring(1).trim();  // remove "="
        char quot = data.charAt(0);
        _version = data.substring(1, data.indexOf(quot, 1));
        data = data.substring(data.indexOf(quot, 1)+1).trim();
      }

      if (data.startsWith("encoding")) {
        data = data.substring(8).trim();
        data = data.substring(1).trim();  // remove "="
        char quot = data.charAt(0);
        _encoding = data.substring(1, data.indexOf(quot, 1));
        data = data.substring(data.indexOf(quot, 1)+1).trim();

        try {
          if (_is != null)
            _is.setEncoding(_encoding);
        }
        catch (java.io.UnsupportedEncodingException e) {
          throw new XMLStreamException(e);
        }
      }

      ch = readByte();

      if (ch != '>')
        throw error(L.l("Expected '>' at end of '<?xml' declaration at {0}",
                        charName(ch)));
    }

    skipWhitespace();
    unread();
  }

  /**
   * Reads and validate next character.
   */
  private void expect(int expect)
    throws XMLStreamException
  {
    int ch = read();

    if (ch != expect)
      throw error(L.l("expected {0} at {1}", charName(expect), charName(ch)));
  }

  /**
   * Reads a character.
   */
  private int read()
    throws XMLStreamException
  {
    if (_inputLength <= _inputOffset && ! fillBuffer())
      return -1;

    int ch = _inputBuf[_inputOffset++];

    _offset++;

    // XXX '\r'
    if (ch == '\n') {
      _row++;
      _lastCol = _col;
      _col = 1;
    }
    else
      _col++;

    return ch;
  }

  /**
   * Reads a character.
   */
  private void unread()
  {
    if (_inputOffset > 0) {
      _inputOffset--;
      _offset--;

      if (_col > 1)
        _col--;

      if (_inputBuf[_inputOffset] == '\n') {
        _row--;
        _col = _lastCol;
      }
    }
  }

  /**
   * Reads a character.
   */
  private int readByte()
    throws XMLStreamException
  {
    try {
      if (_inputLength <= _inputOffset) {
        int ch = -1;
        
        if (_is != null)
          ch = _is.read();
        else if (_reader != null)
          ch = _reader.read();

        if (ch < 0)
          return ch;

        if (_inputBuf.length <= _inputLength)
          _inputLength = 0;

        _inputBuf[_inputLength++] = (char) ch;
        _inputOffset = _inputLength;

        _offset++;

        // XXX '\r'
        if (ch == '\n') {
          _row++;
          _col = 1;
        }
        else
          _col++;

        return ch;
      }
      else
        return _inputBuf[_inputOffset++];
    } 
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  /**
   * Unreads a byte.
   */
  private void unreadByte()
  {
    if (_inputOffset > 0)
      _inputOffset--;
  }

  /**
   * Fills the input buffer.
   */
  private final boolean fillBuffer()
    throws XMLStreamException
  {
    try {
      if (_is != null) {
        _inputOffset = 0;

        _inputLength = _is.read(_inputBuf, 0, _inputBuf.length);

        return _inputLength > 0;
      }
      else if (_reader != null) {
        _inputOffset = 0;
        _inputLength = _reader.read(_inputBuf, 0, _inputBuf.length);

        return _inputLength > 0;
      }
      else {
        _inputOffset = 0;
        _inputLength = 0;

        return false;
      }
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private String charName(int ch)
  {
    if (ch < 0)
      return "end of file";
    else if (ch > 0x20 && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else
      return "0x" + Integer.toHexString(ch);
  }

  private XMLStreamException error(String s)
  {
    return new XMLStreamException(location() + s);
  }

  private String location()
  {
    return ":" + _row + ":" + _col + " ";
  }

  public void close() throws XMLStreamException
  {
    TempCharBuffer tempCharBuffer = _tempCharBuffer;
    _tempCharBuffer = null;
    _cBuf = null;
      
    TempCharBuffer tempInputBuffer = _tempInputBuffer;
    _tempInputBuffer = null;
    _inputBuf = null;

    _inputOffset = _inputLength = 0;
      
    if (tempCharBuffer != null)
      TempCharBuffer.free(tempCharBuffer);
      
    if (tempInputBuffer != null)
      TempCharBuffer.free(tempInputBuffer);
      
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();

    Reader r = _reader;
    _reader = null;

    if (r != null) {
      try {
        r.close();
      }
      catch (IOException e) {
        throw new XMLStreamException(e);
      }
    }
  }

  static class RawName {
    char []_buffer = new char[64];
    int _prefix;
    int _length;

    public QName resolve(NamespaceContext nsc)
    {
      if (getPrefix() == null)
        return new QName(nsc.getNamespaceURI(null),
                         getLocalName());
      return new QName(nsc.getNamespaceURI(getPrefix()),
                       getLocalName(),
                       getPrefix());
    }

    public String toString()
    {
      return new String(_buffer, 0, _length);
    }

    String getLocalName()
    {
      return new String(_buffer, _prefix + 1, _length - _prefix - 1);
    }

    String getPrefix()
    {
      if (_prefix==-1) return null;
      return new String(_buffer, 0, _prefix);
    }

    void expandCapacity()
    {
      char []newBuffer = new char[_buffer.length + 64];

      System.arraycopy(_buffer, 0, newBuffer, 0, _buffer.length);

      _buffer = newBuffer;
    }
  }
  /*
  static class NSContext {

    NSContext _parent;

    public NSContext(NSContext parent)
    {
      _parent = parent;
    }
  }
  */
  static {
    for (int i = 0; i < IS_XML_NAME.length; i++) {
      if (isXmlName(i) && i != ':')
        IS_XML_NAME[i] = isXmlName(i);
    }
  }

  private class StreamReaderLocation implements Location {

    private int _offset;
    private int _row;
    private int _col;

    public StreamReaderLocation(int ofs, int row, int col)
    {
      _offset = ofs;
      _row = row;
      _col = col;
    }

    public int getCharacterOffset()
    {
      return _offset;
    }

    public int getColumnNumber()
    {
      return _col;
    }

    public int getLineNumber()
    {
      return _row;
    }

    public String getPublicId()
    {
      return _publicId;
    }

    public String getSystemId()
    {
      return _systemId;
    }

    public String toString() {
      return _row + ":" + _col;
    }

  }

}
