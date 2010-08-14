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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zlib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.FileValue;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

/**
 * Zlib object oriented API facade
 */

public class Zlib {
  private static final Logger log = Logger.getLogger(Zlib.class.getName());
  private static final L10N L = new L10N(Zlib.class);

  private InputStream _in;
  private ZlibOutputStream _gzout;

  private FileValue _fileValue;
  private boolean _isGZIPInputStream;

  /**
   * XXX: todo - implement additional read/write modes (a,etc)
   *
   * Creates and sets GZIP stream if mode is 'w'
   * Also creates _fileValue.  All write functions are wrappers around
   * the _fileValue functions using the private class GZFileValueWriter to
   * compress the byte stream.
   *
   *
   * @param fileName
   * @param mode (ie: "w9" or "r7f")
   * @param useIncludePath is always on
   */
  public Zlib(Env env, String filename, String mode, boolean useIncludePath)
  {
    String filemode = getFileMode(mode);
    int compressionLevel = getCompressionLevel(mode);
    int compressionStrategy = getCompressionStrategy(mode);

    Value val = null;
    // FileModule.fopen(env, filename, mode, useIncludePath, null);
    
    if (val != BooleanValue.FALSE)
      _fileValue = (FileValue)val;

      /*
    try {
      if (filemode.equals("r")) {
        _in = getGZIPInputStream();
      }
      else if (filemode.equals("w")) {
        _gzout = new ZlibOutputStream(_fileValue.getPath().openWrite(),
                                      compressionLevel,
                                      compressionStrategy);
      }
      else if (filemode.equals("a")) {
        _gzout = new ZlibOutputStream(_fileValue.getPath().openAppend(),
                                      compressionLevel,
                                      compressionStrategy);
      }
      else if (filemode.equals("x")) {
        _gzout = new ZlibOutputStream(_fileValue.getPath().openWrite(),
                                      compressionLevel,
                                      compressionStrategy);
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }
      */
  }


