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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Quercus Oracle OCI-Lob object oriented API.
 */
public class OracleOciLob {
  private static final Logger log = Logger.getLogger(
      OracleOciLob.class.getName());
  private static final L10N L = new L10N(OracleOciLob.class);

  // The large object
  private Object _lob;

  // Cache the connection. See writeTemporary()
  private Oracle _conn;

  // Current position (seek)
  private long _currentPointer;

  // OCI Lob type: OCI_D_FILE, OCI_D_LOB or OCI_D_ROWID
  private int _type;

  // This could be refactored in two classes: OracleOciBlob and OracleOciClob
  private OutputStream _outputStream;
  private Writer _writer;

  // Cache classes and methods for oracle.sql.BLOB and oracle.sql.CLOB
  private static Class classOracleBLOB;
  private static Class classOracleCLOB;
  private static Method createTemporaryBLOB;
  private static Method createTemporaryCLOB;
  private static int BLOB_DURATION_CALL;
  private static int BLOB_DURATION_SESSION;
  private static int CLOB_DURATION_CALL;
  private static int CLOB_DURATION_SESSION;

  static {
    try {
      classOracleBLOB = Class.forName("oracle.sql.BLOB");
      classOracleCLOB = Class.forName("oracle.sql.CLOB");
      createTemporaryBLOB = classOracleBLOB.getDeclaredMethod(
          "createTemporary",
          new Class[]{Connection.class,
              Boolean.TYPE,
              Integer.TYPE});
      createTemporaryCLOB = classOracleCLOB.getDeclaredMethod(
          "createTemporary",
          new Class[]{Connection.class,
              Boolean.TYPE,
              Integer.TYPE});
      BLOB_DURATION_CALL = classOracleBLOB.getDeclaredField(
          "DURATION_CALL").getInt(null);
      BLOB_DURATION_SESSION = classOracleBLOB.getDeclaredField(
          "DURATION_SESSION").getInt(null);
      CLOB_DURATION_CALL = classOracleCLOB.getDeclaredField(
          "DURATION_CALL").getInt(null);
      CLOB_DURATION_SESSION = classOracleCLOB.getDeclaredField(
          "DURATION_SESSION").getInt(null);
    } catch (Exception e) {
      log.log(Level.FINER, L.l(
          "Unable to load LOB classes or methods for "
              + "oracle.sql.BLOB and oracle.sql.CLOB."));
    }
  }


  /**
   * Constructor for OracleOciLob
   *
   * @param type one of the following types:
   *
   * OCI_D_FILE - a FILE descriptor
   *
   * OCI_D_LOB - a LOB descriptor
   *
   * OCI_D_ROWID - a ROWID descriptor
   */
  OracleOciLob(Oracle conn, int type)
  {
    _conn = conn;
    _lob = null;
    _currentPointer = 0;
    _type = type;
    _outputStream = null;
    _writer = null;
  }

