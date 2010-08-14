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

import java.io.Serializable;

import com.caucho.bam.AbstractActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;

/**
 * xmpp client to broker
 */
public class XmppWriter extends AbstractActorStream
{
  private XmppWriterImpl _out;

  XmppWriter(XmppWriterImpl out)
  {
    _out = out;
  }

  @Override
  public ActorStream getLinkStream()
  {
    return null;
  }

  public String getJid()
  {
    return null;
  }

  public void message(String to, String from, Serializable value)
  {
    _out.message(to, from, value);
  }

  public void messageError(String to, String from, Serializable value,
                           ActorError error)
  {
    _out.messageError(to, from, value, error);
  }

  public void queryGet(long id, String to, String from, Serializable value)
  {
    String sid = String.valueOf(id);
    
    _out.sendQuery(sid, to, from, value, "get", null);
  }

  public void querySet(long id, String to, String from, Serializable value)
  {
    String sid = String.valueOf(id);
    
    _out.sendQuery(sid, to, from, value, "set", null);
  }

  public void queryResult(long id, String to, String from, Serializable value)
  {
    String sid = String.valueOf(id);
    
    _out.sendQuery(sid, to, from, value, "result", null);
  }

  public void queryError(long id, String to, String from, Serializable value,
                         ActorError error)
  {
    String sid = String.valueOf(id);
    
    _out.sendQuery(sid, to, from, value, "error", error);
  }

  public void presence(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "presence", null);
  }

  public void presenceProbe(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "probe", null);
  }

  public void presenceUnavailable(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "unavailable", null);
  }

  public void presenceSubscribe(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "subscribe", null);
  }

  public void presenceSubscribed(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "subscribed", null);
  }

  public void presenceUnsubscribe(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "unsubscribe", null);
  }

  public void presenceUnsubscribed(String to, String from, Serializable value)
  {
    _out.sendPresence(to, from, value, "unsubscribed", null);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + "]";
  }
}
