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

import com.caucho.util.L10N;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.Vfs;

import org.w3c.dom.Document;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
abstract public class AbstractParser implements XMLReader, Parser
{
  static final Logger log = Logger.getLogger(AbstractParser.class.getName());
  static final L10N L = new L10N(AbstractParser.class);

  static Hashtable<String,String> _attrTypes = new Hashtable<String,String>();
  static Entities _xmlEntities = new XmlEntities();

  Policy _policy;

  boolean _isCoalescing = true;
  
  boolean _optionalTags = true;
  boolean _skipWhitespace;
  boolean _skipComments;
  boolean _strictComments;
  boolean _strictAttributes;
  boolean _entitiesAsText = false;
  boolean _expandEntities = true;
  boolean _strictCharacters;
  boolean _strictXml;
  boolean _singleTopElement;
  boolean _normalizeWhitespace = false;
  boolean _forgiving;
  boolean _extraForgiving;
  boolean _switchToXml = false;
  boolean _doResinInclude = false;

  boolean _isNamespaceAware = true;
  boolean _isNamespacePrefixes = true;
  boolean _isSAXNamespaces = false;
  
  boolean _isXmlnsPrefix;
  boolean _isXmlnsAttribute;
  
  boolean _isValidating = false;
  boolean _isDtdValidating;

  boolean _isJsp;

  boolean _isStaticEncoding = false;
  String _defaultEncoding = "UTF-8";

  // sax stuff
  ContentHandler _contentHandler;
  EntityResolver _entityResolver;
  DTDHandler _dtdHandler;
  LexicalHandler _lexicalHandler;
  ErrorHandler _errorHandler;
  Locale _locale;
  
  Entities _entities;
  QDocument _owner;
  QDocumentType _dtd;

  DOMBuilder _builder;

  Path _searchPath;
  
  String _publicId;
  String _systemId;
  String _filename;
  int _line = 1;

  /**
   * Creates a new parser with the XmlPolicy and a new dtd.
   */
  AbstractParser()
  {
    this(new XmlPolicy(), null);

    _policy.strictComments = true;
    _policy.strictAttributes = true;
    _policy.strictCharacters = true;
    _policy.strictXml = true;
    _policy.singleTopElement = true;
    _policy.optionalTags = false;
  }

  /**
   * Creates a new parser with a given policy and dtd.
   *
   * @param policy the parsing policy, handling optional tags.
   * @param dtd the parser's dtd.
   */
  AbstractParser(Policy policy, QDocumentType dtd)
  {
    _policy = policy;
    
    if (dtd == null)
      dtd = new QDocumentType(null);
    _dtd = dtd;

    _entities = _xmlEntities;
    if (policy instanceof HtmlPolicy)
      _entities = HtmlEntities.create(4.0);
  }

  void clear()
  {
    _isCoalescing = true;

    _isNamespaceAware = true;
    _isSAXNamespaces = false;
    _isNamespacePrefixes = false;
    _optionalTags = true;
    _skipWhitespace = false;
    _skipComments = false;
    _strictComments = false;
    _strictAttributes = false;
    _entitiesAsText = false;
    _expandEntities = true;
    _strictCharacters = false;
    _strictXml = false;
    _singleTopElement = false;
    _normalizeWhitespace = false;
    _forgiving = false;
    _extraForgiving = false;
    _switchToXml = false;
    _doResinInclude = false;

    _isJsp = false;

    _defaultEncoding = "UTF-8";
    _isStaticEncoding = false;
  }

  void init()
  {
    /*
    _isXmlnsPrefix = (_isNamespaceAware ||
                      _isSAXNamespaces ||
                      _isNamespacePrefixes);
    */
    _isXmlnsPrefix = _isNamespaceAware || _isNamespacePrefixes;
    _isXmlnsAttribute = _isNamespacePrefixes || ! _isNamespaceAware;
  }

  /**
   * Sets the owner.
   */
  public void setOwner(QDocument doc)
  {
    _owner = doc;
  }

  public String getEncoding()
  {
    if (_owner != null)
      return _owner.getEncoding();
    else
      return null;
  }

  public void setFilename(String filename)
  {
    _filename = filename;
  }

