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

import com.caucho.server.snmp.SnmpRuntimeException;

/*
 * Represents an ObjectIdentifier => SNMP value mapping.
 */
public class VarBindValue extends SequenceValue<SnmpValue>
{
  public VarBindValue(ObjectIdentifierValue name)
  {
    super();
    
    add(name);
    add(NullValue.NULL);
  }
  
  public VarBindValue(ObjectIdentifierValue name, SnmpValue value)
  {
    super();
    
    add(name);
    add(value);
  }
  
  public ObjectIdentifierValue getName()
  {
    return (ObjectIdentifierValue) get(0);
  }
  
  public SnmpValue getValue()
  {
    return get(1);
  }
  
  @Override
  public void add(SnmpValue value)
  {
    if (size() == 0 && value.getType() == SnmpValue.OBJECT_IDENTIFIER)
      super.add(value);
    else if (size() == 1)
      super.add(value);
    else
      throw new SnmpRuntimeException("adding prohibited because of type");
  }
}
