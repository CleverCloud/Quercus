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

package com.caucho.xmpp;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.inject.Module;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Protocol handler from the TCP/XMPP stream forwarding to the broker
 */
@Module
public class XmppBrokerStream
  implements SocketLinkDuplexListener, ActorStream
{
  private static final Logger log
    = Logger.getLogger(XmppBrokerStream.class.getName());

  private XmppRequest _request;
  private XmppProtocol _protocol;
  private XmppContext _xmppContext;
  
  private Broker _broker;
  private ActorClient _conn;
  private ActorStream _toBroker;

  private ActorStream _toClient;
  private ActorStream _authHandler;

  private ReadStream _is;
  private WriteStream _os;
  
  private XmppStreamReader _in;
  private XmppStreamWriter _out;

  private XmppReader _reader;

  private String _jid;
  private long _requestId;

  private String _uid = "test@localhost";
  private boolean _isFinest;

  // XXX: needs timeout(?)
  private HashMap<Long,String> _idMap = new HashMap<Long,String>();

  XmppBrokerStream(XmppRequest request, Broker broker,
                   ReadStream is, XmppStreamReader in, WriteStream os)
  {
    _request = request;
    _protocol = request.getProtocol();
    _xmppContext = new XmppContext(_protocol.getMarshalFactory());
    _broker = broker;

    if (broker == null)
      throw new NullPointerException();

    _in = in;
    _os = os;

    _uid = request.getUid();

    _out =  new XmppStreamWriterImpl(os, _protocol.getMarshalFactory());

    _toClient = new XmppAgentStream(this, _os);
    _authHandler = null;//new AuthBrokerStream(this, _callbackHandler);

    _reader = new XmppReader(_xmppContext,
                             is, _in, _toClient,
                             new XmppBindCallback(this));

    _reader.setUid(_uid);

    _isFinest = log.isLoggable(Level.FINEST);
  }

  public String getJid()
  {
    return _jid;
  }

  ActorStream getActorStream()
  {
    return _toClient;
  }

  XmppMarshalFactory getMarshalFactory()
  {
    return _protocol.getMarshalFactory();
  }

  XmppContext getXmppContext()
  {
    return _xmppContext;
  }
  
  public void onRead(SocketLinkDuplexController context)
    throws IOException
  {
    _reader.readNext();
  }
  
  public void onComplete(SocketLinkDuplexController context)
    throws IOException
  {
  }
  
  public void onTimeout(SocketLinkDuplexController context)
    throws IOException
  {
  }
  
  public boolean serviceWrite(WriteStream os,
                              SocketLinkDuplexController controller)
    throws IOException
  {
    return false;
  }

  String login(String uid, Serializable credentials, String resource)
  {
    String password = (String) credentials;
    
    _uid = uid + _broker.getJid();
    
    _toBroker = _broker.getBrokerStream();
    _jid = _broker.createClient(_toClient, uid, resource);

    return _jid;
  }

  String bind(String resource, String jid)
  {
    String password = null;
    
    _toBroker = _broker.getBrokerStream();
    _jid = _broker.createClient(_toClient, jid, resource);
     
    return _jid;
  }

  String findId(long bamId)
  {
    synchronized (_idMap) {
      return _idMap.remove(bamId);
    }
  }

  void writeValue(Serializable value)
    throws IOException, XMLStreamException
  {
    if (value == null)
      return;
    
    XmppMarshalFactory marshalFactory = _protocol.getMarshalFactory();
    XmppMarshal marshal = marshalFactory.getSerialize(value.getClass().getName());

    if (marshal != null) {
      marshal.toXml(_out, value);
    }
  }
  
  /**
   * Handles a message
   */
  public void message(String to,
                      String from,
                      Serializable value)
  {
    _toBroker.message(to, _jid, value);
  }
  
  /**
   * Handles a message
   */
  public void messageError(String to,
                           String from,
                           Serializable value,
                           ActorError error)
  {
    _toBroker.messageError(to, _jid, value, error);
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void queryGet(long id,
                              String to,
                              String from,
                              Serializable value)
  {
    _toBroker.queryGet(id, to, _jid, value);
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void querySet(long id,
                              String to,
                              String from,
                              Serializable value)
  {
    _toBroker.querySet(id, to, _jid, value);
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void queryResult(long id,
                              String to,
                              String from,
                              Serializable value)
  {
    _toBroker.queryResult(id, to, _jid, value);
  }
  
  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void queryError(long id,
                             String to,
                             String from,
                             Serializable value,
                             ActorError error)
  {
    _toBroker.queryError(id, to, _jid, value, error);
  }
  
  public boolean isClosed()
  {
    return _in == null;
  }

  public void close()
  {
    XmppStreamReader in = _in;
    _in = null;

    if (in != null) {
      try { in.close(); } catch (Exception e) {}
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }

  /* (non-Javadoc)
   * @see com.caucho.network.listen.SocketLinkDuplexListener#onStart(com.caucho.network.listen.SocketLinkDuplexController)
   */
  @Override
  public void onStart(SocketLinkDuplexController context) throws IOException
  {
    // TODO Auto-generated method stub
    
  }
}