  /**
   * Sets the configuration for a document builder.
   */
  public void setConfig(DocumentBuilderFactory factory)
  {
    if (_builder == null)
      _builder = new DOMBuilder();

    _isCoalescing = factory.isCoalescing();
    setExpandEntities(factory.isExpandEntityReferences());
    setSkipComments(factory.isIgnoringComments());
    setSkipWhitespace(factory.isIgnoringElementContentWhitespace());
    setNamespaceAware(factory.isNamespaceAware());
    setNamespacePrefixes(false);
    setValidating(factory.isValidating());
  }
    
  public void setEntitiesAsText(boolean entitiesAsText)
  {
    _entitiesAsText = entitiesAsText;
  }

  public boolean getEntitiesAsText()
  {
    return _entitiesAsText;
  }

  public void setExpandEntities(boolean expandEntities)
  {
    _expandEntities = expandEntities;
    _policy.expandEntities = expandEntities;
  }

  /**
   * Set to true if comments should be skipped.  If false events will be
   * generated for the comments.
   */
  public void setSkipComments(boolean skipComments)
  {
    _skipComments = skipComments;
  }

  /**
   * Set to true if ignorable-whitespace should be skipped.
   */
  public void setSkipWhitespace(boolean skipWhitespace)
  {
    _skipWhitespace = skipWhitespace;
  }

  /**
   * Returns true if text and cdata nodes will be combined.
   */
  public boolean isCoalescing()
  {
    return _isCoalescing;
  }

  /**
   * Set true if text and cdata nodes should be combined.
   */
  public void setCoalescing(boolean isCoalescing)
  {
    _isCoalescing = isCoalescing;
  }

  /**
   * Returns true if the XML should be validated
   */
  public boolean isValidating()
  {
    return _isValidating;
  }

  /**
   * Set true if the XML should be validated
   */
  public void setValidating(boolean isValidating)
  {
    _isValidating = isValidating;
  }

  /**
   * Returns true if the XML should be validated
   */
  public boolean isDtdValidating()
  {
    return _isDtdValidating;
  }

  /**
   * Set true if the XML should be validated
   */
  public void setDtdValidating(boolean isValidating)
  {
    _isDtdValidating = isValidating;
  }

  /**
   * Returns true if the parsing is namespace aware.
   */
  public boolean isNamespaceAware()
  {
    return _isNamespaceAware;
  }

  /**
   * Set true if the parsing is namespace aware.
   */
  public void setNamespaceAware(boolean isNamespaceAware)
  {
    _isNamespaceAware = isNamespaceAware;
  }

  /**
   * Returns true if the parsing uses sax namespaces
   */
  public boolean isSAXNamespaces()
  {
    return _isSAXNamespaces;
  }

  /**
   * Set true if the parsing uses sax namespaces
   */
  public void setSAXNamespaces(boolean isNamespaces)
  {
    _isSAXNamespaces = isNamespaces;
  }

  /**
   * Returns true if the parsing uses namespace prefixes
   */
  public boolean isNamespacePrefixes()
  {
    return _isNamespacePrefixes;
  }

  /**
   * Set true if the parsing uses sax namespaces
   */
  public void setNamespacePrefixes(boolean isNamespaces)
  {
    _isNamespacePrefixes = isNamespaces;
  }

  /**
   * If true, normalizes HTML tags to lower case.
   */
  public void setToLower(boolean toLower)
  {
    if (_policy instanceof HtmlPolicy)
      ((HtmlPolicy) _policy).setToLower(toLower);
  }

  public boolean getSkipComments()
  {
    return _skipComments;
  }

  /**
   * Sets the parser as a forgiving parser, allowing some non-strict
   * XML.
   *
   * @param forgiving if true, forgives non-strict XML.
   */
  public void setForgiving(boolean forgiving)
  {
    _forgiving = forgiving;
  }

  /**
   * Returns true if the parser is forgiving.
   *
   * @return true if the parser forgives non-strict XML.
   */
  public boolean getForgiving()
  {
    return _forgiving;
  }

  /**
   * Set true if the parser should switch from HTML to XML if it detects
   * the &lt;?xml ?> header.
   */
  public void setAutodetectXml(boolean autodetectXml)
  {
    _switchToXml = autodetectXml;
  }
  
