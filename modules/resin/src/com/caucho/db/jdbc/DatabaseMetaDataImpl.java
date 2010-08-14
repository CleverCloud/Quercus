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
 * @author Scott Ferguson
 */

package com.caucho.db.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;

class DatabaseMetaDataImpl implements DatabaseMetaData {
  private Connection _conn;
  
  DatabaseMetaDataImpl(Connection conn)
  {
    _conn = conn;
  }

  public java.sql.Connection getConnection()
  {
    return _conn;
  }
  
  public String getCatalogTerm()
  {
    return "database";
  }

  /**
   * Returns the types supported by mysql.
   */
  public java.sql.ResultSet getTypeInfo()
    throws SQLException
  {
    DummyResultSet dummy = new DummyResultSet();
    dummy.addColumn("TYPE_NAME", Types.VARCHAR);
    dummy.addColumn("DATA_TYPE", Types.SMALLINT);
    dummy.addColumn("PRECISION", Types.INTEGER);
    dummy.addColumn("LITERAL_PREFIX", Types.VARCHAR);
    dummy.addColumn("LITERAL_SUFFIX", Types.VARCHAR);
    dummy.addColumn("CREATE_PARAMS", Types.VARCHAR);
    dummy.addColumn("NULLABLE", Types.SMALLINT);
    dummy.addColumn("CASE_SENSITIVE", Types.BIT);
    dummy.addColumn("SEARCHABLE", Types.SMALLINT);
    dummy.addColumn("UNSIGNED_ATTRIBUTE", Types.BIT);
    dummy.addColumn("FIXED_PREC_SCALE", Types.BIT);
    dummy.addColumn("AUTO_INCREMENT", Types.BIT);
    dummy.addColumn("LOCAL_TYPE_NAME", Types.VARCHAR);
    dummy.addColumn("MINIMUM_SCALE", Types.SMALLINT);
    dummy.addColumn("MAXIMUM_SCALE", Types.SMALLINT);
    dummy.addColumn("SQL_DATA_TYPE", Types.INTEGER);
    dummy.addColumn("SQL_DATETIME_SUB", Types.INTEGER);
    dummy.addColumn("NUM_PREC_RADIX", Types.INTEGER);

    // boolean
    dummy.addRow("CHAR:16:1::::1:1:1:1:0:0:CHAR:0:0:1::10");
    
    // bit
    dummy.addRow("TINYINT:-7:1::::1:1:1:1:0:0:TINYINT:0:0:254::10");
    
    // byte
    dummy.addRow("TINYINT:-6:3::::1:0:1:1:0:1:TINYINT:0:0:1::10");
    
    // short
    dummy.addRow("SMALLINT:5:5::::1:0:1:1:0:1:SMALLINT:0:0:2::10");
    
    // int
    dummy.addRow("INTEGER:4:10::::1:0:1:1:0:1:INTEGER:0:0:3::10");
    
    // long
    dummy.addRow("BIGINT:-5:20::::1:0:1:1:0:1:BIGINT:0:0:8::10");

    // float
    dummy.addRow("FLOAT:6:10:::(M,D) ZEROFILL:1:0:0:1:0:1:FLOAT:3:3:8::10");

    // real
    dummy.addRow("FLOAT:7:10:::(M,D) ZEROFILL:1:0:0:1:0:1:FLOAT:3:3:8::10");
    
    // double
    dummy.addRow("DOUBLE:8:10:::(M,D) ZEROFILL:1:0:0:1:0:1:FLOAT:3:3:8::10");
    
    // char
    dummy.addRow("CHAR:1:255:':':(M) BINARY:1:1:1:1:0:0:CHAR:0:0:254::10");
    
    // varchar
    dummy.addRow("VARCHAR:12:255:':':(M) BINARY:1:1:1:1:0:0:VARCHAR:0:0:253::10");
    
    // timestamp
    dummy.addRow("DATETIME:93:0:':'::1:1:1:1:0:0:DATETIME:0:0:253::10");
    
    // date
    dummy.addRow("DATETIME:91:0:':'::1:1:1:1:0:0:DATETIME:0:0:253::10");
    
    // time
    dummy.addRow("DATETIME:92:0:':'::1:1:1:1:0:0:DATETIME:0:0:253::10");
    
    // numeric
    dummy.addRow("NUMERIC:2:10:::(M,D) ZEROFILL:1:0:0:0:1:0:NUMERIC:0:20:3::10");
    dummy.addRow("NUMERIC:3:10:::(M,D) ZEROFILL:1:0:0:0:1:0:NUMERIC:0:20:3::10");

    return dummy;
  }

