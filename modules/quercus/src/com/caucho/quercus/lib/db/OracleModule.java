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

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Quercus oracle routines.
 *
 * NOTE from php.net:
 *
 * "...
 * These functions allow you to access Oracle 10, Oracle 9, Oracle 8
 * and Oracle 7 databases using the Oracle Call Interface (OCI). They
 * support binding of PHP variables to Oracle placeholders, have full
 * LOB, FILE and ROWID support, and allow you to use user-supplied
 * define variables.
 *
 * Requirements
 *
 * You will need the Oracle client libraries to use this extension.
 * Windows users will need libraries with version at least 10
 * to use the php_oci8.dll.
 *
 * ..."
 *
 */
public class OracleModule extends AbstractQuercusModule {
  private static final Logger log = Log.open(OracleModule.class);
  private static final L10N L = new L10N(OracleModule.class);

  // WARNING: Do not change order or constant values.
  // They are mapped to oracle types below.
  // See arrayPhpToOracleType.
  public static final int OCI_B_BFILE                    = 0x01;
  public static final int OCI_B_CFILEE                   = 0x02;
  public static final int OCI_B_CLOB                     = 0x03;
  public static final int OCI_B_BLOB                     = 0x04;
  public static final int OCI_B_ROWID                    = 0x05;
  public static final int OCI_B_CURSOR                   = 0x06;
  public static final int OCI_B_NTY                      = 0x07;
  public static final int OCI_B_BIN                      = 0x08;
  public static final int OCI_DTYPE_FILE                 = 0x09;
  public static final int OCI_DTYPE_LOB                  = 0x0A;
  public static final int OCI_DTYPE_ROWID                = 0x0B;
  public static final int OCI_D_FILE                     = 0x0C;
  public static final int OCI_D_LOB                      = 0x0D;
  public static final int OCI_D_ROWID                    = 0x0E;
  public static final int OCI_SYSDATE                    = 0x0F;
  public static final int OCI_TEMP_CLOB                  = 0x10;
  public static final int OCI_TEMP_BLOB                  = 0x11;
  public static final int SQLT_BFILEE                    = 0x12;
  public static final int SQLT_CFILEE                    = 0x13;
  public static final int SQLT_CLOB                      = 0x14;
  public static final int SQLT_BLOB                      = 0x15;
  public static final int SQLT_RDD                       = 0x16;
  public static final int SQLT_NTY                       = 0x17;
  public static final int SQLT_LNG                       = 0x18;
  public static final int SQLT_LBI                       = 0x19;
  public static final int SQLT_BIN                       = 0x1A;
  public static final int SQLT_NUM                       = 0x1B;
  public static final int SQLT_INT                       = 0x1C;
  public static final int SQLT_AFC                       = 0x1D;
  public static final int SQLT_CHR                       = 0x1E;
  public static final int SQLT_VCS                       = 0x1F;
  public static final int SQLT_AVC                       = 0x20;
  public static final int SQLT_STR                       = 0x21;
  public static final int SQLT_LVC                       = 0x22;
  public static final int SQLT_FLT                       = 0x23;
  public static final int SQLT_ODT                       = 0x24;
  public static final int SQLT_BDOUBLE                   = 0x25;
  public static final int SQLT_BFLOAT                    = 0x26;
  public static final int SQLT_RSET                      = 0x27;
  public static final int SQLT_FILE                      = 0x28;
  public static final int SQLT_CFILE                     = 0x29;

  // Reserved for future types and extensions
  // 0x30 - 0x4F

  // OCI Control Constants 0x50 - ...
  public static final int OCI_DEFAULT                    = 0x50;
  public static final int OCI_DESCRIBE_ONLY              = 0x51;
  public static final int OCI_COMMIT_ON_SUCCESS          = 0x52;
  public static final int OCI_EXACT_FETCH                = 0x53;
  public static final int OCI_FETCHSTATEMENT_BY_COLUMN   = 0x54;
  public static final int OCI_FETCHSTATEMENT_BY_ROW      = 0x55;
  public static final int OCI_ASSOC                      = 0x56;
  public static final int OCI_NUM                        = 0x57;
  public static final int OCI_BOTH                       = 0x58;
  public static final int OCI_RETURN_NULLS               = 0x59;
  public static final int OCI_RETURN_LOBS                = 0x5A;
  public static final int OCI_SYSOPER                    = 0x5B;
  public static final int OCI_SYSDBA                     = 0x5C;
  public static final int OCI_LOB_BUFFER_FREE            = 0x5D;
  public static final int OCI_SEEK_SET                   = 0x5E;
  public static final int OCI_SEEK_CUR                   = 0x5F;
  public static final int OCI_SEEK_END                   = 0x6A;


  // Cache class oracle.jdbc.OracleTypes to be used below.
  private static Class classOracleTypes;

  // Map php to oracle type
  private static int []arrayPhpToOracleType;

  static {
    try {
      classOracleTypes = Class.forName("oracle.jdbc.OracleTypes");

      arrayPhpToOracleType = new int[] {
        -1,
        classOracleTypes.getDeclaredField("BFILE").getInt(null), // OCI_B_BFILE
        -1, // OCI_B_CFILEE
        classOracleTypes.getDeclaredField("CLOB").getInt(null), // OCI_B_CLOB
        classOracleTypes.getDeclaredField("BLOB").getInt(null), // OCI_B_BLOB
        classOracleTypes.getDeclaredField("ROWID").getInt(null), // OCI_B_ROWID

        // OCI_B_CURSOR:
        classOracleTypes.getDeclaredField("CURSOR").getInt(null),
        classOracleTypes.getDeclaredField("OTHER").getInt(null), // OCI_B_NTY
        classOracleTypes.getDeclaredField("RAW").getInt(null), // OCI_B_BIN
        -1, // OCI_DTYPE_FILE
        -1, // OCI_DTYPE_LOB
        -1, // OCI_DTYPE_ROWID
        -1, // OCI_D_FILE
        -1, // OCI_D_LOB
        -1, // OCI_D_ROWID
        // OCI_SYSDATE:
        classOracleTypes.getDeclaredField("TIMESTAMP").getInt(null),
        -1, // OCI_TEMP_CLOB
        -1, // OCI_TEMP_BLOB
        classOracleTypes.getDeclaredField("BFILE").getInt(null), // SQLT_BFILEE
        -1, // SQLT_CFILEE
        classOracleTypes.getDeclaredField("CLOB").getInt(null), // SQLT_CLOB
        classOracleTypes.getDeclaredField("BLOB").getInt(null), // SQLT_BLOB
        classOracleTypes.getDeclaredField("ROWID").getInt(null), // SQLT_RDD
        classOracleTypes.getDeclaredField("OTHER").getInt(null), // SQLT_NTY
        classOracleTypes.getDeclaredField("NUMBER").getInt(null), // SQLT_LNG
        classOracleTypes.getDeclaredField("RAW").getInt(null), // SQLT_LBI
        classOracleTypes.getDeclaredField("RAW").getInt(null), // SQLT_BIN
        classOracleTypes.getDeclaredField("NUMBER").getInt(null), // SQLT_NUM
        classOracleTypes.getDeclaredField("INTEGER").getInt(null), // SQLT_INT
        classOracleTypes.getDeclaredField("CHAR").getInt(null), // SQLT_AFC
        classOracleTypes.getDeclaredField("CHAR").getInt(null), // SQLT_CHR
        classOracleTypes.getDeclaredField("VARCHAR").getInt(null), // SQLT_VCS
        classOracleTypes.getDeclaredField("CHAR").getInt(null), // SQLT_AVC
        classOracleTypes.getDeclaredField("VARCHAR").getInt(null), // SQLT_STR
        // SQLT_LVC:
        classOracleTypes.getDeclaredField("LONGVARCHAR").getInt(null),
        classOracleTypes.getDeclaredField("FLOAT").getInt(null), // SQLT_FLT
        classOracleTypes.getDeclaredField("DATE").getInt(null), // SQLT_ODT
         // SQLT_BDOUBLE:
        classOracleTypes.getDeclaredField("DOUBLE").getInt(null),
        classOracleTypes.getDeclaredField("FLOAT").getInt(null), // SQLT_BFLOAT
        classOracleTypes.getDeclaredField("CURSOR").getInt(null), // SQLT_RSET
        classOracleTypes.getDeclaredField("BFILE").getInt(null), // SQLT_FILE
        -1 // SQLT_CFILE
      };
    } catch (Exception e) {
      L.l("Unable to load Oracle types from oracle.jdbc.OracleTypes. "
          + "Check your Oracle JDBC driver version.");
    }
  }

