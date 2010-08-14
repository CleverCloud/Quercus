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
 * ServletReponse control the output to the client.
 *
 * <p>A typical servlet will output its response as follows:
 *
 * <code><pre>
 * response.setContentType("text/html; charset=UTF-8");
 * PrintWriter pw = response.getWriter();
 * pw.println("Hello, world");
 * </pre></code>
 *
 * <h4>Buffering</h4>
 *
 * The servlet engine buffers output before sending it to the client.
 * Buffering adds efficiency and flexibility.  Until data is actually
 * committed to the client, the servlet can change headers or reset the
 * entire results.
 * 
 * <h4>Character encoding</h4>
 *
 * Use either <code>setContentType</code> or <code>setLocale</code> to
 * set the character encoding.
 *
 * <code><pre>
 * response.setContentType("text/html; charset=ISO-8859-2");
 * </pre></code>
 */
public interface ServletResponse {
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
  public void setContentType(String type);
  
  /**
   * Returns the content type for the response.
   *
   * @since 2.4
   */
  public String getContentType();

  /**
   * Returns the character encoding the response is using for output.
   * If no character encoding is specified, ISO-8859-1 will be used.
   */
  public String getCharacterEncoding();

  /**
   * Sets the character encoding the response is using for output.
   * If no character encoding is specified, ISO-8859-1 will be used.
   *
   * @since 2.4
   */
  public void setCharacterEncoding(String charset);
  
  /**
   * Sets the output locale.  The response will set the character encoding
   * based on the locale.  For example, setting the "kr" locale will set
   * the character encoding to "EUC_KR".
   */
  public void setLocale(Locale locale);
  /**
   * Returns the output locale.
   */
  public Locale getLocale();
  /**
   * Returns an output stream for writing to the client.  You can use
   * the output stream to write binary data.
   */
  public ServletOutputStream getOutputStream()
    throws IOException;
  /**
   * Returns a PrintWriter with the proper character encoding for writing
   * text data to the client.
   */
  public PrintWriter getWriter()
    throws IOException;
  /**
   * Sets the output buffer size to <code>size</code>.  The servlet engine
   * may round the size up.
   *
   * @param size the new output buffer size.
   */
  public void setBufferSize(int size);
  /**
   * Returns the size of the output buffer.
   */
  public int getBufferSize();
  /**
   * Flushes the buffer to the client.
   */
  public void flushBuffer()
    throws IOException;
  /**
   * Returns true if some data has actually been send to the client.  The
   * data will be sent if the buffer overflows or if it's explicitly flushed.
   */
  public boolean isCommitted();
  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void reset();
  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void resetBuffer();
  
  /**
   * Explicitly sets the length of the result value.  Normally, the servlet
   * engine will handle this.
   */
  public void setContentLength(int len);
}
