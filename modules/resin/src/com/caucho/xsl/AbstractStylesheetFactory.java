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

package com.caucho.xsl;

import com.caucho.java.JavaCompiler;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.SimpleLoader;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Crc64Stream;
import com.caucho.vfs.Path;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.*;
import com.caucho.xpath.Expr;
import com.caucho.xpath.XPath;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract factory for creating stylesheets.
 */
abstract public class AbstractStylesheetFactory
  extends SAXTransformerFactory {
  static final Logger log
    = Logger.getLogger(AbstractStylesheetFactory.class.getName());
  static final L10N L = new L10N(AbstractStylesheetFactory.class);

  private static
    EnvironmentLocal<LruCache<String,SoftReference<StylesheetImpl>>> _stylesheetCache =
    new EnvironmentLocal<LruCache<String,SoftReference<StylesheetImpl>>>();

  private URIResolver _uriResolver;
  private ErrorListener _errorListener;

  private String _systemId;

  private Path _workPath;
  private Path _stylePath;
  private ClassLoader _loader;
  private String _className;
  private boolean _isAutoCompile = true;
  private boolean _loadPrecompiledStylesheet = true;

  protected AbstractStylesheetFactory()
  {
  }

  /**
   * Returns an implementation-specific attribute.
   *
   * @param name the attribute name
   */
  public Object getAttribute(String name)
  {
    return null;
  }

  /**
   * Sets an implementation-specific attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void setAttribute(String name, Object value)
  {
  }

  /**
   * Returns an implementation-specific feature.
   *
   * @param name the feature name
   */
  public boolean getFeature(String name)
  {
    if (name.equals(SAXTransformerFactory.FEATURE) ||
        name.equals(SAXTransformerFactory.FEATURE_XMLFILTER) ||
        name.equals(DOMResult.FEATURE) ||
        name.equals(DOMSource.FEATURE) ||
        name.equals(SAXResult.FEATURE) ||
        name.equals(SAXSource.FEATURE) ||
        name.equals(StreamResult.FEATURE) ||
        name.equals(StreamSource.FEATURE))
      return true;
    else
      return false;
  }

  /**
   * Sets an implementation-specific feature
   *
   * @param name the feature name
   * @param value the feature value
   */
  public void setFeature(String name, boolean value)
  {
  }

  /**
   * Returns the URI to filename resolver.
   */
  public URIResolver getURIResolver()
  {
    return _uriResolver;
  }

  /**
   * Sets the URI to filename resolver.
   */
  public void setURIResolver(URIResolver uriResolver)
  {
    _uriResolver = uriResolver;
  }

  /**
   * Returns the error listener.
   */
  public ErrorListener getErrorListener()
  {
    return _errorListener;
  }

  /**
   * Sets the error listener.
   */
  public void setErrorListener(ErrorListener errorListener)
  {
    _errorListener = errorListener;
  }

  public String getSystemId()
  {
    return _systemId;
  }

  public void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  /**
   * Sets the search path for stylesheets.  Generally applications will use
   * MergePath to create their search path.
   *
   * @param path path containing stylesheets.
   */
  public void setStylePath(Path path)
  {
    _stylePath = path;
  }

  /**
   * Returns the stylesheet search path.
   */
  public Path getStylePath()
  {
    if (_stylePath != null)
      return _stylePath;
    else
      return getSearchPath();
  }

  /**
   * Sets the search path for stylesheets.  Generally applications will use
   * MergePath to create their search path.
   *
   * @param path path containing stylesheets.
   */
  public void setSearchPath(Path path)
  {
    _stylePath = path;
  }

  /**
   * Returns the stylesheet search path.
   */
  public Path getSearchPath()
  {
    if (_stylePath != null)
      return _stylePath;
    else
      return Vfs.getPwd();
  }

  /**
   * Sets the working directory.
   */
  public void setWorkPath(Path path)
  {
    _workPath = path;
  }

  /**
   * Gets the working directory.
   */
  public Path getWorkPath()
  {
    if (_workPath != null)
      return _workPath;
    else
      return CauchoSystem.getWorkPath();
  }

  public void setClassName(String className)
  {
    _className = className;
  }

  public String getClassName()
  {
    return _className;
  }

  /**
   * Sets the classloader for the stylesheet.
   *
   * @param loader the new loader.
   */
  public void setClassLoader(ClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Gets the classloader for the stylesheet.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Returns true if precompiled stylesheets should be loaded.
   */
  public boolean getLoadPrecompiledStylesheet()
  {
    return _loadPrecompiledStylesheet;
  }

  /**
   * Returns true if precompiled stylesheets should be loaded.
   */
  public void setLoadPrecompiledStylesheet(boolean preload)
  {
    _loadPrecompiledStylesheet = preload;
  }

  /**
   * Returns true if the stylesheet should be automatically compiled.
   */
  public boolean isAutoCompile()
  {
    return _isAutoCompile;
  }

  /**
   * Returns true if precompiled stylesheets should be loaded.
   */
  public void setAutoCompile(boolean autoCompile)
  {
    _isAutoCompile = autoCompile;
  }

  /**
   * Returns the stylesheet source object associated with the given
   * XML document.
   *
   * @param source the XML document which needs a stylesheet.
   * @param media the media attribute for the stylesheet
   * @param title the title attribute for the stylesheet
   * @param charset the character encoding for the stylesheet result.
   */
  public Source getAssociatedStylesheet(Source source,
                                        String media,
                                        String title,
                                        String charset)
    throws TransformerConfigurationException
  {
    try {
      XmlStylesheetReader reader = new XmlStylesheetReader();

      parseSAX(source, reader);

      String href = reader.getAssociatedStylesheet(media, title, charset);

      if (href == null)
        return null;

      String base = source.getSystemId();
    
      return getSource(href, base);
    } catch (Exception e) {
      throw new TransformerConfigurationException(e);
    }
  }
  
  /**
   * Opens a relative path.
   */
  private Source getSource(String href, String base)
    throws Exception
  {
    Path subpath;

    if (href == null)
      href = "";
    if (base == null)
      base = "/";
    
    if (_uriResolver != null) {
      if (href.startsWith("/") || base.equals("/"))
        subpath = getSearchPath().lookup(href);
      else {
        subpath = getSearchPath().lookup(base).getParent().lookup(href);
      }
      
      Source source = _uriResolver.resolve(href, base);

      if (source != null) {
        if (source.getSystemId() == null)
          source.setSystemId(subpath.getURL());

        return source;
      }
    }
    
    if (href.startsWith("/") || base.equals("/"))
      subpath = getSearchPath().lookup(href);
    else {
      if (base.startsWith("file:"))
        base = base.substring(5);
      
      subpath = getSearchPath().lookup(base).getParent().lookup(href);
    }

    return new StreamSource(subpath.getURL());
  }

  private void parseSAX(Source source, ContentHandler handler)
    throws TransformerConfigurationException
  {
    try {
      if (source instanceof SAXSource) {
        SAXSource saxSource = (SAXSource) source;

        XMLReader reader = saxSource.getXMLReader();
        InputSource inputSource = saxSource.getInputSource();

        reader.setContentHandler(handler);

        reader.parse(inputSource);
      }
      else if (source instanceof StreamSource) {

        XmlParser parser = new Xml();

        parser.setContentHandler(handler);

        ReadStream rs = openPath(source);
        try {
          parser.parse(rs);
        } finally {
          rs.close();
        }
      }
      else if (source instanceof DOMSource) {
        DOMSource domSource = (DOMSource) source;

        Node node = domSource.getNode();

        XmlUtil.toSAX(node, handler);
      }
    } catch (Exception e) {
      throw new TransformerConfigurationException(e);
    }
  }

  /**
   * Create a transformer from an input stream.
   *
   * @param source the source stream
   *
   * @return the compiled stylesheet
   */
  public javax.xml.transform.Transformer newTransformer(Source source)
    throws TransformerConfigurationException
  {
    Templates templates = newTemplates(source);

    return templates.newTransformer();
  }

  /**
   * Create an identity transformer.
   *
   * @return the compiled stylesheet
   */
  public javax.xml.transform.Transformer newTransformer()
    throws TransformerConfigurationException
  {
    return new TransformerImpl(new IdentityStylesheet());
  }

  /**
   * Creates a new stylesheet from an XML document.
   */
  public StylesheetImpl newStylesheet(Document xsl)
    throws Exception
  {
    return (StylesheetImpl) generate(xsl, null);
  }

  /**
   * Create a new stylesheet from a reader.
   */
  public StylesheetImpl newStylesheet(Reader reader)
    throws Exception
  {
    ReadStream rs = Vfs.openRead(reader);

    return (StylesheetImpl) generate(parseXSL(rs), rs.getPath());
  }

  /**
   * Create a new stylesheet from an input stream.
   */
  public StylesheetImpl newStylesheet(InputStream is)
    throws Exception
  {
    ReadStream rs = Vfs.openRead(is);

    return (StylesheetImpl) generate(parseXSL(rs), rs.getPath());
  }

  /**
   * Loads a stylesheet from a named file
   *
   * @param systemId the URL of the file
   */
  public StylesheetImpl newStylesheet(String systemId)
    throws Exception
  {
    StylesheetImpl stylesheet = loadPrecompiledStylesheet(systemId, systemId);

    if (stylesheet != null)
      return stylesheet;

    synchronized (AbstractStylesheetFactory.class) {
      stylesheet = loadPrecompiledStylesheet(systemId, systemId);

      if (stylesheet != null)
        return stylesheet;
      
      ReadStream is;

      if (_stylePath != null)
        is = _stylePath.lookup(systemId).openRead();
      else
        is = Vfs.lookup(systemId).openRead();
    
      try {
        return newStylesheet(is);
      } finally {
        if (is != null)
          is.close();
      }
    }
  }

  public StylesheetImpl newStylesheet(Path path)
    throws Exception
  {
    StylesheetImpl stylesheet = loadPrecompiledStylesheet(path.getFullPath(),
                                                          path.getUserPath());

    if (stylesheet != null)
      return stylesheet;

    synchronized (AbstractStylesheetFactory.class) {
      stylesheet = loadPrecompiledStylesheet(path.getFullPath(),
                                             path.getUserPath());

      if (stylesheet != null)
        return stylesheet;
    
      Path oldStylePath = _stylePath;
    
      if (_stylePath == null)
        _stylePath = path.getParent();
    
      InputStream is = null;
    
      try {
        is = path.openRead();
        return newStylesheet(is);
      } finally {
        _stylePath = oldStylePath;
        if (is != null)
          is.close();
      }
    }
  }

  /**
   * Create a compiled stylesheet from an input stream.
   *
   * @param source the source stream
   *
   * @return the compiled stylesheet
   */
  public Templates newTemplates(Source source)
    throws TransformerConfigurationException
  {
    String systemId = source.getSystemId();

    try {
      if (systemId != null) {
        StylesheetImpl stylesheet = loadPrecompiledStylesheet(systemId,
                                                              systemId);

        if (stylesheet != null)
          return stylesheet;
      }

      if (source instanceof DOMSource) {
        Node node = ((DOMSource) source).getNode();

        return generateFromNode(node, systemId);
      }
      else if (source instanceof SAXSource) {
        SAXSource saxSource = (SAXSource) source;
        XMLReader reader = saxSource.getXMLReader();
        InputSource inputSource = saxSource.getInputSource();

        Document doc = new QDocument();
        DOMBuilder builder = new DOMBuilder();
        builder.init(doc);
        reader.setContentHandler(builder);

        reader.parse(inputSource);
      
        return generateFromNode(doc, systemId);
      }

      ReadStream rs = openPath(source);
      try {
        Path path = rs.getPath();

        Document doc = parseXSL(rs);

        if (systemId != null) {
          String mangledName = getMangledName(systemId);
          Path genPath = getWorkPath().lookup(mangledName);

          genPath.setUserPath(systemId);

          return generate(doc, genPath);
        }
        else
          return generateFromNode(doc, null);
      } finally {
        if (rs != null)
          rs.close();
      }
    } catch (TransformerConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new XslParseException(e);
    } 
  }

  private Templates generateFromNode(Node node, String systemId)
    throws IOException, TransformerConfigurationException
  {
    Path tempPath = writeTempFile(node);

    String tempId = tempPath.getTail();
    
    StylesheetImpl stylesheet = loadPrecompiledStylesheet(tempId,
                                                          tempId,
                                                          false);

    if (systemId != null)
      tempPath.setUserPath(systemId);

    if (stylesheet != null)
      return stylesheet;

    return generate(node, tempPath);
  }

  private Path writeTempFile(Node node)
    throws IOException
  {
    Path workDir = CauchoSystem.getWorkPath().lookup("_xsl");
    workDir.mkdirs();
    
    // Path temp = workDir.createTempFile("tmp", "xsl");

    WriteStream os = Vfs.lookup("null:").openWrite();
    Crc64Stream crcStream = new Crc64Stream(os.getSource());
    os.init(crcStream);
    try {
      XmlPrinter printer = new XmlPrinter(os);

      printer.printNode(node);
    } finally {
      os.close();
    }

    long crc = crcStream.getCRC();
    CharBuffer cb = new CharBuffer();
    Base64.encode(cb, crc);

    String crcValue = cb.toString().replace('/', '-');

    Path xslPath = workDir.lookup(crcValue + ".xsl");

    // temp.renameTo(xslPath);

    return xslPath;
  }
  
  /**
   * Create a new transformer handler.
   */
  public TransformerHandler newTransformerHandler()
    throws TransformerConfigurationException
  {
    return newTransformerHandler(new StylesheetImpl());
  }
  
  /**
   * Create a new transformer handler based on a source.
   */
  public TransformerHandler newTransformerHandler(Source source)
    throws TransformerConfigurationException
  {
    return newTransformerHandler(newTemplates(source));
  }
  
  /**
   * Create a new transformer handler based on a stylesheet.
   */
  public TransformerHandler newTransformerHandler(Templates templates)
    throws TransformerConfigurationException
  {
    return new TransformerHandlerImpl(templates.newTransformer());
  }
  
  /**
   * Returns a templates handler.
   *
   * @param source the source file
   */
  public TemplatesHandler newTemplatesHandler()
    throws TransformerConfigurationException
  {
    return new TemplatesHandlerImpl(this);
  }
  
  /**
   * Returns an XML filter from the transformer.
   *
   * @param source the source file
   */
  public XMLFilter newXMLFilter(Source source)
    throws TransformerConfigurationException
  {
    Templates templates = newTemplates(source);
    
    return newXMLFilter(templates);
  }
  
  /**
   * Returns an XML filter from the transformer.
   *
   * @param source the source file
   */
  public XMLFilter newXMLFilter(Templates templates)
    throws TransformerConfigurationException
  {
    return new SAXFilterImpl((TransformerImpl) templates.newTransformer());
  }

  /**
   * Parses a stylesheet from the source.
   */
  protected Node parseStylesheet(Source source)
    throws TransformerConfigurationException
  {
    if (source instanceof DOMSource)
      return ((DOMSource) source).getNode();
    else if (source instanceof StreamSource) {
      InputStream is = ((StreamSource) source).getInputStream();
      ReadStream rs = null;

      try {
        rs = Vfs.openRead(is);
        return parseXSL(rs);
      } catch (Exception e) {
        throw new TransformerConfigurationException(e);
      } finally {
        if (rs != null)
          rs.close();
      }
    }
    else
      return null;
  }

  /**
   * Convenience class to create a compiled stylesheet.
   *
   * @param node DOM source for the stylesheet.
   *
   * @return a compiled stylesheet
   */
  public javax.xml.transform.Templates newTemplates(Node node)
    throws TransformerConfigurationException
  {
    Document doc = node.getOwnerDocument();
    if (node instanceof Document)
      doc = (Document) node;
    
    DocumentType dtd = doc.getDoctype();
    
    if (dtd != null && dtd.getSystemId() != null)
      return generate(node, getSearchPath().lookup(dtd.getSystemId()));
    else if (doc instanceof CauchoDocument) {
      String systemId = ((CauchoDocument) doc).getFilename();
    
      return generate(node, getSearchPath().lookup(systemId));
    }
    else
      return generate(node, null);
  }

  /**
   * Convenience class to create a compiled stylesheet.
   *
   * @param systemId source path for the stylesheet.
   *
   * @return a compiled stylesheet
   */
  public javax.xml.transform.Templates newTemplates(String systemId)
    throws TransformerConfigurationException
  {
    StylesheetImpl stylesheet = loadPrecompiledStylesheet(systemId, systemId);

    if (stylesheet != null)
      return stylesheet;

    else if (systemId == null)
      return generate(new QDocument(), null);

    Path path = getSearchPath().lookup(systemId);
    
    try {
      ReadStream is = path.openRead();
      Document doc;
      try {
        doc = parseXSL(is);
      } finally {
        is.close();
      }

      return generate(doc, path);
    } catch (TransformerConfigurationException e) {
      throw e;
    } catch (IOException e) {
      System.out.println("MP: " + ((MergePath) getSearchPath()).getMergePaths());
      throw new TransformerConfigurationExceptionWrapper(e);
    } catch (Exception e) {
      throw new TransformerConfigurationExceptionWrapper(e);
    }
  }

  

  /**
   * Opens a relative path.
   */
  ReadStream openPath(String href, String base)
    throws TransformerException, IOException
  {
    if (_uriResolver != null) {
      Source source = _uriResolver.resolve(href, base);

      if (source != null)
        return openPath(source);
    }

    if (href.startsWith("/") || base.equals("/"))
      return getSearchPath().lookup(href).openRead();
    else {
      Path path = getSearchPath().lookup(base).getParent().lookup(href);

      if (path.exists())
        return path.openRead();
      else
        return getSearchPath().lookup(href).openRead();
    }
  }

  /**
   * Opens a path based on a Source.
   */
  ReadStream openPath(Source source)
    throws TransformerException, IOException
  {
    String systemId = source.getSystemId();
    
    Path path;
    if (systemId != null)
      path = getSearchPath().lookup(systemId);
    else
      path = getSearchPath().lookup("anonymous.xsl");

    if (source instanceof StreamSource) {
      StreamSource stream = (StreamSource) source;

      InputStream is = stream.getInputStream();

      if (is instanceof ReadStream) {
        ReadStream rs = (ReadStream) is;

        rs.setPath(path);

        return rs;
      }
      else if (is != null) {
        ReadStream rs = Vfs.openRead(is);
        rs.setPath(path);

        return rs;
      }

      Reader reader = stream.getReader();

      if (reader != null) {
        ReadStream rs = Vfs.openRead(reader);
        rs.setPath(path);

        return rs;
      }
    }
    
    if (systemId != null)
      return getSearchPath().lookup(systemId).openRead();

    throw new TransformerException("bad source " + source);
  }

  Path lookupPath(String base, String href)
    throws TransformerException
  {
    if (_uriResolver != null) {
      Source source = _uriResolver.resolve(href, base);

      if (source != null) {
        String systemId = source.getSystemId();

        if (systemId != null)
          return getSearchPath().lookup(systemId);
      }
    }
    
    return getSearchPath().lookup(base).lookup(href);
  }

  /**
   * Convenience class to create a transformer instance.
   *
   * @param xsl DOM source for the stylesheet.
   *
   * @return a transformer instance.
   */
  public javax.xml.transform.Transformer newTransformer(Document xsl)
    throws TransformerConfigurationException
  {
    return newTemplates(xsl).newTransformer();
  }

  /**
   * Convenience class to transform a node.
   *
   * @param xsl DOM containing the parsed xsl.
   * @param xml DOM document node.
   * @param out output stream destination.
   */
  public void transform(Document xsl, Node xml, OutputStream out)
    throws Exception
  {
    TransformerImpl transformer = (TransformerImpl) newTransformer(xsl);

    transformer.transform(xml, out);
  }
  
  /**
   * Convenience class to transform a node.
   *
   * @param xsl path name to the xsl file.
   * @param xml dom source document.
   * @param out output stream destination.
   */
  public void transform(String xsl, Node xml, OutputStream out)
    throws Exception
  {
    TransformerImpl transformer;

    transformer = (TransformerImpl) newTemplates(xsl).newTransformer();

    transformer.transform(xml, out);
  }

  /**
   * Parses the XSL into a DOM document.
   *
   * @param rs the input stream.
   */
  abstract protected Document parseXSL(ReadStream rs)
    throws TransformerConfigurationException;

  /**
   * Generates a compiled stylesheet from a parsed XSL document.
   *
   * @param xsl the parsed xsl document.
   * @param path the path to the document.
   */
  Templates generate(Node xsl, Path path)
    throws TransformerConfigurationException
  {
    log.fine("Generating XSL from " + path);
    
    // The code generation needs a static lock because the
    // application might have a separate factory object
    // for each thread.  The static lock protects the code generation
    // from overwriting its own code.
    synchronized (AbstractStylesheetFactory.class) {
      Generator gen = null;
    
      try {
        if (path == null && xsl != null) {
          Document doc = xsl.getOwnerDocument();
          if (doc == null && xsl instanceof Document)
            doc = (Document) xsl;

          DocumentType dtd = doc.getDoctype();
          String systemId = null;
          if (dtd != null)
            systemId = dtd.getSystemId();

          if (systemId != null)
            path = getStylePath().lookup(systemId);
        }
      
        if (path == null && xsl instanceof CauchoNode) {
          String filename = ((CauchoNode) xsl).getFilename();
          if (filename != null)
            path = getStylePath().lookup(filename);
        }

        if (path == null)
          path = getStylePath().lookup("anonymous.xsl");

        Path stylePath = path.getParent();
      
        Expr expr = XPath.parseExpr("//xtp:directive.page/@language");
        String language = expr.evalString(xsl);

        String userName = path.getUserPath();
        String mangledName = getMangledName(userName);
      
        String encoding = XPath.evalString("//xsl:output/@encoding", xsl);
        if (encoding != null && encoding.equals(""))
          encoding = null;

        if (language == null || language.equals("") || language.equals("java")) {
          language = "java";
          gen = new JavaGenerator(this, mangledName, encoding);
        }
        else
          throw new XslParseException(L.l("unsupported language `{0}'",
                                          language));

        gen.setPath(path);

        Iterator iter = XPath.select("//xtp:directive.page/@*", xsl);
        while (iter.hasNext()) {
          Attr attr = (Attr) iter.next();
          String name = attr.getNodeName();
          String value = attr.getNodeValue();

          if (name.equals("errorPage"))
            gen.setErrorPage(value);
          else if (name.equals("import"))
            gen.addImport(value);
          else if (name.equals("contentType"))
            gen.setContentType(value);
          else if (name.equals("language")) {
            if (! language.equalsIgnoreCase(value))
              throw new XslParseException(L.l("mismatched language `{0}'",
                                              value));
          }
          else if (name.equals("xml:space")) {
          }
          else
            throw new XslParseException(L.l("unknown directive `{0}'",
                                            name));
        }

        StylesheetImpl stylesheet = gen.generate(xsl);
        gen = null;
        stylesheet.init(path);
        // XXX: why was this here? stylesheet.init(getStylePath());
        stylesheet.setURIResolver(_uriResolver);

        return stylesheet;
      } catch (TransformerConfigurationException e) {
        throw e;
      } catch (Exception e) {
        throw new XslParseException(e);
      } finally {
        try {
          if (gen != null)
            gen.close();
        } catch (IOException e) {
        }
      }
    }
  }

  /**
   * Returns the mangled classname for the stylesheet.  If getClassName()
   * is not null, it will be used as the mangled name.
   *
   * @param userName the user specified name for the stylesheet.
   *
   * @return a valid Java classname for the generated stylesheet.
   */
  private String getMangledName(String userName)
  {
    String name = null;

    if (userName == null || userName.equals("anonymous.xsl") ||
        userName.equals("string") || userName.equals("stream")) {
      userName = "x" + (new Random().nextInt() & 0x3ff) + ".xsl";
    }
    
    if (getClassName() == null)
      name = userName;
    else
      name = getClassName();

    if (name.startsWith("/"))
      name = "xsl" + name;
    else
      name = "xsl/" + name;
    
    return JavaCompiler.mangleName(name);
  }
  
  /**
   * Returns existing compiled Templates if it exists.
   *
   * @param systemId source path for the stylesheet.
   *
   * @return a compiled stylesheet
   */
  StylesheetImpl loadPrecompiledStylesheet(String systemId,
                                           String userId)
  {
    return loadPrecompiledStylesheet(systemId, userId, _isAutoCompile);
  }
  
  /**
   * Returns existing compiled Templates if it exists.
   *
   * @param systemId source path for the stylesheet.
   *
   * @return a compiled stylesheet
   */
  StylesheetImpl loadPrecompiledStylesheet(String systemId,
                                           String userId,
                                           boolean checkModified)
  {
    if (! _loadPrecompiledStylesheet)
      return null;

    try {
      // look for compiled template base on SystemID
      StylesheetImpl stylesheet = loadStylesheet(systemId,
                                                 getMangledName(userId));

      if (stylesheet == null)
        return null;
      
      stylesheet.setURIResolver(_uriResolver);
      // and check if it's modified or not
      if (! checkModified || ! stylesheet.isModified()) {
        stylesheet.setURIResolver(_uriResolver);
        return stylesheet;
      }
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return null;
  }

  /**
   * Loads the compiled stylesheet .class file
   *
   * @param className the mangled classname for the stylesheet
   */
  protected StylesheetImpl loadStylesheet(String systemId, String className)
    throws Exception
  {
    LruCache<String,SoftReference<StylesheetImpl>> cache;

    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();

    cache = _stylesheetCache.getLevel(parentLoader);
    
    if (cache == null) {
      cache = new LruCache<String,SoftReference<StylesheetImpl>>(256);
      _stylesheetCache.set(cache, parentLoader);
    }

    SoftReference<StylesheetImpl> stylesheetRef;

    stylesheetRef = cache.get(className);

    StylesheetImpl stylesheet = null;

    if (stylesheetRef != null)
      stylesheet = stylesheetRef.get();

    try {
      if (stylesheet != null && ! stylesheet.isModified())
        return stylesheet;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    Path classPath = getWorkPath().lookup(className.replace('.', '/') + ".class");
    if (! classPath.canRead())
      throw new ClassNotFoundException("can't find compiled XSL `" + className + "'");

    DynamicClassLoader loader;
    loader = SimpleLoader.create(parentLoader, getWorkPath(), className);

    Class cl = null;

    // If the loading fails, remove the class because it may be corrupted
    try {
      cl = CauchoSystem.loadClass(className, false, loader);
    } catch (Error e) {
      try {
        classPath.remove();
      } catch (IOException e1) {
        log.log(Level.FINE, e1.toString(), e1);
      }
      
      throw e;
    }

    stylesheet = (StylesheetImpl) cl.newInstance();
    Path path;

      path = getSearchPath().lookup("").lookup(systemId);
      /*
    try {
    } catch (TransformerException e) {
      log.log(Level.FINE, e.toString(), e);
      
      path = Vfs.lookup(systemId);
    }
      */

    // stylesheet.init(path);
    stylesheet.init(getStylePath());
    stylesheet.setURIResolver(_uriResolver);

    stylesheetRef = new SoftReference<StylesheetImpl>(stylesheet);
    cache.put(className, stylesheetRef);

    return stylesheet;
  }
}

