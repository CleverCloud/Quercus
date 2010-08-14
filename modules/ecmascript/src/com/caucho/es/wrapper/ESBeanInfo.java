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

import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
    
/**
 * Full analyzed information on the class as a JavaScript object.
 */
public class ESBeanInfo {
  static String BAD = "bad";

  Class cl;
  HashMap<String,ArrayList<Method>> _methodMap;
  HashMap<String,ArrayList<Method>> _staticMethodMap;
  HashMap propMap;
  ArrayList nonPkgClasses = new ArrayList();
  PropertyDescriptor []propertyDescriptors;
  ESMethodDescriptor iterator;

  ESBeanInfo(Class cl)
  {
    this.cl = cl;
    _methodMap = new HashMap<String,ArrayList<Method>>();
    _staticMethodMap = new HashMap<String,ArrayList<Method>>();
    propMap = new HashMap();
  }
  
  void addNonPkgClass(String name)
  {
    if (name.indexOf('$') >= 0)
      return;
    
    if (! nonPkgClasses.contains(name))
      nonPkgClasses.add(name);
  }

  ArrayList getNonPkgClasses()
  {
    return nonPkgClasses;
  }

  /**
   * Return the property descriptors for the bean.
   */
  public PropertyDescriptor []getPropertyDescriptors()
  {
    if (propertyDescriptors == null)
      propertyDescriptors = propMapToArray(propMap);

    return propertyDescriptors;
  }
  
  private static PropertyDescriptor []propMapToArray(HashMap props)
  {
    int count = 0;
    Iterator i = props.keySet().iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      Object value = props.get(key);

      if (value != BAD)
        count++;
    }

    PropertyDescriptor []descriptors = new PropertyDescriptor[count];

    count = 0;
    i = props.keySet().iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      Object value = props.get(key);

