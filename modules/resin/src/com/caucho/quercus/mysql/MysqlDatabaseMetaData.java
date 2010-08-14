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

package com.caucho.quercus.mysql;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql metadata
 */
public class MysqlDatabaseMetaData implements DatabaseMetaData {
  private static final Logger log
    = Logger.getLogger(MysqlDatabaseMetaData.class.getName());
  private static final L10N L = new L10N(MysqlDatabaseMetaData.class);

  private MysqlConnectionImpl _conn;

  MysqlDatabaseMetaData(MysqlConnectionImpl conn)
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
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
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
    return "Quercus Mysql Driver";
  }

  public String getDriverVersion()
    throws SQLException
  {
    return "1.0";
  }

  public int getDriverMajorVersion()
  {
    return 5;
  }

  public int getDriverMinorVersion()
  {
    return 0;
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
    throw new UnsupportedOperationException(getClass().getName());
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getColumnPrivileges(String catalog,
                              String schemaPattern,
                              String tableNamePattern,
                              String columnNamePattern)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getTable(String catalog,
                              String schemaPattern,
                              String tableNamePattern)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getTablePrivileges(String catalog,
                              String schemaPattern,
                              String tableNamePattern)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getBestRowIdentifier(String catalog,
                                        String schema,
                                        String table,
                                        int scope,
                                        boolean nullable)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getVersionColumns(String catalog,
                                     String schema,
                                     String table)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getPrimaryKeys(String catalog,
                                  String schema,
                                  String table)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getImportedKeys(String catalog,
                                   String schema,
                                   String table)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getExportedKeys(String catalog,
                                   String schema,
                                   String table)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getCrossReference(String primaryCatalog,
                                     String primarySchema,
                                     String primaryTable,
                                     String foreignCatalog,
                                     String foreignSchema,
                                     String foreignTable)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public java.sql.ResultSet getIndexInfo(String catalog,
                                String schema,
                                String table,
                                boolean unique,
                                boolean approximate)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}
