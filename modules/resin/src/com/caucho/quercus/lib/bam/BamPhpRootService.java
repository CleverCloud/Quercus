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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.SimpleActor;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.xmpp.disco.DiscoInfoQuery;

import javax.annotation.PostConstruct;

/**
 * BAM actor spawns a new BamPhpActor when requested.
 **/
public class BamPhpRootService extends SimpleActor {
  private static final L10N L = new L10N(BamPhpActor.class);
  private static final Logger log
    = Logger.getLogger(BamPhpRootService.class.getName());

  private final HashMap<String,BamPhpActor> _actors = 
    new HashMap<String,BamPhpActor>();

  private Path _script;
  private String _encoding = "ISO-8859-1";

  public Path getScript()
  {
    return _script;
  }

  public void setScript(Path script)
  {
    _script = script;
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_script == null)
      throw new ConfigException(L.l("script path not specified"));

    // super.init();
  }
/*
  @Override
  public boolean startChild(String jid)
  {
    if (log.isLoggable(Level.FINE)) 
      log.fine(L.l("{0}.startActor({1})", toString(), jid));

    BamPhpActor actor = _actors.get(jid);

    if (actor == null) {
      actor = new BamPhpActor(_script, _encoding);
      actor.setJid(jid);

      //InjectManager container = InjectManager.getCurrent();
      //container.injectObject(actor);

      _actors.put(jid, actor);
    }

    return true;
  }
  */

  public String toString()
  {
    return "BamPhpRootService[jid=" + getJid() + 
                            ",script=" + _script + "]";
  }
}
