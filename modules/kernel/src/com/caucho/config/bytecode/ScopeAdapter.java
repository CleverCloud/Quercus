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

package com.caucho.config.bytecode;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Remove;
import javax.enterprise.inject.spi.Bean;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaClassLoader;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.JavaMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.gen.CandiUtil;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.ProxyClassLoader;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Scope adapting
 */
@Module
public class ScopeAdapter {
  private static final L10N L = new L10N(ScopeAdapter.class);
  private static final Logger log 
    = Logger.getLogger(ScopeAdapter.class.getName());

  private final Class<?> _beanClass;
  private final Class<?> _cl;
  private final Class<?> []_types;

  private Class<?> _proxyClass;
  private Constructor<?> _proxyCtor;
  
  private ScopeAdapter(Class<?> beanClass, Class<?> cl, Class<?> []types)
  {
    _types = types;
    
    _beanClass = beanClass;
    _cl = cl;

    generateProxy(_cl, types);
  }
  
  public static ScopeAdapter create(Bean<?> bean)
  {
    Set<Type> types = bean.getTypes();
    
    ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
    
    Class<?> beanClass = bean.getBeanClass();
    Class<?> cl = null;
    
    for (Type type : types) {
      Class<?> rawClass = CandiUtil.getRawClass(type);
      
      if (rawClass.equals(Object.class))
        continue;
      
      classList.add(rawClass);
      
      if (cl == null
          || cl.isAssignableFrom(rawClass)
          || cl.isInterface() && ! rawClass.isInterface()
          || (cl.getName().startsWith("java") 
              && ! rawClass.getName().startsWith("java"))) {
        cl = rawClass;
      }
    }
    
    Class<?> []classes = new Class<?>[classList.size()];
    
    classList.toArray(classes);
    
    return new ScopeAdapter(beanClass, cl, classes);
  }

  public static ScopeAdapter create(Class<?> cl)
  {
    ScopeAdapter adapter = new ScopeAdapter(cl, cl, new Class<?>[] { cl });

    return adapter;
  }

  public static void validateType(Type type)
  {
    BaseType baseType = InjectManager.getCurrent().createTargetBaseType(type);
    Class<?> rawType = baseType.getRawClass();
    
    if (rawType.isPrimitive())
      throw new ConfigException(L.l("'{0}' is an invalid @NormalScope bean because it's a Java primitive.",
                                    baseType));
    
    
    if (rawType.isArray())
      throw new ConfigException(L.l("'{0}' is an invalid @NormalScope bean because it's a Java array.",
                                    baseType));
    
  }
  
