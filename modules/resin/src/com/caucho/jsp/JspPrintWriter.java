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

import com.caucho.vfs.FlushBuffer;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A buffered JSP writer encapsulating a Writer.
 */
public class JspPrintWriter extends PrintWriter implements FlushBuffer {
  private static final Logger log
    = Logger.getLogger(JspPrintWriter.class.getName());
  
  private static final Writer _dummyWriter = new StringWriter();
  
  private JspWriter _jspWriter;

  /**
   * Creates a new JspPrintWriter
   */
  JspPrintWriter()
  {
    super(_dummyWriter);
  }

  /**
   * Creates a new JspPrintWriter
   */
  JspPrintWriter(JspWriter jspWriter)
  {
    super(jspWriter);
    
    _jspWriter = jspWriter;
  }

  /**
   * Initialize the JSP writer
   *
   * @param os the underlying stream
   */
  void init(JspWriter jspWriter)
  {
    _jspWriter = jspWriter;
  }

  /**
   * Writes a character array to the writer.
   *
   * @param buf the buffer to write.
   * @param off the offset into the buffer
   * @param len the number of characters to write
   */
  final public void write(char []buf, int offset, int length)
  {
    try {
      _jspWriter.write(buf, offset, length);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Writes a character to the output.
   *
   * @param buf the buffer to write.
   */
  final public void write(int ch)
  {
    try {
      _jspWriter.write(ch);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a char buffer to the output.
   *
   * @param buf the buffer to write.
   */
  final public void write(char []buf)
  {
    try {
      _jspWriter.write(buf, 0, buf.length);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a string to the output.
   */
  final public void write(String s)
  {
    try {
      _jspWriter.write(s, 0, s.length());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a subsection of a string to the output.
   */
  final public void write(String s, int off, int len)
  {
    try {
      _jspWriter.write(s, 0, s.length());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes the newline character.
   */
  final public void newLine()
  {
    try {
      _jspWriter.newLine();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints a boolean.
   */
  final public void print(boolean b)
  {
    try {
      _jspWriter.print(b);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints a character.
   */
  final public void print(char ch)
  {
    try {
      _jspWriter.write(ch);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints an integer.
   */
  final public void print(int v)
  {
    try {
      _jspWriter.print(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints a long
   */
  final public void print(long v)
  {
    try {
      _jspWriter.print(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints a float
   */
  final public void print(float f)
  {
    try {
      _jspWriter.print(f);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints a double.
   */
  final public void print(double d)
  {
    try {
      _jspWriter.print(d);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints a character array
   */
  final public void print(char []s)
  {
    try {
      _jspWriter.write(s, 0, s.length);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints a string.
   */
  final public void print(String s)
  {
    try {
      _jspWriter.print(s);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints the value of the object.
   */
  final public void print(Object v)
  {
    try {
      _jspWriter.print(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints the newline.
   */
  final public void println()
  {
    try {
      _jspWriter.newLine();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints the boolean followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(boolean v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Prints a character followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(char v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints an integer followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(int v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints a long followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(long v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Prints a float followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(float v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  
  /**
   * Prints a double followed by a newline.
   *
   * @param v the value to print
   */
  final public void println(double v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a character array followed by a newline.
   */
  final public void println(char []s)
  {
    try {
      _jspWriter.println(s);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a string followed by a newline.
   */
  final public void println(String s)
  {
    try {
      _jspWriter.println(s);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  /**
   * Writes an object followed by a newline.
   */
  final public void println(Object v)
  {
    try {
      _jspWriter.println(v);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Flushes the buffer and the writer.
   */
  public void flushBuffer()
  {
    try {
      if (_jspWriter instanceof FlushBuffer)
        ((FlushBuffer) _jspWriter).flushBuffer();
      else
        _jspWriter.flush();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Flushes the buffer and the writer.
   */
  public void flush()
  {
    try {
      _jspWriter.flush();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  final public void clear()
  {
    try {
      _jspWriter.clear();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  final public void close()
  {
    try {
      _jspWriter.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
}
