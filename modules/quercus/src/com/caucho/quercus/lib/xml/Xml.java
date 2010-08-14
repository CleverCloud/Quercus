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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.xml;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.*;
import com.caucho.util.L10N;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML object oriented API facade
 */
public class Xml {
  private static final Logger log = Logger.getLogger(Xml.class.getName());
  private static final L10N L = new L10N(Xml.class);

  /**
   * XML_OPTION_CASE_FOLDING is enabled by default
   *
   * only affects startElement (including attribute
   * names) and endElement handlers.
   */
  private boolean _xmlOptionCaseFolding = true;

  private String _xmlOptionTargetEncoding;

  /**
   *  XML_OPTION_SKIP_TAGSTART specifies how many chars
   *  should be skipped in the beginning of a tag name (default = 0)
   *
   *  XXX: Not yet implemented
   */
  private long _xmlOptionSkipTagstart = 0;

  /**
   *  XXX: _xmlOptionSkipWhite not yet implemented
   */
  private boolean _xmlOptionSkipWhite = false;

  /** XXX: _separator is set by xml_parse_create_ns but
   *  not yet used.  Default value is ":"
   *  Possibly should report error if user wants to use
   *  anything other than ":"
   */
  private String _separator;

  private int _errorCode = XmlModule.XML_ERROR_NONE;
  private String _errorString;

  private Callable _startElementHandler;
  private Callable _endElementHandler;
  private Callable _characterDataHandler;
  private Callable _processingInstructionHandler;
  private Callable _defaultHandler;
  private Callable _startNamespaceDeclHandler;
  private Callable _endNamespaceDeclHandler;
  private Callable _notationDeclHandler;
  private Callable _unparsedEntityDeclHandler;

  private Value _parser;
  private Value _obj;

  SAXParserFactory _factory = SAXParserFactory.newInstance();
  
  private StringValue _xmlString;
  private XmlHandler _xmlHandler;

  public Xml(Env env,
             String outputEncoding,
             String separator)
  {
    _xmlOptionTargetEncoding = outputEncoding;
    _parser = env.wrapJava(this);
    _separator = separator;
  }

  public int getLine()
  {
    if (_xmlHandler != null)
      return _xmlHandler.getLine();
    else
      return 0;
  }

  public int getColumn()
  {
    if (_xmlHandler != null)
      return _xmlHandler.getColumn();
    else
      return 0;
  }

  public int getByteIndex()
  {
    return 0;
  }

  public int getErrorCode()
  {
    return _errorCode;
  }

  public String getErrorString()
  {
    return _errorString;
  }

  /**
   * Sets the element handler functions for the XML parser.
   *
   * @param startElementHandler must exist when xml_parse is called
   * @param endElementHandler must exist when xml_parse is called
   * @return true always even if handlers are disabled
   */

  public boolean xml_set_element_handler(Env env,
                                         Value startElementHandler,
                                         Value endElementHandler)
  {
    if (_obj == null) {
      _startElementHandler = startElementHandler.toCallable(env);
      _endElementHandler = endElementHandler.toCallable(env);
    } else {
      if (! startElementHandler.isEmpty()) {
        Value value = new ArrayValueImpl();
        value.put(_obj);
        value.put(startElementHandler);
        _startElementHandler = value.toCallable(env);
      }

      if (! endElementHandler.isEmpty()) {
        Value value = new ArrayValueImpl();
        value.put(_obj);
        value.put(endElementHandler);
        _endElementHandler = value.toCallable(env);
      }
    }
    return true;
  }

  /**
   * Sets the character data handler function.
   *
   * @param handler can be empty string or FALSE
   * @return true always even if handler is disabled
   */
  public boolean xml_set_character_data_handler(Env env, Value handler)
  {
    if (_obj == null) {
      _characterDataHandler = handler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _characterDataHandler = value.toCallable(env);
    }

    return true;
  }

