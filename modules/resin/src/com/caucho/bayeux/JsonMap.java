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

package com.caucho.bayeux;

import java.io.*;
import java.util.*;

class JsonMap implements JsonObject {
  private final HashMap<String,JsonObject> _values = 
    new HashMap<String,JsonObject>();

  public void put(String name, JsonObject value)
  {
    _values.put(name, value);
  }

  public JsonObject get(String name)
  {
    return _values.get(name);
  }

  public int size()
  {
    return _values.size();
  }

  public void writeTo(PrintWriter out)
    throws IOException
  {
    out.print("{");

    for (Map.Entry<String,JsonObject> entry : _values.entrySet()) {
      out.print("\"" + entry.getKey() + "\":");
      writeTo(out);
      out.print(",");
    }

    out.print("}");
  }
}
