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

package com.caucho.quercus.resources;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.StringValue;

import java.io.IOException;

/**
 * Represents a PHP open stream
 */
public class StreamResource extends ResourceValue {
  /**
   * Reads the next byte, returning -1 on eof.
   */
  public int read()
    throws IOException
  {
    return -1;
  }
  
  /**
   * Reads a buffer, returning -1 on eof.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    return -1;
  }

  /**
   * Reads the optional linefeed character from a \r\n
   */
  public boolean readOptionalLinefeed()
    throws IOException
  {
    return false;
  }
  
  /**
   * Reads a line from the buffer.
   */
  public StringValue readLine(Env env)
    throws IOException
  {
    return null;
  }
  
  /**
   * Writes to a buffer.
   */
  public int write(byte []buffer, int offset, int length)
    throws IOException
  {
    return -1;
  }
  
  /**
   * prints
   */
  public void print(char ch)
    throws IOException
  {
    print(String.valueOf(ch));
  }
  
  /**
   * prints
   */
  public void print(String s)
    throws IOException
  {
  }

  /**
   * Returns true on the end of file.
   */
  public boolean isEOF()
  {
    return true;
  }

  /**
   * Flushes the output
   */
  public void flush()
  {
  }

  /**
   * Returns the current location in the file.
   */
  public long getPosition()
  {
    return 0;
  }

  /**
   * Closes the stream.
   */
  public void close()
  {
  }

  /**
   * Closes the stream for reading
   */
  public void closeRead()
  {
  }

  /**
   * Closes the stream for writing
   */
  public void closeWrite()
  {
  }
}