  /**
   * Sets the parser to handle special JSP forgiveness.
   *
   * @param isJsp if true, handles special JSP forgiveness.
   */
  public void setJsp(boolean isJsp)
  {
    _isJsp = isJsp;

    if (_policy instanceof HtmlPolicy)
      ((HtmlPolicy) _policy).setJsp(isJsp);
  }
  
  /**
   * Returns true if the parser should handle special JSP forgiveness.
   *
   * @return true if handles special JSP forgiveness.
   */
  public boolean getJsp()
  {
    return _isJsp;
  }

  /**
   * Sets the search path for included documents.  MergePaths are often
   * used.
   *
   * @param path the path to search
   */
  public void setSearchPath(Path path)
  {
    _searchPath = path;
  }

  /**
   * Gets the search path for included documents.  MergePaths are often
   * used.
   *
   * @return the path to search
   */
  public Path getSearchPath()
  {
    return _searchPath;
  }

  /**
   * Sets the default encoding if none is specified.
   *
   * @param encoding the default encoding
   */
  public void setDefaultEncoding(String encoding)
  {
    _defaultEncoding = encoding;
  }

  /**
   * Gets the default encoding if none is specified.
   */
  public String getDefaultEncoding()
  {
    return _defaultEncoding;
  }

  /**
   * Enables including of other XML documents with resin:include.
   *
   * @param doResinInclude if true, enables the include.
   */
  public void setResinInclude(boolean doResinInclude)
  {
    _doResinInclude = doResinInclude;
  }

  /**
   * Returns true if resin:include will include other XML documents.
   *
   * @param doResinInclude if true, enables the include.
   */
  public boolean getResinInclude()
  {
    return _doResinInclude;
  }

  public Object getProperty(String name)
    throws SAXNotRecognizedException
  {
    if (name.equals("http://xml.org/sax/properties/lexical-handler"))
      return _lexicalHandler;
    else if (name.equals("http://xml.org/sax/properties/dom-node"))
      return null;
    else if (name.equals("http://xml.org/sax/properties/xml-string"))
      return null;
    else
      throw new SAXNotRecognizedException(name);
  }

  public void setProperty(String name, Object obj)
    throws SAXNotSupportedException
  {
    if (name.equals("http://xml.org/sax/properties/lexical-handler"))
      _lexicalHandler = (LexicalHandler) obj;
    else if (name.equals("http://xml.org/sax/handlers/LexicalHandler"))
      _lexicalHandler = (LexicalHandler) obj;
    else
      throw new SAXNotSupportedException(name);
  }

  public boolean getFeature(String name)
    throws SAXNotRecognizedException
  {
    if (name.equals("http://xml.org/sax/features/namespaces"))
      return _isSAXNamespaces;
    else if (name.equals("http://xml.org/sax/features/namespace-prefixes"))
      return _isNamespacePrefixes;
    else if (name.equals("http://xml.org/sax/features/string-interning"))
      return true;
    else if (name.equals("http://xml.org/sax/features/validation"))
      return _isValidating;
    else if (name.equals("http://xml.org/sax/features/external-general-entities"))
      return true;
    else if (name.equals("http://xml.org/sax/features/external-parameter-entities"))
      return false;
    else if (name.equals("http://caucho.com/xml/features/skip-comments"))
      return _skipComments;
    else if (name.equals("http://caucho.com/xml/features/resin-include"))
      return _doResinInclude;
    else
      throw new SAXNotRecognizedException(name);
  }

  public void setFeature(String name, boolean value)
    throws SAXNotSupportedException
  {
    if (name.equals("http://xml.org/sax/features/namespaces")) {
      _isNamespaceAware = value;
    }
    else if (name.equals("http://xml.org/sax/features/namespace-prefixes")) {
      // setting namespace-prefixes, even if false, sets namespace-aware
      // see xml/032b
      _isNamespacePrefixes = value;
      _isNamespaceAware = true;
    }
    else if (name.equals("http://caucho.com/xml/features/skip-comments")) {
      _skipComments = value;
    }
    else if (name.equals("http://caucho.com/xml/features/resin-include"))
      _doResinInclude = value;
    else if (name.equals("http://xml.org/sax/features/validation"))
      _isValidating = value;
    else
      throw new SAXNotSupportedException(name);
  }

