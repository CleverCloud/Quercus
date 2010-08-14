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
 * @author Sam
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.FileReadValue;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PDO object oriented API facade.
 */
public class PDOStatement
  implements Iterable<Value>, EnvCleanup
{
  private static final Logger log = Logger.getLogger(
      PDOStatement.class.getName());
  private static final L10N L = new L10N(PDOStatement.class);

  private static final Value[] NULL_VALUES = new Value[0];

  //private static final Value FETCH_FAILURE = new NullValue() {};
  //private static final Value FETCH_EXHAUSTED = new NullValue() {};
  //private static final Value FETCH_CONTINUE = new NullValue() {};
  //private static final Value FETCH_SUCCESS = new NullValue() {};
  
  private static final int FETCH_FAILURE = 0;
  private static final int FETCH_EXHAUSTED = 1;
  private static final int FETCH_CONTINUE = 2;
  private static final int FETCH_SUCCESS = 3;
  
  private int _fetchErrorCode;

  private final Env _env;
  private final PDOError _error;

  private final String _query;

  private Statement _statement;
  private PreparedStatement _preparedStatement;

  private ResultSet _resultSet;
  private ResultSetMetaData _resultSetMetaData;
  private boolean _resultSetExhausted = true;
  private String _lastInsertId;

  private int _fetchMode = PDO.FETCH_BOTH;
  private Value[] _fetchModeArgs = NULL_VALUES;
  private ArrayList<BindColumn> _bindColumns;
  private ArrayList<BindParam> _bindParams;
  private IntMap _parameterNameMap;

  PDOStatement(Env env, Connection conn,
               String query, boolean isPrepared,
               ArrayValue options)
    throws SQLException
  {
    _env = env;
    _error = new PDOError(_env);

    _query = query;

    env.addCleanup(this);

    if (options != null && options.getSize() > 0) {
      _env.notice(L.l("PDOStatement options unsupported"));
    }

    query = parseQueryString(query);

    if (isPrepared) {
      _statement = null;

      int ch;
      if (query.length() > 4
          && ((ch = query.charAt(0)) == 'c' || ch == 'C')
          && ((ch = query.charAt(1)) == 'a' || ch == 'A')
          && ((ch = query.charAt(2)) == 'l' || ch == 'L')
          && ((ch = query.charAt(3)) == 'l' || ch == 'L')) {
        _preparedStatement = conn.prepareCall(query);
      }
      else
        _preparedStatement = conn.prepareStatement(query);

      // php/1s41 - oracle can't handle this
      //_preparedStatement.setEscapeProcessing(false);
    }
    else {
      _preparedStatement = null;

      Statement statement = null;

      try {
        statement = conn.createStatement();
        statement.setEscapeProcessing(false);

        if (statement.execute(query)) {
          _resultSet = statement.getResultSet();
          _resultSetExhausted = false;
        }

        _statement = statement;

        statement = null;

      } finally {
        try {
          if (statement != null)
            statement.close();
        } catch (SQLException e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  // side-effect, updates _parameterNameMap
  private String parseQueryString(String query)
  {
    final int queryLength = query.length();
    StringBuilder parsedQuery = new StringBuilder(queryLength);

    int parameterCount = 0;
    StringBuilder name = null;

    int quote = 0;

    for (int i = 0; i < queryLength + 1; i++) {
      int ch = -1;

      if (i < queryLength)
        ch = query.charAt(i);

      if (ch == '\'' || ch == '"') {
        if (quote == 0)
          quote = ch;
        else if (quote == ch)
          quote = 0;
      }
      else if (quote == 0 && ch == '?') {
        parameterCount++;
      }
      else if (quote == 0 && ch == ':') {
        parameterCount++;
        name = new StringBuilder();
        continue;
      }
      // XXX: check what characters are allowed
      else if (name != null
          && (ch == -1 || !Character.isJavaIdentifierPart(ch))) {
        if (_parameterNameMap == null)
          _parameterNameMap = new IntMap();

        _parameterNameMap.put(name.toString(), parameterCount);

        parsedQuery.append('?');

        name = null;
      }

      if (ch != -1) {
        if (name != null)
          name.append((char) ch);
        else
          parsedQuery.append((char) ch);
      }
    }

    return parsedQuery.toString();
  }

  private boolean advanceResultSet()
  {
    if (_resultSet == null || _resultSetExhausted)
      return false;

    try {
      boolean isNext =  _resultSet.next();

      if (!isNext)
        _resultSetExhausted = true;

      if (!isNext)
        return false;

      if (_bindColumns != null) {
        for (BindColumn bindColumn : _bindColumns)
          if (!bindColumn.bind())
            return false;
      }

      return isNext;
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }
  }

  public boolean bindColumn(
      Value column, @Reference Value var, @Optional("-1") int type)
  {
    if (_bindColumns == null)
      _bindColumns = new ArrayList<BindColumn>();

    try {
      _bindColumns.add(new BindColumn(column, var, type));
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }

    return true;
  }

  public boolean bindParam(Value parameter,
                           @Reference Value variable,
                           @Optional("-1") int dataType,
                           @Optional("-1") int length,
                           @Optional Value driverOptions)
  {
    if (length != -1)
      throw new UnimplementedException("length");

    if (!(driverOptions == null || driverOptions.isNull()))
      throw new UnimplementedException("driverOptions");

    if (dataType == -1)
      dataType = PDO.PARAM_STR;

    boolean isInputOutput = (dataType & PDO.PARAM_INPUT_OUTPUT) != 0;

    if (isInputOutput) {
      dataType = dataType & (~PDO.PARAM_INPUT_OUTPUT);
      if (true) throw new UnimplementedException("PARAM_INPUT_OUTPUT");
    }

    switch (dataType) {
      case PDO.PARAM_BOOL:
      case PDO.PARAM_INT:
      case PDO.PARAM_LOB:
      case PDO.PARAM_NULL:
      case PDO.PARAM_STMT:
      case PDO.PARAM_STR:
        break;

      default:
        _error.warning(L.l("unknown dataType `{0}'", dataType));
        return false;
    }

    if (_bindParams == null)
      _bindParams = new ArrayList<BindParam>();

    BindParam bindParam = new BindParam(
        parameter, variable, dataType, length, driverOptions);

    _bindParams.add(bindParam);

    return true;
  }

  public boolean bindValue(Value parameter,
                           Value value,
                           @Optional("-1") int dataType)
  {
    return bindParam(parameter, value.toValue(), dataType, -1, null);
  }

  /**
   * Closes the current cursor.
   */
  public boolean closeCursor()
  {
    if (_resultSet == null)
      return false;

    ResultSet resultSet = _resultSet;

    _resultSet = null;
    _resultSetMetaData = null;
    _resultSetExhausted = true;
    _lastInsertId = null;

    try {
      resultSet.close();
    }
    catch (SQLException e) {
      _error.error(e);

      return false;
    }

    return true;
  }

  /**
   * Returns the number of columns.
   */
  public int columnCount()
  {
    if (_resultSet == null)
      return 0;

    try {
      return getResultSetMetaData().getColumnCount();
    }
    catch (SQLException e) {
      _error.error(e);

      return 0;
    }
  }

  public BindParam createBindParam(
      Value parameter,
      Value value,
      int dataType,
      int length,
      Value driverOptions)
  {
    return new BindParam(parameter, value, dataType, length, driverOptions);
  }

  public void close()
  {
    cleanup();
  }

  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    ResultSet resultSet = _resultSet;
    Statement statement = _statement;
    PreparedStatement preparedStatement = _preparedStatement;

    _resultSet = null;
    _resultSetMetaData = null;
    _resultSetExhausted = true;
    _lastInsertId = null;
    _statement = null;
    _preparedStatement = null;

    if (resultSet != null)  {
      try {
        resultSet.close();
      }
      catch (SQLException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    if (statement != null)  {
      try {
        statement.close();
      }
      catch (SQLException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    if (preparedStatement != null)  {
      try {
        preparedStatement.close();
      }
      catch (SQLException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String errorCode()
  {
    return _error.errorCode();
  }

  public ArrayValue errorInfo()
  {
    return _error.errorInfo();
  }

  /**
   * Execute the statement.
   *
   * @param inputParameters an array containing input values to correspond to
   * the bound parameters for the statement.
   *
   * @return true for success, false for failure
   */
  public boolean execute(@Optional @ReadOnly Value inputParameters)
  {
    // XXX: s/b to do this with ArrayValue arg, but cannot differentiate between
    // no args and bad arg that isn't an ArrayValue
    ArrayValue parameters;

    if (inputParameters instanceof ArrayValue)
      parameters = (ArrayValue) inputParameters;
    else if (inputParameters instanceof DefaultValue)
      parameters = null;
    else {
      _env.warning(L.l(
          "'{0}' is an unexpected argument, expected ArrayValue",
          inputParameters));
      return false;
    }

    closeCursor();

    try {
      _preparedStatement.clearParameters();
      _preparedStatement.clearWarnings();

      if (parameters != null) {
        for (Map.Entry<Value, Value> entry : parameters.entrySet()) {
          Value key = entry.getKey();

          if (key.isNumberConvertible()) {
            if (! setParameter(key.toInt() + 1, entry.getValue(), -1))
              return false;
          }
          else {
            if (! setParameter(resolveParameter(key), entry.getValue(), -1))
              return false;
          }
        }
      }
      else if (_bindParams != null) {
        for (BindParam bindParam : _bindParams) {
          if (!bindParam.apply())
            return false;
        }
      }

      if (_preparedStatement.execute()) {
        _resultSet = _preparedStatement.getResultSet();
        _resultSetExhausted = false;
      }

      SQLWarning sqlWarning = _preparedStatement.getWarnings();

      if (sqlWarning != null) {
        _error.error(sqlWarning);
        return false;
      }

      return true;
    } catch (SQLException e) {
      _error.error(e);

      return false;
    }
  }

  /**
   * Fetch the next row.
   *
   * @param fetchMode the mode, 0 to use the value
   * set by {@link #setFetchMode}.
   * @return a value, BooleanValue.FALSE if there
   * are no more rows or an error occurs.
   */
  public Value fetch(@Optional int fetchMode,
                     @Optional("-1") int cursorOrientation,
                     @Optional("-1") int cursorOffset)
  {
    if (cursorOrientation != -1)
      throw new UnimplementedException("fetch with cursorOrientation");

    if (cursorOffset != -1)
      throw new UnimplementedException("fetch with cursorOffset");

    return fetchImpl(fetchMode, -1);
  }

  /**
   *
   * @param fetchMode
   * @param columnIndex 0-based column index when fetchMode is FETCH_BOTH
   */
  public Value fetchAll(
      @Optional("0") int fetchMode, @Optional("-1") int columnIndex)
  {
    int effectiveFetchMode;

    if (fetchMode == 0) {
      effectiveFetchMode = _fetchMode;
    }
    else {
      effectiveFetchMode = fetchMode;
    }

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    if (isGroup)
      throw new UnimplementedException("PDO.FETCH_GROUP");

    if (isUnique)
      throw new UnimplementedException("PDO.FETCH_UNIQUE");

    effectiveFetchMode = effectiveFetchMode
        & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    switch (effectiveFetchMode) {
      case PDO.FETCH_COLUMN:
        break;

      case PDO.FETCH_LAZY:
        _error.warning(L.l(
            "PDO::FETCH_LAZY can't be used with PDOStatement::fetchAll()"));
        return BooleanValue.FALSE;

      default:
        if (columnIndex != -1) {
          _error.warning(L.l("unexpected arguments"));
          return BooleanValue.FALSE;
        }
    }

    ArrayValueImpl rows = new ArrayValueImpl();

    while (true) {
      Value value = fetchImpl(effectiveFetchMode, columnIndex);

      if (_fetchErrorCode == FETCH_FAILURE) {
        rows.clear();
        return rows;
      }

      if (_fetchErrorCode == FETCH_EXHAUSTED)
        break;

      if (_fetchErrorCode == FETCH_CONTINUE)
        continue;

      rows.put(value);
    }

    return rows;
  }

  private Value fetchAssoc()
  {
    try {
      if (!advanceResultSet()) {
        _fetchErrorCode = FETCH_EXHAUSTED;
        
        return BooleanValue.FALSE;
      }

      if (_fetchModeArgs.length != 0) {
        _error.notice(L.l("unexpected arguments"));
        
        _fetchErrorCode = FETCH_FAILURE;
        
        return BooleanValue.FALSE;
      }

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        array.put(_env.createString(name), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }
  }

  private Value fetchBoth()
  {
    try {
      if (! advanceResultSet()) {
        _fetchErrorCode = FETCH_EXHAUSTED;
        
        return BooleanValue.FALSE;
      }

      if (_fetchModeArgs.length != 0) {
        _error.notice(L.l("unexpected arguments"));
        
        _fetchErrorCode = FETCH_FAILURE;
        
        return BooleanValue.FALSE;
      }

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        array.put(_env.createString(name), value);
        array.put(LongValue.create(i - 1), value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }
  }

  private Value fetchBound()
  {
    if (!advanceResultSet()) {
      _fetchErrorCode = FETCH_EXHAUSTED;
      
      return BooleanValue.FALSE;
    }

    _fetchErrorCode = FETCH_SUCCESS;
    
    return BooleanValue.TRUE;
  }

  private Value fetchClass()
  {
    String className;
    Value[] ctorArgs;

    if (_fetchModeArgs.length == 0 || _fetchModeArgs.length > 2)
      return fetchBoth();

    className = _fetchModeArgs[0].toString();

    if (_fetchModeArgs.length == 2) {
      if (_fetchModeArgs[1].isArray()) {
        // XXX: inefiicient, but args[1].getValueArray(_env)
        // doesn't handle references
        ArrayValue argsArray = (ArrayValue) _fetchModeArgs[1];

        ctorArgs = new Value[argsArray.getSize()];

        int i = 0;

        for (Value key : argsArray.keySet())
          ctorArgs[i++] = argsArray.getVar(key);
      }
      else
        return fetchBoth();
    }
    else
      ctorArgs = NULL_VALUES;

    return fetchObject(className, ctorArgs);
  }

  /**
   * @param column 0-based column number
   */
  public Value fetchColumn(@Optional int column)
  {
    if (!advanceResultSet()) {
      _fetchErrorCode = FETCH_EXHAUSTED;
      
      return BooleanValue.FALSE;
    }

    if (column < 0 && _fetchModeArgs.length > 0)
      column = _fetchModeArgs[0].toInt();

    try {
      if (column < 0 || column >= getResultSetMetaData().getColumnCount()) {
        _fetchErrorCode = FETCH_CONTINUE;
        
        return BooleanValue.FALSE;
      }

      return getColumnValue(column + 1);
    }
    catch (SQLException ex) {
      _error.error(ex);
      
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }
  }

  private Value fetchFunc()
  {
    throw new UnimplementedException();
  }

  /**
   * Fetch the next row.
   *
   * @param fetchMode the mode, 0 to use the value set by {@link #setFetchMode}.
   * @return a value, BooleanValue.FALSE if there are no more
   *  rows or an error occurs.
   */
  private Value fetchImpl(int fetchMode, int columnIndex)
  {
    if (fetchMode == 0) {
      fetchMode = _fetchMode;

      fetchMode = fetchMode & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));
    }
    else {
      if ((fetchMode & PDO.FETCH_GROUP) != 0) {
        _error.warning(L.l("FETCH_GROUP is not allowed"));
        return BooleanValue.FALSE;
      }
      else if ((fetchMode & PDO.FETCH_UNIQUE) != 0) {
        _error.warning(L.l("FETCH_UNIQUE is not allowed"));
        return BooleanValue.FALSE;
      }
    }

    boolean isClasstype = (fetchMode & PDO.FETCH_CLASSTYPE) != 0;
    boolean isSerialize = (fetchMode & PDO.FETCH_SERIALIZE) != 0;

    fetchMode = fetchMode & (~(PDO.FETCH_CLASSTYPE | PDO.FETCH_SERIALIZE));

    _fetchErrorCode = FETCH_SUCCESS;
    
    switch (fetchMode) {
      case PDO.FETCH_ASSOC:
        return fetchAssoc();

      case PDO.FETCH_BOTH:
        return fetchBoth();

      case PDO.FETCH_BOUND:
        return fetchBound();

      case PDO.FETCH_COLUMN:
        return fetchColumn(columnIndex);

      case PDO.FETCH_CLASS:
        return fetchClass();

      case PDO.FETCH_FUNC:
        return fetchFunc();

      case PDO.FETCH_INTO:
        return fetchInto();

      case PDO.FETCH_LAZY:
        return fetchLazy();

      case PDO.FETCH_NAMED:
        return fetchNamed();

      case PDO.FETCH_NUM:
        return fetchNum();

      case PDO.FETCH_OBJ:
        return fetchObject();

    default:
      _error.warning(L.l("invalid fetch mode {0}",  fetchMode));
      closeCursor();
      return BooleanValue.FALSE;
    }
  }

  private Value fetchInto()
  {
    assert _fetchModeArgs.length > 0;
    assert _fetchModeArgs[0].isObject();

    Value var = _fetchModeArgs[0];

    if (!advanceResultSet()) {
      _fetchErrorCode = FETCH_EXHAUSTED;
      
      return BooleanValue.FALSE;
    }

    try {
      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        var.putField(_env, name, value);
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }

    return var;
  }

  private Value fetchLazy()
  {
    // XXX: need to check why lazy is no different than object
    return fetchObject(null, NULL_VALUES);
  }

  private Value fetchNamed()
  {
    try {
      if (! advanceResultSet()) {
        _fetchErrorCode = FETCH_EXHAUSTED;
        
        return BooleanValue.FALSE;
      }

      ArrayValue array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        Value name = _env.createString(getResultSetMetaData().getColumnName(i));
        Value value = getColumnValue(i);

        Value existingValue = array.get(name);

        if (! (existingValue instanceof UnsetValue)) {

          if (! existingValue.isArray()) {
            ArrayValue arrayValue = new ArrayValueImpl();
            arrayValue.put(existingValue);
            array.put(name, arrayValue);
            existingValue = arrayValue;
          }

          existingValue.put(value);
        }
        else
          array.put(name, value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }
  }

  private Value fetchNum()
  {
    try {
      if (!advanceResultSet()) {
        _fetchErrorCode = FETCH_EXHAUSTED;
        
        return BooleanValue.FALSE;
      }

      if (_fetchModeArgs.length != 0) {
        _error.notice(L.l("unexpected arguments"));
        
        _fetchErrorCode = FETCH_FAILURE;
        
        return BooleanValue.FALSE;
      }

      ArrayValueImpl array = new ArrayValueImpl();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        Value value = getColumnValue(i);

        array.put(value);
      }

      return array;
    }
    catch (SQLException ex) {
      _error.error(ex);
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }
  }

  private Value fetchObject()
  {
    return fetchObject(null, NULL_VALUES);
  }

  public Value fetchObject(@Optional String className, @Optional Value[] args)
  {
    QuercusClass cl;

    if (className != null) {
      cl = _env.findAbstractClass(className);

      if (cl == null)
        return fetchBoth();
    }
    else {
      cl = null;

      if (args.length != 0) {
        advanceResultSet();
        _error.warning(L.l("unexpected arguments"));
        return BooleanValue.FALSE;
      }
    }

    if (!advanceResultSet()) {
      _fetchErrorCode = FETCH_EXHAUSTED;
      
      return BooleanValue.FALSE;
    }

    try {
      Value object;

      if (cl != null)
        object = cl.callNew(_env, args);
      else
        object = _env.createObject();

      int columnCount = getResultSetMetaData().getColumnCount();

      for (int i = 1; i <= columnCount; i++) {
        String name = getResultSetMetaData().getColumnName(i);
        Value value = getColumnValue(i);

        object.putField(_env, name, value);
      }

      return object;
    }
    catch (Throwable ex) {
      _error.error(ex);
      _fetchErrorCode = FETCH_FAILURE;
      
      return BooleanValue.FALSE;
    }

  }

  public Value getAttribute(int attribute)
  {
    _error.unsupportedAttribute(attribute);

    return BooleanValue.FALSE;
  }

  /**
   * @param column 0-based column index
   */
  public Value getColumnMeta(int column)
  {
    throw new UnimplementedException();
  }

  /**
   * @param column 1-based column index
   */
  private Value getColumnValue(int column)
    throws SQLException
  {
    return getColumnValue(column, -1, -1);
  }

  /**
   * @param column 1-based column index
   * @param jdbcType a jdbc type, or -1 if it is unknown
   * @param returnType a PDO.PARAM_* type, or -1
   */
  private Value getColumnValue(int column, int jdbcType, int returnType)
    throws SQLException
  {
    if (returnType != -1)
      throw new UnimplementedException("parm type " + returnType);

    if (jdbcType == -1)
      jdbcType = getResultSetMetaData().getColumnType(column);

    // XXX: needs tests

    switch (jdbcType) {
      case Types.NULL:
        return NullValue.NULL;

      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
      {
        String value = _resultSet.getString(column);

        if (value == null || _resultSet.wasNull())
          return NullValue.NULL;
        else
          return _env.createString(value);
      }

      case Types.DOUBLE:
      {
        double value = _resultSet.getDouble(column);

        if (_resultSet.wasNull())
          return NullValue.NULL;
        else
          return (new DoubleValue(value)).toStringValue();
      }

      // XXX: lob

      default:
      {
        String value = _resultSet.getString(column);

        if (value == null || _resultSet.wasNull())
          return NullValue.NULL;
        else
          return _env.createString(value);
      }
    }

  }

  private ResultSetMetaData getResultSetMetaData()
    throws SQLException
  {
    if (_resultSetMetaData == null)
      _resultSetMetaData = _resultSet.getMetaData();

    return _resultSetMetaData;
  }

  /**
   * Returns an iterator of the values.
   */
  public Iterator<Value> iterator()
  {
    Value value = fetchAll(0, -1);

    if (value instanceof ArrayValue)
      return ((ArrayValue) value).values().iterator();
    else {
      Set<Value> emptySet = Collections.emptySet();
      return emptySet.iterator();
    }
  }

  String lastInsertId(String name)
  {
    if (!(name == null || name.length() == 0))
      throw new UnimplementedException("lastInsertId with name ");

    if (_lastInsertId != null)
      return _lastInsertId;

    String lastInsertId = null;

    Statement stmt;

    if (_preparedStatement != null)
      stmt = _preparedStatement;
    else
      stmt = _statement;

    ResultSet resultSet = null;

    try {
      resultSet = stmt.getGeneratedKeys();

      if (resultSet.next())
        lastInsertId = resultSet.getString(1);
    }
    catch (SQLException ex) {
      _error.error(ex);
    }
    finally {
      try {
        if (resultSet != null)
          resultSet.close();
      }
      catch (SQLException ex) {
        log.log(Level.WARNING, ex.toString(), ex);
      }
    }

    _lastInsertId = lastInsertId == null ? "0" : lastInsertId;

    return _lastInsertId;
  }

  public boolean nextRowset()
  {
    throw new UnimplementedException();
  }

  private int resolveParameter(Value parameter)
  {
    int index = -1;

    if (parameter instanceof LongValue) {
      // slight optimization for normal case
      index = parameter.toInt();
    }
    else {
      String name = parameter.toString();

      if (name.length() > 1 && name.charAt(0) == ':') {
        name = name.substring(1);
        if (_parameterNameMap != null)
          index = _parameterNameMap.get(name);
      }
      else
        index = parameter.toInt();
    }

    return index;
  }

  public int rowCount()
  {
    if (_resultSet == null)
      return 0;

    try {
      int row = _resultSet.getRow();

      try {
        _resultSet.last();

        return _resultSet.getRow();
      }
      finally {
        if (row == 0)
          _resultSet.beforeFirst();
        else
          _resultSet.absolute(row);
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      return 0;
    }
  }

  public boolean setAttribute(int attribute, Value value)
  {
    return setAttribute(attribute, value, false);
  }

  public boolean setAttribute(
      int attribute, Value value, boolean isFromConstructor)
  {
    if (isFromConstructor) {
      switch (attribute) {
        case PDO.CURSOR_FWDONLY:
        case PDO.CURSOR_SCROLL:
          return setCursor(attribute);
      }
    }

    _error.unsupportedAttribute(attribute);

    return false;
  }

  private boolean setCursor(int attribute)
  {
    switch (attribute) {
      case PDO.CURSOR_FWDONLY:
        throw new UnimplementedException();
      case PDO.CURSOR_SCROLL:
        throw new UnimplementedException();

      default:
        _error.unsupportedAttribute(attribute);
        return false;
    }
  }


  /**
   * Sets the fetch mode, the default is {@link PDO.FETCH_BOTH}.
   */
  public boolean setFetchMode(int fetchMode, Value[] args)
  {
    _fetchMode = PDO.FETCH_BOTH;
    _fetchModeArgs = NULL_VALUES;

    int fetchStyle = fetchMode;

    boolean isGroup = (fetchMode & PDO.FETCH_GROUP) != 0;
    boolean isUnique = (fetchMode & PDO.FETCH_UNIQUE) != 0;

    if (isGroup)
      throw new UnimplementedException("PDO.FETCH_GROUP");

    if (isUnique)
      throw new UnimplementedException("PDO.FETCH_UNIQUE");

    fetchStyle = fetchStyle & (~(PDO.FETCH_GROUP | PDO.FETCH_UNIQUE));

    boolean isClasstype = (fetchMode & PDO.FETCH_CLASSTYPE) != 0;
    boolean isSerialize = (fetchMode & PDO.FETCH_SERIALIZE) != 0;

    fetchStyle = fetchStyle & (~(PDO.FETCH_CLASSTYPE | PDO.FETCH_SERIALIZE));

    switch (fetchStyle) {
      case PDO.FETCH_ASSOC:
      case PDO.FETCH_BOTH:
      case PDO.FETCH_BOUND:
      case PDO.FETCH_LAZY:
      case PDO.FETCH_NAMED:
      case PDO.FETCH_NUM:
      case PDO.FETCH_OBJ:
        break;

      case PDO.FETCH_CLASS:
        if (args.length < 1 || args.length > 2)
          return false;

        if (_env.findClass(args[0].toString()) == null)
          return false;

        if (args.length == 2 && !(args[1].isNull() || args[1].isArray())) {
          _env.warning(L.l("constructor args must be an array"));

          return false;
        }

        break;

      case PDO.FETCH_COLUMN:
        if (args.length != 1)
          return false;

        break;

     case PDO.FETCH_FUNC:
       _error.warning(L.l(
           "PDO::FETCH_FUNC can only be used with PDOStatement::fetchAll()"));
       return false;

      case PDO.FETCH_INTO:
        if (args.length != 1 || !args[0].isObject())
          return false;

        break;

      default:
        _error.warning(L.l("invalid fetch mode"));
        break;
    }

    _fetchModeArgs = args;
    _fetchMode = fetchMode;

    return true;
  }

  /**
   * @param index 1-based position number
   * @param value the value for the parameter
   *
   * @return true for success, false for failure
   */
  private boolean setLobParameter(int index, Value value, long length)
  {
    try {
      if (value == null || value.isNull()) {
        _preparedStatement.setObject(index, null);
      }
      else if (value instanceof StringValue) {
        if (length < 0) {
          _preparedStatement.setBinaryStream(
              index, value.toInputStream(), value.toString().length());
        }
        else
          _preparedStatement.setBinaryStream(
              index, value.toInputStream(), (int) length);
      }
      else {
        InputStream inputStream = value.toInputStream();

        if (inputStream == null) {
          _error.warning(L.l(
              "type {0} ({1}) for parameter index {2} cannot be used for lob",
              value.getType(), value.getClass(), index));
          return false;
        }

        if (length < 0 && (value instanceof FileReadValue)) {
          length = ((FileReadValue) value).getLength();

          if (length <= 0)
            length = -1;
        }

        if (length < 0) {
          TempBuffer tempBuffer = TempBuffer.allocate();

          try {
            byte[] bytes = new byte[1024];

            int len;

            while ((len = inputStream.read(bytes, 0, 1024)) != -1)
              tempBuffer.write(bytes, 0, len);
          }
          catch (IOException ex) {
            _error.error(ex);
            return false;
          }

          TempReadStream tempReadStream = new TempReadStream(tempBuffer);
          tempReadStream.setFreeWhenDone(true);

          _preparedStatement.setBinaryStream(
              index, new ReadStream(tempReadStream), tempBuffer.getLength());
        }
        else
          _preparedStatement.setBinaryStream(index, inputStream, (int) length);
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }

    return true;
  }

  /**
   * @param index 1-based position number
   * @param value the value for the parameter
   *
   * @return true for success, false for failure
   */
  private boolean setParameter(int index, Value value, long length)
  {
    try {
      if (value instanceof DoubleValue) {
        _preparedStatement.setDouble(index, value.toDouble());
      }
      else if (value instanceof LongValue) {
        long v = value.toLong();

        _preparedStatement.setLong(index, v);
      }
      else if (value instanceof StringValue) {
        String string = value.toString();

        if (length >= 0)
          string = string.substring(0, (int) length);

        _preparedStatement.setString(index, string);
      }
      else if (value instanceof NullValue) {
        _preparedStatement.setObject(index, null);
      }
      else {
        _error.warning(L.l(
            "unknown type {0} ({1}) for parameter index {2}",
            value.getType(), value.getClass(), index));
        return false;
      }
    }
    catch (SQLException ex) {
      _error.error(ex);
      return false;
    }

    return true;
  }

  public String toString()
  {
    return "PDOStatement[" + _query + "]";
  }

  /**
   * Bind a value from a resultSet to a variable.
   */
  private class BindColumn {
    private final String _columnAsName;
    private final Value _var;
    private final int _type;

    private int _column;
    private int _jdbcType;

    private boolean _isInit;
    private boolean _isValid;

    /**
     * @param column 1-based column index
     * @param var reference that receives the value
     * @param type a PARM_* type, -1 for default
     */
    private BindColumn(Value column, Value var, int type)
      throws SQLException
    {
      assert column != null;
      assert var != null;

      if (column.isNumberConvertible()) {
        _column = column.toInt();
        _columnAsName = null;
      }
      else {
        _columnAsName = column.toString();
      }

      _var = var;
      _type = type;

      if (_resultSet != null)
        init();
    }

    private boolean init()
      throws SQLException
    {
      if (_isInit)
        return true;

      ResultSetMetaData resultSetMetaData = getResultSetMetaData();

      int columnCount = resultSetMetaData.getColumnCount();

      if (_columnAsName != null) {

        for (int i = 1; i <= columnCount; i++) {
          String name = resultSetMetaData.getColumnName(i);
          if (name.equals(_columnAsName)) {
            _column = i;
            break;
          }
        }
      }

      _isValid = _column > 0 && _column <= columnCount;

      if (_isValid) {
        _jdbcType = resultSetMetaData.getColumnType(_column);
      }

      _isInit = true;

      return true;
    }

    public boolean bind()
      throws SQLException
    {
      if (!init())
        return false;

      if (!_isValid) {
        // this matches php behaviour
        _var.set(_env.getEmptyString());
      }
      else {
        Value value = getColumnValue(_column, _jdbcType, _type);

        _var.set(value);
      }

      return true;
    }
  }

  /**
   * Bind a value to a parameter when the statement is executed.
   */
  private class BindParam {
    private final int _index;
    private final Value _value;
    private final int _dataType;
    private final int _length;
    private final Value _driverOptions;

    public BindParam(
        Value parameter,
        Value value,
        int dataType,
        int length,
        Value driverOptions)
    {
      int index = resolveParameter(parameter);

      _index = index;
      _value = value;
      _dataType = dataType;
      _length = length;
      _driverOptions = driverOptions;
    }

    public boolean apply()
      throws SQLException
    {
      switch (_dataType) {
      case PDO.PARAM_BOOL:
      case PDO.PARAM_INT:
      case PDO.PARAM_STR:
        return setParameter(_index, _value.toValue(), _length);
      case PDO.PARAM_LOB:
        return setLobParameter(_index, _value.toValue(), _length);
      case PDO.PARAM_NULL:
        return setParameter(_index, NullValue.NULL, _length);
      case PDO.PARAM_STMT:
        throw new UnimplementedException("PDO.PARAM_STMT");
      default:
        throw new AssertionError();
      }
    }
  }
}
