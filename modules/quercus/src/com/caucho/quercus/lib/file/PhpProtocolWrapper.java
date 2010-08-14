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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.io.IOException;

public class PhpProtocolWrapper extends ProtocolWrapper {
  private static final L10N L = new L10N(PhpProtocolWrapper.class);

  public PhpProtocolWrapper()
  {
  }

  public BinaryStream fopen(Env env, StringValue pathV, StringValue mode, 
                            LongValue options)
  {
    String path = pathV.toString();
    
    if (path.equals("php://output"))
      return new PhpBinaryOutput(env);
    else if (path.equals("php://input"))
      return new PhpBinaryInput(env);
    else if (path.equals("php://stdout"))
      return new PhpStdout();
    else if (path.equals("php://stderr"))
      return new PhpStderr();
    else if (path.equals("php://stdin"))
      return new PhpStdin(env);
    
    env.warning(L.l("{0} is an unsupported or unknown path for this protocol",
                    path));

    return null;
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

