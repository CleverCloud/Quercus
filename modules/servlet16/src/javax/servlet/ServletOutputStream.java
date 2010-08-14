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
import java.io.OutputStream;

/**
 * Servlets can use ServletOutputStream to write binary data to
 * the response.  ServletOutputStream provides several methods similar
 * to the methods of PrintWriter.
 *
 * <p>Typically, servlets will use response.getOutputStream to get
 * this object.
 *
 * <p>Note, the PrintWriter-like methods do not handle character
 * encoding properly.  If you need non-ascii character output, use
 * getWriter().
 *
 * <p>Buffering of the output stream is controlled by the Response object.
 */
public abstract class ServletOutputStream extends OutputStream {
  
  protected ServletOutputStream()
  {
  }

  /**
   * Prints a string to the stream.  Note, this method does not properly
   * handle character encoding.
   *
   * @param s the string to write.
   */
  public void print(String s) throws IOException
  {
    if (s == null)
      s = "null";

    int length = s.length();

    // server/0810
    for (int i = 0; i < length; i++) {
      write(s.charAt(i));
    }
  }

  /**
   * Prints a boolean value to output.
   *
   * @param b the boolean value
   */
  public void print(boolean b) throws IOException
  {
    print(String.valueOf(b));
  }

  /**
   * Prints a character to the output.  Note, this doesn't handle
   * character encoding properly.
   *
   * @param c the character value
   */
  public void print(char c) throws IOException
  {
    print(String.valueOf(c));
  }

  /**
   * Prints an integer to the output.
   *
   * @param i the integer value
   */
  public void print(int i) throws IOException
  {
    print(String.valueOf(i));
  }

  /**
   * Prints a long to the output.
   *
   * @param l the long value
   */
  public void print(long l) throws IOException
  {
    print(String.valueOf(l));
  }

  /**
   * Prints a float to the output.
   *
   * @param f the float value
   */
  public void print(float f) throws IOException
  {
    print(String.valueOf(f));
  }

  /**
   * Prints a double to the output.
   *
   * @param d the double value
   */
  public void print(double d) throws IOException
  {
    print(String.valueOf(d));
  }

  /**
   * Prints a newline to the output.
   */
  public void println() throws IOException
  {
    print("\n");
  }

  /**
   * Prints a string to the output, followed by a newline.  Note,
   * character encoding is not properly handled.
   *
   * @param s the string value
   */
  public void println(String s) throws IOException
  {
    print(s);
    println();
  }

  /**
   * Prints a boolean to the output, followed by a newline.
   *
   * @param b the boolean value
   */
  public void println(boolean b) throws IOException
  {
    println(String.valueOf(b));
  }

  /**
   * Prints a character to the output, followed by a newline.  Note,
   * character encoding is not properly handled.
   *
   * @param c the character value
   */
  public void println(char c) throws IOException
  {
    println(String.valueOf(c));
  }

  /**
   * Prints an integer to the output, followed by a newline.
   *
   * @param i the integer value
   */
  public void println(int i) throws IOException
  {
    println(String.valueOf(i));
  }

  /**
   * Prints a long to the output, followed by a newline.
   *
   * @param l the long value
   */
  public void println(long l) throws IOException
  {
    println(String.valueOf(l));
  }

  /**
   * Prints a float to the output, followed by a newline.
   *
   * @param f the float value
   */
  public void println(float f) throws IOException
  {
    println(String.valueOf(f));
  }

  /**
   * Prints a double to the output, followed by a newline.
   *
   * @param d the double value
   */
  public void println(double d) throws IOException
  {
    println(String.valueOf(d));
  }
}
