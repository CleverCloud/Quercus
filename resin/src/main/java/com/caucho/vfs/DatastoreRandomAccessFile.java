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
 * @author Nam Nguyen
 */

package com.caucho.vfs;

import java.io.IOException;

public class DatastoreRandomAccessFile
{
  private int _position;
  
  private DatastoreFile _file;
  
  public DatastoreRandomAccessFile(DatastoreFile file)
  {
    _file = file;
  }
  
  public long getLength()
    throws IOException
  {
    return _file.getLength();
  }
  
  public long getFilePointer()
  {
    return _position;
  }
  
  public boolean seek(long offset)
    throws IOException
  {
    _position = (int) offset;
    
    return true;
  }
  
  public long skip(long offset)
    throws IOException
  {
    _position += (int) offset;
    
    return offset;
  }
  
  public void setPosition(long position)
    throws IOException
  {
    _position = (int) position;
  }
  
  public int read()
    throws IOException
  {
    byte []buffer = new byte[1];
    
    if (read(buffer, 0, 1) <= 0)
      return -1;
    
    return buffer[0];
  }
  
  public void write(int b)
    throws IOException
  {
    byte []buffer = new byte[1];
    buffer[0] = (byte) (b & 0xFF);
    
    write(buffer, 0, 1);
  }
  
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    int len = _file.read(_position, buffer, offset, length);
    
    _position += len;
    
    return len;
  }
  
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    _file.write(_position, buffer, offset, length);
    
    _position += length;
  }
  
  public void close()
    throws IOException
  {
    
  }
}