  public void setLexicalHandler(LexicalHandler handler)
  {
    _lexicalHandler = handler;
  }

  /**
   * Sets the callback object to find files.
   *
   * @param resolver the object to find files.
   */
  public void setEntityResolver(EntityResolver resolver)
  {
    _entityResolver = resolver;
  }

  /**
   * Sets the callback object finding files from system ids.
   *
   * @return the resolver to find files.
   */
  public EntityResolver getEntityResolver()
  {
    return _entityResolver;
  }

  public void setDTDHandler(DTDHandler handler)
  {
    _dtdHandler = handler;
  }

  public DTDHandler getDTDHandler()
  {
    return _dtdHandler;
  }

  public void setContentHandler(ContentHandler handler)
  {
    _contentHandler = handler;
  }

  public ContentHandler getContentHandler()
  {
    return _contentHandler;
  }

  /**
   * Configures the document handler callback.
   *
   * @param handler the new document handler.
   */
  public void setDocumentHandler(DocumentHandler handler)
  {
    if (handler == null)
      _contentHandler = null;
    else
      _contentHandler = new ContentHandlerAdapter(handler);
  }

  public void setErrorHandler(ErrorHandler handler)
  {
    _errorHandler = handler;
  }

  public ErrorHandler getErrorHandler()
  {
    return _errorHandler;
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
  }

  /**
   * SAX parsing from a SAX InputSource
   *
   * @param source source containing the XML
   */
  public void parse(InputSource source)
    throws IOException, SAXException
  {
    init();
    
    if (_searchPath == null) {
      if (source.getSystemId() != null)
        _searchPath = Vfs.lookup(source.getSystemId()).getParent();
    }

    _systemId = source.getSystemId();
    _publicId = source.getPublicId();
    ReadStream stream;
    String encoding = null;

    if (source.getByteStream() != null) {
      stream = Vfs.openRead(source.getByteStream());
      encoding = source.getEncoding();
    }
    else if (source.getCharacterStream() != null) {
      encoding = "UTF-8";
      _isStaticEncoding = true;
      stream = Vfs.openRead(source.getCharacterStream());
    }
    else if (source.getSystemId() != null) {
      InputStream is = openStream(source.getSystemId(),
                                  source.getPublicId(),
                                  null,
                                  true);
      stream = Vfs.openRead(is);
      encoding = source.getEncoding();
    }
    else
      throw new FileNotFoundException(L.l("invalid InputSource"));

    if (encoding != null)
      stream.setEncoding(encoding);

    try {
      parseInt(stream);
    } finally {
      stream.close();
    }
  }
  