  /**
   * The php documentation is very vague as to the purpose
   * of the default handler.
   *
   * We are interpreting it as an alternative to the character
   * data handler.
   *
   * If character handler is defined, then use that.  Otherwise,
   * use default handler, if it is defined.
   *
   * XXX: Need to confirm that this is appropriate
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_default_handler(Env env, Value handler)
  {
    if (_obj == null) {
      _defaultHandler = handler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _defaultHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * Sets the processing instruction handler function
   *
   * @param processingInstructionHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_processing_instruction_handler(
      Env env,
      Value processingInstructionHandler)
  {
    if (_obj == null) {
      _processingInstructionHandler
        = processingInstructionHandler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(processingInstructionHandler);
      _processingInstructionHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * Sets the startPrefixMapping handler
   *
   * @param startNamespaceDeclHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_start_namespace_decl_handler(
      Env env,
      Value startNamespaceDeclHandler)
  {
    if (_obj == null) {
      _startNamespaceDeclHandler = startNamespaceDeclHandler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(startNamespaceDeclHandler);
      _startNamespaceDeclHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * Sets the unparsedEntityDecl handler
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_unparsed_entity_decl_handler(Env env, Value handler)
  {
    if (_obj == null) {
      _unparsedEntityDeclHandler = handler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _unparsedEntityDeclHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * Sets the endPrefixMapping handler
   *
   * @param endNamespaceDeclHandler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_end_namespace_decl_handler(
      Env env,
      Value endNamespaceDeclHandler)
  {
    if (_obj == null) {
      _endNamespaceDeclHandler = endNamespaceDeclHandler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(endNamespaceDeclHandler);
      _endNamespaceDeclHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * Sets the notationDecl handler
   *
   * @param handler
   * @return true always even if handler is disabled
   */
  public boolean xml_set_notation_decl_handler(Env env, Value handler)
  {
    if (_obj == null) {
      _notationDeclHandler = handler.toCallable(env);
    } else {
      Value value = new ArrayValueImpl();
      value.put(_obj);
      value.put(handler);
      _notationDeclHandler = value.toCallable(env);
    }
    return true;
  }

  /**
   * sets the object which houses all the callback functions
   *
   * @param obj
   * @return returns true unless obj == null
   */
  public boolean xml_set_object(Value obj)
  {
    if (obj == null)
      return false;

    _obj = obj;

    return true;
  }

  /**
   * xml_parse will keep accumulating "data" until
   * either is_final is true or omitted
   *
   * @param data
   * @param isFinal
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public int xml_parse(Env env,
                       StringValue data,
                       @Optional("true") boolean isFinal)
    throws Exception
  {
    if (_xmlString == null)
      _xmlString = data.createStringBuilder();
    
    _xmlString.append(data);

    if (isFinal) {
      InputSource is;
      
      if (_xmlString.isUnicode()) {
        is = new InputSource(_xmlString.toReader("utf-8"));
        
        _xmlOptionTargetEncoding = is.getEncoding();
      }
      else if (_xmlOptionTargetEncoding != null
               && _xmlOptionTargetEncoding.length() > 0)
        is = new InputSource(_xmlString.toReader(_xmlOptionTargetEncoding));
      else {
        is = new InputSource(_xmlString.toInputStream());
        
        _xmlOptionTargetEncoding = is.getEncoding();
      }
      
      try {
        _errorCode = XmlModule.XML_ERROR_NONE;
        _errorString = null;

        _xmlHandler = new XmlHandler(env);

        SAXParser saxParser = _factory.newSAXParser();
        saxParser.parse(is, _xmlHandler);
      } catch (SAXException e) {
        _errorCode = XmlModule.XML_ERROR_SYNTAX;
        _errorString = e.toString();

        log.log(Level.FINE, e.getMessage(), e);
        return 0;
      } catch (IOException e) {
        _errorCode = XmlModule.XML_ERROR_SYNTAX;
        _errorString = e.toString();

        log.log(Level.FINE, e.getMessage(), e);
        return 0;
      } catch (Exception e) {
        _errorCode = XmlModule.XML_ERROR_SYNTAX;
        _errorString = e.toString();
        
        log.log(Level.FINE, e.toString(), e);
        return 0;
      } finally {
        _xmlHandler = null;
      }
      
    }

    return 1;
  }

  /**
   * Parses data into 2 parallel array structures.
   *
   * @param data
   * @param valsV
   * @param indexV
   * @return 0 for failure, 1 for success
   */
  public int xml_parse_into_struct(Env env,
                                   StringValue data,
                                   @Reference Value valsV,
                                   @Optional @Reference Value indexV)
    throws Exception
  {
    ArrayValueImpl valueArray = new ArrayValueImpl();
    ArrayValueImpl indexArray = new ArrayValueImpl();
    
    valsV.set(valueArray);
    indexV.set(indexArray);
    
    if (data == null || data.length() == 0)
      return 0;

    if (_xmlString == null)
      _xmlString = data.toStringBuilder(env);

    InputSource is;
    
    if (_xmlString.isUnicode())
      is = new InputSource(_xmlString.toReader("utf-8"));
    else
      is = new InputSource(_xmlString.toInputStream());

    try {
      SAXParser saxParser = _factory.newSAXParser();
      saxParser.parse(is, new StructHandler(env, valueArray, indexArray));
    } catch (SAXException e) {
      _errorCode = XmlModule.XML_ERROR_SYNTAX;
      _errorString = e.toString();
      
      log.log(Level.FINE, e.toString(), e);
      
      return 0;
    } catch (IOException e) {
      _errorCode = XmlModule.XML_ERROR_SYNTAX;
      _errorString = e.toString();
      
      log.log(Level.FINE, e.toString(), e);
      
      return 0;
    } catch (Exception e) {
      _errorCode = XmlModule.XML_ERROR_SYNTAX;
      _errorString = e.toString();
      
      log.log(Level.FINE, e.toString(), e);
      
      return 0;
    }

    return 1;
  }

