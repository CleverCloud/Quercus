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

package com.caucho.vfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Underlying stream handling HTTP requests.
 */
public class GzipStream extends OutputStream {
  private static final Logger log
    = Logger.getLogger(GzipStream.class.getName());

  private final Deflater _deflater =
    new Deflater(Deflater.DEFAULT_COMPRESSION, true);
  private final CRC32 _crc = new CRC32();

  private final byte []_buffer = new byte[1024];
  private final byte []_byteBuffer = new byte[1];

  private OutputStream _os;
  private boolean _enable;
  private boolean _isFirst;
  private boolean _isData;

  private boolean _isGzip = true;

  /**
   * Create a new Gzip stream.
   */
  public GzipStream()
  {
  }

  /**
   * Create a new Gzip stream.
   */
  public GzipStream(OutputStream os)
    throws IOException
  {
    init(os);
  }

  /**
   * Sets true to enable;
   */
  public void setEnable(boolean enable)
  {
    _enable = enable;
  }

  /**
   * If true, use gzip.  If false, use deflate
   */
  public void setGzip(boolean isGzip)
  {
    _isGzip = isGzip;
  }

  /**
   * Initializes the stream for the next request.
   */
  public void init(OutputStream os)
  {
    _os = os;
    _enable = true;
    _deflater.reset();
    _crc.reset();
    _isFirst = true;
    _isData = false;
  }

  /**
   * Resets the stream
   */
  public void reset()
  {
    _deflater.reset();
    _crc.reset();
    _isFirst = true;
    _isData = false;
  }

  /**
   * Checks if data is written to the stream.
   */
  public boolean isData()
  {
    return _isData;
  }

  /**
   * The stream is always writable (?)
   */
  public boolean canWrite()
  {
    return _os != null;
  }

  /**
   * Writes a byte
   *
   * @param v the value to write
   */
  public void write(int v)
    throws IOException
  {
    _byteBuffer[0] = (byte) v;

    write(_byteBuffer, 0, 1);
  }

  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   */
  public void write(byte []buf, int offset, int length)
    throws IOException
  {
    OutputStream os = _os;

    if (os == null || length == 0)
      return;

    if (! _enable) {
      os.write(buf, offset, length);
    }
    else {
      if (_isGzip && _isFirst) {
        _isFirst = false;

        writeHeader(os);
      }

      _isData = true;
      _deflater.setInput(buf, offset, length);
      _crc.update(buf, offset, length);

      writeToStream(os);
    }
  }

  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   * @param isEnd true when the write is flushing a close.
   */
  public void closeWrite()
    throws IOException
  {
    OutputStream os = _os;

    if (os == null || ! _enable)
      return;

    // server/183s - xxx: may want config item
    if (! _isData)
      return;

    if (_isGzip && _isFirst)
      writeHeader(os);

    _deflater.finish();

    writeToStream(os);

    if (_isGzip)
      writeFooter(os);
  }

  private void writeToStream(OutputStream os)
    throws IOException
  {
    int sublen;

    // XXX: doesn't seem to be a performance win
    if (false && os instanceof OutputStreamWithBuffer) {
      OutputStreamWithBuffer osBuf = (OutputStreamWithBuffer) os;

      byte []buffer = osBuf.getBuffer();
      int offset = osBuf.getBufferOffset();

      while ((sublen = _deflater.deflate(buffer,
                                         offset,
                                         buffer.length - offset)) > 0) {
        buffer = osBuf.nextBuffer(offset + sublen);
        offset = osBuf.getBufferOffset();
      }
      osBuf.setBufferOffset(offset);
    }
    else {
      while ((sublen = _deflater.deflate(_buffer, 0, _buffer.length)) > 0) {
        os.write(_buffer, 0, sublen);
      }
    }
  }

  /**
   * Writes the header
   */
  private static void writeHeader(OutputStream os)
    throws IOException
  {
    os.write(31);
    os.write(139);

    os.write(8);
    os.write(0);

    os.write(0);
    os.write(0);
    os.write(0);
    os.write(0);

    os.write(0);
    os.write(0);
  }

  /**
   * Writes the header
   */
  private void writeFooter(OutputStream os)
    throws IOException
  {
    writeInt(os, (int) _crc.getValue());
    writeInt(os, _deflater.getTotalIn());
  }

  /**
   * Writes a short value.
   */
  private static void writeInt(OutputStream os, int i)
    throws IOException
  {
    os.write(i & 0xff);
    os.write((i >> 8) & 0xff);
    os.write((i >> 16) & 0xff);
    os.write((i >> 24) & 0xff);
  }

  /**
   * Writes a short value.
   */
  private static void writeShort(OutputStream os, int i)
    throws IOException
  {
    os.write(i & 0xff);
    os.write((i >> 8) & 0xff);
  }

  /**
   * Flushes the underlying stream.
   */
  public void flush()
    throws IOException
  {
    _os.flush();
  }

  /**
   * Close the connection.
   */
  public void close() throws IOException
  {
    try {
      closeWrite();
    } finally {
      OutputStream os = _os;
      _os = null;

      if (os != null)
        os.close();
    }
  }

  public void free()
  {
    _os = null;
  }
}
