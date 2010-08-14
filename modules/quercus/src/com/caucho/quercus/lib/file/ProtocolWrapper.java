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
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;

public class ProtocolWrapper {
  private QuercusClass _qClass;

  protected ProtocolWrapper()
  {
  }

  public ProtocolWrapper(QuercusClass qClass)
  {
    _qClass = qClass;
  }

  public BinaryStream fopen(Env env, StringValue path, StringValue mode, 
                            LongValue options)
  {
    return new WrappedStream(env, _qClass, path, mode, options);
  }

  public Value opendir(Env env, StringValue path, LongValue flags)
  {
    WrappedDirectoryValue value = new WrappedDirectoryValue(env, _qClass);

    if (! value.opendir(path, flags))
      return BooleanValue.FALSE;
    else
      return value;
  }

  public boolean unlink(Env env, StringValue path)
  {
    AbstractFunction function = _qClass.getStaticFunction("unlink");

    if (function == null)
      return false;

    return function.call(env, path).toBoolean();
  }

  public boolean rename(Env env, StringValue path_from, StringValue path_to)
  {
    AbstractFunction function = _qClass.getStaticFunction("rename");

    if (function == null)
      return false;

    return function.call(env, path_from, path_to).toBoolean();
  }

  public boolean mkdir(Env env, 
                       StringValue path, LongValue mode, LongValue options)
  {
    AbstractFunction function = _qClass.getStaticFunction("mkdir");

    if (function == null)
      return false;

    return function.call(env, path, mode, options).toBoolean();
  }

  public boolean rmdir(Env env, StringValue path, LongValue options)
  {
    AbstractFunction function = _qClass.getStaticFunction("rmdir");

    if (function == null)
      return false;

    return function.call(env, path, options).toBoolean();
  }

  public Value url_stat(Env env, StringValue path, LongValue flags)
  {
    AbstractFunction function = _qClass.getStaticFunction("url_stat");

    if (function == null)
      return BooleanValue.FALSE;

    return function.call(env, path, flags);
  }

}
