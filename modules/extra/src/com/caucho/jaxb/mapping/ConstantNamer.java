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

package com.caucho.jaxb.mapping;

import java.util.HashMap;
import javax.xml.namespace.QName;

public class ConstantNamer implements Namer {
  private static final HashMap<QName,ConstantNamer> _namers = 
    new HashMap<QName,ConstantNamer>();

  private final QName _name;

  private ConstantNamer(QName name)
  {
    _name = name;
  }

  public static ConstantNamer getConstantNamer(QName qname)
  {
    ConstantNamer namer = _namers.get(qname);

    if (namer == null) {
      namer = new ConstantNamer(qname);
      _namers.put(qname, namer);
    }

    return namer;
  }

  public QName getQName(Object obj) 
  {
    return _name;
  }

  public String toString()
  {
    return "ConstantNamer[" + _name + "]";
  }
}
