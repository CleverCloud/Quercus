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
import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.Stateful;
import javax.ejb.SessionBean;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.session.StatefulHandle;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Generates the skeleton for a stateful bean.
 */
@Module
public class StatefulGenerator<X> extends SessionGenerator<X> 
{
  private final AspectBeanFactory<X> _aspectBeanFactory;
  
  public StatefulGenerator(String ejbName, AnnotatedType<X> beanType,
                           ArrayList<AnnotatedType<? super X>> localApi,
                           AnnotatedType<X> localBean,
                           ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    super(ejbName, beanType, localApi, localBean, remoteApi, 
          Stateful.class.getSimpleName());
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory = new StatefulAspectBeanFactory<X>(manager, getBeanType());
  }

  @Override
  protected AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _aspectBeanFactory;
  }
  
  @Override
  public boolean isStateless()
  {
    return false;
  }
  
  @Override
  protected boolean isTimerSupported()
  {
    return false;
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which
   * calls an instance class.
   */
  @Override
  public boolean isProxy()
  {
    return true;
  }

  @Override
  public String getViewClassName()
  {
    return "StatefulProxy";
  }

  @Override
  public String getBeanClassName()
  {
    // XXX: 4.0.7
    // return getViewClass().getJavaClass().getSimpleName() + "__Bean";
    return getBeanType().getJavaClass().getName();
  }
  
  //
  // introspection
  //

  /**
   * Scans for the @Local interfaces
   */
  @Override
  protected AnnotatedType<? super X> introspectLocalDefault() 
  {
    return getBeanType();
  }
  
  //
  // Java code generation
  //

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.config.inject.CreationalContextImpl;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    generateClassHeader(out);
    
    out.println("{");
    out.pushDepth();

    generateClassStaticFields(out);
    
    generateClassContent(out);

    generateDependency(out);
    
    out.popDepth();
    out.println("}");
  }
  
  private void generateClassHeader(JavaWriter out) 
    throws IOException
  {
    out.println();
    out.println("public class " + getClassName() + "<T>");

    if (hasNoInterfaceView())
      out.println("  extends " + getBeanType().getJavaClass().getName());

    out.print("  implements SessionProxyFactory<T>, com.caucho.config.gen.CandiEnhancedBean, java.io.Serializable");

    for (AnnotatedType<? super X> api : getLocalApi()) {
      out.print(", " + api.getJavaClass().getName());
    }
    out.println();
  }

  @Override
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private transient StatefulManager _manager;");
    out.println("private transient StatefulContext _context;");

    out.println("private " + getBeanClassName() + " _bean;");

    out.println("private transient boolean _isActive;");
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    
    generateContentImpl(out, map);
    
    generateSerialization(out);
  }
  
  protected void generateContentImpl(JavaWriter out,
                                     HashMap<String,Object> map)
    throws IOException
  {
    
    generateConstructor(out, map);
    
    generateProxyFactory(out);

    generateBeanPrologue(out, map);

    generateBusinessMethods(out, map);
    
    generateEpilogue(out, map);
    generateInject(out, map);
    generateDelegate(out, map);
    generatePostConstruct(out, map);
    generateDestroy(out, map);
  }

  private void generateConstructor(JavaWriter out,
                                   HashMap<String,Object> map)
    throws IOException
  {
    // generateProxyConstructor(out);
    
    out.println();
    out.print("public " + getClassName() + "(StatefulManager manager, ");
    out.println("StatefulContext context)");
    out.println("{");
    out.pushDepth();

    out.println("_manager = manager;");
    out.println("_context = context;");
    
    out.println("if (__caucho_exception != null)");
    out.println("  throw __caucho_exception;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("private " + getClassName() + "(StatefulManager manager"
                + ", StatefulContext context"
                + ", CreationalContextImpl<T> env)");
    out.println("{");
    out.pushDepth();
    
    out.println("_manager = manager;");
    out.println("_context = context;");

    out.println("_bean = (" + getBeanClassName() + ") _manager.newInstance(env);");
    
    // ejb/5011
    if (SessionBean.class.isAssignableFrom(getBeanType().getJavaClass())) {
      out.println("_bean.setSessionContext(context);");
    }
    
    generateContextObjectConstructor(out);

    out.popDepth();
    out.println("}");
  }

  private void generateProxyFactory(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public T __caucho_createProxy(CreationalContextImpl<T> env)");
    out.println("{");
    out.println("  return (T) new " + getClassName() + "(_manager, _context, env);");
    out.println("}");
  }
 
  @Override
  public void generateDestroy(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateDestroy(out, map);

    out.println();
    out.println("@Override");
    out.println("public void __caucho_destroy() {}");
  }
  
  @Override
  protected void generateDestroyImpl(JavaWriter out)
    throws IOException
  {
    super.generateDestroyImpl(out);
  
    out.println("_manager.destroy(_bean, env);");
    out.println("_bean = null;");
  }
  
  private void generateSerialization(JavaWriter out)
    throws IOException
  {
    out.println("private Object writeReplace()");
    out.println("{");
    out.pushDepth();
    
    out.print("return new ");
    out.printClass(StatefulHandle.class);
    out.println("(_manager.getEJBName(), null);");
    
    out.popDepth();
    out.println("}");
  }
}
