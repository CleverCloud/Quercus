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

package com.caucho.servlets.webdav;

import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.util.HTTPUtil;
import com.caucho.util.QDate;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.XmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves the WebDAV protocol.  The underlying AbstractPath controls
 * the actual files served and modified.  The default AbstractPath
 * just uses getRealPath from the current ServletContext.
 *
 * <p>More sophisticated users can customize AbstractPath to provide their
 * own WebDAV view for their objects, much like the Linux /proc
 * filesystem provides a view to Linux kernel modules.
 *
 * <pre>
 * &lt;resource-ref res-ref-name='resin/webdav'>
 *   &lt;class-name>test.foo.MyDataSource&lt;/class-name>
 *   &lt;init-param my-foo='bar'/>
 * &lt;/resource-ref>
 *
 * &lt;servlet-mapping url-pattern='/webdav/*'
 *                  servlet-name='com.caucho.http.webdav.WebDavServlet'>
 *   &lt;init-param enable='write'/>
 *   &lt;init-param path-source='resin/webdav'/>
 * &lt;/servlet-mapping>
 * </pre>
 */
public class WebDavServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(WebDavServlet.class.getName());
  
  private QDate _calendar = new QDate();

  private boolean _enable = false;
  private boolean _enableWrite = false;

  private boolean _addCrLf = false;

  private String _user;
  private String _role;
  private boolean _needsSecure;
  private AbstractPath _path;
  private String _root;

  /**
   * Sets the enable value.
   */
  public void setEnable(String enable)
  {

    if (enable == null || enable.equals(""))
      return;
    else if (enable.equals("read"))
      _enable = true;
    else if (enable.equals("write") ||
             enable.equals("all") ||
             enable.equals("yes") ||
             enable.equals("true")) {
      _enable = true;
      _enableWrite = true;
    }
  }

  /**
   * Sets the allowed role.
   */
  public void setRole(String role)
  {
    _role = role;
  }

  /**
   * Sets the allowed user.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Set true for securted.
   */
  public void setSecure(boolean needsSecure)
  {
    _needsSecure = needsSecure;
  }

  /**
   * Sets the path.
   */
  public void setPathSource(AbstractPath path)
  {
    _path = path;
  }

  /**
   * Sets the root.
   */
  public void setRoot(String root)
  {
    _root = root;
  }

  /**
   * Sets true if should add cr/lf
   */
  public void setCrLf(boolean addCrLf)
  {
    _addCrLf = addCrLf;
  }

  public void init()
    throws ServletException
  {
    String enable = getInitParameter("enable");

    if (enable != null)
      setEnable(enable);
    
    String role = getInitParameter("role");
    if (role != null)
      setRole(role);
    
    if (_role == null)
      _role = "webdav";
    else if (_role.equals("*"))
      _role = null;
    
    String user = getInitParameter("user");
    if (user != null)
      setUser(user);

    String secure = getInitParameter("secure");
    if (secure == null) {
    }
    else if ("false".equalsIgnoreCase(secure) ||
             "no".equalsIgnoreCase(secure))
      _needsSecure = false;
    else
      _needsSecure = true;

    String pathSource = getInitParameter("path-source");
    try {
      if (pathSource != null) {
        Context env = (Context) new InitialContext().lookup("java:comp/env");
        _path = (AbstractPath) env.lookup(pathSource);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      if (pathSource != null && _path == null) {
        _path = (AbstractPath) new InitialContext().lookup(pathSource);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }

    String root = getInitParameter("root");

    if (_path != null) {
    }
    else if (_root != null) {
      Path pwd = ((WebApp) getServletContext()).getAppDir();

      _path = new FilePath(pwd.lookup(_root));
    }
    else if (root != null) {
      Path pwd = ((WebApp) getServletContext()).getAppDir();

      _path = new FilePath(pwd.lookup(root));
    }
    else
      _path = new ApplicationPath();
  }

  /**
   * Service the webdav request.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (! _enable) {
      res.sendError(res.SC_FORBIDDEN);
      return;
    }

    if (_needsSecure && ! req.isSecure()) {
      res.sendError(res.SC_FORBIDDEN);
      return;
    }

    if (_role != null && ! req.isUserInRole(_role)) {
      res.sendError(res.SC_FORBIDDEN);
      return;
    }

    if (_user != null) {
      java.security.Principal principal = req.getUserPrincipal();
      if (principal == null) {
        res.sendError(res.SC_FORBIDDEN);
        return;
      }
      if (! principal.getName().equals(_user)) {
        res.sendError(res.SC_FORBIDDEN);
        return;
      }
    }
    
    ServletContext app = getServletContext();
    String requestURI = req.getRequestURI();
    String pathInfo = req.getPathInfo();
    
    String depthString = req.getHeader("Depth");
    int depth = Integer.MAX_VALUE;

    OutputStream os = res.getOutputStream();
    WriteStream out = Vfs.openWrite(os);
    out.setEncoding("UTF-8");

    if (_addCrLf)
      out.setNewlineString("\r\n");

    try {
      if ("0".equals(depthString))
        depth = 0;
      else if ("1".equals(depthString))
        depth = 1;

      if (req.getMethod().equals("OPTIONS")) {
        res.setHeader("DAV", "1");

        res.setHeader("MS-Author-Via", "DAV");
        if (_enableWrite)
          res.setHeader("Allow", "OPTIONS, PROPFIND, GET, HEAD, PUT, MKCOL, DELETE, COPY, MOVE, PROPPATCH");
        else if (_enable)
          res.setHeader("Allow", "OPTIONS, PROPFIND, GET, HEAD");
      }
      else if (req.getMethod().equals("PROPFIND")) {
        handlePropfind(req, res, out, depth);
      }
      else if (req.getMethod().equals("GET") ||
               req.getMethod().equals("HEAD")) {
        handleGet(req, res, out);
      }
      else if (req.getMethod().equals("PUT") && _enableWrite) {
        handlePut(req, res, out);
      }
      else if (req.getMethod().equals("MKCOL") && _enableWrite) {
        handleMkcol(req, res, out);
      }
      else if (req.getMethod().equals("DELETE") && _enableWrite) {
        handleDelete(req, res, out);
      }
      else if (req.getMethod().equals("COPY") && _enableWrite) {
        handleCopy(req, res, out, depth);
      }
      else if (req.getMethod().equals("MOVE") && _enableWrite) {
        handleMove(req, res, out);
      }
      else if (req.getMethod().equals("PROPPATCH") && _enableWrite) {
        handleProppatch(req, res, out, depth);
      }
      else if (! _enableWrite &&
               "PUT".equals(req.getMethod()) ||
               "MKCOL".equals(req.getMethod()) ||
               "DELETE".equals(req.getMethod()) ||
               "COPY".equals(req.getMethod()) ||
               "MOVE".equals(req.getMethod()) ||
               "PROPPATCH".equals(req.getMethod())) {
        res.sendError(res.SC_FORBIDDEN);
      }
      else {
        res.sendError(res.SC_NOT_IMPLEMENTED, "Method not implemented");
      }
    } finally {
      out.close();
    }
  }

  private void handlePropfind(HttpServletRequest req,
                              HttpServletResponse res,
                              WriteStream out,
                              int depth)
    throws ServletException, IOException
  {
    InputStream is = req.getInputStream();
    PropfindHandler handler = new PropfindHandler();
    XmlParser parser = new XmlParser();
    parser.setContentHandler(handler);
    
    try {
      parser.parse(is);
    } catch (SAXException e) {
      sendError(res, out, res.SC_BAD_REQUEST, "Bad Request for PROPFIND",
                String.valueOf(e));
      return;
    }

    WebApp app = (WebApp) getServletContext();
    Path appDir = app.getAppDir();

    String pathInfo = req.getPathInfo();
    String uriPwd = app.getContextPath() + req.getServletPath();
    
    if (pathInfo == null)
      pathInfo = "/";
    else
      uriPwd = uriPwd + pathInfo;

    if (_path.isDirectory(pathInfo, req, app) && ! uriPwd.endsWith("/"))
      uriPwd = uriPwd + "/";

    ServletContext rootApp = app.getContext("/");

    ArrayList<AttributeName> properties = handler.getProperties();
    boolean isPropname = handler.isPropname();

    if (properties.size() == 0)
      addAllProperties(properties, pathInfo, req, app);

    startMultistatus(res, out);

    printPathProperties(out, req, app, uriPwd, pathInfo,
                        properties, isPropname, depth);
    
    out.println("</D:multistatus>");
  }

  /**
   * Proppatch sets properties.  This implementation does not allow
   * any property setting.
   */
  private void handleProppatch(HttpServletRequest req,
                               HttpServletResponse res,
                               WriteStream out,
                               int depth)
    throws ServletException, IOException
  {
    InputStream is = req.getInputStream();
    ProppatchHandler handler = new ProppatchHandler();
    XmlParser parser = new XmlParser();
    parser.setContentHandler(handler);
    
    try {
      parser.parse(is);
    } catch (SAXException e) {
      sendError(res, out, res.SC_BAD_REQUEST, "Bad Request for PROPPATCH",
                "Bad Request: " + e);
      return;
    }
    
    WebApp app = (WebApp) getServletContext();
    Path appDir = app.getAppDir();

    String pathInfo = req.getPathInfo();
    String uriPwd = app.getContextPath() + req.getServletPath();
    
    if (pathInfo == null)
      pathInfo = "/";
    else
      uriPwd = uriPwd + pathInfo;

    if (_path.isDirectory(pathInfo, req, app) && ! uriPwd.endsWith("/"))
      uriPwd = uriPwd + "/";

    ArrayList forbidden = new ArrayList();
    
    startMultistatus(res, out);

    out.println("<D:response>");
    out.println("<D:href>" + escapeXml(uriPwd) + "</D:href>");

    ArrayList properties = new ArrayList();
    ArrayList<ProppatchCommand> commands = handler.getCommands();

    for (int i = 0; i < commands.size(); i++) {
      ProppatchCommand command = commands.get(i);
      int code = command.getCode();
      AttributeName name = command.getName();
      String value = command.getValue();
      int status;

      out.println("<D:propstat><D:prop><" + name.getName() + " xmlns:" +
                  name.getPrefix() + "=\"" + name.getNamespace() + "\"/>");

      if (code == ProppatchCommand.SET) {
        _path.setAttribute(name, value, pathInfo, req, app);

        out.println("<D:status>HTTP/1.1 200 OK</D:status>");
      }
      else if (code == ProppatchCommand.REMOVE) {
        _path.removeAttribute(name, pathInfo, req, app);

        out.println("<D:status>HTTP/1.1 200 OK</D:status>");
      }
      else
        out.println("<D:status>HTTP/1.1 424 Failed</D:status>");

      out.println("</D:prop></D:propstat>");
    }

    out.println("</D:response>");

    out.println("</D:multistatus>");
  }
  
  private void handlePut(HttpServletRequest req,
                         HttpServletResponse res,
                         WriteStream out)
    throws ServletException, IOException
  {
    ServletContext app = getServletContext();

    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    if (! _path.isDirectory(getParent(pathInfo), req, app)) {
      sendError(res, out, 409, "Conflict", "PUT requires a parent collection");
      return;
    }
    else if (! _path.exists(pathInfo, req, app))
      res.setStatus(201, "Created");
    else
      res.setStatus(204, "No Content");
    
    OutputStream os;

    try {
      os = _path.openWrite(pathInfo, req, app);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      sendError(res, out, 403, "Forbidden", "PUT forbidden");
      return;
    }
    
    WriteStream ws = Vfs.openWrite(os);
    Path path =ws.getPath();
    try {
      InputStream is = req.getInputStream();
      ws.writeStream(is);
    } finally {
      ws.close();
    }
  }

  /**
   * Creates a directory.
   */
  private void handleMkcol(HttpServletRequest req,
                           HttpServletResponse res,
                           WriteStream out)
    throws ServletException, IOException
  {
    res.setContentType("text/xml; charset=\"utf-8\"");
    
    ServletContext app = getServletContext();

    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    if (_path.exists(pathInfo, req, app)) {
      res.sendError(res.SC_METHOD_NOT_ALLOWED, "Collection already exists");
      return;
    }

    if (! _path.isDirectory(getParent(pathInfo), req, app)) {
      res.sendError(res.SC_CONFLICT, "MKCOL needs parent collection");
      return;
    }

    InputStream is = req.getInputStream();
    int ch = is.read();

    if (ch >= 0) {
      res.sendError(res.SC_UNSUPPORTED_MEDIA_TYPE, "MKCOL doesn't understand content-type");
      return;
    }

    if (! _path.mkdir(pathInfo, req, app)) {
      res.sendError(res.SC_FORBIDDEN, "MKCOL forbidden");
      return;
    }

    res.setHeader("Location", req.getRequestURI());
    sendError(res, out, res.SC_CREATED, null, 
              "Created collection " +
              HTTPUtil.encodeString(req.getRequestURI()));
  }

  private void addAllProperties(ArrayList<AttributeName> properties,
                                String pathInfo,
                                HttpServletRequest req,
                                WebApp app)
    throws IOException, ServletException
  {
    properties.add(new AttributeName("DAV:", "resourcetype", "D:resourcetype"));
    properties.add(new AttributeName("DAV:", "getcontenttype", "D:getcontenttype"));
    properties.add(new AttributeName("DAV:", "getcontentlength", "D:getcontentlength"));
    properties.add(new AttributeName("DAV:", "creationdate", "D:creationdate"));
    properties.add(new AttributeName("DAV:", "getlastmodified", "D:getlastmodified"));

    Iterator<AttributeName> iter = _path.getAttributeNames(pathInfo, req, app);
    while (iter.hasNext()) {
      AttributeName name = iter.next();

      if (! properties.contains(name))
        properties.add(name);
    }
  }

  private void printPathProperties(WriteStream out,
                                   HttpServletRequest req,
                                   ServletContext app,
                                   String uri, String pathInfo,
                                   ArrayList<AttributeName> properties,
                                   boolean isPropname, int depth)
    throws IOException, ServletException
  {
    out.println("<D:response>");
    out.print("<D:href>");
    out.print(escapeXml(uri));
    out.println("</D:href>");

    if (! _path.exists(pathInfo, req, app)) {
      out.println("<D:propstat>");
      out.println("<D:status>HTTP/1.1 404 Not Found</D:status>");
      out.println("</D:propstat>");
      out.println("</D:response>");
      return;
    }
      
    ArrayList<AttributeName> unknownProperties = new ArrayList<AttributeName>();
      
    out.println("<D:propstat>");

    out.println("<D:prop>");

    boolean isDirectory = _path.isDirectory(pathInfo, req, app);

    for (int j = 0; j < properties.size(); j++) {
      AttributeName prop = properties.get(j);
      String localName = prop.getLocal();
      String propUri = prop.getNamespace();
      String qName = prop.getName();
      String prefix = prop.getPrefix();

      if (isPropname) {
        if (propUri.equals("DAV:"))
          out.println("<D:" + localName + "/>");
        else {
          String nsPrefix;

          if (prefix.equals("D")) {
            prefix = "caucho-D";
            qName = "caucho-D:" + localName;
          }

          if (prefix.equals(""))
            nsPrefix = "xmlns";
          else
            nsPrefix = "xmlns:" + prefix;
          
          out.println("<" + qName + " " + nsPrefix + "=\"" +
                      propUri + "\"/>");
        }
        continue;
      }

      String value = _path.getAttribute(prop, pathInfo, req, app);
      if (value != null) {
        String nsPrefix;

        if (prefix.equals("D")) {
          prefix = "caucho-D";
          qName = "caucho-D:" + localName;
        }

        if (prefix.equals(""))
          nsPrefix = "xmlns";
        else
          nsPrefix = "xmlns:" + prefix;
          
        out.print("<" + qName + " " + nsPrefix + "=\"" +
                  propUri + "\">");
        out.print(value);
        out.println("</" + prop.getName() + ">");
        continue;
      }

      if (! propUri.equals("DAV:")) {
        unknownProperties.add(prop);
      }
      else if (localName.equals("resourcetype")) {
        if (isDirectory) {
          out.print("<D:resourcetype>");
          out.print("<D:collection/>");
          out.println("</D:resourcetype>");
        }
        else {
          out.println("<D:resourcetype/>");
        }
      }
      else if (localName.equals("getcontentlength")) {
        out.print("<D:getcontentlength>");
        out.print(_path.getLength(pathInfo, req, app));
        out.println("</D:getcontentlength>");
      }
      else if (localName.equals("getlastmodified")) {
        out.print("<D:getlastmodified>");
        out.print(_calendar.formatGMT(_path.getLastModified(pathInfo, req, app)));
        out.println("</D:getlastmodified>");
      }
      else if (localName.equals("creationdate")) {
        out.print("<D:creationdate>");

        long time = _path.getLastModified(pathInfo, req, app);
        
        out.print(_calendar.formatGMT(time, "%Y-%m-%dT%H:%M:%SZ"));

        out.println("</D:creationdate>");
      }
      else if (localName.equals("displayname")) {
        out.print("<D:displayname>");

        String name = pathInfo;
        if (name.endsWith("/"))
          name = name.substring(0, name.length() - 1);
        int p = pathInfo.lastIndexOf('/');
        if (p > 0 && p < pathInfo.length())
          name = pathInfo.substring(p + 1);

        out.print(escapeXml(name));
        
        out.println("</D:displayname>");
      }
      else if (localName.equals("getcontenttype")) {
        String mimeType = app.getMimeType(uri);

        if (mimeType != null) {
          out.print("<D:getcontenttype>");
          out.print(mimeType);
          out.println("</D:getcontenttype>");
        }
        else {
          out.println("<D:getcontenttype/>");
        }
      }
      else
        unknownProperties.add(prop);
    }

    out.println("</D:prop>");
    out.println("<D:status>HTTP/1.1 200 OK</D:status>");
    out.println("</D:propstat>");

    if (unknownProperties.size() != 0) {
      out.println("<D:propstat>");
      out.println("<D:prop>");
        
      for (int j = 0; j < unknownProperties.size(); j++) {
        AttributeName prop = (AttributeName) unknownProperties.get(j);

        if (prop.getNamespace().equals("DAV:"))
          out.println("<D:" + prop.getLocal() + "/>");
        else {
          String nsPrefix;
          String prefix = prop.getPrefix();
          String qName = prop.getName();

          if (prefix.equals("D")) {
            prefix = "caucho-D";
            qName = "caucho-D:" + prop.getLocal();
          }

          if (prefix.equals(""))
            nsPrefix = "xmlns";
          else
            nsPrefix = "xmlns:" + prefix;
          
          out.println("<" + qName + " " + nsPrefix + "=\"" +
                      prop.getNamespace() + "\"/>");
        }
      }
      out.println("</D:prop>");
      out.println("<D:status>HTTP/1.1 404 Not Found</D:status>");
      out.println("</D:propstat>");
    }
      
    out.println("</D:response>");

    if (depth > 0 && _path.isDirectory(pathInfo, req, app)) {
      String []list = _path.list(pathInfo, req, app);
      ArrayList<String> sortedList = new ArrayList<String>();

      for (int i = 0; i < list.length; i++)
        sortedList.add(list[i]);
      Collections.sort(sortedList);
      
      for (int i = 0; i < sortedList.size(); i++) {
        String filename = sortedList.get(i);
        
        String suburi;
        if (uri.endsWith("/"))
          suburi = uri + filename;
        else
          suburi = uri + "/" + filename;

        String subpath;
        if (pathInfo.endsWith("/"))
          subpath = pathInfo + filename;
        else
          subpath = pathInfo + "/" + filename;

        if (_path.isDirectory(subpath, req, app))
          suburi = suburi + '/';

        if (! _path.canRead(subpath, req, app) || filename.startsWith(".") ||
            filename.equals("CVS") || filename.endsWith("~"))
          continue;

        printPathProperties(out, req, app, suburi, subpath,
                            properties, isPropname, depth - 1);
      }
    }
  }
  
  private void handleDelete(HttpServletRequest req,
                            HttpServletResponse res,
                            WriteStream out)
    throws ServletException, IOException
  {
    ServletContext app = getServletContext();

    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    String uri = req.getContextPath() + pathInfo;

    if (_path.isFile(pathInfo, req, app)) {
      if (! _path.remove(pathInfo, req, app))
        res.sendError(403, "Forbidden");
      else
        res.setStatus(204, "No Content");
    }
    else if (_path.isDirectory(pathInfo, req, app)) {
      if (deleteRecursive(req, res, out, uri, pathInfo, false)) {
        out.println("<D:status>HTTP/1.0 403 Forbidden</D:status>");
        out.println("</D:response>");
        out.println("</D:multistatus>");
      }
      else
        res.setStatus(204, "No Content");
    }
    else {
      res.sendError(res.SC_NOT_FOUND);
    }
  }

  private boolean deleteRecursive(HttpServletRequest req,
                                  HttpServletResponse res,
                                  WriteStream out,
                                  String uri, String pathInfo,
                                  boolean hasError)
    throws IOException
  {
    ServletContext app = getServletContext();
    boolean newError = false;
    
    if (_path.isDirectory(pathInfo, req, app)) {
      String []list = _path.list(pathInfo, req, app);
      for (int i = 0; i < list.length; i++) {
        try {
          String suburi = lookup(uri, list[i]);
          String subpath = lookup(pathInfo, list[i]);
            
          hasError = deleteRecursive(req, res, out, suburi, subpath, hasError);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      if (! _path.rmdir(pathInfo, req, app))
        newError = true;
    }
    else if (! _path.remove(pathInfo, req, app))
      newError = true;

    if (newError) {
      if (! hasError) {
        startMultistatus(res, out);
        out.println("<D:response>");
      }
      
      out.println("<D:href>" + escapeXml(uri) + "</D:href>");

      hasError = true;
    }

    return hasError;
  }
  
  private void handleCopy(HttpServletRequest req,
                          HttpServletResponse res,
                          WriteStream out,
                          int depth)
    throws ServletException, IOException
  {
    ServletContext app = getServletContext();
    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    if (depth == 1)
      depth = Integer.MAX_VALUE;

    if (! _path.exists(pathInfo, req, app)) {
      res.sendError(res.SC_NOT_FOUND);
      return;
    }

    String destURI = getDestination(req);
    if (destURI == null) {
      res.sendError(403, "Forbidden");
      return;
    }

    String prefix = req.getContextPath();
    if (req.getServletPath() != null)
      prefix += req.getServletPath();
    if (! destURI.startsWith(prefix)) {
      res.sendError(403, "Forbidden");
      return;
    }

    String destPath = destURI.substring(prefix.length());

    if (destPath.equals(pathInfo)) {
      res.sendError(403, "Forbidden");
      return;
    }
    else if (destPath.startsWith(pathInfo) &&
             (pathInfo.endsWith("/") || destPath.startsWith(pathInfo + '/'))) {
      res.sendError(403, "Forbidden");
      return;
    }
    else if (pathInfo.startsWith(destPath) &&
             (destPath.endsWith("/") || pathInfo.startsWith(destPath + '/'))) {
      res.sendError(403, "Forbidden");
      return;
    }

    String overwrite = req.getHeader("Overwrite");
    if (overwrite == null)
      overwrite = "T";

    if (! _path.exists(destPath, req, app)) {
      res.setStatus(res.SC_CREATED);
    }
    else if (! overwrite.equals("F")) {
      removeRecursive(destPath, req);
      res.setStatus(204, "No Content");
    }
    else {
      res.sendError(412, "Overwrite not allowed for COPY");
      return;
    }

    if (! _path.exists(getParent(destPath), req, app)) {
      res.sendError(409, "COPY needs parent of destination");
      return;
    }

    if (_path.isFile(pathInfo, req, app)) {
      OutputStream os = _path.openWrite(destPath, req, app);
      WriteStream ws = Vfs.openWrite(os);
      try {
        InputStream is = _path.openRead(pathInfo, req, app);
        try {
          ws.writeStream(is);
        } finally {
          is.close();
        }
      } finally {
        ws.close();
      }
      return;
    }
    else {
      copyRecursive(pathInfo, destPath, depth, req);
      return;
    }
  }

  private void removeRecursive(String pathInfo, HttpServletRequest req)
    throws IOException
  {
    ServletContext app = getServletContext();
    
    if (_path.isDirectory(pathInfo, req, app)) {
      String []list = _path.list(pathInfo, req, app);

      for (int i = 0; i < list.length; i++) {
        try {
          removeRecursive(lookup(pathInfo, list[i]), req);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    _path.remove(pathInfo, req, app);
  }

  private void copyRecursive(String srcPath, String destPath, int depth,
                             HttpServletRequest req)
    throws IOException
  {
    ServletContext app = getServletContext();
    
    if (_path.isDirectory(srcPath, req, app)) {
      _path.mkdir(destPath, req, app);

      if (depth == 0)
        return;
      
      String []list = _path.list(srcPath, req, app);
      for (int i = 0; i < list.length; i++) {
        try {
          copyRecursive(lookup(srcPath, list[i]),
                        lookup(destPath, list[i]), depth - 1,
                        req);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
    else {
      OutputStream os = _path.openWrite(destPath, req, app);
      WriteStream ws = Vfs.openWrite(os);
      try {
        InputStream is = _path.openRead(srcPath, req, app);
        try {
          ws.writeStream(is);
        } finally {
          is.close();
        }
      } finally {
        ws.close();
      }
    }
  }
  
  private void handleMove(HttpServletRequest req,
                          HttpServletResponse res,
                          WriteStream out)
    throws ServletException, IOException
  {
    ServletContext app = getServletContext();
    
    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    int depth = Integer.MAX_VALUE;

    if (! _path.exists(pathInfo, req, app)) {
      res.sendError(res.SC_NOT_FOUND);
      return;
    }

    String destURI = getDestination(req);
    if (destURI == null) {
      res.sendError(403, "Forbidden");
      return;
    }

    String prefix = req.getContextPath();
    if (req.getServletPath() != null)
      prefix += req.getServletPath();
    if (! destURI.startsWith(prefix)) {
      res.sendError(403, "Forbidden");
      return;
    }

    String destPath = destURI.substring(prefix.length());

    if (destPath.equals(pathInfo)) {
      res.sendError(403, "Forbidden");
      return;
    }
    else if (destPath.startsWith(pathInfo) &&
             (pathInfo.endsWith("/") || destPath.startsWith(pathInfo + '/'))) {
      res.sendError(403, "Forbidden");
      return;
    }
    else if (pathInfo.startsWith(destPath) &&
             (destPath.endsWith("/") || pathInfo.startsWith(destPath + '/'))) {
      res.sendError(403, "Forbidden");
      return;
    }

    String overwrite = req.getHeader("Overwrite");
    if (overwrite == null)
      overwrite = "T";

    if (! _path.exists(destPath, req, app)) {
      res.setStatus(res.SC_CREATED);
    }
    else if (! overwrite.equals("F")) {
      removeRecursive(destPath, req);
      res.setStatus(204, "No Content");
    }
    else {
      res.sendError(412, "Overwrite not allowed for MOVE");
      return;
    }

    if (! _path.exists(getParent(destPath), req, app)) {
      res.sendError(409, "MOVE needs parent of destination");
      return;
    }

    if (_path.rename(pathInfo, destPath, req, app)) {
      // try renaming directly
      res.setStatus(204, "No Content");
    }
    else if (_path.isFile(pathInfo, req, app)) {
      HashMap<AttributeName,String> props = getProperties(pathInfo, req, app);
      OutputStream os = _path.openWrite(destPath, req, app);
      WriteStream ws = Vfs.openWrite(os);
      
      try {
        InputStream is = _path.openRead(pathInfo, req, app);
        try {
          ws.writeStream(is);
        } finally {
          is.close();
        }
      } finally {
        ws.close();
      }
      setProperties(props, destPath, req, app);

      _path.remove(pathInfo, req, app);
    }
    else {
      moveRecursive(pathInfo, destPath, req);
      res.setStatus(204, "No Content");
    }
  }

  private String getDestination(HttpServletRequest request)
  {
    String dest = request.getHeader("Destination");

    dest = java.net.URLDecoder.decode(dest);

    if (dest.startsWith("/"))
      return dest;

    String prefix = request.getScheme() + "://";
    String host = request.getHeader("Host");
    if (host != null)
      prefix = prefix + host.toLowerCase();

    if (dest.startsWith(prefix))
      return dest.substring(prefix.length());
    else
      return null;
  }

  private void moveRecursive(String srcPath, String destPath,
                             HttpServletRequest req)
    throws IOException
  {
    ServletContext app = getServletContext();
    
    if (_path.isDirectory(srcPath, req, app)) {
      _path.mkdir(destPath, req, app);
      
      String []list = _path.list(srcPath, req, app);
      for (int i = 0; i < list.length; i++) {
        try {
          moveRecursive(lookup(srcPath, list[i]),
                        lookup(destPath, list[i]),
                        req);
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
      
      _path.remove(srcPath, req, app);
    }
    else {
      HashMap<AttributeName,String> props = getProperties(srcPath, req, app);
      OutputStream os = _path.openWrite(destPath, req, app);
      WriteStream rs = Vfs.openWrite(os);
      
      try {
        InputStream is = _path.openRead(srcPath, req, app);
        try {
          rs.writeStream(is);
        } finally {
          is.close();
        }
      } finally {
        rs.close();
        os.close();
      }
      
      setProperties(props, destPath, req, app);
      _path.remove(srcPath, req, app);
    }
  }

  /**
   * Grabs all the properties from a path.
   */
  private HashMap<AttributeName,String> getProperties(String pathInfo,
                                                      HttpServletRequest req,
                                                      ServletContext app)
    throws IOException
  {
    HashMap<AttributeName,String> properties = null;

    Iterator<AttributeName> iter = _path.getAttributeNames(pathInfo, req, app);
    while (iter.hasNext()) {
      AttributeName name = iter.next();
      String value = _path.getAttribute(name, pathInfo, req, app);

      if (properties == null)
        properties = new HashMap<AttributeName,String>();
      
      properties.put(name, value);
    }

    return properties;
  }

  /**
   * Sets all the properties for a path.
   */
  private void setProperties(HashMap<AttributeName,String> map,
                             String pathInfo,
                             HttpServletRequest req,
                             ServletContext app)
    throws IOException
  {
    if (map == null)
      return;

    Iterator<AttributeName> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      AttributeName name = iter.next();
      
      String value = map.get(name);

      _path.setAttribute(name, value, pathInfo, req, app);
    }
  }
  
  private void handleGet(HttpServletRequest req,
                         HttpServletResponse res,
                         WriteStream out)
    throws ServletException, IOException
  {
    ServletContext app = getServletContext();

    String pathInfo = req.getPathInfo();
    if (pathInfo == null)
      pathInfo = "/";

    String mimeType = app.getMimeType(pathInfo);
    res.setContentType(mimeType);

    if (! _path.isFile(pathInfo, req, app) ||
        ! _path.canRead(pathInfo, req, app)) {
      res.sendError(res.SC_NOT_FOUND);
      return;
    }

    long length = _path.getLength(pathInfo, req, app);
    res.setContentLength((int) length);

    if ("HTTP/1.1".equals(req.getProtocol())) {
      res.setDateHeader("Last-Modified", _path.getLastModified(pathInfo, req, app));
      res.setHeader("Cache-Control", "private");
    }
    
    if (req.getMethod().equals("HEAD"))
      return;

    OutputStream os = res.getOutputStream();
    InputStream is = _path.openRead(pathInfo, req, app);
    ReadStream rs = Vfs.openRead(is);
    try {
      rs.writeToStream(os);
    } finally {
      rs.close();
    }
  }

  protected void startMultistatus(HttpServletResponse res,
                                  WriteStream out)
    throws IOException
  {
    res.setStatus(207, "Multistatus");
    res.setContentType("text/xml; charset=\"utf-8\"");

    out.println("<?xml version=\"1.0\"?>");
    out.println("<D:multistatus xmlns:D=\"DAV:\">");
  }
  
  protected void sendError(HttpServletResponse res,
                           WriteStream out,
                           int status, String statusText, String message)
    throws IOException
  {
    if (statusText == null)
      res.setStatus(status);
    else
      res.setStatus(status, statusText);

    res.setContentType("text/html");
    
    if (statusText != null) {
      out.print("<title>");
      out.print(statusText);
      out.println("</title>");
      out.print("<h1>");
      out.print(statusText);
      out.println("</h1>");
      out.println(message);
    }
    else {
      out.print("<title>");
      out.print(message);
      out.println("</title>");
      out.print("<h1>");
      out.print(message);
      out.println("</h1>");
    }
  }

  private void handleDirectory(HttpServletRequest req,
                               HttpServletResponse res,
                               WriteStream out,
                               String pathInfo)
    throws IOException, ServletException
  {
    ServletContext app = getServletContext();
    
    res.setContentType("text/html");
    
    out.println("<title>Directory of " + pathInfo + "</title>");
    out.println("<h1>Directory of " + pathInfo + "</h1>");

    String []list = _path.list(pathInfo, req, app);
    for (int i = 0; i < list.length; i++) {
      out.println("<a href=\"" + list[i] + "\">" + list[i] + "</a><br>");
    }
  }

  private String escapeXml(String data)
  {
    CharBuffer cb = CharBuffer.allocate();
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);

      switch (ch) {
      case '<':
        cb.append("&lt;");
        break;
      case '>':
        cb.append("&gt;");
        break;
      case '&':
        cb.append("&amp;");
        break;
      default:
        cb.append(ch);
        break;
      }
    }

    return cb.close();
  }

  protected String getParent(String pathInfo)
  {
    int p = pathInfo.lastIndexOf('/', pathInfo.length() - 2);

    if (p < 0)
      return "/";
    else
      return pathInfo.substring(0, p);
  }

  protected String lookup(String parent, String child)
  {
    if (parent.endsWith("/"))
      return parent + child;
    else
      return parent + '/' + child;
  }

  public void destroy()
  {
    _path.destroy();
  }

  static class PropfindHandler extends org.xml.sax.helpers.DefaultHandler {
    ArrayList<AttributeName> properties = new ArrayList<AttributeName>();
    boolean inProp;
    boolean isPropname;
    
    ArrayList<AttributeName> getProperties()
    {
      return properties;
    }

    boolean isPropname()
    {
      return isPropname;
    }

    public void startElement (String uri, String localName,
                              String qName, Attributes attributes)
      throws SAXException
    {
      if (localName.equals("prop"))
        inProp = true;
      else if (localName.equals("propname"))
        isPropname = true;
      else if (inProp) {
        if (qName.indexOf(':') > 0 && uri.equals(""))
          throw new SAXException("illegal empty namespace");

        properties.add(new AttributeName(uri, localName, qName));
      }
    }

    public void endElement (String uri, String localName, String qName)
        throws SAXException
    {
      if (localName.equals("prop"))
        inProp = false;
    }
  }

  static class ProppatchHandler extends org.xml.sax.helpers.DefaultHandler {
    ArrayList<ProppatchCommand> _commands = new ArrayList<ProppatchCommand>();
    boolean _inProp;
    boolean _inSet;
    boolean _inRemove;
    boolean _isPropname;
    AttributeName _attributeName;
    CharBuffer _value;
    
    boolean isPropname()
    {
      return _isPropname;
    }

    ArrayList<ProppatchCommand> getCommands()
    {
      return _commands;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException
    {
      if (localName.equals("set"))
        _inSet = true;
      else if (localName.equals("remove"))
        _inRemove = true;
      else if (localName.equals("prop"))
        _inProp = true;
      else if (localName.equals("propname"))
        _isPropname = true;
      else if (! _inProp) {
      }
      else if (_attributeName == null) {
        _attributeName = new AttributeName(uri, localName, qName);
        _value = CharBuffer.allocate();
      }
      else {
        int p = qName.indexOf(':');

        if (p > 0)
          _value.append("<" + qName + " xmlns:" + qName.substring(p + 1) +
                       "=\"" + uri + "\">");
        else
          _value.append("<" + qName + " xmlns=\"" + uri + "\">");
      }
    }

    public void characters(char []buffer, int offset, int length)
    {
      if (_value != null)
        _value.append(buffer, offset, length);
    }

    public void endElement (String uri, String localName, String qName)
        throws SAXException
    {
      if (localName.equals("prop"))
        _inProp = false;
      else if (localName.equals("set"))
        _inSet = false;
      else if (localName.equals("remove"))
        _inRemove = false;
      else if (_attributeName == null) {
      }
      else if (localName.equals(_attributeName.getLocal()) &&
               uri.equals(_attributeName.getNamespace())) {
        if (_inSet) {
          _commands.add(new ProppatchCommand(ProppatchCommand.SET,
                                             _attributeName,
                                             _value.close()));
        }
        else if (_inRemove) {
          _commands.add(new ProppatchCommand(ProppatchCommand.REMOVE,
                                             _attributeName,
                                             _value.close()));
        }

        _value = null;
        _attributeName = null;
      }
      else {
        _value.append("</" + qName + ">");
      }
    }
  }
  
  static class ProppatchCommand {
    public static int SET = 0;
    public static int REMOVE = 1;
    public static int CHANGE = 2;
    
    private int _code;
    private AttributeName _name;
    private String _value;

    ProppatchCommand(int code, AttributeName name, String value)
    {
      _code = code;
      _name = name;
      _value = value;
    }

    int getCode()
    {
      return _code;
    }

    AttributeName getName()
    {
      return _name;
    }

    String getValue()
    {
      return _value;
    }
  }
}
