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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract implementation of an {@link com.caucho.bam.ActorStream}
 * returns query errors for RPC packets, and ignores unknown packets
 * for messages and presence announcement.
 *
 * Most developers will use {@link com.caucho.bam.SimpleActorStream}
 * or {@link com.caucho.bam.SimpleActor} because those classes use
 * introspection with {@link com.caucho.bam.Message @Message} annotations
 * to simplify Actor development.
 */
abstract public class AbstractActorStream implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(AbstractActorStream.class.getName());
  
  /**
   * Returns the jid at the end of the stream.
   */
  @Override
  abstract public String getJid();

  /**
   * Returns the stream to the link.
   */
  abstract public ActorStream getLinkStream();

  //
  // Unidirectional messages
  //
  
  /**
   * Receives a unidirectional message.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's JID
   * @param from the source actor's JID
   * @param payload the message payload
   */
  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " message ignored " + payload
                + " {from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": message is not implemented by this actor.\n"
           + payload + " {from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    ActorStream linkStream = getLinkStream();

    if (linkStream != null)
      linkStream.messageError(from, to, payload, error);
  }
  
  /**
   * Receives a message error.
   *
   * The abstract implementation ignores the message.
   * 
   * @param to the target actor's JID
   * @param from the source actor's JID
   * @param payload the original message payload
   * @param error the message error
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " messageError ignored " + error + " " + payload
                + " {from:" + from + ", to:" + to + "}");
    }
  }

  //
  // RPC query/response calls
  //
  
  /**
   * Receives a query information call (get), acting as a service for
   * the query.
   *
   * The default implementation returns a feature-not-implemented QueryError
   * message to the client.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's JID
   * @param from the client actor's JID
   * @param payload the query payload
   *
   * @return true if this stream understand the query, false otherwise
   */
  @Override
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryGet not implemented " + payload
                + " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": queryGet is not implemented by this actor.\n"
           + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + ".getLinkStream() did not return an ActorStream, which is needed to send an error for a QueryGet");
    
    linkStream.queryError(id, from, to, payload, error);
  }
  
  /**
   * Receives a query update call (set), acting as a service for
   * the query.
   *
   * The default implementation returns a feature-not-implemented QueryError
   * message to the client.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the service actor's JID
   * @param from the client actor's JID
   * @param payload the query payload
   *
   * @return true if this stream understand the query, false otherwise
   */
  @Override
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " querySet not implemented " + payload
                + " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }

    String msg;
    msg = (this + ": querySet is not implemented for this payload:\n"
           + "  " + payload
           + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);


    ActorStream linkStream = getLinkStream();

    if (linkStream == null)
      throw new IllegalStateException(this + ".getLinkStream() did not return an ActorStream, which is needed to send an error for a QuerySet");
        
    linkStream.queryError(id, from, to, payload, error);
  }
  
  /**
   * Handles a query response from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's JID
   * @param from the service actor's JID
   * @param payload the result payload
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryResult ignored " + payload
                + " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }
  }
  
  
  /**
   * Handles a query error from a service Actor.
   * The default implementation ignores the packet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the client actor's JID
   * @param from the service actor's JID
   * @param payload the result payload
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " queryError ignored " + error + " " + payload
                + " {id:" + id + ", from:" + from + ", to:" + to + "}");
    }
  }
  
  /**
   * Tests if the stream is closed.
   */
  @Override
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Closes the stream
   */
  @Override
  public void close()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