  private int typeNameToTypes(String typeName)
  {
    int p = typeName.indexOf('(');
    if (p > 0)
      typeName = typeName.substring(0, p);

    if ("varchar".equals(typeName))
      return Types.VARCHAR;
    else if ("char".equals(typeName))
      return Types.VARCHAR;
    else if ("timestamp".equals(typeName))
      return Types.TIMESTAMP;
    else if ("tinyint".equals(typeName))
      return Types.TINYINT;
    else if ("smallint".equals(typeName))
      return Types.SMALLINT;
    else if ("integer".equals(typeName))
      return Types.INTEGER;
    else if ("bigint".equals(typeName))
      return Types.BIGINT;
    else if ("double".equals(typeName))
      return Types.DOUBLE;
    else
      return Types.VARCHAR;
  }
  public boolean allProceduresAreCallable()
    throws SQLException
  {
    return true;
  }

  public boolean allTablesAreSelectable()
    throws SQLException
  {
    return true;
  }

  public String getURL()
    throws SQLException
  {
    return "jdbc:mysql_caucho://localhost:3306/test";
  }
  
  public String getUserName()
    throws SQLException
  {
    return "fergie";
  }

  public boolean isReadOnly()
    throws SQLException
  {
    return false;
  }

  public boolean nullsAreSortedHigh()
    throws SQLException
  {
    return false;
  }

  public boolean nullsAreSortedLow()
    throws SQLException
  {
    return false;
  }

  public boolean nullsAreSortedAtStart()
    throws SQLException
  {
    return false;
  }

  public boolean nullsAreSortedAtEnd()
    throws SQLException
  {
    return true;
  }

  public String getDatabaseProductName()
    throws SQLException
  {
    return "Resin";
  }

  public String getDatabaseProductVersion()
    throws SQLException
  {
    return "1.1";
  }

  public String getDriverName()
    throws SQLException
  {
    return "Resin Driver";
  }

  public String getDriverVersion()
    throws SQLException
  {
    return "1.0";
  }

  public int getDriverMajorVersion()
  {
    return 1;
  }

  public int getDriverMinorVersion()
  {
    return 2;
  }

  public boolean usesLocalFiles()
    throws SQLException
  {
    return true;
  }

  public boolean usesLocalFilePerTable()
    throws SQLException
  {
    return true;
  }

  public boolean supportsMixedCaseIdentifiers()
    throws SQLException
  {
    return true;
  }

  public boolean storesUpperCaseIdentifiers()
    throws SQLException
  {
    return false;
  }

  public boolean storesLowerCaseIdentifiers()
    throws SQLException
  {
    return false;
  }

  public boolean storesMixedCaseIdentifiers()
    throws SQLException
  {
    return true;
  }

