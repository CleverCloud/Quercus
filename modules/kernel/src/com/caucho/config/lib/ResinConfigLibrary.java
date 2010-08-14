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

package com.caucho.config.lib;

import com.caucho.config.Config;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.naming.Jndi;

import java.lang.reflect.*;
import java.util.logging.*;

/**
 * Library of static config functions.
 */
public class ResinConfigLibrary {
  private static Logger _log;
  
  public static boolean class_exists(String className)
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      return cl != null;
    } catch (Throwable e) {
      log().log(Level.FINEST, e.toString(), e);
    }

    return false;
  }
  
  public static Object jndi(String jndiName)
  {
    return jndi_lookup(jndiName);
  }
  
  public static Object jndi_lookup(String jndiName)
  {
    return Jndi.lookup(jndiName);
  }

  public static void configure(InjectManager webBeans)
  {
    try {
      for (Method m : ResinConfigLibrary.class.getMethods()) {
        if (! Modifier.isStatic(m.getModifiers()))
          continue;
        if (! Modifier.isPublic(m.getModifiers()))
          continue;
        if (m.getName().equals("configure"))
          continue;

        //BeanFactory factory = webBeans.createBeanFactory(m.getClass());

        // webBeans.addBean(factory.name(m.getName()).singleton(m));
        Config.setProperty(m.getName(), m);
      }
    } catch (Exception e) {
      log().log(Level.FINE, e.toString(), e);
    }
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinConfigLibrary.class.getName());

    return _log;
  }
}