      if (value != BAD) {
        descriptors[count] = (PropertyDescriptor) value;
        count++;
      }
    }

    return descriptors;
  }

  void addProp(String name, Field field, 
               ESMethodDescriptor getter,
               ESMethodDescriptor setter,
               boolean overwrite)
    throws IntrospectionException
  {
    Object value = propMap.get(name);

    if (value instanceof ESPropertyDescriptor &&
        ! (value instanceof ESIndexedPropertyDescriptor) &&
        ! (value instanceof NamedPropertyDescriptor)) {
      ESPropertyDescriptor prop = (ESPropertyDescriptor) value;
      
      
      if (field != null)
        prop.field = field;
      if (getter != null)
        prop.getter = getter;
      if (setter != null)
        prop.setter = setter;

      propMap.put(name, prop);
    } else if (value == null || overwrite) {
      propMap.put(name, new ESPropertyDescriptor(name, field, getter, setter));
    } else {
      propMap.put(name, BAD);
    }
  }

  void addProp(String name, Field field, ESMethodDescriptor getter, ESMethodDescriptor setter)
    throws IntrospectionException
  {
    addProp(name, field, getter, setter, false);
  }

  void addIndexedProp(String name, ESMethodDescriptor getter, ESMethodDescriptor setter, ESMethodDescriptor size,
                      boolean overwrite)
    throws IntrospectionException
  {
    Object value = propMap.get(name);

    if (value instanceof ESIndexedPropertyDescriptor) {
      ESIndexedPropertyDescriptor prop = (ESIndexedPropertyDescriptor) value;
      
      if (getter != null)
        prop.getter = getter;
      if (setter != null)
        prop.setter = setter;
      if (size != null)
        prop.size = size;

      propMap.put(name, prop);
    } else if (value == null || overwrite) {
      propMap.put(name, new ESIndexedPropertyDescriptor(name, getter, setter,
                                                        size));
    } else
      propMap.put(name, BAD);
  }

  void addIndexedProp(String name, ESMethodDescriptor getter, ESMethodDescriptor setter, ESMethodDescriptor size)
    throws IntrospectionException
  {
    addIndexedProp(name, getter, setter, size, false);
  }

  void addNamedProp(String name, 
                    ESMethodDescriptor getter,
                    ESMethodDescriptor setter,
                    ESMethodDescriptor remover,
                    ESMethodDescriptor iterator,
                    boolean overwrite)
    throws IntrospectionException
  {
    Object value = propMap.get(name);

    if (value instanceof NamedPropertyDescriptor) {
      NamedPropertyDescriptor prop = (NamedPropertyDescriptor) value;
      
      if (getter != null)
        prop.namedGetter = getter;
      if (setter != null)
        prop.namedSetter = setter;
      if (remover != null)
        prop.namedRemover = remover;
      if (iterator != null)
        prop.namedIterator = iterator;
    } else if (value == null || overwrite) {
      try {
        propMap.put(name, new NamedPropertyDescriptor(name, null, null,
                                                      getter, setter,
                                                      remover, iterator));
      } catch (Exception e) {
        propMap.put(name, BAD);
      }
    } else
      propMap.put(name, BAD);
  }

  void addNamedProp(String name, ESMethodDescriptor getter, 
                    ESMethodDescriptor setter,
                    ESMethodDescriptor remover,
                    ESMethodDescriptor iterator)
    throws IntrospectionException
  {
    addNamedProp(name, getter, setter, remover, iterator, false);
  }

  /**
   * Returns the methods matching the given name.
   */
  public ArrayList<Method> getMethods(String name)
  {
    return _methodMap.get(name);
  }

  /**
   * Returns the static methods matching the given name.
   */
  public ArrayList<Method> getStaticMethods(String name)
  {
    return _staticMethodMap.get(name);
  }

  public ArrayList getConstructors()
  {
    ArrayList overload = new ArrayList();
    if (! Modifier.isPublic(cl.getModifiers()))
      return overload;

    // non-static inner classes have no constructor
    if (cl.getDeclaringClass() != null &&
        ! Modifier.isStatic(cl.getModifiers()))
      return overload;
    
    Constructor []constructors = cl.getConstructors();
    for (int i = 0; i < constructors.length; i++) {
      if (! Modifier.isPublic(constructors[i].getModifiers()))
        continue;

      if (Modifier.isPublic(constructors[i].getModifiers()))
        addConstructor(overload, constructors[i]);
    }

    return overload;
  }

  private void addConstructor(ArrayList overload,
                              Constructor constructor)
  {
    int modifiers = constructor.getModifiers();

    if (! Modifier.isPublic(modifiers))
      return;

    Class []params = constructor.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      if (! Modifier.isPublic(params[i].getModifiers()))
        return;
      if (! params[i].isPrimitive() && ! params[i].isArray() &&
          params[i].getName().indexOf('.') < 0)
        addNonPkgClass(params[i].getName());
    }
    int length = params.length;

    Object oldConstructor;
    if (length < overload.size() &&
        (oldConstructor = overload.get(length)) != null) {
      overload.set(length, BAD);
    }
    else {
      while (overload.size() <= length)
        overload.add(null);
      overload.set(length, constructor);
    }
  }

  /**
   * Create a new method descriptor.
   */
  ESMethodDescriptor createMethodDescriptor(Method method, boolean overwrite)
    throws IntrospectionException
  {
    boolean staticVirtual = isStaticVirtual(cl, method);
    
    return new ESMethodDescriptor(method, overwrite, staticVirtual);
  }

  /**
   * Returns true if this is a static virtual method.
   * Static virtual methods are used by FooEcmaWrap classes add new methods
   * to a class.
   *
   * <p>The method is always static and the first argument has the class
   * of the overwriting class.
   */
  private static boolean isStaticVirtual(Class cl, Method method)
  {
    int modifiers = method.getModifiers();

    if (! Modifier.isStatic(modifiers))
      return false;

    if (method.getName().equals("<init>"))
      return false;

    if (! method.getDeclaringClass().getName().endsWith("EcmaWrap"))
      return false;

    Class []params = method.getParameterTypes();

    boolean result = params.length > 0 && params[0].equals(cl);

    return result;
  }

  /**
   * Adds a new method to the bean info, changing the overwrite property.
   *
   * @param oldMd the old method descriptor.
   * @param boolean true if overwritable.
   */
  void addMethod(MethodDescriptor oldMd, boolean overwrite)
    throws IntrospectionException
  {
    ESMethodDescriptor md = createMethodDescriptor(oldMd.getMethod(), 
                                                   overwrite);
    md.setName(oldMd.getName());
    addMethod(_methodMap, md, false);
    addMethod(_staticMethodMap, md, true);
  }

  /**
   * Adds a new method to the bean info.
   *
   * @param me the method descriptor.
   */
  void addMethod(ESMethodDescriptor md)
    throws IntrospectionException
  {
    addMethod(_methodMap, md, false);
    addMethod(_staticMethodMap, md, true);
  }

  void addMethods(ESBeanInfo info)
    throws IntrospectionException
  {
    if (info.iterator != null) {
      iterator = info.iterator;
    }
    addMap(_methodMap, info._methodMap, false);
    addMap(_staticMethodMap, info._staticMethodMap, true);
  }

  /**
   * Merges two method maps together to produce a new map.
   */
  private void addMap(HashMap newMap, HashMap oldMap, boolean isStatic)
  {
    Iterator i = oldMap.entrySet().iterator();
    
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      ArrayList overload = (ArrayList) entry.getValue();

      for (int j = 0; j < overload.size(); j++) {
        ESMethodDescriptor []mds = (ESMethodDescriptor []) overload.get(j);

        if (mds != null) {
          for (int k = 0; k < mds.length; k++)
            addMethod(newMap, mds[k], isStatic);
        }
      }
    }
  }

  /**
   * Adds a method to the method map.
   *
   * @param map the method map to modify.
   * @param md the method descriptor of the new method.
   * @param isStatic true if this is a static method.
   */
  private void addMethod(HashMap map, 
                         ESMethodDescriptor md, boolean isStatic)
  {
    Method method = md.getMethod();
    int modifiers = method.getModifiers();
    boolean staticVirtual = md.isStaticVirtual();
    boolean overwrite = md.isOverwrite();

    if (! Modifier.isPublic(modifiers))
      return;
    if (isStatic && ! md.isStatic())
      return;

    Class []params = md.getParameterTypes();
    for (int i = 0; i < params.length; i++) {
      if (! Modifier.isPublic(params[i].getModifiers()))
        return;
      if (! params[i].isPrimitive() && ! params[i].isArray() &&
          params[i].getName().indexOf('.') < 0)
        addNonPkgClass(params[i].getName());
    }
          
    ArrayList overload = (ArrayList) map.get(md.getName());
    if (overload == null) {
      overload = new ArrayList();
      map.put(md.getName(), overload);
    }

    int length = params.length;

    if (length >= overload.size()) {
      while (overload.size() <= length)
        overload.add(null);
    }
    
    ESMethodDescriptor []oldMethods;
    oldMethods = (ESMethodDescriptor []) overload.get(length);

    if (oldMethods == null) {
      overload.set(length, new ESMethodDescriptor[] { md });
      return;
    }

    for (int i = 0; i < oldMethods.length; i++) {
      ESMethodDescriptor testMd = oldMethods[i];
                         
      if (testMd.overwrites(md))
        return;
      else if (md.overwrites(testMd)) {
        oldMethods[i] = md;
        return;
      }

      Class []oldParams = testMd.getParameterTypes();

      int j = 0;
      for (; j < length; j++) {
        if (! params[j].equals(oldParams[j]))
          break;
      }

      // duplicates another method
      if (j == length && md.isOverwrite()) {
        oldMethods[i] = md;
        return;
      } else if (j == length) {
        return;
      }
    }

    ESMethodDescriptor []newMethods;
    newMethods = new ESMethodDescriptor[oldMethods.length + 1];
    System.arraycopy(oldMethods, 0, newMethods, 0, oldMethods.length);
    newMethods[oldMethods.length] = md;
    overload.set(length, newMethods);
  }

  private void addPropMap(HashMap oldMap)
    throws IntrospectionException
  {
    Iterator i = oldMap.entrySet().iterator();
    
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry) i.next();
      Object value = entry.getValue();

      if (value == BAD) {
        // XXX: else add bad?
        continue;
      }

      if (value instanceof NamedPropertyDescriptor) {
        NamedPropertyDescriptor pd = (NamedPropertyDescriptor) value;

        addNamedProp(pd.getName(), pd.namedGetter, pd.namedSetter,
                     pd.namedRemover, pd.namedIterator);
      } else if (value instanceof ESIndexedPropertyDescriptor) {
        ESIndexedPropertyDescriptor pd = (ESIndexedPropertyDescriptor) value;

        addIndexedProp(pd.getName(), pd.getESReadMethod(),
                       pd.getESWriteMethod(), pd.getESSizeMethod());
      } else if (value instanceof ESPropertyDescriptor) {
        ESPropertyDescriptor pd = (ESPropertyDescriptor) value;

        addProp(pd.getName(), pd.field, pd.getter, pd.setter);
      }
      // XXX: else add bad?
    }
  }

  void addField(Field field) 
    throws IntrospectionException
  {
    int modifiers = field.getModifiers();
    if (! Modifier.isPublic(modifiers))
      return;

    addProp(field.getName(), field, null, null);
  }

  void addProps(ESBeanInfo info)
    throws IntrospectionException
  {
    addPropMap(info.propMap);
  }
}
