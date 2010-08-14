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

package com.caucho.hemp.broker;

import com.caucho.bam.ActorMissingEvent;
import com.caucho.bam.Broker;
import com.caucho.bam.BrokerListener;

/**
 * An ActorManager dynamically registers {@link com.caucho.bam.Actor Actors}
 * with the {@link com.caucho.bam.Broker Broker} based on matching jids.
 * The ActorManager is registered with the Broker and waits for requests
 * for new or unknown jids.
 *
 * For example, an Instant Messaging system stores its users in a database
 * and only creates Actors for a users when they log on.  When a new
 * Actor logs on, the Broker asks its registered ActorManagers if they
 * manage the jid.
 */
public class AbstractActorManager implements BrokerListener
{
  private Broker _broker;
  
  /**
   * Sets the Broker the ActorManager is registered with.
   */
  public void setBroker(Broker broker)
  {
    _broker = broker;
  }
  
  
  /**
   * Returns the Broker the ActorManager is registered with.
   */
  public Broker getBroker()
  {
    return _broker;
  }
  /**
   * Called by the {@link com.caucho.bam.Broker} to request creation
   * of an {@link com.caucho.bam.Actor} with the given jid.  If the
   * ActorManager supports the jid, it will create an Actor, register
   * it with the Broker, and return true.
   *
   * @param jid the requested jid from the Broker
   *
   * @return true if the ActorManager has registered an Agent with the Broker
   */
  public boolean startActor(String jid)
  {
    return true;
  }

  /**
   * Called by the {@link com.caucho.bam.Broker} to request removal
   * of an {@link com.caucho.bam.Actor} with the given jid.  If the
   * ActorManager supports the jid, it will unregister the Actor
   * from the Broker, and return true.
   *
   * @param jid the requested jid from the Broker
   *
   * @return true if the ActorManager has unregistered an Agent from the Broker
   */
  public boolean stopActor(String jid)
  {
    return false;
  }


  @Override
  public void hostMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void resourceMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void userMissing(ActorMissingEvent event)
  {
    // TODO Auto-generated method stub
    
  }
}
