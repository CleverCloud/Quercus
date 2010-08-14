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

package com.caucho.filters;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.RequestAdapter;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathException;
import com.caucho.xsl.AbstractStylesheetFactory;
import com.caucho.xsl.CauchoStylesheet;
import com.caucho.xsl.StyleScript;
import com.caucho.xsl.TransformerImpl;

import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends the results of the servlet through XSLT.
 *
 * @since Resin 2.0.6
 */
public class XsltFilter implements Filter {
  private static final L10N L = new L10N(XsltFilter.class);
  private static final Logger log
    = Logger.getLogger(XsltFilter.class.getName());
  
  private MergePath _stylePath;
  private ServletContext _application;
  private boolean _isConditional = true;

  public void setMimeType(String mimeType)
  {
  }

  public void setUnconditional(boolean isUnconditional)
  {
    _isConditional = ! isUnconditional;
  }
  
  public void init(FilterConfig config)
    throws ServletException
  {
    _stylePath = new MergePath();
    _stylePath.addMergePath(Vfs.lookup());
    DynamicClassLoader loader;
    loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
    String resourcePath = loader.getResourcePathSpecificFirst();
    _stylePath.addClassPath(resourcePath);

    _application = config.getServletContext();

    if ("true".equals(config.getInitParameter("unconditional")))
      _isConditional = false;
  }
  
  /**
   * Creates a wrapper to compress the output.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    XsltResponse xsltResponse = new XsltResponse(req, res);

    nextFilter.doFilter(req, xsltResponse);
    xsltResponse.finish(req, res);
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  class XsltResponse extends CauchoResponseWrapper {
    private HttpServletRequest _request;
    private XsltTempStream _xsltStream;
    private String _chainingType;
    
    XsltResponse(HttpServletRequest request, HttpServletResponse response)
    {
      super(response);

      _request = request;
    }

    /**
     * This needs to be bypassed because the file's content
     * length has nothing to do with the returned length.
     */
    public void setContentLength(int length)
    {
    }

    /**
     * Sets the content type of the filter.
     */
    public void setContentType(String contentType)
    {
      super.setContentType(contentType);

      int p = contentType.indexOf(';');

      if (p > 0)
        contentType = contentType.substring(0, p);

      if (! _isConditional ||
          contentType.equals("x-application/xslt") ||
          contentType.equals("x-application/xsl") ||
          contentType.equals("x-application/stylescript")) {
        _chainingType = contentType;

        if (log.isLoggable(Level.FINER))
          log.finer(L.l("'{0}' chaining xslt with {1}",
                        _request.getRequestURI(), contentType));

        if (_xsltStream == null)
          _xsltStream = new XsltTempStream(_response);
        
        _xsltStream.setChaining();
      }
    }

    /**
     * Calculates and returns the proper stream.
     */
    protected OutputStream getStream() throws IOException
    {
      if (_xsltStream == null)
        _xsltStream = new XsltTempStream(_response);

      return _xsltStream;
    }

    /**
     * Flushes the stream's buffer.
     */
    public void flushBuffer()
      throws IOException
    {
      super.flushBuffer();
      
      if (_stream != null)
        _stream.flush();
    }

