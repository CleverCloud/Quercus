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

package com.caucho.jmx;

import javax.management.*;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * Resin implementation of StandardMBean.
 */
public class IntrospectionMBean implements DynamicMBean {
  private static final L10N L = new L10N(IntrospectionMBean.class);
  private static final Logger log
    = Logger.getLogger(IntrospectionMBean.class.getName());

  private static final Class[] NULL_ARG = new Class[0];

  private static final Class _descriptionAnn;
  private static final Class _nameAnn;

  private static final WeakHashMap<Class,SoftReference<MBeanInfo>> _cachedInfo
    = new WeakHashMap<Class,SoftReference<MBeanInfo>>();

  private final Object _impl;
  private final Class _mbeanInterface;
  private final boolean _isLowercaseAttributeNames;

  private final MBeanInfo _mbeanInfo;

  private final HashMap<String,OpenModelMethod> _attrGetMap
    = new HashMap<String,OpenModelMethod>();

  /**
   * Makes a DynamicMBean.
   */
  public IntrospectionMBean(Object impl, Class mbeanInterface)
    throws NotCompliantMBeanException
  {
    this(impl, mbeanInterface, false);
  }

  /**
   * Makes a DynamicMBean.
   *
   * @param isLowercaseAttributeNames true if attributes should have first
   * letter lowercased
   */
  public IntrospectionMBean(Object impl,
                            Class mbeanInterface,
                            boolean isLowercaseAttributeNames)
    throws NotCompliantMBeanException
  {
    if (impl == null)
      throw new NullPointerException();

    _mbeanInterface = mbeanInterface;
    _isLowercaseAttributeNames = isLowercaseAttributeNames;

    _mbeanInfo = introspect(impl, mbeanInterface, isLowercaseAttributeNames);
    _impl = impl;
  }

  /**
   * Returns the implementation.
   */
  public Object getImplementation()
  {
    return _impl;
  }

  /**
   * Returns an attribute value.
   */
  public Object getAttribute(String attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException
  {
    try {
      OpenModelMethod method = getGetMethod(attribute);

      if (method != null)
        return method.invoke(_impl, (Object []) null);
      else
        throw new AttributeNotFoundException(L.l("'{0}' is an unknown attribute in '{1}'",
                                                 attribute,
                                                 _mbeanInterface.getName()));
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception)
        throw new ReflectionException((Exception) e.getCause());
      else
        throw (Error) e.getCause();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets an attribute value.
   */
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
           MBeanException, ReflectionException
  {
    try {
      Method method = getSetMethod(attribute.getName(), attribute.getValue());

      if (method != null)
        method.invoke(_impl, new Object[] { attribute.getValue() });
      else
        throw new AttributeNotFoundException(attribute.getName());
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      throw new MBeanException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e.toString());
    }
  }

  /**
   * Returns matching attribute values.
   */
  public AttributeList getAttributes(String []attributes)
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.length; i++) {
      try {
        OpenModelMethod method = getGetMethod(attributes[i]);

        if (method != null) {
          Object value = method.invoke(_impl, (Object []) null);

          list.add(new Attribute(attributes[i], value));
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }

  /**
   * Sets attribute values.
   */
  public AttributeList setAttributes(AttributeList attributes)
  {
    AttributeList list = new AttributeList();

    for (int i = 0; i < attributes.size(); i++) {
      try {
        Attribute attr = (Attribute) attributes.get(i);
        Method method = getSetMethod(attr.getName(), attr.getValue());

        if (method != null) {
          method.invoke(_impl, new Object[] { attr.getValue() });
          list.add(new Attribute(attr.getName(), attr.getValue()));
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return list;
  }

  /**
   * Returns the set method matching the name.
   */
  private OpenModelMethod getGetMethod(String name)
  {
    OpenModelMethod method;

    synchronized (_attrGetMap) {
      method = _attrGetMap.get(name);
    }

    if (method != null)
      return method;

    method = createGetMethod(name);

    if (method != null) {
      synchronized (_attrGetMap) {
        _attrGetMap.put(name, method);
      }
    }

    return method;
  }

  /**
   * Returns the get or is method matching the name.
   */
  private OpenModelMethod createGetMethod(String name)
  {
    String methodName;

    if (_isLowercaseAttributeNames)  {
      StringBuilder builder = new StringBuilder(name);
      builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
      methodName = builder.toString();
    }
    else
      methodName = name;

    String getName = "get" + methodName;
    String isName = "is" + methodName;

    Method []methods = _mbeanInterface.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(getName) &&
          ! methods[i].getName().equals(isName))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length == 0 &&
          ! methods[i].getReturnType().equals(void.class)) {
        Class retType = methods[i].getReturnType();

        return new OpenModelMethod(methods[i], createUnmarshall(retType));
      }
    }

    return null;
  }

  /**
v   * Returns the open mbean unmarshaller for the given return type.
   */
  private Unmarshall createUnmarshall(Class cl)
  {
    Unmarshall mbean = getMBeanObjectName(cl);

    if (mbean != null)
      return mbean;

    if (cl.isArray()) {
      Class componentType = cl.getComponentType();
      
      mbean = getMBeanObjectName(componentType);

      if (mbean != null)
        return new UnmarshallArray(ObjectName.class, mbean);
    }

    return Unmarshall.IDENTITY;
  }

  private Unmarshall getMBeanObjectName(Class cl)
  {
    try {
      Method method = cl.getMethod("getObjectName");

      if (method != null
          && ObjectName.class.equals(method.getReturnType()))
        return new UnmarshallMBean(method);
    } catch (NoSuchMethodException e) {
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return null;
  }

  /**
   * Returns the set method matching the name.
   */
  private Method getSetMethod(String name, Object value)
  {
    String methodName;

    if (_isLowercaseAttributeNames)  {
      StringBuilder builder = new StringBuilder(name);
      builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
      methodName = builder.toString();
    }
    else
      methodName = name;

    String setName = "set" + methodName;

    Method []methods = _mbeanInterface.getMethods();

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(setName))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 1)
        continue;

      /*
      if (value != null && ! args[0].isAssignableFrom(value.getClass()))
        continue;
      */

      return methods[i];
    }

    return null;
  }

