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

package com.caucho.jmtp;

import com.caucho.bam.Actor;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorException;
import com.caucho.config.ConfigException;
import com.caucho.json.*;
import com.caucho.servlet.*;
import com.caucho.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.servlet.*;

/**
 * JmtpReader stream handles client packets received from the server.
 */
public class JmtpServlet extends GenericServlet {
  private static final L10N L = new L10N(JmtpServlet.class);

  private static final Logger log
    = Logger.getLogger(JmtpServlet.class.getName());

  private Class _actorClass;

  public void setActorClass(Class actorClass)
  {
    _actorClass = actorClass;
  }

  public void init()
  {
    if (_actorClass == null)
      throw new ConfigException(L.l("JmtpServlet requires an actor"));
  }

  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    JanusServletRequest wsRequest = (JanusServletRequest) request;

    Actor actor;

    try {
      actor = (Actor) _actorClass.newInstance();
    } catch (Exception e) {
      throw new ServletException(e);
    }

    Listener listener = new Listener(actor);

    wsRequest.startWebSocket(listener);
  }

  static class Listener implements JanusListener {
    private Actor _actor;
    private ActorStream _actorStream;

    private InputStream _is;
    private OutputStream _os;

    private JmtpReader _jmtpReader;
    private JmtpWriter _jmtpWriter;

    Listener(Actor actor)
    {
      _actor = actor;
    }

    public void onStart(JanusContext context)
      throws IOException
    {
      _is = context.openMessageInputStream();
      _os = context.openMessageOutputStream();

      _jmtpReader = new JmtpReader(_is);
      _jmtpWriter = new JmtpWriter(_os);

      _actor.setLinkStream(_jmtpWriter);
      _actorStream = _actor.getActorStream();
    }

    public void onMessage(JanusContext context)
      throws IOException
    {
      _jmtpReader.readPacket(_actorStream);
    }

    public void onComplete(JanusContext context)
      throws IOException
    {
    }

    public void onTimeout(JanusContext context)
      throws IOException
    {
    }
  }
}
