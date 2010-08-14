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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an output stream for a popen'ed process.
 */
public class PopenOutput extends AbstractBinaryOutput
    implements EnvCleanup
{
  private static final Logger log
    = Logger.getLogger(PopenOutput.class.getName());

  private Env _env;
  private Process _process;
  private WriteStream _os;

  public PopenOutput(Env env, Process process)
    throws IOException
  {
    _env = env;

    _env.addCleanup(this);
    
    _process = process;

    _os = new WriteStream(new VfsStream(null, _process.getOutputStream()));

    _process.getInputStream().close();
  }

  /**
   * Returns the write stream.
   */
  public OutputStream getOutputStream()
  {
    return _os;
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
   * Writes a character
   */
  public void write(int ch)
    throws IOException
  {
    if (_os != null)
      _os.write(ch);
  }

  /**
   * Writes a buffer to a file.
   */
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_os != null)
      _os.write(buffer, offset, length);
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
  public void closeWrite()
  {
    close();
  }

  public int pclose()
  {
    try {
      WriteStream os = _os;
      _os = null;

      if (os != null)
        os.close();

      return _process.waitFor();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return -1;
    } finally {
      _env.removeCleanup(this);
    }
  }

  /**
   * Closes the file.
   */
  public void close()
  {
    pclose();
  }

  /**
   * Implements the EnvCleanup interface.
   */

  public void cleanup()
  {
    pclose();
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "PopenOutput[" + _process + "]";
  }
}