  /**
   * Appends data from the large object to another large object
   */
  public boolean append(Env env,
                        OracleOciLob lobFrom)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          return appendInternalBlob(env, lobFrom);
        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          return appendInternalClob(env, lobFrom);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Closes LOB descriptor
   */
  public boolean close(Env env)
  {
    try {

      _currentPointer = 0;

      if (_outputStream != null) {
        _outputStream.close();
        _outputStream = null;
      }

      if (_writer != null) {
        _writer.close();
        _writer = null;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Tests for end-of-file on a large object's descriptor
   */
  public boolean eof(Env env)
  {
    try {

      long length = -1;

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          length = blob.length();
        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          length = clob.length();
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      if (_currentPointer == length) {
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Erases a specified portion of the internal LOB data
   *
   * @return the actual number of characters/bytes erased or
   * FALSE in case of error.
   */
  @ReturnNullAsFalse
  public LongValue erase(Env env,
                         @Optional("0") long offset,
                         @Optional("-1") long length)
  {
    try {

      if (offset < 0) {
        offset = 0;
      }

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;

          if (_outputStream != null)
            _outputStream.close();

          _outputStream = blob.setBinaryStream(offset);
          long blobLength = blob.length();
          if ((length < 0) || (offset + length > blobLength)) {
            length = blobLength - offset;
          }
          long remaining = length;
          byte []zeroBuffer = new byte[128];
          while (remaining >= 128) {
            _outputStream.write(zeroBuffer, 0, 128);
            remaining -= 128;
          }
          if (remaining > 0) {
            _outputStream.write(zeroBuffer, 0, (int) remaining);
          }

          _outputStream.close();
          _outputStream = null;

        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;

          if (_writer != null)
            _writer.close();

          _writer = clob.setCharacterStream(offset);

          long clobLength = clob.length();
          if ((length < 0) || (offset + length > clobLength)) {
            length = clobLength - offset;
          }
          long remaining = length;
          char []spaceBuffer = new char[128];
          while (remaining >= 128) {
            _writer.write(spaceBuffer, 0, 128);
            remaining -= 128;
          }
          if (remaining > 0) {
            _writer.write(spaceBuffer, 0, (int) remaining);
          }

          _writer.close();
          _writer = null;
        }
        _currentPointer = offset + length;
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      if (length > 0) {
        return LongValue.create(length);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Exports LOB's contents to a file
   */
  public boolean export(Env env,
                        Path file,
                        @Optional("0") long start,
                        @Optional("-1") long length)
  {
    try {

      WriteStream writeStream = file.openWrite();

      if (_lob instanceof Blob) {
        Blob blob = (Blob) _lob;
        InputStream is = blob.getBinaryStream();
        is.skip(start);
        writeStream.writeStream(is);
        is.close();
      } else if (_lob instanceof Clob) {
        Clob clob = (Clob) _lob;
        Reader reader = clob.getCharacterStream();
        reader.skip(start);
        writeStream.writeStream(reader);
        reader.close();
      } else {
        writeStream.close();
        return false;
      }

      writeStream.close();
      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Flushes/writes buffer of the LOB to the server
   */
  public boolean flush(Env env,
                       @Optional("-1") int flag)
  {
    try {

      if (_outputStream != null) {
        _outputStream.flush();
      }

      if (_writer != null) {
        _writer.flush();
      }

      if (flag == OracleModule.OCI_LOB_BUFFER_FREE) {
        close(env);
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }


  /**
   * Frees resources associated with the LOB descriptor
   */
  public boolean free(Env env)
  {
    try {

      _lob = null;

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns current state of buffering for the large object
   */
  public boolean getBuffering(Env env)
  {
    // XXX: we assume buffering is always turned on.
    return true;
  }

  /**
   * Imports file data to the LOB
   */
  @Name("import")
  public boolean q_import(Env env, Path file)
  {
    try {

      ReadStream readStream = file.openRead();

      if (_lob instanceof Blob) {
        Blob blob = (Blob) _lob;
        blob.truncate(0);

        if (_outputStream != null)
          _outputStream.close();

        _outputStream = blob.setBinaryStream(0);
        long nbytes;
        byte []buffer = new byte[128];
        while ((nbytes = readStream.read(buffer, 0, 128)) > 0) {
          _outputStream.write(buffer, 0, (int) nbytes);
          _currentPointer += nbytes;
        }

        _outputStream.close();
        _outputStream = null;
      } else if (_lob instanceof Clob) {
        Clob clob = (Clob) _lob;
        clob.truncate(0);

        if (_writer != null)
          _writer.close();

        _writer = clob.setCharacterStream(0);
        long nchars;
        char []buffer = new char[128];
        while ((nchars = readStream.read(buffer, 0, 128)) > 0) {
          _writer.write(buffer, 0, (int) nchars);
          _currentPointer += nchars;
        }

        _writer.close();
        _writer = null;
      } else {
        readStream.close();
        return false;
      }

      readStream.close();
      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns large object's contents
   */
  @ReturnNullAsFalse
  public Object load(Env env)
  {
    try {
      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          return readInternalBlob(env, -1);
        } else if (_lob instanceof Clob) {
          return readInternalClob(env, -1);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Reads part of the large object
   */
  @ReturnNullAsFalse
  public Object read(Env env,
                     long length)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          return readInternalBlob(env, length);
        } else if (_lob instanceof Clob) {
          return readInternalClob(env, length);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Moves the internal pointer to the beginning of the large object
   */
  public boolean rewind(Env env)
  {
    return seek(env, 0, OracleModule.OCI_SEEK_SET);
  }


  /**
   * Saves data to the large object
   */
  public boolean save(Env env,
                      @NotNull String data,
                      @Optional("0") long offset)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          if (_outputStream != null) {
            _outputStream.close();
          }
          _outputStream = blob.setBinaryStream(offset);
          _outputStream.write(data.getBytes());
          _outputStream.close();
          _outputStream = null;

        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          if (_writer != null) {
            _writer.close();
          }
          _writer = clob.setCharacterStream(offset);
          _writer.write(data);
          _writer.close(); // php/4408
          _writer = null;
        }
        _currentPointer = offset + data.length();
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Alias of import()
   */
  public boolean saveFile(Env env,
                          Path file)
  {
    return q_import(env, file);
  }

  /**
   * Sets the internal pointer of the large object
   */
  public boolean seek(Env env,
                      long offset,
                      @Optional("-1") int whence)
  {
    try {

      switch (whence) {
      case OracleModule.OCI_SEEK_SET:
        _currentPointer = offset;
        break;
      case OracleModule.OCI_SEEK_END:
        long length = 0;
        if (_lob instanceof Blob) {
          length = ((Blob) _lob).length();
        } else if (_lob instanceof Clob) {
          length = ((Clob) _lob).length();
        } else {
          L.l("Unable to determine large object's "
              + "length trying to seek with OCI_SEEK_END");
          return false;
        }
        _currentPointer = length + offset;
        break;
      default: // OCI_SEEK_CUR
        _currentPointer += offset;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Changes current state of buffering for the large object
   */
  public boolean setBuffering(Env env,
                              boolean onOff)
  {
    // XXX: we assume buffering is always turned on.
    return true;
  }


  /**
   * Sets the underlying LOB
   */
  protected void setLob(Object lob) {
    _lob = lob;
  }

  /**
   * Returns size of large object
   */
  @ReturnNullAsFalse
  public LongValue size(Env env)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          return LongValue.create(blob.length());
        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          return LongValue.create(clob.length());
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Returns current position of internal pointer of large object
   */
  @ReturnNullAsFalse
  public LongValue tell(Env env)
  {
    try {

      return LongValue.create(_currentPointer);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  public String toString() {

    String typeName = "UNKNOWN";

    switch (_type) {
    case OracleModule.OCI_D_FILE:
      typeName = "OCI_D_FILE";
      break;
    case OracleModule.OCI_D_LOB:
      typeName = "OCI_D_LOB";
      break;
    case OracleModule.OCI_D_ROWID:
      typeName = "OCI_D_ROWID";
      break;
    }

    return "OracleOciLob(" + typeName + ")";
  }

  /**
   * Truncates large object
   */
  public boolean truncate(Env env,
                          @Optional("0") long length)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          blob.truncate(length);
        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          clob.truncate(length);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Writes data to the large object
   */
  @ReturnNullAsFalse
  public LongValue write(Env env,
                         String data,
                         @Optional("-1") long length)
  {
    try {

      long dataLength = data.length();

      if ((length < 0) || (length > dataLength)) {
        length = dataLength;
      }

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        long written = 0;
        if (_lob instanceof Blob) {
          Blob blob = (Blob) _lob;
          if (_outputStream == null) {
            _outputStream = blob.setBinaryStream(0);
          }

          _outputStream.write(data.getBytes());
        } else if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          if (_writer == null)
            _writer = clob.setCharacterStream(0);

          _writer.write(data);
        }

        _currentPointer += length;
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Writes temporary large object
   */
  public boolean writeTemporary(Env env,
                                String data,
                                @Optional("-1") int lobType)
  {
    try {

      if (_type != OracleModule.OCI_D_LOB) {
        L.l("Unable to write a temporary LOB into a non-lob object");
        return false;
      }

      if (lobType == OracleModule.OCI_TEMP_BLOB) {
        _lob = createTemporaryBLOB.invoke(classOracleBLOB,
                                          new Object[] {_conn,
                                                        true,
                                                        BLOB_DURATION_SESSION});
      } else {
        _lob = createTemporaryCLOB.invoke(classOracleCLOB,
                                          new Object[] {_conn,
                                                        true,
                                                        CLOB_DURATION_SESSION});
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Alias of export()
   */
  public boolean writeToFile(Env env,
                             Path file,
                             @Optional("0") long start,
                             @Optional("-1") long length)
  {
    return export(env, file, start, length);
  }

  private boolean appendInternalBlob(Env env,
                                     OracleOciLob lobFrom)
  {
    try {

      Blob blob = (Blob) _lob;
      long blobLength = blob.length();
      if (_currentPointer != blobLength) {
        if (_outputStream != null) {
          _outputStream.close();
        }
        _outputStream = blob.setBinaryStream(blobLength);
        _currentPointer = blobLength;
      }

      Blob blobFrom = (Blob) lobFrom;
      InputStream is = blobFrom.getBinaryStream();

      long nbytes;
      byte []buffer = new byte[128];
      while ((nbytes = is.read(buffer)) > 0) {
        _outputStream.write(buffer, 0, (int) nbytes);
        _currentPointer += nbytes;
      }

      is.close();
      // Keep this output stream open to be reused.

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  private boolean appendInternalClob(Env env,
                                     OracleOciLob lobFrom)
  {
    try {

      Clob clob = (Clob) _lob;
      long clobLength = clob.length();
      if (_currentPointer != clobLength) {
        if (_writer != null)
          _writer.close();

        _writer = clob.setCharacterStream(clobLength);
        _currentPointer = clobLength;
      }

      Clob clobFrom = (Clob) lobFrom;
      Reader reader = clobFrom.getCharacterStream();

      long nchars;
      char []buffer = new char[128];
      while ((nchars = reader.read(buffer)) > 0) {
        _writer.write(buffer, 0, (int) nchars);
        _currentPointer += nchars;
      }

      reader.close();
      // Keep this writer open to be reused.

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  private StringValue readInternalBlob(Env env, long length)
  {
    try {
      StringValue bb = env.createBinaryBuilder();

      Blob blob = (Blob) _lob;
      InputStream is = blob.getBinaryStream();
      is.skip(_currentPointer);

      if (length < 0)
        length = Integer.MAX_VALUE;
      
      bb.appendReadAll(is, length);

      is.close();

      return bb;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  private StringValue readInternalClob(Env env,
                                       long length)
  {
    try {
      StringValue sb = env.createUnicodeBuilder();

      Clob clob = (Clob) _lob;
      Reader reader = clob.getCharacterStream();
      reader.skip(_currentPointer);

      if (length < 0)
        length = Integer.MAX_VALUE;

      sb.append(reader, length);

      reader.close();

      return sb;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }
}
