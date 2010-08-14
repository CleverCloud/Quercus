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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.UrlModule;
import com.caucho.quercus.lib.zlib.ZlibModule;
import com.caucho.util.L10N;

import java.util.logging.Logger;

public class ZlibProtocolWrapper extends ProtocolWrapper {
  private static final Logger log
    = Logger.getLogger(ZlibProtocolWrapper.class.getName());
  private static final L10N L = new L10N(ZlibProtocolWrapper.class);

  public ZlibProtocolWrapper()
  {
  }

  public BinaryStream fopen(Env env, StringValue path, StringValue mode, 
                            LongValue options)
  {
    boolean useIncludePath = 
      (options.toLong() & StreamModule.STREAM_USE_PATH) != 0;

    Value pathComponent
      = UrlModule.parse_url(env, path, UrlModule.PHP_URL_PATH);
    
    if (! pathComponent.isset()) {
      log.info(L.l("no path component found in '{0}'", path.toString()));
      return null;
    }

    return ZlibModule.gzopen(env, pathComponent.toStringValue(),
                             mode.toString(),
                             useIncludePath);
  }

  public Value opendir(Env env, StringValue path, LongValue flags)
  {
    env.warning(L.l("opendir not supported by protocol"));

    return BooleanValue.FALSE;
  }

  public boolean unlink(Env env, StringValue path)
  {
    env.warning(L.l("unlink not supported by protocol"));

    return false;
  }

  public boolean rename(Env env, StringValue path_from, StringValue path_to)
  {
    env.warning(L.l("rename not supported by protocol"));

    return false;
  }

  public boolean mkdir(Env env, 
                       StringValue path, LongValue mode, LongValue options)
  {
    env.warning(L.l("mkdir not supported by protocol"));

    return false;
  }

  public boolean rmdir(Env env, StringValue path, LongValue options)
  {
    env.warning(L.l("rmdir not supported by protocol"));

    return false;
  }

  public Value url_stat(Env env, StringValue path, LongValue flags)
  {
    env.warning(L.l("stat not supported by protocol"));

    return BooleanValue.FALSE;
  }


}

