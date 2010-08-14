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

package com.caucho.db.index;

import com.caucho.db.block.BlockStore;
import com.caucho.util.Hex;
import com.caucho.util.LruListener;

/**
 * Key to the front-end btree cache
 */
public final class IndexKey implements LruListener {
  private BTree _btree;
  private byte []_data;
  private int _offset;
  private int _length;

  private long _value;
  private boolean _isValid;
  private boolean _isStored;

  public IndexKey()
  {
  }

  public IndexKey(BTree btree, byte []data, int offset, int length, long value)
  {
    init(btree, data, offset, length);

    _value = value;
  }

  public static IndexKey create(BTree btree,
                                byte []data, int offset, int length,
                                long value)
  {
    byte []dataCopy = new byte[length];
    System.arraycopy(data, offset, dataCopy, 0, length);

    return new IndexKey(btree, dataCopy, 0, length, value);
  }

  public void init(BTree btree, byte []data, int offset, int length)
  {
    _btree = btree;
    _data = data;
    _offset = offset;
    _length = length;
  }

  public BTree getBTree()
  {
    return _btree;
  }

  public byte []getBuffer()
  {
    return _data;
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getLength()
  {
    return _length;
  }

  public void setValue(long value)
  {
    _value = value;
  }

  public long getValue()
  {
    return _value;
  }

  public boolean isValid()
  {
    return _isValid;
  }

  public void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  public boolean isStored()
  {
    return _isStored;
  }

  public void setStored(boolean isStored)
  {
    _isStored = isStored;
  }

  public void lruEvent()
  {
    IndexCache cache = IndexCache.create();

    cache.addWrite(this);
  }

  public void update()
  {
  }

  @Override
  public int hashCode()
  {
    int hash = _btree.hashCode();

    byte []data = _data;
    for (int length = _length - 1; length >= 0; length--) {
      hash = 65521 * hash + data[length];
    }

    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (! (o instanceof IndexKey))
      return false;

    IndexKey key = (IndexKey) o;

    if (_btree != key._btree)
      return false;

    int length = _length;
    if (length != key._length)
      return false;

    byte []dataA = _data;
    byte []dataB = key._data;

    int offsetA = _offset;
    int offsetB = key._offset;
    for (int i = 0; i < length; i++) {
      if (dataA[offsetA + i] != dataB[offsetB + i])
        return false;
    }

    return true;
  }

  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _btree
            + "," + Hex.toHex(_data, 0, _length) + "]");
  }
}
