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

package com.caucho.ejb.gen;

import java.io.IOException;

import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.MethodHeadGenerator;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateful local business method
 */
@Module
public class StatefulMethodHeadGenerator<X> extends MethodHeadGenerator<X>
{
  private boolean _isRemoveRetainIfException;
  
  public StatefulMethodHeadGenerator(StatefulMethodHeadFactory<X> factory,
                                     AnnotatedMethod<? super X> method,
                                     AspectGenerator<X> next)
  {
    super(factory, method, next);
  }

  /*
  @Override
  public void setRemoveRetainIfException(boolean isRetain)
  {
    _isRemoveRetainIfException = isRetain;
  }
  */

  protected TransactionAttributeType getDefaultTransactionType()
  {
    return TransactionAttributeType.REQUIRED;
  }
  
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    super.generatePreTry(out);
    
    String beanClassName = getJavaClass().getName();
    
    out.println(beanClassName + " bean = _bean;");
    
    out.println();
    out.println("if (bean == null)");
    out.println("  throw new javax.ejb.NoSuchEJBException(\"stateful instance "
                + getJavaClass().getSimpleName() + " is no longer valid\");");

    out.println();
    out.println("if (_isActive)");
    out.println("  throw new EJBException(\"session bean is not reentrant\");");
    out.println();

    out.println("boolean isValid = false;");
    
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
  }

  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("thread.setContextClassLoader(_manager.getClassLoader());");
    out.println("_isActive = true;");
    
    super.generatePreCall(out);
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
    out.println("isValid = true;");
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    // ejb/5070
    super.generateApplicationException(out, exn);
    
    out.println("isValid = true;");
  }

  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("_isActive = false;");
    out.println();
    
    if (! _isRemoveRetainIfException) {
      out.println("if (! isValid) {");
      out.pushDepth();
    
      out.println("_bean = null;");
      out.println();
      out.println("if (bean != null)");
      out.print("  _manager.destroyInstance(");
      out.print(getBeanFactory().getBeanInstance());
      out.println(");");
    
      out.popDepth();
      out.println("}");
    }
    
    out.println("thread.setContextClassLoader(oldLoader);");
    
    super.generateFinally(out);
  }
}
