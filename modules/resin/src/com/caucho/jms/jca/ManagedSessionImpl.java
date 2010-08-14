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

package com.caucho.jms.jca;

import com.caucho.util.L10N;

import javax.jms.*;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 * The managed session
 */
public class ManagedSessionImpl implements ManagedConnection {
  private static final Logger log
    = Logger.getLogger(ManagedSessionImpl.class.getName());
  private static final L10N L = new L10N(ManagedSessionImpl.class);

  private ConnectionEventListener _listener;
  private ConnectionEvent _connClosedEvent;

  private Destination _destination;
  
  private Connection _connection;
  private Session _session;
  private MessageProducer _producer;

  public ManagedSessionImpl(ConnectionFactory factory, Destination destination)
    throws ResourceException
  {
    _destination = destination;

    try {
      _connection = factory.createConnection();
      _session = _connection.createSession(false, 1);
      _producer = _session.createProducer(destination);
    } catch (Exception e) {
      throw new ResourceException(e);
    }
  }

  public ManagedConnectionMetaData getMetaData()
  {
    return null;
  }

  public LocalTransaction getLocalTransaction()
    throws ResourceException
  {
    throw new NotSupportedException();
  }

  public XAResource getXAResource()
    throws ResourceException
  {
    throw new NotSupportedException();
  }

  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    _listener = listener;
  }

  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    _listener = null;
  }

  public ConnectionEventListener getConnectionEventListener()
  {
    return _listener;
  }

  public Object getConnection(Subject subj, ConnectionRequestInfo info)
  {
    return this;
  }

  public void associateConnection(Object o)
    throws ResourceException
  {
    throw new NotSupportedException();
  }

  Session getSession()
  {
    return _session;
  }

  void send(Message message)
    throws JMSException
  {
    _producer.send(message);
  }

  void close()
  {
    if (_listener != null) {
      ConnectionEvent evt;
      evt = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
      evt.setConnectionHandle(this);
      _listener.connectionClosed(evt);
    }
  }

  public PrintWriter getLogWriter()
  {
    return null;
  }

  public void setLogWriter(PrintWriter out)
  {
  }
  
  public void cleanup()
    throws ResourceException
  {
  }

  public void destroy()
    throws ResourceException
  {
    try {
      _producer.close();
      _session.close();
      _connection.close();
    } catch (Exception e) {
      throw new ResourceException(e);
    }
  }
}

