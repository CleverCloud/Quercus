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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorException;
import com.caucho.bam.ActorStream;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.SimpleActorClient;
import com.caucho.cloud.security.SecurityService;
import com.caucho.hemp.broker.HempMemoryQueue;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.servlet.WebSocketContext;
import com.caucho.servlet.WebSocketListener;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * HMTP client protocol
 */
public class HmtpClient extends SimpleActorClient {
  private static final L10N L = new L10N(HmtpClient.class);
  
  private static final Logger log
    = Logger.getLogger(HmtpClient.class.getName());

  private String _url;
  private String _jid;

  private WebSocketClient _webSocketClient;
  private WebSocketListener _webSocketHandler;

  private ActorException _connException;

  private ClientAuthManager _authManager = new ClientAuthManager();

  public HmtpClient(String url)
  {
    _url = url;
    
    _webSocketClient = new WebSocketClient(url);
    _webSocketHandler = new HmtpWebSocketHandler();
  }

  public HmtpClient(String url, ActorStream actorStream)
    throws IOException
  {
    this(url);
    
    setClientStream(actorStream);
  }

  public void setVirtualHost(String host)
  {
    _webSocketClient.setVirtualHost(host);
  }

  public void setEncryptPassword(boolean isEncrypt)
  {
  }

  public void connect(String user, String password)
  {
    connectImpl();

    loginImpl(user, password);
  }

  public void connect(String user, Serializable credentials)
  {
    connectImpl();

    loginImpl(user, credentials);
  }

  protected void connectImpl()
  {
    try {
      _webSocketClient.connect(_webSocketHandler);
    } catch (ActorException e) {
      _connException = e;

      throw _connException;
    } catch (IOException e) {
      _connException = new RemoteConnectionFailedException("Failed to connect to server at " + _url + "\n  " + e, 
                                                           e);

      throw _connException;
    }
  }
      
  /**
   * Login to the server
   */
  protected void loginImpl(String uid, Serializable credentials)
  {
    try {
      if (uid == null)
        uid = "";
      
      if (credentials == null)
        credentials = "";
      
      if (credentials instanceof SignedCredentials) {
      }
      else if (credentials instanceof String) {
        String password = (String) credentials;
        
        String clientNonce = String.valueOf(Alarm.getCurrentTime());
        
        NonceQuery nonceQuery = new NonceQuery(uid, clientNonce);
        NonceQuery nonceResult
          = (NonceQuery) queryGet(null, nonceQuery);
        
        String serverNonce = nonceResult.getNonce();
        String serverSignature = nonceResult.getSignature();
        
        String testSignature = _authManager.sign(uid, clientNonce, password);
        
        if (! testSignature.equals(serverSignature) && "".equals(uid))
          throw new ActorException(L.l("{0} server signature does not match",
                                      this));

        String signature = _authManager.sign(uid, serverNonce, password);

        SecurityService security = new SecurityService();
        
        if ("".equals(uid))
          credentials = new SignedCredentials(uid, serverNonce, signature);
        else
          credentials = security.createCredentials(uid, password, serverNonce);
      }

      AuthResult result;
      result = (AuthResult) querySet(null, new AuthQuery(uid, credentials));

      _jid = result.getJid();

      if (log.isLoggable(Level.FINE))
        log.fine(this + " login");
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Returns the broker jid
   */
  public String getBrokerJid()
  {
    String jid = getJid();

    if (jid == null)
      return null;

    int p = jid.indexOf('@');
    int q = jid.indexOf('/');

    if (p >= 0 && q >= 0)
      return jid.substring(p + 1, q);
    else if (p >= 0)
      return jid.substring(p + 1);
    else if (q >= 0)
      return jid.substring(0, q);
    else
      return jid;
  }

  public void flush()
    throws IOException
  {
    /*
    ClientToLinkStream stream = _linkStream;

    if (stream != null)
      stream.flush();
      */
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");

    super.close();
    
    _webSocketClient.close();
   }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jid + "," + _url + "]";
  }

  @Override
  protected void finalize()
  {
    close();
  }
  
  class HmtpWebSocketHandler implements WebSocketListener {
    private HmtpReader _in;
    private HmtpWriter _out;
    
    @Override
    public void onStart(WebSocketContext context) throws IOException
    {
      _out = new HmtpWriter(context.getOutputStream());
      setLinkStream(new HempMemoryQueue(_out, getActorStream(), 1));
      
      _in = new HmtpReader(context.getInputStream());
    }

    @Override
    public void onRead(WebSocketContext context) throws IOException
    {
      InputStream is = context.getInputStream();
      
      while (_in.readPacket(getActorStream()) && is.available() > 0) {
      }
    }

    @Override
    public void onComplete(WebSocketContext context) throws IOException
    {
    }

    @Override
    public void onTimeout(WebSocketContext context) throws IOException
    {
    }    
  }
}
