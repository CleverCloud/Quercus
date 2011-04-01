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
public class MethodHeadGenerator<X> extends AbstractAspectGenerator<X> {
  public MethodHeadGenerator(MethodHeadFactory<X> factory,
                             AnnotatedMethod<? super X> method,
                             AspectGenerator<X> next)
  {
    super(factory, method, next);
  }

  protected boolean isOverride()
  {
    return true;
  }

  //
  // business method interception
  //

  //
  // generation for the actual method
  //

  /**
   * Generates the overridden method.
   */
  public final void generate(JavaWriter out,
                             HashMap<String,Object> prologueMap)
    throws IOException
  {
    generateMethodPrologue(out, prologueMap);
    
    String prefix = "";
    String suffix = "";
 
    int modifiers = getJavaMethod().getModifiers();
    String accessModifier = null;
    
    if (Modifier.isPublic(modifiers))
      accessModifier = "public";
    else if (Modifier.isProtected(modifiers))
      accessModifier = "protected";
    /*
    else
      throw new IllegalStateException(getJavaMethod().toString()
                                      + " must be public or protected");
     */

    AspectGeneratorUtil.generateHeader(out, 
                                       isOverride(),
                                       accessModifier, 
                                       prefix, 
                                       getJavaMethod(), 
                                       suffix, 
                                       getThrowsExceptions());

    out.println("{");
    out.pushDepth();

    generateContent(out, prologueMap);

    out.popDepth();
    out.println("}");
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof MethodHeadGenerator<?>))
      return false;

    MethodHeadGenerator<?> bizMethod = (MethodHeadGenerator<?>) o;

    return getJavaMethod().getName().equals(bizMethod.getJavaMethod().getName());
  }
}
