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

package com.caucho.vfs;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.CharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Facade to create useful Path and Stream objects.
 *
 * <code><pre>
 * Path path = Vfs.lookup("foo.html");
 * </pre><code>
 *
 * <p>The default scheme is the file scheme.  Other schemes are
 * available using the full url.
 *
 * <code><pre>
 * Path mail = Vfs.lookup("mailto:drofnats@foo.com.test?subject='hi'");
 * Stream body = mail.openWrite();
 * body.writeln("How's it going?");
 * body.close();
 * </pre><code>
 */
public final class Vfs {
  private static final EnvironmentLocal<Path> ENV_PWD
    = new EnvironmentLocal<Path>("caucho.vfs.pwd");
  
  private static final SchemeMap DEFAULT_SCHEME_MAP;
  
  private static final EnvironmentLocal<SchemeMap> _localSchemeMap
    = new EnvironmentLocal<SchemeMap>();
  
  static FilesystemPath PWD;

  private static AtomicBoolean _isInitJNI = new AtomicBoolean();

  private Vfs() {}
  
  /**
   * Returns a new path relative to the current directory.
   * 
   * @param url a relative or absolute url
   * @return the new path.
   */
  public static Path lookup(String url)
  {
    Path pwd = getPwd();

    if (! url.startsWith("/"))
      return pwd.lookup(url, null);
    else
      return PWD.lookup(url, null);
  }

  public static FilesystemPath getGlobalPwd()
  {
    return PWD;
  }
  
  /**
   * Returns a path for the current directory.
   */
  public static Path getPwd()
  {
    Path pwd = ENV_PWD.get();
    
    if (pwd == null) {
      if (PWD == null) {
        /* JNI set later
        PWD = JniFilePath.create();

        if (PWD == null)
          PWD = new FilePath(null);
        */
        PWD = new FilePath(null);
      }
      pwd = PWD;
      ENV_PWD.setGlobal(pwd);
    }

    return pwd;
  }

  public static SchemeMap getLocalScheme()
  {
    synchronized (_localSchemeMap) {
      SchemeMap map = _localSchemeMap.getLevel();

      if (map == null) {
        map = _localSchemeMap.get().copy();

        if (map == null)
          map = DEFAULT_SCHEME_MAP.copy();

        _localSchemeMap.set(map);
      }

      return map;
    }
  }

  public static SchemeMap getDefaultScheme()
  {
    return DEFAULT_SCHEME_MAP;
  }

  /**
   * Returns a path for the current directory.
   */
  public static Path getPwd(ClassLoader loader)
  {
    return ENV_PWD.get(loader);
  }

