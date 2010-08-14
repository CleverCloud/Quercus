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

package com.caucho.loader.enhancer;

import com.caucho.inject.Module;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;

/**
 * Interface for a scan manager
 */
@Module
public interface ScanListener {
  /**
   * Returns the listener's priority.
   *
   *  0 is an enhancer like Amber
   *  1 is an extender like CanDI
   *  2 is an extender like WebApp 3.0
   */
  public int getScanPriority();
  
  /**
   * Called to check if the archive should be scanned.
   */
  public boolean isRootScannable(Path root, String packageRoot);

  /**
   * Returns the state when scanning the class
   *
   * @param root the module/jar's root path
   * @param packageRoot the virtual package root (usually for Testing) 
   * @param name the class name
   * @param modifiers the class modifiers
   *
   * @return the ScanClass object
   */
  public ScanClass scanClass(Path root, String packageRoot, 
                             String name, int modifiers);
  
  /**
   * Returns true if the string matches an annotation class.
   */
  public boolean isScanMatchAnnotation(CharBuffer string);
  
  /**
   * Callback to note the class matches
   */
  public void classMatchEvent(EnvironmentClassLoader loader,
                              Path root,
                              String className);
}
