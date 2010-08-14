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
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.OutputModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamImplOutputStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * PHP Zlib
 */
public class ZlibModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(ZlibModule.class.getName());
  private static final L10N L = new L10N(ZlibModule.class);

  public static final int FORCE_GZIP = 0x1;
  public static final int FORCE_DEFLATE = 0x2;

  private int _dbg;

  public String []getLoadedExtensions()
  {
    return new String[] { "zlib" };
  }

  /**
   *
   * @param env
   * @param fileName
   * @param mode
   * @param useIncludePath always on
   * @return Zlib
   */
  @ReturnNullAsFalse
  public static BinaryStream gzopen(Env env,
                                    StringValue fileName,
                                    String mode,
                                    @Optional("false") boolean useIncludePath)
  {
    String filemode = getFileMode(mode);
    int compressionLevel = getCompressionLevel(mode);
    int compressionStrategy = getCompressionStrategy(mode);

    Object val = FileModule.fopen(env, fileName, mode, useIncludePath, null);

    if (val == null)
      return null;

    try {
      int ch = filemode.charAt(0);

      if (ch == 'r') {
        BinaryInput is = (BinaryInput) val;
        return new ZlibInputStream(env, is);
      }
      else if (ch == 'w') {
        return new ZlibOutputStream(((BinaryOutput) val).getOutputStream(),
                                    compressionLevel,
                                    compressionStrategy);
      }
      else if (ch == 'a') {
        return new ZlibOutputStream(((BinaryOutput) val).getOutputStream(),
                                    compressionLevel,
                                    compressionStrategy);
      }
      else if (ch == 'x') {
        return new ZlibOutputStream(((BinaryOutput) val).getOutputStream(),
                                    compressionLevel,
                                    compressionStrategy);
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }

    return null;
  }

  /**
   *
   * @param env
   * @param fileName
   * @param useIncludePath
   * @return array of uncompressed lines from fileName
   */
  @ReturnNullAsFalse
  public static ArrayValue gzfile(Env env,
                                  StringValue fileName,
                                  @Optional boolean useIncludePath)
  {
    BinaryInput is = (BinaryInput) gzopen(env, fileName, "r", useIncludePath);

    if (is == null)
      return null;

    try {
      ArrayValue result = new ArrayValueImpl();

      StringValue line;
      while ((line = is.readLine(Integer.MAX_VALUE)) != null
          && line.length() > 0)
        result.put(line);

      return result;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      is.close();
    }
  }

  public static Value ob_gzhandler(Env env, StringValue buffer, int state)
  {
    return OutputModule.ob_gzhandler(env, buffer, state);
  }

  /**
   * outputs uncompressed bytes directly to browser, writes a warning message
   *   if an error has occured
   * Note: PHP5 is supposed to print an error message but it doesn't do it
   *
   * @param env
   * @param fileName
   * @param useIncludePath
   * @return number of bytes read from file, or FALSE if an error occurred
   */
  public static Value readgzfile(Env env,
                                 StringValue fileName,
                                 @Optional boolean useIncludePath)
  {
    BinaryInput is = (BinaryInput) gzopen(env, fileName, "r", useIncludePath);

    if (is == null)
      return BooleanValue.FALSE;

    try {
      return LongValue.create(env.getOut().writeStream(is.getInputStream()));
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      is.close();
    }
  }

  /**
   * Writes a string to the gzip stream.
   */
  public static int gzwrite(@NotNull BinaryOutput os,
                            InputStream is,
                            @Optional("0x7fffffff") int length)
  {
    if (os == null)
      return 0;

    try {
      return os.write(is, length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   *
   * @param env
   * @param zp
   * @param s
   * @param length
   * @return alias of gzwrite
   */
  public int gzputs(Env env,
                    @NotNull BinaryOutput os,
                    InputStream is,
                    @Optional("0x7ffffff") int length)
  {
    if (os == null)
      return 0;

    try {
      return os.write(is, length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Closes the stream.
   */
  public boolean gzclose(@NotNull BinaryStream os)
  {
    if (os == null)
      return false;
    
    os.close();

    return true;
  }

  /**
   * Returns true if the GZip stream is ended.
   */
  public boolean gzeof(@NotNull BinaryStream binaryStream)
  {
    if (binaryStream == null)
      return true;

    return binaryStream.isEOF();
  }

  /**
   * Reads a character from the stream.
   */
  public static Value gzgetc(Env env, @NotNull BinaryInput is)
  {
    if (is == null)
      return BooleanValue.FALSE;

    try {
      int ch = is.read();

      if (ch < 0)
        return BooleanValue.FALSE;
      else {
        StringValue sb = env.createBinaryBuilder(1);

        sb.appendByte(ch);

        return sb;
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Reads a chunk of data from the gzip stream.
   */
  public Value gzread(@NotNull BinaryInput is, int length)
  {
    if (is == null)
      return BooleanValue.FALSE;

    try {
      return is.read(length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Reads a line from the input stream.
   */
  public static Value gzgets(Env env,
                              @NotNull BinaryInput is,
                              int length)
  {
    return FileModule.fgets(env, is, length);
  }

  /**
   * Reads a line from the zip stream, stripping tags.
   */
  public static Value gzgetss(Env env,
                              @NotNull BinaryInput is,
                              int length,
                              @Optional Value allowedTags)
  {
    return FileModule.fgetss(env, is, length, allowedTags);
  }

  /**
   * Rewinds the stream to the very beginning
   */
  public boolean gzrewind(@NotNull BinaryStream binaryStream)
  {
    if (binaryStream == null)
      return false;

    return binaryStream.setPosition(0);
  }

  /**
   * Set stream position to the offset
   * @param offset absolute position to set stream to
   * @param whence if set, changes the interpretation of offset like fseek
   * @return 0 upon success, else -1 for error
   */
  public int gzseek(@NotNull BinaryStream binaryStream,
                    long offset,
                    @Optional("FileModule.SEEK_SET") int whence)
  {
    if (binaryStream == null)
      return -1;

    if (binaryStream.seek(offset, whence) == -1)
      return -1;

    return 0;
  }

  /**
   * Gets the current position in the stream
   * @return the position in the stream, or FALSE for error
   */
  public Value gztell(@NotNull BinaryStream binaryStream)
  {
    if (binaryStream == null)
      return BooleanValue.FALSE;
    return LongValue.create(binaryStream.getPosition());
  }

  /**
   * Prints out the remaining data in the stream to stdout
   */
  public Value gzpassthru(Env env, @NotNull BinaryInput is)
  {
    WriteStream out = env.getOut();
    TempBuffer tempBuf = TempBuffer.allocate();
    byte[] buffer = tempBuf.getBuffer();

    int length = 0;
    try {
      int sublen = is.read(buffer, 0, buffer.length);
      while (sublen > 0) {
        out.write(buffer, 0, sublen);
        length += sublen;
        sublen = is.read(buffer, 0, buffer.length);
      }

      return LongValue.create(length);

    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;

    } finally {
      TempBuffer.free(tempBuf);
    }
  }

  /**
   * Returns the encoding type both allowed by the server
   *   and supported by the user's browser.
   */
  public Value zlib_get_coding_type(Env env)
  {
    String ini = env.getIniString("zlib.output_compression");

    if (ini == null || ini == "")
      return BooleanValue.FALSE;

    //zlib_get_coding_type can also be an integer > 0
    if (! ini.equalsIgnoreCase("on")) {
      int ch = ini.charAt(0);

      if (ch < '0' || ch > '9')
        return BooleanValue.FALSE;
    }

    ServerArrayValue sav = new ServerArrayValue(env);
    Value val = sav.get(env.createString("HTTP_ACCEPT_ENCODING"));

    if (!val.isset())
      return BooleanValue.FALSE;

    String s = val.toString();
    if (s.contains("gzip"))
      return env.createString("gzip");
    else if (s.contains("deflate"))
      return env.createString("deflate");
    else
      return BooleanValue.FALSE;
  }

  /**
   * compresses data using zlib
   *
   * @param data
   * @param level (default is Deflater.DEFAULT_COMPRESSION)
   * @return compressed string
   */
  public Value gzcompress(Env env,
                          InputStream data,
                          @Optional("6") int level)
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();

    Deflater deflater = null;
    
    try {
      deflater = new Deflater(level, true);
      Adler32 crc = new Adler32();

      boolean isFinished = false;

      StringValue out = env.createLargeBinaryBuilder();

      buffer[0] = (byte) 0x78;

      if (level <= 1)
        buffer[1] = (byte) 0x01;
      else if (level < 6)
        buffer[1] = (byte) 0x5e;
      else if (level == 6)
        buffer[1] = (byte) 0x9c;
      else
        buffer[1] = (byte) 0xda;

      out.append(buffer, 0, 2);

      int len;
      while (! isFinished) {
        while (! isFinished && deflater.needsInput()) {
          len = data.read(buffer, 0, buffer.length);

          if (len > 0) {
            crc.update(buffer, 0, len);
            deflater.setInput(buffer, 0, len);
          }
          else {
            isFinished = true;
            deflater.finish();
          }
        }

        while ((len = deflater.deflate(buffer, 0, buffer.length)) > 0) {
          out.append(buffer, 0, len);
        }
      }

      long value = crc.getValue();
    
      buffer[0] = (byte) (value >> 24);
      buffer[1] = (byte) (value >> 16);
      buffer[2] = (byte) (value >> 8);
      buffer[3] = (byte) (value >> 0);

      out.append(buffer, 0, 4);

      return out;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    } finally {
      TempBuffer.free(tempBuf);

      if (deflater != null)
        deflater.end();
    }
  }

  /**
   *
   * @param data
   * @param length (maximum length of string returned)
   * @return uncompressed string
   */
  public Value gzuncompress(Env env,
                            InputStream is,
                            @Optional("0") long length)
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();

    InflaterInputStream in = null;
    try {
      if (length == 0)
        length = Long.MAX_VALUE;

      in = new InflaterInputStream(is);

      StringValue sb = env.createLargeBinaryBuilder();

      int len;
      while ((len = in.read(buffer, 0, buffer.length)) >= 0) {
        sb.append(buffer, 0, len);
      }

      return sb;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    } finally {
      TempBuffer.free(tempBuf);

      try {
        if (in != null)
          in.close();
      } catch (Exception e) {
      }
    }
  }
  
  /**
   *
   * @param level
   * @return compressed using DEFLATE algorithm
   */
  public Value gzdeflate(Env env, InputStream data,
                         @Optional("6") int level)
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();
    Deflater deflater = null;
    
    try {
      deflater = new Deflater(level, true);

      boolean isFinished = false;
      TempStream out = new TempStream();

      int len;
      while (! isFinished) {
        if (! isFinished && deflater.needsInput()) {
          len = data.read(buffer, 0, buffer.length);

          if (len > 0)
            deflater.setInput(buffer, 0, len);
          else {
            isFinished = true;
            deflater.finish();
          }
        }

        while ((len = deflater.deflate(buffer, 0, buffer.length)) > 0) {
          out.write(buffer, 0, len, false);
        }
      }
      deflater.end();

      return env.createBinaryString(out.getHead());

    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    } finally {
      TempBuffer.free(tempBuf);

      if (deflater != null)
        deflater.end();
    }
  }

  /**
   * @param data compressed using Deflate algorithm
   * @param length of data to decompress
   *
   * @return uncompressed string
   */
  public Value gzinflate(Env env,
                         InputStream data,
                         @Optional("0") int length)
  {
    if (length <= 0)
      length = Integer.MAX_VALUE;
    
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();
    Inflater inflater = null;

    try {
      inflater = new Inflater(true);
      StringValue sb = env.createBinaryBuilder();

      while (true) {
        int sublen = Math.min(length, buffer.length);
          
        sublen = data.read(buffer, 0, sublen);

        if (sublen > 0) {
          inflater.setInput(buffer, 0, sublen);
          length -= sublen;
          
          int inflatedLength;
          while ((inflatedLength = inflater.inflate(buffer, 0, sublen)) > 0) {
            sb.append(buffer, 0, inflatedLength);
          }
        }
        else
          break;
      }

      return sb;
    } catch (OutOfMemoryError e) {
      env.warning(e);
      return BooleanValue.FALSE;
    } catch (Exception e) {
      env.warning(e);
      return BooleanValue.FALSE;
    } finally {
      TempBuffer.free(tempBuf);

      if (inflater != null)
        inflater.end();
    }
  }

  /**
   *
   * Compresses data using the Deflate algorithm, output is
   * compatible with gzwrite's output
   *
   * @param data compressed with the Deflate algorithm
   * @param level Deflate compresion level [0-9]
   * @param encodingMode CRC32 trailer is not written if encoding mode
   *    is FORCE_DEFLATE, default is to write CRC32
   * @return StringValue with gzip header and trailer
   */
  public Value gzencode(Env env, InputStream is,
                        @Optional("6") int level,
                        @Optional("1") int encodingMode)
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte[] buffer = tempBuf.getBuffer();

    TempStream ts = new TempStream();
    StreamImplOutputStream out = new StreamImplOutputStream(ts);

    ZlibOutputStream gzOut = null;

    try {
      gzOut = new ZlibOutputStream(out, level,
                                   Deflater.DEFAULT_STRATEGY,
                                   encodingMode);

      int len;
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        gzOut.write(buffer, 0, len);
      }
      gzOut.close();

      StringValue sb = env.createBinaryBuilder();
      for (TempBuffer ptr = ts.getHead(); ptr != null; ptr = ptr.getNext())
        sb.append(ptr.getBuffer(), 0, ptr.getLength());

      return sb;
    } catch (IOException e) {
      throw QuercusModuleException.create(e);
    } finally {
      TempBuffer.free(tempBuf);

      ts.destroy();

      if (gzOut != null)
        gzOut.close();
    }
  }

  /**
   * Helper function to retrieve the filemode closest to the end
   * Note: PHP5 unexpectedly fails when 'x' is the mode.
   *
   * XXX todo: toss a warning if '+' is found
   *    (gzip cannot be open for both reading and writing at the same time)
   *
   */
  private static String getFileMode(String input)
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
   * Helper function to retrieve the compression level
   *     - finds the compression level nearest to the end and returns that
   */
  private static int getCompressionLevel(String input)
  {
    for (int i = input.length() - 1; i >= 0; i--) {
      char ch = input.charAt(i);
      
      if (ch >= '0' && ch <= '9')
        return ch - '0';
    }
    
    return Deflater.DEFAULT_COMPRESSION;
  }

  /**
   * Helper function to retrieve the compression strategy.
   *     - finds the compression strategy nearest to the end and returns that
   */
  private static int getCompressionStrategy(String input)
  {
    for (int i = input.length() - 1; i >= 0; i--) {
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
}
