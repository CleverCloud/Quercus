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

package com.caucho.quercus.lib;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java functions
 */
public class JavaModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(JavaModule.class.getName());

  private static final L10N L = new L10N(JavaModule.class);

  /**
   * Call the Java constructor and return the wrapped Java object.
   * If constructor is not available, then return static class definition.
   */
  public static Object java(Env env,
                            String className,
                            Value []args)
  {
    try {
      JavaClassDef def = env.getJavaClassDefinition(className);

      if (def == null) {
        env.warning(L.l("could not find Java class {0}", className));
        return null;
      }
      
      Value newObj = def.callNew(env, args);

      if (newObj.isNull())
        return new JavaValue(env, null, def);
      else
        return newObj;

    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  /**
   * Returns the static class definition of a Java class.
   */
  public static Object java_class(Env env,
                                  String className)
  {
    try {
      JavaClassDef def = env.getJavaClassDefinition(className);

      if (def == null) {
        env.warning(L.l("could not find Java class {0}", className));
        return null;
      }
      
      return new JavaValue(env, def.getType(), def);
    } catch (Throwable e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  /**
   * Returns the name of the java class.
   */
  public static String get_java_class_name(Env env, Value value)
  {
    if (value instanceof JavaValue)  {
      Object obj =  value.toJavaObject();

      if (obj == null)
        return String.valueOf(null);
      else
        return obj.getClass().getName();
    }

    return value.getClass().getName();
  }
}
