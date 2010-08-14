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

package com.caucho.modules.terracotta;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.Loader;

import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.loaders.NamedClassLoader;

import java.util.ArrayList;
import java.util.Arrays;

public class TerracottaClassLoaderSupport
  implements EnvironmentListener
{
  private final EnvironmentClassLoader _loader;
  private boolean _isRegistered;

  public TerracottaClassLoaderSupport(EnvironmentClassLoader loader)
  {
    _loader = loader;

    _loader.addListener(this);
  }

  public String getClassLoaderName()
  {
    // the ClassLoader name for Terracotta is a sorted list
    // of the components of the classpath
    ArrayList<Loader> loaders = _loader.getLoaders();

    final int size = loaders.size();

    String[] names = new String[size];

    for (int i = 0;  i < size; i++) {
      names[i] = String.valueOf(loaders.get(i));
    }

    Arrays.sort(names);

    StringBuilder name = new StringBuilder();

    name.append(_loader.getClass().getSimpleName());
    name.append('[');

    for (int i = 0;  i < size; i++) {
      if (i > 0)
        name.append(", ");

      name.append(names[i]);
    }

    name.append(']');

    System.out.println("XXX: FOR " + name);

    return name.toString();
  }

  public void environmentBind(EnvironmentClassLoader loader)
    throws ConfigException
  {
  }

  public void environmentStart(EnvironmentClassLoader loader)
    throws StartLifecycleException
  {
    if (! _isRegistered) {
      ClassProcessorHelper.registerGlobalLoader((NamedClassLoader) _loader);
      _isRegistered = true;
    }
  }

  public void environmentStop(EnvironmentClassLoader loader)
  {
  }
}

