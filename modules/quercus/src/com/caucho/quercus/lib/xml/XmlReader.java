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

package com.caucho.quercus.lib.xml;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.xml.stream.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlReader
{
  private static final Logger log
    = Logger.getLogger(XmlReader.class.getName());
  private static final L10N L = new L10N(XmlReader.class);

  private int _depth;
  private int _lastNodeType;

  private int _currentNodeType;

  private boolean _hasAttribute;

  private XMLStreamReader _streamReader;

  private static final HashMap<Integer, Integer> _constConvertMap
    = new HashMap<Integer, Integer>();

  private  HashMap<String, Integer> _startElements;

  public static final int NONE = 0;
  public static final int ELEMENT = 1;
  public static final int ATTRIBUTE = 2;
  public static final int TEXT = 3;
  public static final int CDATA = 4;
  public static final int ENTITY_REF = 5;
  public static final int ENTITY = 6;
  public static final int PI = 7;
  public static final int COMMENT = 8;
  public static final int DOC = 9;
  public static final int DOC_TYPE = 10;
  public static final int DOC_FRAGMENT = 11;
  public static final int NOTATION = 12;
  public static final int WHITESPACE = 13;
  public static final int SIGNIFICANT_WHITESPACE = 14;
  public static final int END_ELEMENT = 15;
  public static final int END_ENTITY = 16;
  public static final int XML_DECLARATION = 17;

  public static final int LOADDTD = 1;
  public static final int DEFAULTATTRS = 2;
  public static final int VALIDATE = 3;
  public static final int SUBST_ENTITIES = 4;

  /**
   * Default constructor.
   *
   * XXX: Not completely sure what the passed in string(s) does.
   *
   * @param string not used
   */
  public XmlReader(@Optional String[] string) {
    _depth = 0;
    _lastNodeType = -1;
    _currentNodeType = XMLStreamConstants.START_DOCUMENT;

    _streamReader = null;

    _startElements = new HashMap<String, Integer>();

    _hasAttribute = false;
  }

  /**
   * Determines if the stream has been opened and produces a warning if not.
   *
   * @param env
   * @param operation name of the operation being performed (i.e. read, etc.)
   * @return true if the stream is open, false otherwise
   */
  private boolean streamIsOpen(Env env, String operation) {
    if (! streamIsOpen()) {
      env.warning(L.l("Load Data before trying to " + operation));

      return false;
    }

    return true;
  }

  /**
   * Determines if the stream has been opened.
   *
   * @return true if the stream is open, false otherwise
   */
  private boolean streamIsOpen() {
    return _streamReader != null;
  }

  /**
   * Returns the number of attributes of the current element.
   *
   * @return the count if it exists, otherwise null
   */
  public Value getAttributeCount() 
  {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      if (_currentNodeType == XMLStreamConstants.START_ELEMENT)
        return LongValue.create(_streamReader.getAttributeCount());
      else
        return LongValue.create(0);
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Returns the base uniform resource locator of the current element.
   *
   * @return the URI, otherwise null
   */
  public Value getBaseURI() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getLocation().getSystemId());
  }

  /**
   * Returns the depth of the current element.
   *
   * @return the depth if it exists, otherwise null
   */
  public Value getDepth() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return LongValue.create(_depth);
  }

  /**
   * Determines whether this element has attributes.
   *
   * @return true if this element has attributes, false if not, otherwise null
   */
  public Value getHasAttributes() {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      if (_currentNodeType == XMLStreamConstants.CHARACTERS)
        return BooleanValue.FALSE;

      return BooleanValue.create(
          _hasAttribute ||  _streamReader.getAttributeCount() > 0);
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Determines whether this element has content.
   *
   * @return true if this element has content, false if not, otherwise null
   */
  public Value getHasValue() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return BooleanValue.create(_streamReader.hasText());
  }

  /**
   * Determines whether this element is default.
   *
   * @return true if this element is default, false if not, otherwise null
   */
  public Value getIsDefault() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // XXX:  StreamReaderImpl.isAttributeSpecified() only checks for
    // attribute existence.  This should be tested against the atttribute list
    // but couldn't find anything like that in StreamReader.
    return BooleanValue.FALSE;
  }

  /**
   * Determines whether this element is empty.
   *
   * @return true if this element is empty, false if not, otherwise null
   */
  public Value getIsEmptyElement() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // The only case I found for isEmptyElement was for something
    // like <element/>.  Even something like <element></element> was
    // not considered empty.
    if (_currentNodeType == XMLStreamConstants.START_ELEMENT
        && _streamReader.isEndElement())
      return BooleanValue.TRUE;

    return BooleanValue.FALSE;
  }

  /**
   * Determines whether this element has attributes.
   *
   * @return true if this element has attributes, false if not, otherwise null
   */
  public Value getLocalName() {
    if (! streamIsOpen())
      return NullValue.NULL;

    String name = "";

    if (_currentNodeType == XMLStreamConstants.CHARACTERS)
      name = "#text";
    else if (_currentNodeType == XMLStreamConstants.COMMENT)
      name = "#comment";
    else
      name = _streamReader.getLocalName();

    return StringValue.create(name);
  }

  /**
   * Returns the name of the current element.
   *
   * @return the name, otherwise null
   */
  public Value getName(Env env) {
    if (! streamIsOpen())
      return NullValue.NULL;

    try {
      String name = "";

      // XXX: Next line should be "String prefix = _streamReader.getPrefix();"
      // but there was a NullPointerException for XMLStreamReaderImpl._name.

      // php/4618
      String prefix = _streamReader.getPrefix();

      if (_currentNodeType == XMLStreamConstants.CHARACTERS)
        name = "#text";
      else if (_currentNodeType == XMLStreamConstants.COMMENT)
        name = "#comment";
      else {
        if (prefix == null || prefix.length() == 0)
          name = _streamReader.getName().toString();
        else
          name = prefix + ":" + _streamReader.getLocalName().toString();
      }

      return StringValue.create(name);
    }
    catch (IllegalStateException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return NullValue.NULL;
    }
  }

  /**
   * Returns the namespace uniform resource locator of the current element.
   *
   * @return the namespace URI, otherwise null
   */
  public Value getNamespaceURI() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getNamespaceURI());
  }

  /**
   * Returns the node type of the current element.
   *
   * @return the node type, otherwise null
   */
  public Value getNodeType() {
    if (! streamIsOpen())
      return NullValue.NULL;

    /*
   Integer convertedInteger = _constConvertMap.get(_nextType);

   int convertedInt = convertedInteger.intValue();

   return LongValue.create(convertedInt);*/

    int convertedInt = SIGNIFICANT_WHITESPACE;

    if (! _streamReader.isWhiteSpace()) {
      Integer convertedInteger =
        _constConvertMap.get(_streamReader.getEventType());

      convertedInt = convertedInteger.intValue();
    }

    return LongValue.create(convertedInt);
  }

  /**
   * Returns the prefix of the current element.
   *
   * @return the prefix, otherwise null
   */
  public Value getPrefix() {
    if (! streamIsOpen())
      return NullValue.NULL;

    return StringValue.create(_streamReader.getPrefix());
  }

  /**
   * Returns the value of the current element.
   *
   * @return the value, otherwise null
   */
  public Value getValue() {
    if (! streamIsOpen())
      return NullValue.NULL;

    if (_currentNodeType != XMLStreamConstants.END_ELEMENT)
      return StringValue.create(_streamReader.getText());

    return StringValue.create(null);
  }

  /**
   * Returns the node type of the current element.
   *
   * @return the node type, otherwise null
   */
  public Value getXmlLang() {
    if (! streamIsOpen())
      return NullValue.NULL;

    // XXX: Defaulted for now.
    return StringValue.create("");
  }

  /**
   * Closes the reader.
   *
   * @return true if success, false otherwise
   */
  public BooleanValue close() {
    if (! streamIsOpen())
      return BooleanValue.TRUE;

    try {
      _streamReader.close();
    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  /**
   *
   * @return
   */
  public Value expand() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param name
   * @return
   */
  public StringValue getAttribute(String name) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param index
   * @return
   */
  public StringValue getAttributeNo(int index) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localName
   * @param namespaceURI
   * @return
   */
  public StringValue getAttributeNS(String localName, String namespaceURI) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param property
   * @return
   */
  public BooleanValue getParserProperty(int property) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue isValid() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param prefix
   * @return
   */
  public BooleanValue lookupNamespace(String prefix) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param name
   * @return
   */
  public BooleanValue moveToAttribute(String name) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param index
   * @return
   */
  public BooleanValue moveToAttributeNo(int index) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localName
   * @param namespaceURI
   * @return
   */
  public BooleanValue moveToAttributeNs(String localName, String namespaceURI) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToElement() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToFirstAttribute() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @return
   */
  public BooleanValue moveToNextAttribute() {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param localname
   * @return
   */
  public BooleanValue next(@Optional String localname) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Opens a stream using the uniform resource locator.
   *
   * @param uri uniform resource locator to open
   * @return true if success, false otherwise
   */
  public BooleanValue open(Env env, Path path) {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();

      _streamReader = factory.createXMLStreamReader(
          path.getNativePath(), path.openRead());
    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("XML input file '{0}' cannot be opened for reading.",
                      path));

      return BooleanValue.FALSE;
    }
    catch (IOException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("Unable to open source data"));

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  /**
   * Updates the depth.
   *
   */
  private void updateDepth(Env env) {
    if (_lastNodeType == XMLStreamConstants.START_ELEMENT
        && ! _streamReader.isEndElement())
      _depth++;
    else if ((_lastNodeType == XMLStreamConstants.CHARACTERS
        || _lastNodeType == XMLStreamConstants.COMMENT)
        && _currentNodeType == XMLStreamConstants.END_ELEMENT)
      _depth--;
  }

  /**
   * Maintains the _hasAttribute variable.
   *
   */
  private void updateAttribute(Env env) {
    _hasAttribute = false;

    String key = getName(env).toString() + _depth;

    if (_currentNodeType == XMLStreamConstants.START_ELEMENT
        && _streamReader.getAttributeCount() > 0) {
      _startElements.put(key, _depth);

      _hasAttribute = true;
    }

    if (_currentNodeType == XMLStreamConstants.END_ELEMENT
        && _startElements.containsKey(key))  {
      _hasAttribute = true;

      _startElements.remove(key);
    }
  }

  /**
   * Moves the cursor to the next node.
   *
   * @return true if success, false otherwise
   */
  public BooleanValue read(Env env) {
    if (! streamIsOpen(env, "read"))
      return BooleanValue.FALSE;

    try {
      if (! _streamReader.hasNext())
        return BooleanValue.FALSE;

      _lastNodeType = _currentNodeType;

      Value isEmptyElement = getIsEmptyElement();
      
      _currentNodeType = _streamReader.next();

      // php/4618
      if (isEmptyElement.toBoolean())
        return read(env);

      if (_currentNodeType == XMLStreamConstants.SPACE)
        return read(env);

      if (_currentNodeType == XMLStreamConstants.END_DOCUMENT)
        return BooleanValue.FALSE;

      updateDepth(env);

      updateAttribute(env);

    }
    catch (XMLStreamException ex) {
      log.log(Level.WARNING, ex.toString(), ex);

      env.warning(L.l("Unable to read :" + ex.toString()));

      return BooleanValue.FALSE;
    }

    return BooleanValue.TRUE;
  }

  public LongValue getNextType() {
    return LongValue.create(_currentNodeType);
  }

  /**
   *
   * @param property
   * @param value
   * @return
   */
  public BooleanValue setParserProperty(int property, boolean value) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param filename
   * @return
   */
  public BooleanValue setRelaxNGSchema(String filename) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param source
   * @return
   */
  public BooleanValue setRelaxNGSchemaSource(String source) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   *
   * @param source
   * @return
   */
  public BooleanValue XML(String source) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  static {
    _constConvertMap.put(XMLStreamConstants.ATTRIBUTE,
                         ATTRIBUTE);
    _constConvertMap.put(XMLStreamConstants.CDATA,
                         CDATA);
    _constConvertMap.put(XMLStreamConstants.CHARACTERS,
                         TEXT);
    _constConvertMap.put(XMLStreamConstants.COMMENT,
                         COMMENT);
    _constConvertMap.put(XMLStreamConstants.END_ELEMENT,
                         END_ELEMENT);
    /*
      _constConvertMap.put(XMLStreamConstants.END_ENTITY,
                      END_ENTITY);
    */
    // XXX: XMLStreamConstants.ENTITY_DECLARATION is 17 in the BAE docs
    // but is 15 in the Resin implementation.
    _constConvertMap.put(XMLStreamConstants.ENTITY_DECLARATION,
                         ENTITY); // ENTITY used twice
    _constConvertMap.put(XMLStreamConstants.ENTITY_REFERENCE,
                         ENTITY_REF);
    _constConvertMap.put(XMLStreamConstants.NOTATION_DECLARATION,
                         NOTATION);
    _constConvertMap.put(XMLStreamConstants.PROCESSING_INSTRUCTION,
                         PI);
    _constConvertMap.put(XMLStreamConstants.SPACE,
                         WHITESPACE);
    _constConvertMap.put(XMLStreamConstants.START_ELEMENT,
                         ELEMENT);
    /*
      _constConvertMap.put(XMLStreamConstants.START_ENTITY,
                      ENTITY);
    */
    // Following constants did not match
    _constConvertMap.put(XMLStreamConstants.DTD, NONE);
    _constConvertMap.put(XMLStreamConstants.END_DOCUMENT, NONE);
    _constConvertMap.put(XMLStreamConstants.NAMESPACE, NONE);
    _constConvertMap.put(XMLStreamConstants.START_DOCUMENT, NONE);
    _constConvertMap.put(0, NONE); // Pre-Read
    _constConvertMap.put(-1, NONE);
    _constConvertMap.put(-1, DOC);
    _constConvertMap.put(-1, DOC_TYPE);
    _constConvertMap.put(-1, DOC_FRAGMENT);
    _constConvertMap.put(-1, DOC_TYPE);
    _constConvertMap.put(-1, XML_DECLARATION);
  }
}
