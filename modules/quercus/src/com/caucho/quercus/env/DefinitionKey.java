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

import com.caucho.quercus.page.QuercusPage;

import java.lang.ref.WeakReference;

/**
 * Key for caching function definitions
 */
public final class DefinitionKey {
  // crc of the current definition
  private final long _crc;

  // the including page
  private final WeakReference<QuercusPage> _includePageRef;

  DefinitionKey(long crc, QuercusPage includePage)
  {
    _crc = crc;
    _includePageRef = new WeakReference<QuercusPage>(includePage);
  }

  public int hashCode()
  {
    return (int) _crc;
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof DefinitionKey))
      return false;

    DefinitionKey key = (DefinitionKey) o;

    QuercusPage page = _includePageRef.get();
    QuercusPage keyPage = key._includePageRef.get();

    if (page == null || keyPage == null)
      return false;
    
    return (_crc == key._crc && page.equals(keyPage));
  }

  public String toString()
  {
    QuercusPage page = _includePageRef.get();
    
    return "DefinitionKey[" + _crc + ", " + page + "]";
  }
}

