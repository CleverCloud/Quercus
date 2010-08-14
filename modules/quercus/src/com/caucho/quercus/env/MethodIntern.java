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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Case-insensitive method mapping
 */
public final class MethodIntern
{
  private static final ConcurrentHashMap<String,StringValue> _internMap
    = new ConcurrentHashMap<String,StringValue>();

  public static StringValue intern(String name)
  {
    StringValue internName = _internMap.get(name);

    if (internName == null) {
      StringValue string = new CompiledConstStringValue(name);

      internName = _internMap.putIfAbsent(name, string);

      if (internName == null)
        internName = string;
    }

    return internName;
  }
}
