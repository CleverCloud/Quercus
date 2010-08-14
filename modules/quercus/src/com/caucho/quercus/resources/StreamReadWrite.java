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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * Represents read/write stream
 */
public class StreamReadWrite extends StreamResource
    implements EnvCleanup
{
  private Env _env;
  private ReadStream _is;
  private WriteStream _os;

  public StreamReadWrite(Env env)
  {
    _env = env;
    
    _env.addCleanup(this);
  }

  public StreamReadWrite(Env env, ReadStream is, WriteStream os)
  {
    this(env);

    init(is, os);
  }

  protected void init(ReadStream is, WriteStream os)
  {
    _is = is;
    _os = os;
  }
  
  /**
   * Reads the next byte, returning -1 on eof.
   */
  public int read()
    throws IOException
  {
    if (_is != null)
      return _is.read();
    else
      return -1;
  }
  
  /**
   * Reads a buffer, returning -1 on eof.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_is != null)
      return _is.read(buffer, offset, length);
    else
      return -1;
  }

  /**
   * Reads the optional linefeed character from a \r\n
   */
  public boolean readOptionalLinefeed()
    throws IOException
  {
    if (_is != null) {
      int ch = _is.read();

      if (ch == '\n') {
        return true;
      }
      else {
        _is.unread();
        return false;
      }
    }
    else
      return false;
  }
  
  /**
   * Reads a line from the buffer.
   */
  @Override
  public StringValue readLine(Env env)
    throws IOException
  {
    if (_is != null)
      return env.createString(_is.readLineNoChop());
    else
      return env.getEmptyString();
  }

  /**
   * Reads a line from the stream into a buffer.
   */
  public int readLine(char []buffer)
  {
    try {
      if (_is != null)
        return _is.readLine(buffer, buffer.length, false);
      else
        return -1;
    } catch (IOException e) {
      return -1;
    }
  }
  
  /**
   * Writes to a buffer.
   */
  public int write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_os != null) {
      _os.write(buffer, offset, length);

      return length;
    } else {
      return -1;
    }
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
    if (_os != null)
      _os.print(s);
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
    try {
      if (_os != null)
        _os.flush();
    } catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  /**
   * Returns the current location in the file.
   */
  public long getPosition()
  {
    return 0;
  }

  /**
   * Closes the stream for reading.
   */
  public void closeRead()
  {
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();
  }

  /**
   * Closes the stream for writing
   */
  public void closeWrite()
  {
    WriteStream os = _os;
    _os = null;

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
    }
  }

  /**
   * Closes the stream.
   */
  public void close()
  {
    _env.removeCleanup(this);

    cleanup();
  }

  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;

    if (is != null)
      is.close();

    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
    }
  }

}

