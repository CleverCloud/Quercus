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

import com.caucho.quercus.env.*;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Quercus file open for reading
 */
abstract public class AbstractBinaryInputOutput
  implements BinaryInput, BinaryOutput
{
  private static final Logger log
    = Logger.getLogger(AbstractBinaryInputOutput.class.getName());

  protected final Env _env;
  protected final LineReader _lineReader;

  private InputStream _is;
  private OutputStream _os;

  // Set to true when EOF is read from the input stream.

  protected boolean _isTimeout;
  protected boolean _isEOF;

  protected AbstractBinaryInputOutput(Env env)
  {
    _env = env;
    _lineReader = new LineReader(env);
  }

  public void init(InputStream is, OutputStream os)
  {
    _is = is;
    _os = os;
  }

  //
  // read methods
  //
  
  /**
   * Returns the input stream.
   */
  public InputStream getInputStream()
  {
    return _is;
  }

  /**
   * Opens a copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Unread the last byte.
   */
  abstract public void unread()
    throws IOException;

  /**
   * Reads a character from a file, returning -1 on EOF.
   */
  public int read()
    throws IOException
  {
    try {
      if (_is != null) {
        int c = _is.read();

        if (c < 0)
          _isEOF = true;

        return c;
      }
      else
        return -1;
    } catch (IOException e) {
      _isTimeout = true;
      _isEOF = true;

      log.log(Level.FINER, e.toString(), e);

      return -1;
    }
  }

  /**
   * Reads a buffer from a file, returning -1 on EOF.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    try {
      if (_is != null) {
        int c = _is.read(buffer, offset, length);

        if (c < 0)
          _isEOF = true;

        return c;
      }
      else
        return -1;
    } catch (IOException e) {
      _isTimeout = true;
      _isEOF = true;

      log.log(Level.FINER, e.toString(), e);

      return -1;
    }
  }

  /**
   * Reads into a binary builder.
   */
  public StringValue read(int length)
    throws IOException
  {
    if (_is == null)
      return null;

    StringValue bb = _env.createBinaryBuilder();

    if (bb.appendRead(_is, length) > 0)
      return bb;
    else
      return null;
  }

  /**
   * Reads a line from a file, returning null on EOF.
   */
  public StringValue readLine(long length)
    throws IOException
  {
    try {
      StringValue line = _lineReader.readLine(_env, this, length);

      return line;
    } catch (IOException e) {
      _isTimeout = true;
      _isEOF = true;

      log.log(Level.FINER, e.toString(), e);

      return _env.getEmptyString();
    }
  }
  
  /**
   * Reads the optional linefeed character from a \r\n
   */
  public boolean readOptionalLinefeed()
    throws IOException
  {
    if (_is == null)
      return false;

    int ch = read();

    if (ch == '\n') {
      return true;
    }
    else {
      unread();
      return false;
    }
  }

  /**
   * Appends to a string builder.
   */
  public StringValue appendTo(StringValue builder)
    throws IOException
  {
    if (_is != null)
      return builder.append(_is);
    else
      return builder;
  }

  /**
   * Returns true on the EOF.
   */
  public boolean isEOF()
  {
    return _is == null || _isEOF;
  }

  /**
   * Returns true on the EOF.
   */
  public boolean isTimeout()
  {
    return _isTimeout;
  }

  /**
   * Returns the current location in the file.
   */
  abstract public long getPosition();

  /**
   * Sets the current location in the file.
   */
  abstract public boolean setPosition(long offset);

  public long seek(long offset, int whence)
  {
    long position;

    switch (whence) {
    case BinaryStream.SEEK_CUR:
      position = getPosition() + offset;
      break;
    case BinaryStream.SEEK_END:
      // don't necessarily have an end
      position = getPosition();
      break;
    case BinaryStream.SEEK_SET:
    default:
      position = offset;
      break;
    }

    if (! setPosition(position))
      return -1L;
    else
      return position;
  }

  public Value stat()
  {
    return BooleanValue.FALSE;
  }

  /**
   * Closes the stream for reading.
   * The isEOF method will return true
   * after this method has been invoked.
   */
  public void closeRead()
  {
    try {
      InputStream is = _is;
      _is = null;

      if (is != null)
        is.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  //
  // write methods
  //

  /**
   * Returns self as the output stream.
   */
  public OutputStream getOutputStream()
  {
    return _os;
  }

  public void write(int ch)
    throws IOException
  {
    _os.write(ch);
  }

  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    _os.write(buffer, offset, length);
  }

  /**
   * Read length bytes of data from the InputStream
   * argument and write them to this output stream.
   */
  public int write(InputStream is, int length)
    throws IOException
  {
    int writeLength = 0;

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();

    try {
      while (length > 0) {
        int sublen;

        if (length < buffer.length)
          sublen = length;
        else
          sublen = buffer.length;

        sublen = is.read(buffer, 0, sublen);

        if (sublen < 0)
          break;

        write(buffer, 0, sublen);

        writeLength += sublen;
        length -= sublen;
      }

      return writeLength;
    } finally {
      TempBuffer.free(tb);
    }
  }
  
  /**
   * Prints a string to a file.
   */
  public void print(char v)
    throws IOException
  {
    write((byte) v);
  }
  
  /**
   * Prints a string to a file.
   */
  public void print(String s)
    throws IOException
  {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      print(s.charAt(i));
    }
  }

  /**
   * Flushes the output.
   */
  public void flush()
    throws IOException
  {
    _os.flush();
  }


  /**
   * Closes the file.
   */
  public void closeWrite()
  {
    try {
      OutputStream os = _os;
      _os = null;

      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Closes the file.
   */
  public void close()
  {
    closeRead();
    closeWrite();
  }

  public Object toJavaObject()
  {
    return this;
  }

  public void setTimeout(long timeout)
  {
  }

  public String getResourceType()
  {
    return "stream";
  }

  protected Env getEnv()
  {
    return _env;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_is != null)
      return "AbstractBinaryInputOutput[]";
    else
      return "AbstractBinaryInputOutput[closed]";
  }
}

