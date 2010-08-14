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

import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorClientFactory;
import com.caucho.bam.SimpleActorClient;
import com.caucho.util.L10N;

public class LocalActorFactoryImpl implements ActorClientFactory
{
  private static final L10N L = new L10N(LocalActorFactoryImpl.class);

  private HempBroker _broker = HempBroker.getCurrent();

  public LocalActorFactoryImpl()
  {
    if (_broker == null)
      throw new NullPointerException(L.l("no local broker is available"));
  }

  @Override
  public ActorClient createClient(String uid, String resource)
  {
    SimpleActorClient client = new SimpleActorClient(_broker, uid, resource);
    
    return client;
  }
}