  /**
   * Invokes a method on the bean.
   */
  public Object invoke(String actionName,
                       Object []params,
                       String []signature)
    throws MBeanException, ReflectionException
  {
    try {
      Method []methods = _mbeanInterface.getMethods();

      int length = 0;
      if (signature != null)
        length = signature.length;
      if (params != null)
        length = params.length;

      for (int i = 0; i < methods.length; i++) {
        if (! methods[i].getName().equals(actionName))
          continue;

        Class []args = methods[i].getParameterTypes();

        if (args.length != length)
          continue;

        boolean isMatch = true;
        for (int j = length - 1; j >= 0; j--) {
          if (signature != null && ! args[j].getName().equals(signature[j]))
            isMatch = false;
        }

        if (isMatch) {
          return methods[i].invoke(_impl, params);
        }
      }

      if (actionName.equals("hashCode")
          && (signature == null || signature.length == 0))
        return _impl.hashCode();
      else if (actionName.equals("toString")
               && (signature == null || signature.length == 0))
        return _impl.toString();
      else
        return null;
    } catch (IllegalAccessException e) {
      throw new MBeanException(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception)
        throw new ReflectionException((Exception) e.getCause());
      else
        throw (Error) e.getCause();
    }
  }

  /**
   * Returns the introspection information for the MBean.
   */
  public MBeanInfo getMBeanInfo()
  {
    return _mbeanInfo;
  }

