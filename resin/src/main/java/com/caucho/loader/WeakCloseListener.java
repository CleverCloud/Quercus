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

package com.caucho.loader;

import com.caucho.util.L10N;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Waits for the close event and calls a destroy() method.
 */
public class WeakCloseListener implements ClassLoaderListener {
  private static final L10N L = new L10N(CloseListener.class);
  private static final Logger log
    = Logger.getLogger(CloseListener.class.getName());

  private WeakReference<Object> _resourceRef;

  /**
   * Creates the new close listener.
   *
   * @param resource the resource which needs closing
   */
  public WeakCloseListener(Object resource)
  {
    _resourceRef = new WeakReference<Object>(resource);
  }

  /**
   * Handles the case where a class loader is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }
  
  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    Object resource = _resourceRef.get();
    if (resource == null)
      return;
    
    Method destroy = CloseListener.getDestroyMethod(resource.getClass());

    if (destroy == null)
      return;
    
    try {
      destroy.invoke(resource, (Object []) null);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    return "WeakCloseListener[" + _resourceRef.get() + "]";
  }
}

