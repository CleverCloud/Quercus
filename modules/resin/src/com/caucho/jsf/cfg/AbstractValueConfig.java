/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.cfg;

import java.lang.reflect.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.util.*;

abstract public class AbstractValueConfig
{
  abstract AbstractValue getValue(Class type);

  protected static Method findGetter(Class type, String name)
  {
    if (type == null)
      return null;

    for (Method method : type.getDeclaredMethods()) {
      if (! method.getName().equals(name))
        continue;
      else if (method.getParameterTypes().length != 0)
        continue;
      else if (Modifier.isStatic(method.getModifiers()))
        continue;

      return method;
    }

    return findGetter(type.getSuperclass(), name);
  }

  protected static Method findSetter(Class type, String name)
  {
    if (type == null)
      return null;

    for (Method method : type.getDeclaredMethods()) {
      if (! method.getName().equals(name))
        continue;
      else if (method.getParameterTypes().length != 1)
        continue;
      else if (Modifier.isStatic(method.getModifiers()))
        continue;

      return method;
    }

    return findSetter(type.getSuperclass(), name);
  }
}