  /**
   * Sets a path for the current directory in the current environment.
   */
  public static void setPwd(Path pwd)
  {
    setPwd(pwd, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Sets a path for the current directory in the current environment.
   */
  public static void setPwd(Path pwd, ClassLoader loader)
  {
    ENV_PWD.set(pwd, loader);
  }

  /**
   * Returns a path for the current directory.
   */
  public static Path lookup()
  {
    return getPwd();
  }

  /**
   * Returns a new path, including attributes.
   * <p>For example, an application may want to set locale headers
   * for an HTTP request.
   *
   * @param url the relative url
   * @param attr attributes used in searching for the url
   */
  public static Path lookup(String url, Map<String,Object> attr)
  {
    return getPwd().lookup(url, attr);
  }

  /**
   * Returns a path using the native filesystem conventions.
   * <p>For example, on windows
   *
   * <code><pre>
   * Path path = Vfs.lookup("d:\\temp\\test.html");
   * </pre></code>
   *
   * @param url a relative path using the native filesystem conventions.
   */
  public static Path lookupNative(String url)
  {
    return getPwd().lookupNative(url, null);
  }

  /**
   * Looks up a URL, decoding '%'
   *
   * @param url a relative path using the native filesystem conventions.
   */
  public static Path lookup(URL url)
  {
    return getPwd().lookup(url);
  }

  /**
   * Returns a native filesystem path with attributes.
   *
   * @param url a relative path using the native filesystem conventions.
   * @param attr attributes used in searching for the url
   */
  public static Path lookupNative(String url, Map<String,Object> attr)
  {
    return getPwd().lookupNative(url, attr);
  }

  public static ReadWritePair openReadWrite(InputStream is, OutputStream os)
  {
    VfsStream s = new VfsStream(is, os);
    WriteStream writeStream = new WriteStream(s);
    ReadStream readStream = new ReadStream(s, writeStream);
    return new ReadWritePair(readStream, writeStream);
  }

  /**
   * Creates new ReadStream from an InputStream
   */
  public static ReadStream openRead(InputStream is)
  {
    if (is instanceof ReadStream)
      return (ReadStream) is;
    
    VfsStream s = new VfsStream(is, null);
    return new ReadStream(s);
  }

  public static ReadStream openRead(InputStream is, WriteStream ws)
  {
    VfsStream s = new VfsStream(is, null);
    return new ReadStream(s, ws);
  }

  /**
   * Creates a ReadStream from a Reader
   */
  public static ReadStream openRead(Reader reader)
  {
    if (reader instanceof ReadStream.StreamReader)
      return ((ReadStream.StreamReader) reader).getStream();
    
    ReaderWriterStream s = new ReaderWriterStream(reader, null);
    ReadStream is = new ReadStream(s);
    try {
      is.setEncoding("utf-8");
    } catch (Exception e) {
    }

    return is;
  }

  /**
   * Create a ReadStream from a string.  utf-8 is used as the encoding
   */
  public static ReadStream openRead(String path)
    throws IOException
  {
    return Vfs.lookup(path).openRead();
  }

  public static ReadStream openString(String string)
  {
    return com.caucho.vfs.StringReader.open(string);
  }

  public static WriteStream openWrite(OutputStream os)
  {
    if (os instanceof WriteStream)
      return ((WriteStream) os);
    
    VfsStream s = new VfsStream(null, os);
    return new WriteStream(s);
  }

  public static WriteStream openWrite(Writer writer)
  {
    ReaderWriterStream s = new ReaderWriterStream(null, writer);
    WriteStream os = new WriteStream(s);
    
    try {
      os.setEncoding("utf-8");
    } catch (Exception e) {
    }

    return os;
  }

  /**
   * Creates a write stream to a CharBuffer.  This is the standard way
   * to write to a string.
   */
  public static WriteStream openWrite(CharBuffer cb)
  {
    com.caucho.vfs.StringWriter s = new com.caucho.vfs.StringWriter(cb);
    WriteStream os = new WriteStream(s);
    
    try {
      os.setEncoding("utf-8");
    } catch (Exception e) {
    }

    return os;
  }

  public static WriteStream openWrite(String path)
    throws IOException
  {
    return lookup(path).openWrite();
  }

  public static WriteStream openAppend(String path)
    throws IOException
  {
    return lookup(path).openAppend();
  }

  public static String decode(String uri)
  {
    StringBuilder sb = new StringBuilder();

    int len = uri.length();

    for (int i = 0; i < len; i++) {
      char ch = uri.charAt(i);

      if (ch != '%' || len <= i + 2) {
        sb.append(ch);
        continue;
      }

      int d1 = uri.charAt(i + 1);
      int d2 = uri.charAt(i + 2);
      int v = 0;

      if ('0' <= d1 && d1 <= '9')
        v = 16 * v + d1 - '0';
      else if ('a' <= d1 && d1 <= 'f')
        v = 16 * v + d1 - 'a' + 10;
      else if ('A' <= d1 && d1 <= 'F')
        v = 16 * v + d1 - 'A' + 10;
      else {
        sb.append('%');
        continue;
      }

      if ('0' <= d2 && d2 <= '9')
        v = 16 * v + d2 - '0';
      else if ('a' <= d2 && d2 <= 'f')
        v = 16 * v + d2 - 'a' + 10;
      else if ('A' <= d2 && d2 <= 'F')
        v = 16 * v + d2 - 'A' + 10;
      else {
        sb.append('%');
        continue;
      }

      sb.append((char) v);
      i += 2;
    }

    return sb.toString();
  }

  /**
   * Initialize the JNI.
   */
  public static void initJNI()
  {
    if (_isInitJNI.getAndSet(true))
      return;

    // order matters because of static init and license checking
    FilesystemPath jniFilePath = JniFilePath.create();

    if (jniFilePath != null) {
      DEFAULT_SCHEME_MAP.put("file", jniFilePath);

      SchemeMap localMap = _localSchemeMap.get();
      if (localMap != null)
        localMap.put("file", jniFilePath);

      localMap = _localSchemeMap.get(ClassLoader.getSystemClassLoader());
      if (localMap != null)
        localMap.put("file", jniFilePath);

      Vfs.PWD = jniFilePath;
      Vfs.setPwd(jniFilePath);
    }
  }

  static {
    DEFAULT_SCHEME_MAP = new SchemeMap();

    Path.setDefaultSchemeMap(DEFAULT_SCHEME_MAP);

    FilePath pwd = new FilePath(null);
    PWD = pwd;
    setPwd(pwd);
    ENV_PWD.setGlobal(pwd);
    ENV_PWD.set(pwd);
    
    _localSchemeMap.setGlobal(DEFAULT_SCHEME_MAP);
    
    DEFAULT_SCHEME_MAP.put("file", pwd);

    DEFAULT_SCHEME_MAP.put("memory", new MemoryScheme());
    
    DEFAULT_SCHEME_MAP.put("jar", new JarScheme(null)); 
    /* XXX:
    DEFAULT_SCHEME_MAP.put("mailto",
                                 new MailtoPath(null, null, null, null));
    */
    DEFAULT_SCHEME_MAP.put("classpath", new ClasspathPath(null, "", ""));
    DEFAULT_SCHEME_MAP.put("http", new HttpPath("127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("https", new HttpsPath("127.0.0.1", 0));
    // DEFAULT_SCHEME_MAP.put("hmux", new HmuxPath("127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("tcp", new TcpPath(null, null, null, "127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("tcps", new TcpsPath(null, null, null, "127.0.0.1", 0));
    // DEFAULT_SCHEME_MAP.put("log", new LogPath(null, "/", null, "/"));

    DEFAULT_SCHEME_MAP.put("merge", new MergePath());

    StreamImpl stdout = StdoutStream.create();
    StreamImpl stderr = StderrStream.create();
    DEFAULT_SCHEME_MAP.put("stdout", stdout.getPath());
    DEFAULT_SCHEME_MAP.put("stderr", stderr.getPath());
    VfsStream nullStream = new VfsStream(null, null);
    DEFAULT_SCHEME_MAP.put("null", new ConstPath(null, nullStream));
    DEFAULT_SCHEME_MAP.put("jndi", new JndiPath());
    
    DEFAULT_SCHEME_MAP.put("datastore", new DatastorePath("/"));
    
    /*
    DEFAULT_SCHEME_MAP.put("config", new ConfigPath());
    */
    DEFAULT_SCHEME_MAP.put("spy", new SpyScheme()); 
  }
}
