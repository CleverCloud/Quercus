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

package com.caucho.vfs;

import com.caucho.util.ByteBuffer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Utility class for converting a byte stream to a character stream.
 *
 * <pre>
 * ByteToChar converter = new ByteToChar();
 * converter.setEncoding("utf-8");
 * converter.clear();
 *
 * converter.addChar('H');
 * converter.addByte(0xc0);
 * converter.addByte(0xb8);
 *
 * String value = converter.getConvertedString();
 * </pre>
 */
public final class OldByteToChar extends ByteToChar {
  private ByteBuffer bb;
  private String javaEncoding;

  /**
   * Creates an uninitialized converter. Use <code>init</code> to initialize.
   */ 
  OldByteToChar()
  {
    bb = new ByteBuffer();
  }

  /**
   * Sets the encoding for the converter.
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    javaEncoding = Encoding.getJavaName(encoding);
  }

  /**
   * Clears the converted
   */
  public void clear()
  {
    bb.clear();
  }

  /**
   * Gets the converted string.
   */
  public String getConvertedString()
    throws IOException
  {
    return bb.toString(javaEncoding);
  }
  
  /**
   * Adds the next byte.
   *
   * @param b the byte to write
   */
  public void addByte(int b)
    throws IOException
  {
    bb.add(b);
  }
  
  /**
   * Adds the next character.
   *
   * @param nextCh the character to write
   */
  public void addChar(char nextCh)
    throws IOException
  {
    // The oldByteToChar doesn't handle this case
    bb.add((byte) nextCh);
  }

  /**
   * Prints the object.
   */
  public String toString()
  {
    return "[OldByteToChar " + javaEncoding + "]";
  }
}
