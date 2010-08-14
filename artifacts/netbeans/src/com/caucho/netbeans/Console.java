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

import com.caucho.netbeans.PluginLogger;
import com.caucho.netbeans.util.LogSupport;

import org.netbeans.modules.j2ee.deployment.plugins.api.UISupport;
import org.openide.ErrorManager;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;

public final class Console
{
  private static final PluginLogger log = new PluginLogger(Console.class);

  private final String _uri;
  private final InputOutput _inputOutput;

  private final OutputWriter _writer;
  private final OutputWriter _errorWriter;
  private final LogSupport _logSupport = new LogSupport();

  private ConsoleRedirector _consoleRedirector;

  public Console(String uri)
  {
    _uri = uri;
    _inputOutput = UISupport.getServerIO(uri);

    try {
      _inputOutput.getOut().reset();
    }
    catch (IOException e) {
      log.log(Level.INFO, e);
    }

    _writer = _inputOutput.getOut();
    _errorWriter = _inputOutput.getErr();
    _inputOutput.select();
  }

  public String getUri()
  {
    return _uri;
  }

  public void println()
  {
    _writer.println();
  }

  public void println(String line)
  {
    LogSupport.LineInfo lineInfo = _logSupport.analyzeLine(line);

    if (lineInfo.isError()) {
      if (lineInfo.isAccessible()) {
        try {
          _errorWriter.println(line,
                              _logSupport.getLink(lineInfo.message(),
                                                 lineInfo.path(),
                                                 lineInfo.line()));
        }
        catch (IOException ex) {
          ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        }
      }
      else {
        _errorWriter.println(line);
      }
      takeFocus();
    }
    else {
      _writer.println(line);
    }
  }

  public void start(Reader in, Reader err)
  {
    if (_consoleRedirector != null)
      throw new IllegalStateException();

    _consoleRedirector = new ConsoleRedirector(this, in, err);
  }

  void flush()
  {
    _writer.flush();
    _errorWriter.flush();
  }

  public void takeFocus()
  {
    _inputOutput.select();
  }

  public void stop()
  {
    ConsoleRedirector consoleRedirector = _consoleRedirector;
    _consoleRedirector = null;

    if (consoleRedirector != null)
      consoleRedirector.destroy();
  }

  public void destroy()
  {
    try {
      stop();
    }
    finally {
      _logSupport.detachAnnotation();
    }
  }
}