  static MBeanInfo introspect(Object obj, Class cl,
                              boolean isLowercaseAttributeNames)
    throws NotCompliantMBeanException
  {
    try {
      SoftReference<MBeanInfo> infoRef = _cachedInfo.get(cl);
      MBeanInfo info = null;

      if (infoRef != null && (info = infoRef.get()) != null)
        return info;

      String className = cl.getName();

      HashMap<String,MBeanAttributeInfo> attributes
        = new HashMap<String,MBeanAttributeInfo>();

      ArrayList<MBeanConstructorInfo> constructors
        = new ArrayList<MBeanConstructorInfo>();

      ArrayList<MBeanOperationInfo> operations
        = new ArrayList<MBeanOperationInfo>();

      Method []methods = cl.getMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];

        if (method.getDeclaringClass() == Object.class)
          continue;

        if (Modifier.isStatic(method.getModifiers()))
          continue;

        String methodName = method.getName();
        Class []args = method.getParameterTypes();
        Class retType = method.getReturnType();

        if (methodName.startsWith("get") && args.length == 0
            && ! retType.equals(void.class)) {
          Method getter = method;
          String name = methodName.substring(3);

          Method setter = getSetter(methods, name, retType);

          String attributeName;

          if (isLowercaseAttributeNames)  {
            StringBuilder builder = new StringBuilder(name);
            builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
            attributeName = builder.toString();
          }
          else
            attributeName = name;

          Class type = method.getReturnType();

          MBeanAttributeInfo attr;

          attr = new MBeanAttributeInfo(attributeName,
                                        getDescription(method),
                                        getter,
                                        setter);

          /*
          Descriptor descriptor = attr.getDescriptor();

          if (descriptor != null) {
            Object openType = getOpenType(type);

            if (openType != null)
              descriptor.setField("openType", openType);

            descriptor.setField("originalType", getTypeName(type));

            attr.setDescriptor(descriptor);
          }
          */

          if (attributes.get(attributeName) == null)
            attributes.put(attributeName, attr);
        }
        else if (methodName.startsWith("is") && args.length == 0 &&
                 (retType.equals(boolean.class) ||
                  retType.equals(Boolean.class))) {
          Method getter = method;
          String name = methodName.substring(2);

          Method setter = getSetter(methods, name, retType);

          String attributeName;

          if (isLowercaseAttributeNames)  {
            StringBuilder builder = new StringBuilder(name);
            builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
            attributeName = builder.toString();
          }
          else
            attributeName = name;

          if (attributes.get(attributeName) == null) {
            attributes.put(attributeName,
                           new MBeanAttributeInfo(attributeName,
                                                  getDescription(method),
                                                  getter,
                                                  setter));
          }
        }
        else if (methodName.startsWith("set") && args.length == 1) {
          Method setter = method;
          String name = methodName.substring(3);

          Method getter = getGetter(methods, name, args[0]);

          if (getter == null) {
            String attributeName;

            if (isLowercaseAttributeNames)  {
              StringBuilder builder = new StringBuilder(name);
              builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
              attributeName = builder.toString();
            }
            else
              attributeName = name;

            if (attributes.get(attributeName) == null) {
              attributes.put(attributeName,
                             new MBeanAttributeInfo(attributeName,
                                                    getDescription(method),
                                                    null,
                                                    setter));
            }
          }
        }
        else {
          operations.add(new MBeanOperationInfo(getName(method),
                                                getDescription(method),
                                                getSignature(method),
                                                method.getReturnType().getName(),
                                                MBeanOperationInfo.UNKNOWN));
        }
      }

      ArrayList<MBeanNotificationInfo> notifications
        = new ArrayList<MBeanNotificationInfo>();

      if (obj instanceof NotificationBroadcaster) {
        NotificationBroadcaster broadcaster;
        broadcaster = (NotificationBroadcaster) obj;

        MBeanNotificationInfo[] notifs = broadcaster.getNotificationInfo();

        if (notifs != null) {
          for (int i = 0; i < notifs.length; i++) {
            MBeanNotificationInfo notif = notifs[i];

            notifications.add((MBeanNotificationInfo) notifs[i].clone());
          }
        }
      }

      Collections.sort(notifications, MBEAN_FEATURE_INFO_COMPARATOR);

      MBeanAttributeInfo []attrArray = new MBeanAttributeInfo[attributes.size()];
      attributes.values().toArray(attrArray);
      Arrays.sort(attrArray, MBEAN_FEATURE_INFO_COMPARATOR);
      
      MBeanConstructorInfo []conArray = new MBeanConstructorInfo[constructors.size()];
      constructors.toArray(conArray);

      MBeanOperationInfo []opArray = new MBeanOperationInfo[operations.size()];
      operations.toArray(opArray);
      Arrays.sort(opArray, MBEAN_FEATURE_INFO_COMPARATOR);
      MBeanNotificationInfo []notifArray = new MBeanNotificationInfo[notifications.size()];
      notifications.toArray(notifArray);
      Arrays.sort(notifArray, MBEAN_FEATURE_INFO_COMPARATOR);

      MBeanInfo modelInfo;

      modelInfo = new MBeanInfo(cl.getName(),
                                     getDescription(cl),
                                     attrArray,
                                     conArray,
                                     opArray,
                                     notifArray);
      /*
      Descriptor descriptor = modelInfo.getMBeanDescriptor();
      if (descriptor != null) {
        descriptor.setField("mxbean", "true");
        modelInfo.setMBeanDescriptor(descriptor);
      }
      */

      info = modelInfo;

      _cachedInfo.put(cl, new SoftReference<MBeanInfo>(info));

