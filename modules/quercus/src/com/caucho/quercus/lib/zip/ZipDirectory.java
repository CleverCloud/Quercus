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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zip;

import com.caucho.quercus.lib.file.BinaryInput;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * Reads the zip header and prepares zip entries.
 */
public class ZipDirectory
{
  private BinaryInput _in;
  private byte[] _tmpBuf;
  private ZipEntry _currentEntry;

  private boolean _eof;
  private boolean _ddescriptor;

  public ZipDirectory(BinaryInput in)
  {
    _in = in;
    _tmpBuf = new byte[32];
    _eof = false;
  }

  /**
   * Closes the previous entry and returns the next entry's metadata.
   */
  public QuercusZipEntry zip_read()
    throws IOException
  {
    closeEntry();

    long position = _in.getPosition();
    ZipEntry entry = readEntry();

    if (entry == null)
      return null;
    else
      return new QuercusZipEntry(entry, _in.openCopy(), position);
  }

  /**
   * Reads the next entry's metadata from the current stream position.
   */
  protected ZipEntry readEntry()
    throws IOException
  {
    if (_eof || _currentEntry != null)
      return null;

    int sublen = _in.read(_tmpBuf, 0, 30);
    if (sublen < 30) {
      _eof = true;
      return null;
    }

    // Zip file signature check
    if ((((_tmpBuf[3] & 0xff) << 24)
        | ((_tmpBuf[2] & 0xff) << 16)
        | ((_tmpBuf[1] & 0xff) << 8)
        | (_tmpBuf[0] & 0xff)) != 0x04034b50) {
      _eof = true;
      return null;
    }

    // Extra data descriptors after the compressed data
    if ((_tmpBuf[6] & 0x04) == 0x04)
      _ddescriptor = true;
    else 
      _ddescriptor = false;

    int compressionMethod = (_tmpBuf[8] & 0xff) | ((_tmpBuf[9] & 0xff) << 8);

    //if (compressionMethod != 0 && compressionMethod != 8)
//      throw new IOException(
//          "Unsupported zip compression method (" + compressionMethod + ").");

    long crc32 = _tmpBuf[14] & 0xff;
    crc32 |= (_tmpBuf[15] & 0xff) << 8;
    crc32 |= (_tmpBuf[16] & 0xff) << 16;
    crc32 |= ((long)_tmpBuf[17] & 0xff) << 24;

    long compressedSize = _tmpBuf[18] & 0xff;
    compressedSize |= (_tmpBuf[19] & 0xff) << 8;
    compressedSize |= (_tmpBuf[20] & 0xff) << 16;
    compressedSize |= ((long)_tmpBuf[21] & 0xff) << 24;

    long uncompressedSize = _tmpBuf[22] & 0xff;
    uncompressedSize |= (_tmpBuf[23] & 0xff) << 8;
    uncompressedSize |= (_tmpBuf[24] & 0xff) << 16;
    uncompressedSize |= ((long)_tmpBuf[25] & 0xff) << 24;

    int filenameLength = _tmpBuf[26] & 0xff;
    filenameLength |= (_tmpBuf[27] & 0xff) << 8;

    int extraLength = _tmpBuf[28] & 0xff;
    extraLength |= (_tmpBuf[29] & 0xff) << 8;

    // XXX: correct char encoding?
    String name;
    if (filenameLength <= _tmpBuf.length) {
      sublen = _in.read(_tmpBuf, 0, filenameLength);
      if (sublen < filenameLength)
        return null;
      name = new String(_tmpBuf, 0, sublen);
    }
    else {
      byte[] buffer = new byte[filenameLength];
      sublen = _in.read(buffer, 0, buffer.length);
      if (sublen < filenameLength)
        return null;
      name = new String(buffer, 0, sublen);
    }

    if (extraLength > 0)
      skip(extraLength);

    ZipEntry entry = new ZipEntry(name);
    entry.setMethod(compressionMethod);
    entry.setCrc(crc32);
    entry.setCompressedSize(compressedSize);
    entry.setSize(uncompressedSize);

    _currentEntry = entry;
    return entry;
  }

  private void skip(long len)
    throws IOException
  {
    while (len-- > 0 && _in.read() != -1) {
    }
  }

  /**
   * Positions stream to beginning of next entry
   */
  protected void closeEntry()
    throws IOException
  {
    if (_currentEntry == null)
      return;

    long length = _currentEntry.getCompressedSize();

    if (_ddescriptor)
      length += 12;

    skip(length);
    _currentEntry = null;
  }

  public boolean zip_close()
  {
    _in.close();

    return true;
  }

  public String toString()
  {
    return "ZipDirectory[]";
  }
}
