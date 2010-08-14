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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es;

import com.caucho.util.IntMap;

import java.util.HashMap;

class ESArguments extends ESObject {
  static ESId CALLEE = ESId.intern("callee");
  static ESId ARGUMENTS = ESId.intern("arguments");
  static ESId LENGTH = ESId.intern("length");

  HashMap aliases = new HashMap();

  private ESArguments(ESId []formals, Call eval, int length)
    throws ESException
  {
    super("Arguments", Global.getGlobalProto().arrayProto);

    for (int i = 0; i < length; i++)
      super.put(ESString.create(i), eval.getArg(i, length), DONT_ENUM);
    
    for (int i = 0; i < formals.length; i++) {
      super.put(formals[i], eval.getArg(i, length), DONT_ENUM|DONT_DELETE);
      aliases.put(formals[i], ESString.create(i));
      aliases.put(ESString.create(i), formals[i]);
    }
    
    super.put(LENGTH, ESNumber.create(length), DONT_ENUM);
    super.put(CALLEE, eval.callee, DONT_ENUM);
  }

  static ESObject create(ESId []formals, Call eval, int length)
    throws ESException
  {
    ESArguments args = new ESArguments(formals, eval, length);
    
    args.put(ARGUMENTS, args, DONT_ENUM|DONT_DELETE);
    
    return args;
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    return ESArray.arrayToSource(this, map, isLoopPass);
  }

  public void setProperty(ESString key, ESBase value) throws Throwable
  {
    ESId alias = (ESId) aliases.get(key);

    super.setProperty(key, value);

    if (alias != null)
      super.setProperty(alias, value);
  }

  public void put(ESString key, ESBase value, int flags)
  {
    ESId alias = (ESId) aliases.get(key);

    super.put(key, value, flags);

    if (alias != null)
      super.put(alias, value, flags);
  }

  public ESBase delete(ESString key) throws Throwable
  {
    ESId alias = (ESId) aliases.get(key);

    if (alias == null)
      return super.delete(key);
    else {
      return ESBoolean.FALSE;
      // aliases.remove(key); // XXX: is this right? 10.1.8
    }
  } 

  protected ESArguments() {}
  protected ESObject dup() { return new ESArguments(); }

  protected void copy(HashMap refs, Object newObj)
  {
    ESArguments newArgs = (ESArguments) newObj;

    newArgs.aliases = aliases;

    super.copy(refs, newObj);
  }
}
