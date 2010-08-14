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

package com.caucho.jsp;

import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.RequestAdapter;
import com.caucho.server.http.ResponseAdapter;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.RegistryException;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.CauchoDocument;
import com.caucho.xml.Html;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlParser;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathException;
import com.caucho.xsl.CauchoStylesheet;
import com.caucho.xsl.StylesheetImpl;
import com.caucho.xsl.TransformerImpl;
import com.caucho.xsl.XslParseException;

import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.*;


/**
 * XtpPage represents the compiled page.
 */
class XtpPage extends Page {
  private static final Logger log
    = Logger.getLogger(XtpPage.class.getName());

  private boolean _strictXml;
  private boolean _toLower = true;
  private boolean _entitiesAsText = false;

  private Path _sourcePath;
  private Path _pwd;
  
  private String _uri;
  private String _className;
  private String _errorPage;

  private WebApp _webApp;

  private XslManager _xslManager;

  private Page _page;
  
  private HashMap<String,SoftReference<Page>> _varyMap;
  private ArrayList<String> _paramNames;

  private JspManager _jspManager;

  private final Semaphore _compileSemaphore = new Semaphore(1, false);

  /**
   * Creates a new XTP page.
   *
   * @param path file containing the xtp page
   * @param uri the request uri for the page
   * @param className the mangled classname for the page
   * @param uriPwd uri working dir for include() or forward()
   * @param req the servlet request
   * @param xslManager manager for the XSL stylesheets
   * @param strictXml if true, use strict XML, now HTML
   */
  XtpPage(Path path, String uri, String className,
          WebApp app,
          XslManager xslManager, boolean strictXml)
    throws ServletException, RegistryException
  {
    _sourcePath = path;
    _sourcePath.setUserPath(uri);
    _pwd = _sourcePath.getParent();
    _className = className;
    _webApp = app;
    _strictXml = strictXml;
    _xslManager = xslManager;
    _uri = uri;

    ServletConfigImpl config = new ServletConfigImpl();
    config.setServletContext(_webApp);
    
    init(config);
  }

  /**
   * Sets the JspManager for the page.
   */
  void setManager(JspManager manager)
  {
    _jspManager = manager;
  }

  /**
   * When true, HTML in XTP is normalized to lower-case.
   */
  void setHtmlToLower(boolean toLower)
  {
    _toLower = toLower;
  }

  /**
   * When true, XML entities are parsed as text.
   */
  void setEntitiesAsText(boolean entitiesAsText)
  {
    _entitiesAsText = entitiesAsText;
  }

  /**
   * Returns true if the sources creating this page have been modified.
   */
  public boolean _caucho_isModified()
  {
    return false;
  }

  /**
   * Handle a request.
   *
   * @param req the servlet request
   * @param res the servlet response
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    CauchoRequest req;
    
    if (request instanceof CauchoRequest)
      req = (CauchoRequest) request;
    else
      req = RequestAdapter.create((HttpServletRequest) request, _webApp);
    
    CauchoResponse res;
    ResponseAdapter resAdapt = null;
    
    if (response instanceof CauchoResponse)
      res = (CauchoResponse) response;
    else {
      resAdapt = ResponseAdapter.create((HttpServletResponse) response);
      res = resAdapt;
    }

    try {
      service(req, res);
    } catch (InterruptedException e) {
      log.log(Level.FINE, e.toString(), e);
      
      log.warning("XTP: interrupted for " + req.getPageURI());
      
      res.sendError(503, "Server busy: XTP generation delayed");
    } finally {
      if (resAdapt != null)
        resAdapt.close();
    }
  }
    

  /**
   * Handle a request.
   *
   * @param req the servlet request
   * @param res the servlet response
   */
  public void service(CauchoRequest req, CauchoResponse res)
    throws IOException, ServletException, InterruptedException
  {
    Page page = getPage(req, res);

    if (page != null) {
      page.pageservice(req, res);
    }
    else {
      log.warning("XTP: server busy on " + req.getPageURI());

      res.setHeader("Retry-After", "15");
      res.sendError(503, "Server busy: XTP generation delayed");
    }
  }

