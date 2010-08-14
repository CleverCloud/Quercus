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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.*;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Represents a Quercus file open for reading
 */
public class AbstractBinaryInput
  implements BinaryInput
{
  private static final Logger log
    = Logger.getLogger(AbstractBinaryInput.class.getName());

  private Env _env;
  private final LineReader _lineReader;

  private ReadStream _is;

  // Set to true when EOF is read from the input stream.

  private boolean _isEOF = false;

  protected AbstractBinaryInput(Env env)
  {
    _env = env;
    _lineReader = new LineReader(env);
  }

  protected AbstractBinaryInput(Env env, ReadStream is)
  {
    this(env);
    init(is);
  }

  public void init(ReadStream is)
  {
    _is = is;
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

  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    if (_is != null)
      _is.setEncoding(encoding);
  }

  /**
   * Unread the last byte.
   */
  public void unread()
    throws IOException
  {
    if (_is != null) {
      _is.unread();
      _isEOF = false;
    }
  }

  /**
   * Reads a character from a file, returning -1 on EOF.
   */
  public int read()
    throws IOException
  {
    if (_is != null) {
      int c = _is.read();

      if (c == -1)
        _isEOF = true;
      else
        _isEOF = false;

      return c;
    }
    else
      return -1;
  }

  /**
   * Reads a buffer from a file, returning -1 on EOF.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_is != null) {
      int c = _is.read(buffer, offset, length);

      if (c == -1)
        _isEOF = true;
      else
        _isEOF = false;

      return c;
    }
    else
      return -1;
  }

  /**
   * Reads a buffer from a file, returning -1 on EOF.
   */
  public int read(char []buffer, int offset, int length)
    throws IOException
  {
    if (_is != null) {
      int c = _is.read(buffer, offset, length);

      if (c == -1)
        _isEOF = true;
      else
        _isEOF = false;

      return c;
    }
    else
      return -1;
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
      _is.unread();
      return false;
    }
  }

  public void writeToStream(OutputStream os, int length)
    throws IOException
  {
    if (_is != null) {
      _is.writeToStream(os, length);
    }
  }

  /**
   * Reads a line from a file, returning null on EOF.
   */
  public StringValue readLine(long length)
    throws IOException
  {
    return _lineReader.readLine(_env, this, length);
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
    if (_is == null)
      return true;
    else {
      return _isEOF;
    }
  }

  /**
   * Returns the current location in the file.
   */
  public long getPosition()
  {
    if (_is == null)
      return -1;
    else
      return _is.getPosition();
  }

  /**
   * Sets the current location in the file.
   */
  public boolean setPosition(long offset)
  {
    if (_is == null)
      return false;

    _isEOF = false;

    try {
      return _is.setPosition(offset);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

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
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();
  }

  public Object toJavaObject()
  {
    return this;
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
   * Closes the file.
   */
  public void close()
  {
    closeRead();
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_is != null)
      return "AbstractBinaryInput[" + _is.getPath() + "]";
    else
      return "AbstractBinaryInput[closed]";
  }
}

