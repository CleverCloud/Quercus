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

package com.caucho.ejb.hessian;

import com.caucho.hessian.io.JavaDeserializer;

import java.lang.reflect.Constructor;

/**
 * Serializing an object for known object types.
 */
public class ThrowableDeserializer extends JavaDeserializer {
  private Constructor constructor;
  
  public ThrowableDeserializer(Class cl)
  {
    super(cl);

    try {
      Constructor zeroArg = cl.getConstructor(new Class[0]);
      if (zeroArg != null)
        return;
    } catch (Exception e) {
    }

    try {
      constructor = cl.getConstructor(new Class[] { String.class });
    } catch (Exception e) {
    }
  }

  protected Object instantiate()
    throws Exception
  {
    if (constructor != null)
      return constructor.newInstance(new Object[1]);
    else
      return super.instantiate();
  }
}
