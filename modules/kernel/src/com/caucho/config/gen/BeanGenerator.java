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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;
import com.caucho.make.ClassDependency;
import com.caucho.make.VersionDependency;
import com.caucho.vfs.PersistentDependency;

/**
 * Generates the skeleton for a bean.
 */
@Module
abstract public class BeanGenerator<X> extends GenClass
{
  private final AnnotatedType<X> _beanType;
  
  private DependencyComponent _dependency = new DependencyComponent();

  protected BeanGenerator(String fullClassName,
                          AnnotatedType<X> beanType)
  {
    super(fullClassName);
    
    _beanType = beanType;

    addDependency(new VersionDependency());
    addDependency(beanType.getJavaClass());
  }

  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }

  protected void addDependency(PersistentDependency depend)
  {
    _dependency.addDependency(depend);
  }

  protected void addDependency(Class<?> cl)
  {
    _dependency.addDependency(new ClassDependency(cl));
  }

  public String getBeanClassName()
  {
    return getBeanType().getJavaClass().getName();
  }

  public String getViewClassName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isRemote()
  {
    return false;
  }
  
  public boolean isProxy()
  {
    return false;
  }

  /**
   * Returns the introspected methods
   */
  public ArrayList<AspectGenerator<X>> getMethods()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected abstract AspectBeanFactory<X> getAspectBeanFactory();

  public void introspect()
  {
    
  }
  
  //
  // Java generation
  //
  
  public void generateClassStaticFields(JavaWriter out)
    throws IOException
  {
    out.println("private static final java.util.logging.Logger __caucho_log");
    out.println("  = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("private static RuntimeException __caucho_exception;");
    
    out.println();
    out.println("public static RuntimeException __caucho_getException()");
    out.println("{");
    out.println("  return __caucho_exception;");
    out.println("}");
  }
  
  /**
   * Generates the view contents
   */
  /*
  @Override
  abstract public void generate(JavaWriter out)
    throws IOException;
    */

  /**
   * Generates the view contents
   */
  public void generateDestroyViews(JavaWriter out)
    throws IOException
  {
    // view.generateDestroy(out);
  }

  /**
   * Generates context object's constructor
   */
  public void generateContextObjectConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates timer code
   */
  public void generateTimer(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Frees a bean instance.
   */
  public void generateFreeInstance(JavaWriter out, String name)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates any global destroy
   */
  public void generateDestroy(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    generatePreDestroy(out, map);
    
    out.println();
    out.println("public void __caucho_destroy(com.caucho.config.inject.CreationalContextImpl env)");
    out.println("{");
    out.pushDepth();

    generateDestroyImpl(out);
    
    out.println("__caucho_preDestroy();");

    out.popDepth();
    out.println("}");
  }
  
  protected void generateDestroyImpl(JavaWriter out)
    throws IOException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    for (AspectGenerator<X> method : getMethods()) {
      method.generateDestroy(out, map);
    }
  }

  /**
   * Generates constructor additions
   */
  public void generateProxyConstructor(JavaWriter out)
    throws IOException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    generateProxyConstructor(out, map);
  }

  /**
   * Generates constructor additions
   */
  public void generateProxyConstructor(JavaWriter out, 
                                       HashMap<String,Object> map)
    throws IOException
  {
    for (AspectGenerator<X> method : getMethods()) {
      method.generateProxyConstructor(out, map);
    }
  }

  /**
   * Generates constructor additions
   */
  public void generateBeanConstructor(JavaWriter out)
    throws IOException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    generateBeanConstructor(out, map);
  }

  /**
   * Generates constructor additions
   */
  public void generateBeanConstructor(JavaWriter out, 
                                      HashMap<String,Object> map)
    throws IOException
  {
    for (AspectGenerator<X> method : getMethods()) {
      method.generateBeanConstructor(out, map);
    }
  }

  /**
   * Generates prologue additions
   */
  public void generateBeanPrologue(JavaWriter out, 
                                   HashMap<String,Object> map)
    throws IOException
  {
    for (AspectGenerator<X> method : getMethods()) {
      method.generateBeanPrologue(out, map);
    }
  }

  protected void generateInject(JavaWriter out,
                                HashMap<String,Object> map)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_inject(Object []delegates"
                + ", com.caucho.config.inject.CreationalContextImpl<?> parentEnv)");
    out.println("{");
    out.pushDepth();

    generateInjectContent(out, map);

    out.popDepth();
    out.println("}");
  }

  protected void generateInjectContent(JavaWriter out,
                                       HashMap<String,Object> map)
    throws IOException
  {
    for (AspectGenerator<X> method : getMethods()) {
      method.generateInject(out, map);
    }

    getAspectBeanFactory().generateInject(out, map);
  }

  protected void generateDelegate(JavaWriter out,
                                HashMap<String,Object> map)
    throws IOException
  {
    out.println();
    out.println("public Object __caucho_getDelegate()");
    out.println("{");
    out.print("  return ");
    out.print(getAspectBeanFactory().getBeanInstance());
    out.println(";");
    out.println("}");
  }

  protected void generatePostConstruct(JavaWriter out, 
                                       HashMap<String,Object> map)
     throws IOException
  {
    ArrayList<Method> postConstructMethods 
      = getLifecycleMethods(PostConstruct.class);
    
    out.println();
    out.println("public void __caucho_postConstruct()");
    out.println("{");
    out.pushDepth();

    for (AspectGenerator<X> method : getMethods()) {
      method.generatePostConstruct(out, map);
    }
     
    getAspectBeanFactory().generatePostConstruct(out, map);

    out.popDepth();
    out.println("}");
    
    generateLifecycleMethodReflection(out, postConstructMethods, "postConstruct");
    
    out.println();
    out.println("private void __caucho_postConstructImpl()");
    out.println("  throws Exception");
    out.println("{");
    out.pushDepth();
    
    generateLifecycleMethods(out, postConstructMethods, "postConstruct");
    
    out.popDepth();
    out.println("}");
  }

  private void generatePreDestroy(JavaWriter out, 
                                  HashMap<String,Object> map)
     throws IOException
  {
    ArrayList<Method> preDestroyMethods = getLifecycleMethods(PreDestroy.class);
    
    out.println();
    out.println("public void __caucho_preDestroy()");
    out.println("{");
    out.pushDepth();

    for (AspectGenerator<X> method : getMethods()) {
      method.generatePreDestroy(out, map);
    }
     
    getAspectBeanFactory().generatePreDestroy(out, map);

    out.popDepth();
    out.println("}");
    
    generateLifecycleMethodReflection(out, preDestroyMethods, "preDestroy");
    
    out.println();
    out.println("private void __caucho_preDestroyImplConstructImpl()");
    out.println("  throws Exception");
    out.println("{");
    out.pushDepth();
    
    generateLifecycleMethods(out, preDestroyMethods, "preDestroy");
    
    out.popDepth();
    out.println("}");
  }
  
  protected void generateLifecycleMethodReflection(JavaWriter out,
                                                   ArrayList<Method> methods,
                                                   String lifecycleType)
    throws IOException
  {
    for (int i = 0; i < methods.size(); i++) {
      Method method = methods.get(i);
      

      out.println();
      out.println("java.lang.reflect.Method __caucho_" + lifecycleType + "_" + i + " = ");
      
      out.print("  ");
      
      out.printClass(CandiUtil.class);
      out.print(".findMethod(");
      out.printClass(method.getDeclaringClass());
      out.print(".class, \"");
      out.print(method.getName());
      out.println("\");");
    }
  }
  
  protected void generateLifecycleMethods(JavaWriter out,
                                          ArrayList<Method> methods,
                                          String lifecycleType)
    throws IOException
  {
    for (int i = 0; i < methods.size(); i++) {
      out.println("try {");
      out.pushDepth();
      
      out.print("__caucho_" + lifecycleType + "_" + i + ".invoke(");
      out.print(getAspectBeanFactory().getBeanInstance());
      out.println(");");
      
      out.popDepth();
      out.println("} catch (java.lang.reflect.InvocationTargetException ex) {");
      out.println("  if (ex.getCause() instanceof Exception)");
      out.println("    throw (Exception) ex.getCause();");
      out.println("  else");
      out.println("    throw ex;");
      out.println("}");
    }
  }

  
  protected ArrayList<Method> 
  getLifecycleMethods(Class<? extends Annotation> annType)
  {
    ArrayList<Method> methods = new ArrayList<Method>();
    
    for (AnnotatedMethod<? super X> method : getBeanType().getMethods()) {
      if (! method.isAnnotationPresent(annType))
        continue;
      
      if (method.getParameters().size() != 0)
        continue;
      
      methods.add(method.getJavaMember());
    }
    
    Collections.sort(methods, new PostConstructComparator());
    
    return methods;
  }

  protected void generateEpilogue(JavaWriter out, HashMap<String,Object> map)
     throws IOException
  {
    getAspectBeanFactory().generateEpilogue(out, map);
  }

  /**
   * Generates view's business methods
   */
  public void generateBusinessMethods(JavaWriter out,
                                      HashMap<String,Object> map)
    throws IOException
  {
    for (AspectGenerator<X> method : getMethods()) {
      method.generate(out, map);
    }
  }

  protected void generateDependency(JavaWriter out)
    throws IOException
  {
    _dependency.generate(out);
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _beanType.getJavaClass().getSimpleName() + "]");
  }
  
  static class PostConstructComparator implements Comparator<Method> {
    @Override
    public int compare(Method a, Method b)
    {
      Class<?> classA = a.getDeclaringClass();
      Class<?> classB = b.getDeclaringClass();
      
      if (classA == classB)
        return a.getName().compareTo(b.getName());
      else if (classA.isAssignableFrom(classB))
        return -1;
      else if (classB.isAssignableFrom(classA))
        return 1;
      else
        return a.getName().compareTo(b.getName());
    }
  }
}
