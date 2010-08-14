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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.LocalBean;
import javax.ejb.MessageDrivenBean;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a message bean.
 */
@Module
public class MessageGenerator<X> extends BeanGenerator<X> {
  private AspectBeanFactory<X> _aspectBeanFactory;
  
  private ArrayList<AspectGenerator<X>> _businessMethods
    = new ArrayList<AspectGenerator<X>>();
  
  private ArrayList<AnnotatedMethod<? super X>> _annotatedMethods
    = new ArrayList<AnnotatedMethod<? super X>>();
  
  public MessageGenerator(String ejbName, AnnotatedType<X> ejbClass)
  {
    super(toFullClassName(ejbName, ejbClass.getJavaClass().getName()), ejbClass);
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory = new MessageAspectBeanFactory<X>(manager, getBeanType());
  }

  private static String toFullClassName(String ejbName, String className)
  {
    StringBuilder sb = new StringBuilder();

    /*
    sb.append("_ejb.");

    if (! Character.isJavaIdentifierStart(ejbName.charAt(0)))
      sb.append('_');

    for (int i = 0; i < ejbName.length(); i++) {
      char ch = ejbName.charAt(i);

      if (ch == '/')
        sb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
      else
        sb.append('_');
    }

    sb.append(".");
    sb.append(className);
    */
    sb.append(className);
    sb.append("__MessageProxy");

    return sb.toString();
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  @Override
  public String getViewClassName()
  {
    return getClassName();
  }

  /**
   * Returns the introspected methods
   */
  @Override
  public ArrayList<AspectGenerator<X>> getMethods()
  {
    return _businessMethods;
  }

  /**
   * Introspects the bean.
   */
  @Override
  public void introspect()
  {
    super.introspect();
    
    introspectType(getBeanType());
    
    introspectImpl();
  }
  
  private void introspectType(AnnotatedType<? super X> type)
  {
    for (AnnotatedMethod<? super X> method : type.getMethods())
      introspectMethod(method);
  }
  
  private void introspectMethod(AnnotatedMethod<? super X> method)
  {
    /*
    AnnotatedMethod<? super X> oldMethod 
      = AnnotatedTypeUtil.findMethod(_annotatedMethods, method);
    
    if (oldMethod != null) {
      // XXX: merge annotations
      return;
    }
    
    AnnotatedMethod<? super X> baseMethod
      = AnnotatedTypeUtil.findMethod(getBeanType(), method);
    
    if (baseMethod == null)
      throw new IllegalStateException(L.l("{0} does not have a matching base method in {1}",
                                          method, getBeanType()));
    
    // XXX: merge annotations
    
    _annotatedMethods.add(baseMethod);
     */
    _annotatedMethods.add(method);
  }

  
  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  private void introspectImpl()
  {
    for (AnnotatedMethod<? super X> method : _annotatedMethods) {
      introspectMethodImpl(method);
    }
  }

  private void introspectMethodImpl(AnnotatedMethod<? super X> apiMethod)
  {
    Method javaMethod = apiMethod.getJavaMember();
      
    if (javaMethod.getDeclaringClass().equals(Object.class))
      return;
    if (javaMethod.getDeclaringClass().getName().startsWith("javax.ejb."))
      return;
    /*
    if (javaMethod.getName().startsWith("ejb")) {
      throw new ConfigException(L.l("{0}: '{1}' must not start with 'ejb'.  The EJB spec reserves all methods starting with ejb.",
                                    javaMethod.getDeclaringClass(),
                                    javaMethod.getName()));
    }
    */
    
    int modifiers = javaMethod.getModifiers();

    if (! Modifier.isPublic(modifiers)) {
      return;
    }

    if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
      return;

    addBusinessMethod(apiMethod);
  }
  
  public void addBusinessMethod(AnnotatedMethod<? super X> method)
  {
    AspectGenerator<X> bizMethod
      = _aspectBeanFactory.create(method);
      
    if (bizMethod != null) {
      _businessMethods.add(bizMethod);
    }
  }

  /**
   * Generates the message session bean
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.message.*;");
    out.println();
    out.println("import java.util.*;");
    out.println("import java.lang.reflect.*;");
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");
    out.println("import javax.transaction.xa.*;");
    out.println("import javax.resource.spi.endpoint.*;");
    
    out.println();
    out.println("public class " + getClassName()
                + " extends " + getBeanType().getJavaClass().getName()
                + " implements MessageEndpoint, CauchoMessageEndpoint");
    out.println("{");
    out.pushDepth();

    /*
    // ejb/0931
    out.println();
    out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
    out.println("  = new com.caucho.ejb3.xa.XAManager();");
    */

    out.println("private static HashSet<Method> _xaMethods = new HashSet<Method>();");
    out.println();
    out.println("private MessageManager _server;");
    out.println("private XAResource _xaResource;");
    out.println("private boolean _isXa;");

    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("caucho.ejb.xa", "true");
    
    // view.generateContextPrologue(out);
    generateBeanPrologue(out, map);

    out.println();
    //out.println("public " + getClassName() + "(MessageManager server)");
    out.println("public " + getClassName() + "()");
    out.println("{");
    out.pushDepth();

    // out.println("_server = server;");
    /*
    if (MessageDrivenBean.class.isAssignableFrom(getBeanType().getJavaClass())) {
      out.println("setMessageDrivenContext(server.getMessageContext());");
    }
    */

    generateBeanConstructor(out);

    out.popDepth();
    out.println("}");

    out.println();
    out.println("static {");
    out.pushDepth();
    out.println("try {");
    out.pushDepth();

    // XXX: 4.0.7
    /*
    for (AspectGenerator<X> bizMethod : _view.getMethods()) {
      if (REQUIRED.equals(bizMethod.getXa().getTransactionType())) {
        Method api = bizMethod.getApiMethod().getJavaMember();

        out.print("_xaMethods.add(");
        out.printClass(api.getDeclaringClass());
        out.print(".class.getMethod(\"");
        out.print(api.getName());
        out.print("\", new Class[] { ");

        for (Class<?> cl : api.getParameterTypes()) {
          out.printClass(cl);
          out.print(".class, ");
        }
        out.println("}));");
      }
    }
    */

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_setXAResource(XAResource xaResource)");
    out.println("{");
    out.println("  _xaResource = xaResource;");
    out.println("}");


    out.println();
    out.println("public void beforeDelivery(java.lang.reflect.Method method)");
    out.println("{");
    out.println("  if (_xaMethods.contains(method)) {");
    out.println("    _isXa = (_xa.beginRequired() == null);");
    out.println("  }");
    out.println("}");

    out.println("public void afterDelivery()");
    out.println("{");
    out.println("  if (_isXa) {");
    out.println("    _isXa = false;");
    out.println("    _xa.commit();");
    out.println("  }");
    out.println("}");

    out.println();
    out.println("public void release()");
    out.println("{");
    out.pushDepth();

    if (getImplMethod("ejbRemove", new Class[0]) != null) {
      out.println("ejbRemove();");
    }
    
    out.popDepth();
    out.println("}");

    /*
    for (View view : getViews()) {
      view.generateContextPrologue(out);
    }
    */

    generateView(out);

    generateDependency(out);
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the view code.
   */
  private void generateView(JavaWriter out)
    throws IOException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();

    map.put("caucho.ejb.xa", "done");

    out.println();
    out.println("private static final com.caucho.ejb.util.XAManager _xa");
    out.println("  = new com.caucho.ejb.util.XAManager();");

    /* ejb/0fbm
    for (BusinessMethodGenerator bizMethod : _businessMethods) {
      bizMethod.generatePrologueTop(out, map);
    }
    */
    
    for (AspectGenerator<X> bizMethod : _businessMethods) {
      bizMethod.generate(out, map);
    }
  }
  
  private AnnotatedMethod<? super X> getImplMethod(String name, Class<?> []param)
  {
    return AnnotatedTypeUtil.findMethod(getBeanType(), name, param);
  }

  /* (non-Javadoc)
   * @see com.caucho.config.gen.BeanGenerator#getAspectBeanFactory()
   */
  @Override
  protected AspectBeanFactory<X> getAspectBeanFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
