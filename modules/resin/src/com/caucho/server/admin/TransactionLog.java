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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.xalog.AbstractXALogManager;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionLog
{
  private static final Logger log = Logger.getLogger(TransactionLog.class.getName());
  private static final L10N L = new L10N(TransactionLog.class);

  private final TransactionManager _manager;

  private boolean _isEnable = true;
  private String _path;
  private AbstractXALogManager _xaLog;

  public TransactionLog(TransactionManager manager)
  {
    _manager = manager;
  }

  public void setPath(String path)
    throws IOException
  {
    _path = path;
  }

  public boolean isEnable()
  {
    return _isEnable;
  }

  public void setEnable(boolean enable)
  {
    _isEnable = enable;
  }

  public void start()
  {
    if (_path == null) {
      Server server = Server.getCurrent();

      if (server == null)
        return;

      String serverId = server.getServerId();

      if (serverId == null || serverId.length() == 0)
        serverId = "default";

      _path = "xa-" + serverId + ".log";
    }

    if (!_isEnable)
      return;

    try {
      Class cl = Class.forName("com.caucho.transaction.xalog.XALogManager");

      _xaLog = (AbstractXALogManager) cl.newInstance();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (_xaLog == null)
      throw new ConfigException(L.l("<transaction-log> requires Resin Professional.  See http://www.caucho.com for information and licensing."));

    try {
      if (_manager.getPath() != null)
        _xaLog.setPath(_manager.getPath().lookup(_path));
      else
        _xaLog.setPath(Vfs.lookup(_path));

      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      tm.setXALogManager(_xaLog);

      _xaLog.start();
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }


  public void destroy()
  {
    AbstractXALogManager xaLog = _xaLog;
    _xaLog = null;

    if (xaLog != null) {
      try {
        xaLog.close();
      }
      catch (Exception ex) {
        log.log(Level.INFO, ex.toString(), ex);
      }
    }

  }
}
