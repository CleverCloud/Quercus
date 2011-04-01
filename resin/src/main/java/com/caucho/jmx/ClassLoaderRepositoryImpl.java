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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx;

import com.caucho.util.L10N;

import javax.management.loading.ClassLoaderRepository;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Resin implementation for a class loader repository.
 */
public class ClassLoaderRepositoryImpl implements ClassLoaderRepository {
  private static final L10N L = new L10N(ClassLoaderRepositoryImpl.class);
  private static final Logger log
    = Logger.getLogger(ClassLoaderRepositoryImpl.class.getName());

  private ArrayList<ClassLoader> _loaders = new ArrayList<ClassLoader>();
  
  /**
   * Adds a class loader to the repository.
   */
  void addClassLoader(ClassLoader loader)
  {
    _loaders.add(loader);
  }
  
  /**
   * Loads a class from the repository
   */
  public Class loadClass(String className)
    throws ClassNotFoundException
  {
    for (int i = 0; i < _loaders.size(); i++) {
      ClassLoader loader = _loaders.get(i);

      try {
        Class cl = loader.loadClass(className);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
      }
    }

    throw new ClassNotFoundException(L.l("can't load class {0}",
                                         className));
  }
  
  /**
   * Loads a class from the repository, stopping at a given one
   */
  public Class loadClassBefore(ClassLoader stop, String className)
    throws ClassNotFoundException
  {
    for (int i = 0; i < _loaders.size(); i++) {
      ClassLoader loader = _loaders.get(i);

      if (loader == stop)
        break;

      try {
        Class cl = loader.loadClass(className);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
      }
    }

    throw new ClassNotFoundException(L.l("can't load class {0}",
                                         className));
  }
  
  /**
   * Loads a class from the repository, excluding a given one
   */
  public Class loadClassWithout(ClassLoader exclude, String className)
    throws ClassNotFoundException
  {
    for (int i = 0; i < _loaders.size(); i++) {
      ClassLoader loader = _loaders.get(i);

      if (loader == exclude)
        continue;

      try {
        Class cl = loader.loadClass(className);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
      }
    }

    throw new ClassNotFoundException(L.l("can't load class {0}",
                                         className));
  }
}
