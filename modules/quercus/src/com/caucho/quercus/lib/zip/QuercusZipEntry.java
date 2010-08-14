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

import com.caucho.quercus.*;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

public class QuercusZipEntry {
  private static final Logger log =
    Logger.getLogger(QuercusZipEntry.class.getName());
  private static final L10N L = new L10N(QuercusZipEntry.class);

  private final ZipEntry _entry;
  private final BinaryInput _binaryInput;
  private final long _position;

  private ZipEntryInputStream _in;

  public QuercusZipEntry(ZipEntry zipEntry,
                         BinaryInput binaryInput,
                         long position)
  {
    _entry = zipEntry;
    _binaryInput = binaryInput;
    _position = position;
  }

  /**
   * Returns the file name.
   */
  public String zip_entry_name()
  {
    return _entry.getName();
  }

  /**
   * Returns the file's uncompressed size.
   */
  public long zip_entry_filesize()
  {
    return _entry.getSize();
  }

  /**
   * Opens this zip entry for reading.
   */
  public boolean zip_entry_open(Env env, ZipDirectory directory)
  {
    try {
      // php/1u07.qa
      if (_in != null)
        return true;

      _in = new ZipEntryInputStream(_binaryInput.openCopy(), _position);

      return true;

    } catch (IOException e) {
      env.warning(L.l(e.toString()));

      return false;
    }
  }

  /**
   * Closes the zip entry.
   */
  public boolean zip_entry_close()
    throws IOException
  {
    if (_in == null)
      return false;

    ZipEntryInputStream in = _in;
    _in = null;

    in.close();

    return true;
  }

  /**
   * Reads and decompresses entry's compressed data.
   *
   * @return decompressed BinaryValue or FALSE on error
   */
  @ReturnNullAsFalse
    public StringValue zip_entry_read(Env env,
                                      @Optional("1024") int length)
  {
    if (_in == null)
      return null;

    StringValue bb = env.createBinaryBuilder();

    bb.appendReadAll((InputStream) _in, length);

    return bb;
    /*
    if (bb.length() > 0)
      return bb;
    else
      return null;
    */
  }

  /**
   * Returns the size of the compressed data.
   *
   * @return -1, or compressed size
   */
  public long zip_entry_compressedsize()
  {
    if (_entry == null)
      return -1;

    return _entry.getCompressedSize();
  }

  /**
   * Only "deflate" and "store" methods are supported.
   *
   * @return the compression method used for this entry
   */
  public String zip_entry_compressionmethod()
  {
    if (_entry == null)
      return "";

    Integer method = _entry.getMethod();

    switch(method) {
      case java.util.zip.ZipEntry.DEFLATED:
        return "deflated";
      case java.util.zip.ZipEntry.STORED:
        return "stored";
      default:
        return method.toString();
    }
  }

  public String toString()
  {
    return "QuercusZipEntry[" + _entry.getName() + "]";
  }
}
