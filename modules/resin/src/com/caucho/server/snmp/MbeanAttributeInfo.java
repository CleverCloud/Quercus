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

package com.caucho.server.snmp;

import com.caucho.server.snmp.types.SnmpValue;

public class MbeanAttributeInfo
{
  private String _owner;
  private String _name;
  
  private String _oid;
  private int _type = SnmpValue.INTEGER;
  
  public static final int INTEGER = SnmpValue.INTEGER;
  public static final int OCTET_STRING = SnmpValue.OCTET_STRING;
  public static final int GAUGE = SnmpValue.GAUGE;
  public static final int COUNTER = SnmpValue.COUNTER;
  public static final int TIME_TICKS = SnmpValue.TIME_TICKS;
  
  public MbeanAttributeInfo()
  {
  }
  
  public MbeanAttributeInfo(String owner, String name)
  {
    _owner = owner;
    _name = name;
  }
  
  public MbeanAttributeInfo(String owner, String name, int type)
  {
    _owner = owner;
    _name = name;
    _type = type;
  }

  public String getOwner()
  {
    return _owner;
  }
  
  public void setOwner(String owner)
  {
    _owner = owner;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public void setName(String name)
  {
    _name = name;
  }
  
  public int getType()
  {
    return _type;
  }
  
  public void setType(int type)
  {
    _type = type;
  }
  
  public String getOid()
  {
    return _oid;
  }
  
  public void setOid(String oid)
  {
    _oid = oid;
  }
  
  public boolean equals(Object obj)
  {
    if (obj instanceof MbeanAttributeInfo) {
      MbeanAttributeInfo attr = (MbeanAttributeInfo) obj;
      
      return _owner.equals(attr.getOwner())
             && _name.equals(attr.getName());
    }
    else
      return false;
  }
  
  public String toString()
  {
    return "MbeanAttribute[" + _owner + ","
                             + _name + ","
                             + _oid
                             + SnmpValue.typeName(_type) + "]";
  }
}