  /**
   * Returns the page.
   */
  private Page getPage(CauchoRequest req, CauchoResponse res)
    throws IOException, ServletException, InterruptedException
  {
    String ss = null;
    String varyName = null;
    Page page = _page;
    Page deadPage = null;
    
    if (page == null) {
      if (_varyMap != null) {
        varyName = generateVaryName(req);

        if (varyName != null) {
          SoftReference<Page> ref = _varyMap.get(varyName);
          page = ref != null ? ref.get() : null;
        }
      }
    }

    if (page != null && ! page._caucho_isModified())
      return page;

    deadPage = page;
    page = null;

    long timeout = deadPage == null ? 30L : 5L;
    
    Thread.interrupted();
    if (_compileSemaphore.tryAcquire(timeout, TimeUnit.SECONDS)) {
      try {
        varyName = generateVaryName(req);

        page = getPrecompiledPage(req, varyName);

        if (page == null) {
          CauchoDocument doc;

          try {
            doc = parseXtp();
          } catch (FileNotFoundException e) {
            res.sendError(404);
            throw e;
          }

          Templates stylesheet = compileStylesheet(req, doc);

          // the new stylesheet affects the vary name
          varyName = generateVaryName(req);

          page = getPrecompiledPage(req, varyName);

          if (page == null)
            page = compileJspPage(req, res, doc, stylesheet, varyName);
        }

        if (page != null) {
          ServletConfigImpl config = new ServletConfigImpl();
          config.setServletContext(_webApp);

          page.init(config);

          if (varyName != null && _varyMap == null)
            _varyMap = new HashMap<String,SoftReference<Page>>(8);
        
          if (varyName != null)
            _varyMap.put(varyName, new SoftReference<Page>(page));
          else
            _page = page;
        }
        else if (deadPage != null) {
          _page = null;

          if (varyName != null && _varyMap != null)
            _varyMap.remove(varyName);
        }
      } finally {
        _compileSemaphore.release();
      }
    }
    else {
      log.warning("XTP: semaphore timed out on " + req.getPageURI());
    }
      

    if (page != null)
      return page;
    else
      return deadPage;
  }

