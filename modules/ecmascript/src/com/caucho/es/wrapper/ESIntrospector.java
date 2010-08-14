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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es.wrapper;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.WeakLruCache;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
    
/**
 * Analyzes the class from a JavaScript perspective.
 *
 * <p>Each class Foo searches for its "FooEcmaWrap" to see if there are
 * any method changes.
 */
public class ESIntrospector {
  /**
   * Cache of analyzed classes to avoid duplication.
   */
  static WeakLruCache<Class,SoftReference<ESBeanInfo>> _beanMap
    = new WeakLruCache<Class,SoftReference<ESBeanInfo>>(256);
  
  static Integer NULL = new Integer(0);
  final static int METHOD = 1;
  final static int PROPERTY = 2;
  final static int MASK = 3;

  static String []path = new String[] {"", "com.caucho.eswrap"};

  /**
   * Analyzes the class, returning the calculated ESBeanInfo.
   *
   * @param cl the class to be analyzed.
   * @return the analyzed bean info.
   */
  public static ESBeanInfo getBeanInfo(Class cl)
    throws IntrospectionException
  {
    ESBeanInfo info;

    SoftReference<ESBeanInfo> infoRef = _beanMap.get(cl);
      
    info = infoRef != null ? infoRef.get() : null;

    if (info != null)
      return info;

    info = new ESBeanInfo(cl);

    getMethods(info, cl);

    _beanMap.put(cl, new SoftReference<ESBeanInfo>(info));
    
    return info;
  }

  /**
   * Analyzes a Java method, converting it to any equivalent JavaScript
   * properties.
   *
   * <pre>
   * keys()         -> for (item in obj) {
   * getFoo()       -> obj.foo
   * getFoo(int)    -> obj.foo[i]
   * getFoo(String) -> obj.foo["name"]
   * remove(String) -> delete obj.foo
   * </pre>
   *
   * @param info the bean info to be filled
   * @param cl the bean's class
   * @param md the method descriptor
   * @param overwrite if true, this overwrites any previous definition
   */
  private static void
    analyzeProperty(ESBeanInfo info, Class cl, ESMethodDescriptor md,
                    boolean overwrite)
    throws IntrospectionException
  {
    Method method = md.getMethod();
    int modifiers = method.getModifiers();

    if (! Modifier.isPublic(modifiers))
      return;

    String name = method.getName();
    String propName;
    Class returnType = method.getReturnType();
    String returnName = returnType.getName();
    Class []params = md.getParameterTypes();

    if (name.equals("keys") && params.length == 0 &&
        (returnName.equals("java.util.Iterator") ||
         (returnName.equals("java.util.Enumeration")))) {
      info.iterator = md;
    }
    else if (name.equals("iterator") && params.length == 0 &&
             (returnName.equals("java.util.Iterator") ||
              (returnName.equals("java.util.Enumeration")))) {
      // keys has priority over iterator
      if (info.iterator == null)
        info.iterator = md;
    }
    else if (name.startsWith("get") && ! name.equals("get")) {
      propName = Introspector.decapitalize(name.substring(3));

      // kill match?
      if (returnName.equals("void") && params.length < 2) {
        // XXX: props.put(propName, BAD);
        return;
      }

      // name keys
      if (params.length == 0 &&
          (propName.endsWith("Keys") && ! propName.equals("Keys") ||
           propName.endsWith("Names") && ! propName.equals("Names")) &&
          (returnName.equals("java.util.Iterator") ||
           (returnName.equals("java.util.Enumeration")))) {
        if (propName.endsWith("Keys"))
          info.addNamedProp(propName.substring(0, propName.length() - 4),
                            null, null, null, md);
        else
          info.addNamedProp(propName.substring(0, propName.length() - 5),
                            null, null, null, md);
      }
      // index length
      else if (params.length == 0 &&
               (propName.endsWith("Size") && ! propName.equals("Size") ||
                propName.endsWith("Length") && ! propName.equals("Length")) &&
               (returnName.equals("int"))) {
        info.addProp(propName, null, md, null);
        if (propName.endsWith("Size"))
          info.addIndexedProp(propName.substring(0, propName.length() - 4),
                              null, null, md);
        else
          info.addIndexedProp(propName.substring(0, propName.length() - 6),
                              null, null, md);
      }
      else if (params.length == 0)
        info.addProp(propName, null, md, null);
      else if (params.length == 1 && 
               params[0].getName().equals("java.lang.String")) {
        info.addNamedProp(propName, md, null, null, null);
      } else if (params.length == 1 && 
               params[0].getName().equals("int")) {
        info.addIndexedProp(propName, md, null, null);
      } else if (params.length == 1) {
        // xxx: add bad?
      }
    } else if (name.startsWith("set") && ! name.equals("set")) {
      propName = Introspector.decapitalize(name.substring(3));

      if (params.length == 0)
        return;

          // kill match?
      if (! returnType.getName().equals("void") && params.length < 3) {
        // XXX: add bad? props.put(propName, NULL);
        return;
      }

      if (params.length == 1)
        info.addProp(propName, null, null, md);
      else if (params.length == 2 &&
               params[0].getName().equals("java.lang.String")) {
        info.addNamedProp(propName, null, md, null, null);
      } else if (params.length == 2 && 
                 params[0].getName().equals("int")) {
        info.addIndexedProp(propName, null, md, null);
      } else if (params.length == 2) {
        // XXX: props.put(propName, NULL);
      }
    } else if (name.startsWith("remove") && ! name.equals("remove") ||
               name.startsWith("delete") && ! name.equals("remove")) {
      propName = Introspector.decapitalize(name.substring(6));

      if (params.length == 0)
        return;

          // kill match?
      if (! returnType.getName().equals("void") && params.length < 2) {
        // XXX: add bad? props.put(propName, NULL);
        return;
      }

      //if (params.length == 0)
      //        info.addProp(propName, null, null, md);
      if (params.length == 1 &&
          params[0].getName().equals("java.lang.String")) {
        info.addNamedProp(propName, null, null, md, null);
      } 
      /*
      else if (params.length == 2 && 
                 params[0].getName().equals("int")) {
        info.addIndexedProp(propName, null, md, null);
      } else if (params.length == 2) {
        // XXX: props.put(propName, NULL);
      }
      */
    }
  }

