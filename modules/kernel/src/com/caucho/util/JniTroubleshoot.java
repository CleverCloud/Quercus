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

package com.caucho.util;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.inject.Module;
import com.caucho.loader.Environment;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;

/**
 * Common error management for JNI loading
 */
@Module
public class JniTroubleshoot {
  private static final Logger log
    = Logger.getLogger(JniTroubleshoot.class.getName());
  private static final L10N L = new L10N(JniTroubleshoot.class);

  private static final HashSet<String> _loggedLibraries = new HashSet<String>();

  private String _className;
  private String _libraryName;

  private Throwable _cause;

  private boolean _isValid;

  public JniTroubleshoot(Class<?> cl, String libraryName)
  {
    _className = cl.getName();
    _libraryName = libraryName;
    _isValid = true;
  }

  public JniTroubleshoot(Class<?> cl, String libraryName, Throwable cause)
  {
    _className = cl.getName();
    _libraryName = libraryName;
    _cause = cause;
    _isValid = false;
  }

  public void log()
  {
    if (! _isValid && Environment.isLoggingInitialized()) {
      boolean isLogged = false;

      synchronized (_loggedLibraries) {
        isLogged = _loggedLibraries.contains(_libraryName);

        if (! isLogged)
          _loggedLibraries.add(_libraryName);
      }

      if (! isLogged) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, getMessage(), _cause);
        else
          log.warning(getMessage());
      }
    }
  }

  public String getMessage()
  {
    Path lib = getLib();

    if (! lib.exists()) {
      if (isMacOSX()) {
        return L.l("Unable to find native library '{0}' for {1}. "
                   + "Resin expects to find this library in:\n"
                   + "  (Mac OS X) {2}\n"
                   + "On Mac OS X, run ./configure; make; make install.\n"
                   + "The JVM exception was: {3}\n",
                   _libraryName, _className, lib.getNativePath(), _cause);
      }
      else if (isWin()) {
        return L.l("Unable to find native library '{0}' for {1}. "
                   + "Resin expects to find this library in:\n"
                   + "  (Windows) {2}\n"
                   + "On Windows, check your installation for the DLL above.\n"
                   + "The JVM exception was: {3}\n",
                   _libraryName, _className, lib.getNativePath(), _cause);
      }
      else {
        return L.l("Unable to find native library '{0}' for {1}. "
                   + "Resin expects to find this library in:\n"
                   + "  (Unix) {2}\n"
                   + "On Unix, run ./configure; make; make install.\n\n"
                   + "The JVM exception was: {3}\n",
                   _libraryName, _className, lib.getNativePath(), _cause);
      }
    }
    else {
      return L.l("Found library '{0}' as '{1}', but the load failed. "
                 + "The JVM exception was: {2}\n",
                 _libraryName, lib.getNativePath(), _cause);
    }
  }

  public void checkIsValid()
  {
    if (! _isValid)
      throw new IllegalStateException(getMessage(), _cause);
  }

  public boolean isEnabled()
  {
    log();

    return _isValid;
  }

  private boolean is64()
  {
    return "64".equals(System.getProperty("sun.arch.data.model"));
  }

  private boolean isMacOSX()
  {
    return System.getProperty("os.name").equals("Mac OS X");
  }

  private boolean isWin()
  {
    return System.getProperty("os.name").startsWith("Windows");
  }

  private Path getLibexec()
  {
    Path resinHome = CauchoSystem.getResinHome();

    if (isWin()) {
      if (is64())
        return resinHome.lookup("win64");
      else
        return resinHome.lookup("win32");
    }
    else {
      if (is64())
        return resinHome.lookup("libexec64");
      else
        return resinHome.lookup("libexec");
    }
  }

  private Path getLib()
  {
    Path libexec = getLibexec();

    if (isMacOSX()) {
      return libexec.lookup("lib" + _libraryName + ".jnilib");
    }
    else if (isWin()) {
      return libexec.lookup(_libraryName + ".dll");
    }
    else {
      return libexec.lookup("lib" + _libraryName + ".so");
    }
  }
}
