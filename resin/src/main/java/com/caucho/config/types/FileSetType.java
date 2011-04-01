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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract type building a path pattern.  The pattern follows ant.
 */
public class FileSetType {
  static final L10N L = new L10N(PathPatternType.class);
  static final Logger log = Logger.getLogger(PathPatternType.class.getName());

  private Path _dir;
  private String _userPathPrefix = "";
  
  private ArrayList<PathPatternType> _includeList;
  
  private ArrayList<PathPatternType> _excludeList
    = new ArrayList<PathPatternType>();

  /**
   * Sets the starting directory.
   */
  public void setDir(Path dir)
  {
    _dir = dir;
  }

  /**
   * Gets the starting directory.
   */
  public Path getDir()
  {
    return _dir;
  }

  /**
   * Adds an include pattern.
   */
  public void addInclude(PathPatternType pattern)
  {
    if (_includeList == null)
      _includeList = new ArrayList<PathPatternType>();
    
    _includeList.add(pattern);
  }

  /**
   * Adds an exclude pattern.
   */
  public void addExclude(PathPatternType pattern)
  {
    _excludeList.add(pattern);
  }

  /**
   * Sets the user-path prefix for better error reporting.
   */
  public void setUserPathPrefix(String prefix)
  {
    if (prefix != null && ! prefix.equals("") && ! prefix.endsWith("/"))
      _userPathPrefix = prefix + "/";
    else
      _userPathPrefix = prefix;
  }

  /**
   * Initialize the type.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_dir == null)
      _dir = Vfs.lookup();
  }

  /**
   * Returns the set of files.
   */
  public ArrayList<Path> getPaths()
  {
    return getPaths(new ArrayList<Path>());
  }

  /**
   * Returns the set of files.
   */
  public ArrayList<Path> getPaths(ArrayList<Path> paths)
  {
    String dirPath = _dir.getPath();
    
    if (! dirPath.endsWith("/"))
      dirPath += "/";

    getPaths(paths, _dir, dirPath);

    return paths;
  }

  /**
   * Returns the set of files.
   */
  public void getPaths(ArrayList<Path> paths, Path path, String prefix)
  {
    if (path.isDirectory()) {
      try {
        String []list = path.list();

        for (int i = 0; i < list.length; i++)
          getPaths(paths, path.lookup(list[i]), prefix);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    else if (path.exists()) {
      // server/2438 - logging on unreadable
      //  if (path.canRead()) {
      if (isMatch(path, prefix)) {
        String suffix = "";
        String fullPath = path.getPath();

        if (prefix.length() < fullPath.length())
          suffix = path.getPath().substring(prefix.length());

        path.setUserPath(_userPathPrefix + suffix);

        paths.add(path);
      }
    }
  }

  /**
   * Returns the set of files.
   */
  public boolean isMatch(Path path, String prefix)
  {
    String suffix = "";
    String fullPath = path.getPath();

    if (prefix.length() < fullPath.length())
      suffix = path.getPath().substring(prefix.length());

    for (int i = 0; i < _excludeList.size(); i++) {
      PathPatternType pattern = _excludeList.get(i);

      if (pattern.isMatch(suffix))
        return false;
    }

    if (_includeList == null)
      return true;
    
    for (int i = 0; i < _includeList.size(); i++) {
      PathPatternType pattern = _includeList.get(i);

      if (pattern.isMatch(suffix))
        return true;
    }

    return false;
  }

  public String toString()
  {
    return "FileSetType[]";
  }
}
