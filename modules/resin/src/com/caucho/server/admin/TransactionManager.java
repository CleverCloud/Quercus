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
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.server.resin.Resin;

import java.util.logging.Logger;

/**
 * Configures the transaction manager.
 */
public class TransactionManager
{
  private static L10N L = new L10N(TransactionManager.class);
  private static Logger log
    = Logger.getLogger(TransactionManager.class.getName());

  private final Management _management;
  private final Resin _resin;
  private final Path _path;

  private TransactionLog _transactionLog;

  public TransactionManager(Management management)
  {
    _management = management;
    _resin = null;
    _path = null;
  }

  @Deprecated
  public TransactionManager(Resin resin)
  {
    _management = null;
    _resin = resin;
    _path = null;
  }

  @Deprecated
  public TransactionManager(Path path)
  {
    _management = null;
    _resin = null;
    _path = path;
  }

  public Path getPath()
  {
    if (_resin != null)
      return _resin.getResinDataDirectory();
    else if (_path != null)
      return _path;
    else
      return Vfs.lookup("resin-data");
  }

  /**
   * Configures the xa log.
   */
  public TransactionLog createTransactionLog()
    throws ConfigException
  {
    if (_transactionLog == null)
      _transactionLog =  new TransactionLog(this);
    
    return _transactionLog;
  }

  /**
   * Initializes the XA manager.
   */
  public void start()
    throws ConfigException
  {
    if (_transactionLog != null)
      _transactionLog.start();
  }

  public void destroy()
  {
    TransactionLog transactionLog = _transactionLog;
    _transactionLog = null;

    if (transactionLog != null)
      transactionLog.destroy();
  }
}
