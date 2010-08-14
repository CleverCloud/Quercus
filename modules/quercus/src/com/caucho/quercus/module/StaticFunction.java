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

package com.caucho.quercus.module;

import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.env.JavaInvoker;
import com.caucho.util.L10N;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Represents the introspected static function information.
 */
public class StaticFunction extends JavaInvoker {
  private static final L10N L = new L10N(StaticFunction.class);
  private static final Logger log =
    Logger.getLogger(StaticFunction.class.getName());

  protected final QuercusModule _quercusModule;
  protected final Method _method;
  private final int _argLength;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public StaticFunction(ModuleContext moduleContext,
                        QuercusModule quercusModule,
                        Method method)
  {
    super(moduleContext,
          getName(method),
          method.getParameterTypes(),
          method.getParameterAnnotations(),
          method.getAnnotations(),
          method.getReturnType());

    _method = method;
    _argLength = method.getParameterTypes().length;
    _quercusModule = quercusModule;
  }
  
  /*
   * Returns true for a static function.
   */
  public boolean isStatic()
  {
    return true;
  }

  private static String getName(Method method)
  {
    String name;

    Name nameAnn = method.getAnnotation(Name.class);

    if (nameAnn != null)
      name = nameAnn.value();
    else
      name = method.getName();

    return name;
  }
  
  @Override
  public String getDeclaringClassName()
  {
    return _method.getDeclaringClass().getSimpleName();
  }

  /**
   * Returns the owning module object.
   *
   * @return the module object
   */
  public QuercusModule getModule()
  {
    return _quercusModule;
  }

  /**
   * Returns the function's method.
   *
   * @return the reflection method.
   */
  public Method getMethod()
  {
    return _method;
  }

  public int getArgumentLength()
  {
    return _argLength;
  }

  /**
   * Evalutes the function.
   */
  @Override
  public Object invoke(Object obj, Object []javaArgs)
  {
    try {
      return _method.invoke(_quercusModule, javaArgs);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(toString(_method, javaArgs), e);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      // php/03k5
      // exceptions from invoked calls are wrapped inside
      // InvocationTargetException

      Throwable cause = e.getCause();

      if (cause instanceof QuercusExitException)
        throw ((QuercusExitException) cause);
      else if (cause != null)
        throw QuercusModuleException.create(cause);
      else
        throw QuercusModuleException.create(e);
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  private String toString(Method method, Object []javaArgs)
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(method.getDeclaringClass().getName());
    sb.append(".");
    sb.append(method.getName());
    sb.append("(");

    for (int i = 0; i < javaArgs.length; i++) {
      if (i != 0)
        sb.append(", ");

      sb.append(javaArgs[i]);
    }
      
    sb.append(")");

    return sb.toString();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
