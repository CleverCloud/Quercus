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

package com.caucho.server.snmp.types;

/*
 * Holds a list of VarBinds.
 */
public class VarBindListValue extends SequenceValue<VarBindValue>
{
  public VarBindListValue()
  {
    super();
  }
  
  @Override
  public void add(VarBindValue value)
  {
    super.add(value);
  }
  
  public void addVarBind(VarBindValue varBind)
  {
    super.add(varBind);
  }
  
  public VarBindValue getVarBind(int index)
  {
    return (VarBindValue) get(index);
  }
  
  public ObjectIdentifierValue []getNames()
  {
    ObjectIdentifierValue []oidArray = new ObjectIdentifierValue[size()];
    
    for (int i = 0; i < oidArray.length; i++) {
      oidArray[i] = get(i).getName();
    }
    
    return oidArray;
  }
  
  public SnmpValue []getValue()
  {
    SnmpValue []array = new SnmpValue[size()];
    
    for (int i = 0; i < array.length; i++) {
      array[i] = get(i).getValue();
    }
    
    return array;
  }
}
