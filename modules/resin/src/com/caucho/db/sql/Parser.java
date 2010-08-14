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

package com.caucho.db.sql;

import com.caucho.db.Database;
import com.caucho.db.table.Column;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableFactory;
import com.caucho.inject.Module;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@Module
public class Parser {
  private static final Logger log
    = Logger.getLogger(Parser.class.getName());
  private static final L10N L = new L10N(Parser.class);

  final static int IDENTIFIER = 128;
  final static int INTEGER = IDENTIFIER + 1;
  final static int LONG = INTEGER + 1;
  final static int DOUBLE = LONG + 1;
  final static int STRING = DOUBLE + 1;
  final static int TRUE = STRING + 1;
  final static int FALSE = TRUE + 1;
  final static int UNKNOWN = FALSE + 1;
  final static int NULL = UNKNOWN + 1;
  final static int EXISTS = NULL + 1;

  final static int FROM = EXISTS + 1;
  final static int IN = FROM + 1;
  final static int SELECT = IN + 1;
  final static int DISTINCT = SELECT + 1;
  final static int WHERE = SELECT + 1;
  final static int AS = WHERE + 1;
  final static int ORDER = AS + 1;
  final static int GROUP = ORDER + 1;
  final static int BY = GROUP + 1;
  final static int ASC = BY + 1;
  final static int DESC = ASC + 1;
  final static int LIMIT = DESC + 1;
  final static int OFFSET = LIMIT + 1;

  final static int BETWEEN = OFFSET + 1;
  final static int LIKE = BETWEEN + 1;
  final static int ESCAPE = LIKE + 1;
  final static int IS = ESCAPE + 1;
  final static int CONCAT = IS + 1;

  final static int EQ = CONCAT + 1;
  final static int NE = EQ + 1;
  final static int LT = NE + 1;
  final static int LE = LT + 1;
  final static int GT = LE + 1;
  final static int GE = GT + 1;

  final static int AND = GE + 1;
  final static int OR = AND + 1;
  final static int NOT = OR + 1;

  final static int ARG = NOT + 1;

  final static int CREATE = ARG + 1;
  final static int TABLE = CREATE + 1;
  final static int INSERT = TABLE + 1;
  final static int INTO = INSERT + 1;
  final static int VALUES = INTO + 1;
  final static int DROP = VALUES + 1;
  final static int UPDATE = DROP + 1;
  final static int SET = UPDATE + 1;
  final static int DELETE = SET + 1;
  final static int VALIDATE = DELETE + 1;
  final static int SHOW = VALIDATE + 1;

  final static int CONSTRAINT = SHOW + 1;
  final static int UNIQUE = CONSTRAINT + 1;
  final static int PRIMARY = UNIQUE + 1;
  final static int CHECK = PRIMARY + 1;
  final static int FOREIGN = CHECK + 1;
  final static int KEY = FOREIGN + 1;

  private final static IntMap _reserved;

  private Database _database;

  private final String _sql;
  private final char []_sqlChars;
  private final int _sqlLength;

  private int _parseIndex;

  private final CharBuffer _cb = new CharBuffer();

  private String _lexeme;
  private int _token;

  private ArrayList<ParamExpr> _params = new ArrayList<ParamExpr>();

  private Query _query;
  private AndExpr _andExpr;

  private Parser(Database database, String sql)
  {
    _database = database;
    _sql = sql;
    _sqlLength = _sql.length();
    _sqlChars = new char[_sqlLength];
    _sql.getChars(0, _sqlLength, _sqlChars, 0);
  }

  public static Query parse(Database database, String sql)
    throws SQLException
  {
    Parser parser = new Parser(database, sql);

    Query query = parser.parse();

    query.bind();

    return query;
  }

  public static Expr parseExpr(Database database, String sql)
    throws SQLException
  {
    Parser parser = new Parser(database, sql);

    Expr expr = parser.parseExpr();

    return expr.bind(null);
  }

  /**
   * Parses the query.
   */
  private Query parse()
    throws SQLException
  {
    int token = scanToken();

    switch (token) {
    case SELECT:
      return parseSelect();

    case CREATE:
      return parseCreate();

    case INSERT:
      return parseInsert();

    case DELETE:
      return parseDelete();

    case VALIDATE:
      return parseValidate();

    case DROP:
      return parseDrop();

    case UPDATE:
      return parseUpdate();
    
    //case SHOW:
      //return parseShow();

    default:
      throw new SQLParseException(L.l("unknown query at {0}",
                                      tokenName(token)));
    }
  }

  /**
   * Parses the select.
   */
  private SelectQuery parseSelect()
    throws SQLException
  {
    return parseSelect(new SelectQuery(_database, _sql));
  }

