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

package com.caucho.util;

import java.security.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Creates hashes for the identifiers.
 */
public class HashKeyManager {
  public static final int SIZE = 32;
  public static final String HASH_ALGORITHM = "SHA-256";
  
  public static final HashKey NULL = new HashKey(new byte[SIZE]);

  private static final HashKeyManager _manager = new HashKeyManager();
  
  private final AtomicReference<MessageDigest> _digestRef
    = new AtomicReference<MessageDigest>();

  /**
   * Generates a hash from a string
   */
  public static HashKey generateHash(String key)
  {
    return _manager.generateHashImpl(key);
  }

  /**
   * Generates a hash from a prior hash and a string
   */
  public static HashKey generateHash(HashKey priorHash, String key)
  {
    return _manager.generateHashImpl(priorHash, key);
  }

  private HashKey generateHashImpl(String key)
  {
    MessageDigest digest = getDigest();

    int len = key.length();
    for (int i = 0; i < len; i++) {
      char ch = key.charAt(i);

      digest.update((byte) ch);
      digest.update((byte) (ch >> 8));
    }

    HashKey hashKey = new HashKey(digest.digest());

    _digestRef.set(digest);

    return hashKey;
  }

  /**
   * Generates a hash from a prior hash and a string
   */
  private HashKey generateHashImpl(HashKey priorHash, String key)
  {
    MessageDigest digest = getDigest();

    digest.update(priorHash.getHash());
    
    int len = key.length();
    for (int i = 0; i < len; i++) {
      char ch = key.charAt(i);

      digest.update((byte) ch);
      digest.update((byte) (ch >> 8));
    }

    HashKey hashKey = new HashKey(digest.digest());

    _digestRef.set(digest);

    return hashKey;
  }

  private MessageDigest getDigest()
  {
    MessageDigest digest = _digestRef.getAndSet(null);

    if (digest != null) {
      digest.reset();
    }
    else {
      try {
        digest = MessageDigest.getInstance(HASH_ALGORITHM);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return digest;
  }
}
