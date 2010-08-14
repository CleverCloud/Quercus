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

import javax.faces.*;
import java.util.*;
import java.sql.*;

public class ResultSetDataModel extends DataModel
{
  private ResultSet _rs;
  private ResultSetMetaData _metaData;
  private DataMap _map;
  private int _rowIndex = -1;

  public ResultSetDataModel()
  {
  }

  public ResultSetDataModel(ResultSet value)
  {
    _rs = value;
    setRowIndex(0);
  }

  public int getRowCount()
  {
    return -1;
  }

  public Object getRowData()
  {
    if (_rs == null)
      return null;
    else if (isRowAvailable()) {
      if (_map == null)
        _map = new DataMap(_rs, getMetaData());

      return _map;
    }
    else
      throw new IllegalArgumentException();
  }
  
  public boolean isRowAvailable()
  {
    try {
      return _rs != null && _rs.absolute(getRowIndex() + 1);
    } catch (SQLException e) {
      throw new FacesException(e);
    }
  }

  public Object getWrappedData()
  {
    return _rs;
  }

  public void setWrappedData(Object data)
  {
    _rs = (ResultSet) data;
    _metaData = null;
    _map = null;
    setRowIndex(0);
  }

  public int getRowIndex()
  {
    return _rowIndex;
  }

  public void setRowIndex(int index)
  {
    if (_rs != null && index < -1)
      throw new IllegalArgumentException("rowIndex '" + index + "' cannot be less than -1.");

    DataModelListener []listeners = getDataModelListeners();

    if (listeners.length > 0 && _rs != null && _rowIndex != index) {
      DataModelEvent event = new DataModelEvent(this, index, _rs);

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].rowSelected(event);
      }
    }
    
    _rowIndex = index;
  }

  private ResultSetMetaData getMetaData()
  {
    try {
      if (_metaData == null) {
        _metaData = _rs.getMetaData();
      }

      return _metaData;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  static class DataMap extends AbstractMap<String,Object> {
    private ResultSet _rs;
    private ResultSetMetaData _metaData;
    private int _columnCount;
    private Set<String> _keySet;
    private Set<Entry<String,Object>> _entrySet;

    DataMap(ResultSet resultSet, ResultSetMetaData metaData)
    {
      _rs = resultSet;
      _metaData = metaData;

      try {
        _columnCount = _metaData.getColumnCount();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Object get(Object key)
    {
      int column = getColumn(String.valueOf(key));
      
      if (column < 0)
        throw new IllegalArgumentException("'" + key + "' is an unknown column");

      try {
        return _rs.getObject(column + 1);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Object remove(Object key)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
      throw new UnsupportedOperationException();
    }
    
    public Set<String> keySet()
    {
      if (_keySet == null)
        _keySet = new DataMapKeySet(_metaData);
      
      return _keySet;
    }
    
    public Collection<Object> values()
    {
      return new DataMapValues(_rs, _columnCount);
    }

    @Override
    public boolean containsKey(Object v)
    {
      String key = String.valueOf(v);

      return getColumn(key) >= 0;
    }

    @Override
    public Object put(String k, Object value)
    {
      try {
        String key = String.valueOf(k);

        int column = getColumn(key) + 1;

        if (column <= 0)
          throw new IllegalArgumentException();

        _rs.updateObject(column, value);

        return null;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } catch (RuntimeException e) {
        throw e;
      }
    }
    
    public Set<Entry<String,Object>> entrySet()
    {
      if (_entrySet == null)
        _entrySet = new DataMapEntrySet(_rs, _metaData);
      
      return _entrySet;
    }

    private int getColumn(String key)
    {
      try {
        for (int i = 0; i < _columnCount; i++) {
          if (_metaData.getColumnName(i + 1).equalsIgnoreCase(key))
            return i;
        }

        return -1;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class DataMapKeySet
    extends AbstractSet<String>
  {
    private ResultSetMetaData _metaData;
    private int _columnCount;

    DataMapKeySet(ResultSetMetaData metaData)
    {
      _metaData = metaData;

      try {
        _columnCount = _metaData.getColumnCount();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    
    public int size()
    {
      return _columnCount;
    }

    @Override
    public boolean contains(Object v)
    {
      try {
        String key = String.valueOf(v);

        for (int i = 0; i < _columnCount; i++) {
          if (_metaData.getColumnName(i + 1).equalsIgnoreCase(key))
            return true;
        }

        return false;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean remove(Object key)
    {
      throw new UnsupportedOperationException();
    }
    
    public Iterator<String> iterator()
    {
      ArrayList<String> keys = new ArrayList<String>();

      try {
        for (int i = 0; i < _columnCount; i++) {
          keys.add(_metaData.getColumnName(i + 1));
        }

        return Collections.unmodifiableList(keys).iterator();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class DataMapValues
    extends AbstractCollection
  {
    private ResultSet _rs;
    private int _columnCount;

    DataMapValues(ResultSet rs, int columnCount)
    {
      _rs = rs;
      _columnCount = columnCount;
    }
    
    public int size()
    {
      return _columnCount;
    }

    @Override
    public boolean contains(Object v)
    {
      try {
        if (v == null)
          return false;

        for (int i = 0; i < _columnCount; i++) {
          if (v.equals(_rs.getObject(i + 1)))
            return true;
        }

        return false;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean remove(Object key)
    {
      throw new UnsupportedOperationException();
    }
    
    public Iterator iterator()
    {
      ArrayList values = new ArrayList();

      try {
        for (int i = 0; i < _columnCount; i++) {
          values.add(_rs.getObject(i + 1));
        }

        return Collections.unmodifiableList(values).iterator();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class DataMapEntrySet
    extends AbstractSet<Map.Entry<String,Object>>
  {
    private ResultSet _rs;
    private ResultSetMetaData _metaData;
    private int _columnCount;
    private int _index;

    DataMapEntrySet(ResultSet rs, ResultSetMetaData metaData)
    {
      _rs = rs;
      _metaData = metaData;

      try {
        _columnCount = _metaData.getColumnCount();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    
    public int size()
    {
      return _columnCount;
    }

    @Override
    public boolean remove(Object key)
    {
      throw new UnsupportedOperationException();
    }
    
    public Iterator<Map.Entry<String,Object>> iterator()
    {
      return new EntrySetIterator(_rs, _metaData, _columnCount);
    }
  }

  static class EntrySetIterator implements Iterator<Map.Entry<String,Object>>
  {
    private ResultSet _rs;
    private ResultSetMetaData _metaData;
    private Entry _entry = new Entry();
    private int _count;
    private int _index;

    EntrySetIterator(ResultSet rs, ResultSetMetaData metaData, int count)
    {
      _rs = rs;
      _metaData = metaData;
      _count = count;
    }

    public boolean hasNext()
    {
      return _index < _count;
    }

    public Map.Entry<String,Object> next()
    {
      if (_index++ < _count)
        return _entry;
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
      
    class Entry implements Map.Entry<String,Object> {
      public String getKey()
      {
        try {
          return _metaData.getColumnName(_index);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
      
      public Object getValue()
      {
        try {
          return _rs.getObject(_index);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      public Object setValue(Object value)
      {
        try {
          _rs.updateObject(_index, value);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }

        return null;
      }
    }
  }
}
