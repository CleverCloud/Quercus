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
package com.caucho.config.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

import javax.ejb.ApplicationException;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the method aspect code for the head or proxy of the method.
 */
@Module
public class AspectGeneratorUtil {
  /**
   * Generates the method's signature before the call:
   *
   * <code><pre>
   * MyValue myCall(int a0, String, a1, ...)
   *   throws MyException, ...
   * </pre><?code>
   * @param prefix TODO
  */
  public static void generateHeader(JavaWriter out,
                                    boolean isOverride,
                                    String accessModifier,
                                    String prefix,
                                    Method method,
                                    String suffix, 
                                    Class<?> []exnList)
    throws IOException
  {
    out.println();
    
    if (isOverride)
      out.println("@Override");

    if (accessModifier != null) {
      out.print(accessModifier);
      out.print(" ");
    }

    out.printClass(method.getReturnType());
    out.print(" ");
    out.print(prefix + method.getName() + suffix);
    out.print("(");

    Class<?>[] types = method.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];

      if (i != 0)
        out.print(", ");

      if (i == types.length - 1 && type.isArray() && method.isVarArgs()) {
        out.printClass(type.getComponentType());
        out.print("...");
      } else
        out.printClass(type);

      out.print(" a" + i);
    }

    out.println(")");
    
    generateThrows(out, exnList);
  }

  /**
   * Generates the method's "throws" declaration in the
   * method signature.
   *
   * @param out generated Java output
   * @param exnCls the exception classes
   */
  protected static void generateThrows(JavaWriter out,
                                       Class<?>[] exnCls)
    throws IOException
  {
    if (exnCls.length == 0)
      return;

    out.print(" throws ");

    for (int i = 0; i < exnCls.length; i++) {
      if (i != 0)
        out.print(", ");

      out.printClass(exnCls[i]);
    }
    out.println();
  }
}