  /**
   * Try to load a precompiled version of the page.
   *
   * @param req the request for the page.
   * @param varyName encoding for the variable stylesheet and parameters
   * @return the precompiled page or null
   */
  private Page getPrecompiledPage(CauchoRequest req, String varyName)
    throws IOException, ServletException
  {
    Page page = null;

    String className = getClassName(varyName);
    
    try {
      page = _jspManager.preload(className,
                                 _webApp.getClassLoader(),
                                 _webApp.getAppDir(),
                                 null);

      if (page != null) {
        if (log.isLoggable(Level.FINE))
          log.fine("XTP using precompiled page " + className);
      
        return page;
      }
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  /**
   * Parses the XTP file as either an XML document or an HTML document.
   */
  private CauchoDocument parseXtp()
    throws IOException, ServletException
  {
    ReadStream is = _sourcePath.openRead();
    try {
      XmlParser parser;
      
      if (_strictXml) {
        parser = new Xml();
        parser.setEntitiesAsText(_entitiesAsText);
      }
      else {
        parser = new Html();
        parser.setAutodetectXml(true);
        parser.setEntitiesAsText(true);
        // parser.setXmlEntitiesAsText(entitiesAsText);
        parser.setToLower(_toLower);
      }

      parser.setResinInclude(true);
      parser.setJsp(true);

      return (CauchoDocument) parser.parseDocument(is);
    } catch (Exception e) {
      JspParseException jspE = JspParseException.create(e);
      
      jspE.setErrorPage(_errorPage);
      
      throw jspE;
    } finally {
      is.close();
    }
  }

  /**
   * Compiles a stylesheet pages on request parameters and the parsed
   * XML file.
   *
   * @param req the servlet request.
   * @param doc the parsed XTP file as a DOM tree.
   *
   * @return the compiled stylesheet
   */
  private Templates compileStylesheet(CauchoRequest req, CauchoDocument doc)
    throws IOException, ServletException
  {
    String ssName = (String) req.getAttribute("caucho.xsl.stylesheet");

    Templates stylesheet = null;

    try {
      if (ssName == null)
        ssName = getStylesheetHref(doc, null);
    
      stylesheet = _xslManager.get(ssName, req);
    } catch (XslParseException e) {
      JspParseException jspE;
      if (e.getException() != null)
        jspE = new JspParseException(e.getException());
      else
        jspE = new JspParseException(e);

      jspE.setErrorPage(_errorPage);

      throw jspE;
    } catch (Exception e) {
      JspParseException jspE;
      
      jspE = new JspParseException(e);

      jspE.setErrorPage(_errorPage);

      throw jspE;
    }

    ArrayList<String> params = null;
    if (stylesheet instanceof StylesheetImpl) {
      StylesheetImpl ss = (StylesheetImpl) stylesheet;
      params = (ArrayList) ss.getProperty(CauchoStylesheet.GLOBAL_PARAM);
    }

    for (int i = 0; params != null && i < params.size(); i++) {
      String param = params.get(i);

      if (_paramNames == null)
        _paramNames = new ArrayList<String>();

      if (param.equals("xtp:context_path") ||
          param.equals("xtp:servlet_path"))
        continue;
      
      if (! _paramNames.contains(param))
        _paramNames.add(param);
    }

    return stylesheet;
  }

  /**
   * Mangles the page name to generate a unique page name.
   *
   * @param req the servlet request. 
   * @param res the servlet response.
   * @param stylesheet the stylesheet.
   * @param varyName the unique query.
   *
   * @return the compiled page.
   */
  private Page compileJspPage(CauchoRequest req,
                              CauchoResponse res,
                              CauchoDocument doc,
                              Templates stylesheet,
                              String varyName)
    throws IOException, ServletException
  {
    // changing paramNames changes the varyName
    varyName = generateVaryName(req);

    String className = getClassName(varyName);

    try {
      return getJspPage(doc, stylesheet, req, res, className);
    } catch (TransformerConfigurationException e) {
      throw new ServletException(e);
    } catch (JspException e) {
      throw new ServletException(e);
    }      
  }

  /**
   * Mangles the classname
   */
  private String getClassName(String varyName)
  {
    if (varyName == null)
      return _className;
    else
      return _className + JavaCompiler.mangleName("?" + varyName);
  }

  /**
   * Generates a unique string for the variable parameters.  The parameters
   * depend on:
   * <ul>
   * <li>The value of caucho.xsl.stylesheet selecting the stylesheet.
   * <li>The top-level xsl:param variables, which use request parameters.
   * <li>The request's path-info.
   * </ul>
   *
   * @param req the page request.
   *
   * @return a unique string encoding the important variations of the request.
   */
  private String generateVaryName(CauchoRequest req)
  {
    CharBuffer cb = CharBuffer.allocate();

    String ss = (String) req.getAttribute("caucho.xsl.stylesheet");

    if (ss == null && (_paramNames == null || _paramNames.size() == 0))
      return null;

    if (ss != null) {
      cb.append("ss.");
      cb.append(ss);
    }
    
    for (int i = 0; _paramNames != null && i < _paramNames.size(); i++) {
      String name = (String) _paramNames.get(i);

      String value;

      if (name.equals("xtp:path_info"))
        value = req.getPathInfo();
      else
        value = req.getParameter(name);
      
      cb.append(".");
      cb.append(name);
      
      if (value != null) {
        cb.append(".");
        cb.append(value);
      }
    }

    if (cb.length() == 0)
      return null;

    if (cb.length() < 64)
      return cb.close();

    long hash = 37;
    for (int i = 0; i < cb.length(); i++)
      hash = 65521 * hash + cb.charAt(i);

    cb.setLength(32);
    Base64.encode(cb, hash);

    return cb.close();
  }

  /**
   * Compile a JSP page.
   *
   * @param doc the parsed Serif page.
   * @param stylesheet the stylesheet
   * @param req the servlet request
   * @param res the servlet response
   * @param className the className of the generated page
   *
   * @return the compiled JspPage
   */
  private Page getJspPage(CauchoDocument doc, Templates stylesheet,
                          CauchoRequest req, CauchoResponse res,
                          String className)
    throws IOException, ServletException, JspException, TransformerConfigurationException
  {
    Path workDir = _jspManager.getClassDir();
    String fullClassName = className;
    Path path = workDir.lookup(fullClassName.replace('.', '/') + ".jsp");
    path.getParent().mkdirs();

    Properties output = stylesheet.getOutputProperties();
    
    String encoding = (String) output.get(OutputKeys.ENCODING);
    String mimeType = (String) output.get(OutputKeys.MEDIA_TYPE);
    String method = (String) output.get(OutputKeys.METHOD);

    if (method == null || encoding != null) {
    }
    else if (method.equals("xml"))
      encoding = "UTF-8";

    javax.xml.transform.Transformer transformer;
    transformer = stylesheet.newTransformer();
      
    for (int i = 0; _paramNames != null && i < _paramNames.size(); i++) {
      String param = (String) _paramNames.get(i);

      transformer.setParameter(param, req.getParameter(param));
    }

    String contextPath = req.getContextPath();
    if (contextPath != null && ! contextPath.equals(""))
      transformer.setParameter("xtp:context_path", contextPath);

    String servletPath = req.getServletPath();
    if (servletPath != null && ! servletPath.equals(""))
      transformer.setParameter("xtp:servlet_path", servletPath);

    String pathInfo = req.getPathInfo();
    if (pathInfo != null && ! pathInfo.equals(""))
      transformer.setParameter("xtp:path_info", pathInfo);

    transformer.setOutputProperty("caucho.jsp", "true");

    LineMap lineMap = null;
    WriteStream os = path.openWrite();
    try {
      if (encoding != null) {
        os.setEncoding(encoding);
        if (mimeType == null)
          mimeType = "text/html";
      
        os.print("<%@ page contentType=\"" + mimeType + "; charset=" +
                 encoding + "\" %>");
      }
      else if (mimeType != null)
        os.print("<%@ page contentType=\"" + mimeType + "\" %>");
    
      lineMap = writeJspDoc(os, doc, transformer, req, res);
    } finally {
      os.close();
    }

    StylesheetImpl ss = null;
    if (stylesheet instanceof StylesheetImpl)
      ss = (StylesheetImpl) stylesheet;

    try {
      path.setUserPath(_sourcePath.getPath());
      
      boolean cacheable = true; // jspDoc.isCacheable();
      ArrayList<PersistentDependency> depends =
        new ArrayList<PersistentDependency>();

      ArrayList<Depend> styleDepends = null;
      if (ss != null)
        styleDepends = (ArrayList) ss.getProperty(StylesheetImpl.DEPENDS);
      for (int i = 0; styleDepends != null && i < styleDepends.size(); i++) {
        Depend depend = styleDepends.get(i);

        Depend jspDepend = new Depend(depend.getPath(),
                                      depend.getLastModified(),
                                      depend.getLength());
        jspDepend.setRequireSource(true);

        if (! depends.contains(jspDepend))
          depends.add(jspDepend);
      }

      // Fill the page dependency information from the document into
      // the jsp page.
      ArrayList<Path> docDepends;
      docDepends = (ArrayList) doc.getProperty(CauchoDocument.DEPENDS);
      for (int i = 0; docDepends != null && i < docDepends.size(); i++) {
        Path depend = docDepends.get(i);

        Depend jspDepend = new Depend(depend);
        if (! depends.contains(jspDepend))
          depends.add(jspDepend);
      }

      // stylesheet cache dependencies are normal dependencies for JSP
      ArrayList<Path> cacheDepends = null;
      TransformerImpl xform = null;
      if (transformer instanceof TransformerImpl)
        xform = (TransformerImpl) transformer;
      if (xform != null)
        cacheDepends = (ArrayList) xform.getProperty(TransformerImpl.CACHE_DEPENDS);
      for (int i = 0; cacheDepends != null && i < cacheDepends.size(); i++) {
        Path depend = cacheDepends.get(i);
        Depend jspDepend = new Depend(depend);
        if (! depends.contains(jspDepend))
          depends.add(jspDepend);
      }

      ServletConfig config = null;
      Page page = _jspManager.createGeneratedPage(path, _uri, className,
                                                  config, depends);
      
      return page;
    } catch (IOException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new QJspException(e);
    }
  }

  /**
   * Transform XTP page with the stylesheet to JSP source.
   *
   * @param os the output stream to the generated JSP.
   * @param doc the parsed XTP file.
   * @param transformed the XSL stylesheet with parameters applied.
   * @param req the servlet request.
   * @param res the servlet response.
   *
   * @return the line map from the JSP to the original source.
   */
  private LineMap writeJspDoc(WriteStream os,
                              Document doc,
                              javax.xml.transform.Transformer transformer,
                              CauchoRequest req,
                              CauchoResponse res)
    throws IOException, ServletException
  {
    PageContext pageContext;

    JspFactory factory = JspFactory.getDefaultFactory();

    TransformerImpl xform = null;
    if (transformer instanceof TransformerImpl)
      xform = (TransformerImpl) transformer;
    String errorPage = null;
    if (xform != null)
      errorPage = (String) xform.getProperty("caucho.error.page");
    pageContext = factory.getPageContext(this,
                                         req, res,
                                         errorPage,
                                         false,
                                         8192, // bufferSize,
                                         false); // autoFlush);

    try {
      if (xform != null) {
        xform.setProperty("caucho.page.context", pageContext);
        xform.setProperty("caucho.pwd", Vfs.lookup());
      }

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(os);

      xform.setFeature(TransformerImpl.GENERATE_LOCATION, true);
      transformer.transform(source, result);

      if (xform != null)
        return (LineMap) xform.getProperty(TransformerImpl.LINE_MAP);
      else
        return null;
    } catch (Exception e) {
      pageContext.handlePageException(e);
    } finally {
      factory.releasePageContext(pageContext);
    }

    return null;
  }

  /**
   * Returns the stylesheet specified by the page.
   *
   * The syntax is:
   * <pre>
   *  &lt;?xml-stylesheet href='...' media='...'?>
   * </pre>
   *
   * @param doc the XTP document
   * @param media the http request media
   *
   * @return the href of the xml-stylesheet processing-instruction or 
   * "default.xsl" if none is found.
   */
  private String getStylesheetHref(Document doc, String media) 
    throws XPathException
  {
    Iterator iter = XPath.select("//processing-instruction('xml-stylesheet')",
                                 doc);
    while (iter.hasNext()) {
      ProcessingInstruction pi = (ProcessingInstruction) iter.next();
      String value = pi.getNodeValue();
      String piMedia = XmlUtil.getPIAttribute(value, "media");
      
      if (piMedia == null || piMedia.equals(media))
        return XmlUtil.getPIAttribute(value, "href");
    }

    return "default.xsl"; // xslManager.getDefaultStylesheet();
  }

  /**
   * Returns true if the document varies according to the "media".
   * (Currently unused.)
   */
  private boolean varyMedia(Document doc)
    throws XPathException
  {
    Iterator iter = XPath.select("//processing-instruction('xml-stylesheet')",
                                 doc);
    while (iter.hasNext()) {
      ProcessingInstruction pi = (ProcessingInstruction) iter.next();
      String value = pi.getNodeValue();
      String piMedia = XmlUtil.getPIAttribute(value, "media");
      
      if (piMedia != null)
        return true;
    }

    return false;
  }

  public boolean disableLog()
  {
    return true;
  }

  /**
   * Returns a printable version of the page object
   */
  public String toString()
  {
    return "XtpPage[" + _uri + "]";
  }
}
