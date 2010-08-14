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

package com.caucho.vfs;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class QJniServerSocket {
  private static final L10N L = new L10N(QJniServerSocket.class);
  private static final Logger log = Log.open(QJniServerSocket.class);
  
  private QJniServerSocket()
  {
  }

  /**
   * Creates the SSL ServerSocket.
   */
  public static QServerSocket create(int port, int listenBacklog)
    throws IOException
  {
    return create(null, port, listenBacklog, true);
  }
  
  public static QServerSocket create(InetAddress host, int port,
                                     int listenBacklog)
    throws IOException
  {
    return create(host, port, listenBacklog, true);
  }

  /**
   * Creates the SSL ServerSocket.
   */
  public static QServerSocket create(InetAddress host, int port,
                                     int listenBacklog,
                                     boolean isEnableJni)
    throws IOException
  {
    if (isEnableJni) {
      try {
        // JNI doesn't listen immediately
        QServerSocket ss = createJNI(host, port);

        if (ss != null)
          return ss;
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    for (int i = 0; i < 10; i++) {
      try {
        ServerSocket ss = new ServerSocket(port, listenBacklog, host);
      
        return new QServerSocketWrapper(ss);
      } catch (BindException e) {
      }

      try {
        Thread.currentThread().sleep(1);
      } catch (Throwable e) {
      }
    }
    
    try {
      ServerSocket ss = new ServerSocket(port, listenBacklog, host);
      
      return new QServerSocketWrapper(ss);
    } catch (BindException e) {
      if (host != null)
        throw new BindException(L.l("{0}\nCan't bind to {1}:{2}.\nCheck for another server listening to that port.", e.getMessage(), host, String.valueOf(port)));
      else
        throw new BindException(L.l("{0}\nCan't bind to *:{1}.\nCheck for another server listening to that port.", e.getMessage(), String.valueOf(port)));
    }

  }

  /**
   * Creates the SSL ServerSocket.
   */
  public static QServerSocket createJNI(InetAddress host, int port)
    throws IOException
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();
      
      Class cl = Class.forName("com.caucho.vfs.JniServerSocketImpl",
                               false, loader);

      Method method = cl.getMethod("create",
                                   new Class[] { String.class,
                                                 int.class });

      String hostAddress;

      if (host != null)
        hostAddress = host.getHostAddress();
      else {
        hostAddress = null;
      }

      try {
        return (QServerSocket) method.invoke(null, hostAddress, port);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    } catch (IOException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      log.fine(e.toString());
      
      throw new IOException(L.l("JNI Socket support requires Resin Professional."));
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new IOException(L.l("JNI Socket support requires Resin Professional."));
    }
  }

  /**
   * Creates the SSL ServerSocket.
   */
  public static QServerSocket openJNI(int fd, int port)
    throws IOException
  {
    try {
      Class cl = Class.forName("com.caucho.vfs.JniServerSocketImpl");

      Method method = cl.getMethod("open", new Class[] { int.class,
                                                         int.class});

      try {
        return (QServerSocket) method.invoke(null, fd, port);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    } catch (IOException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      log.fine(e.toString());
      
      throw new IOException(L.l("JNI Socket support requires Resin Professional."));
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new IOException(L.l("JNI Socket support requires Resin Professional."));
    }
  }
}

