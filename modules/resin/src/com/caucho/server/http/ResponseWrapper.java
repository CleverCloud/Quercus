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

package com.caucho.server.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.server.cache.AbstractCacheEntry;
import com.caucho.server.cache.AbstractCacheFilterChain;
import com.caucho.util.L10N;

/**
 * Wraps a servlet response in another response.  Filters may
 * use ServletResponseWrapper to grab results from the servlet.
 *
 * <p/>The default methods just call the wrapped response methods.
 *
 * @since servlet 2.3
 */
public class ResponseWrapper implements ServletResponse {
  private static final L10N L = new L10N(ResponseWrapper.class);
  
  // the wrapped response
  protected HttpServletResponse _response;
  
  /**
   * Create a new ServletResponseWrapper, wrapping a specified response.
   */
  public ResponseWrapper()
  {
  }
  
  /**
   * Create a new ServletResponseWrapper, wrapping a specified response.
   *
   * @param response the response to wrap.
   */
  public ResponseWrapper(HttpServletResponse response)
  {
    _response = response;
  }
  
  /**
   * Sets the response to be wrapped.
   *
   * @param response the response to wrap.
   */
  public void setResponse(HttpServletResponse response)
  {
    _response = response;
  }
  
  /**
   * Gets the wrapped response
   *
   * @return the wrapped response
   */
  public ServletResponse getResponse()
  {
    return _response;
  }

  public AbstractHttpResponse getAbstractHttpResponse()
  {
    if (_response instanceof AbstractHttpResponse)
      return (AbstractHttpResponse) _response;
    else
      return null;
  }
  
  /**
   * Sets the response content type.  The content type includes
   * the character encoding.  The content type must be set before
   * calling <code>getWriter()</code> so the writer can use the
   * proper character encoding.
   *
   * <p>To set the output character encoding to ISO-8859-2, use the
   * following:
   *
   * <code><pre>
   * response.setContentType("text/html; charset=ISO-8859-2");
   * </pre></code>
   *
   * @param type the mime type of the output
   */
  public void setContentType(String type)
  {
    _response.setContentType(type);
  }
  
  /**
   * Returns the content type
   *
   * @since 2.4
   */
  public String getContentType()
  {
    return _response.getContentType();
  }
  
  /**
   * Returns the character encoding the response is using for output.
   */
  public String getCharacterEncoding()
  {
    return _response.getCharacterEncoding();
  }
  
  /**
   * Sets the character encoding the response is using for output.
   *
   * @since 2.4
   */
  public void setCharacterEncoding(String encoding)
  {
    _response.setCharacterEncoding(encoding);
  }
  
  /**
   * Sets the output locale.  The response will set the character encoding
   * based on the locale.  For example, setting the "kr" locale will set
   * the character encoding to "EUC_KR".
   */
  public void setLocale(Locale locale)
  {
    _response.setLocale(locale);
  }
  /**
   * Returns the output locale.
   */
  public Locale getLocale()
  {
    return _response.getLocale();
  }
  /**
   * Returns an output stream for writing to the client.  You can use
   * the output stream to write binary data.
   */
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    return _response.getOutputStream();
  }
  /**
   * Returns a PrintWriter with the proper character encoding for writing
   * text data to the client.
   */
  public PrintWriter getWriter()
    throws IOException
  {
    return _response.getWriter();
  }
  /**
   * Sets the output buffer size to <code>size</code>.  The servlet engine
   * may round the size up.
   *
   * @param size the new output buffer size.
   */
  public void setBufferSize(int size)
  {
    _response.setBufferSize(size);
  }
  /**
   * Returns the size of the output buffer.
   */
  public int getBufferSize()
  {
    return _response.getBufferSize();
  }
  /**
   * Flushes the buffer to the client.
   */
  public void flushBuffer()
    throws IOException
  {
    _response.flushBuffer();
  }
  /**
   * Returns true if some data has actually been send to the client.  The
   * data will be sent if the buffer overflows or if it's explicitly flushed.
   */
  public boolean isCommitted()
  {
    return _response.isCommitted();
  }
  
  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void reset()
  {
    _response.reset();
  }
  
  /**
   * Resets the output stream without clearing headers and the output buffer.
   * Calling <code>resetBuffer()</code> after data has been committed is
   * illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void resetBuffer()
  {
    _response.resetBuffer();
  }
  /**
   * Resin automatically handles output content length and chunking.  This
   * method is ignored.
   *
   * @deprecated
   */
  public void setContentLength(int len)
  {
    _response.setContentLength(len);
  }

  /**
   * Sets the HTTP status
   *
   * @param sc the HTTP status code
   */
  public void setStatus(int sc)
  {
    _response.setStatus(sc);
  }
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc, String msg)
    throws IOException
  {
    _response.sendError(sc, msg);
  }
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc)
    throws IOException
  {
    _response.sendError(sc);
  }
  /**
   * Redirects the client to another page.
   *
   * @param location the location to redirect to.
   */
  public void sendRedirect(String location)
    throws IOException
  {
    _response.sendRedirect(location);
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
    _response.setHeader(name, value);
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
    _response.addHeader(name, value);
  }
  /**
   * Returns true if the output headers include <code>name</code>
   *
   * @param name the header name to test
   */
  public boolean containsHeader(String name)
  {
    return _response.containsHeader(name);
  }
  /**
   * Sets a header by converting a date to a string.
   *
   * <p>To set the page to expire in 15 seconds use the following:
   * <pre><code>
   * long now = System.currentTime();
   * _response.setDateHeader("Expires", now + 15000);
   * </code></pre>
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void setDateHeader(String name, long date)
  {
    _response.setDateHeader(name, date);
  }
  /**
   * Adds a header by converting a date to a string.
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void addDateHeader(String name, long date)
  {
    _response.addDateHeader(name, date);
  }
  /**
   * Sets a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void setIntHeader(String name, int value)
  {
    _response.setIntHeader(name, value);
  }
  /**
   * Adds a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void addIntHeader(String name, int value)
  {
    _response.addIntHeader(name, value);
  }
  /**
   * Sends a new cookie to the client.
   */
  public void addCookie(Cookie cookie)
  {
    _response.addCookie(cookie);
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
    return _response.encodeURL(url);
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
    return _response.encodeRedirectURL(name);
  }
  
  public void setStatus(int sc, String msg)
  {
    _response.setStatus(sc, msg);
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

  //
  // caucho response
  //
  
  public void setFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).setFooter(key, value);
  }

  public void addFooter(String key, String value)
  {
    if (_response instanceof CauchoResponse)
      ((CauchoResponse) _response).addFooter(key, value);
  }

  /**
   * Disables the response
   *
   * @since Servlet 3.0
   */
  public void disable()
  {
  }

  /**
   * Enables the response
   *
   * @since Servlet 3.0
   */
  public void enable()
  {
  }

  /**
   * Returns true if the response is disabled
   *
   * @since Servlet 3.0
   */
  public boolean isDisabled()
  {
    return false;
  }

  public void setCacheInvocation(AbstractCacheFilterChain cacheFilterChain)
  {
  }

  public void setMatchCacheEntry(AbstractCacheEntry cacheEntry)
  {
  }
}
