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

package com.caucho.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.cloud.loadbalance.LoadBalanceBuilder;
import com.caucho.cloud.loadbalance.LoadBalanceManager;
import com.caucho.cloud.loadbalance.LoadBalanceService;
import com.caucho.config.types.Period;
import com.caucho.network.balance.ClientSocket;
import com.caucho.server.http.CauchoRequest;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

/**
 * HTTP proxy
 *
 * <pre>
 * &lt;servlet>
 *   &lt;servlet-name>http-proxy&lt;/servlet-name>
 *   &lt;servlet-class>com.caucho.servlets.HttpProxyServlet&lt;/servlet-class>
 *   &lt;init host='localhost:8081'/>
 * &lt;/servlet>
 * </pre>
 */
@SuppressWarnings("serial")
public class HttpProxyServlet extends GenericServlet {
  private static final Logger log =
    Logger.getLogger(HttpProxyServlet.class.getName());
  private static final L10N L = new L10N(HttpProxyServlet.class);

  private LoadBalanceBuilder _loadBalanceBuilder;
  private LoadBalanceManager _loadBalancer;

  public HttpProxyServlet()
  {
    LoadBalanceService loadBalanceService = LoadBalanceService.getCurrent();
    
    if (loadBalanceService == null) {
      throw new IllegalStateException(L.l("'{0}' requires an active {1}",
                                          this,
                                          LoadBalanceService.class.getSimpleName()));
    }
    
    _loadBalanceBuilder = loadBalanceService.createBuilder();
  }

  /**
   * Adds an address
   */
  public void addAddress(String address)
  {
    _loadBalanceBuilder.addAddress(address);
  }

  /**
   * Adds a host
   */
  public void addHost(String host)
  {
    addAddress(host);
  }

  /**
   * Sets the fail recover time.
   */
  public void setFailRecoverTime(Period period)
  {
    // _tcpPool.setFailRecoverTime(period);
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  @Override
  public void init()
    throws ServletException
  {
    _loadBalancer = _loadBalanceBuilder.create();
  }

  /**
   * Handle the request.
   */
  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    CauchoRequest cReq = null;

    if (req instanceof CauchoRequest)
      cReq = (CauchoRequest) req;

    String sessionId = req.getRequestedSessionId();

    String uri;
    if (req.isRequestedSessionIdFromURL()) {
      uri =  (req.getRequestURI() + ";jsessionid=" +
              req.getRequestedSessionId());
    }
    else
      uri = req.getRequestURI();

    String queryString = null;

    if (cReq != null)
      queryString = cReq.getPageQueryString();
    else {
      queryString = (String) req.getAttribute("javax.servlet.include.query_string");

      if (queryString == null)
        queryString = req.getQueryString();
    }

    if (queryString != null)
      uri += '?' + queryString;

    ClientSocket stream = _loadBalancer.openSticky(sessionId, null);

    try {
      long startRequestTime = Alarm.getCurrentTime();
      
      if (stream == null) {
        log.warning(L.l("{0}: no backend servers available to process {1}",
                        this, req.getRequestURI()));

        res.sendError(503); // send a busy
      }
      else if (handleRequest(req, res, uri, stream)) {
        stream.free(startRequestTime);
        stream = null;
        return;
      }
    } finally {
      if (stream != null)
        stream.close();
    }
  }

