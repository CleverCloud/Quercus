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

import javax.ejb.Singleton;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.CandiEnhancedBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a singleton bean.
 */
@Module
public class SingletonGenerator<X> extends SessionGenerator<X> {
  
  private final AspectBeanFactory<X> _aspectBeanFactory;

  public SingletonGenerator(String ejbName, AnnotatedType<X> ejbClass,
                            ArrayList<AnnotatedType<? super X>> localApi,
                            AnnotatedType<X> localBean,
                            ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    super(ejbName, ejbClass, localApi, localBean, remoteApi, 
          Singleton.class.getSimpleName());
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory = new SingletonAspectBeanFactory<X>(manager, getBeanType());
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
    return true;
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which
   * calls an instance class.
   */
  public boolean isProxy()
  {
    return true;
  }

  @Override
  public String getViewClassName()
  {
    return "SingletonView";
  }

  @Override
  public String getBeanClassName()
  {
    return "Bean";
  }

  /**
   * Scans for the @Local interfaces
   */
  @Override
  protected AnnotatedType<? super X> introspectLocalDefault()
  {
    return getBeanType();
  }

  /**
   * Generates the singleton session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {    
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");
    out.println("import javax.enterprise.context.spi.CreationalContext;");

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
    
    out.print("  implements SessionProxyFactory<T>");
    out.print(", " + CandiEnhancedBean.class.getName());

    for (AnnotatedType<? super X> apiType : getLocalApi()) {
      out.print(", " + apiType.getJavaClass().getName());
    }
    out.println();
  }

  @Override
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private transient SingletonContext _context;");
    out.println("private transient SingletonManager _manager;");

    String beanClassName = getBeanType().getJavaClass().getName();
    
    out.println("private " + beanClassName + " _bean;");

    out.println("private transient boolean _isValid;");
    out.println("private transient boolean _isActive;");
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    
    generateConstructor(out);
    
    generateProxyFactory(out);
    
    generateContentImpl(out, map);
    
    // generateSerialization(out);

  }

  @Override
  protected void generateInjectContent(JavaWriter out,
                                       HashMap<String,Object> map)
    throws IOException
  {
    String beanClassName = getBeanType().getJavaClass().getName();
    
    out.println("_bean = (" + beanClassName + ") _manager.newInstance(parentEnv);");
    
    super.generateInjectContent(out, map);
  }

  private void generateConstructor(JavaWriter out)
    throws IOException
  {
    String beanClassName = getBeanType().getJavaClass().getName();
    
    out.println();
    out.print("public " + getClassName() + "(SingletonManager manager");
    out.println(", SingletonContext context)");
    out.println("{");
    out.pushDepth();

    out.println("_manager = manager;");

    out.popDepth();
    out.println("}");

    out.println();
    out.print("private ");
    out.println(getClassName() 
                + "(SingletonManager manager"
                + ", com.caucho.config.inject.CreationalContextImpl<T> env)");
    out.println("{");
    out.pushDepth();

    out.println("_manager = manager;");
    out.println("_isValid = true;");
  
    generateContextObjectConstructor(out);

    out.popDepth();
    out.println("}");
  }

  private void generateProxyFactory(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public T __caucho_createProxy(com.caucho.config.inject.CreationalContextImpl<T> env)");
    out.println("{");
    out.println("  return (T) new " + getClassName() + "(_manager, env);");
    out.println("}");
    out.println();

    out.println("@Override");
    out.println("public void __caucho_destroy()");
    out.println("{");
    out.println("}");
  }
}