  /**
   * Parses the select.
   */
  private SelectQuery parseSelect(SelectQuery query)
    throws SQLException
  {
    boolean distinct = false;

    int token = scanToken();

    if (token == DISTINCT)
      distinct = true;
    else
      _token = token;

    ArrayList<Expr> resultItems = new ArrayList<Expr>();

    int startToken = scanToken();
    String startLexeme = _lexeme;
    int startOffset = _parseIndex;

    while ((token = scanToken()) >= 0 && token != FROM) {
    }

    if (token != FROM)
      throw error(L.l("expected FROM at `{0}'", tokenName(token)));

    query.setParent(_query);
    _query = query;

    AndExpr oldAnd = _andExpr;
    _andExpr = new AndExpr();

    ArrayList<FromItem> fromItems = parseFromItems();

    query.setFromItems(fromItems);

    token = scanToken();
    
    int tailToken = token;
    int tailOffset = _parseIndex;

    _token = startToken;
    _parseIndex = startOffset;
    _lexeme = startLexeme;

    Expr expr = parseSelectExpr();

    resultItems.add(expr);

    while ((token = scanToken()) == ',') {
      expr = parseSelectExpr();

      resultItems.add(expr);
    }

    _token = tailToken;
    _parseIndex = tailOffset;

    token = scanToken();

    if (token == WHERE)
      _andExpr.add(parseExpr());
    else
      _token = token;

    ParamExpr []params = _params.toArray(new ParamExpr[_params.size()]);

    Expr whereExpr = _andExpr.getSingleExpr();
    _andExpr = null;
    query.setWhereExpr(whereExpr);
    query.setParams(params);

    for (int i = resultItems.size() - 1; i >= 0; i--) {
      Expr subExpr = resultItems.get(i);

      if (subExpr instanceof UnboundStarExpr) {
        UnboundStarExpr unboundExpr = (UnboundStarExpr) subExpr;
        ArrayList<Expr> exprList = unboundExpr.expand(query.getFromItems());

        resultItems.remove(i);
        resultItems.addAll(i, exprList);
      }
    }

    ArrayList<Expr> groupItems = null;
    token = scanToken();
    if (token == GROUP) {
      token = scanToken();

      if (token != BY)
        throw error(L.l("expected BY at `{0}'", tokenName(token)));

      groupItems = parseGroup(query);
    }
    else
      _token = token;

    token = scanToken();
    if (token == ORDER) {
      token = scanToken();

      if (token != BY)
        throw error(L.l("expected BY at `{0}'", tokenName(token)));

      Order order = parseOrder(query, resultItems);
    }
    else
      _token = token;

    Expr []resultArray = resultItems.toArray(new Expr[resultItems.size()]);

    query.setResults(resultArray);

    if (query.isGroup()) {
      Expr []resultList = query.getResults();

      bindGroup(query, groupItems);

      for (int i = 0; i < resultList.length; i++) {
        Expr subExpr = resultList[i];

        if (! (subExpr instanceof GroupExpr)) {
          resultList[i] = new GroupResultExpr(i, subExpr);
        }
      }
    }
    
    token = scanToken();
    if (token == LIMIT) {
      parseLimit(query);
    }
    else
      _token = token;
    
    if (query.getParent() == null
        && token >= 0 && token != LIMIT && token != OFFSET)
      throw error(L.l("unexpected token at end '{0}'", tokenName(token)));

    _query = query.getParent();
    _andExpr = oldAnd;

    return query;
  }

  private ArrayList<FromItem> parseFromItems()
    throws SQLException
  {
    ArrayList<FromItem> fromItems = new ArrayList<FromItem>();

    int token;

    // XXX: somewhat hacked syntax
    while ((token = scanToken()) == '(') {
    }
    _token = token;
    
    FromItem fromItem = parseFromItem();

    if (fromItem != null)
      fromItems.add(fromItem);
    
    int parenCount = 0;

    while (true) {
      token = scanToken();

      boolean isNatural = false;
      boolean isOuter = false;
      boolean isLeft = true;
      boolean isRight = true;

      if (token == ',') {
        fromItem = parseFromItem();
        fromItems.add(fromItem);
        continue;
      }
      else if (token == '(') {
        parenCount++;
        continue;
      }
      else if (token == ')') {
        if (--parenCount < 0) {
          _token = token;
          break;
        }
        else
          continue;
      }
      else if (token != IDENTIFIER) {
        _token = token;
        break;
      }
      else if ("join".equalsIgnoreCase(_lexeme)) {
      }
      else if ("inner".equalsIgnoreCase(_lexeme)) {
        String join = parseIdentifier();

        if (! "join".equalsIgnoreCase(join))
          throw error(L.l("expected JOIN at '{0}'", join));
      }
      else if ("left".equalsIgnoreCase(_lexeme)) {
        String name = parseIdentifier();

        if ("outer".equalsIgnoreCase(name))
          name = parseIdentifier();

        if (! "join".equalsIgnoreCase(name))
          throw error(L.l("expected JOIN at '{0}'", name));

        isOuter = true;
      }
      else if ("right".equalsIgnoreCase(_lexeme)) {
        String name = parseIdentifier();

        if ("outer".equalsIgnoreCase(name))
          name = parseIdentifier();

        if (! "join".equalsIgnoreCase(name))
          throw error(L.l("expected JOIN at '{0}'", name));

        isRight = true;
        isOuter = true;

        throw error(L.l("right outer joins are not supported"));
      }
      else if ("natural".equalsIgnoreCase(_lexeme)) {
        String name = parseIdentifier();

        isNatural = true;

        if ("left".equalsIgnoreCase(name)) {
          name = parseIdentifier();

          if ("outer".equalsIgnoreCase(name))
            name = parseIdentifier();

          isOuter = true;
        }
        else if ("right".equalsIgnoreCase(name)) {
          name = parseIdentifier();

          if ("outer".equalsIgnoreCase(name))
            name = parseIdentifier();

          isRight = true;
          isOuter = true;

          throw error(L.l("right outer joins are not supported"));
        }

        if (! "join".equalsIgnoreCase(name))
          throw error(L.l("expected JOIN at '{0}'", name));
      }
      else {
        _token = token;
        break;
      }

      fromItem = parseFromItem();
      fromItems.add(fromItem);

      _query.setFromItems(fromItems);

      token = scanToken();
      if (token == IDENTIFIER && "on".equalsIgnoreCase(_lexeme)) {
        Expr onExpr = parseExpr();

        if (isOuter) {
          FromItem leftItem = fromItems.get(fromItems.size() - 2);
          FromItem rightItem = fromItems.get(fromItems.size() - 1);

          onExpr = new LeftOuterJoinExpr(rightItem, onExpr);

          rightItem.setDependTable(leftItem);
        }

        _andExpr.add(onExpr);
      }
      else
        _token = token;
    }

    return fromItems;
  }

