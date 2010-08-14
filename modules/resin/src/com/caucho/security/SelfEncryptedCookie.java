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

package com.caucho.security;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.util.L10N;
import com.caucho.vfs.TempOutputStream;

/**
 * Self-encrypted cookie for server to server authentication.
 *
 * @since Resin 4.0.0
 */
public class SelfEncryptedCookie implements Serializable {
  private static final long serialVersionUID = 4678288478579787074L;

  private static final L10N L = new L10N(SelfEncryptedCookie.class); 
  
  private final String _cookie;
  private final long _createTime;

  /**
   * Hessian serialization
   */
  @SuppressWarnings("unused")
  private SelfEncryptedCookie()
  {
    _cookie = null;
    _createTime = 0;
  }

  public SelfEncryptedCookie(String cookie, long createTime)
  {
    _cookie = cookie;
    _createTime = createTime;
  }

  public String getCookie()
  {
    return _cookie;
  }

  public long getCreateTime()
  {
    return _createTime;
  }

  public static byte []encrypt(String cookie, long createTime)
  {
    try {
      Cipher cipher = initCipher(cookie, Cipher.ENCRYPT_MODE);
      
      SelfEncryptedCookie cookieObj
        = new SelfEncryptedCookie(cookie, createTime);

      TempOutputStream tos = new TempOutputStream();
      Hessian2Output hOut = new Hessian2Output(tos);

      hOut.writeObject(cookieObj);

      hOut.close();

      TempOutputStream cipherOut = new TempOutputStream();
      CipherOutputStream cOut
        = new CipherOutputStream(cipherOut, cipher);

      tos.writeToStream(cOut);
      tos.destroy();

      cOut.close();

      byte []encryptedData = cipherOut.toByteArray();

      cipherOut.destroy();

      return encryptedData;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static SelfEncryptedCookie decrypt(String cookie, byte []encData)
  {
    try {
      Cipher cipher = initCipher(cookie, Cipher.DECRYPT_MODE);

      ByteArrayInputStream is = new ByteArrayInputStream(encData);
      CipherInputStream cIn = new CipherInputStream(is, cipher);

      Hessian2Input in = new Hessian2Input(cIn);

      Object obj = in.readObject();

      if (! (obj instanceof SelfEncryptedCookie))
        throw new SecurityException(L.l("SelfEncryptedCookie[] is invalid because it does not correctly decrypt"));

      SelfEncryptedCookie cookieObj = (SelfEncryptedCookie) obj;

      in.close();
      cIn.close();
      is.close();

      return cookieObj;
    } catch (SecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new SecurityException(L.l("SelfEncryptedCookie[] does not correctly decrypt."), e);
    }
  }

  private static Cipher initCipher(String cookie, int mode)
    throws NoSuchAlgorithmException,
           InvalidKeyException,
           NoSuchPaddingException
  {
    Cipher cipher = Cipher.getInstance("AES");

    byte []keyBytes = new byte[cipher.getBlockSize()];
    for (int i = 0; i < cookie.length() && i < keyBytes.length; i++) {
      keyBytes[i] = (byte) cookie.charAt(i);
    }

    SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
    cipher.init(mode, keySpec);

    return cipher;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
