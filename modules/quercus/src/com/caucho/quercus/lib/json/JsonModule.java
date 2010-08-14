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

package com.caucho.quercus.lib.json;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.simplexml.SimpleXMLElement;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class JsonModule
    extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(JsonModule.class.getName());
  private static final L10N L = new L10N(JsonModule.class);

  public String []getLoadedExtensions()
  {
    return new String[] { "json" };
  }

  /**
   * Returns a JSON-encoded String.
   *
   * JSON strings can be in any Unicode format (UTF-8, UTF-16, UTF-32).
   * Therefore need to pay special attention to multi-char characters.
   *
   * @param env
   * @param val to encode into json format
   * @return String JSON-encoded String
   */
  public StringValue json_encode(Env env, Value val)
  {
    StringValue sb = env.createUnicodeBuilder();

    val.jsonEncode(env, sb);
    
    return sb;
  }

  /**
   * Takes a JSON-encoded string and returns a PHP value.
   *
   * @param env
   * @param s JSON-encoded string.
   * @param assoc determines whether a generic PHP object or PHP associative
   *     array should be returned when decoding json objects.
   * @return decoded PHP value.
   */
  public Value json_decode(Env env,
                          StringValue s,
                          @Optional("false") boolean assoc)
  {
    if (s.length() == 0)
      return new ArrayValueImpl();
    
    return (new JsonDecoder()).jsonDecode(env, s, assoc);
  }

}
