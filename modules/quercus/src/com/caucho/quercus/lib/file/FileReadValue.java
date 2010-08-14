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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Quercus open file
 */
public class FileReadValue extends FileValue {
  private static final Logger log
    = Logger.getLogger(FileReadValue.class.getName());

  private ReadStream _is;
  private long _offset;

  public FileReadValue(Path path)
    throws IOException
  {
    super(path);

    _is = path.openRead();
  }

  /**
   * Returns the number of bytes available to be read, 0 if no known.
   */
  public long getLength()
  {
    return getPath().getLength();
  }

  /**
   * Reads a character from a file, returning -1 on EOF.
   */
  public int read()
    throws IOException
  {
    if (_is != null) {
      int v = _is.read();

      if (v >= 0)
        _offset++;
      else
        close();

      return v;
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
      int len = _is.read(buffer, offset, length);

      if (len >= 0)
        _offset += len;
      else
        close();

      return len;
    }
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
        _offset++;
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

  @Override
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
  @Override
  public StringValue readLine(Env env)
    throws IOException
  {
    // XXX: offset messed up

    if (_is != null)
      return env.createString(_is.readLineNoChop());
    else
      return null;
  }

  /**
   * Returns true on the EOF.
   */
  public boolean isEOF()
  {
    if (_is == null)
      return true;
    else {
      try {
        // XXX: not quite right for sockets
        return  _is.available() <= 0;
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        return true;
      }
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
   * Closes the file.
   */
  public void close()
  {
    ReadStream is = _is;
    _is = null;

    if (is != null)
      is.close();
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "File[" + getPath() + "]";
  }

}

