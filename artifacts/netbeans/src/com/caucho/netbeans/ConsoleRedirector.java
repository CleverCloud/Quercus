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
 * @author Sam
 */


package com.caucho.netbeans;

import com.caucho.netbeans.util.LogSupport;

import org.openide.ErrorManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads from the standard and error output and write to the console.
 */
public final class ConsoleRedirector
{
  private final Console _console;
  private BufferedReader _inReader;
  private BufferedReader _errReader;
  private final LogSupport _logSupport = new LogSupport();
  private final ConsoleWriterThread _thread;

  private volatile boolean _isActive = true;

  public ConsoleRedirector(Console console, Reader in, Reader err)
  {
    _console = console;
    _inReader = new BufferedReader(in);
    _errReader = new BufferedReader(err);

    _thread = new ConsoleWriterThread("resin-" + console.getUri() + "-console-writer");
    _thread.start();
  }

  public void destroy()
  {
    if (_isActive) {
      _isActive = false;
      _thread.interrupt();
    }
  }

  private class ConsoleWriterThread
    extends Thread
  {
    public ConsoleWriterThread(String name)
    {
      super(name);

      setDaemon(true);
    }

    public void run()
    {
      try {
        while (_isActive) {

          // read max 256 lines at a time

          int lines = 0;

          while (_isActive && lines < 256)
          {
            boolean isInReady = _inReader.ready();
            boolean isErrReady = _errReader.ready();

            if (!(isInReady || isErrReady))
              break;

            if (isInReady) {
              String line = _inReader.readLine();

              if (line == null)
                _isActive = false;
              else {
                _console.println(line);
                lines++;
              }
            }

            if (isErrReady) {
              String line = _errReader.readLine();

              if (line == null)
                _isActive = false;
              else {
                _console.println(line);
                lines++;
              }
            }
          }

          _console.flush();

          sleep(100);
        }
      }
      catch (IOException e) {
        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
      }
      catch (InterruptedException e) {
        // no-op
      }
    }
  }
}