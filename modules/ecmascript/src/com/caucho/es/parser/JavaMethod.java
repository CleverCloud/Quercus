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

package com.caucho.es.parser;

import com.caucho.es.Call;
import com.caucho.es.wrapper.ESBeanInfo;
import com.caucho.es.wrapper.ESIntrospector;
import com.caucho.es.wrapper.ESMethodDescriptor;

import java.util.ArrayList;

/**
 * Utility class for selecting the best Java method matching the
 * JavaScript call arguments.
 */
class JavaMethod {
  /**
   * Returns the best matching method for the class.
   */
  static ESMethodDescriptor bestMethod(Class cl, String name,
                                       boolean isStatic, ArrayList args)
  {
    ESBeanInfo beanInfo;

    try {
      beanInfo = ESIntrospector.getBeanInfo(cl);
    } catch (Exception e) {
      return null;
    }

    ArrayList overload;

    if (isStatic)
      overload = beanInfo.getStaticMethods(name);
    else
      overload = beanInfo.getMethods(name);

    if (overload == null)
      return null;
    
    ESMethodDescriptor []methods;

    // special case of (Call, int)
    if (overload.size() > 2) {
      methods = (ESMethodDescriptor []) overload.get(2);
      for (int i = 0; methods != null && i < methods.length; i++) {
        Class []param = methods[i].getParameterTypes();
        
        if (param[0].equals(Call.class) && param[1].equals(int.class))
          return null;
      }
    }
    
    methods = (ESMethodDescriptor []) overload.get(args.size());

    if (methods == null)
      return null;
      
    ESMethodDescriptor bestMethod = null;
    int bestCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < methods.length; i++) {
      ESMethodDescriptor method = methods[i];
      

      Class []param = method.getParameterTypes();

      int cost = methodCost(param, args);

      if (cost < bestCost && cost < 1000000) {
        bestCost = cost;
        bestMethod = method;
      }
    }

    return bestMethod;
  }

  /**
   * Return the class of the method.
   */
  static int methodCost(Class []param, ArrayList args)
  {
    int cost = 0;

    if (param.length < args.size())
      cost += (args.size() - param.length) * 10000000;
    
    for (int j = 0; j < param.length; j++) {
      if (j >= args.size()) {
        cost += 1000000;
        continue;
      }
      Expr arg = (Expr) args.get(j);
      Class argType = arg.getJavaClass();

      if (argType == null)
        cost += 1000;
      else if (argType.equals(param[j])) {
      }
      else if (param[j].isAssignableFrom(argType))
        cost += 10;
      else if (argType.isAssignableFrom(param[j]))
        cost += 100;
      else
        cost += 1000000;
    }

    return cost;
  }
}
