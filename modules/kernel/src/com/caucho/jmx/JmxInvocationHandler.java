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

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy hander for mbeans.
 */
public class JmxInvocationHandler implements InvocationHandler {
  private MBeanServer _server;
  private ClassLoader _loader;
  private ObjectName _name;

  /**
   * Creates the invocation handler.
   */
  public JmxInvocationHandler(MBeanServer mbeanServer,
                              ClassLoader loader,
                              ObjectName objectName)
  {
    _server = mbeanServer;
    _loader = loader;
    _name = objectName;
  }

  /**
   * Creates a new proxy instance.
   */
  public static Object newProxyInstance(MBeanServer server,
                                        ClassLoader loader,
                                        ObjectName objectName,
                                        Class interfaceClass,
                                        boolean notificationBroadcaster)
  {
    Class []interfaces;

    if (notificationBroadcaster)
      interfaces = new Class[] { interfaceClass, NotificationEmitter.class };
    else
      interfaces = new Class[] { interfaceClass };

    JmxInvocationHandler handler;
    
    handler = new JmxInvocationHandler(server, loader, objectName);
    
    return Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                                  interfaces,
                                  handler);
  }

  /**
   * Handles the object invocation.
   *
   * @param proxy the proxy object to invoke
   * @param method the method to call
   * @param args the arguments to the proxy object
   */
  public Object invoke(Object proxy, Method method, Object []args)
    throws Throwable
  {
    String methodName = method.getName();
    Class []params = method.getParameterTypes();

    // equals and hashCode are special cased
    if (methodName.equals("equals")
        && params.length == 1 && params[0].equals(Object.class)) {
      Object value = args[0];
      if (value == null || ! Proxy.isProxyClass(value.getClass()))
        return Boolean.FALSE;

      JmxInvocationHandler handler;

      handler = (JmxInvocationHandler) Proxy.getInvocationHandler(value);

      return new Boolean(_name.equals(handler._name));
    }
    else if (methodName.equals("hashCode") && params.length == 0)
      return new Integer(_name.hashCode());

    int len = methodName.length();
    String attrName;

    Class returnType = method.getReturnType();
    
    if (params.length == 0 && methodName.startsWith("get") && len > 3) {
      attrName = methodName.substring(3);

      return marshall(_server.getAttribute(_name, attrName), returnType);
    }
    else if (params.length == 1 && returnType.equals(void.class) &&
             methodName.startsWith("set") && len > 3) {
      attrName = methodName.substring(3);

      Attribute attr = new Attribute(attrName, args[0]);

      _server.setAttribute(_name, attr);

      return null;
    }
    else if (methodName.equals("addNotificationListener")) {
      if (args.length != 3) {
      }
      else if (args[0] instanceof NotificationListener) {
        _server.addNotificationListener(_name,
                                        (NotificationListener) args[0],
                                        (NotificationFilter) args[1],
                                        args[2]);
        return null;
      }
      else if (args[0] instanceof ObjectName) {
        _server.addNotificationListener(_name,
                                        (ObjectName) args[0],
                                        (NotificationFilter) args[1],
                                        args[2]);
        return null;
      }
    }
    else if (methodName.equals("removeNotificationListener")) {
      if (args.length == 3) {
        if (args[0] instanceof NotificationListener) {
          _server.removeNotificationListener(_name,
                                             (NotificationListener) args[0],
                                             (NotificationFilter) args[1],
                                             args[2]);
          return null;
        }
        else if (args[0] instanceof ObjectName) {
          _server.removeNotificationListener(_name,
                                             (ObjectName) args[0],
                                             (NotificationFilter) args[1],
                                             args[2]);
          return null;
        }
      }
      else if (args.length == 1) {
        if (args[0] instanceof NotificationListener) {
          _server.removeNotificationListener(_name,
                                             (NotificationListener) args[0]);
          return null;
        }
        else if (args[0] instanceof ObjectName) {
          _server.removeNotificationListener(_name, (ObjectName) args[0]);

          return null;
        }
      }
    }

    String []sig = new String[params.length];

    for (int i = 0; i < sig.length; i++)
      sig[i] = params[i].getName();

    Object value = _server.invoke(_name, methodName, args, sig);

    return marshall(value, returnType);
  }

  private Object marshall(Object value, Class retType)
  {
    if (retType == null || value == null ||
        retType.isAssignableFrom(value.getClass()))
      return value;

    if (value instanceof ObjectName) {
      ObjectName name = (ObjectName) value;

      Object proxy = Jmx.find(name, _loader, _server);

      if (retType.isInstance(proxy))
        return proxy;
      else
        return value;
    }
    else if (value instanceof ObjectName[] && retType.isArray()) {
      Class type = retType.getComponentType();
      ObjectName []names = (ObjectName []) value;
      
      Object proxies = Array.newInstance(type, names.length);

      for (int i = 0; i < names.length; i++) {
        Object proxy = Jmx.find(names[i], _loader, _server);

        if (proxy == null) {
          Array.set(proxies, i, null);
          continue;
        }

        if (! type.isInstance(proxy))
          return value;

        Array.set(proxies, i, Jmx.find(names[i], _loader, _server));
      }

      return proxies;
    }

    return value;
  }
}
