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


/**
 * Abstract implementation of a BAM actor.
 */
public class AbstractActor implements Actor
{
  private ActorStream _actorStream;
  private ActorStream _linkStream;

  private String _jid;

  /**
   * The jid to the {@link com.caucho.bam.Broker} for addressing
   * from other Actors.
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  /**
   * The jid to the {@link com.caucho.bam.Broker} for addressing
   * from other Actors.
   */
  public String getJid()
  {
    return _jid;
  }

  /**
   * Returns the stream to the Actor from the link so
   * messages from other Actors can be delivered.
   */
  public ActorStream getActorStream()
  {
    return _actorStream;
  }

  /**
   * The stream to the Actor from the link so
   * messages from other Actors can be delivered.
   */
  public void setActorStream(ActorStream actorStream)
  {
    _actorStream = actorStream;
  }

  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  public void setLinkStream(ActorStream linkStream)
  {
    _linkStream = linkStream;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
