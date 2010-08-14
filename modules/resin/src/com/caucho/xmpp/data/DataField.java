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
 * element field {
 *    attribute label?,
 *    attribute type?,
 *    attribute var?,
 *
 *    desc?,
 *    required?,
 *    value*,
 *    option*,
 * }
 *
 * element option {
 *   attribute label?,
 *
 *   value*
 * }
 *
 * element reported {
 *   field+
 * }
 *
 * element value {
 *   string
 * }
 * </pre></code>
 */
public class DataField implements java.io.Serializable {
  private String _label;
  
  // "boolean", "fixed", "hidden",
  // "jid-multi", "jid-single",
  // "list-multi", "list-single",
  // "text-multi", "text-private", "text-single"
  private String _type = "text-single";
  private String _var;

  private String _desc;
  private boolean _isRequired;
  private DataValue []_value;
  private DataOption []_option;
  
  private DataField()
  {
  }
  
  public DataField(String type)
  {
    _type = type;
  }
  
  public DataField(String type, String var)
  {
    _type = type;
    _var = var;
  }
  
  public DataField(String type, String var, String label)
  {
    _type = type;
    _var = var;
    _label = label;
  }

  public String getType()
  {
    return _type;
  }

  public String getVar()
  {
    return _var;
  }

  public String getLabel()
  {
    return _label;
  }

  public String getDesc()
  {
    return _desc;
  }

  public void setDesc(String desc)
  {
    _desc = desc;
  }

  public boolean isRequired()
  {
    return _isRequired;
  }

  public void setRequired(boolean isRequired)
  {
    _isRequired = isRequired;
  }
  
  public DataOption []getOption()
  {
    return _option;
  }
  
  public void setOption(DataOption []option)
  {
    _option = option;
  }
  
  public void setOptionList(ArrayList<DataOption> optionList)
  {
    if (optionList != null && optionList.size() > 0) {
      _option = new DataOption[optionList.size()];
      optionList.toArray(_option);
    }
    else
      _option = null;
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
    sb.append("[type=").append(_type);

    if (_isRequired)
      sb.append(",required");

    if (_var != null)
      sb.append(",var=").append(_var);

    if (_label != null)
      sb.append(",label='").append(_label).append("'");

    if (_desc != null)
      sb.append(",desc='").append(_desc).append("'");

    if (_value != null) {
      for (int i = 0; i < _value.length; i++) {
        sb.append(",value=").append(_value[i]);
      }
    }

    if (_option != null) {
      for (int i = 0; i < _option.length; i++) {
        sb.append(",option=").append(_option[i]);
      }
    }

    sb.append("]");
    
    return sb.toString();
  }
}
