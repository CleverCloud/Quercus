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

import java.util.Iterator;

class PropertyEnumeration implements Iterator {
  IntMap completed = new IntMap();
  ESObject object;
  int index;

  public Object next()
  {
    if (object == null)
      return null;

    if (object.propNames != null) {
      for (; index < object.propNames.length; index++) {
        if (object.propValues[index] != null &&
            (object.propFlags[index] & ESObject.DONT_ENUM) == 0 &&
            completed.get(object.propNames[index]) != 1) {
          index++;
          completed.put(object.propNames[index - 1], 1);

          return object.propNames[index - 1];
        }
      }
    }

    if (object.prototype instanceof ESObject) {
      object = (ESObject) object.prototype;
      index = 0;

      return next();
    }

    object = null;

    return null;
  }

  public boolean hasNext()
  {
    if (object == null)
      return false;

    if (object.propNames != null) {
      for (; index < object.propNames.length; index++) {
        if (object.propValues[index] != null &&
            (object.propFlags[index] & ESObject.DONT_ENUM) == 0 &&
            completed.get(object.propNames[index]) != 1) {
          break;
        }
      }

      if (index < object.propNames.length)
        return true;
    }

    if (object.prototype instanceof ESObject) {
      object = (ESObject) object.prototype;
      index = 0;

      return hasNext();
    }

    object = null;

    return false;
  }

  public void remove()
  {
    throw new RuntimeException();
  }

  PropertyEnumeration(ESObject object)
  {
    this.object = object;
    index = 0;
  }
}
