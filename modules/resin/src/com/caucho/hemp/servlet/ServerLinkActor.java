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

package com.caucho.hemp.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorException;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.bam.QueryGet;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;
import com.caucho.hmtp.AuthQuery;
import com.caucho.hmtp.AuthResult;
import com.caucho.hmtp.GetPublicKeyQuery;
import com.caucho.hmtp.NonceQuery;

/**
 * The LinkService is low-level link
 */

public class ServerLinkActor extends SimpleActor {
  private static final Logger log
    = Logger.getLogger(ServerLinkActor.class.getName());
  
  private final Broker _broker;
  private final ServerLinkStream _serverLinkStream;
  private final ServerPassStream _serverPassStream;
  private final ActorStream _brokerStream;
  private final ServerAuthManager _authManager;
  private final String _ipAddress;
  
  /**
   * Creates the LinkService for low-level link messages
   */
  public ServerLinkActor(ActorStream linkStream,
                         Broker broker,
                         ServerAuthManager authManager,
                         String ipAddress,
                         boolean isUnidir)
  {
    if (linkStream == null)
      throw new NullPointerException();
    
    if (broker == null)
      throw new NullPointerException();
    
    setLinkStream(linkStream);
    
    _broker = broker;
    _authManager = authManager;
    _ipAddress = ipAddress;
   
    if (isUnidir) {
      _serverPassStream = new ServerPassStream(linkStream, this);
      _brokerStream = _serverPassStream;
      _serverLinkStream = null;
    }
    else {
      _serverPassStream = null;
      _serverLinkStream = new ServerLinkStream(linkStream, this);
      _brokerStream = _serverLinkStream;
    }
  }
  
  public ActorStream getBrokerStream()
  {
    return _brokerStream;
  }

  //
  // message handling
  //

  @QueryGet
  public void getNonce(long id, String to, String from,
                       NonceQuery query)
  {
    NonceQuery result = _authManager.generateNonce(query);

    getLinkStream().queryResult(id, from, to, result);
  }

  @QuerySet
  public void authLogin(long id, String to, String from, LoginQuery query)
  {
    login(id, to, from, query.getAuth(), query.getAddress());
  }

  @QuerySet
  public void authLogin(long id, String to, String from, AuthQuery query)
  {
    login(id, to, from, query, _ipAddress);
  }

  private void login(long id, String to, String from,
                     AuthQuery query, String ipAddress)
  {
    String uid = query.getUid();
    Object credentials = query.getCredentials();
  
    try {
      _authManager.authenticate(query.getUid(), credentials, ipAddress);
    } catch (ActorException e) {
      log.log(Level.FINE, e.toString(), e);
    
      getLinkStream().queryError(id, from, to, query,
                                 e.createActorError());

      return;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    
      getLinkStream().queryError(id, from, to, query,
                                 new ActorError(ActorError.TYPE_AUTH,
                                                ActorError.NOT_AUTHORIZED,
                                                e.getMessage()));
      return;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (_serverLinkStream != null)
      _serverLinkStream.setBrokerStream(_broker.getBrokerStream());
    if (_serverPassStream != null)
      _serverPassStream.setBrokerStream(_broker.getBrokerStream());

    String jid
      = _broker.createClient(getLinkStream(), uid, query.getResource());

    if (_serverLinkStream != null)
      _serverLinkStream.setJid(jid);
    if (_serverPassStream != null)
      _serverPassStream.setJid(jid);
    
    notifyValidLogin(from);
    
    AuthResult result = new AuthResult(jid);
    getLinkStream().queryResult(id, from, to, result);
  }
  
  protected void notifyValidLogin(String jid)
  {
  }
}
