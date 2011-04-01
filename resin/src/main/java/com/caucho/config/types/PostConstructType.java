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

import com.caucho.config.j2ee.PostConstructProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.L10N;

import java.lang.reflect.*;
import java.util.logging.Logger;

/**
 * Configuration for the init-param pattern.
 */
public class PostConstructType
{
  private static Logger log
    = Logger.getLogger(PostConstructType.class.getName());
  private static L10N L = new L10N(PostConstructType.class);

  private String _declaringClass;
  private String _methodName;

  /**
   * Sets the id
   */
  public void setId(String id)
  {
  }

  public void setLifecycleCallbackClass(String name)
  {
    _declaringClass = name;
  }

  public void setLifecycleCallbackMethod(String name)
  {
    _methodName = name;
  }

  public ConfigProgram getProgram(Class cl)
  {
    if (cl == null) {
      throw new ConfigException(L.l("'{0}' is an unknown callback method.",
                                    _methodName));
    }

    if (_declaringClass != null && ! _declaringClass.equals(cl.getName()))
      return getProgram(cl.getSuperclass());

    Method []methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (method.getName().equals(_methodName)
          && method.getParameterTypes().length == 0) {
        return new PostConstructProgram(method);
      }
    }

    return getProgram(cl.getSuperclass());
  }

  public String toString()
  {
    return "PostConstructType[" + _methodName + "]";
  }
}
