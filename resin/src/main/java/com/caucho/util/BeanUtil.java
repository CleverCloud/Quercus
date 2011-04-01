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

package com.caucho.util;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Bean utilities.
 */
public class BeanUtil {
  static final Logger log = Log.open(BeanUtil.class);
  static L10N L = new L10N(BeanUtil.class);

  /**
   * Returns the bean property type.
   *
   * @param obj the bean object
   * @param name the property name
   */
  public static Class
  getBeanPropertyClass(Object obj, String name)
  {
    Method method = getBeanPropertyMethod(obj, name);

    if (method == null)
      return null;

    Class []paramTypes = method.getParameterTypes();
    if (paramTypes.length == 1)
      return paramTypes[0];
    else
      return null;
  }

  /**
   * Returns the bean property type.
   *
   * @param obj the bean object
   * @param name the property name
   */
  public static Method
  getBeanPropertyMethod(Object obj, String name)
  {
    name = configToBeanName(name);

    Class beanClass = obj.getClass();
    Method method = getSetMethod(beanClass, name);

    if (method == null)
      method = getAddMethod(beanClass, name);

    return method;
  }

  public static void
  validateClass(Class cl, Class parent)
    throws RegistryException
  {
    if (parent.isAssignableFrom(cl)) {
    }
    else if (parent.isInterface())
      throw new RegistryException(L.l("{0} must implement {1}",
                                      cl.getName(), parent.getName()));
    else
      throw new RegistryException(L.l("{0} must extend {1}",
                                      cl.getName(), parent.getName()));

    if (cl.isInterface())
      throw new RegistryException(L.l("{0} must be a concrete class.",
                                      cl.getName()));
    
    if (Modifier.isAbstract(cl.getModifiers()))
      throw new RegistryException(L.l("{0} must not be abstract.",
                                      cl.getName()));
    
    if (! Modifier.isPublic(cl.getModifiers()))
      throw new RegistryException(L.l("{0} must be public.",
                                      cl.getName()));

    Constructor zero = null;
    try {
      zero = cl.getConstructor(new Class[0]);
    } catch (Throwable e) {
    }

    if (zero == null)
      throw new RegistryException(L.l("{0} must have a public zero-arg constructor.",
                                      cl.getName()));
  }

  /**
   * Returns the native path for a configured path name.  The special cases
   * $app-dir and $resin-home specify the root directory.
   *
   * @param pathName the configuration path name.
   * @param varMap the map of path variables.
   * @param pwd the default path.
   *
   * @return a real path corresponding to the path name
   */
  public static Path lookupPath(String pathName, HashMap varMap, Path pwd)
  {
    if (pwd == null)
      pwd = Vfs.lookup();
    
    if (pathName.startsWith("$")) {
      int p = pathName.indexOf('/');
      String prefix;
      String suffix;
      
      if (p > 0) {
        prefix = pathName.substring(1, p);
        suffix = pathName.substring(p + 1);
      }
      else {
        prefix = pathName.substring(1);
        suffix = null;
      }

      Object value = varMap != null ? varMap.get(prefix) : null;
      if (value instanceof Path) {
        pwd = (Path) value;
        pathName = suffix;
      }
    }

    if (pathName == null)
      return pwd;
    else if (pathName.indexOf('$') < 0)
      return pwd.lookup(pathName);
    
    CharBuffer cb = CharBuffer.allocate();
    int head = 0;
    int tail = 0;
    while ((tail = pathName.indexOf('$', head)) >= 0) {
      cb.append(pathName.substring(head, tail));

      if (tail + 1 == pathName.length()) {
        cb.append('$');
        continue;
      }

      int ch = pathName.charAt(tail + 1);
      
      if (ch >= '0' && ch <= '9') {
        for (head = tail + 1; head < pathName.length(); head++) {
          ch = pathName.charAt(head);
        
          if (ch < '0' || ch > '9')
            break;
        }
      }
      else {
        for (head = tail + 1; head < pathName.length(); head++) {
          ch = pathName.charAt(head);
        
          if (ch == '/' || ch == '\\' || ch == '$' || ch == ' ')
            break;
        }
      }

      String key = pathName.substring(tail + 1, head);
      Object value = varMap != null ? varMap.get(key) : null;

      if (value == null)
        value = System.getProperty(key);

      if (value != null)
        cb.append(value);
      else
        cb.append(pathName.substring(tail, head));
    }

    if (head > 0 && head < pathName.length())
      cb.append(pathName.substring(head));
    
    return pwd.lookupNative(cb.close());
  }

