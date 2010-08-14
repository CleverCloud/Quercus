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

package com.caucho.remote;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.config.Admin;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.broker.HempMemoryQueue;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.hemp.servlet.ServerLinkActor;
import com.caucho.hmtp.HmtpReader;
import com.caucho.hmtp.HmtpWriter;
import com.caucho.security.Authenticator;
import com.caucho.server.cluster.Server;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.servlet.WebSocketContext;
import com.caucho.servlet.WebSocketListener;
import com.caucho.util.L10N;

/**
 * Main protocol handler for the HTTP version of BAM.
 */
@SuppressWarnings("serial")
public class HmtpServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());
  private static final L10N L = new L10N(HmtpServlet.class);

  private boolean _isAdmin;
  private boolean _isAuthenticationRequired = true;

  private @Inject Instance<Authenticator> _authInstance;
  private @Inject @Admin Instance<Authenticator> _adminInstance;

  private Broker _broker;
  private Authenticator _auth;
  private ServerAuthManager _authManager;

  public void setAdmin(boolean isAdmin)
  {
    _isAdmin = isAdmin;
  }

  public void setAuthenticationRequired(boolean isAuthRequired)
  {
    _isAuthenticationRequired = isAuthRequired;
  }

  public boolean isAuthenticationRequired()
  {
    return _isAuthenticationRequired;
  }

  public void init()
  {
    String authRequired = getInitParameter("authentication-required");

    if ("false".equals(authRequired))
      _isAuthenticationRequired = false;

    String admin = getInitParameter("admin");

    if ("true".equals(admin))
      _isAdmin = true;

    try {
      if (_isAdmin)
        _auth = _adminInstance.get();
      else
        _auth = _authInstance.get();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.",
                                 this), e);
      }
      else {
        log.info(L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.  In the resin.xml, add an <sec:AdminAuthenticator>",
                   this));
      }
    }

    // _authManager = new ServerAuthManager(_auth);
    _authManager = new ServerAuthManager();
    _authManager.setAuthenticationRequired(_isAuthenticationRequired);

    if (_isAdmin)
      _broker = Server.getCurrent().getAdminBroker();
    else
      _broker = HempBroker.getCurrent();
    
    log.fine(L.l("{0} starting with broker={1}", this, _broker));
  }

  /**
   * Service handling
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequestImpl req = (HttpServletRequestImpl) request;
    HttpServletResponseImpl res = (HttpServletResponseImpl) response;

    String upgrade = req.getHeader("Upgrade");

    if (! "WebSocket".equals(upgrade)) {
      // eventually can use alt method
      res.sendError(400, "Upgrade denied:" + upgrade);
      return;
    }
    
    String ipAddress = req.getRemoteAddr();
    
    WebSocketHandler handler
      = new WebSocketHandler(ipAddress);
    
    WebSocketContext webSocket = req.startWebSocket(handler);

    webSocket.setTimeout(30 * 60 * 1000L);
  }
  
  class WebSocketHandler implements WebSocketListener {
    private String _ipAddress;
    
    private HmtpReader _in;
    private HmtpWriter _out;
    
    private ActorStream _linkStream;
    private ActorStream _brokerStream;
    
    private ServerLinkActor _linkService;

    WebSocketHandler(String ipAddress)
    {
      _ipAddress = ipAddress;
    }
    
    @Override
    public void onStart(WebSocketContext context) throws IOException
    {
      _in = new HmtpReader(context.getInputStream());
      _out = new HmtpWriter(context.getOutputStream());
      _linkStream = new HempMemoryQueue(_out, _broker.getBrokerStream(), 1);
      
      _linkService = new ServerLinkActor(_linkStream, _broker, _authManager,
                                           _ipAddress, false);
      _brokerStream = _linkService.getBrokerStream();
    }

    @Override
    public void onComplete(WebSocketContext context) throws IOException
    {
      _brokerStream.close();
      _linkService.close();
    }

    @Override
    public void onRead(WebSocketContext context) throws IOException
    {
      InputStream is = context.getInputStream();
      
      while (_in.readPacket(_brokerStream)
            && is.available() > 0) {
      }
    }

    @Override
    public void onTimeout(WebSocketContext context) throws IOException
    {
    }
  }
}
