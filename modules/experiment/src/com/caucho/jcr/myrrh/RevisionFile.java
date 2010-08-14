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

package com.caucho.jcr.myrrh;

import com.caucho.vfs.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Syntax: repeated data chunks of the following format.
 *
 * 4      - length of revision
 * 20     - sha-1 of uncompressed
 * 8      - local commit timestamp
 * 4      - base offset (for delta)
 *  [...] - delta data
 * 4      - crc-32
 *
 * delta:
 *  00xxxxxx - add [0-63] bytes of data
 *  01xxxxxx b1 b0 - copy [0-63] bytes of data with rel offset to source
 *  10xxxxxx b0 - add upto 8k bytes of data
 *  11xxxxxx b0 b3 b2 b1 b0- copy up to 8k bytes of data with rel offset
 */
public class RevisionFile {
  private final Path _path;
  private boolean _isAllowCompress = true;

  public RevisionFile(Path path)
  {
    _path = path;
  }

  public void setAllowCompress(boolean allowCompress)
  {
    _isAllowCompress = allowCompress;
  }

  public byte []appendFile(Path path, long timestamp)
    throws IOException
  {
    int fileLength = (int) path.getLength();

    TempOutputStream ts = new TempOutputStream();

    OutputStream os = ts;

    DeflaterOutputStream dOut = null;

    if (_isAllowCompress) {
      dOut = new DeflaterOutputStream(os);
      os = dOut;
    }

    byte []id = getSha1(path, timestamp);

    ReadStream in = path.openRead();
    try {
      in.writeToStream(os);
    } finally {
      in.close();
    }

    if (dOut != null)
      dOut.close();

    long offset = _path.getLength();
    WriteStream out = _path.openAppend();

    try {
      if (offset <= 0) {
        out.write(0);
        out.write(1);
        offset = 2;
      }
      
      CRC32 crc = new CRC32();
      CheckedOutputStream crcOut = new CheckedOutputStream(out, crc);

      int len = ts.getLength() + 37;
    
      writeInt(crcOut, len); // length

      crcOut.write(id);
      writeLong(crcOut, timestamp);
      writeInt(crcOut, 0); // no base

      if (_isAllowCompress)
        crcOut.write('d');
      else
        crcOut.write(0);
      
      ts.writeToStream(crcOut);
      ts.destroy();

      writeInt(out, (int) crc.getValue());
      
      crcOut.close();
    } finally {
      out.close();
    }

    return id;
  }

  private byte []getSha1(Path path, long timestamp)
    throws IOException
  {
    ReadStream in = path.openRead();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");

      md.update((byte) (timestamp >> 56));
      md.update((byte) (timestamp >> 48));
      md.update((byte) (timestamp >> 40));
      md.update((byte) (timestamp >> 32));
      md.update((byte) (timestamp >> 24));
      md.update((byte) (timestamp >> 16));
      md.update((byte) (timestamp >> 8));
      md.update((byte) (timestamp));

      TempBuffer tempBuf = TempBuffer.allocate();
      byte []buffer = tempBuf.getBuffer();
      int len;

      while ((len = in.read(buffer, 0, buffer.length)) > 0) {
        md.update(buffer, 0, len);
      }

      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } finally {
      in.close();
    }
  }

  private void writeInt(OutputStream out, int v)
    throws IOException
  {
    out.write(v >> 24);
    out.write(v >> 16);
    out.write(v >> 8);
    out.write(v);
  }

  private void writeLong(OutputStream out, long v)
    throws IOException
  {
    out.write((int) (v >> 56));
    out.write((int) (v >> 48));
    out.write((int) (v >> 40));
    out.write((int) (v >> 32));
    out.write((int) (v >> 24));
    out.write((int) (v >> 16));
    out.write((int) (v >> 8));
    out.write((int) v);
  }
}


