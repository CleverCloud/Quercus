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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ResourceType;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * mysqli object oriented API facade
 * returns true for is_resource()
 */
@ResourceType("mysql link")
public class MysqliResource extends Mysqli {
  private static final Logger log = Logger
    .getLogger(MysqliResource.class.getName());
  private static final L10N L = new L10N(MysqliResource.class);

  /**
    * This is the constructor for the mysqli class.
    * It can be invoked by PHP or and by Java code.
    */
  public MysqliResource(Env env,
                        @Optional("localhost") StringValue host,
                        @Optional StringValue user,
                        @Optional StringValue password,
                        @Optional String db,
                        @Optional("3306") int port,
                        @Optional StringValue socket)
  {
    super(env, host, user, password, db, port, socket);
  }

  /**
   * This constructor can only be invoked by other method
   * implementations in the mysql and mysqli modules. It
   * accepts String arguments and supports additional
   * arguments not available in the mysqli constructor.
   */

  MysqliResource(Env env,
                 String host,
                 String user,
                 String password,
                 String db,
                 int port,
                 String socket,
                 int flags,
                 String driver,
                 String url,
                 boolean isNewLink)
  {
    super(
      env, 
      host, user, password,
      db, port, socket, flags, driver, url, isNewLink);
  }

  protected MysqliResource(Env env)
  {
    super(env);
  }
}