  private boolean handleRequest(HttpServletRequest req,
                                HttpServletResponse res,
                                String uri,
                                ClientSocket stream)
    throws ServletException, IOException
  {
    ReadStream rs = stream.getInputStream();
    WriteStream out = stream.getOutputStream();

    try {
      out.print(req.getMethod());
      out.print(' ');
      out.print(uri);
      out.print(" HTTP/1.1\r\n");

      out.print("Host: ");
      String host = req.getHeader("Host");
      if (host != null)
        out.println(host);
      else
        out.println(req.getServerName() + ":" + req.getServerPort());

      out.print("X-Forwarded-For: ");
      out.println(req.getRemoteAddr());

      Enumeration<String> e = req.getHeaderNames();
      while (e.hasMoreElements()) {
        String name = e.nextElement();

        if (name.equalsIgnoreCase("Connection"))
          continue;

        Enumeration<String> e1 = req.getHeaders(name);
        while (e1.hasMoreElements()) {
          String value = (String) e1.nextElement();

          out.print(name);
          out.print(": ");
          out.println(value);
        }
      }

      int contentLength = req.getContentLength();

      InputStream is = req.getInputStream();

      TempBuffer tempBuffer = TempBuffer.allocate();
      byte []buffer = tempBuffer.getBuffer();

      boolean isFirst = true;

      if (contentLength >= 0) {
        isFirst = false;
        out.print("\r\n");
      }

      int len;
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        if (isFirst) {
          out.print("Transfer-Encoding: chunked\r\n");
        }
        isFirst = false;

        if (contentLength < 0) {
          out.print("\r\n");
          out.print(Integer.toHexString(len));
          out.print("\r\n");
        }

        out.write(buffer, 0, len);
      }

      if (isFirst) {
        out.print("Content-Length: 0\r\n");
      }
      else
        out.print("\r\n0\r\n");

      out.print("\r\n");

      TempBuffer.free(tempBuffer);

      out.flush();
      
      return parseResults(rs, req, res);
    } catch (IOException e1) {
      log.log(Level.FINE, e1.toString(), e1);

      return false;
    }
  }

  private boolean parseResults(ReadStream is,
                               HttpServletRequest req,
                               HttpServletResponse res)
    throws IOException
  {
    String line = parseStatus(is);

    boolean isKeepalive = true;

    if (! line.startsWith("HTTP/1.1"))
      isKeepalive = false;

    int statusCode = parseStatusCode(line);

    String location = null;

    boolean isChunked = false;
    int contentLength = -1;

    while (true) {
      line = is.readLine();

      if (line == null)
        break;

      int p = line.indexOf(':');

      if (p < 0)
        break;

      String name = line.substring(0, p);
      String value = line.substring(p + 1).trim();

      if (name.equalsIgnoreCase("transfer-encoding")) {
        isChunked = true;
      }
      else if (name.equalsIgnoreCase("content-length")) {
        contentLength = Integer.parseInt(value);
      }
      else if (name.equalsIgnoreCase("location"))
        location = value;
      else if (name.equalsIgnoreCase("connection")) {
        if ("close".equalsIgnoreCase(value))
          isKeepalive = false;
      }
      else {
        // XXX: split header
        res.addHeader(name, value);
      }
    }

    if (location == null) {
    }
    /* server/1965
    else if (location.startsWith(hostURL)) {
      location = location.substring(hostURL.length());

      String prefix;
      if (req.isSecure()) {
        if (req.getServerPort() != 443)
          prefix = ("https://" + req.getServerName() +
                    ":" + req.getServerPort());
        else
          prefix = ("https://" + req.getServerName());
      }
      else {
        if (req.getServerPort() != 80)
          prefix = ("http://" + req.getServerName() +
                    ":" + req.getServerPort());
        else
          prefix = ("http://" + req.getServerName());
      }

      if (! location.startsWith("/"))
        location = prefix + "/" + location;
      else
        location = prefix + location;
    }
    */

    if (location != null)
      res.setHeader("Location", location);

    if (statusCode == 302 && location != null)
      res.sendRedirect(location);
    else if (statusCode != 200)
      res.setStatus(statusCode);

    OutputStream os = res.getOutputStream();

    if (isChunked)
      writeChunkedData(os, is);
    else if (contentLength > 0) {
      res.setContentLength(contentLength);

      writeContentLength(os, is, contentLength);
    }

    return isKeepalive;
  }

  private String parseStatus(ReadStream is)
    throws IOException
  {
    int ch;

    for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
    }

    StringBuilder sb = new StringBuilder();
    for (; ch >= 0 && ch != '\n'; ch = is.read()) {
      if (ch != '\r')
        sb.append((char) ch);
    }

    return sb.toString();
  }

  private int parseStatusCode(String line)
  {
    int len = line.length();

    int i = 0;
    int ch;

    for (; i < len && (ch = line.charAt(i)) != ' '; i++) {
    }

    for (; i < len && (ch = line.charAt(i)) == ' '; i++) {
    }

    int statusCode = 0;

    for (; i < len && '0' <= (ch = line.charAt(i)) && ch <= '9'; i++) {
      statusCode = 10 * statusCode + ch - '0';
    }

    if (statusCode == 0)
      return 400;
    else
      return statusCode;
  }

  private void writeChunkedData(OutputStream os, ReadStream is)
    throws IOException
  {
    int ch;

    while (true) {
      for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
      }

      int len = 0;
      for (; ch >= 0; ch = is.read()) {
        if ('0' <= ch && ch <= '9')
          len = 16 * len + ch - '0';
        else if ('a' <= ch && ch <= 'f')
          len = 16 * len + ch - 'a' + 10;
        else if ('A' <= ch && ch <= 'F')
          len = 16 * len + ch - 'A' + 10;
        else
          break;
      }

      if (ch == '\r')
        ch = is.read();

      if (ch != '\n')
        throw new IllegalStateException(L.l("unexpected chunking at '{0}'",
                                            (char) ch));

      if (len == 0)
        break;

      is.writeToStream(os, len);
    }

    ch = is.read();
    if (ch == '\r')
      ch = is.read();

    // XXX: footer
  }

  private void writeContentLength(OutputStream os, ReadStream is, int length)
    throws IOException
  {
    is.writeToStream(os, length);
  }
}
