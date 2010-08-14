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
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Similar to GZIPInputStream but with ability to read appended gzip.
 */
public class GZInputStream extends InputStream
{
  private PushbackInputStream _in;
  private Inflater _inflater;

  private CRC32 _crc;
  private boolean _eof;
  private boolean _isGzip;

  private byte[] _readBuffer;        //raw input data buffer
  private byte[] _tbuffer;        //temporary buffer

  private int _readBufferSize;        //amount of raw data read into _readBuffer
  private int _inputSize;        //decompressed bytes read so far
                        //  for the current 'append' stream

  private long _totalInputSize;        //total decompressed bytes read

  public GZInputStream(InputStream in)
    throws IOException
  {
    this(in, 512);
  }

  public GZInputStream(InputStream in, int size)
    throws IOException
  {
    // Need to use same buffer size for pushback and _readBuffer
    // because will need to unread <= _readBuffer.length.
    _in = new PushbackInputStream(in, size);

    _inflater = new Inflater(true);
    _crc = new CRC32();
    _eof = false;

    _readBuffer = new byte[size];
    _tbuffer = new byte[128];

    _totalInputSize = 0;

    init();
  }

  /**
   * Returns 0 if gzip EOF has been reached, 1 otherwise
   */
  public int available()
    throws IOException
  {
    if (!_isGzip)
      return _in.available();

    if (_eof == true)
      return 0;
    return 1;
  }

  public void close()
    throws IOException
  {
    _inflater.end();
  }

  /**
   * mark() and reset() are not supported by this class.
   * @return false always
   */
  public boolean markSupported()
  {
    return false;
  }

  /**
   * Returns the byte read, -1 if EOF
   * @return number of bytes read, or -1 if EOF
   */
  public int read() throws IOException
  {
    byte[] b = new byte[1];
    int n = read(b);
    if (n < 0)
      return -1;
    
    return b[0];
  }

  /**
   * Reads from the compressed stream and
   *  stores the resulting uncompressed data into the byte array.
   * @return number of bytes read, or -1 upon EOF
   */
  public int read(byte[] b)
    throws IOException
  {
    return read(b, 0, b.length);
  }

  /**
   * Reads from the compressed stream and
   *  stores the resulting uncompressed data into the byte array.
   * @return number of bytes read, or -1 upon EOF
   */
  public int read(byte[] b, int off, int len)
    throws IOException
  {
    if (len <= 0 || off < 0 || off + len > b.length)
      return 0;

    if (_eof)
      return -1;

    // Read from uncompressed stream
    if (! _isGzip)
      return _in.read(b, off, len);

    try {
      int sublen;
      int length = 0;
      while (length < len) {
        if (_inflater.needsInput()) {
          _readBufferSize = _in.read(_readBuffer, 0, _readBuffer.length);
          if (_readBufferSize < 0)
            break;

          _inflater.setInput(_readBuffer, 0, _readBufferSize);
        }

       sublen = _inflater.inflate(b, off + length, len - length);
       
        _crc.update(b, off + length, sublen);
       _inputSize += sublen;
       _totalInputSize += sublen;
       
       length += sublen;

        // Unread gzip trailer and possibly beginning of appended gzip data.
        if (_inflater.finished()) {
          int remaining = _inflater.getRemaining();
          _in.unread(_readBuffer, _readBufferSize - remaining, remaining);
          
          readTrailer();
          
          int secondPart = read(b, off + length, len - length);
          
          return secondPart > 0 ? length + secondPart : length;
        }
      }

      return length;
    }
    catch (DataFormatException e) {
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Skips over and discards n bytes.
   * @param n number of bytes to skip
   * @return actual number of bytes skipped
   */
  public long skip(long n)
    throws IOException
  {
    if (_eof || n <= 0)
      return 0;
    
    long remaining = n;
    while (remaining > 0) {
      int length = (int)Math.min(_tbuffer.length, remaining);
      
      int sublen = read(_tbuffer, 0, length);
      if (sublen < 0)
        break;
      
      remaining -= sublen;
    }
    return (n - remaining);
  }

  /**
   * Inits/resets this class to be ready to read the start of a gzip stream.
   */
  private void init()
    throws IOException
  {
    _inflater.reset();
    _crc.reset();
    _inputSize = 0;
    _readBufferSize = 0;

    byte flg;

    int length = _in.read(_tbuffer, 0, 10);

    if (length < 0) {
      _isGzip = false;
      return;
    }
    else if (length != 10) {
      _isGzip = false;
      _in.unread(_tbuffer, 0, length);
      return;
    }
    
    if (_tbuffer[0] != (byte)0x1f || _tbuffer[1] != (byte)0x8b) {
      _isGzip = false;
      _in.unread(_tbuffer, 0, length);
      return;
    }

    flg = _tbuffer[3];

    // Skip optional field
    if ((flg & (byte)0x04) > 0) {
      length = _in.read(_tbuffer, 0, 2);
      if (length != 2)
        throw new IOException("Bad GZIP (FEXTRA) header.");
      length = (((int)_tbuffer[1]) << 4) | _tbuffer[0];
      _in.skip(length);
    }

    int c;

    // Skip optional field
    if ((flg & (byte)0x08) > 0) {
      c = _in.read();
      while (c != 0) {
        if (c < 0)
          throw new IOException("Bad GZIP (FNAME) header.");
        c = _in.read();
      }
    }

    // Skip optional field
    if ((flg & 0x10) > 0) {
      c = _in.read();
      while (c != 0) {
        if (c < 0)
          throw new IOException("Bad GZIP (FCOMMENT) header.");
        
        c = _in.read();
      }
    }

    // Skip optional field
    if ((flg & 0x02) > 0) {
      length = _in.read(_tbuffer, 0, 2);
      if (length != 2)
        throw new IOException("Bad GZIP (FHCRC) header.");
    }

    _isGzip = true;
  }

  /**
   * Reads the trailer and prepare this class for the possibility
   * of an appended gzip stream.
   */
  private void readTrailer()
    throws IOException
  {
    int length = _in.read(_tbuffer, 0, 8);
    if (length !=  8)
      throw new IOException("Bad GZIP trailer.");

    int refValue = _tbuffer[3] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[2] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[1] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[0] & 0xff;

    int value = (int)_crc.getValue();

    if (refValue != value)
      throw new IOException("Bad GZIP trailer (CRC32).");

    refValue = _tbuffer[7] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[6] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[5] & 0xff;
    refValue <<= 8;
    refValue |= _tbuffer[4] & 0xff;

    if (refValue != _inputSize)
      throw new IOException("Bad GZIP trailer (LENGTH).");

    // Check to see if this gzip stream is appended with a valid gzip stream.
    // If it is appended, then can continue reading from stream.
    int c = _in.read();
    
    if (c < 0)
      _eof = true;
    else {
      _in.unread(c);
      
      init();

      if (!_isGzip)
        _eof = true;
    }
  }

  /*
   * Returns true if stream is in gzip format.
   */
  public boolean isGzip()
  {
    return _isGzip;
  }
}