  /**
   * SAX parsing from an InputStream
   *
   * @param is stream containing the XML
   */
  public void parse(InputStream is)
    throws IOException, SAXException
  {
    init();

    _systemId = "stream";
    
    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();
      _systemId = path.getURL();
      _filename = path.getUserPath();
      
      if (_searchPath != null) {
      }
      else if (path != null)
        _searchPath = path.getParent();

      parseInt((ReadStream) is);
    }
    else {
      ReadStream rs = VfsStream.openRead(is);
      try {
        parseInt(rs);
      } finally {
        if (rs != is)
          rs.close();
      }
    }
  }
  
  /**
   * SAX parsing from an InputStream
   *
   * @param is stream containing the XML
   */
  public void parse(InputStream is, String systemId)
    throws IOException, SAXException
  {
    init();
    
    parseImpl(is, systemId);
  }
  
  /**
   * SAX parsing from an InputStream
   *
   * @param is stream containing the XML
   */
  public void parseImpl(InputStream is, String systemId)
    throws IOException, SAXException
  {
    if (is instanceof ReadStream) {
      Path path = ((ReadStream) is).getPath();
      
      if (_searchPath != null) {
      }
      else if (path != null) {
        _searchPath = path.getParent();
        if (systemId != null)
          _searchPath = _searchPath.lookup(systemId).getParent();
      }
      else if (systemId != null)
        _searchPath = Vfs.lookup(systemId).getParent();

      if (systemId == null) {
        systemId = path.getURL();
        _filename = ((ReadStream) is).getUserPath();
      }
      else
        _filename = systemId;

      _systemId = systemId;
      
      parseInt((ReadStream) is);
    }
    else {
      if (systemId == null) {
        _systemId = "anonymous.xml";
      }
      else {
        _searchPath = Vfs.lookup(systemId).getParent();
        _systemId = systemId;
      }

      ReadStream rs = VfsStream.openRead(is);
      try {
        parseInt(rs);
      } finally {
        if (rs != is)
          rs.close();
      }
    }
  }

  /**
   * SAX parsing from a file path
   *
   * @param systemId path to the file containing the XML
   */
  public void parse(String systemId)
    throws IOException, SAXException
  {
    InputStream is = openTopStream(systemId, null);
    try {
      parse(is);
    } finally {
      is.close();
    }
  }
  
  /**
   * SAX parsing from a VFS path
   */
  public void parse(Path path)
    throws IOException, SAXException
  {
    init();
    
    if (_searchPath == null)
      _searchPath = path.getParent();
    
    ReadStream is = path.openRead();
    try {
      parseInt(is);
    } finally {
      is.close();
    }
  }

  /**
   * SAX parsing from a string.
   *
   * @param string string containing the XML
   */
  public void parseString(String string)
    throws IOException, SAXException
  {
    init();
    
    ReadStream is = Vfs.openString(string);

    try {
      parseInt(is);
    } finally {
      is.close();
    }
  }
  
  /**
   * Parses a document from a SAX InputSource
   *
   * @param source SAX InputSource containing the XML data.
   */
  public Document parseDocument(InputSource source)
    throws IOException, SAXException
  {
    init();
    
    QDocument doc = new QDocument();

    if (_builder == null)
      _builder = new DOMBuilder();

    _builder.init(doc);
    setOwner(doc);
    
    doc.setSystemId(source.getSystemId());
    _builder.setSystemId(source.getSystemId());
    _builder.setStrictXML(_strictXml);
    _builder.setCoalescing(_isCoalescing);
    _builder.setSkipWhitespace(_skipWhitespace);
    _contentHandler = _builder;

    parse(source);

    return doc;
  }

  /**
   * Parses a document from system path.
   *
   * @param systemId path to the XML data.
   */
  public Document parseDocument(String systemId)
    throws IOException, SAXException
  {
    InputStream is = openTopStream(systemId, null);
    try {
      return parseDocument(is);
    } finally {
      is.close();
    }
  }

  /**
   * Parses a document from a VFS path
   *
   * @param path the VFS path containing the XML document.
   */
  public Document parseDocument(Path path)
    throws IOException, SAXException
  {
    if (_searchPath == null)
      _searchPath = path.getParent();
    
    ReadStream is = path.openRead();
    try {
      Document document = parseDocument(is);
      document.setDocumentURI(path.getURL());

      return document;
    } finally {
      is.close();
    }
  }

  /**
   * Parses an input stream into a DOM document
   *
   * @param is the input stream containing the XML
   *
   * @return the parsed document.
   */
  public Document parseDocument(InputStream is)
    throws IOException, SAXException
  {
    return parseDocument(is, null);
  }

  /**
   * Parses an input stream into a DOM document
   *
   * @param is the input stream containing the XML
   * @param systemId the URL of the stream.
   *
   * @return the parsed document.
   */
  public Document parseDocument(InputStream is, String systemId)
    throws IOException, SAXException
  {
    init();

    QDocument doc = new QDocument();
    parseDocument(doc, is, systemId);

    return doc;
  }

  public void parseDocument(QDocument doc, InputStream is, String systemId)
    throws IOException, SAXException
  {
    _owner = doc;

    if (_builder == null)
      _builder = new DOMBuilder();

    _builder.init(_owner);
    _builder.setSystemId(systemId);
    _builder.setCoalescing(_isCoalescing);
    _builder.setSkipWhitespace(_skipWhitespace);
    _contentHandler = _builder;

    parseImpl(is, systemId);
  }

  /**
   * Parses a string into a DOM document
   *
   * @param string the string containing the XML
   */
  public Document parseDocumentString(String string)
    throws IOException, SAXException
  {
    ReadStream is = Vfs.openString(string);

    try {
      _isStaticEncoding = true;
      return parseDocument(is);
    } finally {
      is.close();
    }
  }

  /**
   * Looks up an input stream from the system id.
   */
  public InputStream openStream(String systemId, String publicId)
    throws IOException, SAXException
  {
    return openStream(systemId, publicId, _entityResolver, false);
  }

  /**
   * Looks up an input stream from the system id.
   */
  public InputStream openTopStream(String systemId, String publicId)
    throws IOException, SAXException
  {
    return openStream(systemId, publicId, _entityResolver, true);
  }

  /**
   * Looks up an input stream from the system id.
   */
  public InputStream openStream(String systemId, String publicId,
                                EntityResolver entityResolver)
    throws IOException, SAXException
  {
    return openStream(systemId, publicId, entityResolver, false);
  }

  /**
   * Looks up an input stream from the system id.
   */
  protected InputStream openStream(String systemId, String publicId,
                                   EntityResolver entityResolver,
                                   boolean isTop)
    throws IOException, SAXException
  {
    int colon = systemId.indexOf(':');
    int slash = systemId.indexOf('/');
    
    boolean isAbsolute = colon > 0 && (colon < slash || slash < 0);
    
    if (slash == 0 || ! isAbsolute) {
      Path pwd;

      if (_searchPath != null)
        pwd = _searchPath;
      else
        pwd = Vfs.lookup(systemId).getParent();
      
      String newId = pwd.lookup(systemId).getURL();
      if (! newId.startsWith("error:"))
        systemId = newId;
      else {
        int tail = _systemId.lastIndexOf('/');
        if (tail >= 0)
          systemId = _systemId.substring(0, tail + 1) + systemId;
      }
    }

    // xml/03c5 -- must be after the normalization
    if (entityResolver != null) {
      InputSource source = entityResolver.resolveEntity(publicId, systemId);

      if (source != null) {
        _filename = systemId;
        _systemId = systemId;

        return openSource(source);
      }
    }

    int ch;
    if (CauchoSystem.isWindows() && systemId.startsWith("file:") &&
        systemId.length() > 7 && systemId.charAt(6) == ':' &&
        (((ch = systemId.charAt(5)) >= 'a' && ch <= 'z') ||
         ch >= 'A' && ch <= 'Z')) {
      colon = 1;
      isAbsolute = false;
      systemId = "/" + systemId.substring(5);
    }

    if (! isTop &&
        isAbsolute && ! systemId.startsWith("file:") &&
        ! systemId.startsWith("jar:") &&
        ! (colon == 1 && CauchoSystem.isWindows())) {
      throw new RemoteURLException(L.l("URL `{0}' was not opened because it is a remote URL.  Any URL scheme other than file: must be handled by a custom entity resolver.",
                                       systemId));
    }
    else if (_searchPath != null) {
      return _searchPath.lookup(systemId).openRead();
    }
    else
      return Vfs.lookup(systemId).openRead();
  }

  /**
   * Opens the source
   */
  protected InputStream openSource(InputSource source)
    throws IOException, SAXException
  {
    if (source.getByteStream() != null) {
      return source.getByteStream();
    }
    else if (source.getCharacterStream() != null) {
      return Vfs.openRead(source.getCharacterStream());
    }
    else if (source.getSystemId() != null) {
      return Vfs.openRead(source.getSystemId());
    }
    else
      throw new FileNotFoundException(L.l("invalid InputSource {0}", source));
  }

  /**
   * Parse the document from a read stream.
   *
   * @param is read stream to parse from.
   *
   * @return The parsed document.
   */
  abstract Document parseInt(ReadStream is)
    throws IOException, SAXException;
  
  static {
    _attrTypes.put("CDATA", "CDATA");
    _attrTypes.put("ID", "ID");
    _attrTypes.put("IDREF", "IDREF");
    _attrTypes.put("IDREFS", "IDREFS");
    _attrTypes.put("ENTITY", "ENTITY");
    _attrTypes.put("ENTITIES", "ENTITIES");
    _attrTypes.put("NMTOKEN", "NMTOKEN");
    _attrTypes.put("NMTOKENS", "NMTOKENS");
  }
}