  public boolean supportsMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    return true;
  }

  public boolean storesUpperCaseQuotedIdentifiers()
    throws SQLException
  {
    return false;
  }

  public boolean storesLowerCaseQuotedIdentifiers()
    throws SQLException
  {
    return false;
  }

  public boolean storesMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    return true;
  }

  public String getIdentifierQuoteString()
    throws SQLException
  {
    return "`";
  }

  public String getSQLKeywords()
    throws SQLException
  {
    return "";
  }

  public String getNumericFunctions()
    throws SQLException
  {
    return "";
  }

  public String getStringFunctions()
    throws SQLException
  {
    return "";
  }

  public String getSystemFunctions()
    throws SQLException
  {
    return "";
  }

  public String getTimeDateFunctions()
    throws SQLException
  {
    return "";
  }

  public String getSearchStringEscape()
    throws SQLException
  {
    return "\\";
  }

  public String getExtraNameCharacters()
    throws SQLException
  {
    return "";
  }

  public boolean supportsAlterTableWithAddColumn()
    throws SQLException
  {
    return false;
  }

  public boolean supportsAlterTableWithDropColumn()
    throws SQLException
  {
    return false;
  }

  public boolean supportsColumnAliasing()
    throws SQLException
  {
    return true;
  }

  public boolean nullPlusNonNullIsNull()
    throws SQLException
  {
    return true;
  }

  public boolean supportsConvert()
    throws SQLException
  {
    return false;
  }

  public boolean supportsConvert(int fromType, int toType)
    throws SQLException
  {
    return false;
  }

  public boolean supportsTableCorrelationNames()
    throws SQLException
  {
    return true;
  }

  public boolean supportsDifferentTableCorrelationNames()
    throws SQLException
  {
    return false;
  }

  public boolean supportsExpressionsInOrderBy()
    throws SQLException
  {
    return false;
  }

  public boolean supportsOrderByUnrelated()
    throws SQLException
  {
    return false;
  }

  public boolean supportsGroupBy()
    throws SQLException
  {
    return false;
  }

  public boolean supportsGroupByUnrelated()
    throws SQLException
  {
    return false;
  }

  public boolean supportsGroupByBeyondSelect()
    throws SQLException
  {
    return false;
  }

  public boolean supportsLikeEscapeClause()
    throws SQLException
  {
    return false;
  }

  public boolean supportsMultipleResultSets()
    throws SQLException
  {
    return false;
  }

  public boolean supportsMultipleTransactions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsNonNullableColumns()
    throws SQLException
  {
    return true;
  }

  public boolean supportsMinimumSQLGrammar()
    throws SQLException
  {
    return true;
  }

  public boolean supportsCoreSQLGrammar()
    throws SQLException
  {
    return true;
  }

  public boolean supportsExtendedSQLGrammar()
    throws SQLException
  {
    return false;
  }

  public boolean supportsANSI92EntryLevelSQL()
    throws SQLException
  {
    return true;
  }

  public boolean supportsANSI92IntermediateSQL()
    throws SQLException
  {
    return false;
  }

  public boolean supportsANSI92FullSQL()
    throws SQLException
  {
    return false;
  }

  public boolean supportsIntegrityEnhancementFacility()
    throws SQLException
  {
    return false;
  }

  public boolean supportsOuterJoins()
    throws SQLException
  {
    return false;
  }

  public boolean supportsFullOuterJoins()
    throws SQLException
  {
    return false;
  }

  public boolean supportsLimitedOuterJoins()
    throws SQLException
  {
    return false;
  }

  public String getSchemaTerm()
    throws SQLException
  {
    return "schema";
  }

  public String getProcedureTerm()
    throws SQLException
  {
    return "procedure";
  }

  public boolean isCatalogAtStart()
    throws SQLException
  {
    return true;
  }

  public String getCatalogSeparator()
    throws SQLException
  {
    return ".";
  }

  public boolean supportsSchemasInDataManipulation()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSchemasInProcedureCalls()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSchemasInTableDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSchemasInIndexDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSchemasInPrivilegeDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInDataDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInProcedureCalls()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInTableDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInIndexDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInPrivilegeDefinitions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsPositionedDelete()
    throws SQLException
  {
    return false;
  }

  public boolean supportsPositionedUpdate()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSelectForUpdate()
    throws SQLException
  {
    return false;
  }

  public boolean supportsStoredProcedures()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSubqueriesInComparisons()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSubqueriesInExists()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSubqueriesInIns()
    throws SQLException
  {
    return false;
  }

  public boolean supportsSubqueriesInQuantifieds()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCorrelatedSubqueries()
    throws SQLException
  {
    return false;
  }

  public boolean supportsUnion()
    throws SQLException
  {
    return true;
  }

  public boolean supportsUnionAll()
    throws SQLException
  {
    return true;
  }

  public boolean supportsOpenCursorsAcrossCommit()
    throws SQLException
  {
    return false;
  }

  public boolean supportsOpenCursorsAcrossRollback()
    throws SQLException
  {
    return false;
  }

  public boolean supportsOpenStatementsAcrossCommit()
    throws SQLException
  {
    return true;
  }

  public boolean supportsOpenStatementsAcrossRollback()
    throws SQLException
  {
    return true;
  }

  public int getMaxBinaryLiteralLength()
    throws SQLException
  {
    return 16;
  }

  public int getMaxCharLiteralLength()
    throws SQLException
  {
    return 254;
  }

  public int getMaxColumnNameLength()
    throws SQLException
  {
    return 64;
  }

  public int getMaxColumnsInGroupBy()
    throws SQLException
  {
    return 16;
  }

  public int getMaxColumnsInIndex()
    throws SQLException
  {
    return 16;
  }

  public int getMaxColumnsInOrderBy()
    throws SQLException
  {
    return 16;
  }

  public int getMaxColumnsInSelect()
    throws SQLException
  {
    return 16;
  }

  public int getMaxColumnsInTable()
    throws SQLException
  {
    return 16;
  }

  public int getMaxConnections()
    throws SQLException
  {
    return 16;
  }

  public int getMaxCursorNameLength()
    throws SQLException
  {
    return 254;
  }

  public int getMaxIndexLength()
    throws SQLException
  {
    return 254;
  }

  public int getMaxSchemaNameLength()
    throws SQLException
  {
    return 254;
  }

  public int getMaxProcedureNameLength()
    throws SQLException
  {
    return 254;
  }

  public int getMaxCatalogNameLength()
    throws SQLException
  {
    return 64;
  }

  public int getMaxRowSize()
    throws SQLException
  {
    return 65536;
  }

  public int getMaxRowSizeIncludeBlobs()
    throws SQLException
  {
    return 65536;
  }

  public boolean doesMaxRowSizeIncludeBlobs()
    throws SQLException
  {
    return false;
  }

  public int getMaxStatementLength()
    throws SQLException
  {
    return 65536;
  }

  public int getMaxStatements()
    throws SQLException
  {
    return 0;
  }

  public int getMaxTableNameLength()
    throws SQLException
  {
    return 64;
  }

  public int getMaxTablesInSelect()
    throws SQLException
  {
    return 0;
  }

  public int getMaxUserNameLength()
    throws SQLException
  {
    return 0;
  }

  public int getDefaultTransactionIsolation()
    throws SQLException
  {
    return Connection.TRANSACTION_NONE;
  }

  public boolean supportsTransactions()
    throws SQLException
  {
    return true;
  }

  public boolean supportsTransactionIsolationLevel(int level)
    throws SQLException
  {
    return false;
  }

  public boolean supportsDataDefinitionAndDataManipulationTransactions()
    throws SQLException
  {
    return false;
  }

  public boolean supportsDataManipulationTransactionsOnly()
    throws SQLException
  {
    return false;
  }

  public boolean supportsCatalogsInDataManipulation()
    throws SQLException
  {
    return false;
  }

  public boolean dataDefinitionCausesTransactionCommit()
    throws SQLException
  {
    return false;
  }

  public boolean dataDefinitionIgnoredInTransactions()
    throws SQLException
  {
    return false;
  }

  public java.sql.ResultSet getProcedures(String catalog,
                                 String schemaPattern,
                                 String procedureNamePattern)
    throws SQLException
  {
    return null;
  }

  public java.sql.ResultSet getProcedureColumns(String catalog,
                                       String schemaPattern,
                                       String procedureNamePattern,
                                       String columnNamePatterns)
    throws SQLException
  {
    return null;
  }

  public java.sql.ResultSet getTables(String catalog,
                                      String schemaPattern,
                                      String tableNamePattern,
                                      String []types)
    throws SQLException
  {
    DummyResultSet dummy = new DummyResultSet();

    dummy.addColumn("TABLE_CAT", Types.VARCHAR);
    dummy.addColumn("TABLE_SCHEM", Types.VARCHAR);
    dummy.addColumn("TABLE_NAME", Types.VARCHAR);
    dummy.addColumn("TABLE_TYPE", Types.VARCHAR);
    dummy.addColumn("REMARKS", Types.VARCHAR);
    
    return dummy;
  }

  public java.sql.ResultSet getSchemas()
    throws SQLException
  {
    return null;
  }

  public java.sql.ResultSet getCatalogs()
    throws SQLException
  {
    return null;
  }

  public java.sql.ResultSet getTableTypes()
    throws SQLException
  {
    return null;
  }

  public java.sql.ResultSet getColumns(String catalog,
                              String schemaPattern,
                              String tableNamePattern,
                              String columnNamePattern)
    throws SQLException
  {
    DummyResultSet dummy = new DummyResultSet();

    dummy.addColumn("TABLE_CAT", Types.VARCHAR);
    dummy.addColumn("TABLE_SCHEM", Types.VARCHAR);
    dummy.addColumn("TABLE_NAME", Types.VARCHAR);
    dummy.addColumn("COLUMN_NAME", Types.VARCHAR);
    dummy.addColumn("DATA_TYPE", Types.SMALLINT);
    dummy.addColumn("TYPE_NAME", Types.VARCHAR);
    dummy.addColumn("COLUMN_SIZE", Types.INTEGER);
    dummy.addColumn("BUFFER_LENGTH", Types.INTEGER);
    dummy.addColumn("DECIMAL_DIGITS", Types.INTEGER);
    dummy.addColumn("NUM_PREC_RADIX", Types.INTEGER);
    dummy.addColumn("NULLABLE", Types.INTEGER);
    dummy.addColumn("REMARKS", Types.VARCHAR);
    dummy.addColumn("COLUMN_DEF", Types.VARCHAR);
    dummy.addColumn("SQL_DATA_TYPE", Types.INTEGER);
    dummy.addColumn("SQL_DATETIME_SUB", Types.INTEGER);
    dummy.addColumn("CHAR_OCTET_LENGTH", Types.INTEGER);
    dummy.addColumn("ORDINAL_POSITION", Types.INTEGER);
    dummy.addColumn("IS_NULLABLE", Types.VARCHAR);
    
    return dummy;
  }

  public java.sql.ResultSet getColumnPrivileges(String catalog,
                              String schemaPattern,
                              String tableNamePattern,
                              String columnNamePattern)
    throws SQLException
  {
    return new DummyResultSet();
  }

  public java.sql.ResultSet getTable(String catalog,
                              String schemaPattern,
                              String tableNamePattern)
    throws SQLException
  {
    return new DummyResultSet();
  }

  public java.sql.ResultSet getTablePrivileges(String catalog,
                              String schemaPattern,
                              String tableNamePattern)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getBestRowIdentifier(String catalog,
                                        String schema,
                                        String table,
                                        int scope,
                                        boolean nullable)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getVersionColumns(String catalog,
                                     String schema,
                                     String table)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getPrimaryKeys(String catalog,
                                  String schema,
                                  String table)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getImportedKeys(String catalog,
                                   String schema,
                                   String table)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getExportedKeys(String catalog,
                                   String schema,
                                   String table)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getCrossReference(String primaryCatalog,
                                     String primarySchema,
                                     String primaryTable,
                                     String foreignCatalog,
                                     String foreignSchema,
                                     String foreignTable)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public java.sql.ResultSet getIndexInfo(String catalog,
                                String schema,
                                String table,
                                boolean unique,
                                boolean approximate)
    throws SQLException
  {
    return new DummyResultSet();
  }
  
  public boolean supportsResultSetType(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsResultSetConcurrency(int type, int concurrency)
    throws SQLException
  {
    return false;
  }
  
  public boolean ownUpdatesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean ownDeletesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean ownInsertsAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean othersUpdatesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean othersDeletesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean othersInsertsAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean updatesAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean deletesAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean insertsAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsBatchUpdates()
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsStatementPooling()
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsUpdateCpy()
    throws SQLException
  {
    return false;
  }
  
  public boolean locatorsUpdateCopy()
    throws SQLException
  {
    return false;
  }
  
  public int getSQLStateType()
    throws SQLException
  {
    return 0;
  }
  
  public int getJDBCMajorVersion()
    throws SQLException
  {
    return 2;
  }
  
  public int getJDBCMinorVersion()
    throws SQLException
  {
    return 0;
  }
  
  public int getResultSetHoldability()
    throws SQLException
  {
    return 0;
  }
  
  public boolean supportsResultSetHoldability()
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsResultSetHoldability(int holdability)
    throws SQLException
  {
    return false;
  }
  
  public java.sql.ResultSet getAttributes(String a, String b, String c, String g)
    throws SQLException
  {
    return null;
  }
  
  public java.sql.ResultSet getSuperTypes(String a, String b, String c)
    throws SQLException
  {
    return null;
  }
  
  public java.sql.ResultSet getSuperTables(String a, String b, String c)
    throws SQLException
  {
    return null;
  }
  
  public boolean supportsGetGeneratedKeys()
    throws SQLException
  {
    return true;
  }
  
  public boolean supportsMultipleOpenResults()
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsNamedParameters()
    throws SQLException
  {
    return false;
  }
  
  public boolean supportsSavepoints()
    throws SQLException
  {
    return false;
  }
  
  public int getDatabaseMajorVersion()
    throws SQLException
  {
    return 0;
  }
  
  public int getDatabaseMinorVersion()
    throws SQLException
  {
    return 0;
  }
  
  public java.sql.ResultSet getUDTs(String catalog,
                           String schemaPattern,
                           String typeNamePattern,
                           int []types)
    throws SQLException
  {
    return null;
  }

  public String toString()
  {
    return "DatabaseMetaDataImpl[]";
  }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
