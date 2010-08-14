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

package com.caucho.filters;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.UserTransaction;
import java.io.IOException;

/**
 * Wraps the request in a transaction.  All database calls for
 * the request will either succeed together or fail.
 *
 * @since Resin 2.0.5
 */
public class TransactionFilter implements Filter {
  /**
   * The UserTransaction object.
   */
  private UserTransaction _userTransaction;
  
  /**
   * Lookup java:comp/UserTransaction and cache the results.
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    try {
      Context ic = (Context) new InitialContext();
      
      _userTransaction = (UserTransaction) ic.lookup("java:comp/UserTransaction");
    } catch (NamingException e) {
      throw new ServletException(e);
    }
  }
  
  /**
   * Wrap the request in a transaction.  If the request returns normally,
   * the transaction will commit.  If an exception is thrown it will
   * rollback.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    try {
      _userTransaction.begin();

      nextFilter.doFilter(request, response);

      _userTransaction.commit();
    } catch (ServletException e) {
      rollback();
      throw e;
    } catch (IOException e) {
      rollback();
      throw e;
    } catch (RuntimeException e) {
      rollback();
      throw e;
    } catch (Throwable e) {
      rollback();
      throw new ServletException(e);
    }
  }

  /**
   * Rolls the request back.
   */
  private void rollback()
    throws ServletException
  {
    try {
      _userTransaction.rollback();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }
}
