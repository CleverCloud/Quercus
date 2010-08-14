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

package javax.servlet.http;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.OutputStreamWriter;
import java.util.ResourceBundle;

/**
 * HttpServlet is a convenient abstract class for creating servlets.
 * Normally, servlet writers will only need to override
 * <code>doGet</code> or <code>doPost</code>.
 *
 * <h4>Caching</h4>
 *
 * HttpServlet makes caching simple.  Just override
 * <code>getLastModified</code>.  As long as the page hasn't changed,
 * it can avoid the overhead of any heavy processing or database queries.
 * You cannot use <code>getLastModified</code> if the response depends
 * on sessions, cookies, or any headers in the servlet request.
 *
 * <h4>Hello, world</h4>
 *
 * The Hello.java belongs in myapp/WEB-INF/classes/test/Hello.java under the
 * application's root.  Normally, it will be called as
 * http://myhost.com/myapp/servlet/test.Hello.  If the server doesn't use
 * applications, then use /servlet/test.Hello.
 *
 * <code><pre>
 * package test;
 *
 * import java.io.*;
 * import javax.servlet.*;
 * import javax.servlet.http.*;
 *
 * public class Hello extends HttpServlet {
 *   public void doGet(HttpServletRequest request,
 *                          HttpServletResponse response)
 *     throws ServletException, IOException
 *   {
 *     response.setContentType("text/html");
 *     PrintWriter out = response.getWriter();
 *     out.println("Hello, World");
 *     out.close();
 *   }
 * }
 * </pre></code>
 */
public abstract class HttpServlet extends GenericServlet
  implements Serializable {
  /**
   * Service a request.  Normally not overridden.  If you need to override
   * this, use GenericServlet instead.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    service(req, res);
  }

  /**
   * Services a HTTP request.  Automatically dispatches based on the
   * request method and handles "If-Modified-Since" headers.  Normally
   * not overridden.
   *
   * @param req request information
   * @param res response object for returning data to the client.
   */
  protected void service(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String method = req.getMethod();
    boolean isHead = false;

    if (method.equals("GET") || (isHead = method.equals("HEAD"))) {
      long lastModified = getLastModified(req);
      if (lastModified <= 0) {
        if (isHead)
          doHead(req, res);
        else
          doGet(req, res);
        return;
      }

      char []newETag = null;
      String etag = req.getHeader("If-None-Match");
      if (etag != null) {
        newETag = generateETag(lastModified);
        int len = etag.length();

        if (len == newETag.length) {
          for (len--; len >= 0; len--) {
            if (etag.charAt(len) != newETag[len])
              break;
          }
        }

        if (len < 0) {
          res.sendError(res.SC_NOT_MODIFIED);
          return;
        }
      }

      long requestLastModified = req.getDateHeader("If-Modified-Since");
      if ((lastModified / 1000) == (requestLastModified / 1000)) {
        res.sendError(res.SC_NOT_MODIFIED);
        return;
      }

      if (newETag == null)
        newETag = generateETag(lastModified);
      res.setHeader("ETag", new String(newETag));
      res.setDateHeader("Last-Modified", lastModified);
      if (isHead)
        doHead(req, res);
      else
        doGet(req, res);
    }
    else if (method.equals("POST")) {
      doPost(req, res);
    }
    else if (method.equals("PUT")) {
      doPut(req, res);
    }
    else if (method.equals("DELETE")) {
      doDelete(req, res);
    }
    else if (method.equals("OPTIONS")) {
      doOptions(req, res);
    }
    else if (method.equals("TRACE")) {
      doTrace(req, res);
    }
    else {
      res.sendError(res.SC_NOT_IMPLEMENTED, "Method not implemented");
    }
  }

  private static char []generateETag(long data)
  {
    char []buf = new char[13];

    buf[0] = '"';
    buf[1] = encodeBase64(data >> 60);
    buf[2] = encodeBase64(data >> 54);
    buf[3] = encodeBase64(data >> 48);
    buf[4] = encodeBase64(data >> 42);

    buf[5] = encodeBase64(data >> 36);
    buf[6] = encodeBase64(data >> 30);
    buf[7] = encodeBase64(data >> 24);
    buf[8] = encodeBase64(data >> 18);

    buf[9] = encodeBase64(data >> 12);
    buf[10] = encodeBase64(data >> 6);
    buf[11] = encodeBase64(data);
    buf[12] = '"';

    return buf;
  }

  private static char encodeBase64(long d)
  {
    d &= 0x3f;
    if (d < 26)
      return (char) (d + 'A');
    else if (d < 52)
      return (char) (d + 'a' - 26);
    else if (d < 62)
      return (char) (d + '0' - 52);
    else if (d == 62)
      return '+';
    else
      return '/';
  }

  /**
   * Returns the last-modified time for the page for caching.
   * If at all possible, pages should override <code>getLastModified</code>
   * to improve performance.  Servlet engines like Resin can
   * cache the results of the page, resulting in near-static performance.
   *
   * @param req the request
   * @return the last-modified time of the page.
   */
  protected long getLastModified(HttpServletRequest req)
  {
    return -1;
  }

  /**
   * Process a HEAD request.  By default, uses doGet.
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doHead(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    doGet(req, res);
  }

  /**
   * Process a GET or HEAD request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }

  /**
   * Process a POST request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }

  /**
   * Process a PUT request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doPut(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }


  /**
   * Process a DELETE request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doDelete(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }


  /**
   * Process an OPTIONS request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doOptions(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }


  /**
   * Process a TRACE request
   *
   * @param req the client request
   * @param res response to the client
   */
  protected void doTrace(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    String protocol = req.getProtocol();
    String msg = req.getMethod() + " not supported";
    if (protocol.endsWith("1.1")) {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }
}

/**
 * @since 3.0
 */
class NoBodyResponse
  extends HttpServletResponseWrapper
{

  NoBodyResponse(HttpServletResponse r)
  {
    super(r);
  }

  void setContentLength()
  {
  }

  public void setContentLength(int len)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public ServletOutputStream getOutputStream()
    throws IOException
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public PrintWriter getWriter()
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException("unimplemented");
  }
}

/**
 * @since 3.0
 */
class NoBodyOutputStream
  extends ServletOutputStream
{

  NoBodyOutputStream()
  {
  }

  int getContentLength()
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public void write(int b)
  {
    throw new UnsupportedOperationException("unimplemented");
  }

  public void write(byte buf[], int offset, int len)
    throws IOException
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }
}