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

package com.caucho.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Reads from a file in a random-access fashion.
 */
public class SpyRandomAccessStream extends RandomAccessStream {
  private static final Logger log
    = Logger.getLogger(SpyRandomAccessStream.class.getName());
  
  private RandomAccessStream _file;

  public SpyRandomAccessStream(RandomAccessStream file)
  {
    _file = file;
  }
  
  /**
   * Returns the length.
   */
  public long getLength()
    throws IOException
  {
    return _file.getLength();
  }
  
  /**
   * Reads a block from a given location.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    log.finest("random-read(0x" + Long.toHexString(getFilePointer()) + "," +
               length + ")");
    
    return _file.read(buffer, offset, length);
  }

  /**
   * Reads a block from a given location.
   */
  public int read(char []buffer, int offset, int length)
    throws IOException
  {
    log.finest("random-read(0x" + Long.toHexString(getFilePointer()) + "," +
               length + ")");

    return _file.read(buffer, offset, length);
  }

  /**
   * Reads a block from a given location.
   */
  public int read(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    log.info("random-read(0x" + Long.toHexString(fileOffset) + "," + 
             length + ")");
    
    return _file.read(fileOffset, buffer, offset, length);
  }

  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    log.info("random-write(0x" + Long.toHexString(getFilePointer()) + "," + 
             length + ")");
    
    _file.write(buffer, offset, length);
  }

  /**
   * Writes a block from a given location.
   */
  public void write(long fileOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    log.info("random-write(0x" + Long.toHexString(fileOffset) + "," + 
             length + ")");
    
    _file.write(fileOffset, buffer, offset, length);
  }

    /**
   * Seeks to the given position in the file.
   */
  public boolean seek(long position)
  {
    log.info("random-seek(0x" + position + ")");

    return _file.seek(position);
  }

  /**
   * Returns an OutputStream for this stream.
   */
  public OutputStream getOutputStream()
    throws IOException
  {
    return _file.getOutputStream();
  }

  /**
   * Returns an InputStream for this stream.
   */
  public InputStream getInputStream()
    throws IOException
  {
    return _file.getInputStream();
  }

  /**
   * Read a byte from the file, advancing the pointer.
   */
  public int read()
    throws IOException
  {
    log.info("random-read(0x" + Long.toHexString(getFilePointer()) + ",1)");

    return _file.read();
  }

  /**
   * Write a byte to the file, advancing the pointer.
   */
  public void write(int b)
    throws IOException
  {
    log.info("random-write(0x" + Long.toHexString(getFilePointer()) + ",1)");
    
    _file.write(b);
  }

  /**
   * Returns the current position of the file pointer.
   */
  public long getFilePointer()
    throws IOException
  {
    return _file.getFilePointer();
  }

  /**
   * Closes the stream.
   */
  public void close() throws IOException
  {
    _file.close();
  }
}
