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

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.*;

/**
 * A delegate that intercepts array acces methods on the
 * target objects that implement
 * the {@link com.caucho.quercus.lib.spl.ArrayAccess} interface.
 */
public class ArrayAccessDelegate implements ArrayDelegate
{
  private static final StringValue OFFSET_GET
    = new ConstStringValue("offsetGet");
  private static final StringValue OFFSET_SET
    = new ConstStringValue("offsetSet");
  private static final StringValue OFFSET_UNSET
    = new ConstStringValue("offsetUnset");
  private static final StringValue OFFSET_EXISTS
    = new ConstStringValue("offsetExists");
  
  public Value get(ObjectValue qThis, Value index)
  {
    Env env = Env.getInstance();
    
    return qThis.callMethod(env, OFFSET_GET, index);
  }

  public Value put(ObjectValue qThis, Value index, Value value)
  {
    Env env = Env.getInstance();

    return qThis.callMethod(env, OFFSET_SET, index, value);
  }

  public Value put(ObjectValue qThis, Value index)
  {
    Env env = Env.getInstance();
    
    return qThis.callMethod(env, OFFSET_SET, UnsetValue.UNSET, index);
  }

  public boolean isset(ObjectValue qThis, Value index)
  {
    Env env = Env.getInstance();
    
    return qThis.callMethod(env, OFFSET_EXISTS, index).toBoolean();
  }

  public Value unset(ObjectValue qThis, Value index)
  {
    Env env = Env.getInstance();
    
    return qThis.callMethod(env, OFFSET_UNSET, index);
  }
}
