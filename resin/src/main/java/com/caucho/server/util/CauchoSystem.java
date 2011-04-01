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

package com.caucho.server.util;

import com.caucho.VersionFactory;
import com.caucho.config.ConfigRuntimeException;
import com.caucho.java.WorkDir;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.util.ThreadDump;
import com.caucho.vfs.CaseInsensitive;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A wrapper for Caucho system variables, allowing tests to override
 * the default variables.
 */
public class CauchoSystem {
  private static Logger log
    = Logger.getLogger("com.caucho.util.CauchoSystem");
  private static final L10N L = new L10N(CauchoSystem.class);

  static EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");
  
  public static int EXIT_BIND = 2;
  public static int EXIT_OOM = 3;
  public static int EXIT_DEADLOCK = 4;
  public static int EXIT_OTHER = 5;

  static char _separatorChar = File.separatorChar;
  static char _pathSeparatorChar = File.pathSeparatorChar;
  static String _localHost;
  static String _userDir;
  static String _userName;
  static Path _resinHome;
  static Path _rootDirectory;
  static boolean _isTesting;
  static boolean _isTestWindows;

  static boolean _hasJni;
  static boolean _isResin;
  
  static String _resinVersion;
  static String _resinFullVersion;

  private static int _isUnix = -1;
  private static String _newline;
  private static long _version;

  private static JniCauchoSystem _jniCauchoSystem;
  private static boolean _isDetailedStatistics;
  private static String _user;
  private static String _group;
  private static String _classPath;
  private static ArrayList<String> _classPathList;

  private CauchoSystem()
  {
  }

  /**
   * Returns true if we're currently running a test.
   */
  public static boolean isTesting()
  {
    return _isTesting;
  }

  public static void setIsTesting(boolean testing)
  {
    _isTesting = testing;
  }

  /**
   * Sets the Path to be used as ResinHome.
   */
  public static void setResinHome(Path path)
  {
    _resinHome = path;
  }

  /**
   * Gets the Path used as ResinHome.
   */
  public static Path getResinHome()
  {
    if (_resinHome != null)
      return _resinHome;

    String path = System.getProperty("resin.home");
    if (path != null) {
      _resinHome = Vfs.lookupNative(path);
      return _resinHome;
    }

    String classpath = System.getProperty("java.class.path");
    int head = 0;
    char sep = getFileSeparatorChar();
    char pathSep = sep == '/' ? ':' : ';';
    while (path == null) {
      int p = classpath.indexOf(pathSep, head);
      String subpath;
      if (p < 0)
        subpath = classpath.substring(head);
      else
        subpath = classpath.substring(head, p);

      if (subpath.endsWith(sep + "lib" + sep + "resin.jar") ||
          subpath.equals("lib" + sep + "resin.jar")) {
        path = subpath.substring(0, subpath.length() -
                                    ("lib" + sep + "resin.jar").length());
      }

      else if (subpath.endsWith(sep + "classes") ||
               subpath.equals("classes")) {
        Path resinPath = Vfs.lookupNative(subpath);
        resinPath = resinPath.lookup("com/caucho/util/CauchoSystem.class");
        if (resinPath.exists()) {
          path = subpath.substring(0, subpath.length() - "classes".length());
        }
      }

      if (p < 0)
        break;
      else
        head = p + 1;
    }

    if (path != null)
      _resinHome = Vfs.lookupNative(path);
    if (_resinHome != null && _resinHome.isDirectory())
      return _resinHome;

    return Vfs.lookup();
  }

  public static String getVersion()
  {
    if (_resinVersion == null) {
      _resinVersion = VersionFactory.getVersion();
    }
    
    return _resinVersion;
  }
  
  public static String getFullVersion()
  {
    if (_resinFullVersion == null) {
      _resinFullVersion = VersionFactory.getFullVersion();
    }
    
    return _resinFullVersion;
  }
 
  public static long getVersionId()
  {
    if (_version == 0) {
      _version = Crc64.generate(getFullVersion());
    }
    
    return _version;
  }

  public static String getResinConfig()
  {
    return getResinHome() + "/conf/resin.conf";
  }

  /**
   * Returns a path to the work directory.  The work directory is
   * specified in the resin.conf by /caucho.com/java/work-path.  If
   * unspecified, it defaults to /tmp/caucho.
   *
   * @return directory path to work in.
   */
  public static Path getWorkPath()
  {
    Path workPath = WorkDir.getLocalWorkDir();

    if (workPath != null)
      return workPath;

    Path path = WorkDir.getTmpWorkDir();

    try {
      path.mkdirs();
    } catch (IOException e) {
    }

    return path;
  }

  public static String getServerId()
  {
    return _serverIdLocal.get();
  }

  public static String getUserDir()
  {
    if (_userDir == null)
      _userDir = System.getProperty("user.dir");

    return _userDir;
  }

  public static char getFileSeparatorChar()
  {
    return _separatorChar;
  }

  public static char getPathSeparatorChar()
  {
    return _pathSeparatorChar;
  }

