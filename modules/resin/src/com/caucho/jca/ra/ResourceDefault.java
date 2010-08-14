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

package com.caucho.jca.ra;

import com.caucho.config.program.ConfigProgram;
import com.caucho.jca.cfg.ResourceConfig;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The configuration for a resource-default.
 */
public class ResourceDefault {
  private static final L10N L = new L10N(ResourceDefault.class);
  private static final Logger log
    = Logger.getLogger(ResourceDefault.class.getName());

  private static EnvironmentLocal<ArrayList<ResourceConfig>> _localConfig
    = new EnvironmentLocal<ArrayList<ResourceConfig>>();

  private ResourceConfig _config = new ResourceConfig();


  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _config.addBuilderProgram(program);
  }

  @PostConstruct
  public void init()
  {
    ArrayList<ResourceConfig> defaultList = _localConfig.getLevel();

    if (defaultList == null) {
      defaultList = new ArrayList<ResourceConfig>();
      _localConfig.set(defaultList);
    }

    defaultList.add(_config);
  }

  public static ArrayList<ResourceConfig> getDefaultList()
  {
    return getDefaultList(Thread.currentThread().getContextClassLoader());
  }

  public static ArrayList<ResourceConfig> getDefaultList(ClassLoader loader)
  {
    ArrayList<ResourceConfig> defaultList = new ArrayList<ResourceConfig>();

    getDefaultList(defaultList, loader);

    return defaultList;
  }

  private static void getDefaultList(ArrayList<ResourceConfig> list,
                                     ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        getDefaultList(list, loader.getParent());

        ArrayList<ResourceConfig> defaultList = _localConfig.getLevel(loader);

        if (defaultList != null)
          list.addAll(defaultList);
      }
    }
  }
}
