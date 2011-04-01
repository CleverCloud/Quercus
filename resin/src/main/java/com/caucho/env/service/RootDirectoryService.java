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

package com.caucho.env.service;

import java.io.IOException;

import com.caucho.java.WorkDir;
import com.caucho.util.L10N;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;

/**
 * Root service for the root and data directories.
 *
 */
public class RootDirectoryService extends AbstractResinService 
{
  private static final L10N L = new L10N(RootDirectoryService.class);
  public static final int START_PRIORITY_ROOT_DIRECTORY = 20;
  
  private final Path _rootDirectory;
  private final Path _dataDirectory;

  /**
   * Creates a new servlet server.
   */
  public RootDirectoryService(Path rootDirectory)
    throws IOException
  {
    this(rootDirectory, rootDirectory.lookup("resin-data"));
  }

  /**
   * Creates a new servlet server.
   */
  public RootDirectoryService(Path rootDirectory,
                              Path dataDirectory)
    throws IOException
  {
    if (rootDirectory == null)
      throw new NullPointerException();
    
    if (dataDirectory == null)
      throw new NullPointerException();
    
    if (dataDirectory instanceof MemoryPath) { // QA
      dataDirectory = WorkDir.getTmpWorkDir().lookup("qa/resin-data");
    }
    
    _rootDirectory = rootDirectory;
    _dataDirectory = dataDirectory;
    
    rootDirectory.mkdirs();
    dataDirectory.mkdirs();
  }

  /**
   * Returns the current active directory service.
   */
  public static RootDirectoryService getCurrent()
  {
    return ResinSystem.getCurrentService(RootDirectoryService.class);
  }

  /**
   * Returns the data directory for current active directory service.
   */
  public static Path getCurrentDataDirectory()
  {
    RootDirectoryService rootService = getCurrent();
    
    if (rootService == null)
      throw new IllegalStateException(L.l("{0} must be active for getCurrentDataDirectory().",
                                          RootDirectoryService.class.getSimpleName()));
    
    return rootService.getDataDirectory();
  }
  
  /**
   * Returns the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Returns the internal data directory.
   */
  public Path getDataDirectory()
  {
    return _dataDirectory;
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_ROOT_DIRECTORY;
  }
}