  public static String getNewlineString()
  {
    if (_newline == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

        _newline = System.getProperty("line.separator");
        if (_newline != null) {
        }
        else if (isWindows())
          _newline = "\r\n";
        else
          _newline = "\n";
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    return _newline;
  }

  public static boolean isWindows()
  {
    return _separatorChar == '\\' || _isTestWindows;
  }

  public static boolean isTest()
  {
    return Alarm.isTest();
  }

  public static boolean isCaseInsensitive()
  {
    return CaseInsensitive.isCaseInsensitive();
  }

  public static boolean isUnix()
  {
    if (_isUnix >= 0)
      return _isUnix == 1;

    _isUnix = 0;

    if (_separatorChar == '/' && Vfs.lookup("/bin/sh").canRead())
      _isUnix = 1;

    return _isUnix == 1;
  }

  public static void setWindowsTest(boolean isWindows)
  {
    _isTesting = true;
    _isTestWindows = isWindows;
    Path.setTestWindows(isWindows);
  }

  public static String getLocalHost()
  {
    if (_localHost != null)
      return _localHost;

    try {
      InetAddress addr = InetAddress.getLocalHost();
      _localHost = addr.getHostName();
    } catch (Exception e) {
      _localHost = "127.0.0.1";
    }

    return _localHost;
  }

  public static boolean isJdk15()
  {
    try {
      return Class.forName("java.lang.annotation.Annotation") != null;
    } catch (Throwable e) {
      return false;
    }
  }

  public static String getUserName()
  {
    if (_userName == null)
      _userName = System.getProperty("user.name");

    return _userName;
  }

  /**
   * Set true to cause the tracking of detailed statistcs, default false.
   * Detailed statistics cause various parts of Resin to keep more detailed
   * statistics at the possible expense of performance.
   */
  public static void setDetailedStatistics(boolean isVerboseStatistics)
  {
    _isDetailedStatistics = isVerboseStatistics;
  }

  /**
   * Detailed statistics cause various parts of Resin to keep more detailed
   * statistics at the possible expense of some performance.
   */
  public static boolean isDetailedStatistics()
  {
    return _isDetailedStatistics;
  }

  /**
   * Loads a class from the context class loader.
   *
   * @param name the classname, separated by '.'
   *
   * @return the loaded class.
   */
  public static Class loadClass(String name)
    throws ClassNotFoundException
  {
    return loadClass(name, false, null);
  }

  /**
   * Loads a class from a classloader.  If the loader is null, uses the
   * context class loader.
   *
   * @param name the classname, separated by '.'
   * @param init if true, resolves the class instances
   * @param loader the class loader
   *
   * @return the loaded class.
   */
  public static Class loadClass(String name, boolean init, ClassLoader loader)
    throws ClassNotFoundException
  {
    if (loader == null)
      loader = Thread.currentThread().getContextClassLoader();

    if (loader == null || loader.equals(CauchoSystem.class.getClassLoader()))
      return Class.forName(name);
    else
      return Class.forName(name, init, loader);
  }

  /**
   * Returns the system classpath, including the bootpath
   */
  public static String getClassPath()
  {
    if (_classPath != null)
      return _classPath;

    String cp = System.getProperty("java.class.path");

    String boot = System.getProperty("sun.boot.class.path");
    if (boot != null && ! boot.equals(""))
      cp = cp + File.pathSeparatorChar + boot;

    Pattern pattern = Pattern.compile("" + File.pathSeparatorChar);

    String []path = pattern.split(cp);

    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < path.length; i++) {
      Path subpath = Vfs.lookup(path[i]);

      if (subpath.canRead() || subpath.isDirectory()) {
        if (cb.length() > 0)
          cb.append(File.pathSeparatorChar);

        cb.append(path[i]);
      }
    }

    _classPath = cb.toString();

    return _classPath;
  }

  /**
   * Returns the system classpath, including the bootpath
   */
  public static ArrayList<String> getClassPathList()
  {
    if (_classPathList != null)
      return _classPathList;

    ArrayList<String> list = new ArrayList<String>();

    String classPath = getClassPath();
    String []classPathArray
      = classPath.split("[" + getPathSeparatorChar() + "]");

    for (int i = 0; i < classPathArray.length; i++) {
      if (! list.contains(classPathArray[i]))
        list.add(classPathArray[i]);
    }

    _classPathList = list;

    return _classPathList;
  }

  public static double getLoadAvg()
  {
    if (_jniCauchoSystem == null)
      _jniCauchoSystem = JniCauchoSystem.create();

    return _jniCauchoSystem.getLoadAvg();
  }

  public static void exitOom(Class cl, Throwable e)
  {
    try {
      System.err.println(cl + " Resin restarting due to OutOfMemoryError " + e);
      ThreadDump.dumpThreads();
    } finally {
      Runtime.getRuntime().halt(EXIT_OOM);
    }
  }

  /**
   * @return
   */
  public static boolean is64Bit()
  {
    return "64".equals(System.getProperty("sun.arch.data.model"));
  }
}
