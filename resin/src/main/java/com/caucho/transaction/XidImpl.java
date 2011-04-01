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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.transaction;

import javax.transaction.xa.Xid;

import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;

/**
 * Transaction identifier implementation.
 */
public class XidImpl implements Xid {
  public static final int GLOBAL_LENGTH = 28;

  private byte [] _global;
  private byte [] _local;

  /**
   * Creates a new transaction id.
   * 
   * @param id
   *          the 64 bit number for the id.
   */
  public XidImpl(long serverId, 
                 long randomId, 
                 long sequence)
  {
    _global = new byte[GLOBAL_LENGTH];
    _local = new byte[4];
    _local[0] = 1;

    // the global id has the requirement of being globally unique

    // first 4 identify as Resin.
    _global[0] = 'R';
    _global[1] = 'e';
    _global[2] = 's';
    _global[3] = 'n';

    // next 8 is the crc64 of the caucho.server-id
    _global[4] = (byte) (serverId >> 56);
    _global[5] = (byte) (serverId >> 48);
    _global[6] = (byte) (serverId >> 40);
    _global[7] = (byte) (serverId >> 32);
    _global[8] = (byte) (serverId >> 24);
    _global[9] = (byte) (serverId >> 16);
    _global[10] = (byte) (serverId >> 8);
    _global[11] = (byte) (serverId);

    // next 8 is a 64-bit random long
    _global[12] = (byte) (randomId >> 56);
    _global[13] = (byte) (randomId >> 48);
    _global[14] = (byte) (randomId >> 40);
    _global[15] = (byte) (randomId >> 32);
    _global[16] = (byte) (randomId >> 24);
    _global[17] = (byte) (randomId >> 16);
    _global[18] = (byte) (randomId >> 8);
    _global[19] = (byte) (randomId);

    // next 8 is the current sequence

    _global[20] = (byte) (sequence >> 56);
    _global[21] = (byte) (sequence >> 48);
    _global[22] = (byte) (sequence >> 40);
    _global[23] = (byte) (sequence >> 32);
    _global[24] = (byte) (sequence >> 24);
    _global[25] = (byte) (sequence >> 16);
    _global[26] = (byte) (sequence >> 8);
    _global[27] = (byte) (sequence);
  }

  XidImpl(XidImpl base, int branch)
  {
    _global = new byte[base._global.length];
    _local = new byte[4];
    _local[0] = (byte) (branch);

    System.arraycopy(base._global, 0, _global, 0, _global.length);
  }

  public XidImpl(byte [] global, byte [] local)
  {
    _global = new byte[global.length];
    _local = new byte[local.length];

    System.arraycopy(global, 0, _global, 0, global.length);
    System.arraycopy(local, 0, _local, 0, local.length);
  }

  XidImpl(byte [] global)
  {
    _global = new byte[global.length];
    _local = new byte[4];

    System.arraycopy(global, 0, _global, 0, global.length);
    _local[0] = 1;
  }

  @Override
  public int getFormatId()
  {
    return 1234;
  }

  @Override
  public byte [] getBranchQualifier()
  {
    return _local;
  }

  @Override
  public byte [] getGlobalTransactionId()
  {
    return _global;
  }

  /**
   * Clones the xid.
   */
  @Override
  public Object clone()
  {
    return new XidImpl(_global, _local);
  }

  /**
   * Returns hashCode.
   */
  @Override
  public int hashCode()
  {
    byte [] global = _global;

    int hash = 37;

    for (int i = global.length - 1; i >= 0; i--)
      hash = 65521 * hash + global[i];

    return hash;
  }

  /**
   * Returns equality.
   */
  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof Xid))
      return false;

    Xid xid = (Xid) o;

    byte [] global = xid.getGlobalTransactionId();
    byte [] local = xid.getBranchQualifier();

    if (global.length != _global.length)
      return false;

    byte [] selfGlobal = _global;
    byte [] selfLocal = _local;

    for (int i = global.length - 1; i >= 0; i--) {
      if (global[i] != selfGlobal[i])
        return false;
    }

    for (int i = local.length - 1; i >= 0; i--) {
      if (local[i] != selfLocal[i])
        return false;
    }

    return true;
  }

  /**
   * Printable version of the transaction id.
   */
  @Override
  public String toString()
  {
    StringBuilder cb = new StringBuilder();

    cb.append("Xid[");

    byte [] branch = getBranchQualifier();

    addByte(cb, branch[0]);

    cb.append(":");

    byte [] global = getGlobalTransactionId();
    for (int i = 24; i < 28; i++)
      addByte(cb, global[i]);

    cb.append("]");

    return cb.toString();
  }

  /**
   * Adds hex for debug
   * 
   * @param cb
   *          the character buffer for the new value
   * @param b
   *          the byte value
   */
  static private void addByte(StringBuilder cb, int b)
  {
    int h = (b / 16) & 0xf;
    int l = b & 0xf;

    if (h >= 10)
      cb.append((char) ('a' + h - 10));
    else
      cb.append((char) ('0' + h));

    if (l >= 10)
      cb.append((char) ('a' + l - 10));
    else
      cb.append((char) ('0' + l));
  }
}
