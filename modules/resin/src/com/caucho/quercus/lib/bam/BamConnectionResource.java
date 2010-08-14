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

package com.caucho.quercus.lib.bam;

import com.caucho.bam.ActorClient;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BAM connection resource.
 */
public class BamConnectionResource 
  implements EnvCleanup
{
  private static final Logger log
    = Logger.getLogger(BamConnectionResource.class.getName());

  private static final L10N L = new L10N(BamConnectionResource.class);

  private ActorClient _connection;

  public BamConnectionResource(ActorClient connection)
  {
    _connection = connection;
  }

  public ActorClient getConnection()
  {
    return _connection;
  }

  public void cleanup()
  {
    if (_connection != null) {
      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("ActorClientResource.cleanup(): closing {0}", 
                       _connection));

      _connection.close();
      _connection = null;
    }
  }
}
