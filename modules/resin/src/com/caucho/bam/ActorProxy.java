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
 * ActorProxy is a convenience API for sending messages to a specific Actors,
 * which always using the actor's JID as the "from" parameter.
 */
public interface ActorProxy {
  /**
   * Returns the proxy's jid used for all "from" parameters.
   */
  public String getJid();

  /**
   * Returns the target actor's jid used for all "to" parameters.
   */
  public String getTo();

  //
  // message handling
  //

  /**
   * Sends a unidirectional message to the target {@link com.caucho.bam.Actor},
   *
   * @param payload the message payload
   */
  public void message(Serializable payload);

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
   * @param payload the query payload
   */
  public Serializable queryGet(Serializable payload);

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
   * @param payload the query payload
   * @param timeout time spent waiting for the query to return
   */
  public Serializable queryGet(Serializable payload,
                               long timeout);

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
  public void queryGet(Serializable payload,
                       QueryCallback callback);

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
   * @param payload the query payload
   */
  public Serializable querySet(Serializable payload);

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
   * @param payload the query payload
   * @param timeout time spent waiting for the query to return
   */
  public Serializable querySet(Serializable payload,
                               long timeout);

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
   * @param payload the query payload
   * @param callback the application's callback for the result
   */
  public void querySet(Serializable payload,
                       QueryCallback callback);

  //
  // presence handling
  //

  /**
   * Registers a callback {@link com.caucho.bam.ActorStream} with the client
   */
  public void setClientStream(ActorStream clientStream);

  /**
   * Returns the registered callback {@link com.caucho.bam.ActorStream}.
   */
  public ActorStream getClientStream();

  /**
   * Returns the stream to this client.
   */
  public ActorStream getActorStream();

  /**
   * The ActorStream to the link.
   */
  public ActorStream getLinkStream();

  /**
   * Sets the ActorStream to the link.
   */
  public void setLinkStream(ActorStream linkStream);
  
  //
  // lifecycle
  //

  /**
   * Returns true if the proxy is closed
   */
  public boolean isClosed();

  /**
   * Closes the proxy
   */
  public void close();
}
