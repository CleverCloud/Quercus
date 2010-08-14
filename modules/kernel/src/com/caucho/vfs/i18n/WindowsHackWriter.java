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

package com.caucho.vfs.i18n;

import java.io.IOException;

import com.caucho.util.ByteAppendable;
import com.caucho.vfs.OutputStreamWithBuffer;

/**
 * Implements an encoding char-to-byte writer for the windows hack
 * factory.
 */
public class WindowsHackWriter extends EncodingWriter {
  private final static WindowsHackWriter _writer = new WindowsHackWriter();
  
  /**
   * Null-arg constructor for instantiation by com.caucho.vfs.Encoding only.
   */
  public WindowsHackWriter()
  {
  }
  
  /**
   * Returns the Java encoding for the writer.
   */
  public String getJavaEncoding()
  {
    return "WindowsHack";
  }

  /**
   * Create a windows-hack writer using on the OutputStream to send bytes.
   *
   * @param os the write stream receiving the bytes.
   * @param javaEncoding the JDK name for the encoding.
   *
   * @return the windows-hack writer.
   */
  public EncodingWriter create(String javaEncoding)
  {
    return _writer;
  }

  /**
   * Writes a character to the output stream with the correct encoding.
   *
   * @param ch the character to write.
   */
  public void write(ByteAppendable os, char ch)
    throws IOException
  {
    os.write(ch);
  }

  /**
   * Writes into a character buffer using the correct encoding.
   *
   * @param cbuf character array with the data to write.
   * @param off starting offset into the character array.
   * @param len the number of characters to write.
   */
  public void write(OutputStreamWithBuffer os, char []cbuf, int off, int len)
    throws IOException
  {
    for (int i = 0; i < len; i++)
      os.write(cbuf[off + i]);
  }
}


