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

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * HttpServletResponse extends ServletResponse allowing servlets to
 * set the status and headers.
 *
 * <h4>Useful Headers</h4>
 *
 * <table>
 * <tr><td>Expires
 *     <td>Lets clients cache the page.  Resin can use Expires to
 *         internally cache the page.
 * </table>
 */
public interface HttpServletResponse extends ServletResponse {
  public final static int SC_ACCEPTED = 202;
  public final static int SC_BAD_GATEWAY = 502;
  public final static int SC_BAD_REQUEST = 400;
  public final static int SC_CONFLICT = 409;
  public final static int SC_CONTINUE = 100;
  public final static int SC_CREATED = 201;
  public final static int SC_EXPECTATION_FAILED = 417;
  public final static int SC_FORBIDDEN = 403;
  public final static int SC_GATEWAY_TIMEOUT = 504;
  public final static int SC_GONE = 410;
  public final static int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
  public final static int SC_INTERNAL_SERVER_ERROR = 500;
  public final static int SC_LENGTH_REQUIRED = 411;
  public final static int SC_METHOD_NOT_ALLOWED = 405;
  public final static int SC_MOVED_PERMANENTLY = 301;
  public final static int SC_MOVED_TEMPORARILY = 302;
  public final static int SC_FOUND = 302;
  public final static int SC_MULTIPLE_CHOICES = 300;
  public final static int SC_NO_CONTENT = 204;
  public final static int SC_NON_AUTHORITATIVE_INFORMATION = 203;
  public final static int SC_NOT_ACCEPTABLE = 406;
  public final static int SC_NOT_FOUND = 404;
  public final static int SC_NOT_IMPLEMENTED = 501;
  public final static int SC_NOT_MODIFIED = 304;
  public final static int SC_OK = 200;
  public final static int SC_PARTIAL_CONTENT = 206;
  public final static int SC_PAYMENT_REQUIRED = 402;
  public final static int SC_PRECONDITION_FAILED = 412;
  public final static int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
  public final static int SC_REQUEST_ENTITY_TOO_LARGE = 413;
  public final static int SC_REQUEST_TIMEOUT = 408;
  public final static int SC_REQUEST_URI_TOO_LONG = 414;
  public final static int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
  public final static int SC_RESET_CONTENT = 205;
  public final static int SC_SEE_OTHER = 303;
  public final static int SC_SERVICE_UNAVAILABLE = 503;
  public final static int SC_SWITCHING_PROTOCOLS = 101;
  public final static int SC_UNAUTHORIZED = 401;
  public final static int SC_UNSUPPORTED_MEDIA_TYPE = 415;
  public final static int SC_USE_PROXY = 305;
  public final static int SC_TEMPORARY_REDIRECT = 307;

  /**
   * Sets the HTTP status
   *
   * @param sc the HTTP status code
   */
  public void setStatus(int sc);
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc, String msg)
    throws IOException;
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc)
    throws IOException;
  /**
   * Redirects the client to another page.
   *
   * @param location the location to redirect to.
   */
  public void sendRedirect(String location)
    throws IOException;
  /**
   * Sets a header.  This will override a previous header
   * with the same name.
   *
   * @param name the header name
   * @param value the header value
   */
  public void setHeader(String name, String value);
  /**
   * Adds a header.  If another header with the same name exists, both
   * will be sent to the client.
   *
   * @param name the header name
   * @param value the header value
   */
  public void addHeader(String name, String value);
  /**
   * Returns true if the output headers include <code>name</code>
   *
   * @param name the header name to test
   */
  public boolean containsHeader(String name);
  /**
   * Sets a header by converting a date to a string.
   *
   * <p>To set the page to expire in 15 seconds use the following:
   * <pre><code>
   * long now = System.currentTime();
   * response.setDateHeader("Expires", now + 15000);
   * </code></pre>
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void setDateHeader(String name, long date);
  /**
   * Adds a header by converting a date to a string.
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void addDateHeader(String name, long date);
  /**
   * Sets a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void setIntHeader(String name, int value);
  /**
   * Adds a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void addIntHeader(String name, int value);
  /**
   * Sends a new cookie to the client.
   */
  public void addCookie(Cookie cookie);
  /**
   * Encodes session information in a URL. Calling this will enable
   * sessions for users who have disabled cookies.
   *
   * @param url the url to encode
   * @return a url with session information encoded
   */
  public String encodeURL(String url);
  /**
   * Encodes session information in a URL suitable for
   * <code>sendRedirect()</code> 
   *
   * @param name the url to encode
   * @return a url with session information encoded
   */
  public String encodeRedirectURL(String name);
  /**
   * @deprecated
   */
  public void setStatus(int sc, String msg);
  /**
   * @deprecated
   */
  public String encodeUrl(String url);
  /**
   * @deprecated
   */
  public String encodeRedirectUrl(String url);

  /**
   * Returns the current status code of this response
   *
   * @return
   */
  public int getStatus();

  /**
   * Retuns value of header with a given name
   *
   * @param name
   * @return
   */
  public String getHeader(String name);

  /**
   * Returns an Iterable for header values with a given name
   *
   * @param name
   * @return
   */
  public Collection<String> getHeaders(String name);

  /**
   * Returns an Iterable for header names set via {@link #setHeader}, {@link
   * #addHeader}, {@link #setDateHeader}, {@link #addDateHeader}, {@link
   * #setIntHeader}, or {@link #addIntHeader}, respectively.
   *
   * @return
   */
  public Collection<String> getHeaderNames();
}
