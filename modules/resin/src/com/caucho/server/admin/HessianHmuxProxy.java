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

package com.caucho.server.admin;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy implementation for Hessian clients.  Applications will generally
 * use HessianProxyFactory to create proxy clients.
 */
public class HessianHmuxProxy implements InvocationHandler {
  private static final L10N L = new L10N(HessianHmuxProxy.class);
  
  private Path _path;
  
  private HessianHmuxProxy(Path url)
  {
    _path = url;
  }
  
  public static <X> X create(Path url, Class<X> api)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();
    
    return (X) Proxy.newProxyInstance(loader,
                                      new Class[] { api },
                                      new HessianHmuxProxy(url));
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
    if (methodName.equals("equals") &&
        params.length == 1 && params[0].equals(Object.class)) {
      Object value = args[0];
      if (value == null || ! Proxy.isProxyClass(value.getClass()))
        return new Boolean(false);

      HessianHmuxProxy handler = (HessianHmuxProxy) Proxy.getInvocationHandler(value);

      return new Boolean(_path.equals(handler._path));
    }
    else if (methodName.equals("hashCode") && params.length == 0)
      return new Integer(_path.hashCode());
    else if (methodName.equals("getHessianType"))
      return proxy.getClass().getInterfaces()[0].getName();
    else if (methodName.equals("getHessianURL"))
      return _path.toString();
    else if (methodName.equals("toString") && params.length == 0)
      return "HessianHmuxProxy[" + _path + "]";

    ReadStream is = null;
    
    try {
      if (args != null)
        methodName = methodName + "__" + args.length;
      else
        methodName = methodName + "__0";

      is = sendRequest(methodName, args);

      String code = (String) is.getAttribute("status");

      if (! "200".equals(code)) {
        CharBuffer sb = new CharBuffer();

        int count = 1024;

        while (is.readLine(sb, false) && count-- >= 0) {
        }

        throw new HessianProtocolException(code + ": " + sb);
      }

      int ch = is.read();

      if (ch != 'H')
        throw new HessianProtocolException(L.l("expected 'H' at '{0}'", ch));

      int major = is.read();
      int minor = is.read();

      AbstractHessianInput in = new Hessian2Input(is);

      return in.readReply(method.getReturnType());
    } catch (HessianProtocolException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }
  }

  private ReadStream sendRequest(String methodName, Object []args)
    throws IOException
  {
    ReadWritePair pair = _path.openReadWrite();

    ReadStream is = pair.getReadStream();
    WriteStream os = pair.getWriteStream();

    try {
      Hessian2Output out = new Hessian2Output(os);
      
      out.writeVersion();
      out.call(methodName, args);
      out.flush();

      return is;
    } catch (IOException e) {
      try {
        os.close();
      } catch (Exception e1) {
      }
      
      try {
        is.close();
      } catch (Exception e1) {
      }

      throw e;
    } catch (RuntimeException e) {
      try {
        os.close();
      } catch (Exception e1) {
      }
      
      try {
        is.close();
      } catch (Exception e1) {
      }

      throw e;
    }
  }
}
