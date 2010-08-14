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

import java.util.ArrayList;
import java.util.Iterator;

/*
 * SNMP Sequence type.
 */
public class SequenceValue<T extends SnmpValue> extends SnmpValue
{
  ArrayList<T> _list = new ArrayList<T>();
  
  public SequenceValue()
  {
  }
  
  public void add(T item)
  {
    _list.add(item);
  }
  
  public T get(int index)
  {
    return _list.get(index);
  }
  
  public int size()
  {
    return _list.size();
  }
  
  public Iterator<T> iterator()
  {
    return _list.iterator();
  }
  
  public void clear()
  {
    _list.clear();
  }
  
  @Override
  public int getType()
  {
    return SnmpValue.SEQUENCE;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    StringBuilder innerSb = new StringBuilder();
    
    for (SnmpValue obj: _list) {
      obj.toAsn1(innerSb);
    }
    
    header(sb, innerSb.length());

    sb.append(innerSb.toString());
  }
  
  public String toString()
  {
    String s = "Sequence[";
    
    for (int i = 0; i < _list.size(); i++) {
      if (i != 0)
        s += ",";
      
      s += _list.get(i).toString();
    }
    
    s += "]";
    
    return s;
  }
}
