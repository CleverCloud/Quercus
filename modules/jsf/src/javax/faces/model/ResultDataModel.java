/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.model;

import javax.servlet.jsp.jstl.sql.Result;

public class ResultDataModel extends DataModel
{
  private Result _value;
  private int _rowIndex = -1;

  public ResultDataModel()
  {
  }

  public ResultDataModel(Result value)
  {
    _value = value;
    setRowIndex(0);
  }

  public int getRowCount()
  {
    if (_value != null)
      return _value.getRowCount();
    else
      return -1;
  }

  public Object getRowData()
  {
    if (_value == null)
      return null;
    else if (getRowIndex() < _value.getRowCount())
      return _value.getRows()[getRowIndex()];
    else
      throw new IllegalArgumentException();
  }
  
  public boolean isRowAvailable()
  {
    return _value != null && getRowIndex() < _value.getRowCount();
  }

  public Object getWrappedData()
  {
    return _value;
  }

  public void setWrappedData(Object data)
  {
    _value = (Result) data;
    setRowIndex(0);
  }

  public int getRowIndex()
  {
    return _rowIndex;
  }

  public void setRowIndex(int index)
  {
    if (_value != null && index < -1)
      throw new IllegalArgumentException("rowIndex '" + index + "' cannot be less than -1.");

    DataModelListener []listeners = getDataModelListeners();

    if (listeners.length > 0 && _value != null && _rowIndex != index) {
      DataModelEvent event = new DataModelEvent(this, index, _value);

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].rowSelected(event);
      }
    }
    
    _rowIndex = index;
  }
}
