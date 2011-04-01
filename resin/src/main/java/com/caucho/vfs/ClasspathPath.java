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

import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.QDate;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * The classpath scheme.
 */
public class ClasspathPath extends FilesystemPath {
  protected static L10N L = new L10N(ClasspathPath.class);

  /**
   * Creates a new classpath sub path.
   *
   * @param root the classpath filesystem root
   * @param userPath the argument to the calling lookup()
   * @param newAttributes any attributes passed to http
   * @param path the full normalized path
   * @param query any query string
   */
  public ClasspathPath(FilesystemPath root,
                       String userPath,
                       String path)
  {
    super(root, userPath, path);

    if (_root == null)
      _root = this;
  }
  
  /**
   * Lookup the actual path relative to the filesystem root.
   *
   * @param userPath the user's path to lookup()
   * @param attributes the user's attributes to lookup()
   * @param path the normalized path
   *
   * @return the selected path
   */
  public Path fsWalk(String userPath,
                        Map<String,Object> attributes,
                        String path)
  {
    return new ClasspathPath(_root, userPath, path);
  }

  /**
   * Returns the scheme, http.
   */
  public String getScheme()
  {
    return "classpath";
  }

  /**
   * Returns true if the file exists.
   */
  public boolean exists()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return loader.getResource(getPath()) != null;
  }

  /**
   * Returns true if the file exists.
   */
  public boolean isFile()
  {
    return exists();
  }

  /**
   * Returns true if the file is readable.
   */
  public boolean canRead()
  {
    return exists();
  }

  /**
   * Returns the last modified time.
   */
  public boolean isDirectory()
  {
    return false;
  }

  /**
   * Returns a read stream for a GET request.
   */
  public StreamImpl openReadImpl() throws IOException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    String path = getPath();
    if (path.startsWith("/"))
      path = path.substring(1);

    InputStream is = loader.getResourceAsStream(path);

    if (is == null)
      throw new FileNotFoundException(getFullPath());
    
    return new VfsStream(is, null);
  }
  
  /**
   * Returns the string form of the http path.
   */
  public String toString()
  {
    return getURL();
  }
}
