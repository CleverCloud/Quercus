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

import java.io.*;
import java.security.*;

/**
 * Creates hashes for the identifiers.
 */
public class Sha256OutputStream extends OutputStream {
  private static FreeList<MessageDigest> _freeDigestList
    = new FreeList<MessageDigest>(16);

  private OutputStream _os;
  private MessageDigest _digest;
  private byte []_hash;
  
  /**
   * Creates the output
   */
  public Sha256OutputStream(OutputStream os)
  {
    _os = os;
    
    try {
      _digest = _freeDigestList.allocate();

      if (_digest == null)
        _digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public byte []getDigest()
  {
    return _hash;
  }

  public void write(int value)
    throws IOException
  {
    _os.write(value);

    _digest.update((byte) value);
  }

  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    _os.write(buffer, offset, length);

    _digest.update(buffer, offset, length);
  }

  public void flush()
    throws IOException
  {
    _os.flush();
  }

  /**
   * Close the stream
   */
  public void close()
    throws IOException
  {
    OutputStream os = _os;
    _os = null;

    if (os != null)
      os.close();

    MessageDigest digest = _digest;
    _digest = null;

    if (digest != null) {
      _hash = digest.digest();

      digest.reset();
      _freeDigestList.free(digest);
    }
  }
}
