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

/*
 * Mapping from from SNMP objects to MBeans.
 */
public class Oid
{
  private String _name;
  
  private String _mbean;
  private String _attribute;
  
  private Object _value;

  // Snmp type for this mbean attribute
  private int _type = SnmpValue.OCTET_STRING;

  public Oid()
  {
  }
  
  public Oid(String name, String mbean, String attribute, int type)
  {
    _name = name;
    _mbean = mbean;
    _attribute = attribute;
    _type = type;
  }
  
  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public String getMbean()
  {
    return _mbean;
  }

  public void setMbean(String mbean)
  {
    _mbean = mbean;
  }

  public void setAttribute(String attribute)
  {
    _attribute = attribute;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  public int getType()
  {
    return _type;
  }

  public void setType(int type)
  {
    _type = type;
  }
  
  public Object getValue()
  {
    return _value;
  }
  
  public void setValue(Object value)
  {
    _value = value;
  }
  
  public void setIntegerValue(Integer value)
  {
    _value = value;
    
    _type = SnmpValue.INTEGER;
  }
  
  public void setOctetStringValue(String value)
  {
    _value = value;
    
    _type = SnmpValue.OCTET_STRING;
  }
  
  public void setGaugeValue(Long value)
  {
    _value = value;
    
    _type =  SnmpValue.GAUGE;
  }
  
  public void setCounterValue(Long value)
  {
    _value = value;
    
    _type = SnmpValue.COUNTER;
  }
  
  public void setTimeTicksValue(Long value)
  {
    _value = value;
    
    _type = SnmpValue.TIME_TICKS;
  }
  
  public void setIpAddressValue(String value)
  {
    _value = value;
    
    _type = SnmpValue.IP_ADDRESS;
  }

  public boolean equals(Object obj)
  {
    return obj instanceof Oid
      && ((Oid) obj).getName().equals(_name);
  }
  
  public String toString()
  {
    return "Oid[" + _name + ","
                  + _mbean + ","
                  + _attribute + ","
                  + _type + "]";
  }
}