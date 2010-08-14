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

package com.caucho.server.distcache;

import com.caucho.util.HashKey;
import java.security.*;

/**
 * Creates hashes for the identifiers.
 */
public class HashManager {
  public static final int SIZE = 32;
  public static final String HASH_ALGORITHM = "SHA-256";
  
  public static final HashKey NULL = new HashKey(new byte[SIZE]);
  
  private MessageDigest _digest;

  /**
   * Creates the manager
   */
  public HashManager()
  {
    try {
      _digest = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates a hash from a string
   */
  public HashKey generateHash(String key)
  {
    synchronized (_digest) {
      _digest.reset();

      int len = key.length();
      for (int i = 0; i < len; i++) {
        char ch = key.charAt(i);

        _digest.update((byte) ch);
        _digest.update((byte) (ch >> 8));
      }

      return new HashKey(_digest.digest());
    }
  }

  /**
   * Generates a hash from a prior hash and a string
   */
  public HashKey generateHash(HashKey priorHash, String key)
  {
    synchronized (_digest) {
      _digest.reset();

      _digest.update(priorHash.getHash());

      int len = key.length();
      for (int i = 0; i < len; i++) {
        char ch = key.charAt(i);

        _digest.update((byte) ch);
        _digest.update((byte) (ch >> 8));
      }

      return new HashKey(_digest.digest());
    }
  }
}
