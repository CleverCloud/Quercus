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

package com.caucho.quercus.lib;

import com.caucho.config.ConfigException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Hash functions.
 *
 * This module uses the {@link MessageDigest} class to calculate
 * digests. Typical java installations support MD2, MD5, SHA1, SHA256, SHA384,
 * and SHA512.
 */
public class HashModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(HashModule.class);
  private static final Logger log
    = Logger.getLogger(HashModule.class.getName());

  public static final int HASH_HMAC = 1;

  private static HashMap<String,String> _algorithmMap
    = new HashMap<String,String>();

  public HashModule()
  {
  }

  public String []getLoadedExtensions()
  {
    return new String[] { "hash" };
  }

  /**
   * Hashes a string
   */
  public Value hash(Env env,
                    String algorithm,
                    StringValue string,
                    @Optional boolean isBinary)
  {
    try {
      algorithm = getAlgorithm(algorithm);
      
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      
      int len = string.length();

      for (int i = 0; i < len; i++) {
        digest.update((byte) string.charAt(i));
      }

      byte []bytes = digest.digest();

      return hashToValue(env, bytes, isBinary);
    } catch (NoSuchAlgorithmException e) {
      env.error(L.l("'{0}' is an unknown algorithm", algorithm), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the list of known algorithms
   */
  public static Value hash_algos(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    for (String name : _algorithmMap.keySet()) {
      array.put(env.createString(name));
    }

    Collection<String> values = _algorithmMap.values();

    for (String name : Security.getAlgorithms("MessageDigest")) {
      if (! values.contains(name))
        array.put(env.createString(name));
    }
    
    return array;
  }

  /**
   * Copies a hash instance
   */
  public HashContext hash_copy(HashContext context)
  {
    if (context != null)
      return context.copy();
    else
      return null;
  }

  /**
   * Hashes a file
   */
  public Value hash_file(Env env,
                         String algorithm,
                         Path path,
                         @Optional boolean isBinary)
  {
    try {
      algorithm = getAlgorithm(algorithm);
      
      MessageDigest digest = MessageDigest.getInstance(algorithm);

      TempBuffer tempBuffer = TempBuffer.allocate();
      byte []buffer = tempBuffer.getBuffer();
      ReadStream is = path.openRead();
      
      try {
        int len;

        while ((len = is.read(buffer, 0, buffer.length)) > 0) {
          digest.update(buffer, 0, len);
        }

        byte []bytes = digest.digest();

        return hashToValue(env, bytes, isBinary);
      } finally {
        TempBuffer.free(tempBuffer);
        
        is.close();
      }
    } catch (NoSuchAlgorithmException e) {
      env.error(L.l("'{0}' is an unknown algorithm", algorithm), e);

      return BooleanValue.FALSE;
    } catch (IOException e) {
      env.error(L.l("'{0}' is an unknown file", path), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the final hash value
   */
  public Value hash_final(Env env,
                          HashContext context,
                          @Optional boolean isBinary)
  {
    if (context == null)
      return BooleanValue.FALSE;

    return hashToValue(env, context.digest(), isBinary);
  }

  /**
   * Hashes a string with the algorithm.
   */
  public Value hash_hmac(Env env,
                         String algorithm,
                         StringValue data,
                         StringValue key,
                         @Optional boolean isBinary)
  {
    algorithm = getAlgorithm(algorithm);
    
    HashContext context = hash_init(env, algorithm, HASH_HMAC, key);
    
    hash_update(env, context, data);

    return hash_final(env, context, isBinary);
  }

  /**
   * Hashes a file with the algorithm.
   */
  public Value hash_hmac_file(Env env,
                              String algorithm,
                              Path path,
                              StringValue key,
                              @Optional boolean isBinary)
  {
    algorithm = getAlgorithm(algorithm);
    
    HashContext context = hash_init(env, algorithm, HASH_HMAC, key);
    
    hash_update_file(env, context, path);

    return hash_final(env, context, isBinary);
  }

  /**
   * Initialize a hash context.
   */
  public HashContext hash_init(Env env,
                               String algorithm,
                               @Optional int options,
                               @Optional StringValue keyString)
  {
    try {
      algorithm = getAlgorithm(algorithm);
      
      if (options == HASH_HMAC) {
        algorithm = "Hmac" + algorithm;

        Mac mac = Mac.getInstance(algorithm);

        int keySize = 64;

        // php/530c
        if (keyString != null)
          keySize = keyString.length();

        byte []keyBytes = new byte[keySize];

        for (int i = 0; i < keyString.length(); i++) {
          keyBytes[i] = (byte) keyString.charAt(i);
        }

        Key key = new SecretKeySpec(keyBytes, "dsa");
        mac.init(key);

        return new HashMacContext(mac);
      }
      else {
        MessageDigest md = MessageDigest.getInstance(algorithm);

        return new HashDigestContext(md);
      }
    } catch (Exception e) {
      env.error(L.l("hash_init: '{0}' is an unknown algorithm",
                    algorithm));

      return null;
    }
  }

  /**
   * Updates the hash with more data
   */
  public Value hash_update(Env env,
                           HashContext context,
                           StringValue value)
  {
    if (context == null)
      return BooleanValue.FALSE;

    context.update(value);

    return BooleanValue.TRUE;
  }

  /**
   * Updates the hash with more data
   */
  public Value hash_update_file(Env env,
                                HashContext context,
                                Path path)
  {
    if (context == null)
      return BooleanValue.FALSE;

    TempBuffer tempBuffer = TempBuffer.allocate();
    byte []buffer = tempBuffer.getBuffer();
    ReadStream is = null;

    try {
      is = path.openRead();
      
      int len;

      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        context.update(buffer, 0, len);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      TempBuffer.free(tempBuffer);

      if (is != null)
        is.close();
    }

    return BooleanValue.TRUE;
  }

  /**
   * Updates the hash with more data
   */
  public int hash_update_stream(Env env,
                                HashContext context,
                                InputStream is,
                                @Optional("-1") int length)
  {
    if (context == null)
      return -1;

    if (length < 0)
      length = Integer.MAX_VALUE - 1;

    TempBuffer tempBuffer = TempBuffer.allocate();
    byte []buffer = tempBuffer.getBuffer();
    
    int readLength = 0;

    try {
      while (length > 0) {
        int sublen = buffer.length;

        if (length < sublen)
          sublen = length;

        int len = is.read(buffer, 0, sublen);

        if (len < 0)
          return readLength;

        context.update(buffer, 0, len);

        readLength += len;
        length -= len;
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      TempBuffer.free(tempBuffer);
    }

    return readLength;
  }
  
  // XXX: hash_update_file
  // XXX: hash_update_stream
  // XXX: hash_update
  // XXX: hash

  private static Value hashToValue(Env env, byte []bytes, boolean isBinary)
  {
    if (isBinary) {
      StringValue v = env.createBinaryBuilder();
      v.append(bytes, 0, bytes.length);
      return v;
    }
    else {
      StringValue v = env.createUnicodeBuilder();

      for (int i = 0; i < bytes.length; i++) {
        int ch = bytes[i];
        int d1 = (ch >> 4) & 0xf;
        int d2 = (ch) & 0xf;

        if (d1 < 10)
          v.append((char) ('0' + d1));
        else
          v.append((char) ('a' + d1 - 10));

        if (d2 < 10)
          v.append((char) ('0' + d2));
        else
          v.append((char) ('a' + d2 - 10));
      }

      return v;
    }
  }
  
  private static String getAlgorithm(String algorithm)
  {
    String name = _algorithmMap.get(algorithm);
    
    if (name != null)
      return name;
    else
      return algorithm;
  }

  public abstract static class HashContext
  {
    abstract void update(StringValue value);
    
    abstract void update(byte []buffer, int offset, int length);
    
    abstract byte []digest();

    abstract HashContext copy();
  }

  public static class HashDigestContext extends HashContext
  {
    private MessageDigest _digest;

    HashDigestContext(MessageDigest digest)
    {
      _digest = digest;
    }

    MessageDigest getDigest()
    {
      return _digest;
    }
    
    void update(byte value)
    {
      _digest.update(value);
    }
    
    void update(StringValue value)
    {
      int len = value.length();

      MessageDigest digest = _digest;
    
      for (int i = 0; i < len; i++) {
        digest.update((byte) value.charAt(i));
      }
    }
    
    void update(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    byte []digest()
    {
      return _digest.digest();
    }

    HashContext copy()
    {
      try {
        return new HashDigestContext((MessageDigest) _digest.clone());
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      }
    }

    public String toString()
    {
      return (getClass().getSimpleName() + "[" + _digest + "]");
    }
  }

  public static class HashMacContext extends HashContext
  {
    private Mac _digest;

    HashMacContext(Mac digest)
    {
      _digest = digest;
    }
    
    void update(byte value)
    {
      _digest.update(value);
    }
    
    void update(StringValue value)
    {
      int len = value.length();

      Mac digest = _digest;

      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.getBuffer();
      
      int offset = 0;
      
      while (offset < len) {
        int sublen = len - offset;
        if (buffer.length < sublen)
          sublen = buffer.length;
        
        for (int i = 0; i < sublen; i++) {
          buffer[i] = (byte) value.charAt(offset + i);
        }

        digest.update(buffer, 0, sublen);

        offset += sublen;
      }

      TempBuffer.free(tBuf);
    }
    
    void update(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    byte []digest()
    {
      return _digest.doFinal();
    }

    HashContext copy()
    {
      try {
        return new HashDigestContext((MessageDigest) _digest.clone());
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      }
    }

    public String toString()
    {
      return (getClass().getSimpleName() + "[" + _digest + "]");
    }
  }

  static {
    _algorithmMap.put("md2", "MD2");
    _algorithmMap.put("md5", "MD5");
    _algorithmMap.put("sha1", "SHA");
    _algorithmMap.put("sha256", "SHA-256");
    _algorithmMap.put("sha384", "SHA-384");
    _algorithmMap.put("sha512", "SHA-512");
  }
}

