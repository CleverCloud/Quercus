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

package javax.servlet.http;

import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import java.io.IOException;
import java.util.Collection;

/**
 * Wraps a servlet request in another request.  Filters may
 * use HttpServletResponseWrapper to modify the headers passed to the servlet.
 *
 * <p/>The default methods just call the wrapped request methods.
 *
 * @since servlet 2.3
 */
public class HttpServletResponseWrapper extends ServletResponseWrapper
  implements HttpServletResponse {
  // The wrapped response
  private HttpServletResponse response;

  /**
   * Creates a new Response wrapper
   *
   * @param response the wrapped response
   */
  public HttpServletResponseWrapper(HttpServletResponse response)
  {
    super(response);

    this.response = response;
  }

  /**
   * Sets a response object.
   *
   * @param response the response object
   */
  public void setResponse(ServletResponse response)
  {
    super.setResponse(response);

    this.response = (HttpServletResponse) response;
  }

  /**
   * Sets the HTTP status
   *
   * @param sc the HTTP status code
   */
  public void setStatus(int sc)
  {
    response.setStatus(sc);
  }
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc, String msg)
    throws IOException
  {
    response.sendError(sc, msg);
  }
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc)
    throws IOException
  {
    response.sendError(sc);
  }
  /**
   * Redirects the client to another page.
   *
   * @param location the location to redirect to.
   */
  public void sendRedirect(String location)
    throws IOException
  {
    response.sendRedirect(location);
  }
  /**
   * Sets a header.  This will override a previous header
   * with the same name.
   *
   * @param name the header name
   * @param value the header value
   */
  public void setHeader(String name, String value)
  {
    response.setHeader(name, value);
  }
  /**
   * Adds a header.  If another header with the same name exists, both
   * will be sent to the client.
   *
   * @param name the header name
   * @param value the header value
   */
  public void addHeader(String name, String value)
  {
    response.addHeader(name, value);
  }
  
  /**
   * Returns true if the output headers include <code>name</code>
   *
   * @param name the header name to test
   */
  public boolean containsHeader(String name)
  {
    return response.containsHeader(name);
  }
  
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
  public void setDateHeader(String name, long date)
  {
    response.setDateHeader(name, date);
  }
  /**
   * Adds a header by converting a date to a string.
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void addDateHeader(String name, long date)
  {
    response.addDateHeader(name, date);
  }
  /**
   * Sets a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void setIntHeader(String name, int value)
  {
    response.setIntHeader(name, value);
  }
  /**
   * Adds a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void addIntHeader(String name, int value)
  {
    response.addIntHeader(name, value);
  }
  /**
   * Sends a new cookie to the client.
   */
  public void addCookie(Cookie cookie)
  {
    response.addCookie(cookie);
  }
  /**
   * Encodes session information in a URL. Calling this will enable
   * sessions for users who have disabled cookies.
   *
   * @param url the url to encode
   * @return a url with session information encoded
   */
  public String encodeURL(String url)
  {
    return response.encodeURL(url);
  }
  /**
   * Encodes session information in a URL suitable for
   * <code>sendRedirect()</code> 
   *
   * @param url the url to encode
   * @return a url with session information encoded
   */
  public String encodeRedirectURL(String name)
  {
    return response.encodeRedirectURL(name);
  }
  
  public void setStatus(int sc, String msg)
  {
    response.setStatus(sc, msg);
  }

  public int getStatus()
  {
    return response.getStatus();
  }

  public String getHeader(String name)
  {
    //XXX: test
    return response.getHeader(name);
  }

  public Collection<String> getHeaders(String name)
  {
    return response.getHeaders(name);
  }

  public Collection<String> getHeaderNames()
  {
    return response.getHeaderNames();
  }

  /**
   * @deprecated
   */
  public String encodeUrl(String url)
  {
    return encodeURL(url);
  }
  /**
   * @deprecated
   */
  public String encodeRedirectUrl(String url)
  {
    return encodeRedirectURL(url);
  }
}
