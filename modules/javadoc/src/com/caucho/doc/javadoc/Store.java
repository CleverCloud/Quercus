/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits redistribution, modification and use
 * of this file in source and binary form ("the Software") under the
 * Caucho Developer Source License ("the License").  The following
 * conditions must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Redistributions of the Software in source or binary form must include
 *    an unmodified copy of the License, normally in a plain ASCII text
 *
 * 3. The names "Resin" or "Caucho" are trademarks of Caucho Technology and
 *    may not be used to endorse products derived from this software.
 *    "Resin" or "Caucho" may not appear in the names of products derived
 *    from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.
 *
 * @author Sam 
 */

package com.caucho.doc.javadoc;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.loader.Environment;
import com.caucho.log.Log;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.PathMapping;
import com.caucho.util.CharBuffer;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.sql.DataSource;

/**
 * A store for javadoc information.
 */
public class Store {
  static protected final Logger log = Log.open(Store.class);
  static final L10N L = new L10N(Store.class);

  private final static String STORE_JNDINAME = "resin-javadoc/store";

  private String _dataSource;
  private LinkedHashMap<String,Api> _api = new LinkedHashMap<String,Api>();

  private Path _timestampFile;
  private String _tableNameFile = "javadoc_file";
  private String _tableNameItem = "javadoc_item";
  private long _httpCachePeriod;

  private String _startPage = "overview.jsp";
  private boolean _showAddHelp;

  private long _crc64 = 0;
  private DataSource _pool;

  private Exception _initError;

  /** 
   * A convenience method to do the JNDI lookup
   */
  static public Store getInstance()
    throws NamingException
  {
    Context env = (Context) new InitialContext().lookup("java:comp/env");

    Store store = (Store) env.lookup(STORE_JNDINAME);

    if (store == null)
      throw new NamingException(L.l("`{0}' is an unknown Store",STORE_JNDINAME));
    return store;
  }

