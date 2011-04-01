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

package com.caucho.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * Object input stream which loads based on the context class loader.
 */
public class ContextLoaderObjectInputStream extends ObjectInputStream {
  /**
   * Creates the new object input stream.
   */
  public ContextLoaderObjectInputStream(InputStream is)
    throws IOException
  {
    super(is);
  }

  protected Class resolveClass(ObjectStreamClass v)
    throws IOException, ClassNotFoundException
  {
    String name = v.getName();

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    return Class.forName(name, false, loader);
  }

  protected Class resolveProxyClass(String []interfaceNames)
    throws IOException, ClassNotFoundException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    Class []clArray = new Class[interfaceNames.length];

    for (int i = 0; i < interfaceNames.length; i++) {
      clArray[i] = Class.forName(interfaceNames[i], false, loader);
    }

    return Proxy.getProxyClass(loader, clArray);
  }
}
