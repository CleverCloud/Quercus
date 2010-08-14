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
import com.caucho.util.L10N;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.http.HttpServletRequest;

/**
 * Utilities common to all jsp pages.
 */
public class JspUtil {
  static protected final Logger log = Log.open(JspUtil.class);
  static final L10N L = new L10N(JspUtil.class);

  private HttpServletRequest _request;
  private HttpServletResponse _response;

  private Store _store;

  public JspUtil()
  {
  }

  public void setRequest(HttpServletRequest request)
  {
    _request = request;
  }

  public void setResponse(HttpServletResponse response)
  {
    _response = response;
  }

  /**
   * Get the Store object.
   */
  public Store getStore()
    throws JspException
  {
    if (_store == null) {
      try {
        _store = Store.getInstance();
      } catch (Exception ex) {
        throw new JspException(ex);
      }
    }

    return _store;
  }

  /**
   * Send appropriate HTTP cache headers based on the value of
   * http-cache-period for the Store.
   */
  public void sendHttpCacheHeaders()
    throws JspException
  {
    Store store = getStore();

    long period = getStore().getHttpCachePeriod();

    if (period < 0) {
      // disable caching
      _response.setHeader("Cache-Control","no-cache,post-check=0,pre-check=0,no-store");
      _response.setHeader("Pragma","no-cache");
      _response.setHeader("Expires","Thu,01Dec199416:00:00GMT");
    } else {
      long now = System.currentTimeMillis();
      _response.setDateHeader("Expires", now + period);
    }
  }

}

