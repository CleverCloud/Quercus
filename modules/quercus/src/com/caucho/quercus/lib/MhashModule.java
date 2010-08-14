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
 * @author Sam
 */

package com.caucho.quercus.lib;

import com.caucho.config.ConfigException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mhash functions.
 *
 * This module uses the {@link MessageDigest} class to calculate
 * digests. Typical java installations support MD2, MD5, SHA1, SHA256, SHA384,
 * and SHA512.
 */
public class MhashModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(MhashModule.class);
  private static final Logger log
    = Logger.getLogger(MhashModule.class.getName());

  public static final int MHASH_CRC32 = 0;
  public static final int MHASH_MD5 = 1;
  public static final int MHASH_SHA1 = 2;
  public static final int MHASH_HAVAL256 = 3;
  public static final int MHASH_RIPEMD160 = 5;
  public static final int MHASH_TIGER = 7;
  public static final int MHASH_GOST = 8;
  public static final int MHASH_CRC32B = 9;
  public static final int MHASH_HAVAL224 = 10;
  public static final int MHASH_HAVAL192 = 11;
  public static final int MHASH_HAVAL160 = 12;
  public static final int MHASH_HAVAL128 = 13;
  public static final int MHASH_TIGER128 = 14;
  public static final int MHASH_TIGER160 = 15;
  public static final int MHASH_MD4 = 16;
  public static final int MHASH_SHA256 = 17;
  public static final int MHASH_ADLER32 = 18;
  public static final int MHASH_SHA224 = 19;
  public static final int MHASH_SHA512 = 20;
  public static final int MHASH_SHA384 = 21;
  public static final int MHASH_WHIRLPOOL = 22;
  public static final int MHASH_RIPEMD128 = 23;
  public static final int MHASH_RIPEMD256 = 24;
  public static final int MHASH_RIPEMD320 = 25;
  public static final int MHASH_SNEFRU128 = 26;
  public static final int MHASH_SNEFRU256 = 27;
  public static final int MHASH_MD2 = 28;

  private HashMap<Integer, MhashAlgorithm> _algorithmMap
    = new HashMap<Integer, MhashAlgorithm>();

  private int _highestOrdinal;

  public MhashModule()
  {
    addAlgorithm(MHASH_CRC32, "CRC32",  "CRC32");
    addAlgorithm(MHASH_MD5, "MD5",  "MD5");
    addAlgorithm(MHASH_SHA1, "SHA1",  "SHA-1");
    addAlgorithm(MHASH_HAVAL256, "HAVAL256",  "HAVAL-256");
    addAlgorithm(MHASH_RIPEMD160, "RIPEMD160",  "RIPEMD-160");
    addAlgorithm(MHASH_TIGER, "TIGER",  "TIGER");
    addAlgorithm(MHASH_GOST, "GOST",  "GOST");
    addAlgorithm(MHASH_CRC32B, "CRC32B",  "CRC32B");
    addAlgorithm(MHASH_HAVAL224, "HAVAL224",  "HAVAL-224");
    addAlgorithm(MHASH_HAVAL192, "HAVAL192",  "HAVAL-192");
    addAlgorithm(MHASH_HAVAL160, "HAVAL160",  "HAVAL-160");
    addAlgorithm(MHASH_HAVAL128, "HAVAL128",  "HAVAL-128");
    addAlgorithm(MHASH_TIGER128, "TIGER128",  "TIGER-128");
    addAlgorithm(MHASH_TIGER160, "TIGER160",  "TIGER-160");
    addAlgorithm(MHASH_MD4, "MD4",  "MD4");
    addAlgorithm(MHASH_SHA256, "SHA256",  "SHA-256");
    addAlgorithm(MHASH_ADLER32, "ADLER32",  "ADLER-32");
    addAlgorithm(MHASH_SHA224, "SHA224",  "SHA-224");
    addAlgorithm(MHASH_SHA512, "SHA512",  "SHA-512");
    addAlgorithm(MHASH_SHA384, "SHA384",  "SHA-384");
    addAlgorithm(MHASH_WHIRLPOOL, "WHIRLPOOL",  "WHIRLPOOL");
    addAlgorithm(MHASH_RIPEMD128, "RIPEMD128",  "RIPEMD-128");
    addAlgorithm(MHASH_RIPEMD256, "RIPEMD256",  "RIPEMD-256");
    addAlgorithm(MHASH_RIPEMD320, "RIPEMD320",  "RIPEMD-320");
    addAlgorithm(MHASH_SNEFRU128, "SNEFRU128",  "SNEFRU-128");
    addAlgorithm(MHASH_SNEFRU256, "SNEFRU256",  "SNEFRU-256");
    addAlgorithm(MHASH_MD2, "MD2",  "MD2");
  }

  public String []getLoadedExtensions()
  {
    return new String[] {  "mhash" };
  }


  private void addAlgorithm(int ordinal, String name, String javaName)
  {
    MhashAlgorithm algorithm = new MhashAlgorithm(name, javaName, null);

    _algorithmMap.put(ordinal, algorithm);

    if (_highestOrdinal < ordinal)
      _highestOrdinal = ordinal;
  }

  public Value mhash(Env env, int hash, StringValue data, @Optional String key)
  {
    if (key.length() > 0)
      throw new UnsupportedOperationException("key"); // XXX:
    
    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null)
      return BooleanValue.FALSE;

    MessageDigest messageDigest = algorithm.createMessageDigest();

    if (messageDigest == null) {
      log.warning(L.l("no MessageDigest for {0}", algorithm));

      return BooleanValue.FALSE;
    }

    byte[] result = messageDigest.digest(data.toBytes());

    return env.createBinaryBuilder(result);
  }

  /**
   * Returns the highest available hash id.
   */
  public int mhash_count()
  {
    return _highestOrdinal;
  }

  public Value mhash_get_block_size(int hash)
  {
    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null || algorithm.createMessageDigest() == null)
      return BooleanValue.FALSE;

    return LongValue.create(512); // XXX: stubbed
  }

  public Value mhash_get_hash_name(Env env, int hash)
  {
    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null)
      return BooleanValue.FALSE;
    else
      return env.createString(algorithm.getName());
  }

  // XXX: public String mhash_keygen_s2k(
  // int hash, String password, String salt, int bytes)

  public static class MhashAlgorithm
  {
    private String _name;
    private String _javaName;
    private String _javaProvider;

    MhashAlgorithm(String name, String javaName, String javaProvider)
    {
      _name = name;
      _javaName = javaName;
      _javaProvider = javaProvider;
    }

    public MhashAlgorithm()
    {
    }

    /**
     * The php name, for example `CRC32'.
     */
    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    /**
     * The algorithm name to use when creating the java {@link MessageDigest}.
     *
     * @see MessageDigest#getInstance(String)
     */
    public void setJavaName(String javaName)
    {
      _javaName = javaName;
    }

    public String getJavaName()
    {
      return _javaName;
    }

    /**
     * The provider name to use when creating the java {@link MessageDigest},
     * null for the default.
     *
     * @see MessageDigest#getInstance(String, String)
     */
    public void setJavaProvider(String javaProvider)
    {
      _javaProvider = javaProvider;
    }

    public String getJavaProvider()
    {
      return _javaProvider;
    }

    public void init()
      throws ConfigException
    {
      if (_name == null)
        throw new ConfigException(L.l("`{0}' is required", "name"));

      if (_javaName == null)
        throw new ConfigException(L.l("`{0}' is required", "java-name"));
    }

    /**
     * Create a MessageDigest using
     * the javaName (and javaProvider, if not null).
     */
    public MessageDigest createMessageDigest()
    {
      try {
        if (_javaProvider != null)
          return MessageDigest.getInstance(_javaName, _javaProvider);
        else
          return MessageDigest.getInstance(_javaName);
      }
      catch (NoSuchAlgorithmException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, ex.toString(), ex);

        return null;
      }
      catch (NoSuchProviderException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, ex.toString(), ex);

        return null;
      }
    }

    public String toString()
    {
      return
        "MhashAlgorithm[name=" + _name
            + " java-name=" + _javaName 
            + " java-provider=" + _javaProvider
            + "]";
    }
  }
}