  /**
   * Reads from the input and writes to the gzip stream
   * @param s
   * @param length # of bytes to compress
   * @return # of uncompressed bytes
   */
  public int gzwrite(Env env, InputStream is, @Optional("-1") int length)
  {
    if (_fileValue == null) {
      env.warning(L.l("file could not be open for writing"));
      return -1;
    }

    TempBuffer tb = TempBuffer.allocate();
    byte[] buffer = tb.getBuffer();

    int inputSize = 0;
    int sublen;

    if (length < 0)
      length = Integer.MAX_VALUE;

    try {
      while (length > 0) {
        if (buffer.length < length)
          sublen = buffer.length;
        else
          sublen = length;

        sublen = is.read(buffer, 0, sublen);

        if (sublen <= 0)
          break;

        _gzout.write(buffer, 0, sublen);

        inputSize += sublen;
        length -= sublen;
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }
    
    TempBuffer.free(tb);
    return inputSize;
  }


  /**
   * Closes the gzip stream
   * @return true if successful, false otherwise
   */
  public boolean gzclose()
  {
    if (_fileValue == null) {
      return false;
    }

    try {
      if (_gzout != null) {
        _gzout.close();
        _gzout = null;
      }
      
      if (_in != null) {
        _in.close();
        _in = null;
      }
    }
    catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
    return true;
  }

  /**
   * alias of gzwrite
   * @param env
   * @param s
   * @param length
   * @return # of uncompressed bytes
   */
  public int gzputs(Env env,
                    @NotNull InputStream is,
                    @Optional("-1") int length)
  {
    return gzwrite(env, is, length);
  }

  /**
   *
   * @return the next character or BooleanValue.FALSE
   */
  public Value gzgetc(Env env)
  {
    try {
      int ch = _in.read();

      if (ch >= 0)
        return env.createString(Character.toString((char) ch));
      else
        return BooleanValue.FALSE;
    } catch (IOException e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * Gets a (uncompressed) string of up to 'length' bytes read
   * from the given file pointer. Reading ends when 'length' bytes
   * have been read, on a newline, or on EOF (whichever comes first).
   *
   * @param length
   * @return StringValue
   */
  @ReturnNullAsFalse
  public StringValue gzgets(int length)
  {
    if (_in == null)
      return null;

    UnicodeBuilderValue sbv = new UnicodeBuilderValue();
    int readChar;

    try {
      for (int i = 0; i < length - 1; i++) {
        readChar = _in.read();

        if (readChar >= 0) {
          sbv.append((char) readChar);

          if (readChar == '\n' || readChar == '\r')
            break;
        } else
          break;
      }
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }

    if (sbv.length() > 0)
      return sbv;
    else
      return null;
  }

  /**
   * helper function for ZlibModule.gzfile
   * need to have created a Zlib before calling this
   *
   * @return array of uncompressed lines
   * @throws IOException
   * @throws DataFormatException
   */
  public ArrayValue gzfile()
  {
    Value line;
    int oldLength = 0; 

    ArrayValue array = new ArrayValueImpl();

    try {
      //read in String BuilderValue's initial capacity
      while ((line = gzgets(Integer.MAX_VALUE)) != BooleanValue.FALSE) {
        array.put(line);
      }

      return array;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * same as gzgets but does not stop at '\n' or '\r'
   * @param length
   * @return BinaryValue, an empty BinaryValue if no data read
   * @throws IOException
   * @throws DataFormatException
   */
  public StringValue gzread(Env env, int length)
  {
    StringValue sb = env.createBinaryBuilder();
    int readChar;

    if (_in == null)
      return sb;

    sb.appendReadAll(_in, length);

    return sb;
  }

  /**
   *
   * @return true if eof
   */
  public boolean gzeof()
  {
    if (_isGZIPInputStream)
      return ((GZIPInputStream)_in).isEOS();
    else {
      try {
        _in.mark(1);
        int ch = _in.read();
        _in.reset();
        return (ch == -1);
      } catch (IOException e) {
        throw new QuercusModuleException(e);
      }
    }
  }

  /**
   *
   * @param length
   * @param allowedTags
   * @return next line stripping tags
   * @throws IOException
   * @throws DataFormatException
   */
  @ReturnNullAsFalse
  public StringValue gzgetss(int length,
                             @Optional StringValue allowedTags)
  {
    try {
      if (_in == null)
        return null;

      UnicodeBuilderValue sbv = new UnicodeBuilderValue();
      int readChar;
      for (int i = 0; i < length; i++) {
        readChar = _in.read();
        if (readChar >= 0) {
          sbv.append((char)readChar);
          if (readChar == '\n' || readChar == '\r')
            break;
        } else
          break;
      }
      if (sbv.length() > 0)
        return StringModule.strip_tags(sbv, allowedTags);
      else
        return null;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * resets to the beginning of the file stream.
   *
   * @return always true
   * @throws IOException
   */
  public boolean gzrewind()
  {
    try {
      if (_in != null)
        _in.close();

      _in = getGZIPInputStream();
    }
    catch (IOException e)
    {
      throw QuercusModuleException.create(e);
    }
    return true;
  }

  /**
   * helper function to open file for reading when necessary
   *
   * @throws IOException
   */
  protected InputStream getGZIPInputStream()
    throws IOException
  {
    try {
      _isGZIPInputStream = true;
      return new GZIPInputStream(_fileValue.getPath().openRead());
    }
    catch (IOException e) {
      //GZIPInputStream throws an Exception if not in gzip format
      //else open uncompressed stream
      _isGZIPInputStream = false;

      return _fileValue.getPath().openRead();
    }
  }

  /**
   * Helper function to retrieve the filemode closest to the end
   * Note: PHP5 unexpectedly fails when 'x' is the mode.
   *
   * XXX todo: toss a warning if '+' is found (gzip cannot be open for
   *  both reading and writing at the same time)
   *
   */
  private String getFileMode(String input)
  {
    String modifier = "";
    String filemode = input.substring(0, 1);

    for (int i = 1; i < input.length(); i++)
    {
      char ch = input.charAt(i);
      switch (ch) {
        case 'r':
          filemode = "r";
          break;
        case 'w':
          filemode = "w";
          break;
        case 'a':
          filemode = "a";
          break;
        case 'b':
          modifier = "b";
          break;
        case 't':
          modifier = "t";
          break;
      }
    }
    return filemode + modifier;
  }

  /**
   * Helper function to retrieve the compression level like how PHP5 does it.
   *         1. finds the compression level nearest to the end and returns that
   */
  private int getCompressionLevel(String input)
  {
    for (int i = input.length() - 1; i >= 0; i--)
    {
      char ch = input.charAt(i);
      if (ch >= '0' && ch <= '9')
        return ch - '0';
    }
    return Deflater.DEFAULT_COMPRESSION;
  }

  /**
   * Helper function to retrieve the compression strategy like how PHP5 does it.
   *     1. finds the compression strategy nearest to the end and returns that
   */
  private int getCompressionStrategy(String input)
  {
    for (int i = input.length() - 1; i >= 0; i--)
    {
      char ch = input.charAt(i);
      switch (ch) {
        case 'f':
          return Deflater.FILTERED;
        case 'h':
          return Deflater.HUFFMAN_ONLY;
      }
    }
    return Deflater.DEFAULT_STRATEGY;
  }

  public String toString()
  {
    return "Zlib[]";
  }
}
