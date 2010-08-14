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
 * element item {
 *   field+
 * }
 * </pre></code>
 */
public class DataItem implements java.io.Serializable {
  private DataField []_field;
  
  public DataItem()
  {
  }
  
  public DataItem(DataField []fields)
  {
    _field = fields;
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
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_field != null) {
      for (int i = 0; i < _field.length; i++) {
        if (i != 0)
          sb.append(",");

        sb.append("field=").append(_field[i]);
      }
    }

    sb.append("]");
    
    return sb.toString();
  }
}
