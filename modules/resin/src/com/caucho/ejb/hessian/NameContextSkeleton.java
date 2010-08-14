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

package com.caucho.ejb.hessian;

import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.Skeleton;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.HessianRemote;
import com.caucho.services.name.NameServerRemote;
import com.caucho.util.*;

import javax.ejb.EJBHome;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for any bean skeleton capable of handling a Hessian request.
 *
 * <p/>Once selected, the calling servlet will dispatch the request through
 * the <code>_service</code> call.  After parsing the request headers,
 * <code>_service</code> calls the generated entry <code>_execute</code>
 * to execute the request.
 */
public class NameContextSkeleton extends Skeleton {
  private static final L10N L = new L10N(NameContextSkeleton.class);
  private static final Logger log
    = Logger.getLogger(NameContextSkeleton.class.getName());

  private HessianProtocol _protocol;
  private String _prefix;

  NameContextSkeleton(HessianProtocol protocol, String prefix)
  {
    _protocol = protocol;
    _prefix = prefix;
  }

  /**
   * Services the request.
   */
  public void _service(InputStream is, OutputStream os)
    throws Exception
  {
    int ch = is.read();

    if (ch != 'H')
      throw new IOException(L.l("expected 'H' for Hessian 2 at '{0}'", String.valueOf((char) ch)));

    is.read();
    is.read();

    Hessian2Input in = new Hessian2Input(is);
    Hessian2Output out = new Hessian2Output(os);

    in.startCall();

    String method = in.getMethod();
    Object []args = in.readArguments();
    in.completeCall();

    try {
      if (method.equals("lookup") ||
          method.equals("lookup_string") ||
          method.equals("lookup_1"))
        executeLookup(args, in, out);
      else if (method.equals("list"))
        executeList(args, in, out);
      else
        executeUnknown(method, args, in, out);
    } catch (HessianProtocolException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      out.startReply();
      out.writeFault("ServiceException", e.getMessage(), e);
      out.completeReply();
    }
    
    out.flush();
  }

  private void executeLookup(Object []args,
                             Hessian2Input in,
                             Hessian2Output out)
    throws Throwable
  {
    String name = (String) args[0];

    while (name.startsWith("/"))
      name = name.substring(1);
    
    EjbProtocolManager container = _protocol.getProtocolManager();
    
    AbstractEjbBeanManager server;

    server = container.getServerByServerId(name);

    if (server == null)
      server = container.getServerByEJBName(name);

    if (server != null) {
      /*
      EJBHome home = server.getEJBHome();
      
      out.startReply();

      if (home != null)
        out.writeObject(home);
      else // if (server instanceof 
        out.writeObject(server.getRemoteObject(server.getRemoteHomeClass(), "hessian"));

      out.completeReply();
      */
      throw new UnsupportedOperationException(getClass().getName());
    }
    else if (container.getRemoteChildren(name) != null) {
      out.startReply();

      String serverId;

      if (_prefix.endsWith("/") || name.startsWith("/"))
        serverId = _prefix + name;
      else
        serverId = _prefix + '/' + name;

      if (serverId.startsWith("/"))
        serverId = serverId.substring(1);

      String url;
      String prefix = _protocol.getURLPrefix();
      if (prefix.endsWith("/"))
        url = prefix + serverId;
      else
        url = prefix + '/' + serverId;

      out.writeObject(new HessianRemote(NameServerRemote.class.getName(), url));
      
      out.completeReply();
    }
    else {
      out.startReply();

      out.writeNull();
      out.completeReply();
    }
  }

  private void executeList(Object []args,
                           Hessian2Input in,
                           Hessian2Output out)
    throws Throwable
  {
    EjbProtocolManager container = _protocol.getProtocolManager();
    
    AbstractEjbBeanManager server = container.getServerByEJBName(_prefix);

    ArrayList children;

    if (server != null) {
      throw new UnsupportedOperationException(getClass().getName());
      /*
      EJBHome home = server.getEJBHome();
      
      out.startReply();
      
      out.writeNull();

      out.completeReply();
      */
    }
    else if ((children = container.getRemoteChildren(_prefix)) != null) {
      out.startReply();
      
      out.writeObject(children.toArray(new String[children.size()]));
      
      out.completeReply();
    }
    else {
      out.startReply();

      out.writeNull();
      out.completeReply();
    }
  }

  /**
   * Executes an unknown method.
   *
   * @param method the method name to match.
   * @param in the hessian input stream
   * @param out the hessian output stream
   */
  protected void executeUnknown(String method,
                                Object []args,
                                Hessian2Input in,
                                Hessian2Output out)
    throws Exception
  {
    if (method.equals("_hessian_getAttribute")) {
      String key = (String) args[0];

      out.startReply();

      if ("java.api.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.home.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.object.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("home-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("remote-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else
        out.writeNull();

      out.completeReply();
    }
    else {
      out.startReply();
      out.writeFault("NoMethod", "no such method: " + method, null);
      out.completeReply();
    }
  }
}
