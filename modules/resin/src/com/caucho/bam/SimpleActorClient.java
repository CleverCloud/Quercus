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

package com.caucho.bam;

import java.io.Serializable;

/**
 * ActorClient is a convenience API for sending messages to other Actors,
 * which always using the actor's JID as the "from" parameter.
 */
public class SimpleActorClient implements ActorClient {
  private String _jid;

  private ActorStream _actorStream;
  private ActorStream _linkStream;
  private ActorStream _clientStream;

  private final QueryManager _queryManager = new QueryManager();

  private long _timeout = 10000L;

  public SimpleActorClient()
  {
    _actorStream = new QueryFilterStream();
  }
  
  public SimpleActorClient(Broker broker, 
                           String uid, 
                           String resource)
  {
    this();
    
    _linkStream = broker.getBrokerStream();
    _jid = broker.createClient(_actorStream, uid, resource);
  }

  /**
   * Returns the Actor's jid used for all "from" parameters.
   */
  public String getJid()
  {
    return _jid;
  }
  
  public void setJid(String jid)
  {
    _jid = jid;
  }

  //
  // streams
  //

  /**
   * Registers a callback {@link com.caucho.bam.ActorStream} with the client
   */
  public void setClientStream(ActorStream clientStream)
  {
    _clientStream = clientStream;
  }

  /**
   * Returns the registered callback {@link com.caucho.bam.ActorStream}.
   */
  public ActorStream getClientStream()
  {
    return _clientStream;
  }

  /**
   * Sets the stream to the client
   */
  public void setActorStream(ActorStream actorStream)
  {
    _actorStream = actorStream;
  }

  /**
   * Returns the registered callback {@link com.caucho.bam.ActorStream}.
   */
  public ActorStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * The underlying, low-level stream to the link
   */
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  /**
   * The underlying, low-level stream to the link
   */
  public void setLinkStream(ActorStream linkStream)
  {
    _linkStream = linkStream;
  }

  //
  // message handling
  //

  /**
   * Sends a unidirectional message to an {@link com.caucho.bam.Actor},
   * addressed by the Actor's JID.
   *
   * @param to the target actor's JID
   * @param payload the message payload
   */
  public void message(String to, Serializable payload)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a message because the link is closed.");

    linkStream.message(to, getJid(), payload);
  }

  //
  // query handling
  //

  /**
   * Sends a query information call (get) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable queryGet(String to,
                               Serializable payload)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, _timeout);

    linkStream.queryGet(id, to, getJid(), payload);

    return future.get();
  }

  /**
   * Sends a query information call (get) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable queryGet(String to,
                               Serializable payload,
                               long timeout)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, timeout);

    linkStream.queryGet(id, to, getJid(), payload);

    return future.get();
  }


  /**
   * Sends a query information call (get) to an actor,
   * providing a callback to receive the result or error.
   *
   * The target actor of a <code>queryGet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void queryGet(String to,
                       Serializable payload,
                       QueryCallback callback)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    _queryManager.addQueryCallback(id, callback);

    linkStream.queryGet(id, to, getJid(), payload);
  }

  /**
   * Sends a query update call (set) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>querySet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable querySet(String to,
                               Serializable payload)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, _timeout);

    linkStream.querySet(id, to, getJid(), payload);

    return future.get();
  }

  /**
   * Sends a query update call (set) to an actor,
   * blocking until the actor responds with a result or an error.
   *
   * The target actor of a <code>querySet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   */
  public Serializable querySet(String to,
                               Serializable payload,
                               long timeout)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    QueryFuture future
      = _queryManager.addQueryFuture(id, to, getJid(), payload, timeout);

    linkStream.querySet(id, to, getJid(), payload);

    return future.get();
  }


  /**
   * Sends a query update call (set) to an actor,
   * providing a callback to receive the result or error.
   *
   * The target actor of a <code>querySet</code> acts as a service and the
   * caller acts as a client.  Because BAM Actors are symmetrical, all
   * Actors can act as services and clients for different RPC calls.
   *
   * The target actor MUST send a <code>queryResult</code> or
   * <code>queryError</code> to the client using the same <code>id</code>,
   * because RPC clients rely on a response.
   *
   * @param to the target actor's JID
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void querySet(String to,
                       Serializable payload,
                       QueryCallback callback)
  {
    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + " can't send a query because the link is closed.");

    long id = _queryManager.generateQueryId();
    
    _queryManager.addQueryCallback(id, callback);

    linkStream.querySet(id, to, getJid(), payload);
  }

  //
  // presence handling
  //

  /**
   * Returns true if the client is closed
   */
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Closes the client
   */
  public void close()
  {
    // _queryMap.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
  
  final class QueryFilterStream
    extends AbstractActorStreamFilter {
    @Override
    protected ActorStream getNext()
    {
      ActorStream clientStream = getClientStream();
      
      if (clientStream == null) {
        clientStream = new SimpleActorStream();
      }
      
      return clientStream;
    }

    @Override
    public void queryResult(long id,
                            String to,
                            String from,
                            Serializable payload)
    {
      if (_queryManager.onQueryResult(id, to, from, payload)) {
        return;
      }
      
      super.queryResult(id, to, from, payload);
    }

    @Override
    public void queryError(long id,
                           String to,
                           String from,
                           Serializable payload,
                           ActorError error)
    {
      if (_queryManager.onQueryError(id, to, from, payload, error)) {
        return;
      }
      
      super.queryError(id, to, from, payload, error);
    }
  }
}
