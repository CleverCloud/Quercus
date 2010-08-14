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
public class GZIPOutputStream extends DeflaterOutputStream {
  private CRC32 _crc32;

  private byte[] _header = {
    (byte) 0x1f, (byte) 0x8b,  // gzip file identifier (ID1, ID2)
    8,           // Deflate compression method (CM)
    0,           // optional flags (FLG)
    0, 0, 0, 0,  // modification time (MTIME)
    0,           // extra optional flags (XFL)
    0            // operating system (OS)
  };

  private int _encodingMode;
  private boolean _isGzip;

  /**
   * Writes gzip header to OutputStream upon construction.
   * XXX: set operating system (file architecure) header.
   *
   * @param out
   * @param def
   */
  private GZIPOutputStream(OutputStream out, Deflater def)
    throws IOException
  {
    super(out, def);
    
    out.write(_header, 0, _header.length);
  }

  /**
   * @param out
   * @param compressionLevel
   * @param strategy Deflate compression strategy
   * @param encodingMode FORCE_GZIP to write gzwrite compatible output;
   *    FORCE_DEFLATE to write gzip header and zlib header,
   *    but do not write crc32 trailer
   */
  public GZIPOutputStream(OutputStream out,
                          int compressionLevel,
                          int strategy,
                          int encodingMode)
    throws IOException
  {
    this(out, createDeflater(compressionLevel, strategy, encodingMode));

    _isGzip = (encodingMode == ZlibModule.FORCE_GZIP);

    if (_isGzip)
      _crc32 = new CRC32();

    _encodingMode = encodingMode;
  }

  /**
   * Creates a deflater based on the Zlib arguments.
   */
  private static Deflater createDeflater(int compressionLevel,
                                         int strategy,
                                         int encodingMode)
  {
    Deflater defl;

    if (encodingMode == ZlibModule.FORCE_GZIP)
      defl = new Deflater(compressionLevel, true);
    else
      defl = new Deflater(compressionLevel, false);

    defl.setStrategy(strategy);

    return defl;
  }

  /**
   * @param out
   * @param compressionLevel
   * @param strategy Deflate compression strategy
   */
  public GZIPOutputStream(OutputStream out, int compressionLevel, int strategy)
    throws IOException
  {
    this(out, compressionLevel, strategy, ZlibModule.FORCE_GZIP);
  }

  /**
   * @param out
   */
  public GZIPOutputStream(OutputStream out)
    throws IOException
  {
    this(out, Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
  }

  /**
   * Writes a byte.
   *
   * @param input
   */
  public void write(int v)
    throws IOException
  {
    super.write(v);

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
    super.write(buffer, offset, length);
    
    if (_isGzip)
      _crc32.update(buffer, offset, length);
  }

  public void finish()
    throws IOException
  {
    super.finish();

    if (_isGzip) {
      long crcValue = _crc32.getValue();
      
      byte[] trailerCRC = new byte[4];
      
      trailerCRC[0] = (byte) crcValue;
      trailerCRC[1] = (byte) (crcValue >> 8);
      trailerCRC[2] = (byte) (crcValue >> 16);
      trailerCRC[3] = (byte) (crcValue >> 24);
      
      out.write(trailerCRC, 0, trailerCRC.length);
    }

    long inputSize = def.getBytesRead();
    
    byte[] trailerInputSize = new byte[4];

    trailerInputSize[0] = (byte) inputSize;
    trailerInputSize[1] = (byte) (inputSize >> 8);
    trailerInputSize[2] = (byte) (inputSize >> 16);
    trailerInputSize[3] = (byte) (inputSize >> 24);
    
    out.write(trailerInputSize, 0, trailerInputSize.length);

    out.flush();
  }

  /**
   * Calls super function, which in turn closes the underlying 'in' stream
   */
  public void close()
    throws IOException
  {
    if (! def.finished())
      finish();
    
    super.close();
  }
}
