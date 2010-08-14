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

package com.caucho.server.resin;

import com.caucho.naming.Jndi;
import com.caucho.server.thread.ResinThreadPoolExecutor;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.UserTransactionProxy;

/**
 * Initialization from EnvironmentLoader, split out of the kernel
 */
public class EnvInit {

  public EnvInit() throws Exception
  {
    init();
  }

  private void init() throws Exception
  {
    TransactionManagerImpl tm = TransactionManagerImpl.getInstance();
    UserTransactionProxy ut = UserTransactionProxy.getInstance();

    // server/16g0
    // Applications are incorrectly using TransactionManager
    // as an extended UserTransaction
    Jndi.bindDeep("java:comp/TransactionManager", tm);
    //TODO Is this alias used?
    Jndi.bindDeep("java:/TransactionManager", tm);
    Jndi.bindDeep("java:comp/TransactionSynchronizationRegistry",
                  tm.getSyncRegistry());
    Jndi.bindDeep("java:comp/UserTransaction", ut);
    Jndi.bindDeep("java:comp/ThreadPool", 
                  ResinThreadPoolExecutor.getThreadPool());
  }
}
