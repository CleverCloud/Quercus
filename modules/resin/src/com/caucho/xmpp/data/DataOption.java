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

package com.caucho.xmpp.data;

import java.util.*;

/**
 * data forms
 *
 * XEP-0004: http://www.xmpp.org/extensions/xep-0004.html
 *
 * <code><pre>
 * namespace = jabber:x:data
 *
 * element option {
 *   attribute label?,
 *
 *   value*
 * }
 *
 * element value {
 *   string
 * }
 * </pre></code>
 */
public class DataOption implements java.io.Serializable {
  private String _label;
  
  private DataValue []_value;
  
  public DataOption()
  {
  }
  
  public DataOption(String label)
  {
    _label = label;
  }
  
  public DataOption(String label, DataValue []value)
  {
    _label = label;
    _value = value;
  }

  public String getLabel()
  {
    return _label;
  }
  
  public DataValue []getValue()
  {
    return _value;
  }
  
  public void setValue(DataValue []value)
  {
    _value = value;
  }
  
  public void setValueList(ArrayList<DataValue> valueList)
  {
    if (valueList != null && valueList.size() > 0) {
      _value = new DataValue[valueList.size()];
      valueList.toArray(_value);
    }
    else
      _value = null;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_label != null)
      sb.append("label=").append(_label);

    if (_value != null) {
      for (int i = 0; i < _value.length; i++) {
        sb.append(",value=").append(_value[i]);
      }
    }

    sb.append("]");
    
    return sb.toString();
  }
}