  /**
   * Add methods from a FooEcmaWrap class.
   */
  static void addEcmaMethods(ESBeanInfo info, Class cl, Class wrapCl, int mask)
    throws IntrospectionException
  {
    Method []methods = wrapCl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      int modifiers = methods[i].getModifiers();

      if (! Modifier.isStatic(modifiers))
        continue;

      ESMethodDescriptor md = info.createMethodDescriptor(methods[i], true);

      if ((mask & METHOD) != 0)
        info.addMethod(md);
      if ((mask & PROPERTY) != 0)
        analyzeProperty(info, cl, md, true);
    }  
  }

  /**
   * Search for FooEcmaWrap classes.
   *
   * @param info the bean info to be filled
   * @param cl the bean being analyzed
   * @param mask the replacement mask.
   */
  static void addEcmaWrap(ESBeanInfo info, Class cl, int mask)
    throws IntrospectionException
  {
    String name = cl.getName();
    int lastDot = name.lastIndexOf('.');
    String prefix = lastDot == -1 ? "" : name.substring(0, lastDot);
    String tail = lastDot == -1 ? name : name.substring(lastDot + 1);

    ClassLoader loader = cl.getClassLoader();

    for (int i = 0; i < path.length; i++) {
      String testName;

      if (path[i].equals(""))
        testName = name + "EcmaWrap";
      else
        testName = path[i] + "." + name + "EcmaWrap";

      try {
        Class wrapCl = CauchoSystem.loadClass(testName, false, loader);

        if (wrapCl != null) {
          addEcmaMethods(info, cl, wrapCl, mask);
          return;
        }
      } catch (ClassNotFoundException e) {
      }

      if (path[i].equals(""))
        testName = tail + "EcmaWrap";
      else
        testName = path[i] + "." + tail + "EcmaWrap";

      try {
        Class wrapCl = CauchoSystem.loadClass(testName, false, loader);

        if (wrapCl != null) {
          addEcmaMethods(info, cl, wrapCl, mask);
          return;
        }
      } catch (ClassNotFoundException e) {
      }
    }
  }

  /**
   * Fills information for a FooBeanInfo class.
   *
   * @param info the result information object
   * @param cl the bean class
   *
   * @return a mask
   */
  static int getBeanInfo(ESBeanInfo info, Class cl)
  {
    try {
      String name = cl.getName() + "BeanInfo";
      
      Class beanClass = CauchoSystem.loadClass(name, false, cl.getClassLoader());
      if (beanClass == null)
        return MASK;
      BeanInfo beanInfo = (BeanInfo) beanClass.newInstance();

      MethodDescriptor []mds = beanInfo.getMethodDescriptors();
      if (mds == null)
        return MASK;

      for (int i = 0; i < mds.length; i++) {
        Method method = mds[i].getMethod();
        int modifiers = method.getModifiers();

        if (! Modifier.isStatic(modifiers) &&
            ! method.getDeclaringClass().isAssignableFrom(cl))
          continue;

        info.addMethod(mds[i], true);
      }

      return MASK & ~METHOD;
    } catch (Exception e) {
      return MASK;
    }
  }

  static void getPropBeanInfo(ESBeanInfo info, Class cl)
  {
    try {
      String name = cl.getName() + "BeanInfo";
      Class beanClass = CauchoSystem.loadClass(name, false, cl.getClassLoader());
      if (beanClass == null)
        return;
      BeanInfo beanInfo = (BeanInfo) beanClass.newInstance();

      PropertyDescriptor []props = beanInfo.getPropertyDescriptors();

      for (int i = 0; props != null && i < props.length; i++) {
        ESMethodDescriptor read;
        ESMethodDescriptor write;
        
        read = new ESMethodDescriptor(props[i].getReadMethod(), false, false);
        write = new ESMethodDescriptor(props[i].getWriteMethod(),
                                       false, false);

        info.addProp(props[i].getName(), null, read, write, true);
      }
    } catch (Exception e) {
    }
  }

  private static void getMethods(ESBeanInfo info, Class cl)
    throws IntrospectionException
  {
    if (! cl.isPrimitive() && ! cl.isArray() &&
        cl.getName().indexOf('.') < 0)
      info.addNonPkgClass(cl.getName());

    getPropBeanInfo(info, cl);

    int mask = getBeanInfo(info, cl);

    if (mask == 0)
      return;

    Class []interfaces = cl.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      ESBeanInfo subInfo = getBeanInfo(interfaces[i]);

      if ((mask & METHOD) != 0)
        info.addMethods(subInfo);
      if ((mask & PROPERTY) != 0)
        info.addProps(subInfo);
    }

    Class superClass = cl.getSuperclass();
    if (superClass != null) {
      ESBeanInfo subInfo = getBeanInfo(superClass);

      if ((mask & METHOD) != 0)
        info.addMethods(subInfo);
      if ((mask & PROPERTY) != 0)
        info.addProps(subInfo);
    }

    int modifiers = cl.getModifiers();
    if (Modifier.isPublic(modifiers)) {
      Method []methods = cl.getDeclaredMethods();
      int len = methods == null ? 0 : methods.length;
      for (int i = 0; i < len; i++) {
        ESMethodDescriptor md = info.createMethodDescriptor(methods[i], false);

        if ((mask & METHOD) != 0)
          info.addMethod(md);
        if ((mask & PROPERTY) != 0)
          analyzeProperty(info, cl, md, false);
      }

      Field []fields = cl.getDeclaredFields();
      for (int i = 0; fields != null && i < fields.length; i++) {
        info.addField(fields[i]);
      }
    }

    addEcmaWrap(info, cl, mask);
  }
}
