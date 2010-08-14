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

import com.caucho.xmpp.data.DataField;
import java.util.*;

/**
 * data forms
 *
 * XEP-0004: http://www.xmpp.org/extensions/xep-0004.html
 *
 * <code><pre>
 * namespace = jabber:x:data
 *
 * element x {
 *   attribute type,
 *
 *   instructions*,
 *   title?,
 *   field*,
 *   reported?,
 *   item*
 * }
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
 * element item {
 *   field+
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
public class DataForm implements java.io.Serializable {
  // "cancel", "form", "result", "submit"
  private String _type;

  private String _title;

  private DataInstructions []_instructions;
  private DataField []_field;
  private DataReported _reported;
  private DataItem []_item;
  
  private DataForm()
  {
  }
  
  public DataForm(String type)
  {
    _type = type;
  }

  public String getType()
  {
    return _type;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public String getTitle()
  {
    return _title;
  }
  
  public DataField []getField()
  {
    return _field;
  }
  
  public void setField(DataField []field)
  {
    _field = field;
  }
  
  public void setFieldList(ArrayList<DataField> fieldList)
  {
    if (fieldList != null && fieldList.size() > 0) {
      _field = new DataField[fieldList.size()];
      fieldList.toArray(_field);
    }
    else
      _field = null;
  }

  public DataReported getReported()
  {
    return _reported;
  }

  public void setReported(DataReported reported)
  {
    _reported = reported;
  }
  
  public DataInstructions []getInstructions()
  {
    return _instructions;
  }
  
  public void setInstructions(DataInstructions []instructions)
  {
    _instructions = instructions;
  }
  
  public void setInstructionsList(ArrayList<DataInstructions> instructionsList)
  {
    if (instructionsList != null && instructionsList.size() > 0) {
      _instructions = new DataInstructions[instructionsList.size()];
      instructionsList.toArray(_instructions);
    }
    else
      _instructions = null;
  }
  
  public DataItem []getItem()
  {
    return _item;
  }
  
  public void setItem(DataItem []item)
  {
    _item = item;
  }
  
  public void setItemList(ArrayList<DataItem> itemList)
  {
    if (itemList != null && itemList.size() > 0) {
      _item = new DataItem[itemList.size()];
      itemList.toArray(_item);
    }
    else
      _item = null;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[type=").append(_type);

    if (_title != null)
      sb.append(",title=").append(_title);
    
    if (_instructions != null) {
      for (int i = 0; i < _instructions.length; i++) {
        sb.append(",instruction='").append(_instructions[i]).append("'");
      }
    }

    if (_field != null) {
      for (int i = 0; i < _field.length; i++) {
        sb.append(",field=").append(_field[i]);
      }
    }

    if (_reported != null)
      sb.append(",reported=").append(_reported);

    if (_item != null) {
      for (int i = 0; i < _item.length; i++) {
        sb.append(",item='").append(_item[i]);
      }
    }

    sb.append("]");
    
    return sb.toString();
  }
}
