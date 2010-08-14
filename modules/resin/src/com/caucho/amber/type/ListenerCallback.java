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

package com.caucho.amber.type;

import java.lang.reflect.*;

import com.caucho.amber.*;

/**
 * A listener callback
 */
public class ListenerCallback
{
  private final Object _listener;
  private final Method _method;

  public ListenerCallback(Object listener, Method method)
  {
    _listener = listener;
    _method = method;

    method.setAccessible(true);
  }

  public void invoke(Object entity)
  {
    try {
      _method.invoke(_listener, entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
      else
        throw new AmberRuntimeException(e.getCause());
    } catch (Exception e) {
      throw new AmberRuntimeException(e.getCause());
    }
  }
}
