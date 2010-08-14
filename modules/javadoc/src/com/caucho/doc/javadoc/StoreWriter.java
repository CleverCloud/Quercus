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

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StoreWriter {
  static protected final Logger log = Log.open(StoreWriter.class);
  static final L10N L = new L10N(StoreWriter.class);

  private PreparedStatement _stmtJavadocFile;
  private PreparedStatement _stmtJavadocItem;

  private HashMap<String,JavadocFile> _javadocFiles = new HashMap<String,JavadocFile>();
  private int _javadocFileNextId = 1;
  private int _javadocItemNextId = 1;

  Store _store;
  Api _currApi;
  int _addCount;

  StoreWriter(Store store)
  {
    _store = store;
  }
    
  /**
   * Clear out the old database tables
   */
  public void clear(Connection conn)
    throws SQLException
  {
    CharBuffer cb = CharBuffer.allocate();

    conn.setAutoCommit(true);

    Statement stmt = conn.createStatement();

    // drop tables, allow errors
    String q = null;

    try {
      cb.clear();
      cb.append("DROP TABLE ");
      cb.append(_store.getTableNameFile());
      q = cb.toString();
      log.fine(q);
      stmt.executeUpdate(q);

      cb.clear();
      cb.append("DROP TABLE ");
      cb.append(_store.getTableNameItem());
      q = cb.toString();
      log.fine(q);
      stmt.executeUpdate(q);
    } catch (SQLException ex) {
      log.log(Level.FINE,q,ex);
    }

    // create tables and indexes, allow errors

    try {
      cb.clear();
      cb.append("CREATE TABLE ");
      cb.append(_store.getTableNameFile());
      cb.append(" (id INT NOT NULL, path VARCHAR(255), api VARCHAR(255), PRIMARY KEY (id))");
      q = cb.toString();
      log.fine(q);
      stmt.executeUpdate(q);
      
      cb.clear();
      cb.append("CREATE TABLE ");
      cb.append(_store.getTableNameItem());
      cb.append(" (id INT NOT NULL, name VARCHAR(255), fullname VARCHAR(255), typ INT, file_id INT, anchor VARCHAR(255), description TEXT, PRIMARY KEY (id))"); 
      q = cb.toString();
      log.fine(q); 
      stmt.executeUpdate(q); 

      cb.clear();
      cb.append("CREATE INDEX ");
      cb.append(_store.getTableNameItem());
      cb.append("NIdx ON ");
      cb.append(_store.getTableNameItem());
      cb.append(" (name)");
      q = cb.toString();
      log.fine(q); 
      stmt.executeUpdate(q); 

      cb.clear();
      cb.append("CREATE INDEX ");
      cb.append(_store.getTableNameItem());
      cb.append("FNIdx ON ");
      cb.append(_store.getTableNameItem());
      cb.append(" (fullname)");
      q = cb.toString();
      log.fine(q); 
      stmt.executeUpdate(q); 

    }
    catch (SQLException ex) {
      log.log(Level.FINE,q,ex);
    }

    // XXX: resin db cannot do drop , must allow error on delete
    //
    // delete all rows from tables
    try {
      cb.clear();
      cb.append("DELETE FROM ");
      cb.append(_store.getTableNameFile());
      q = cb.toString();
      log.fine(q);
      stmt.executeUpdate(q);

      cb.clear();
      cb.append("DELETE FROM ");
      cb.append(_store.getTableNameItem());
      q = cb.toString();
      log.fine(q);
      stmt.executeUpdate(q);
    }
    catch (SQLException ex) {
        log.log(Level.FINE,q,ex);
    }

    stmt.close();
    cb.free();
  }

  /**
   * Add index entries to the database for the passed api.
   *
   * @return the number of index items added
   */
  public int add(Connection conn, Api api)
    throws SQLException, IOException
  {
    _addCount = 0; 

    String q = null;

    conn.setAutoCommit(true);

    // PreparedStatement for updating each table
    q = "INSERT INTO " + _store.getTableNameFile() + "  (id,path,api) VALUES (?,?,?)";
    log.finer(q);
    _stmtJavadocFile = conn.prepareStatement(q);
    _stmtJavadocFile.setString(3,api.getId());

    q = "INSERT INTO " + _store.getTableNameItem() + " (id, name, fullname, typ, file_id, anchor, description) VALUES (?,?,?,?,?,?,?)";
    log.finer(q);
    _stmtJavadocItem = conn.prepareStatement(q);

    ArrayList<Path> paths = api.getIndexes();

    log.info(L.l("  adding entries for `{0}'", api.getName()));

    for (int i = 0; i < paths.size(); i++) {
      Path path = paths.get(i);
      _currApi = api;

      int addCount = _addCount;

      parseFile(path);

      if (_addCount - addCount == 0) {
        log.warning(L.l("  {0} entries found for `{1}', is this not a javadoc index file?", new Integer(_addCount), api.getName()));
      }
    }

    if (log.isLoggable(Level.INFO))
      log.info(L.l("  added {0} entries for `{1}'", new Integer(_addCount), api.getName()));
    
    return _addCount;
  }

  private void parseFile(Path path)
    throws IOException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(L.l("reading from index file {0}", path));
    ReadStream in = path.openRead();

    IndexParser indexParser = new IndexParser(in,
        new IndexParser.Callback() {
          public void item(String path, String anchor, String name, String fullname, int typ, int modifier, String description)
          {
            _addCount++;

            int jditype;

            switch (typ) {

              case IndexParser.TYPE_CONSTRUCTOR:
                return; // ignore constructors

              case IndexParser.TYPE_PACKAGE:

                jditype = JavadocItem.PACKAGE;
                break;

              case IndexParser.TYPE_CLASS:
              case IndexParser.TYPE_ENUM:
              case IndexParser.TYPE_INTERFACE:
              case IndexParser.TYPE_EXCEPTION:
              case IndexParser.TYPE_ERROR:
              case IndexParser.TYPE_ANNOTATION:

                jditype = JavadocItem.CLASS;
                break;

              case IndexParser.TYPE_METHOD:

                jditype = JavadocItem.METHOD;
                break;

              case IndexParser.TYPE_VARIABLE:

                jditype = JavadocItem.VARIABLE;
                break;

              default:
                throw new RuntimeException(L.l("unknown typ {0}",new Integer(typ)));
            }

            try {
              JavadocFile file = makeJavadocFile(_currApi,path);
              JavadocItem item = makeJavadocItem(name, fullname, jditype, anchor, description,file);
            }
            catch (SQLException ex) {
              throw new RuntimeException(ex);
            }
          }
        });

    indexParser.parse();
  }

  JavadocFile makeJavadocFile(Api api, String path)
    throws SQLException
  {
    CharBuffer cbkey = CharBuffer.allocate();
    cbkey.append(api.getId());
    cbkey.append('|');
    cbkey.append(path);
    String key = cbkey.close();

    JavadocFile file = _javadocFiles.get(key);
    if (file == null) {
      file = new JavadocFile(api,_javadocFileNextId++,path);


      CharBuffer cb = CharBuffer.allocate();

      _stmtJavadocFile.setInt(1,file.getId());
      _stmtJavadocFile.setString(2,file.getPath());
      _stmtJavadocFile.executeUpdate();

      _javadocFiles.put(key,file);
    }

    return file;
  }

  private JavadocItem makeJavadocItem(String name, String fullname, int typ, String anchor, String description,JavadocFile file)
    throws SQLException
  {
    JavadocItem item = new JavadocItem(name,fullname,typ,anchor,description,file);
    _stmtJavadocItem.setInt(1,_javadocItemNextId++);
    _stmtJavadocItem.setString(2,item.getName());
    _stmtJavadocItem.setString(3,item.getFullName());
    _stmtJavadocItem.setInt(4,item.getType());
    _stmtJavadocItem.setInt(5,item.getFile().getId());
    _stmtJavadocItem.setString(6,item.getAnchor());
    _stmtJavadocItem.setString(7,item.getDescription());
    _stmtJavadocItem.executeUpdate();
    return item;
  }

}

