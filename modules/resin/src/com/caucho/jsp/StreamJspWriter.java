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

package com.caucho.jsp;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A JSP writer encapsulating a Writer.
 */
class StreamJspWriter extends AbstractBodyContent {
  // the underlying writer
  private Writer _writer;
  
  private JspPrintWriter _printWriter;

  private JspWriter _parent;
  
  // the page context
  private PageContextImpl _pageContext;

  private boolean _isClosed;
  
  /**
   * Creates a new StreamJspWriter
   */
  StreamJspWriter()
  {
  }

  /**
   * Initialize the JSP writer
   *
   * @param os the underlying stream
   */
  void init(JspWriter parent, Writer writer)
  {
    _parent = parent;
    _writer = writer;
    _isClosed = false;
  }

  /**
   * Returns the print writer.
   */
  public PrintWriter getWriter()
  {
    if (_printWriter == null)
      _printWriter = new JspPrintWriter(this);

    _printWriter.init(this);

    return _printWriter;
  }

  /**
   * Writes a character array to the writer.
   *
   * @param buf the buffer to write.
   * @param off the offset into the buffer
   * @param len the number of characters to write
   */
  final public void write(char []buf, int offset, int length)
    throws IOException
  {
    if (_isClosed)
      return;

    _writer.write(buf, offset, length);
  }
  
  /**
   * Writes a character to the output.
   *
   * @param buf the buffer to write.
   */
  final public void write(int ch) throws IOException
  {
    if (_isClosed)
      return;

    _writer.write(ch);
  }

  /**
   * Writes the newline character.
   */
  final public void newLine() throws IOException
  {
    if (_isClosed)
      return;

    _writer.write('\n');
  }

  /**
   * Prints a character.
   */
  final public void print(char ch) throws IOException
  {
    if (_isClosed)
      return;

    _writer.write(ch);
  }

  /**
   * Prints the newline.
   */
  final public void println() throws IOException
  {
    _writer.write('\n');
  }

  public void clear() throws IOException
  {
    clearBuffer();
  }

  public void clearBuffer() throws IOException
  {
  }

  public void flushBuffer()
    throws IOException
  {
    if (_isClosed)
      return;
    
    _writer.flush();
  }

  public void flush() throws IOException
  {
    _writer.flush();
  }

  final public void close() throws IOException
  {
    _isClosed = true;
  }

  public int getBufferSize()
  {
    return 0;
  }

  public int getRemaining()
  {
    return 0;
  }

  AbstractJspWriter popWriter()
  {
    AbstractJspWriter parent = super.popWriter();

    return parent;
  }
}
