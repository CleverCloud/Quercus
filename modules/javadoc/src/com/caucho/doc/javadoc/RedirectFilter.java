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

import java.io.IOException;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.caucho.util.CharBuffer;

/**
 * Redirect to the search servlet if the first component of the path
 * is not a recognized api.
 */
public class RedirectFilter implements Filter {
  static protected final Logger log = Log.open(RedirectFilter.class);
  static final L10N L = new L10N(RedirectFilter.class);

  private final static String STORE_JNDINAME = "resin-javadoc/store";

  private Store _store;

  public void init(FilterConfig filterConfig)
    throws ServletException
  {
    try {
      Context env = (Context) new InitialContext().lookup("java:comp/env");

      _store = (Store) env.lookup(STORE_JNDINAME);

      if (_store == null)
        throw new ServletException(L.l("`{0}' is an unknown Store",STORE_JNDINAME));
    } catch (NamingException ex) {
      throw new ServletException(ex);
    }
  }

  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain chain)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String url = req.getPathInfo();
    int i = url.indexOf('/');
    if (i > -1) {
      String api = url.substring(0,i);
      url = url.substring(i);
      if (_store.getApi(api) == null) {
        // redirect to search
        CharBuffer redirect = CharBuffer.allocate();
        redirect.append(req.getRequestURI());
        redirect.setLength(redirect.length() - req.getPathInfo().length());
        redirect.append("index.jsp?query=");
        // rewrite it
        redirect.append(url);
        res.sendRedirect(res.encodeRedirectURL(url));
      }
    }

    chain.doFilter(request, response);
  }

  
  public void destroy()
  {
  }
  
}