  /**
   * Parses a select expression.
   */
  private Expr parseSelectExpr()
    throws SQLException
  {
    int token = scanToken();

    if (token == '*')
      return new UnboundStarExpr();
    else {
      _token = token;

      return parseExpr();
    }
  }

  /**
   * Parses a from item
   */
  private FromItem parseFromItem()
    throws SQLException
  {
    String tableName = parseIdentifier();
    
    if (tableName.equalsIgnoreCase("DUAL"))
      return null;

    Table table = _database.getTable(tableName);

    if (table == null)
      throw error(L.l("'{0}' is an unknown table.  'FROM table' requires an existing table.", tableName));

    String name = table.getName();

    int token = scanToken();
    if (token == AS)
      name = parseIdentifier();
    else if (token == IDENTIFIER)
      name = _lexeme;
    else
      _token = token;

    return new FromItem(table, name);
  }

  /**
   * Parses the ORDER BY
   */
  private Order parseOrder(SelectQuery query,
                           ArrayList<Expr> resultList)
    throws SQLException
  {
    int token;

    Order order = null;

    do {
      Expr expr = parseExpr();

      expr = expr.bind(query);

      token = scanToken();
      boolean isAsc = true;
      if (token == ASC)
        isAsc = true;
      else if (token == DESC)
        isAsc = false;
      else
        _token = token;

      int index;
      for (index = 0; index < resultList.size(); index++) {
        Expr resultExpr = resultList.get(index);

        if (expr.equals(resultExpr))
          break;
      }

      if (resultList.size() <= index) {
        resultList.add(expr);
      }

      Order tailOrder = expr.createOrder(index);
      tailOrder.setAscending(isAsc);

      order = Order.append(order, tailOrder);

      // ascList.add(isAsc ? Boolean.TRUE : Boolean.FALSE);
    } while ((token = scanToken()) == ',');

    query.setOrder(order);

    _token = token;

    return order;
  }

  /**
   * Parses the GROUP BY
   */
  private ArrayList<Expr> parseGroup(SelectQuery query)
    throws SQLException
  {
    query.setGroup(true);
    int token;

    ArrayList<Expr> groupList = new ArrayList<Expr>();

    do {
      groupList.add(parseExpr());
    } while ((token = scanToken()) == ',');

    _token = token;

    return groupList;
  }

  /**
   * Parses the GROUP BY
   */
  private void bindGroup(SelectQuery query, ArrayList<Expr> groupList)
    throws SQLException
  {
    query.setGroup(true);

    Expr []resultList = query.getResults();

    for (int i = 0; i < groupList.size(); i++) {
      Expr expr = groupList.get(i);

      expr = expr.bind(query);

      int index;
      for (index = 0; index < resultList.length; index++) {
        Expr resultExpr = resultList[index];

        if (expr.equals(resultExpr)) {
          resultList[index] = new GroupResultExpr(index, resultExpr);

          break;
        }
      }

      if (resultList.length <= index) {
        throw error(L.l("GROUP BY field '{0}' must refer to a result field.",
                        expr));
      }

      query.setGroupResult(index);
    }
  }

  /**
   * Parses the LIMIT
   */
  private void parseLimit(SelectQuery query)
    throws SQLException
  {
    int token = scanToken();

    if (token == INTEGER) {
      query.setLimit(Integer.valueOf(_lexeme));
      _token = scanToken();
    }
    else
      throw error(L.l("LIMIT expected LIMIT int"));
  }

  /**
   * Parses the create.
   */
  private Query parseCreate()
    throws SQLException
  {
    int token;

    TableFactory factory = _database.createTableFactory();

    if ((token = scanToken()) != TABLE)
      throw error(L.l("expected TABLE at `{0}'", tokenName(token)));

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    factory.startTable(_lexeme);

    if ((token = scanToken()) != '(')
      throw error(L.l("expected '(' at `{0}'", tokenName(token)));

    do {
      token = scanToken();

      switch (token) {
      case IDENTIFIER:
        parseCreateColumn(factory, _lexeme);
        break;

      case UNIQUE:
        token = scanToken();
        
        if (token != KEY) {
          _token = token;
        }
        
        factory.addUnique(parseColumnNames());
        break;

      case PRIMARY:
        token = scanToken();
        if (token != KEY)
          throw error(L.l("expected 'key' at {0}", tokenName(token)));

        factory.addPrimaryKey(parseColumnNames());
        break;

      case KEY:
        String key = parseIdentifier();

        parseColumnNames(); // factory.addPrimaryKey(parseColumnNames());
        break;

      case CHECK:
        if ((token = scanToken()) != '(')
          throw error(L.l("Expected '(' at '{0}'", tokenName(token)));

        parseExpr();

        if ((token = scanToken()) != ')')
          throw error(L.l("Expected ')' at '{0}'", tokenName(token)));
        break;

      default:
        throw error(L.l("unexpected token `{0}'", tokenName(token)));
      }

      token = scanToken();
    } while (token == ',');

    if (token != ')')
      throw error(L.l("expected ')' at `{0}'", tokenName(token)));

    return new CreateQuery(_database, _sql, factory);
  }

  /**
   * Parses a column declaration.
   */
  private void parseCreateColumn(TableFactory factory, String name)
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected column type at {0}", tokenName(token)));

    String type = _lexeme;
    int length = -1;
    int scale = -1;

    if (type.equalsIgnoreCase("double")) {
      if ((token = scanToken()) == IDENTIFIER) {
        if (_lexeme.equalsIgnoreCase("precision")) {
        }
        else
          throw error(L.l("unexpected double type at {0}", _lexeme));
      }
      else
        _token = token;
    }

