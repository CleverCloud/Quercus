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

import com.caucho.java.LineMap;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.*;
import com.caucho.xpath.XPathFun;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class TransformerImpl extends javax.xml.transform.Transformer {
  protected static L10N L = new L10N(TransformerImpl.class);
  
  public final static String LINE_MAP = "caucho.line-map";
  public final static String CACHE_DEPENDS = "caucho.cache.depends";
  public final static String GENERATE_LOCATION = "caucho.generate.location";

  protected StylesheetImpl _stylesheet;
  protected HashMap<String,Object> _properties = new HashMap<String,Object>();
  protected HashMap<String,Object> _parameters;
  
  private URIResolver _uriResolver;
  private ErrorListener _errorListener;

  private Properties _output;
  
  protected LineMap _lineMap;

  protected ArrayList<Path> _cacheDepends = new ArrayList<Path>();

  protected TransformerImpl(StylesheetImpl stylesheet)
  {
    _stylesheet = stylesheet;
    _uriResolver = stylesheet.getURIResolver();
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
  
  public boolean getFeature(String name)
  {
    if (name.equals(DOMResult.FEATURE))
      return true;
    else if (name.equals(DOMSource.FEATURE))
      return true;
    else if (name.equals(StreamSource.FEATURE))
      return true;
    else if (name.equals(StreamResult.FEATURE))
      return true;
    else if (name.equals(SAXSource.FEATURE))
      return true;
    else if (name.equals(SAXResult.FEATURE))
      return true;
    else
      return false;
  }

  public void setFeature(String name, boolean enable)
  {
    if (name.equals(GENERATE_LOCATION))
      _stylesheet.setGenerateLocation(enable);
  }

  public StylesheetImpl getStylesheet()
  {
    return _stylesheet;
  }

  public Object getProperty(String name)
  {
    Object property = _properties.get(name);

    if (property != null)
      return property;
    
    if (name.equals(CACHE_DEPENDS))
      return _cacheDepends;
    else if (name.equals(LINE_MAP))
      return _lineMap;
    else
      return _stylesheet.getProperty(name);
  }

  public void setProperty(String name, Object value)
  {
    _properties.put(name, value);
  }

  /**
   * Sets a parameter that XPath expressions in the stylesheet can
   * use as $name.
   *
   * @param name the name of the XPath variable.
   * @param value the value for the variable.
   */
  public void setParameter(String name, Object value)
  {
    if (_parameters == null)
      _parameters = new HashMap<String,Object>();
    
    _parameters.put(name, value);
  }

  /**
   * Returns a copy of the xsl:output properties.
   *
   * @return a copy of the properties.
   */
  public Properties getOutputProperties()
  {
    if (_output == null)
      _output = (Properties) _stylesheet.getOutputProperties().clone();
    
    return (Properties) _output.clone();
  }

  /**
   * Sets the output properties.
   *
   * @param properties the new output properties.
   */
  public void setOutputProperties(Properties properties)
  {
    _output = properties;
  }

  /**
   * Sets a single xsl:output property.
   *
   * @param name the name of the property.
   * @param value the value of the property.
   */
  public void setOutputProperty(String name, String value)
  {
    if (_output == null)
      _output = (Properties) _stylesheet.getOutputProperties().clone();

    _output.put(name, value);
  }

  /**
   * Returns the value of a single named xsl:output property.
   *
   * @param name the name of the property.
   */
  public String getOutputProperty(String name)
  {
    if (_output == null)
      _output = (Properties) _stylesheet.getOutputProperties().clone();

    return (String) _output.get(name);
  }

  /**
   * Returns the named stylesheet parameter.
   *
   * @param name the name of the parameter.
   *
   * @ return the value of the named parameter.
   */
  public Object getParameter(String name)
  {
    if (_parameters == null)
      return null;
    else
      return _parameters.get(name);
  }

  /**
   * Clears all the external stylesheet parameters.
   */
  public void clearParameters()
  {
    if (_parameters != null)
      _parameters.clear();
    
    if (_cacheDepends != null)
      _cacheDepends.clear();
  }

  /**
   * Adds a new custom function.
   *
   * @param name the name of the function.
   * @param fun the new function.
   */
  public void addFunction(String name, XPathFun fun)
  {
    _stylesheet.addFunction(name, fun);
  }

  /**
   * Transforms the source into the result.
   *
   * @param source descriptor specifying the input source.
   * @param result descriptor specifying the output result.
   */
  public void transform(Source source, Result result)
    throws TransformerException
  {
    try {
      Node node = parseDocument(source);
      
      if (result instanceof StreamResult) {
        StreamResult stream = (StreamResult) result;

        if (stream.getOutputStream() != null)
          transform(node, stream.getOutputStream(), null, result.getSystemId());
        else if (stream.getWriter() != null) {
          Writer writer = stream.getWriter();
          WriteStream os = Vfs.openWrite(writer);

          if (writer instanceof OutputStreamWriter) {
            String javaEncoding = ((OutputStreamWriter) writer).getEncoding();
            String mimeEncoding = Encoding.getMimeName(javaEncoding);
            transform(node, os, mimeEncoding, result.getSystemId());
          }
          else
            transform(node, os, null, result.getSystemId());

          os.flush();
          os.free();
        }
        else {
          WriteStream os = Vfs.lookup(result.getSystemId()).openWrite();
            
          try {
            transform(node, os, null, result.getSystemId());
          } finally {
            os.close();
          }
        }
      }
      else if (result instanceof DOMResult) {
        DOMResult domResult = (DOMResult) result;

        Node resultNode = domResult.getNode();

        domResult.setNode(transform(node, resultNode));
      }
      else if (result instanceof SAXResult) {
        SAXResult sax = (SAXResult) result;

        transform(node, sax.getHandler(), sax.getLexicalHandler());
      }
      else
        throw new TransformerException(String.valueOf(result));
    } catch (TransformerException e) {
      throw e;
    } catch (Exception e) {
      throw new TransformerExceptionWrapper(e);
    }
  }

  public void transform(Node node, OutputStream os)
    throws TransformerException
  {
    if (os instanceof WriteStream) {
      String encoding = ((WriteStream) os).getEncoding();
      if (encoding == null)
        encoding = "ISO-8859-1";

      transform(node, os, encoding, null);
    }
    else
      transform(node, os, null, null);
  }
  /**
   * Transforms from a DOM node to an output stream.
   *
   * @param node the source node
   * @param os the destination stream
   */
  public void transform(Node node, OutputStream os,
                        String encoding, String systemId)
    throws TransformerException
  {
    if (node == null)
      throw new IllegalArgumentException("can't transform null node");

    try {
      _lineMap = null;
      Properties output = getOutputProperties();

      WriteStream ws;

      if (os instanceof WriteStream)
        ws = (WriteStream) os;
      else {
        ws = Vfs.openWrite(os);

        if (systemId != null)
          ws.setPath(Vfs.lookup(systemId));
        else if (node instanceof QNode) {
          String baseURI = ((QNode) node).getBaseURI();

          if (baseURI != null)
            ws.setPath(Vfs.lookup(baseURI));
        }
      }

      XmlPrinter out = new XmlPrinter(ws);

      String method = (String) output.get(OutputKeys.METHOD);
      out.setMethod(method);
      if (encoding == null)
        encoding = (String) output.get(OutputKeys.ENCODING);
      if (encoding == null && ! (os instanceof WriteStream) &&
          ! "html".equals(method))
        encoding = "UTF-8";
      if (encoding != null)
        out.setEncoding(encoding);
      out.setMimeType((String) output.get(OutputKeys.MEDIA_TYPE));
      String omit = (String) output.get(OutputKeys.OMIT_XML_DECLARATION);

      if (omit == null || omit.equals("false") || omit.equals("no"))
        out.setPrintDeclaration(true);
      
      out.setStandalone((String) output.get(OutputKeys.STANDALONE));
      out.setSystemId((String) output.get(OutputKeys.DOCTYPE_SYSTEM));
      out.setPublicId((String) output.get(OutputKeys.DOCTYPE_PUBLIC));
    
      String indent = (String) output.get(OutputKeys.INDENT);
      if (indent != null)
        out.setPretty(indent.equals("true") || indent.equals("yes"));
      
      String jsp = (String) output.get("caucho.jsp");
      if (jsp != null)
        out.setJSP(jsp.equals("true") || jsp.equals("yes"));
      
      out.setVersion((String) output.get(OutputKeys.VERSION));

      String includeContentType = (String) output.get("include-content-type");
      if (includeContentType != null)
        out.setIncludeContentType(includeContentType.equals("true") ||
                                  includeContentType.equals("yes"));
      
      if (! _stylesheet.getGenerateLocation()) {
      }
      else if (node instanceof CauchoNode) {
        String filename = ((CauchoNode) node).getFilename();
        if (filename != null)
          out.setLineMap(filename);
        else
          out.setLineMap("anonymous.xsl");
      }
      else
        out.setLineMap("anonymous.xsl");

      //out.beginDocument();
      _stylesheet.transform(node, out, this);
      //out.endDocument();
      _lineMap = out.getLineMap();
      if (os != ws) {
        ws.flush();
        ws.free();
      }
    } catch (TransformerException e) {
      throw e;
    } catch (Exception e) {
      throw new TransformerExceptionWrapper(e);
    }
  }

  /**
   * Transforms from the source node to the destination node, returning
   * the destination node.
   */
  public Node transform(Node sourceNode, Node destNode)
    throws SAXException, IOException
  {
    _lineMap = null;

    if (destNode == null)
      destNode = Xml.createDocument();

    DOMBuilder out = new DOMBuilder();
    out.init(destNode);

    try {
      out.startDocument();
      _stylesheet.transform(sourceNode, out, this);
      //out.endDocument();
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }

    return destNode;
  }

  /**
   * Transforms from the source node to the sax handlers.
   */
  public void transform(Node sourceNode,
                        ContentHandler contentHandler,
                        LexicalHandler lexicalHandler)
    throws SAXException, IOException, TransformerException
  {
    if (contentHandler == null)
      throw new IllegalArgumentException(L.l("null content handler"));
    
    _lineMap = null;

    SAXBuilder out = new SAXBuilder();
    out.setContentHandler(contentHandler);

    out.startDocument();
    _stylesheet.transform(sourceNode, out, this);
    //out.endDocument();
  }

  /**
   * Parses the source XML document from the source.
   *
   * @param source the JAXP source.
   *
   * @return the parsed document.
   */
  protected Node parseDocument(Source source)
    throws IOException, SAXException, TransformerException
  {
    if (source instanceof StreamSource) {
      StreamSource stream = (StreamSource) source;

      InputSource in = new InputSource();
      in.setSystemId(stream.getSystemId());
      in.setByteStream(stream.getInputStream());
      in.setCharacterStream(stream.getReader());

      XmlParser parser = Xml.create();

      Node node = parser.parseDocument(in);

      parser.free();

      return node;

      // return new QDocument();
    }
    else if (source instanceof DOMSource){
      Node node = ((DOMSource) source).getNode();
      
      return node != null ? node : new QDocument();
    }
    else if (source instanceof StringSource) {
      String string = ((StringSource) source).getString();

      if (string != null)
        return parseStringDocument(string, source.getSystemId());
      else
        return new QDocument();
    }
    else if (source instanceof SAXSource) {
      SAXSource saxSource = (SAXSource) source;
      
      XMLReader reader = saxSource.getXMLReader();

      if (reader == null)
        return new QDocument();
      
      InputSource inputSource = saxSource.getInputSource();

      Document doc = new QDocument();
      DOMBuilder builder = new DOMBuilder();
      builder.init(doc);
      reader.setContentHandler(builder);

      reader.parse(inputSource);
      
      return doc;
    }

    else
      throw new TransformerException(L.l("unknown source {0}", source));
  }
  
  /**
   * Parses the source XML document from the input stream.
   *
   * @param is the source input stream.
   * @param systemId the path of the source
   *
   * @return document DOM node for the parsed XML.
   */
  protected Node parseDocument(InputStream is, String systemId)
    throws IOException, SAXException
  {
    XmlParser parser = Xml.create();

    Node node = parser.parseDocument(is);

    parser.free();

    return node;
  }

  /**
   * Parses the source document specified by a URL
   *
   * @param url path to the document to be parsed.
   *
   * @return the parsed document.
   */
  protected Node parseDocument(String url)
    throws IOException, SAXException
  {
    XmlParser parser = Xml.create();

    Node node = parser.parseDocument(url);

    parser.free();

    return node;
  }
  
  /**
   * Parses a string as an XML document.
   *
   * @param source the string to use as the XML source
   * @param systemId the URL for the string document.
   *
   * @return the parsed document.
   */
  protected Node parseStringDocument(String source, String systemId)
    throws IOException, SAXException
  {
    XmlParser parser = Xml.create();

    Node node = parser.parseDocumentString(source);

    parser.free();

    return node;
  }

  public void addCacheDepend(Path path)
  {
    _cacheDepends.add(path);
  }

  protected void addCacheDepend(String path)
  {
    _cacheDepends.add(Vfs.lookup(path));
  }

  public ArrayList<Path> getCacheDepends()
  {
    return _cacheDepends;
  }
}
