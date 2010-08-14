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

import java.util.EventListener;


/**
 * The BrokerListener listens for broker events. Typically, a listener
 * will listen for actor missing events.
 *
 * For example, an Instant Messaging system stores its users in a database
 * and only creates Actors for a users when they log on.  When a new
 * Actor logs on, the Broker asks its registered ActorManagers if they
 * manage the jid.
 */
public interface BrokerListener extends EventListener
{
  /**
   * Called by the {@link com.caucho.bam.Broker} when it cannot
   * find an actor in the current host.
   *
   * @param event the actor missing event
   */
  public void resourceMissing(ActorMissingEvent event);
  
  /**
   * Called by the {@link com.caucho.bam.Broker} when it cannot
   * find an actor in the current host.
   *
   * @param event the actor missing event
   */
  public void userMissing(ActorMissingEvent event);
  
  /**
   * Called by the {@link com.caucho.bam.Broker} when it cannot
   * find an host for the actor.
   *
   * @param event the host missing event
   */
  public void hostMissing(ActorMissingEvent event);
}
