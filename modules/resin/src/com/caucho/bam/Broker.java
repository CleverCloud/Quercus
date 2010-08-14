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
 * Broker is the hub which routes messages to actors.
 */
public interface Broker
{
  /**
   * Returns the broker's jid, i.e. the virtual host domain name.
   */
  public String getJid();
  
  /**
   * Returns the stream to the broker
   */
  public ActorStream getBrokerStream();
  
  /**
   * Adds an actor.
   */
  public void addActor(ActorStream actorStream);
  
  /**
   * Removes an actor.
   */
  public void removeActor(ActorStream actorStream);
  
  /**
   * Registers the client under a unique id. The
   * resource is only a suggestion; the broker may
   * return a different resource id.
   * 
   * @param clientStream the stream to the client
   * @param uid the client's uid
   * @param resource the suggested resource for the jid
   * @return the generated jid
   */
  public String createClient(ActorStream clientStream,
                             String uid,
                             String resource);
  
  /**
   * Registers an listener for broker events, e.g. a missing actor.
   */
  public void addBrokerListener(BrokerListener listener);   
  
  /**
   * Returns true if the broker has been closed
   */
  public boolean isClosed();  
}
