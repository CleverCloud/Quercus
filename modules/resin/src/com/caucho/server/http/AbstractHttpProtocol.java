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

package com.caucho.server.http;

import com.caucho.config.ConfigException;
import com.caucho.network.listen.AbstractProtocol;
import com.caucho.server.cluster.Server;
import com.caucho.util.L10N;

/**
 * Abstract Protocol handling for HTTP requests.
 */
abstract public class AbstractHttpProtocol extends AbstractProtocol {
  private static final L10N L = new L10N(AbstractHttpProtocol.class);
  
  private Server _server;
  
  protected AbstractHttpProtocol()
  {
    _server = Server.getCurrent();
    
    if (_server == null)
      throw new ConfigException(L.l("{0} needs an active Resin Server.",
                                    getClass().getName()));
  }

  /**
   * Returns the active server.
   */
  public Server getServer()
  {
    return _server;
  }
}
