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

import java.sql.SQLException;

import java.util.LinkedList;
import java.util.logging.Logger;

import javax.naming.NamingException;

/**
 * A query to be created and used from jsp.
 */
public class Query {
  static protected final Logger log = Log.open(Query.class);
  static final L10N L = new L10N(Query.class);

  private final static int LIMIT_DEFAULT = 15;
  private final static int LIMIT_MAX = 250;

  private Store _store;

  private String _query;
  private int _offset;
  private int _limit = LIMIT_DEFAULT;

  private LinkedList<JavadocItem> _results;

  public Query()
    throws NamingException
  {
    _store =  Store.getInstance();
  }

  protected void reset()
  {
    _results = null;
  }

  /**
   * The store that this Query should use.
   */
  public void setStore(Store store)
  {
    _store = store;
  }

  /**
   * The query string submitted by the user.
   */
  public void setQuery(String query)
  {
    _query = safeString(query);

    if (_query != null) {
      // turn com/caucho/vfs/Path.html#method() style submissions
      // into appropriate ones

      CharBuffer cb = new CharBuffer(_query);

      // take .html out
      int di = cb.indexOf(".html");
      if (di > -1)
        cb.delete(di,di+5);


      // take all (...) out
      while  ( (di = cb.indexOf('(')) > -1) {
        int di2 = cb.indexOf(')',di);
        if (di2 > di)
          cb.delete(di,di2+1);
      }

      for (int i = 0; i < cb.length(); i++) {
        char ch = cb.charAt(i);
        if (i > 0 && ch == '/'  || ch == '\\' || ch == '#')
          cb.setCharAt(i,'.');
      }

      _query = cb.toString();
    }

    reset();
  }

  /**
   * The query string submitted by the user.
   */
  public String getQuery()
  {
    return _query;
  }

  /**
   * An offset into the search results, for previous/next page
   * functionality.
   */
  public void setOffset(String offset)
  {
    _offset = safeParseInt(offset,_offset);
    reset();
  }

  /**
   * An offset into the search results, for previous/next page
   * functionality.
   */
  public int getOffset()
  {
    return _offset;
  }

  /**
   * A limit on the number of search results to return for one page.
   */
  public void setLimit(String limit)
  {
    _limit = safeParseInt(limit,_limit);
    if (_limit > LIMIT_MAX)
      _limit = LIMIT_MAX;
    reset();
  }

  /**
   * A limit on the number of search results to return for one page.
   */
  public int getLimit()
  {
    return _limit;
  }

  /**
   * Do the query and return the results.
   */
  public LinkedList<JavadocItem> getResults()
    throws SQLException
  {
    if (_results != null)
      return _results;

    if (_query == null)
      _results = new LinkedList<JavadocItem>();
    else
      _results = _store.query(_query,_offset,_limit);

    return _results;
  }

  /**
   * True if there is a next page.
   */
  public boolean getIsNextPage()
  {
    return _results != null && _results.size() == _limit;
  }

  /**
   * The offset to be used for a link to the next set of results.
   */
  public int getNextPageOffset()
  {
    return _offset + _limit;
  }

  /**
   * True if there is a previous page
   */
  public boolean getIsPreviousPage()
  {
    return _offset > 0;
  }

  /**
   * The offset to be used for a link to the previous set of results, null
   * if there is no next page.
   */
  public int getPreviousPageOffset()
  {
    int r = _offset - _limit;
    if (r < 0)
      r = 0;
    return r;
  }

  public String toString()
  {
    return _query;
  }

  private static String safeString(String s, String deflt)
  {
    if (s == null || s.length() == 0)
      return deflt;
    else
      return s;
  }

  private static String safeString(String s)
  {
    return safeString(s,null);
  }

  private static int safeParseInt(String s, int deflt)
  {
    if (s == null || s.length() == 0)
      return deflt;
    else {
      try {
        return Integer.parseInt(s);
      } catch (Exception ex) {
        return deflt;
      }
    }
  }

}

