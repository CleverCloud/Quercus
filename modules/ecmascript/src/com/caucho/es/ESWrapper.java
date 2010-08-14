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

/**
 * JavaScript object
 */
class ESWrapper extends ESObject {
  ESBase value;

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    if (isLoopPass) {
      return value.toSource(map, isLoopPass);
    }

    Global resin = Global.getGlobalProto();

    if (prototype == resin.boolProto)
      return ESString.create("new Boolean(" + 
                             value.toSource(map, isLoopPass) + ")");
    else if (prototype == resin.numProto)
      return ESString.create("new Number(" + 
                             value.toSource(map, isLoopPass) + ")");
    else if (prototype == resin.stringProto)
      return ESString.create("new String(" + 
                             value.toSource(map, isLoopPass) + ")");
    else
      return ESString.create("#unknown#");
  }

  protected ESWrapper() {}
  protected ESObject dup() { 
    ESWrapper newWrapper = new ESWrapper(); 
    newWrapper.value = value; // needs clone?
    return newWrapper;
  }

  public ESWrapper(String className, ESBase proto, ESBase value)
  {
    super(className, proto);

    if (value instanceof ESObject)
      throw new RuntimeException("cannot wrap object");

    this.value = value;
  }
}