      return info;
    } catch (Exception e) {
      NotCompliantMBeanException exn;
      exn = new NotCompliantMBeanException(String.valueOf(e));

      exn.initCause(e);

      throw exn;
    }
  }

  /**
   * Returns the matching setter.
   */
  static Method getSetter(Method []methods, String property, Class type)
  {
    String name = "set" + property;

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(name))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 1 || ! args[0].equals(type))
        continue;

      return methods[i];
    }

    return null;
  }

  /**
   * Returns the matching getter.
   */
  static Method getGetter(Method []methods, String property, Class type)
  {
    String getName = "get" + property;
    String isName = "is" + property;

    for (int i = 0; i < methods.length; i++) {
      if (! methods[i].getName().equals(getName) &&
          ! methods[i].getName().equals(isName))
        continue;

      Class []args = methods[i].getParameterTypes();

      if (args.length != 0)
        continue;

      Class retType = methods[i].getReturnType();

      if (! retType.equals(type))
        continue;

      return methods[i];
    }

    return null;
  }

  /**
   * Returns the class's description.
   */
  static String getDescription(Class cl)
  {
    try {
      Description desc = (Description) cl.getAnnotation(_descriptionAnn);

      if (desc != null)
        return desc.value();
      else
        return "";
    } catch (Throwable e) {
      return "";
    }
  }

  /**
   * Returns the method's description.
   */
  static String getDescription(Method method)
  {
    try {
      Description desc = (Description) method.getAnnotation(_descriptionAnn);

      if (desc != null)
        return desc.value();
      else
        return "";
    } catch (Throwable e) {
      return "";
    }
  }

  /**
   * Returns the method's name, the optional {@link Name} annotation overrides.
   */
  static String getName(Method method)
  {
    try {
      Name name = (Name) method.getAnnotation(_nameAnn);

      if (name != null)
        return name.value();
      else
        return method.getName();
    } catch (Throwable e) {
      return method.getName();
    }
  }

  private static MBeanParameterInfo[] getSignature(Method method)
  {
    Class[] params = method.getParameterTypes();
    MBeanParameterInfo[] paramInfos = new MBeanParameterInfo[params.length];

    for (int i = 0; i < params.length; i++) {
      Class  cl = params[i];

      String name = getName(method, i);
      String description = getDescription(method, i);

      paramInfos[i] = new MBeanParameterInfo(name, cl.getName(), description);
    }

    return paramInfos;
  }

  private static String getName(Method method, int i)
  {
    try {
      for (Annotation ann : method.getParameterAnnotations()[i]) {
        if (ann instanceof Name)
          return ((Name) ann).value();
      }
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return "p" + i;
  }

  private static String getDescription(Method method, int i)
  {
    try {
      for (Annotation ann : method.getParameterAnnotations()[i]) {
        if (ann instanceof Description)
          return ((Description) ann).value();
      }
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return "";
  }

  private static String getTypeName(Class type)
  {
    if (type.isArray())
      return getTypeName(type.getComponentType()) + "[]";
    else
      return type.getName();
  }

  private static OpenType getOpenType(Class type)
  {
    try {
      if (type.isArray()) {
        OpenType component = getOpenType(type.getComponentType());

        if (component != null)
          return new ArrayType(1, component);
        else
          return null;
      }
      else if (type.getName().endsWith("MXBean")
               || type.getName().endsWith("MBean"))
        return SimpleType.OBJECTNAME;
      else if (void.class.equals(type))
        return SimpleType.VOID;
      else if (boolean.class.equals(type) || Boolean.class.equals(type))
        return SimpleType.BOOLEAN;
      else if (byte.class.equals(type) || Byte.class.equals(type))
        return SimpleType.BYTE;
      else if (short.class.equals(type) || Short.class.equals(type))
        return SimpleType.SHORT;
      else if (int.class.equals(type) || Integer.class.equals(type))
        return SimpleType.INTEGER;
      else if (long.class.equals(type) || Long.class.equals(type))
        return SimpleType.LONG;
      else if (float.class.equals(type) || Float.class.equals(type))
        return SimpleType.FLOAT;
      else if (double.class.equals(type) || Double.class.equals(type))
        return SimpleType.DOUBLE;
      else if (String.class.equals(type))
        return SimpleType.STRING;
      else if (char.class.equals(type) || Character.class.equals(type))
        return SimpleType.CHARACTER;
      else if (java.util.Date.class.equals(type))
        return SimpleType.DATE;
      else if (java.util.Calendar.class.equals(type))
        return SimpleType.DATE;
      else
        return null; // can't deal with more complex at the moment
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }

  private static Class findClass(String name)
  {
    try {
      return Class.forName(name);
    } catch (Throwable e) {
      return null;
    }
  }

  private static final Comparator<MBeanFeatureInfo> MBEAN_FEATURE_INFO_COMPARATOR
    = new Comparator<MBeanFeatureInfo>() {

    public int compare(MBeanFeatureInfo o1, MBeanFeatureInfo o2)
    {
      return o1.getName().compareTo(o2.getName());
    }
  };

  static {
    _descriptionAnn = findClass("com.caucho.jmx.Description");
    _nameAnn = findClass("com.caucho.jmx.Name");
  }
}