  /**
   * The data-source indicates the database to use.
   */
  public void setDataSource(String dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * A timestamp file is used to determine if the database needs to be
   * recreated from the javadoc, default "WEB-INF/timestamp".
   */
  public void setTimestampFile(Path timestampFile)
  {
    _timestampFile = timestampFile;
  }

  /**
   * An api is a javadoc generated set of html files.
   */
  public void addApi(Api api)
    throws ConfigException
  {
    if (_api.get(api.getId()) != null)
      throw new ConfigException(L.l("id `{0}' already used, id must be unique",api.getId()));
    _api.put(api.getId(),api);
  }

  /**
   * An api is a javadoc generated set of html files.
   */
  public Api getApi(String id)
  {
    return _api.get(id);
  }

  /**
   * An api is a javadoc generated set of html files.
   */
  public Collection<Api> getAllApi()
  {
    return _api.values();
  }

  /**
   * The table name for storing information about javadoc items, the default is
   * "javadoc_item".
   */
  public void setTableNameItem(String tableNameItem)
  {
    _tableNameItem = tableNameItem;
  }
  
  /**
   * The table name for storing information about javadoc items.
   */
  public String getTableNameItem()
  {
    return _tableNameItem;
  }
  
  /**
   * The table name for storing information about javadoc files, the default is
   * "javadoc_file".
   */
  public void setTableNameFile(String tableNameFile)
  {
    _tableNameFile = tableNameFile;
  }
  
  /**
   * The table name for storing information about javadoc files.
   */
  public String getTableNameFile()
  {
    return _tableNameFile;
  }

  /**
   * The time period to indicate to Resin's cache and to browsers
   * that the responses (including search results) should be cached.
   * If Resin's http cache is enabled, then the cached
   * result will be used for subsequent searches for the same thing, even
   * by a different user.  This is an effective way to avoid hitting the
   * database for every search.
   *
   * examples: 10m, 10h, 10D,
   * -1 disables (not a good idea, but useful during development).
   */
  public void setHttpCachePeriod(Period httpCachePeriod)
  {
    _httpCachePeriod = httpCachePeriod.getPeriod();
  }
  
  /**
   * The time period for cache expiry, in ms.
   */
  public long getHttpCachePeriod()
  {
    return _httpCachePeriod;
  }
  
  /**
   * The page to show in the class window for the first request, default
   * "overview.jsp".
   */
  public void setStartPage(String startPage)
  {
    _startPage = startPage;
  }
  
  /**
   * The page to show in the class window for the first request.
   */
  public String getStartPage()
  {
    return _startPage;
  }
  
  /**
   * True/false show a help message about adding more api's.
   */
  public void setShowAddHelp(boolean showAddHelp)
  {
    _showAddHelp = showAddHelp;
  }
  
  /**
   * True/false show a help message about adding more api's.
   */
  public boolean getShowAddHelp()
  {
    return _showAddHelp;
  }
  
  public void init()
  {
    try {
      if (_timestampFile == null)
        _timestampFile = Vfs.lookup("WEB-INF/timestamp");

      try {
        Context env = (Context) new InitialContext().lookup("java:comp/env");

        _pool = (DataSource) env.lookup(_dataSource);

        if (_pool == null)
          throw new ConfigException(L.l("`{0}' is an unknown DataSource, database has not been configured or is not configured properly.",_dataSource));
      } catch (NamingException e) {
        throw new ConfigException(e);
      }

      // update database if needed

      for (Iterator<Api> i = _api.values().iterator(); i.hasNext(); ) {
        Api api = i.next();
        _crc64 = api.generateCrc64(_crc64);
      }
      _crc64 = Crc64.generate(_crc64,_dataSource);
      _crc64 = Crc64.generate(_crc64,_timestampFile.toString());
      _crc64 = Crc64.generate(_crc64,_tableNameFile);
      _crc64 = Crc64.generate(_crc64,_tableNameItem);

      try {
        if (isNeedUpdate())
          createFromIndex();
      } catch (Exception ex) {
        throw new ConfigException(ex);
      }

      // add path-mappings to map local files with absolute paths

      WebApp app = WebApp.getLocal();

      CharBuffer cb = CharBuffer.allocate();

      for (Iterator<Api> i = _api.values().iterator(); i.hasNext(); ) {
        Api api = i.next();

        if (api.isLocalAbsolute()) {
          cb.clear();
          cb.append("/");
          cb.append(api.getId());
          cb.append("/*");
          String urlPattern = cb.toString();

          PathMapping pm = new PathMapping();
          pm.setUrlPattern(urlPattern);
          pm.setRealPath(api.getLocation());
          try {
            app.addPathMapping(pm);
          } catch (Exception ex) {
            throw new ConfigException(ex);
          }
        }
      }

      cb.free();


      // add dependencies to the Environment so that if a local api
      // is regenerated the web-app is restarted

      for (Iterator<Api> i = _api.values().iterator(); i.hasNext(); ) {
        Api api = i.next();
        if (api.isLocal()) {
          Path index = api.getLocationPath().lookup("index.html");
          Environment.addDependency(index);
        }
      }
    } catch (Exception ex) {
      log.log(Level.FINE,"resin-javadoc init error",ex);
      _initError = ex;
    }
  }

  public Exception getInitError()
  {
    return _initError;
  }

  /**
   * True if the timestamp file indicates that the database needs updating to
   * match the index file(s).
   */ 
  public boolean isNeedUpdate()
  {
    try {
      ReadStream rs = _timestampFile.openRead();
      String lms = rs.readLine();
      long crc = Long.parseLong(lms);
      lms = rs.readLine();
      long tsfiles = Long.parseLong(lms);
      rs.close();

      if (crc != _crc64) {
        log.info(L.l("javadoc index needs update - config has changed"));
        if (log.isLoggable(Level.FINE))
          log.finer(L.l("timestamp file {0} lastmodified {1}",_timestampFile,new Long(crc)));
      }
      else if (tsfiles != getTimestampForLocalFiles()) {
        log.info(L.l("javadoc index needs update - a local api has changed"));
      }
      else {
        return false;
      }
    } catch (Exception ex) {
      log.info(L.l("javadoc index needs update - timestamp file could not be read"));
      if (log.isLoggable(Level.FINE))
        log.finer(L.l("timestamp file {0} {1}",_timestampFile,ex));
    }

    return true;
  }

  /**
   * Clear the database tables (if they exist) and create new 
   * contents based on the passed javadoc index file(s).
   */ 
  public void createFromIndex()
    throws SQLException, IOException
  {
    long st = System.currentTimeMillis();
    int cnt = 0;

    log.info(L.l("creating javadoc index db entries"));
    Connection conn = null;
    try {
      conn = _pool.getConnection();

      StoreWriter sw = new StoreWriter(this);
      sw.clear(conn);
      for (Iterator<Api> i = _api.values().iterator(); i.hasNext(); ) {
        Api api = i.next();
        cnt += sw.add(conn,api);
      }
    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        log.warning(L.l("conn.close() error",e));
      }
    }

    updateTimestampFile();

    long tm = System.currentTimeMillis() - st;
    log.info(L.l("created {0} javadoc index entries in {1}sec", new Integer(cnt), new Double( (double) tm / 1000.0)));
  }

  private long getTimestampForLocalFiles()
  {
    long ts = 0L;
    for (Iterator<Api> i = _api.values().iterator(); i.hasNext(); ) {
      Api api = i.next();
      if (api.isLocal()) {
        ts += api.getLocationPath().lookup("index.html").getLastModified();
      }
    }

    return ts;
  }

  private void updateTimestampFile()
    throws IOException
  {
    _timestampFile.getParent().mkdirs();
    WriteStream ws = _timestampFile.openWrite();
    ws.println(_crc64);
    ws.println(getTimestampForLocalFiles());
    ws.close();
  }

