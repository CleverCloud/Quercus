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

import java.util.logging.*;
import java.util.concurrent.atomic.*;

import java.io.Serializable;

/**
 * Base ActorStream implementation using introspection and
 * {@link com.caucho.bam.Message @Message} annotations to simplify
 * Actor development.
 *
 * <h2>Message Handline</h2>
 *
 * To handle a message, create a method with the proper signature for
 * the expected payload type and
 * annotate it with {@link com.caucho.bam.Message @Message}.  To send
 * a response message or query, use <code>getBrokerStream()</code> or
 * <code>getClient()</code>.
 *
 * <code><pre>
 * @Message
 * public void myMessage(String to, String from, MyPayload payload);
 * </pre></code>
 */
public class SimpleActorStream implements ActorStream
{
  private static final Logger log
    = Logger.getLogger(SimpleActorStream.class.getName());
  
  private final Skeleton _skeleton;

  private String _jid;
  private ActorStream _linkStream;
 
  public SimpleActorStream()
  {
    _skeleton = Skeleton.getSkeleton(getClass());
  }

  /**
   * Returns the Actor's jid so the {@link com.caucho.bam.Broker} can
   * register it.
   */
  @Override
  public String getJid()
  {
    return _jid;
  }

  /**
   * Sets the Actor's jid so the {@link com.caucho.bam.Broker} can
   * register it.
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  /**
   * Returns the stream to the broker for query results or errors, or
   * low-level messaging.
   */
  public void setLinkStream(ActorStream linkStream)
  {
    _linkStream = linkStream;
  }

  //
  // message
  //

  /**
   * Dispatches a unidirectional message to a matching method on
   * the SimpleActorStream.
   *
   * By default, message invokes a method
   * annotated by {@link com.caucho.bam.Message @Message} with
   * a payload class matching the message payload.
   *
   * If no method is found, the message is ignored.
   *
   * @param to the SimpleActorStream's JID
   * @param from the sending actor's JID
   * @param payload the message payload
   */
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    _skeleton.message(this, to, from, payload);
  }

  /**
   * Dispatches a messageError to a matching method on
   * the SimpleActorStream.
   *
   * By default, messageError invokes a method
   * annotated by {@link com.caucho.bam.MessageError @MessageError} with
   * a payload class matching the messageError payload.
   *
   * If no method is found, the messageError is ignored.
   *
   * @param to the SimpleActorStream's JID
   * @param from the sending actor's JID
   * @param payload the message payload
   * @param error the message error
   */
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    _skeleton.messageError(this, to, from, payload, error);
  }

  //
  // RPC query
  //

  /**
   * Dispatches a queryGet to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryGet invokes a method
   * annotated by {@link com.caucho.bam.QueryGet @QueryGet} with
   * a payload class matching the queryGet payload.
   *
   * The {@link com.caucho.bam.QueryGet @QueryGet} method MUST
   * send either a queryResult or queryError as a response.
   *
   * If no method is found, queryGet sends a queryError response with
   * a feature-not-implemented error.
   *
   * @param id a correlation id to match the result or error
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void queryGet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    _skeleton.queryGet(this, getLinkStream(), id, to, from, payload);
  }

  /**
   * Dispatches a querySet to a matching method on
   * the SimpleActorStream.
   *
   * By default, querySet invokes a method
   * annotated by {@link com.caucho.bam.QuerySet @QuerySet} with
   * a payload class matching the querySet payload.
   *
   * The {@link com.caucho.bam.QuerySet @QuerySet} method MUST
   * send either a queryResult or queryError as a response.
   *
   * If no method is found, querySet sends a queryError response with
   * a feature-not-implemented error.
   *
   * @param id a correlation id to match the result or error
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    _skeleton.querySet(this, getLinkStream(), id, to, from, payload);
  }

  /**
   * Dispatches a queryResult to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryResult invokes a method
   * annotated by {@link com.caucho.bam.QueryResult @QueryResult} with
   * a payload class matching the queryResult payload.
   *
   * If no method is found, queryResult ignores the packet.
   *
   * @param id the correlation id from the original query
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   */
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    _skeleton.queryResult(this, id, to, from, payload);
  }

  /**
   * Dispatches a queryError to a matching method on
   * the SimpleActorStream.
   *
   * By default, queryError invokes a method
   * annotated by {@link com.caucho.bam.QueryError @QueryError} with
   * a payload class matching the queryError payload.
   *
   * If no method is found, queryError ignores the packet.
   *
   * @param id the correlation id from the original query
   * @param to the SimpleActorStream's JID
   * @param from the client actor's JID
   * @param payload the query payload
   * @param error the error information
   */
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    _skeleton.queryError(this, id, to, from, payload, error);
  }

  protected Skeleton getSkeleton()
  {
    return _skeleton;
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void messageFallback(String to, String from, Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " message ignored " + payload
               + " {from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void messageErrorFallback(String to,
                                      String from,
                                      Serializable payload,
                                      ActorError error)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " messageError ignored " + error + " " + payload
               + " {from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void queryGetFallback(long id,
                                  String to,
                                  String from,
                                  Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryGet not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }

    String msg;
    msg = (this + ": queryGet is not implemented for this payload:\n"
           + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    getLinkStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void querySetFallback(long id,
                                  String to,
                                  String from,
                                  Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " querySet not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }

    String msg;
    msg = (this + ": querySet is not implemented for this payload:\n"
           + "  " + payload + " {id:" + id + ", from:" + from + ", to:" + to + "}");

    ActorError error = new ActorError(ActorError.TYPE_CANCEL,
                                      ActorError.FEATURE_NOT_IMPLEMENTED,
                                      msg);

    getLinkStream().queryError(id, from, to, payload, error);
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void queryResultFallback(long id,
                                     String to,
                                     String from,
                                     Serializable payload)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryResult not implemented for " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }
  }
  
  /**
   * Fallback for messages which don't match the skeleton.
   */
  protected void queryErrorFallback(long id,
                                    String to,
                                    String from,
                                    Serializable payload,
                                    ActorError error)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " queryError ignored " + error + " " + payload
               + " {id: " + id + ", from: " + from + " to: " + to + "}");
    }
  }
  
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Close the stream
   */
  public void close()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