    if ((token = scanToken()) == '(') {
      if ((token = scanToken()) != INTEGER)
        throw error(L.l("expected column width at `{0}'", tokenName(token)));

      length = Integer.parseInt(_lexeme);

      if ((token = scanToken()) == ',') {
        if ((token = scanToken()) != INTEGER)
          throw error(L.l("expected column scale at `{0}'", tokenName(token)));

        scale = Integer.parseInt(_lexeme);

        token = scanToken();
      }

      if (token != ')')
        throw error(L.l("expected ')' at '{0}'", tokenName(token)));
    }
    else
      _token = token;

    if (type.equalsIgnoreCase("varchar")) {
      if (length < 0)
        throw error(L.l("VARCHAR needs a defined length"));

      factory.addVarchar(name, length);
    }
    else if (type.equalsIgnoreCase("char")) {
      if (length < 0)
        length = 1;

      factory.addVarchar(name, length);
    }
    else if (type.equalsIgnoreCase("varbinary")) {
      if (length < 0)
        throw error(L.l("VARBINARY needs a defined length"));

      factory.addVarbinary(name, length);
    }
    else if (type.equalsIgnoreCase("binary")) {
      if (length < 0)
        throw error(L.l("BINARY needs a defined length"));

      factory.addBinary(name, length);
    }
    else if (type.equalsIgnoreCase("blob")) {
      factory.addBlob(name);
    }
    else if (type.equalsIgnoreCase("tinytext")) {
      factory.addTinytext(name);
    }
    else if (type.equalsIgnoreCase("mediumtext")) {
      factory.addVarchar(name, 256);
    }
    else if (type.equalsIgnoreCase("longtext")) {
      factory.addVarchar(name, 512);
    }
    else if (type.equalsIgnoreCase("smallint")
             || type.equalsIgnoreCase("tinyint")
             || type.equalsIgnoreCase("bit")) {
      factory.addShort(name);
    }
    else if (type.equalsIgnoreCase("integer")
             || type.equalsIgnoreCase("int")
             || type.equalsIgnoreCase("mediumint")) {
      factory.addInteger(name);
    }
    else if (type.equalsIgnoreCase("bigint")) {
      factory.addLong(name);
    }
    else if (type.equalsIgnoreCase("double")
             || type.equalsIgnoreCase("float")
             || type.equalsIgnoreCase("real")) {
      factory.addDouble(name);
    }
    else if (type.equalsIgnoreCase("datetime")
             || type.equalsIgnoreCase("timestamp")) {
      factory.addDateTime(name);
    }
    else if (type.equalsIgnoreCase("text")
             || type.equalsIgnoreCase("clob")) {
      factory.addVarchar(name, 255);
    }
    else if (type.equalsIgnoreCase("decimal")
             || type.equalsIgnoreCase("numeric")) {
      factory.addNumeric(name, length, scale);
    }
    else if (type.equalsIgnoreCase("identity")) {
      factory.addIdentity(name);
    }
    else
      throw error(L.l("Unknown type {0}", type));

    token = scanToken();
    if (token == IDENTIFIER && _lexeme.equalsIgnoreCase("default")) {
      Expr defaultExpr = parseExpr();

      factory.setDefault(name, defaultExpr);
    }
    else
      _token = token;

