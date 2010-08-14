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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.zlib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.lib.file.AbstractBinaryOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * As opposed to java's GZIPOutputStream, this class allows for more control on
 * what is written to the underlying OutputStream. 
 *
 * @see java.util.zip.GZIPOutputStream
 */
public class ZlibOutputStream extends AbstractBinaryOutput {
  private OutputStream _os;
  private DeflaterOutputStream _out;
  private CRC32 _crc32;

  private static byte[] _header = {
    (byte) 0x1f, (byte) 0x8b,   // gzip file identifier (ID1, ID2)
    (byte) 0x8,                 // Deflate compression method (CM)
    0,                          // optional flags (FLG)
    0, 0, 0, 0,                 // modification time (MTIME)
    0,                          // extra optional flags (XFL)
    (byte) 0x3                  // operating system (OS)
  };

  private int _encodingMode;
  private boolean _isGzip;
  private long _inputSize;

  /**
   * Writes gzip header to OutputStream upon construction.
   * XXX: set operating system (file architecure) header.
   *
   * @param out
   * @param def
   */
  private ZlibOutputStream(OutputStream os, Deflater def)
    throws IOException
  {
    _os = os;
    _out = new DeflaterOutputStream(_os, def);
    
    _os.write(_header, 0, _header.length);
  }

  /**
   * @param out
   * @param compressionLevel
   * @param strategy Deflate compression strategy
   * @param encodingMode FORCE_GZIP to write gzwrite compatible output;
   *    FORCE_DEFLATE to write gzip header and zlib header, but do not
   *    write crc32 trailer
   */
  public ZlibOutputStream(OutputStream os,
                          int compressionLevel,
                          int strategy,
                          int encodingMode)
    throws IOException
  {
    this(os, createDeflater(compressionLevel, strategy, encodingMode));

    _isGzip = (encodingMode == ZlibModule.FORCE_GZIP);

    if (_isGzip)
      _crc32 = new CRC32();
  }

  /**
   * @param os
   * @param compressionLevel
   * @param strategy Deflate compression strategy
   */
  public ZlibOutputStream(OutputStream os, int compressionLevel, int strategy)
    throws IOException
  {
    this(os, compressionLevel, strategy, ZlibModule.FORCE_GZIP);
  }

  public ZlibOutputStream(OutputStream os)
    throws IOException
  {
    this(os,
         Deflater.DEFAULT_COMPRESSION,
         Deflater.DEFAULT_STRATEGY,
         ZlibModule.FORCE_GZIP);
  }

  /**
   * Creates a deflater based on the Zlib arguments.
   */
  private static Deflater createDeflater(int compressionLevel,
                                         int strategy,
                                         int encodingMode)
  {
    Deflater def;

    if (encodingMode == ZlibModule.FORCE_GZIP)
      def = new Deflater(compressionLevel, true);
    else
      def = new Deflater(compressionLevel, false);

    def.setStrategy(strategy);

    return def;
  }

  /**
   * Writes a byte.
   *
   * @param input
   */
  public void write(int v)
    throws IOException
  {
    _out.write(v);

    _inputSize++;
    
    if (_isGzip)
      _crc32.update(v);
  }

  /**
   * @param input
   * @param offset
   * @param length
   */
  public void write(byte[] buffer, int offset, int length)
    throws IOException
  {
    _out.write(buffer, offset, length);
    
    _inputSize += length;
    
    if (_isGzip)
      _crc32.update(buffer, offset, length);
  }

  private void finish(DeflaterOutputStream out)
    throws IOException
  {
    out.finish();
    
    if (_isGzip) {
      long crcValue = _crc32.getValue();
      
      _os.write((byte) crcValue);
      _os.write((byte) (crcValue >> 8));
      _os.write((byte) (crcValue >> 16));
      _os.write((byte) (crcValue >> 24));
    }

    _os.write((byte) _inputSize);
    _os.write((byte) (_inputSize >> 8));
    _os.write((byte) (_inputSize >> 16));
    _os.write((byte) (_inputSize >> 24));

    _os.flush();
  }

  public void flush()
  {
  }

  public void closeWrite()
  {
    close();
  }
  
  public void close()
  {
    try {
      DeflaterOutputStream out = _out;
      _out = null;

      if (out != null) {
        finish(out);

        out.close();
      }

      _os.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns false always for a write stream.
   */
  public boolean isEOF()
  {
    return false;
  }

  /**
   * Tells the position in the gzip stream
   */
  public long getPosition()
  {
    return _inputSize;
  }

  /**
   * Sets the position.
   */
  public boolean setPosition(long offset)
  {
    if (offset < _inputSize)
      return false;

    offset -= _inputSize;
    byte[] buffer = new byte[128];

    try {
      while (offset > 0) {
        int sublen = (int)Math.min(offset, buffer.length);
        write(buffer, 0, sublen);
        offset -= sublen;
      }

      return true;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public String toString()
  {
    return "ZlibOutputStream[]";
  }
}
