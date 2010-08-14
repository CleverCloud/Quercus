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
 */

package com.caucho.ejb.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.gen.AbstractAspectGenerator;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.AspectGeneratorUtil;
import com.caucho.java.JavaWriter;

public class StatelessScheduledMethodHeadGenerator<X> 
  extends AbstractAspectGenerator<X> 
{
  public StatelessScheduledMethodHeadGenerator(StatelessScheduledMethodHeadFactory<X> factory,
                                               AnnotatedMethod<? super X> method,
                                               AspectGenerator<X> next)
  {
    super(factory, method, next);
  }

  public boolean isOverride()
  {
    return false;
  }
  
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    out.println("isValid = true;");
    
    super.generateApplicationException(out, exn);
  }
  
  /**
   * Generates the overridden method.
   */
  @Override
  public void generate(JavaWriter out,
                             HashMap<String,Object> prologueMap)
    throws IOException
  {
    generateMethodPrologue(out, prologueMap);
    
    String prefix = "__caucho_schedule_";
    String suffix = "";
    String accessModifier = "public";

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
}