    while (true) {
      token = scanToken();

      // XXX: stuff like NOT NULL

      switch (token) {
      case ')':
      case ',':
        _token = token;
        return;

      case UNIQUE:
        factory.setUnique(name);
        break;

      case PRIMARY:
        token = scanToken();
        if (token != KEY)
          throw error(L.l("expected key at {0}", tokenName(token)));

        factory.setPrimaryKey(name);
        break;

      case CHECK:
        if ((token = scanToken()) != '(')
          throw error(L.l("Expected '(' at '{0}'", tokenName(token)));

        parseExpr();

        if ((token = scanToken()) != ')')
          throw error(L.l("Expected ')' at '{0}'", tokenName(token)));
        break;

      case IDENTIFIER:
        String id = _lexeme;
        if (id.equalsIgnoreCase("references")) {
          ArrayList<String> foreignKey = new ArrayList<String>();
          foreignKey.add(name);
          parseReferences(foreignKey);
        }
        else if (id.equalsIgnoreCase("default")) {
          Expr expr = parseExpr();
        }
        else if (id.equalsIgnoreCase("auto_increment")) {
          factory.setAutoIncrement(name, 1);
        }
        else if (id.equalsIgnoreCase("unsigned")) {
        }
        else if (id.equalsIgnoreCase("binary")) {
        }
        else
          throw error(L.l("unexpected token '{0}'", tokenName(token)));
        break;

      case NULL:
        break;

      case NOT:
        if ((token = scanToken()) == NULL)
          factory.setNotNull(name);
        else
          throw error(L.l("unexpected token '{0}'", tokenName(token)));
        break;

      default:
        throw error(L.l("unexpected token '{0}'", tokenName(token)));
      }
    }
  }
  
  /**
   * Parses a key constraint declaration.
   */
  private void parseKeyConstraint(TableFactory factory)
    throws SQLException
  {
    String key = parseIdentifier();

    int token = scanToken();

    if (token == '(') {
      parseIdentifier();

      token = scanToken();
      if (token != ')')
        throw error("expected ')'");
    }
    else
      _token = token;
  }

  /**
   * Parses the references clause.
   */
  public void parseReferences(ArrayList<String> name)
    throws SQLException
  {
    String foreignTable = parseIdentifier();

    int token = scanToken();

    ArrayList<String> foreignColumns = new ArrayList<String>();

    if (token == '(') {
      _token = token;

      foreignColumns = parseColumnNames();
    }
    else
      _token = token;
  }

  /**
   * Parses a list of column names
   */
  public ArrayList<String> parseColumnNames()
    throws SQLException
  {
    ArrayList<String> columns = new ArrayList<String>();

    int token = scanToken();
    if (token == '(') {
      do {
        columns.add(parseIdentifier());

        token = scanToken();
      } while (token == ',');

      if (token != ')') {
        throw error(L.l("expected ')' at '{0}'", tokenName(token)));
      }
    }
    else if (token == IDENTIFIER) {
      columns.add(_lexeme);

      _token = token;
    }
    else
      throw error(L.l("expected '(' at '{0}'", tokenName(token)));

    return columns;
  }

  /**
   * Parses the insert.
   */
  private Query parseInsert()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != INTO)
      throw error(L.l("expected INTO at `{0}'", tokenName(token)));

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    Table table = _database.getTable(_lexeme);

    if (table == null)
      throw error(L.l("unknown table `{0}'", tokenName(token)));

    FromItem fromItem = new FromItem(table, table.getName());
    FromItem[] fromList = new FromItem[] { fromItem };

    ArrayList<Column> columns = new ArrayList<Column>();

    if ((token = scanToken()) == '(') {
      do {
        String columnName = parseIdentifier();

        Column column = table.getColumn(columnName);

        if (column == null)
          throw new SQLException(L.l("`{0}' is not a valid column in {1}",
                                     columnName, table.getName()));
        columns.add(column);
      } while ((token = scanToken()) == ',');

      if (token != ')')
        throw error(L.l("expected ')' at `{0}'", tokenName(token)));

      token = scanToken();
    }
    else {
      Column []columnArray = table.getColumns();

      for (int i = 0; i < columnArray.length; i++)
        columns.add(columnArray[i]);
    }

    if (token != VALUES)
      throw error(L.l("expected VALUES at `{0}'", tokenName(token)));

    if ((token = scanToken()) != '(')
      throw error(L.l("expected '(' at `{0}'", tokenName(token)));

    ArrayList<Expr> values = new ArrayList<Expr>();

    InsertQuery query = new InsertQuery(_database, _sql, table, columns);
    _query = query;

    int i = 0;
    do {
      Expr expr = parseExpr();

      expr = expr.bind(new TempQuery(fromList));

      values.add(expr);

      i++;
    } while ((token = scanToken()) == ',');

    if (token != ')')
      throw error(L.l("expected ')' at {0}", tokenName(token)));

    if (columns.size() != values.size())
      throw error(L.l("number of columns does not match number of values"));

    ParamExpr []params = _params.toArray(new ParamExpr[_params.size()]);

    query.setParams(params);
    query.setValues(values);
    query.init();

    return query;
  }

  /**
   * Parses the delete.
   */
  private Query parseDelete()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != FROM)
      throw error(L.l("expected FROM at `{0}'", tokenName(token)));

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    Table table = _database.getTable(_lexeme);

    if (table == null)
      throw error(L.l("unknown table `{0}'", tokenName(token)));

    DeleteQuery query = new DeleteQuery(_database, _sql, table);
    _query = query;

    Expr whereExpr = null;

    token = scanToken();
    if (token == WHERE)
      whereExpr = parseExpr();
    else if (token >= 0)
      throw error(L.l("expected WHERE at `{0}'", tokenName(token)));

    ParamExpr []params = _params.toArray(new ParamExpr[_params.size()]);

    query.setParams(params);
    query.setWhereExpr(whereExpr);

    return query;
  }

  /**
   * Parses the delete.
   */
  private Query parseValidate()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at '{0}'", tokenName(token)));

    Table table = _database.getTable(_lexeme);

    if (table == null)
      throw error(L.l("unknown table '{0}'", tokenName(token)));

    ValidateQuery query = new ValidateQuery(_database, _sql, table);

    return query;
  }

  /**
   * Parses the insert.
   */
  private Query parseDrop()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != TABLE)
      throw error(L.l("expected TABLE at `{0}'", tokenName(token)));

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    String table = _lexeme;

    if ((token = scanToken()) >= 0)
      throw error(L.l("expected end of query at `{0}'", tokenName(token)));

    return new DropQuery(_sql, _database, table);
  }

  /**
   * Parses the select.
   */
  private Query parseUpdate()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    String name = _lexeme;

    Table table = _database.getTable(name);

    if (table == null)
      throw error(L.l("`{0}' is an unknown table in INSERT.", name));

    if ((token = scanToken()) != SET)
      throw error(L.l("expected SET at {0}", tokenName(token)));

    UpdateQuery query = new UpdateQuery(_database, _sql, table);
    _query = query;

    ArrayList<SetItem> setItemList = new ArrayList<SetItem>();

    do {
      SetItem item = parseSetItem(table);

      setItemList.add(item);
    } while ((token = scanToken()) == ',');

    Expr whereExpr = null;

    if (token == WHERE)
      whereExpr = parseExpr();

    SetItem []setItems = new SetItem[setItemList.size()];
    setItemList.toArray(setItems);

    ParamExpr []params = _params.toArray(new ParamExpr[_params.size()]);

    query.setSetItems(setItems);
    query.setParams(params);
    query.setWhereExpr(whereExpr);

    return query;
  }
  
  /*
  private Query parseShow()
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    String name = _lexeme;
    
    if (name.equalsIgnoreCase("tables"))
      return new ShowTablesQuery();
    else if (name.equalsIgnoreCase("databases"))
      return new ShowDatabasesQuery();
    else
      throw error(L.l("`{0}' is an unknown type in SHOW.", name));
  }
  */

  /**
   * Parses a set item.
   */
  private SetItem parseSetItem(Table table)
    throws SQLException
  {
    int token;

    if ((token = scanToken()) != IDENTIFIER)
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));

    Column column = table.getColumn(_lexeme);

    if (column == null)
      throw error(L.l("`{0}' is an unknown column in table {1}.",
                      _lexeme, table.getName()));

    if ((token = scanToken()) != EQ)
      throw error(L.l("expected `=' at {0}", tokenName(token)));

    Expr expr = parseExpr();

    return new SetItem(table, column, expr);
  }

  /**
   * Parses an expression.
   */
  private Expr parseExpr()
    throws SQLException
  {
    int token = scanToken();

    if (token == SELECT)
      return parseSubSelect();
    else {
      _token = token;
      return parseOrExpr();
    }
  }

  /**
   * Parses a sub-select expression.
   */
  private Expr parseSubSelect()
    throws SQLException
  {
    return parseSubSelect(new SelectQuery(_database, _sql));
  }

  /**
   * Parses a sub-select expression.
   */
  private Expr parseSubSelect(SelectQuery query)
    throws SQLException
  {
    parseSelect(query);

    SubSelectExpr expr = new SubSelectExpr(query);

    query.setSubSelect(expr);

    _andExpr.add(new SubSelectEvalExpr(expr));

    return expr;
  }

  /**
   * Parses an OR expression.
   */
  private Expr parseOrExpr()
    throws SQLException
  {
    Expr left = parseAndExpr();

    while (true) {
      int token = scanToken();

      switch (token) {
      case OR:
        left = new OrExpr(left, parseAndExpr());
        break;

      default:
        _token = token;
        return left;
      }
    }
  }

  /**
   * Parses an AND expression.
   */
  private Expr parseAndExpr()
    throws SQLException
  {
    AndExpr oldAndExpr = _andExpr;
    AndExpr andExpr = new AndExpr();
    _andExpr = andExpr;

    andExpr.add(parseNotExpr());

    while (true) {
      int token = scanToken();

      switch (token) {
      case AND:
        andExpr.add(parseNotExpr());
        break;

      default:
        _token = token;

        _andExpr = oldAndExpr;

        return andExpr.getSingleExpr();
      }
    }
  }

  /**
   * Parses a term.
   */
  private Expr parseNotExpr()
    throws SQLException
  {
    int token = scanToken();

    switch (token) {
    case NOT:
      return new NotExpr(parseNotExpr());

    default:
      _token = token;
      return parseCmpExpr();
    }
  }

  /**
   * Parses a CMP expression.
   */
  private Expr parseCmpExpr()
    throws SQLException
  {
    Expr left = parseConcatExpr();

    int token = scanToken();
    boolean isNot = false;

    if (token == NOT) {
      isNot = true;

      token = scanToken();

      if (token != BETWEEN && token != LIKE && token != IN) {
        _token = token;

        return left;
      }
    }

    switch (token) {
    case EQ:
      return new EqExpr(left, parseConcatExpr());

    case LT:
    case LE:
    case GT:
    case GE:
    case NE:
      return new CmpExpr(left, parseConcatExpr(), token);

    case BETWEEN:
      {
        Expr min = parseConcatExpr();

        token = scanToken();
        if (token != AND)
          throw error(L.l("expected AND at '{0}'", tokenName(token)));

        Expr max = parseConcatExpr();

        return new BetweenExpr(left, min, max, isNot);
      }

    case IS:
      {
        token = scanToken();
        isNot = false;
        if (token == NOT) {
          token = scanToken();
          isNot = true;
        }

        if (token == NULL)
          return new IsNullExpr(left, isNot);
        else
          throw error(L.l("expected NULL at '{0}'", tokenName(token)));
      }

    case LIKE:
      {
        token = scanToken();

        if (token == STRING)
          return new LikeExpr(left, _lexeme, isNot);
        else
          throw error(L.l("expected string at '{0}'", tokenName(token)));
      }

    case IN:
      {
        HashSet<String> values = parseInValues();

        return new InExpr(left, values, isNot);
      }

    default:
      _token = token;
      return left;
    }
  }

  /**
   * Parses the IN values.
   */
  private HashSet<String> parseInValues()
    throws SQLException
  {
    int token = scanToken();

    if (token != '(')
      throw error(L.l("Expected '('"));

    HashSet<String> values = new HashSet<String>();

    while ((token = scanToken()) != ')') {
      if (token == STRING) {
        values.add(_lexeme);
      }
      else
        throw error(L.l("expected STRING at {0}", tokenName(token)));

      if ((token = scanToken()) != ',')
        break;
    }

    if (token != ')')
        throw error(L.l("expected ')' at {0}", tokenName(token)));

    return values;
  }

  /**
   * Parses a concat expression.
   */
  private Expr parseConcatExpr()
    throws SQLException
  {
    Expr left = parseAddExpr();

    while (true) {
      int token = scanToken();

      switch (token) {
      case CONCAT:
        left = new ConcatExpr(left, parseAddExpr());
        break;

      default:
        _token = token;
        return left;
      }
    }
  }

  /**
   * Parses a +/- expression.
   */
  private Expr parseAddExpr()
    throws SQLException
  {
    Expr left = parseMulExpr();

    while (true) {
      int token = scanToken();

      switch (token) {
      case '+':
      case '-':
        left = new BinaryExpr(left, parseMulExpr(), token);
        break;

      default:
        _token = token;
        return left;
      }
    }
  }

  /**
   * Parses a mul/div expression
   */
  private Expr parseMulExpr()
    throws SQLException
  {
    Expr left = parseTerm();

    while (true) {
      int token = scanToken();

      switch (token) {
      case '*':
      case '/':
      case '%':
        left = new BinaryExpr(left, parseTerm(), token);
        break;

      default:
        _token = token;
        return left;
      }
    }
  }

  /**
   * Parses a term.
   */
  private Expr parseTerm()
    throws SQLException
  {
    int token = scanToken();

    switch (token) {
    case '+':
      return parseTerm();

    case '-':
      return new UnaryExpr(parseTerm(), token);

    case '(':
      Expr expr = parseExpr();
      int peekToken;
      if ((peekToken = scanToken()) != ')')
        throw error(L.l("expected ')' at {0}", tokenName(peekToken)));
      return expr;

    default:
      _token = token;
      return parseSimpleTerm();
    }
  }

  /**
   * Parses a simple term.
   */
  private Expr parseSimpleTerm()
    throws SQLException
  {
    int token = scanToken();

    switch (token) {
    case IDENTIFIER:
      {
        String name = _lexeme;

        token = scanToken();
        if (token == '.') {
          token = scanToken();

          if (token == IDENTIFIER) {
            String column = _lexeme;
            return _query.bind(name, column);
          }
          else if (token == '*') {
            return new UnboundStarExpr(name);
          }
          else
            throw error("expected IDENTIFIER");
        }
        else if (token == '(') {
          FunExpr fun = null;
          if (name.equalsIgnoreCase("max"))
            fun = new MaxExpr();
          else if (name.equalsIgnoreCase("min"))
            fun = new MinExpr();
          else if (name.equalsIgnoreCase("sum"))
            fun = new SumExpr();
          else if (name.equalsIgnoreCase("avg"))
            fun = new AvgExpr();
          else if (name.equalsIgnoreCase("count")) {
            fun = new CountExpr();

            token = scanToken();
            if (token == '*') {
              fun.addArg(new UnboundStarExpr());
            }
            else
              _token = token;
          }
          else if (name.equalsIgnoreCase("exists")) {
            token = scanToken();

            if (token != SELECT)
              throw error(L.l("exists requires SELECT at '{0}'",
                              tokenName(token)));

            ExistsQuery query = new ExistsQuery(_database, _sql);

            parseSelect(query);

            ExistsExpr expr = new ExistsExpr(query);

            query.setSubSelect(expr);

            _andExpr.add(new ExistsEvalExpr(expr));

            token = scanToken();

            if (token != ')')
              throw error(L.l("exists requires ')' at '{0}'",
                              tokenName(token)));

            return expr;
          }
          else {
            String funName = (Character.toUpperCase(name.charAt(0)) +
                              name.substring(1).toLowerCase());

            funName = "com.caucho.db.fun." + funName + "Expr";

            try {
              Class cl = Class.forName(funName);

              fun = (FunExpr) cl.newInstance();
            } catch (ClassNotFoundException e) {
              log.finer(e.toString());
            } catch (Throwable e) {
              log.log(Level.FINER, e.toString(), e);
            }

            if (fun == null)
              throw error(L.l("`{0}' is an unknown function.", name));
          }

          token = scanToken();
          while (token > 0 && token != ')') {
            _token = token;

            Expr arg = parseExpr();

            fun.addArg(arg);

            token = scanToken();

            if (token == ',')
              token = scanToken();
          }

          return fun;
        }
        else {
          _token = token;
          return _query.bind(null, name);
        }
      }

    case STRING:
      return new StringExpr(_lexeme);

    case DOUBLE:
    case INTEGER:
    case LONG:
      return NumberExpr.create(_lexeme);

    case NULL:
      return new NullExpr();

    case TRUE:
      return BooleanLiteralExpr.create(true);

    case FALSE:
      return BooleanLiteralExpr.create(false);

    case '?':
      ParamExpr param = new ParamExpr(_params.size());
      _params.add(param);
      return param;

    default:
      throw error(L.l("unexpected term {0}", tokenName(token)));
    }
  }

  /**
   * Parses an identifier.
   */
  private String parseIdentifier()
    throws SQLException
  {
    int token = scanToken();

    if (token != IDENTIFIER)
      throw error(L.l("expected identifier at {0}", tokenName(token)));

    return _lexeme;
  }

  /**
   * Scan the next token.  If the lexeme is a string, its string
   * representation is in "lexeme".
   *
   * @return integer code for the token
   */
  private int scanToken()
    throws SQLException
  {
    if (_token > 0) {
      int value = _token;
      _token = -1;
      return value;
    }

    int sign = 1;
    int ch;

    for (ch = read(); Character.isWhitespace((char) ch); ch = read()) {
    }

    switch (ch) {
    case -1:
    case '(':
    case ')':
    case '.':
    case '*':
    case '/':
    case '%':
    case ',':
    case '?':
      return ch;

    case '+':
      if ((ch = read()) >= '0' && ch <= '9')
        break;
      else {
        unread(ch);
        return '+';
      }

    case '-':
      if ((ch = read()) >= '0' && ch <= '9') {
        sign = -1;
        break;
      }
      else {
        unread(ch);
        return '-';
      }

    case '=':
      return EQ;

    case '<':
      if ((ch = read()) == '=')
        return LE;
      else if (ch == '>')
        return NE;
      else {
        unread(ch);
        return LT;
      }

    case '>':
      if ((ch = read()) == '=')
        return GE;
      else {
        unread(ch);
        return GT;
      }

    case '|':
      if ((ch = read()) == '|')
        return CONCAT;
      else {
        throw error(L.l("'|' expected at {0}", charName(ch)));
      }

      // @@ is useless?
    case '@':
      if ((ch = read()) != '@')
        throw error(L.l("`@' expected at {0}", charName(ch)));
      return scanToken();
    }

    if (Character.isJavaIdentifierStart((char) ch)) {
      CharBuffer cb = _cb;
      cb.clear();

      for (; ch > 0 && Character.isJavaIdentifierPart((char) ch); ch = read())
        cb.append((char) ch);

      unread(ch);

      _lexeme = cb.toString();
      String lower = _lexeme.toLowerCase();

      int token = _reserved.get(lower);

      if (token > 0)
        return token;
      else
        return IDENTIFIER;
    }
    else if (ch >= '0' && ch <= '9') {
      CharBuffer cb = _cb;
      cb.clear();

      int type = INTEGER;

      if (sign < 0)
        cb.append('-');

      for (; ch >= '0' && ch <= '9'; ch = read())
        cb.append((char) ch);

      if (ch == '.') {
        type = DOUBLE;

        cb.append('.');
        for (ch = read(); ch >= '0' && ch <= '9'; ch = read())
          cb.append((char) ch);
      }

      if (ch == 'e' || ch == 'E') {
        type = DOUBLE;

        cb.append('e');
        if ((ch = read()) == '+' || ch == '-') {
          cb.append((char) ch);
          ch = read();
        }

        if (! (ch >= '0' && ch <= '9'))
          throw error(L.l("exponent needs digits at {0}",
                          charName(ch)));

        for (; ch >= '0' && ch <= '9'; ch = read())
          cb.append((char) ch);
      }

      if (ch == 'F' || ch == 'D')
        type = DOUBLE;
      else if (ch == 'L') {
        type = LONG;
      }
      else
        unread(ch);

      _lexeme = cb.toString();

      return type;
    }
    else if (ch == '\'') {
      CharBuffer cb = _cb;
      cb.clear();

      for (ch = read(); ch >= 0; ch = read()) {
        if (ch == '\'') {
          if ((ch = read()) == '\'')
            cb.append('\'');
          else {
            unread(ch);
            break;
          }
        }
        else if (ch == '\\') {
          ch = read();

          if (ch >= 0)
            cb.append(ch);
        }
        else
          cb.append((char) ch);
      }

      _lexeme = cb.toString();

      return STRING;
    }
    else if (ch == '#') {
      // skip comment
      while ((ch = read()) >= 0 && ch != '\n' && ch != '\r') {
      }

      // XXX: cleanup to avoid recursion
      return scanToken();
    }

    throw error(L.l("unexpected char at {0} ({1})", "" + (char) ch,
                    String.valueOf(ch)));
  }

  /**
   * Returns the next character.
   */
  private int read()
  {
    if (_parseIndex < _sqlLength)
      return _sqlChars[_parseIndex++];
    else
      return -1;
  }

  /**
   * Unread the last character.
   */
  private void unread(int ch)
  {
    if (ch >= 0)
      _parseIndex--;
  }

  /**
   * Returns the name for a character
   */
  private String charName(int ch)
  {
    if (ch < 0)
      return L.l("end of query");
    else
      return String.valueOf((char) ch);
  }

  /**
   * Returns the name of a token
   */
  private String tokenName(int token)
  {
    switch (token) {
    case AS: return "AS";
    case ARG: return "?";
    case FROM: return "FROM";
    case IN: return "IN";
    case SELECT: return "SELECT";
    case WHERE: return "WHERE";
    case OR: return "OR";
    case AND: return "AND";
    case NOT: return "NOT";
    case BETWEEN: return "BETWEEN";
    case TRUE: return "TRUE";
    case FALSE: return "FALSE";
    case NULL: return "NULL";
    case GROUP: return "GROUP";
    case ORDER: return "ORDER";
    case BY: return "BY";
    case ASC: return "ASC";
    case DESC: return "DESC";
    case LIMIT: return "LIMIT";
    
    case INSERT: return "INSERT";
    case DELETE: return "DELETE";

    case -1:
      return L.l("end of query");

    default:
      if (token < 128)
        return "'" + String.valueOf((char) token) + "' (" + token + ")";
      else
        return "'" + _lexeme + "' (" + token + ", '" + _lexeme.toLowerCase() + "')";
    }
  }

  private SQLException error(String msg)
  {
    return new SQLParseException(msg + "\n" + _sql);
  }

  static {
    _reserved = new IntMap();
    _reserved.put("as", AS);
    _reserved.put("from", FROM);
    _reserved.put("in", IN);
    _reserved.put("select", SELECT);
    _reserved.put("distinct", DISTINCT);
    _reserved.put("where", WHERE);
    _reserved.put("order", ORDER);
    _reserved.put("group", GROUP);
    _reserved.put("by", BY);
    _reserved.put("asc", ASC);
    _reserved.put("desc", DESC);
    _reserved.put("limit", LIMIT);
    _reserved.put("offset", OFFSET);

    _reserved.put("or", OR);
    _reserved.put("and", AND);
    _reserved.put("not", NOT);

    _reserved.put("between", BETWEEN);
    _reserved.put("like", LIKE);
    _reserved.put("escape", ESCAPE);
    _reserved.put("is", IS);

    _reserved.put("true", TRUE);
    _reserved.put("false", FALSE);
    _reserved.put("unknown", UNKNOWN);
    _reserved.put("null", NULL);

    _reserved.put("create", CREATE);
    _reserved.put("table", TABLE);
    _reserved.put("insert", INSERT);
    _reserved.put("into", INTO);
    _reserved.put("values", VALUES);
    _reserved.put("drop", DROP);
    _reserved.put("update", UPDATE);
    _reserved.put("set", SET);
    _reserved.put("delete", DELETE);
    _reserved.put("validate", VALIDATE);

    _reserved.put("constraint", CONSTRAINT);
    _reserved.put("unique", UNIQUE);
    _reserved.put("check", CHECK);
    _reserved.put("primary", PRIMARY);
    _reserved.put("key", KEY);
    _reserved.put("foreign", FOREIGN);
  }
}
