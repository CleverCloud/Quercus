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

package com.caucho.es.wrapper;

import com.caucho.es.Call;
import com.caucho.es.ESBase;
import com.caucho.es.ESNull;

/**
 * Dynamic selection of overloaded methods.  This is really a last
 * resort.
 */
public class MethodDispatcher {
  private Class [][]parameterTypes;

  public MethodDispatcher(Class [][]parameterTypes)
  {
    this.parameterTypes = parameterTypes;
  }

  /**
   * Selects the best method among the list.
   */
  public int select(Call call, int length)
    throws Exception
  {
    int bestCost = Integer.MAX_VALUE;
    int bestMethod = -1;

    for (int i = 0; i < parameterTypes.length; i++) {
      Class []parameters = parameterTypes[i];

      int cost = 0;
      for (int j = 0; j < parameters.length; j++) {
        ESBase obj = call.getArg(j, length);
        Class cl = obj.getJavaType();
        Class param = parameters[j];

        if (cl.equals(param))
          continue;

        // cl is the actual class, so don't need to worry about casting
        else if (param.isAssignableFrom(cl))
          cost += 10;

        else if (obj instanceof ESNull)
          cost += 50;

        else if (cl.equals(void.class))
          cost += 50;

        else if (cl.isPrimitive() && param.isPrimitive())
          cost += 100;

        else {
          cost = Integer.MAX_VALUE;
          break;
        }
      }

      if (cost < bestCost) {
        bestMethod = i;
        bestCost = cost;
      }
    }

    return bestMethod;
  }
}