    /**
     * Complets the request.
     */
    public void finish(HttpServletRequest req,
                       HttpServletResponse res)
      throws IOException, ServletException
    {
      try {
        flushBuffer();

        if (_chainingType == null)
          return;

        TempStream ts = _xsltStream.getTempStream();

        Document doc = null;
        
        ReadStream is = ts.openRead();
        Path userPath = Vfs.lookup();
        if (req instanceof CauchoRequest)
          userPath.setUserPath(((CauchoRequest) req).getPageURI());
        else
          userPath.setUserPath(req.getRequestURI());
        is.setPath(userPath);

        try {
          doc = new Xml().parseDocument(is);
        } finally {
          is.close();
        }
      
        String href = (String) req.getAttribute("caucho.xsl.stylesheet");

        if (href == null)
          href = getStylesheetHref(doc);

        if (href == null)
          href = "default.xsl";

        Templates stylesheet = null;
        
        //Path path = Vfs.lookup(href);
        try {
          //ReadStream sis = path.openReadAndSaveBuffer();

          TransformerFactory factory;
          
          if (_chainingType.equals("x-application/stylescript"))
            factory = new StyleScript();
          else {
            factory = TransformerFactory.newInstance();
          }

          if (factory instanceof AbstractStylesheetFactory)
            ((AbstractStylesheetFactory) factory).setStylePath(_stylePath);

          Path path = null;

          if (href.startsWith("/"))
            path = Vfs.getPwd().lookup(_application.getRealPath(href));
          else {
            String servletPath = RequestAdapter.getPageServletPath(req);

            Path pwd = Vfs.getPwd();
            pwd = pwd.lookup(_application.getRealPath(servletPath));
            path = pwd.getParent().lookup(href);
          }

          if (! path.canRead()) {
            Thread thread = Thread.currentThread();
            ClassLoader loader = thread.getContextClassLoader();

            URL url = loader.getResource(href);

            if (url != null) {
              Path newPath = Vfs.getPwd().lookup(url.toString());
              if (newPath.canRead())
                path = newPath;
            }
          }

          Source source;
          if (path.canRead())
            source = new StreamSource(path.getURL());
          else
            source = new StreamSource(href);

          if (log.isLoggable(Level.FINE))
            log.fine(L.l("'{0}' XSLT filter using stylesheet {1}",
                         req.getRequestURI(), source.getSystemId()));

          stylesheet = factory.newTemplates(source);
        } finally {
          // is.close();
        }
        
        Transformer transformer = null;

        transformer = (Transformer) stylesheet.newTransformer();

        TransformerImpl cauchoTransformer = null;
        if (transformer instanceof TransformerImpl)
          cauchoTransformer = (TransformerImpl) transformer;

        String mediaType = (String) transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
        String encoding = (String) transformer.getOutputProperty(OutputKeys.ENCODING);
        String method = (String) transformer.getOutputProperty(OutputKeys.METHOD);

        if (encoding != null) {
        }
        else if (method == null) {
        }
        else if (method.equals("xml"))
          encoding = "UTF-8";

        if (encoding != null) {
          if (mediaType == null)
            mediaType = "text/html";
          res.setContentType(mediaType + "; charset=" + encoding);
        }
        else if (mediaType != null)
          res.setContentType(mediaType);
        else
          res.setContentType("text/html");

        if (encoding == null)
          encoding = "ISO-8859-1";
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

        ArrayList<?> params = null;;
        if (cauchoTransformer != null) {
          params = (ArrayList<?>) cauchoTransformer.getProperty(CauchoStylesheet.GLOBAL_PARAM);
        }

        for (int i = 0; params != null && i < params.size(); i++) {
          String param = (String) params.get(i);
        
          transformer.setParameter(param, req.getParameter(param));
        }

        DOMSource domSource = new DOMSource(doc);
        domSource.setSystemId(userPath.getUserPath());

        Result result = getResult(res.getOutputStream());

        transformer.transform(domSource, result);
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        if (e instanceof CompileException)
          throw new ServletException(e.getMessage(), e);
        else
          throw new ServletException(e.toString(), e);
      }
    }

    /**
     * Returns the result object.
     */
    protected Result getResult(OutputStream out)
    {
      return new StreamResult(out);
    }

    /**
     * Returns the stylesheet specified by the page.
     *
     * The syntax is:
     * <pre>
     *  &lt;?xml-stylesheet href='...'?>
     * </pre>
     *
     * @return the href of the xml-stylesheet processing-instruction or 
     * "default.xsl" if none is found.
     */
    private String getStylesheetHref(Document doc)
      throws XPathException
    {
      ProcessingInstruction pi = null;

      pi = (ProcessingInstruction) XPath.find("//processing-instruction('xml-stylesheet')", doc);
      
      if (pi == null)
        return null;

      String value = pi.getNodeValue();
    
      return XmlUtil.getPIAttribute(value, "href");
    }
  }
  
  static class XsltTempStream extends OutputStream {
    private ServletResponse _response;
    private OutputStream _os;
    
    private TempStream _tempStream;

    XsltTempStream(ServletResponse response)
    {
      _response = response;
    }

    void setChaining()
    {
      if (_os != null)
        throw new IllegalStateException(L.l("setContentType for XSLT chaining must be before any data."));
      
      _tempStream = new TempStream();
      _tempStream.openWrite();

      _os = new WriteStream(_tempStream);
    }

    TempStream getTempStream()
      throws IOException
    {
      if (_tempStream != null) {
        _os.close();
        _os = null;
      }
      
      return _tempStream;
    }

    /**
     * Writes a buffer to the underlying stream.
     *
     * @param ch the byte to write
     */
    public void write(int ch)
      throws IOException
    {
      if (_os == null)
        _os = _response.getOutputStream();

      _os.write(ch);
    }

    /**
     * Writes a buffer to the underlying stream.
     *
     * @param buffer the byte array to write.
     * @param offset the offset into the byte array.
     * @param length the number of bytes to write.
     */
    public void write(byte []buffer, int offset, int length)
      throws IOException
    {
      if (_os == null)
        _os = _response.getOutputStream();

      _os.write(buffer, offset, length);
    }

    public void flush()
      throws IOException
    {
      if (_os == null)
        _os = _response.getOutputStream();

      _os.flush();
    }
  }
}
