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
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * The dispatch to the actual method call for the aspect chain.
 */
@Module
public class MethodTailGenerator<X> extends NullGenerator<X> {
  protected final MethodTailFactory<X> _factory;
  protected final AnnotatedMethod<? super X> _method;

  public MethodTailGenerator(MethodTailFactory<X> factory,
                             AnnotatedMethod<? super X> method)
  {
    _factory = factory;
    _method = method;
  }

  /**
   * Generates the call to the implementation bean.
   *
   * @param superVar java code to reference the implementation
   */
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    String superVar = _factory.getAspectBeanFactory().getBeanSuper();
    
    out.println();

    Method javaImplMethod = _method.getJavaMember();
    
    if (! void.class.equals(javaImplMethod.getReturnType())) {
      out.print("result = ");
    }

    out.print(superVar + "." + javaImplMethod.getName() + "(");

    Class<?>[] types = javaImplMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print(" a" + i);
    }

    out.println(");");

    /*
    // ejb/12b0
    if (! "super".equals(superVar))
      generatePostCall(out);
    */
  }

}
