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

public abstract class DataModel
{
  private static final DataModelListener []NULL = new DataModelListener[0];
  
  private DataModelListener []_listeners = NULL;
  
  public abstract boolean isRowAvailable();

  public abstract int getRowCount();

  public abstract Object getRowData();

  public abstract int getRowIndex();

  public abstract void setRowIndex(int rowIndex);

  public abstract Object getWrappedData();
  
  public abstract void setWrappedData(Object data);

  public void addDataModelListener(DataModelListener listener)
  {
    if (listener == null)
      throw new NullPointerException();

    DataModelListener []newListeners
      = new DataModelListener[_listeners.length + 1];

    System.arraycopy(_listeners, 0, newListeners, 0, _listeners.length);
    newListeners[_listeners.length] = listener;

    _listeners = newListeners;
  }

  public void removeDataModelListener(DataModelListener listener)
  {
    if (listener == null)
      throw new NullPointerException();

    for (int i = _listeners.length - 1; i >= 0; i--) {
      if (_listeners[i].equals(listener)) {
        DataModelListener []newListeners
          = new DataModelListener[_listeners.length - 1];

        System.arraycopy(_listeners, 0, newListeners, 0, i);
        System.arraycopy(_listeners, i + 1, newListeners, i,
                         _listeners.length - i - 1);

        _listeners = newListeners;

        return;
      }
    }
  }

  public DataModelListener []getDataModelListeners()
  {
    return _listeners;
  }
}
