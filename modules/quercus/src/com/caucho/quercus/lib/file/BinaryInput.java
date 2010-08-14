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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.StringValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Interface for a Quercus binary input stream
 */
public interface BinaryInput extends BinaryStream {
  /**
   * Returns an InputStream to the input.
   */
  public InputStream getInputStream();

  /**
   * Opens a new copy.
   */
  public BinaryInput openCopy()
    throws IOException;
  
  /**
   * Reads the next byte, returning -1 on eof.
   */
  public int read()
    throws IOException;
  
  /**
   * Unreads the last byte.
   */
  public void unread()
    throws IOException;

  /**
   * Reads into a buffer, returning -1 on eof.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Reads a Binary string.
   */
  public StringValue read(int length)
    throws IOException;

  /**
   * Reads the optional linefeed character from a \r\n
   */
  public boolean readOptionalLinefeed()
    throws IOException;
  
  /**
   * Reads a line from the buffer.
   */
  public StringValue readLine(long length)
    throws IOException;

  /**
   * Appends to a string builder.
   */
  public StringValue appendTo(StringValue builder)
    throws IOException;

  /**
   * Returns the current location in the stream
   */
  public long getPosition();

  /**
   * Sets the current location in the stream
   */
  public boolean setPosition(long offset);

  /**
   * Closes the stream.
   */
  public void close();

  /**
   * Closes the stream for reading
   */
  public void closeRead();
}