  /**
   *  sets one of the following:
   *  _xmlOptionCaseFolding (ENABLED / DISABLED)
   *  _xmlOptionTargetEncoding (String)
   *  _xmlOptionSkipTagstart (int)
   *  _xmlOptionSkipWhite (ENABLED / DISABLED)
   *
   * XXX: currently only _xmlOptionCaseFolding actually does something
   *
   * @param option
   * @param value
   * @return true unless value could not be set
   */
  public boolean xml_parser_set_option(int option,
                                       Value value)
  {
    switch(option) {
      case XmlModule.XML_OPTION_CASE_FOLDING:
        _xmlOptionCaseFolding = value.toBoolean();
        return true;
      case XmlModule.XML_OPTION_SKIP_TAGSTART:
        _xmlOptionSkipTagstart = value.toLong();
        return true;
      case XmlModule.XML_OPTION_SKIP_WHITE:
        _xmlOptionSkipWhite = value.toBoolean();
        return true;
      case XmlModule.XML_OPTION_TARGET_ENCODING:
        _xmlOptionTargetEncoding = value.toString();
        return true;
      default:
        return false;
    }
  }

  /**
   *
   * @param option
   * @return relevant value
   */
  public Value xml_parser_get_option(Env env, int option)
  {
    switch (option) {
    case XmlModule.XML_OPTION_CASE_FOLDING:
      return (_xmlOptionCaseFolding ? LongValue.ONE : LongValue.ZERO);
    case XmlModule.XML_OPTION_SKIP_TAGSTART:
      return LongValue.create(_xmlOptionSkipTagstart);
    case XmlModule.XML_OPTION_SKIP_WHITE:
      return (_xmlOptionSkipWhite ? LongValue.ONE : LongValue.ZERO);
    case XmlModule.XML_OPTION_TARGET_ENCODING:
      return env.createString(_xmlOptionTargetEncoding);
    default:
      return BooleanValue.FALSE;
    }
  }

  public String toString()
  {
    return "Xml[]";
  }

  /**
   * handler solely for xml_parse_into_struct
   */
  class StructHandler extends DefaultHandler {
    private ArrayValueImpl _valueArray;
    private ArrayValueImpl _indexArray;

    //Keeps track of depth within tree;
    //startElement increments, endElement decrements
    private int _level = 1;

    private HashMap<Integer, String> _paramHashMap =
        new HashMap<Integer, String> ();
    private HashMap<StringValue, ArrayValueImpl> _indexArrayHashMap =
        new HashMap<StringValue, ArrayValueImpl>();
    private ArrayList<StringValue> _indexArrayKeys =
        new ArrayList<StringValue>();

    // Used to determine whether a given element has sub elements
    private boolean _isComplete = true;
    private boolean _isOutside = true;

    private int _valueArrayIndex = 0;

    private Locator _locator;
    
    private Env _env;

    public StructHandler(Env env,
                         ArrayValueImpl valueArray,
                         ArrayValueImpl indexArray)
    {
      _env = env;
      
      _valueArray = valueArray;
      _indexArray = indexArray;
    }

    public void setDocumentLocator(Locator locator)
    {
      _locator = locator;
    }

    public int getLine()
    {
      if (_locator != null)
        return _locator.getLineNumber();
      else
        return 0;
    }

    public int getColumn()
    {
      if (_locator != null)
        return _locator.getColumnNumber();
      else
        return 0;
    }

    /**
     * helper function to create an array of attributes for a tag
     * @param attrs
     * @return array of attributes
     */
    private ArrayValueImpl createAttributeArray(Env env, Attributes attrs)
    {
      ArrayValueImpl result = new ArrayValueImpl();

      // turn attrs into an array of name, value pairs
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name
        if ("".equals(aName)) aName = attrs.getQName(i);
        if (_xmlOptionCaseFolding) aName = aName.toUpperCase();
        result.put(
            env.createString(aName), env.createString(attrs.getValue(i)));
      }

