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

package javax.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;


/**
 * Wraps a servlet response in another response.  Filters may
 * use ServletResponseWrapper to grab results from the servlet.
 *
 * <p/>The default methods just call the wrapped response methods.
 *
 * @since servlet 2.3
 */
public class ServletResponseWrapper implements ServletResponse {
  // the wrapped response
  private ServletResponse _response;
  /**
   * Create a new ServletResponseWrapper, wrapping a specified response.
   *
   * @param response the response to wrap.
   */
  public ServletResponseWrapper(ServletResponse response)
  {
    if (response == null)
      throw new IllegalArgumentException();
    
    _response = response;
  }
  
  /**
   * Sets the response to be wrapped.
   *
   * @param response the response to wrap.
   */
  public void setResponse(ServletResponse response)
  {
    if (response == null)
      throw new IllegalArgumentException();
    
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
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(ServletResponse wrapped)
  {
    if (_response == wrapped)
      return true;
    else if (_response instanceof ServletResponseWrapper)
      return ((ServletResponseWrapper) _response).isWrapperFor(wrapped);
    else
      return false;
  }

  /**
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(Class wrappedType)
  {
    if (!ServletResponse.class.isAssignableFrom(wrappedType))
      throw new IllegalArgumentException(
        "expected instance of javax.servlet.ServletResponse at `"
          + wrappedType
          + "'");
    else if (wrappedType.isAssignableFrom(_response.getClass()))
      return true;
    else if (_response instanceof ServletResponseWrapper)
      return ((ServletResponseWrapper) _response).isWrapperFor(wrappedType);
    else
      return false;
  }
}