  public OracleModule()
  {
  }

  /**
   * Returns true for the oracle extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "oci8" };
  }

  /**
   * Binds PHP array to Oracle PL/SQL array by name.
   *
   * oci_bind_array_by_name() binds the PHP array
   * varArray to the Oracle placeholder name, which
   * points to Oracle PL/SQL array. Whether it will
   * be used for input or output will be determined
   * at run-time. The maxTableLength parameter sets
   * the maximum length both for incoming and result
   * arrays. Parameter maxItemLength sets maximum
   * length for array items. If maxItemLength was
   * not specified or equals to -1,
   * oci_bind_array_by_name() will find the longest
   * element in the incoming array and will use it as
   * maximum length for array items. type parameter
   * should be used to set the type of PL/SQL array
   * items. See list of available types below.
   *
   * @param env the PHP executing environment
   * @param stmt the Oracle statement
   * @param name the Oracle placeholder
   * @param varArray the array to be binded
   * @param maxTableLength maximum table length
   * @param maxItemLength maximum item length
   * @param type one of the following types:
   * <br/>
   * SQLT_NUM - for arrays of NUMBER.
   * <br/>
   * SQLT_INT - for arrays of INTEGER
   * (Note: INTEGER it is actually a synonym for
   *  NUMBER(38), but SQLT_NUM type won't work in
   *  this case even though they are synonyms).
   * <br/>
   * SQLT_FLT - for arrays of FLOAT.
   * <br/>
   * SQLT_AFC - for arrays of CHAR.
   * <br/>
   * SQLT_CHR - for arrays of VARCHAR2.
   * <br/>
   * SQLT_VCS - for arrays of VARCHAR.
   * <br/>
   * SQLT_AVC - for arrays of CHARZ.
   * <br/>
   * SQLT_STR - for arrays of STRING.
   * <br/>
   * SQLT_LVC - for arrays of LONG VARCHAR.
   * <br/>
   * SQLT_ODT - for arrays of DATE.
   *
   * @return true on success of false on failure
   */
  public static boolean oci_bind_array_by_name(Env env,
                                               @NotNull OracleStatement stmt,
                                               @NotNull String name,
                                               @NotNull ArrayValue varArray,
                                               @NotNull int maxTableLength,
                                               @Optional("0") int maxItemLength,
                                               @Optional("0") int type)
  {
    try {

      // JDBC underlying connection
      Connection conn = stmt.getJavaConnection();

      // Oracle underlying statement
      PreparedStatement oracleStmt = stmt.getPreparedStatement();

      // Create an oracle.sql.ARRAY object to hold the values
      // oracle.sql.ArrayDescriptor arrayDesc =
      //   oracle.sql.ArrayDescriptor.createDescriptor("number_varray", conn);


      Class clArrayDescriptor = Class.forName("oracle.sql.ArrayDescriptor");

      Method method
        = clArrayDescriptor.getDeclaredMethod(
        "createDescriptor", new Class[] {String.class, Connection.class});

      Object arrayDesc = method.invoke(clArrayDescriptor,
                                       new Object[] {"NUMBER_VARRAY", conn});

      Value []valueArray
        = varArray.valuesToArray(); // int arrayValues[] = {123, 234};

      Object []objectArray = new Object[5]; // {"aaa", "bbb", "ccc"};
      for (int i = 0; i < valueArray.length; i++) {
        Object obj = valueArray[i].toJavaObject();
        objectArray[i] = obj;
      }

//       oracle.sql.ARRAY array
//         = new oracle.sql.ARRAY(arrayDesc, conn, arrayValues);

      Class clARRAY = Class.forName("oracle.sql.ARRAY");

      Constructor constructor = clARRAY.getDeclaredConstructor(new Class[] {
        clArrayDescriptor, Connection.class, Object.class});

      Array oracleArray = (Array) constructor.newInstance(new Object[]
        {arrayDesc, conn, objectArray});

      // Bind array
      // ((oracle.jdbc.OraclePreparedStatement)oracleStmt).setARRAY(1, array);

      // cl = Class.forName("oracle.jdbc.OraclePreparedStatement");

//       method = cl.getDeclaredMethod(
//         "setARRAY",
//         new Class[] {Integer.TYPE, Object[].class});

      if (name == null) {
        return false;
      }

      if (!name.startsWith(":")) {
        name = ":" + name;
      }

      if (name.length() < 2) {
        return false;
      }

      // method.invoke(oracleStmt, new Object[] {name, oracleArray});

      Integer index = stmt.getBindingVariable(name);

      if (index == null)
        return false;

      int i = index.intValue();
      Object object = varArray.toJavaObject();

      if (object instanceof OracleOciCollection) {
        oracleArray = ((OracleOciCollection) object).getCollection();
        oracleStmt.setArray(i, oracleArray);
      } else if (varArray instanceof ArrayValueImpl) {
        // oracleStmt.setObject(i, varArray.getKeyArray());
        // Object objectArray[] = new Object[] {"aaa", "bbb", "ccc"};
        // oracleStmt.setObject(i, objectArray);
        oracleStmt.setArray(i, oracleArray);
      } else {
        oracleStmt.setObject(i, object);
      }

      // drop descriptor ???? 'number_varray' ????

      return true;

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Binds the PHP variable to the Oracle placeholder
   *
   * @param type one of the following types:
   *
   * SQLT_INT - for integers;
   *
   * SQLT_CHR - for VARCHARs;
   *
   * SQLT_RSET - for cursors, that were created before with oci_new_cursor()
   *
   * OCI_B_BFILE (integer)
   *
   *    Used with oci_bind_by_name() when binding BFILEs.
   *
   * OCI_B_CFILEE (integer)
   *
   *    Used with oci_bind_by_name() when binding CFILEs.
   *
   * OCI_B_CLOB (integer)
   *
   *    Used with oci_bind_by_name() when binding CLOBs.
   *
   * OCI_B_BLOB (integer)
   *
   *    Used with oci_bind_by_name() when binding BLOBs.
   *
   * OCI_B_ROWID (integer)
   *
   *    Used with oci_bind_by_name() when binding ROWIDs.
   *
   * OCI_B_CURSOR (integer)
   *
   *    Used with oci_bind_by_name() when binding cursors,
   *    previously allocated with oci_new_descriptor().
   *
   * OCI_B_NTY (integer)
   *
   *    Used with oci_bind_by_name() when binding named data
   *    types. Note: in PHP < 5.0 it was called OCI_B_SQLT_NTY.
   *
   * OCI_B_BIN (integer)
   *
   * SQLT_FILE (integer)
   *
   * SQLT_BFILEE (integer)
   *
   *    The same as OCI_B_BFILE.
   *
   * SQLT_CFILE (integer)
   *
   * SQLT_CFILEE (integer)
   *
   *    The same as OCI_B_CFILEE.
   *
   * SQLT_CLOB (integer)
   *
   *    The same as OCI_B_CLOB.
   *
   * SQLT_BLOB (integer)
   *
   *    The same as OCI_B_BLOB.
   *
   * SQLT_RDD (integer)
   *
   *    The same as OCI_B_ROWID.
   *
   * SQLT_NTY (integer)
   *
   *    The same as OCI_B_NTY.
   *
   * SQLT_LNG (integer)
   *
   *    Used with oci_bind_by_name() to bind LONG values.
   *
   * SQLT_LBI (integer)
   *
   *    Used with oci_bind_by_name() to bind LONG RAW values.
   *
   * SQLT_BIN (integer)
   *
   *    Used with oci_bind_by_name() to bind RAW values.
   *
   */
  public static boolean oci_bind_by_name(Env env,
                                         @NotNull OracleStatement stmt,
                                         @NotNull String placeholderName,
                                         Value variable,
                                         @Optional("0") int maxLength,
                                         @Optional("0") int type)
  {
    if ((type == OCI_B_CFILEE)
        || (type == SQLT_CFILE)
        || (type == SQLT_CFILEE)) {
      throw new UnimplementedException("oci_bind_by_name with CFILE");
    }

    try {

      if (placeholderName == null) {
        return false;
      }

      if (!placeholderName.startsWith(":")) {
        placeholderName = ":" + placeholderName;
      }

      if (placeholderName.length() < 2) {
        return false;
      }

      Integer index = stmt.getBindingVariable(placeholderName);

      if (index == null)
        return false;

      int i = index.intValue();

      PreparedStatement pstmt = stmt.getPreparedStatement();

      CallableStatement callableStmt = (CallableStatement) pstmt;

      // XXX: We could use ParameterMetaData.getParameterMode
      // to figure out which parameters are IN and/or OUT and
      // then setObject and/or registerOutParameter according
      // to the parameter mode. However, getParameterMode()
      // is unsupported from Oracle JDBC drivers (Jun-2006).
      //
      // ParameterMetaData metaData = pstmt.getParameterMetaData();
      //
      // int paramMode = metaData.getParameterMode(i);
      //
      //  switch (paramMode) {
      // case ParameterMetaData.parameterModeInOut:
      //   {
      //     int oracleType = arrayPhpToOracleType[type];
      //     callableStmt.registerOutParameter(i, oracleType);
      //     pstmt.setObject(i, variable.toJavaObject());
      //     break;
      //   }
      // case ParameterMetaData.parameterModeOut:
      //   {
      //     int oracleType = arrayPhpToOracleType[type];
      //     callableStmt.registerOutParameter(i, oracleType);
      //     break;
      //   }
      // default: // case ParameterMetaData.parameterModeIn:
      //   {
      //     pstmt.setObject(i, variable.toJavaObject());
      //     break;
      //   }
      // }

      switch (type) {
      case OCI_B_CURSOR:
      case SQLT_RSET:
        {
          // Assume the most common scenario: OUT parameter mode.
          int oracleType = arrayPhpToOracleType[type];
          callableStmt.registerOutParameter(i, oracleType);
          // Set the cursor's underlying statement (see php/4404).
          Object cursor = variable.toJavaObject();
          if ((cursor == null) || !(cursor instanceof OracleStatement)) {
            return false;
          }
          ((OracleStatement) cursor).setPreparedStatement(callableStmt);
          break;
        }
      case OCI_B_BFILE:   // BFILE
      case SQLT_BFILEE:   // ...
      case SQLT_FILE:     // ...
      case SQLT_BLOB:     // BLOB
      case OCI_B_BLOB:    // ...
      case SQLT_CLOB:     // CLOB
      case OCI_B_CLOB:    // ...
      case OCI_B_ROWID:   // ROWID
      case SQLT_RDD:      // ...
        {
          // Assume the most common scenario: OUT parameter mode.
          int oracleType = arrayPhpToOracleType[type];
          callableStmt.registerOutParameter(i, oracleType);
          Object ociLob = variable.toJavaObject();
          if ((ociLob == null) || !(ociLob instanceof OracleOciLob)) {
            return false;
          }
          stmt.setOutParameter((OracleOciLob) ociLob);
          break;
        }
      default:
        {
          // Assume the most common scenario: IN parameter mode.

          // XXX: Check the spec. if there is a case where the
          // variable would not be initialized yet
          // stmt.putByNameVariable(placeholderName, variable);
          Object object = variable.toJavaObject();
          if (object instanceof OracleOciCollection) {
            object = ((OracleOciCollection) object).getCollection();
          }
          pstmt.setObject(i, object);
        }
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      try {
        stmt.resetBindingVariables();
        stmt.resetByNameVariables();
      } catch (Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }
    }

    return false;
  }

  /**
   * Cancels reading from cursor
   */
  public static boolean oci_cancel(Env env,
                                   @NotNull OracleStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Closes Oracle connection
   */
  public static boolean oci_close(Env env,
                                  @NotNull Oracle conn)
  {
    if (conn == null)
      conn = getConnection(env);

    if (conn != null) {
      if (conn == getConnection(env))
        env.removeSpecialValue("caucho.oracle");

      conn.close(env);

      return true;
    }
    else
      return false;
  }

  /**
   * Commits outstanding statements
   */
  public static boolean oci_commit(Env env,
                                   @NotNull Oracle conn)
  {
    try {
      return conn.commit();
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Establishes a connection to the Oracle server
   */
  public static Value oci_connect(Env env,
                                  @NotNull String username,
                                  @NotNull String password,
                                  @Optional String db,
                                  @Optional String charset,
                                  @Optional("0") int sessionMode)
  {
    // Note:  The second and subsequent calls to oci_connect() with the
    // same parameters will return the connection handle returned from
    // the first call. This means that queries issued against one handle
    // are also applied to the other handles, because they are the same
    // handle. (source: php.net)

    if (!((charset == null) || charset.length() == 0)) {
      throw new UnimplementedException("oci_connect with charset");
    }

    if (sessionMode == OCI_DEFAULT
        || sessionMode == OCI_SYSOPER
        || sessionMode == OCI_SYSDBA) {
      throw new UnimplementedException("oci_connect with session mode");
    }

    return connectInternal(env, true, username, password, db,
                           charset, sessionMode);
  }

  /**
   * Uses a PHP variable for the define-step during a SELECT
   */
  public static boolean oci_define_by_name(Env env,
                                           @NotNull OracleStatement stmt,
                                           @NotNull String columnName,
                                           @NotNull @Reference Value variable,
                                           @Optional("0") int type)
  {
    // Example:
    //
    //  $stmt = oci_parse($conn, "SELECT id, data FROM test");
    //
    //  /* the define MUST be done BEFORE oci_execute! */
    //
    //  oci_define_by_name($stmt, "ID", $myid);
    //  oci_define_by_name($stmt, "DATA", $mydata);
    //
    //  oci_execute($stmt, OCI_DEFAULT);
    //
    //  while ($row = oci_fetch($stmt)) {
    //     echo "id:" . $myid . "\n";
    //     echo "data:" . $mydata . "\n";
    //  }

    try {
      stmt.putByNameVariable(columnName, variable);
      return true;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns the last error found
   */
  @ReturnNullAsFalse
  public static String oci_error(Env env,
                                 @Optional Value resource)
  {
    if (resource instanceof DefaultValue)
      return null;
    
    JdbcConnectionResource conn = null;

    if (resource == null) {
      ConnectionInfo connectionInfo
        = (ConnectionInfo) env.getSpecialValue("caucho.oracle");

      if (connectionInfo != null) {
        conn = connectionInfo.getConnection();
      }
    } else {
      Object object = resource.toJavaObject();

      if (object instanceof Oracle) {
        conn = ((Oracle) object).validateConnection();
      } else {
        conn = ((OracleStatement) object).validateConnection();
      }
    }

    return conn.getErrorMessage();
  }

  /**
   * Executes a statement
   */
  public static boolean oci_execute(Env env,
                                    @NotNull OracleStatement stmt,
                                    @Optional("0") int mode)
  {
    try {

      //  Scenarios for oci_execute.
      //
      //
      //  1. Simple query: oci_parse / oci_execute
      //
      //  $query = 'SELECT * FROM TEST';
      //
      //  $stmt = oci_parse($conn, $query);
      //
      //  oci_execute($stmt, OCI_DEFAULT);
      //
      //  $result = oci_fetch_array($stid, OCI_BOTH);
      //
      //
      //  2. Define by name: oci_parse / oci_define_by_name / oci_execute
      //
      //  $stmt = oci_parse($conn, "SELECT id, data FROM test");
      //
      //  /* the define MUST be done BEFORE oci_execute! */
      //
      //  oci_define_by_name($stmt, "ID", $myid);
      //  oci_define_by_name($stmt, "DATA", $mydata);
      //
      //  oci_execute($stmt, OCI_DEFAULT);
      //
      //  while ($row = oci_fetch($stmt)) {
      //     echo "id:" . $myid . "\n";
      //     echo "data:" . $mydata . "\n";
      //  }
      //
      //
      //  3. Cursors: oci_new_cursor / oci_parse /
      //     oci_bind_by_name / oci_execute($stmt) / oci_execute($cursor)
      //
      //  $stmt = oci_parse($conn,
      //  "CREATE OR REPLACE PACKAGE cauchopkgtestoci AS ".
      //  "TYPE refcur IS REF CURSOR; ".
      //  "PROCEDURE testproc (var_result out cauchopkgtestoci.refcur); ".
      //  "END cauchopkgtestoci; ");
      //
      //  oci_execute($stmt);
      //
      //  $stmt = oci_parse($conn,
      //  "CREATE OR REPLACE PACKAGE BODY cauchopkgtestoci IS ".
      //  "PROCEDURE testproc (var_result out cauchopkgtestoci.refcur) IS ".
      //  "BEGIN ".
      //  "OPEN var_result FOR SELECT data FROM caucho.test; ".
      //  "END testproc; ".
      //  "END; ");
      //
      //  oci_execute($stmt);
      //
      //  $curs = oci_new_cursor($conn);
      //
      //  $stmt = oci_parse($conn,
      //     "begin cauchopkgtestoci.testproc(:dataCursor); end;");
      //
      //  oci_bind_by_name($stmt, "dataCursor", $curs, 255, SQLT_RSET);
      //
      //  oci_execute($stmt);
      //
      //  oci_execute($curs);
      //
      //  while ($data = oci_fetch_row($curs)) {
      //     var_dump($data);
      //  }

      // Get the underlying JDBC connection.
      Connection conn = stmt.getJavaConnection();

      // Large Objects can not be used in auto-commit mode.
      conn.setAutoCommit(false);

      // Use the underlying callable statement to check different scenarios.
      CallableStatement callableStatement = stmt.getCallableStatement();

      // Check for case (3) oci_execute($cursor);
      // A previous statement has been executed and holds the result set.
      Object cursorResult = null;
      try {
        cursorResult = callableStatement.getObject(1);
        if ((cursorResult != null) && (cursorResult instanceof ResultSet)) {
          ResultSet rs = (ResultSet) cursorResult;
          stmt.setResultSet(rs);
          return true;
        }
      } catch (Exception e) {
        // We assume this is not case (3). No error.
      }

      // Case (1) or executing a more complex query.
      stmt.execute(env);

      OracleOciLob ociLob = stmt.getOutParameter();
      if (ociLob != null) {
        // Ex: java.sql.Clob outParameter = callableStatement.getClob(1);
        ociLob.setLob(callableStatement.getObject(1));
      }

      // Fetch and assign values to corresponding binded variables
      // This is necessary for LOB support and
      // should be reworked calling a private fetch method instead.
      // oci_fetch(env, stmt);

      if (mode == OCI_COMMIT_ON_SUCCESS) {
        conn.commit();
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);

      try {
        stmt.resetBindingVariables();
        stmt.resetByNameVariables();
      } catch (Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }

      return false;
    }
  }

  /**
   * Fetches all rows of result data into an array
   */
  @ReturnNullAsFalse
  public static ArrayValue oci_fetch_all(Env env,
                                         @NotNull OracleStatement stmt,
                                         @NotNull Value output,
                                         @Optional int skip,
                                         @Optional int maxrows,
                                         @Optional int flags)
  {
    JdbcResultResource resource = null;

    ArrayValueImpl newArray = new ArrayValueImpl();

    try {
      if (stmt == null)
        return null;

      resource = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      ArrayValue value
        = resource.fetchArray(env, JdbcResultResource.FETCH_ASSOC);

      int curr = 0;

      if (maxrows == 0)
        maxrows = Integer.MAX_VALUE / 2;

      while (value != null && curr < maxrows) {
        newArray.put(LongValue.create(curr), value);

        curr++;

        value = resource.fetchArray(env, JdbcResultResource.FETCH_ASSOC);
      }
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }

    return newArray;
  }

  /**
   * Returns the next row from the result data as an
   * associative or numeric array, or both
   */
  @ReturnNullAsFalse
  public static ArrayValue oci_fetch_array(Env env,
                                           @NotNull OracleStatement stmt,
                                           @Optional("-1") int mode)
  {
    if (stmt == null)
      return null;

    if (mode == OCI_RETURN_LOBS)
      throw new UnimplementedException("oci_fetch_array with OCI_RETURN_LOBS");

    if (mode == OCI_RETURN_NULLS)
      throw new UnimplementedException("oci_fetch_array with OCI_RETURN_NULLS");

    try {

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      switch (mode) {
      case OCI_ASSOC:
        return resource.fetchArray(env, JdbcResultResource.FETCH_ASSOC);
      case OCI_NUM:
        return resource.fetchArray(env, JdbcResultResource.FETCH_NUM);
      default: // case OCI_BOTH:
        return resource.fetchArray(env, JdbcResultResource.FETCH_BOTH);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the next row from the result data as an associative array
   */
  @ReturnNullAsFalse
  public static ArrayValue oci_fetch_assoc(Env env,
                                           @NotNull OracleStatement stmt)
  {
    try {

      if (stmt == null)
        return null;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);
      
      ArrayValue arrayValue = resource.fetchArray(
        env, JdbcResultResource.FETCH_ASSOC);

      return arrayValue;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the next row from the result data as an object
   */
  public static Value oci_fetch_object(Env env,
                                       @NotNull OracleStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);
      
      return resource.fetchObject(env);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the next row from the result data as a numeric array
   */
  @ReturnNullAsFalse
  public static ArrayValue oci_fetch_row(Env env,
                                         @NotNull OracleStatement stmt)
  {
    try {

      if (stmt == null)
        return null;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);
      
      return resource.fetchArray(env, JdbcResultResource.FETCH_NUM);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetches the next row into result-buffer
   */
  public static boolean oci_fetch(Env env,
                                  @NotNull OracleStatement stmt)
  {
    try {

      if (stmt == null)
        return false;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      Value result = resource.fetchArray(env, JdbcResultResource.FETCH_BOTH);

      stmt.setResultBuffer(result);

      if (!(result instanceof ArrayValue)) {
        return false;
      }

      ArrayValue arrayValue = (ArrayValue) result;

      for (
        Map.Entry<String, Value> entry : stmt.getByNameVariables().entrySet()
        ) {
        String fieldName = entry.getKey();
        Value var = entry.getValue();

        Value newValue = arrayValue.get(StringValue.create(fieldName));
        var.set(newValue);
      }

      // See oci_num_rows()
      stmt.increaseFetchedRows();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      try {
        stmt.resetByNameVariables();
      } catch (Exception ex2) {
        log.log(Level.FINE, ex2.toString(), ex2);
      }
      return false;
    }
  }

  /**
   * Checks if the field is NULL
   *
   * @param stmt oracle statement
   * @param fieldNameOrNumber field's index (1-based) or it's name
   * @return TRUE if the field is null or FALSE otherwise.
   */
  public static boolean oci_field_is_null(Env env,
                                          @NotNull OracleStatement stmt,
                                          @NotNull Value fieldNameOrNumber)
  {
    if (stmt == null)
      return false;

    try {

      ResultSet rs = stmt.getResultSet();

      JdbcResultResource resource
        = new JdbcResultResource(env, null, rs, null);

      int fieldNumber = resource.getColumnNumber(fieldNameOrNumber, 1);

      if (fieldNumber < 0)
        return false;

      ResultSetMetaData metaData = rs.getMetaData();

      return metaData.isNullable(fieldNumber + 1)
             == ResultSetMetaData.columnNullable;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns the name of a field from the statement
   */
  public static Value oci_field_name(Env env,
                                     @NotNull OracleStatement stmt,
                                     @NotNull int fieldNumber)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      return resource.getFieldName(env, fieldNumber);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Tell the precision of a field
   */
  @ReturnNullAsFalse
  public static LongValue oci_field_precision(Env env,
                                              @NotNull OracleStatement stmt,
                                              @NotNull int field)
  {
    if (stmt == null)
      return null;

    try {

      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      int precision = metaData.getPrecision(field);
      return LongValue.create(precision);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Tell the scale of the field
   */
  @ReturnNullAsFalse
  public static LongValue oci_field_scale(Env env,
                                          @NotNull OracleStatement stmt,
                                          @NotNull int field)
  {
    if (stmt == null)
      return null;

    try {

      ResultSet rs = stmt.getResultSet();
      ResultSetMetaData metaData = rs.getMetaData();

      int precision = metaData.getScale(field);
      return LongValue.create(precision);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns field's size
   *
   * @param stmt oracle statement
   * @param fieldNameOrNumber field's index (1-based) or it's name
   * @return the field's size
   */
  public static Value oci_field_size(Env env,
                                     @NotNull OracleStatement stmt,
                                     @Optional("-1") Value fieldNameOrNumber)
  {
    try {

      if (stmt == null)
        return BooleanValue.FALSE;

      ResultSet rs = stmt.getResultSet();

      JdbcResultResource resource
        = new JdbcResultResource(env, null, rs, null);

      int fieldNumber = resource.getColumnNumber(fieldNameOrNumber, 1);

      return resource.getFieldLength(env, fieldNumber);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Tell the raw Oracle data type of the field
   *
   * @param field the field number (1-based)
   */
  public static int oci_field_type_raw(Env env,
                                       @NotNull OracleStatement stmt,
                                       int field)
  {
    try {

      if (stmt == null)
        return -1;

      if (field <= 0)
        return -1;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      Value typeV = resource.getJdbcType(--field);

      if (typeV instanceof LongValue) {

        int type = typeV.toInt();

        switch (type) {

        case Types.BLOB:
        case Types.LONGVARCHAR:
        case Types.LONGVARBINARY:
          type = SQLT_BLOB;
          break;

        case Types.CLOB:
          type = SQLT_CLOB;
          break;

        case Types.BIGINT:
        case Types.BIT:
        case Types.BOOLEAN:
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.INTEGER:
        case Types.NUMERIC:
        case Types.REAL:
        case Types.SMALLINT:
        case Types.TINYINT:
          type = SQLT_NUM;
          break;

        default:
          type = SQLT_CHR;
        }

        return type;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return -1;
  }

  /**
   * Returns field's data type
   */
  public static Value oci_field_type(Env env,
                                     @NotNull OracleStatement stmt,
                                     int fieldNumber)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      return resource.getFieldType(env, fieldNumber);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   *  Frees all resources associated with statement or cursor
   */
  public static boolean oci_free_statement(Env env,
                                           @NotNull OracleStatement stmt)
  {
    try {

      stmt.close();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Enables or disables internal debug output
   */
  public static void oci_internal_debug(Env env,
                                        @NotNull int onoff)
  {
    throw new UnimplementedException("oci_internal_debug");
  }

  /**
   * Copies large object
   */
  public static boolean oci_lob_copy(Env env,
                                     @NotNull OracleOciLob lobTo,
                                     @NotNull OracleOciLob lobFrom,
                                     @Optional("-1") int length)
  {
    try {

      return lobTo.save(env, lobFrom.read(env, length).toString(), 0);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Compares two LOB/FILE locators for equality
   */
  public static boolean oci_lob_is_equal(Env env,
                                         @NotNull OracleOciLob lob1,
                                         @NotNull OracleOciLob lob2)
  {
    try {

      return lob1.equals(lob2);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Allocates new collection object
   */
  @ReturnNullAsFalse
  public static OracleOciCollection oci_new_collection(Env env,
                                                       @NotNull Oracle conn,
                                                       @NotNull String tdo,
                                                       @Optional String schema)
  {
    try {

      String typeName = tdo;

      if ((schema != null) && (schema.length() > 0)) {
        typeName = schema + "." + tdo;
      }

      // XXX: Is this case ever possible?
      // StructDescriptor structDesc
      //   = StructDescriptor.createDescriptor(typeName, jdbcConn);

      // JDBC underlying connection
      Connection jdbcConn = conn.getJavaConnection();

      // Oracle underlying statement
      // PreparedStatement oracleStmt = stmt.getPreparedStatement();

      // Use reflection
      // ArrayDescriptor arrayDesc
      //   = ArrayDescriptor.createDescriptor(typeName, jdbcConn);

      Class clArrayDescriptor = Class.forName("oracle.sql.ArrayDescriptor");

      Method method = clArrayDescriptor.getDeclaredMethod(
        "createDescriptor",
        new Class[] {String.class,
                     Connection.class});

      Object arrayDesc = method.invoke(clArrayDescriptor,
                                       new Object[] {typeName, jdbcConn});

      if (arrayDesc != null) {
        return new OracleOciCollection(jdbcConn, arrayDesc);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Establishes a new connection to the Oracle server
   */
  public static Value oci_new_connect(Env env,
                                      @NotNull String username,
                                      @NotNull String password,
                                      @Optional String db,
                                      @Optional String charset,
                                      @Optional("0") int sessionMode)
  {
    if ((sessionMode == OCI_DEFAULT)
        || (sessionMode == OCI_SYSOPER)
        || (sessionMode == OCI_SYSDBA)) {
      log.warning(L.l("oci_new_connect with session mode '{0}'", sessionMode));
    }

    return connectInternal(env, false, username, password, db,
                           charset, sessionMode);
  }

  /**
   * Allocates and returns a new cursor (statement handle)
   */
  @ReturnNullAsFalse
  public static OracleStatement oci_new_cursor(Env env,
                                               @NotNull Oracle conn)
  {
    try {

      OracleStatement stmt = new OracleStatement(
        (Oracle) conn.validateConnection());

      return stmt;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Initializes a new empty LOB or FILE descriptor
   *
   * @param type one of the following types:
   *
   * OCI_D_FILE - a FILE descriptor
   *
   * OCI_D_LOB - a LOB descriptor
   *
   * OCI_D_ROWID - a ROWID descriptor
   */
  @ReturnNullAsFalse
  public static OracleOciLob oci_new_descriptor(Env env,
                                                @NotNull Oracle conn,
                                                @Optional("-1") int type)
  {
    try {

      if ((type == OCI_D_FILE)
          || (type == OCI_D_LOB)
          || (type == OCI_D_ROWID)) {

        OracleOciLob oracleLob = new OracleOciLob(conn, type);

        return oracleLob;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   *  Returns the number of result columns in a statement
   */
  public static Value oci_num_fields(Env env,
                                     @NotNull OracleStatement stmt)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      JdbcResultResource resource
        = new JdbcResultResource(env, null, stmt.getResultSet(), null);

      return LongValue.create(resource.getFieldCount());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns number of rows affected during statement execution
   *
   * Note:  This function does not return number of rows selected!
   * For SELECT statements this function will return the number of rows,
   * that were fetched to the buffer with oci_fetchxxxx() functions.
   */
  @ReturnNullAsFalse
  public static LongValue oci_num_rows(Env env,
                                       @NotNull OracleStatement stmt)
  {
    try {

      if (stmt == null)
        return null;

      // JdbcResultResource resource = new JdbcResultResource(
      // null, stmt.getResultSet(), null);

      // return LongValue.create(resource.getNumRows());

      return LongValue.create(stmt.getFetchedRows());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Prepares Oracle statement for execution
   */
  @ReturnNullAsFalse
  public static OracleStatement oci_parse(Env env,
                                          @NotNull Oracle conn,
                                          String query)
  {
    try {
      // XXX: Rework this.
      // Enclose the query with "begin ...; end;" so any regular statement
      // or stored procedure call can be executed with a CallableStatement.
      query = query.trim();

      String lowerCaseQuery = query.toLowerCase();

      if (lowerCaseQuery.startsWith("insert")
          || lowerCaseQuery.startsWith("update")
          || lowerCaseQuery.startsWith("delete")) {
        if (!lowerCaseQuery.startsWith("begin ")) {
          query = "begin " + query;
        }

        if (!lowerCaseQuery.endsWith(";")) {
          query += ";";
        }

        if (!lowerCaseQuery.endsWith(" end;")) {
          query += " end;";
        }
      }

      // Make the PHP query a JDBC like query replacing
      // (:mydata -> ?) with question marks.
      // Store binding names for future reference (see oci_execute)
      String regex = ":[a-zA-Z0-9_]+";
      String jdbcQuery = query.replaceAll(regex, "?");
      OracleStatement pstmt = conn.prepare(env, env.createString(jdbcQuery));

      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(query);
      int i = 0;
      while (matcher.find()) {
        String group = matcher.group();
        pstmt.putBindingVariable(group, new Integer(++i));
      }

      return pstmt;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Changes password of Oracle's user
   */
  public static boolean oci_password_change(Env env,
                                            @NotNull Oracle conn,
                                            @NotNull String username,
                                            @NotNull String oldPassword,
                                            @NotNull String newPassword)
  {
    try {

      // XXX: When is oldPassword used?

      if (conn == null)
        return false;

      OracleStatement oracleStmt;

      oracleStmt = oci_parse(
        env, conn, "ALTER USER " + username + " IDENTIFIED BY " + newPassword);

      oci_execute(env, oracleStmt, 0);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Connect to an Oracle database using a persistent connection
   */
  public static Value oci_pconnect(Env env,
                                   @NotNull String username,
                                   @NotNull String password,
                                   @Optional String db,
                                   @Optional String charset,
                                   @Optional("0") int sessionMode)
  {
    if (!((charset == null) || charset.length() == 0)) {
      throw new UnimplementedException("oci_pconnect with charset");
    }

    if ((sessionMode == OCI_DEFAULT)
        || (sessionMode == OCI_SYSOPER)
        || (sessionMode == OCI_SYSDBA)) {
      throw new UnimplementedException("oci_pconnect with session mode");
    }

    return connectInternal(
      env, true, username, password, db, charset, sessionMode);
  }

  /**
   * Returns field's value from the fetched row
   */
  public static Value oci_result(Env env,
                                 @NotNull OracleStatement stmt,
                                 @NotNull Value field)
  {
    try {
      if (stmt == null)
        return BooleanValue.FALSE;

      Value result = stmt.getResultBuffer();

      return ((ArrayValueImpl)result).get(field);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Rolls back outstanding transaction
   */
  public static Value oci_rollback(Env env,
                                   @NotNull Oracle conn)
  {
    try {
      return BooleanValue.create(conn.rollback());
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns server version
   */
  @ReturnNullAsFalse
  public static String oci_server_version(Env env,
                                          @NotNull Oracle conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getServerInfo();
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sets number of rows to be prefetched
   */
  public static boolean oci_set_prefetch(Env env,
                                         @NotNull OracleStatement stmt,
                                         @Optional("1") int rows)
  {
    try {

      if (stmt == null)
        return false;

      PreparedStatement pstmt = stmt.getPreparedStatement();
      pstmt.setFetchSize(rows);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns the type of an OCI statement
   */
  public static String oci_statement_type(Env env,
                                          @NotNull OracleStatement stmt)
  {
    return stmt.getStatementType();
  }

  /**
   * Alias of oci_bind_by_name()
   */
  public static boolean ocibindbyname(Env env,
                                      @NotNull OracleStatement stmt,
                                      @NotNull String variable,
                                      @NotNull Value value,
                                      @Optional("0") int maxLength,
                                      @Optional("0") int type)
  {
    return oci_bind_by_name(env, stmt, variable, value, maxLength, type);
  }

  /**
   * Alias of oci_cancel()
   */
  public static boolean ocicancel(Env env,
                                  @NotNull OracleStatement stmt)
  {
    return oci_cancel(env, stmt);
  }

  /**
   * Alias of OCI-Lob->close
   */
  public static Value ocicloselob(Env env,
                                  @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicloselob");
  }

  /**
   * Alias of OCI-Collection->append
   */
  public static Value ocicollappend(Env env,
                                    @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollappend");
  }

  /**
   * Alias of OCI-Collection->assign
   */
  public static Value ocicollassign(Env env,
                                    @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollassign");
  }

  /**
   * Alias of OCI-Collection->assignElem
   */
  public static Value ocicollassignelem(Env env,
                                        @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollassignelem");
  }

  /**
   * Alias of OCI-Collection->getElem
   */
  public static Value ocicollgetelem(Env env,
                                     @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollgetelem");
  }

  /**
   * Alias of OCI-Collection->max
   */
  public static Value ocicollmax(Env env,
                                 @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollmax");
  }

  /**
   * Alias of OCI-Collection->size
   */
  public static Value ocicollsize(Env env,
                                  @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicollsize");
  }

  /**
   * Alias of OCI-Collection->trim
   */
  public static Value ocicolltrim(Env env,
                                  @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocicolltrim");
  }

  /**
   * Alias of oci_field_is_null()
   */
  public static boolean ocicolumnisnull(Env env,
                                        @NotNull OracleStatement stmt,
                                        @NotNull Value field)
  {
    return oci_field_is_null(env, stmt, field);
  }

  /**
   * Alias of oci_field_name()
   */
  public static Value ocicolumnname(Env env,
                                    @NotNull OracleStatement stmt,
                                    @NotNull int fieldNumber)
  {
    return oci_field_name(env, stmt, fieldNumber);
  }

  /**
   * Alias of oci_field_precision()
   */
  public static Value ocicolumnprecision(Env env,
                                         @NotNull OracleStatement stmt,
                                         @NotNull int field)
  {
    return oci_field_precision(env, stmt, field);
  }

  /**
   * Alias of oci_field_scale()
   */
  public static Value ocicolumnscale(Env env,
                                     @NotNull OracleStatement stmt,
                                     @NotNull int field)
  {
    return oci_field_scale(env, stmt, field);
  }

  /**
   * Alias of oci_field_size()
   */
  public static Value ocicolumnsize(Env env,
                                    @NotNull OracleStatement stmt,
                                    @Optional Value field)
  {
    return oci_field_size(env, stmt, field);
  }

  /**
   * Alias of oci_field_type()
   */
  public static Value ocicolumntype(Env env,
                                    @NotNull OracleStatement stmt,
                                    @Optional int fieldNumber)
  {
    return oci_field_type(env, stmt, fieldNumber);
  }

  /**
   * Alias of oci_field_type_raw()
   */
  public static int ocicolumntyperaw(Env env,
                                     @NotNull OracleStatement stmt,
                                     @Optional int field)
  {
    return oci_field_type_raw(env, stmt, field);
  }

  /**
   * Alias of oci_commit()
   */
  public static boolean ocicommit(Env env,
                                  @NotNull Oracle conn)
  {
    return oci_commit(env, conn);
  }

  /**
   * Alias of oci_define_by_name()
   */
  public static boolean ocidefinebyname(Env env,
                                        @NotNull OracleStatement stmt,
                                        @NotNull String columnName,
                                        @NotNull Value variable,
                                        @Optional("0") int type)
  {
    return oci_define_by_name(env, stmt, columnName, variable, type);
  }

  /**
   * Alias of oci_error()
   */
  @ReturnNullAsFalse
  public static String ocierror(Env env,
                                @Optional Value resource)
  {
    return oci_error(env, resource);
  }

  /**
   * Alias of oci_execute()
   */
  public static boolean ociexecute(Env env,
                                   @NotNull OracleStatement stmt,
                                   @Optional("0") int mode)
  {
    return oci_execute(env, stmt, mode);
  }

  /**
   * Alias of oci_fetch()
   */
  public static boolean ocifetch(Env env,
                                 @NotNull OracleStatement stmt)
  {
    return oci_fetch(env, stmt);
  }

  /**
   * Fetches the next row into an array
   */
  public static Value ocifetchinto(Env env,
                                 @NotNull OracleStatement stmt,
                                 @Reference Value result,
                                 @Optional("-1") int mode)
  {
    if (mode == -1)
      mode = OCI_NUM;
    
    ArrayValue array = oci_fetch_array(env, stmt, mode);

    if (array != null) {
      result.set(array);
      return LongValue.create(array.getSize());
    }
    else {
      return BooleanValue.FALSE;
    }
  }

  /**
   * Alias of oci_fetch_all()
   */
  public static Value ocifetchstatement(Env env,
                                        @NotNull OracleStatement stmt,
                                        @NotNull Value output,
                                        @Optional int skip,
                                        @Optional int maxrows,
                                        @Optional int flags)
  {
    return oci_fetch_all(env, stmt, output, skip, maxrows, flags);
  }

  /**
   * Alias of OCI-Collection->free
   */
  public static Value ocifreecollection(Env env,
                                        @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocifreecollection");
  }

  /**
   * Alias of oci_free_statement()
   */
  public static boolean ocifreecursor(Env env,
                                      @NotNull OracleStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Alias of OCI-Lob->free
   */
  public static Value ocifreedesc(Env env,
                                  @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocifreedesc");
  }

  /**
   * Alias of oci_free_statement()
   */
  public static boolean ocifreestatement(Env env,
                                         @NotNull OracleStatement stmt)
  {
    return oci_free_statement(env, stmt);
  }

  /**
   * Alias of oci_internal_debug()
   */
  public static void ociinternaldebug(Env env,
                                      @NotNull int onoff)
  {
    oci_internal_debug(env, onoff);
  }

  /**
   * Alias of OCI-Lob->load
   */
  public static Value ociloadlob(Env env,
                                 @NotNull Oracle conn)
  {
    throw new UnimplementedException("ociloadlob");
  }

  /**
   * Alias of oci_close()
   */
  public static boolean ocilogoff(Env env,
                                  @NotNull Oracle conn)
  {
    return oci_close(env, conn);
  }

  /**
   * Alias of oci_connect()
   */
  public static Value ocilogon(Env env,
                               @NotNull String username,
                               @NotNull String password,
                               @Optional String db,
                               @Optional String charset,
                               @Optional("0") int sessionMode)
  {
    return oci_connect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_new_collection()
   */
  @ReturnNullAsFalse
  public static OracleOciCollection ocinewcollection(Env env,
                                                     @NotNull Oracle conn,
                                                     @NotNull String tdo,
                                                     @Optional String schema)
  {
    return oci_new_collection(env, conn, tdo, schema);
  }

  /**
   * Alias of oci_new_cursor()
   */
  @ReturnNullAsFalse
  public static OracleStatement ocinewcursor(Env env,
                                             @NotNull Oracle conn)
  {
    return oci_new_cursor(env, conn);
  }

  /**
   * Alias of oci_new_descriptor()
   */
  public static OracleOciLob ocinewdescriptor(Env env,
                                              @NotNull Oracle conn,
                                              @Optional("-1") int type)
  {
    return oci_new_descriptor(env, conn, type);
  }

  /**
   * Alias of oci_new_connect()
   */
  public static Value ocinlogon(Env env,
                                @NotNull String username,
                                @NotNull String password,
                                @Optional String db,
                                @Optional String charset,
                                @Optional("0") int sessionMode)
  {
    return oci_new_connect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_num_fields()
   */
  public static Value ocinumcols(Env env,
                                 @NotNull OracleStatement stmt)
  {
    return oci_num_fields(env, stmt);
  }

  /**
   * Alias of oci_parse()
   */
  @ReturnNullAsFalse
  public static OracleStatement ociparse(Env env,
                                         @NotNull Oracle conn,
                                         @NotNull String query)
  {
    return oci_parse(env, conn, query);
  }

  /**
   * Alias of oci_pconnect()
   */
  public static Value ociplogon(Env env,
                                @NotNull String username,
                                @NotNull String password,
                                @Optional String db,
                                @Optional String charset,
                                @Optional("0") int sessionMode)
  {
    return oci_pconnect(env, username, password, db, charset, sessionMode);
  }

  /**
   * Alias of oci_result()
   */
  public static Value ociresult(Env env,
                                @NotNull OracleStatement stmt,
                                @NotNull Value field)
  {
    return oci_result(env, stmt, field);
  }

  /**
   * Alias of oci_rollback()
   */
  public static Value ocirollback(Env env,
                                  @NotNull Oracle conn)
  {
    return oci_rollback(env, conn);
  }

  /**
   * Alias of oci_num_rows()
   */
  public static Value ocirowcount(Env env,
                                  @NotNull OracleStatement stmt)
  {
    return oci_num_rows(env, stmt);
  }

  /**
   * Alias of OCI-Lob->save
   */
  public static Value ocisavelob(Env env,
                                 @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocisavelob");
  }

  /**
   * Alias of OCI-Lob->import
   */
  public static Value ocisavelobfile(Env env,
                                     @NotNull Oracle conn)
  {
    throw new UnimplementedException("ocisavelobfile");
  }

  /**
   * Alias of oci_server_version()
   */
  public static String ociserverversion(Env env,
                                        @NotNull Oracle conn)
  {
    return oci_server_version(env, conn);
  }

  /**
   * Alias of oci_set_prefetch()
   */
  public static boolean ocisetprefetch(Env env,
                                       @NotNull OracleStatement stmt,
                                       @Optional("1") int rows)
  {
    return oci_set_prefetch(env, stmt, rows);
  }

  /**
   * Alias of oci_statement_type()
   */
  public static String ocistatementtype(Env env,
                                        @NotNull OracleStatement stmt)
  {
    return oci_statement_type(env, stmt);
  }

  /**
   * Alias of OCI-Lob->export
   */
  public static Value ociwritelobtofile(Env env,
                                        @NotNull Oracle conn)
  {
    throw new UnimplementedException("ociwritelobtofile");
  }

  /**
   * Alias of OCI-Lob->writeTemporary
   */
  public static Value ociwritetemporarylob(Env env,
                                           @NotNull Oracle conn)
  {
    throw new UnimplementedException("ociwritetemporarylob");
  }

  private static Oracle getConnection(Env env)
  {
    Oracle conn = null;

    ConnectionInfo connectionInfo
      = (ConnectionInfo) env.getSpecialValue("caucho.oracle");

    if (connectionInfo != null) {
      // Reuse the cached connection
      conn = connectionInfo.getConnection();
      return conn;
    }

    String driver = "oracle.jdbc.OracleDriver";
    String url = "jdbc:oracle:thin:@localhost:1521";

    conn = new Oracle(env, "localhost", "", "", "", 1521, driver, url);

    env.setSpecialValue("caucho.oracle", conn);

    return conn;
  }

  private static Value connectInternal(Env env,
                                       boolean reuseConnection,
                                       String username,
                                       String password,
                                       String db,
                                       String charset,
                                       int sessionMode)
  {
    String host = "localhost";
    int port = 1521;

    String driver = "oracle.jdbc.OracleDriver";

    String url;

    if (db.indexOf("//") == 0) {
      // db is the url itself: "//db_host[:port]/database_name"
      url = "jdbc:oracle:thin:@" + db.substring(2);
      url = url.replace('/', ':');
    } else {
      url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + db;
    }

    Oracle conn = null;

    ConnectionInfo connectionInfo
      = (ConnectionInfo) env.getSpecialValue("caucho.oracle");

    if (reuseConnection && connectionInfo != null
        && url.equals(connectionInfo.getUrl())) {
      // Reuse the cached connection
      conn = connectionInfo.getConnection();
    } else {
      conn = new Oracle(env, host, username, password, db, port, driver, url);

      if (! conn.isConnected())
        return BooleanValue.FALSE;

      connectionInfo = new ConnectionInfo(url, conn);

      env.setSpecialValue("caucho.oracle", connectionInfo);
    }

    Value value = env.wrapJava(conn);

    return value;
  }

  private static class ConnectionInfo {
    private String _url;
    private Oracle _conn;

    public ConnectionInfo(String url, Oracle conn)
    {
      _url = url;
      _conn = conn;
    }

    public String getUrl()
    {
      return _url;
    }

    public Oracle getConnection()
    {
      return _conn;
    }
  }
}