  public <X> X wrap(InjectManager.ReferenceFactory<X> factory)
  {
    try {
      Object v = _proxyCtor.newInstance(factory);
      return (X) v;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void generateProxy(Class<?> cl, Class<?> []types)
  {
    try {
      Constructor<?> zeroCtor = null;

      for (Constructor<?> ctorItem : cl.getDeclaredConstructors()) {
        if (ctorItem.getParameterTypes().length == 0) {
          zeroCtor = ctorItem;
          break;
        }
      }

      if (zeroCtor == null && ! cl.isInterface()) {
        throw new ConfigException(L.l("'{0}' does not have a zero-arg public or protected constructor.  Scope adapter components need a zero-arg constructor, e.g. @RequestScoped stored in @ApplicationScoped.",
                                      cl.getName()));
      }

      if (zeroCtor != null)
        zeroCtor.setAccessible(true);
      
      String typeClassName = cl.getName().replace('.', '/');
      
      String thisClassName = typeClassName + "__ResinScopeProxy";
      
      if (thisClassName.startsWith("java"))
        thisClassName = "cdi/" + thisClassName;
      
      String cleanName = thisClassName.replace('/', '.');
      
      boolean isPackagePrivate = false;
      
      DynamicClassLoader loader;
      
      if (! Modifier.isPublic(cl.getModifiers()) 
          && ! Modifier.isProtected(cl.getModifiers())) {
        isPackagePrivate = true;
      }

      if (isPackagePrivate)
        loader = (DynamicClassLoader) cl.getClassLoader();
      else
        loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
      
      try {
        _proxyClass = Class.forName(cleanName, false, loader);
      } catch (ClassNotFoundException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
      
      if (_proxyClass == null) {
        JavaClassLoader jLoader = new JavaClassLoader(cl.getClassLoader());

        JavaClass jClass = new JavaClass(jLoader);
        jClass.setAccessFlags(Modifier.PUBLIC);

        jClass.setWrite(true);

        jClass.setMajor(49);
        jClass.setMinor(0);

        String superClassName;

        if (! cl.isInterface())
          superClassName = typeClassName;
        else
          superClassName = "java/lang/Object";

        jClass.setSuperClass(superClassName);
        jClass.setThisClass(thisClassName);

        for (Class<?> iface : types) {
          if (iface.isInterface())
            jClass.addInterface(iface.getName().replace('.', '/'));
        }
        
        jClass.addInterface(ScopeProxy.class.getName().replace('.', '/'));

        JavaField factoryField =
          jClass.createField("_factory",
                             "Lcom/caucho/config/inject/InjectManager$ReferenceFactory;");
        factoryField.setAccessFlags(Modifier.PRIVATE);

        JavaMethod ctor =
          jClass.createMethod("<init>",
                              "(Lcom/caucho/config/inject/InjectManager$ReferenceFactory;)V");
        ctor.setAccessFlags(Modifier.PUBLIC);

        CodeWriterAttribute code = ctor.createCodeWriter();
        code.setMaxLocals(3);
        code.setMaxStack(4);

        code.pushObjectVar(0);
        code.pushObjectVar(1);
        code.putField(thisClassName, factoryField.getName(),
                      factoryField.getDescriptor());
        
        code.pushObjectVar(0);
        code.invokespecial(superClassName, "<init>", "()V", 1, 0);
        code.addReturn();
        code.close();

        createGetDelegateMethod(jClass);
        createSerialize(jClass);

        for (Method method : getMethods(_types)) {
          if (Modifier.isStatic(method.getModifiers()))
            continue;
          if (Modifier.isFinal(method.getModifiers()))
            continue;

          if (isRemoveMethod(_beanClass, method))
            createRemoveProxyMethod(jClass, method, method.getDeclaringClass().isInterface());
          else
            createProxyMethod(jClass, method, method.getDeclaringClass().isInterface());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WriteStream out = Vfs.openWrite(bos);

        jClass.write(out);

        out.close();

        byte[] buffer = bos.toByteArray();

        /*
         * try { out = Vfs.lookup("file:/tmp/caucho/qa/temp.class").openWrite();
         * out.write(buffer, 0, buffer.length); out.close(); } catch
         * (IOException e) { }
         */

        if (isPackagePrivate) {
          // ioc/0517
          _proxyClass = loader.loadClass(cleanName, buffer);
        }
        else {
          ProxyClassLoader proxyLoader = new ProxyClassLoader(loader);
          
          _proxyClass = proxyLoader.loadClass(cleanName, buffer);
        }
      }
      
      _proxyCtor = _proxyClass.getConstructors()[0];
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private ArrayList<Method> getMethods(Class<?> []types)
  {
    ArrayList<Method> methodList = new ArrayList<Method>();
    
    for (Class<?> type : types) {
      if (Object.class.equals(type))
        continue;
      
      for (Method method : type.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()))
          continue;
        
        if (Modifier.isPrivate(method.getModifiers()))
          continue;
        
        Method oldMethod = AnnotatedTypeUtil.findMethod(methodList, method);
        
        if (oldMethod == null) {
          methodList.add(method);
          continue;
        }
        
        if (method.getDeclaringClass().isAssignableFrom(oldMethod.getDeclaringClass())) {
          continue;
        }
        else {
          methodList.remove(oldMethod);
          methodList.add(method);
          continue;
        }
      }
    }
    
    return methodList;
  }

  private boolean isRemoveMethod(Class<?> beanClass, Method method)
  {
    if (method.isAnnotationPresent(Remove.class)) {
      return true;
    }
    
    try {
      Method beanMethod = beanClass.getMethod(method.getName(), method.getParameterTypes());
      
      return beanMethod.isAnnotationPresent(Remove.class);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    }
  }

  private void createProxyMethod(JavaClass jClass,
                                 Method method,
                                 boolean isInterface)
  {
    if (method.getName().equals("writeReplace") && method.getParameterTypes().length == 0)
      return;
    
    String descriptor = createDescriptor(method);

    JavaMethod jMethod = jClass.createMethod(method.getName(),
                                             descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class<?> []parameterTypes = method.getParameterTypes();

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1 + 2 * parameterTypes.length);
    code.setMaxStack(3 + 2 * parameterTypes.length);

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_factory",
                  "Lcom/caucho/config/inject/InjectManager$ReferenceFactory;");
    
    code.invoke("com/caucho/config/inject/InjectManager$ReferenceFactory",
                "create",
                "()Ljava/lang/Object;",
                3, 1);

    code.cast(method.getDeclaringClass().getName().replace('.', '/'));

    int stack = 1;
    int index = 1;
    for (Class<?> type : parameterTypes) {
      if (boolean.class.equals(type)
          || byte.class.equals(type)
          || short.class.equals(type)
          || int.class.equals(type)) {
        code.pushIntVar(index);
        index += 1;
        stack += 1;
      }
      else if (long.class.equals(type)) {
        code.pushLongVar(index);
        index += 2;
        stack += 2;
      }
      else if (float.class.equals(type)) {
        code.pushFloatVar(index);
        index += 1;
        stack += 1;
      }
      else if (double.class.equals(type)) {
        code.pushDoubleVar(index);
        index += 2;
        stack += 2;
      }
      else {
        code.pushObjectVar(index);
        index += 1;
        stack += 1;
      }
    }

    if (isInterface) {
      code.invokeInterface(method.getDeclaringClass().getName().replace('.', '/'),
                           method.getName(),
                           createDescriptor(method),
                           stack, 1);
    }
    else {
      code.invoke(method.getDeclaringClass().getName().replace('.', '/'),
                  method.getName(),
                  createDescriptor(method),
                  stack, 1);
    }

    Class<?> retType = method.getReturnType();

    if (boolean.class.equals(retType)
        || byte.class.equals(retType)
        || short.class.equals(retType)
        || int.class.equals(retType)) {
      code.addIntReturn();
    }
    else if (long.class.equals(retType)) {
      code.addLongReturn();
    }
    else if (float.class.equals(retType)) {
      code.addFloatReturn();
    }
    else if (double.class.equals(retType)) {
      code.addDoubleReturn();
    }
    else if (void.class.equals(retType)) {
      code.addReturn();
    }
    else {
      code.addObjectReturn();
    }

    code.close();
  }

  private void createRemoveProxyMethod(JavaClass jClass,
                                       Method method,
                                       boolean isInterface)
  {
    String descriptor = createDescriptor(method);

    JavaMethod jMethod = jClass.createMethod(method.getName(),
                                             descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class<?> []parameterTypes = method.getParameterTypes();

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1 + 2 * parameterTypes.length);
    code.setMaxStack(3 + 2 * parameterTypes.length);

    code.newInstance("java/lang/UnsupportedOperationException");
    code.dup();
    code.invokespecial("java/lang/UnsupportedOperationException",
                       "<init>",
                       "()V",
                       3, 1);
    code.addThrow();

    code.close();
  }

  private void createGetDelegateMethod(JavaClass jClass)
  {
    String descriptor = "()Ljava/lang/Object;";

    JavaMethod jMethod = jClass.createMethod("__caucho_getDelegate",
                                             descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1);
    code.setMaxStack(3);

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_factory",
                  "Lcom/caucho/config/inject/InjectManager$ReferenceFactory;");

    code.invoke("com/caucho/config/inject/InjectManager$ReferenceFactory",
                "create",
                "()Ljava/lang/Object;",
                3, 1);

    code.addObjectReturn();

    code.close();
  }

  private void createSerialize(JavaClass jClass)
  {
    String descriptor = "()Ljava/lang/Object;";

    JavaMethod jMethod = jClass.createMethod("writeReplace",
                                             descriptor);
    
    jMethod.setAccessFlags(Modifier.PRIVATE);

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1);
    code.setMaxStack(3);

    code.newInstance("com/caucho/config/bytecode/ScopeProxyHandle");
    code.dup();
    
    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_factory",
                  "Lcom/caucho/config/inject/InjectManager$ReferenceFactory;");
    
    code.invokespecial("com/caucho/config/bytecode/ScopeProxyHandle",
                       "<init>",
                       "(Lcom/caucho/config/inject/InjectManager$ReferenceFactory;)V",
                       3, 1);

    code.addObjectReturn();

    code.close();
  }

  private String createDescriptor(Method method)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(");

    for (Class<?> param : method.getParameterTypes()) {
      sb.append(createDescriptor(param));
    }

    sb.append(")");
    sb.append(createDescriptor(method.getReturnType()));

    return sb.toString();
  }

  private String createDescriptor(Class<?> cl)
  {
    if (cl.isArray())
      return "[" + createDescriptor(cl.getComponentType());

    String primValue = _prim.get(cl);

    if (primValue != null)
      return primValue;

    return "L" + cl.getName().replace('.', '/') + ";";
  }

  private static HashMap<Class<?>,String> _prim
    = new HashMap<Class<?>,String>();

  static {
    _prim.put(boolean.class, "Z");
    _prim.put(byte.class, "B");
    _prim.put(char.class, "C");
    _prim.put(short.class, "S");
    _prim.put(int.class, "I");
    _prim.put(long.class, "J");
    _prim.put(float.class, "F");
    _prim.put(double.class, "D");
    _prim.put(void.class, "V");
  }

}
