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
 * @author Emil Ong
 */

package com.caucho.cxf;

import org.apache.cxf.BusException;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;

// This class is a workaround for what I assume is a bug in CXF that causes
// Jetty to be reregistered even after it is deregistered as a Transport in
// some cases.  This wraps the DestinationFactoryManager after the proper
// ServletTransport is registered and ignores changes thereafter.
class ReadOnlyDestinationFactoryManager 
  implements DestinationFactoryManager
{
  private final DestinationFactoryManager _manager;

  public ReadOnlyDestinationFactoryManager(DestinationFactoryManager manager)
  {
    _manager = manager;
  }

  public void registerDestinationFactory(String name, 
                                         DestinationFactory factory)
  {
  }

  public void deregisterDestinationFactory(String name)
  {
  }
    
  public DestinationFactory getDestinationFactory(String name) 
    throws BusException
  {
    return _manager.getDestinationFactory(name);
  }

  public DestinationFactory getDestinationFactoryForUri(String uri)
  {
    return _manager.getDestinationFactoryForUri(uri);
  }
}