  /**
   * Look for a javadoc item.  The string can begin with `package', `class',
   * `method', `var', or `any'.  If it begins with none of them, `any' is
   * assumed.  The rest of the string is used as the name to search for.
   * If the name conatins the `*' character, the `*' will match any 
   * characters.
   *
   * @returns a list of the results, a list with size 0 for no results 
   */
  public LinkedList<JavadocItem> query(String query, int offset, int limit)
    throws SQLException 
  {
    LinkedList<JavadocItem> results = new LinkedList<JavadocItem>();

    try {
      int type = -1;

      int i = query.indexOf(' ');
      if (i > -1) {
        String t = query.substring(0,i);
        if (t.equals("package"))
          type = JavadocItem.PACKAGE;
        else if (t.equals("class"))
          type = JavadocItem.CLASS;
        else if (t.equals("method"))
          type = JavadocItem.METHOD;
        else if (t.equals("var"))
          type = JavadocItem.VARIABLE;
        else if (t.equals("any"))
          type = JavadocItem.ANY;

        if (type > -1) {
          while (i < query.length() && Character.isWhitespace(query.charAt(i)))
            i++;
          if (i >= query.length())
            return results;
          else
            query = query.substring(i);
        }
      }


      // handle the special case of ClassName.method
      if (query.length() > 0 && Character.isUpperCase(query.charAt(0))) {
        int di = query.indexOf('.');
        if (di > -1 ) {
          CharBuffer cb = CharBuffer.allocate();
          cb.append('*');
          cb.append(query);
          cb.append('*');
          query = cb.close();
        }
      }

      if (type < 0)
        type = JavadocItem.ANY;

      Connection conn = null;
      try {
        conn = _pool.getConnection();

        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
        // construct the query or queries
        boolean like = query.indexOf("*") > -1 ? true : false; 
        String nameToUse = (query.indexOf(".") > -1) ? "item.fullname" : "item.name";
        String safequery = escapeSQL(query);

        CharBuffer cb = CharBuffer.allocate();
        cb.append("SELECT item.name,item.fullname,item.typ,item.anchor,item.description,file.api,file.path FROM ");
          cb.append(getTableNameItem());
          cb.append(" AS item, ");
          cb.append(getTableNameFile());
          cb.append(" AS file WHERE file.id = item.file_id");

        if (type != JavadocItem.ANY) {
          cb.append(" AND item.typ = ");
          cb.append(type);
        }
        String q_select = cb.close();

        if (like) { 
          cb = CharBuffer.allocate();
          cb.append(q_select);
          cb.append(" AND ");
          cb.append(nameToUse);
          cb.append(" LIKE '");
          cb.append(safequery);
          cb.append("'");

          results = doQuery(safequery,stmt,cb,offset,limit,results);
        }
        else {
          cb = CharBuffer.allocate();
          cb.append(q_select);
          cb.append(" AND ");
          cb.append(nameToUse);
          cb.append(" LIKE '");
          cb.append(safequery);
          cb.append("%'");
          results = doQuery(safequery,stmt,cb,offset,limit,results);
        }

        // see if the first one is exact
        if (results.size() > 0) {
          JavadocItem item = results.getFirst();
          if (item.getName().equals(query) || item.getFullName().equals(query)) {
            item.setExact(true);
          }
        }
        stmt.close();
      } finally {
        if (conn != null)
          conn.close();
      }
    } 
    catch (SQLException ex) {
      if (log.isLoggable(Level.CONFIG))
        log.log(Level.CONFIG,L.l("error with query `{0}': {1}",query,ex.getMessage()));
      throw ex;
    }

    return results;
  }

  private String escapeSQL(String q)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < q.length(); i++) {
      char c = q.charAt(i);
      switch (c) {
        case '\'':
          cb.append("\\'");
          break;
        case '\"':
          cb.append("\\\"");
          break;
        case '*':
          cb.append("%");
          break;

        case '%':
          cb.append('\\');
        default:
          cb.append(c);
      }
    }

    return cb.close();
  }

  private LinkedList<JavadocItem> doQuery(String query, Statement stmt, CharBuffer cb, int offset, int limit, LinkedList<JavadocItem> results)
    throws SQLException
  {
    if (limit <= 0) {
      return results;
    }

    cb.append(" ORDER BY if(item.name = '");
    cb.append(query);
    cb.append("' OR item.fullname = '");
    cb.append(query);
    cb.append("',0,1) ");
    cb.append(",item.typ");
    cb.append(", LENGTH(item.name)");
    cb.append(" LIMIT ");
    cb.append(limit);
    if (offset > 0) {
      cb.append(" OFFSET ");
      cb.append(offset);
    }


    String q = cb.close();
    log.finest(L.l("query is [{0}]",q));

    ResultSet rs = stmt.executeQuery(q);
    try {

      while (rs.next()) {
        results.add(new JavadocItem(
              rs.getString(1),                     // name
              rs.getString(2),                     // fullname
              rs.getInt(3),                        // type
              rs.getString(4),                     // anchor
              rs.getString(5),                      // description
              new JavadocFile(_api.get(rs.getString(6)), // api
                               rs.getString(7))          // path
              ));
      }
    } finally {
      rs.close();
    }

    return results;

  }

}

