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

import com.caucho.quercus.marshal.Marshal;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.IdentityHashMap;

/**
 * Represents a PHP long value.
 */
@SuppressWarnings("serial")
public class LongCacheValue extends LongValue
{
  private transient LongValue _prev;
  private transient final LongValue _next;
  
  public LongCacheValue(long value, LongValue next)
  {
    super(value);
    
    _next = next;
  }
  
  void setPrev(LongValue prev)
  {
    _prev = prev;
  }

  /**
   * Returns the next integer
   */
  @Override
  public Value addOne()
  {
    return _next;
  }

  /**
   * Returns the previous integer
   */
  @Override
  public Value subOne()
  {
    return _prev;
  }

  /**
   * Pre-increment the following value.
   */
  @Override
  public Value preincr()
  {
    return _next;
  }

  /**
   * Pre-increment the following value.
   */
  @Override
  public Value predecr()
  {
    return _prev;
  }

  /**
   * Post-increment the following value.
   */
  @Override
  public Value postincr()
  {
    return _next;
  }

  /**
   * Post-decrement the following value.
   */
  @Override
  public Value postdecr()
  {
    return _prev;
  }

  /**
   * serialization override
   */
  private Object writeReplace()
  {
    return new LongValue(toLong());
  }
}
