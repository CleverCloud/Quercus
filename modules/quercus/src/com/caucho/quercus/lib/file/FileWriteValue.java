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

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP open file
 */
public class FileWriteValue extends FileValue {
  private static final Logger log
    = Logger.getLogger(FileReadValue.class.getName());

  private WriteStream _os;
  private long _offset;

  public FileWriteValue(Path path)
    throws IOException
  {
    super(path);

    _os = path.openWrite();
  }

  public FileWriteValue(Path path, boolean isAppend)
    throws IOException
  {
    super(path);

    if (isAppend)
      _os = path.openAppend();
    else
      _os = path.openWrite();
  }

  /**
   * Prints a string to a file.
   */
  public void print(char v)
    throws IOException
  {
    if (_os != null)
      _os.print(v);
  }

  /**
   * Prints a string to a file.
   */
  public void print(String v)
    throws IOException
  {
    if (_os != null)
      _os.print(v);
  }

  /**
   * Writes a buffer to a file.
   */
  public int write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_os != null) {
      _os.write(buffer, offset, length);

      return length;
    }

    return 0;
  }

  /**
   * Flushes the output.
   */
  public void flush()
  {
    try {
      if (_os != null)
        _os.flush();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }


  /**
   * Closes the file.
   */
  public void close()
  {
    try {
      WriteStream os = _os;
      _os = null;

      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
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

