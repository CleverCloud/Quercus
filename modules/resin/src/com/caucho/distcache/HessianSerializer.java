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

package com.caucho.distcache;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianDebugOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Custom serialization for the cache
 */
public class HessianSerializer implements CacheSerializer
{
  private static final Logger log
    = Logger.getLogger(HessianSerializer.class.getName());
  
  /**
   * Serialize the data
   */
  @Override
  public void serialize(Object value, OutputStream os)
    throws IOException
  {
    if (log.isLoggable(Level.FINEST))
      os = new HessianDebugOutputStream(os, log, Level.FINEST);

    Hessian2Output hOut = new Hessian2Output(os);

    hOut.writeObject(value);

    hOut.close();
  }
  
  /**
   * Deserialize the data
   */
  @Override
  public Object deserialize(InputStream is)
    throws IOException
  {
    if (log.isLoggable(Level.FINEST))
      is = new HessianDebugInputStream(is, log, Level.FINEST);

    Hessian2Input hIn = new Hessian2Input(is);

    Object value = hIn.readObject();

    hIn.close();
    
    return value;
  }
}
