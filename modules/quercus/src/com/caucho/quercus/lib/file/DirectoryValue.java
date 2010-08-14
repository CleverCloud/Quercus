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

package com.caucho.quercus.lib.file;

import java.io.IOException;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.Path;

/**
 * Represents a PHP directory listing
 */
public class DirectoryValue extends ResourceValue {
  private Env _env;
  private Path _path;
  private String []_list;
  private int _index;

  protected DirectoryValue(Env env)
  {
    _env = env;
  }

  public DirectoryValue(Env env, Path path)
    throws IOException
  {
    _env = env;
    _path = path;

    _list = path.list();
  }

  /**
   * Returns the next value.
   */
  public Value readdir()
  {
    if (_index < _list.length)
      return _env.createString(_list[_index++]);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Rewinds the directory
   */
  public void rewinddir()
  {
    _index = 0;
  }

  /**
   * Calls the given method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash)
  {
    String method = methodName.toString();
    
    if ("read".equals(method))
      return readdir();
    else if ("rewind".equals(method)) {
      rewinddir();

      return BooleanValue.TRUE;
    }
    else if ("close".equals(method)) {
      close();

      return BooleanValue.TRUE;
    }
    else
      return super.callMethod(env, methodName, hash);
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "Directory[" + _path + "]";
  }
}

