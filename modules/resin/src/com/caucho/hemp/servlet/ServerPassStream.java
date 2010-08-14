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

import java.io.Serializable;

import com.caucho.bam.AbstractActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;


/**
 * Handles the requests to the server from the link, dispatching requests to
 * the link service and the broker.
 */
public class ServerPassStream extends AbstractActorStream {
  private final ActorStream _linkStream; // string to the link
  private final ActorStream _linkService;

  private ActorStream _brokerStream;

  private String _jid;

  public ServerPassStream(ActorStream linkStream,
                          ActorStream linkService)
  {
    _linkStream = linkStream;
    _linkService = linkService;
  }

  @Override
  public String getJid()
  {
    return _jid;
  }

  public void setJid(String jid)
  {
    _jid = jid;
  }

  public void setBrokerStream(ActorStream brokerStream)
  {
    _brokerStream = brokerStream;
  }

  @Override
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  /**
   * Sends a message to the link service if 'to' is null, else send it to the broker.
   */
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    if (to == null)
      _linkService.message(to, from, payload);
    else if (_brokerStream != null)
      _brokerStream.message(to, from, payload);
    else
      super.message(to, from, payload);
  }

  /**
   * Handles a message
   */
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           ActorError error)
  {
    if (to == null)
      _linkService.messageError(to, from, payload, error);
    else if (_brokerStream != null)
      _brokerStream.messageError(to, from, payload, error);
    else
      super.messageError(to, from, payload, error);
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
                       Serializable payload)
  {
    if (to == null)
      _linkService.queryGet(id, to, from, payload);
    else if (_brokerStream != null)
      _brokerStream.queryGet(id, to, from, payload);
    else
      super.queryGet(id, to, from, payload);
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
                       Serializable payload)
  {
    if (to == null)
      _linkService.querySet(id, to, from, payload);
    else if (_brokerStream != null)
      _brokerStream.querySet(id, to, from, payload);
    else
      super.querySet(id, to, from, payload);
  }

  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (to == null)
      _linkService.queryResult(id, to, from, payload);
    else if (_brokerStream != null)
      _brokerStream.queryResult(id, to, from, payload);
    else
      super.queryResult(id, to, from, payload);
  }

  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         ActorError error)
  {
    if (to == null)
      _linkService.queryError(id, to, from, payload, error);
    else if (_brokerStream != null)
      _brokerStream.queryError(id, to, from, payload, error);
    else
      super.queryError(id, to, from, payload, error);
  }

  public boolean isClosed()
  {
    return _brokerStream == null;
  }

  public void close()
  {
    _brokerStream = null;
 }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "," + _linkService + "]";
  }
}