  /**
   * Translates a configuration name to a bean name.
   *
   * <pre>
   * foo-bar maps to fooBar
   * </pre>
   */
  private static String configToBeanName(String name)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      
      if (ch == '-')
        cb.append(Character.toUpperCase(name.charAt(++i)));
      else
        cb.append(ch);
    }

    return cb.close();
  }

  /**
   * Returns an add method matching the name.
   */
  private static Method getAddMethod(Class cl, String name)
  {
    name = "add" + name;

    Method []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! Modifier.isPublic(methods[i].getModifiers()))
        continue;

      if (! name.equalsIgnoreCase(methods[i].getName()))
        continue;

      if (methods[i].getParameterTypes().length == 1)
        return methods[i];
    }

    return null;
  }

  /**
   * Returns the method matching the name.
   */
  static private Method getMethod(Method []methods, String name)
  {
    Method method = null;
    for (int i = 0; i < methods.length; i++) {
      method = methods[i];

      if (! Modifier.isPublic(method.getModifiers()))
          continue;
      
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
          continue;
      
      if (method.getName().equals(name))
        return method;
    }

    return null;
  }

  /**
   * Returns the method matching the name.
   */
  static private Method getMethod(Method []methods, String name,
                                  Class []params)
  {
    Method method = null;

    loop:
    for (int i = 0; i < methods.length; i++) {
      method = methods[i];
      
      if (! Modifier.isPublic(method.getModifiers()))
        continue;
      
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;
      
      if (! method.getName().equals(name))
        continue;

      Class []actual = method.getParameterTypes();

      if (actual.length != params.length)
        continue;

      for (int j = 0; j < actual.length; j++) {
        if (! actual[j].isAssignableFrom(params[j]))
          continue loop;
      }
      
      return method;
    }

    return null;
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(BeanInfo info, String propertyName)
  {
    // jsp/184c, jsp/184z, jsp/18o1 bug #2634, #3066

    Method method = getSetMethod(info.getBeanDescriptor().getBeanClass(),
                                 propertyName);
    
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    Method bestMethod = method;

    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getName().equals(propertyName)
          && pds[i].getWriteMethod() != null) {
        Method writeMethod = pds[i].getWriteMethod();

        if (method != null && writeMethod.getName().equals(method.getName()))
          continue;

        if (writeMethod.getParameterTypes()[0].equals(String.class))
          return writeMethod;
        else
          bestMethod = writeMethod;
      }
    }

    return bestMethod;
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(Class cl, String propertyName)
  {
    Method method = getSetMethod(cl, propertyName, false);

    if (method != null)
      return method;

    return getSetMethod(cl, propertyName, true);
  }

  /**
   * Returns a set method matching the property name.
   */
  public static Method getSetMethod(Class cl,
                                    String propertyName,
                                    boolean ignoreCase)
  {
    String setName = "set" + propertyNameToMethodName(propertyName);

    Method bestMethod = null;
    
    for (Class ptrCl = cl; ptrCl != null; ptrCl = ptrCl.getSuperclass()) {
      Method method = getSetMethod(ptrCl.getMethods(),
                                   setName,
                                   ignoreCase);


      if (method != null && method.getParameterTypes()[0].equals(String.class))
        return method;
      else if (method != null)
        bestMethod = method;
    }

    if (bestMethod != null)
      return bestMethod;

    Class []interfaces = cl.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      Method method = getSetMethod(interfaces[i].getMethods(),
                                   setName,
                                   ignoreCase);

      if (method != null && method.getParameterTypes()[0].equals(String.class))
        return method;
      else if (method != null)
        bestMethod = method;
    }

    if (bestMethod != null)
      return bestMethod;

    return null;
  }

  /**
   * Finds the matching set method
   *
   * @param method the methods for the class
   * @param setName the method name
   */
  private static Method getSetMethod(Method []methods,
                                     String setName,
                                     boolean ignoreCase)
  {
    Method bestMethod = null;
    
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      // The method name must match
      if (! ignoreCase && ! method.getName().equals(setName))
        continue;
      
      // The method name must match
      if (ignoreCase && ! method.getName().equalsIgnoreCase(setName))
        continue;
      
      // The method must be public
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      // It must be in a public class or interface
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;

      // It must have a single parameter
      if (method.getParameterTypes().length != 1)
        continue;
      
      // It must return void
      if (! method.getReturnType().equals(void.class))
        continue;

      Class paramType = method.getParameterTypes()[0];
      
      if (paramType.equals(String.class))
        return method;
      else if (bestMethod == null)
        bestMethod = method;
      else if (paramType.getName().compareTo(bestMethod.getParameterTypes()[0].getName()) < 0)
        bestMethod = method;
    }

    return bestMethod;
  }
  
  /**
   * Returns a set method matching the property name.
   */
  public static Method getGetMethod(BeanInfo info, String propertyName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getName().equals(propertyName) &&
          pds[i].getReadMethod() != null) {
        if (! Modifier.isPublic(pds[i].getReadMethod().getDeclaringClass().getModifiers())) {
          try {
            pds[i].getReadMethod().setAccessible(true);
          } catch (Throwable e) {
            continue;
          }
        }

        return pds[i].getReadMethod();
    }
    }

    return getGetMethod(info.getBeanDescriptor().getBeanClass(), propertyName);
  }

  /**
   * Returns a get method matching the property name.
   */
  public static Method getGetMethod(Class cl, String propertyName)
  {
    Method method = getGetMethod(cl, propertyName, false);

    if (method != null)
      return method;
    
    return getGetMethod(cl, propertyName, true);
  }

  /**
   * Returns a get method matching the property name.
   */
  public static Method getGetMethod(Class cl,
                                    String propertyName,
                                    boolean ignoreCase)
  {
    String getName = "get" + propertyNameToMethodName(propertyName);
    String isName = "is" + propertyNameToMethodName(propertyName);

    for (Class ptrCl = cl; ptrCl != null; ptrCl = ptrCl.getSuperclass()) {
      Method method = getGetMethod(ptrCl.getDeclaredMethods(), getName,
                                   isName, ignoreCase);

      if (method != null)
        return method;

      Class []interfaces = ptrCl.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        method = getGetMethod(interfaces[i].getDeclaredMethods(),
                              getName, isName, ignoreCase);

        if (method != null)
          return method;
      }
    }

    return null;
  }

  /**
   * Finds the matching set method
   *
   * @param method the methods for the class
   * @param setName the method name
   */
  private static Method getGetMethod(Method []methods,
                                     String getName,
                                     String isName,
                                     boolean ignoreCase)
  {
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      // The method must be public
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      // It must be in a public class or interface
      if (! Modifier.isPublic(method.getDeclaringClass().getModifiers()))
        continue;

      // It must have no parameters
      if (method.getParameterTypes().length != 0)
        continue;
      
      // It must not return void
      if (method.getReturnType().equals(void.class))
        continue;

      // If it matches the get name, it's the right method
      else if (! ignoreCase && methods[i].getName().equals(getName))
        return methods[i];
      
      // If it matches the get name, it's the right method
      else if (ignoreCase && methods[i].getName().equalsIgnoreCase(getName))
        return methods[i];

      // The is methods must return boolean
      else if (! methods[i].getReturnType().equals(boolean.class))
        continue;
      
      // If it matches the is name, it must return boolean
      else if (! ignoreCase && methods[i].getName().equals(isName))
        return methods[i];
      
      // If it matches the is name, it must return boolean
      else if (ignoreCase && methods[i].getName().equalsIgnoreCase(isName))
        return methods[i];
    }

    return null;
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param propertyName the user property name
   * @return the equivalent bean method name
   */
  public static String propertyNameToMethodName(String propertyName)
  {
    char ch = propertyName.charAt(0);
    if (Character.isLowerCase(ch))
      propertyName = Character.toUpperCase(ch) + propertyName.substring(1);

    return propertyName;
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param methodName the method name
   * @return the equivalent property name
   */
  public static String methodNameToPropertyName(BeanInfo info,
                                                String methodName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();

    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getReadMethod() != null &&
          pds[i].getReadMethod().getName().equals(methodName))
        return pds[i].getName();
      if (pds[i].getWriteMethod() != null &&
          pds[i].getWriteMethod().getName().equals(methodName))
        return pds[i].getName();
    }

    return methodNameToPropertyName(methodName);
  }

  /**
   * Converts a user's property name to a bean method name.
   *
   * @param methodName the method name
   * @return the equivalent property name
   */
  public static String methodNameToPropertyName(String methodName)
  {
    if (methodName.startsWith("get"))
      methodName = methodName.substring(3);
    else if (methodName.startsWith("set"))
      methodName = methodName.substring(3);
    else if (methodName.startsWith("is"))
      methodName = methodName.substring(2);

    if (methodName.length() == 0)
      return null;

    char ch = methodName.charAt(0);
    if (Character.isUpperCase(ch) &&
        (methodName.length() == 1 ||
         ! Character.isUpperCase(methodName.charAt(1)))) {
      methodName = Character.toLowerCase(ch) + methodName.substring(1);
    }

    return methodName;
  }
}
