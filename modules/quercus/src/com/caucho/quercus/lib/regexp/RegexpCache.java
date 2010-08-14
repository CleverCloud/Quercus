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

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;

/**
 * FIFO cache of line-item regular expressions.
 */
public class RegexpCache
{
  private final Regexp []_cache;
  private int _head;
  
  private static final int MAX_SIZE = 4;
  
  public RegexpCache()
  {
    _cache = new Regexp[MAX_SIZE];
  }
  
  public Regexp get(Env env, StringValue str)
  {
    int head = _head;

    for (int i = 0; i < MAX_SIZE; i++) {
      Regexp regexp = _cache[(head + i) % MAX_SIZE];
      
      if (regexp == null)
        break;
      else {
        StringValue rawRegexp = regexp.getRawRegexp();
        
        if (rawRegexp == str || rawRegexp.equals(str))
          return regexp;
      }
    }
    
    Regexp regexp = RegexpModule.createRegexp(env, str);

    head = head - 1;
    
    if (head < 0)
      head = MAX_SIZE - 1;
    
    _cache[head] = regexp;
    _head = head;
    
    return regexp;
  }
}