      return result;
    }

    public void endDocument()
      throws SAXException
    {
      for (StringValue sv : _indexArrayKeys) {
        _indexArray.put(sv, _indexArrayHashMap.get(sv));
      }
    }

    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
      throws SAXException
    {
      Value elementArray = new ArrayValueImpl();

      String eName = lName; // element name
      if ("".equals(eName)) eName = qName;
      if (_xmlOptionCaseFolding) eName = eName.toUpperCase();

      elementArray.put(_env.createString("tag"), _env.createString(eName));
      elementArray.put(_env.createString("type"), _env.createString("open"));
      elementArray.put(_env.createString("level"), LongValue.create(_level));
      _paramHashMap.put(_level, eName);

      if (attrs.getLength() > 0) {
        elementArray.put(_env.createString("attributes"), 
                         createAttributeArray(_env, attrs));
      }

      _valueArray.put(LongValue.create(_valueArrayIndex), elementArray);

      addToIndexArrayHashMap(eName);

      _valueArrayIndex++;
      _level++;
      _isComplete = true;
      _isOutside = false;
    }

    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      Value elementArray;

      _level--;

      if (_isComplete) {
        elementArray = _valueArray.get(LongValue.create(_valueArrayIndex - 1));
        elementArray.put(
            _env.createString("type"), _env.createString("complete"));
      } else {
        elementArray = new ArrayValueImpl();
        String eName = sName; // element name
        if ("".equals(sName)) eName = qName;
        if (_xmlOptionCaseFolding) eName = eName.toUpperCase();
        elementArray.put(_env.createString("tag"), _env.createString(eName));
        elementArray.put(_env.createString("type"), _env.createString("close"));
        elementArray.put(_env.createString("level"), LongValue.create(_level));
        _valueArray.put(LongValue.create(_valueArrayIndex), elementArray);

        addToIndexArrayHashMap(eName);
        _valueArrayIndex++;
      }

