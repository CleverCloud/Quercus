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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusRuntimeException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP session callback.
 */
public class SessionCallback extends Value {
  private static final Logger log
    = Logger.getLogger(SessionCallback.class.getName());

  private Callable _open;
  private Callable _close;
  private Callable _read;
  private Callable _write;
  private Callable _destroy;
  private Callable _gc;

  public SessionCallback(Callable open,
                         Callable close,
                         Callable read,
                         Callable write,
                         Callable destroy,
                         Callable gc)
  {
    _open = open;
    _close = close;
    _read = read;
    _write = write;
    _destroy = destroy;
    _gc = gc;
    
    if (open == null)
      throw new NullPointerException("open cannot be null");
  }

  public void open(Env env, String savePath, String sessionName)
  {
    _open.call(env, env.createString(savePath), env.createString(sessionName));
  }

  public StringValue read(Env env, String id)
  {
    try {
      Value value = _read.call(env, env.createString(id));

      if (value instanceof StringValue)
        return (StringValue) value;
      else
        return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new QuercusRuntimeException(e);
    }
  }

  public void write(Env env, String id, String value)
  {
    try {
      _write.call(env, env.createString(id), env.createString(value));
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void destroy(Env env, String id)
  {
    try {
      _destroy.call(env, env.createString(id));
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void close(Env env)
  {
    try {
      _close.call(env);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
}
