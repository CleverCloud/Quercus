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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.env.Value;
import com.caucho.vfs.TempBuffer;

/**
 * Represents a Quercus file open for reading
 */
public class WriteStreamOutput extends OutputStream implements BinaryOutput {
  private static final Logger log
    = Logger.getLogger(WriteStreamOutput.class.getName());

  private OutputStream _os;

  public WriteStreamOutput(OutputStream os)
  {
    _os = os;
  }

  /**
   * Returns the input stream.
   */
  public OutputStream getOutputStream()
  {
    return _os;
  }

  public void close()
  {
    OutputStream os = _os;
    _os = null;

    if (os != null) {
      try {
        os.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  public Object toJavaObject()
  {
    return this;
  }

  public String getResourceType()
  {
    return "stream";
  }

  @Override
  public void write(int ch) throws IOException
  {
    _os.write(ch);
  }

  @Override
  public void write(byte []buffer, int offset, int length) throws IOException
  {
    _os.write(buffer, offset, length);
  }

  @Override
  public void closeWrite()
  {
    close();
  }

  @Override
  public void print(char ch) throws IOException
  {
    _os.write(ch);
  }

  /* (non-Javadoc)
   * @see com.caucho.quercus.lib.file.BinaryOutput#print(java.lang.String)
   */
  @Override
  public void print(String s) throws IOException
  {
    int len = s.length();
    
    for (int i = 0; i < len; i++)
      _os.write(s.charAt(i));
  }

  @Override
  public int write(InputStream is, int length) throws IOException
  {
    TempBuffer tempBuffer = TempBuffer.allocate();
    byte []buffer = tempBuffer.getBuffer();
    
    int writeLength = length;
    
    while (length > 0) {
      int sublen = buffer.length;
      
      if (length < sublen)
        sublen = length;
      
      sublen = is.read(buffer, 0, sublen);
      
      if (sublen <= 0)
        break;
      
      _os.write(buffer, 0, sublen);
      
      length -= sublen;
    }
    
    TempBuffer.free(tempBuffer);
    
    return writeLength;
  }

  @Override
  public long getPosition()
  {
    return 0;
  }

  @Override
  public boolean isEOF()
  {
    return false;
  }

  @Override
  public long seek(long offset, int whence)
  {
    return 0;
  }

  @Override
  public boolean setPosition(long offset)
  {
    return false;
  }

  @Override
  public Value stat()
  {
    return null;
  }
  

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return "WriteStreamOutput[" + _os + "]";
  }
}