      _isComplete = false;
      _isOutside = true;
    }

    private void addToIndexArrayHashMap(String eName)
    {
      StringValue key = _env.createString(eName);
      ArrayValueImpl indexArray = _indexArrayHashMap.get(key);

      if (indexArray == null) {
        indexArray = new ArrayValueImpl();
        _indexArrayKeys.add(key);
      }

      indexArray.put(LongValue.create(_valueArrayIndex));
      _indexArrayHashMap.put(key, indexArray);
    }

    public void characters(char[] ch,
                           int start,
                           int length)
      throws SAXException
    {
      String s = new String(ch, start, length);

      if (_isOutside) {
        Value elementArray = new ArrayValueImpl();
        elementArray.put(
            _env.createString("tag"),
            _env.createString(_paramHashMap.get(_level - 1)));
        elementArray.put(
            _env.createString("value"), _env.createString(s));
        elementArray.put(
            _env.createString("type"), _env.createString("cdata"));
        elementArray.put(
            _env.createString("level"), LongValue.create(_level - 1));
        _valueArray.put(LongValue.create(_valueArrayIndex), elementArray);

        Value indexArray = _indexArray.get(
            _env.createString(_paramHashMap.get(_level - 1)));
        indexArray.put(LongValue.create(_valueArrayIndex));

        _valueArrayIndex++;
      } else {
        Value elementArray = _valueArray.get(
            LongValue.create(_valueArrayIndex - 1));
        elementArray.put(_env.createString("value"), _env.createString(s));
      }
    }
  }

  class XmlHandler extends DefaultHandler {
    private Locator _locator;
    
    private Env _env;
    
    XmlHandler(Env env)
    {
      _env = env;
    }

    public void setDocumentLocator(Locator locator)
    {
      _locator = locator;
    }

    public int getLine()
    {
      if (_locator != null)
        return _locator.getLineNumber();
      else
        return 0;
    }

    public int getColumn()
    {
      if (_locator != null)
        return _locator.getColumnNumber();
      else
        return 0;
    }

    /**
     * wrapper for _startElementHandler.  creates Value[] args
     *
     * @param namespaceURI
     * @param lName
     * @param qName
     * @param attrs
     * @throws SAXException
     */
    public void startElement(String namespaceURI,
                             String lName,
                             String qName,
                             Attributes attrs)
      throws SAXException
    {
      /**
       *  args[0] reference to this parser
       *  args[1] name of element
       *  args[2] array of attributes
       *
       *  Typical call in PHP looks like:
       *
       *  function startElement($parser, $name, $attrs) {...}
       */
      Value[] args = new Value[3];

      args[0] = _parser;

      String eName = lName; // element name
      if ("".equals(eName))
        eName = qName;
      
      if (_xmlOptionCaseFolding)
        eName = eName.toUpperCase();
      
      args[1] = _env.createString(eName);

      // turn attrs into an array of name, value pairs
      args[2] = new ArrayValueImpl();
      for (int i = 0; i < attrs.getLength(); i++) {
        String aName = attrs.getLocalName(i); // Attr name

        if ("".equals(aName))
          aName = attrs.getQName(i);

        if (_xmlOptionCaseFolding)
          aName = aName.toUpperCase();

        args[2].put(
            _env.createString(aName),
            _env.createString(attrs.getValue(i)));
      }

      try {
        if (_startElementHandler != null)
          _startElementHandler.call(_env, args);
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " startElement " + qName);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _endElementHandler
     *
     * @param namespaceURI
     * @param sName
     * @param qName
     * @throws SAXException
     */
    public void endElement(String namespaceURI,
                           String sName,
                           String qName)
      throws SAXException
    {
      try {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName;
        if (_xmlOptionCaseFolding) eName = eName.toUpperCase();

        if (_endElementHandler != null)
          _endElementHandler.call(_env, _parser, _env.createString(eName));
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " endElement " + sName);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _characterDataHandler
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void characters(char[] buf,
                           int start,
                           int length)
      throws SAXException
    {
      StringValue value;
      
      if (_env.isUnicodeSemantics()) {
        value = _env.createString(buf, start, length);
      }
      else {
        String encoding = _xmlOptionTargetEncoding;
        
        if (encoding == null)
          encoding = "UTF-8";
        
        String s = new String(buf, start, length);
        
        byte[] bytes;
        
        try {
          bytes = s.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new QuercusException(e);
        }

        value = _env.createStringBuilder();
        
        value.append(bytes);
      }

      try {
        if (_characterDataHandler != null)
          _characterDataHandler.call(_env, _parser, value);
        else if (_defaultHandler != null)
          _defaultHandler.call(_env, _parser, value);
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " characters '" + value + "'");
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _processingInstructionHandler
     * @param target
     * @param data
     * @throws SAXException
     */
    public void processingInstruction(String target,
                                      String data)
      throws SAXException
    {
      try {
        if (_processingInstructionHandler != null) {
          _processingInstructionHandler.call(_env, _parser,
                                             _env.createString(target),
                                             _env.createString(data));
        }
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " processingInstruction " + target);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _startNamespaceDeclHandler
     * @param prefix
     * @param uri
     * @throws SAXException
     */
    public void startPrefixMapping (String prefix,
                                    String uri)
      throws SAXException
    {
      try {
        if (_startNamespaceDeclHandler != null)
          _startNamespaceDeclHandler.call(
              _env,
              _env.createString(prefix),
              _env.createString(uri));
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " startPrefixMapping " + prefix + " " + uri);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    /**
     * wrapper for _endNamespaceDeclHandler
     *
     * @param prefix
     * @throws SAXException
     */
    public void endPrefixMapping(String prefix)
      throws SAXException
    {
      try {
        if (_endNamespaceDeclHandler != null)
          _endNamespaceDeclHandler.call(_env, _env.createString(prefix));
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " endPrefixMapping");
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    public void notationDecl(String name,
                             String publicId,
                             String systemId)
      throws SAXException
    {
      try {
        if (_notationDeclHandler != null)
          _notationDeclHandler.call(_env,
                                    _parser,
                                    _env.createString(name),
                                    _env.createString(""),
                                    _env.createString(systemId),
                                    _env.createString(publicId));
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " notation " + name);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }

    @Override
    public void unparsedEntityDecl(String name,
                                   String publicId,
                                   String systemId,
                                   String notationName)
      throws SAXException
    {
      /**
       * args[0] reference to this parser
       * args[1] name
       * args[2] base (always "")
       * args[3] systemId
       * args[4] publicId
       * args[5] notationName
       */
      Value[] args = new Value[6];

      args[0] = _parser;
      args[1] = _env.createString(name);
      args[2] = _env.createString("");
      args[3] = _env.createString(systemId);
      args[4] = _env.createString(publicId);
      args[5] = _env.createString(notationName);

      try {
        if (_unparsedEntityDeclHandler != null)
          _unparsedEntityDeclHandler.call(_env, args);
        else {
          if (log.isLoggable(Level.FINER))
            log.finer(this + " unparsedEntity " + name);
        }
      } catch (Exception t) {
        log.log(Level.FINE, t.toString(), t);
        throw new SAXException(L.l(t.getMessage()));
      }
    }
  }
}
