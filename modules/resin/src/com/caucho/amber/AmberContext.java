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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber;

import com.caucho.util.L10N;

import java.sql.SQLException;

/**
 * Context for amber.
 */
public class AmberContext {
  private static final L10N L = new L10N(AmberContext.class);
  
  private static final ThreadLocal<AmberContext> _localContext
    = new ThreadLocal<AmberContext>();

  private AmberFactory _factory;
  private int _depth;
  private AmberConnection _aConn;

  /**
   * Creates a new AmberContext.
   */
  private AmberContext(AmberFactory factory)
  {
    _factory = factory;
  }

  /**
   * Creates a new amber context.
   */
  public static AmberContext create(AmberFactory factory)
  {
    AmberContext context = _localContext.get();

    if (context != null)
      context._depth++;
    else {
      context = new AmberContext(factory);
      _localContext.set(context);
    }

    return context;
  }

  /**
   * Gets the current connection.
   */
  public static AmberConnection getConnection()
    throws SQLException
  {
    AmberContext context = _localContext.get();

    if (context == null)
      throw new AmberException(L.l("No amber context is available."));

    if (context._aConn == null)
      context._aConn = context._factory.getConnection();

    return context._aConn;
  }

  /**
   * Closes the connection.
   */
  public static void close()
  {
    AmberContext context = _localContext.get();

    if (context._depth == 0) {
      _localContext.set(null);
      
      AmberConnection conn = context._aConn;
      if (conn != null)
        conn.close();
    }
    else
      context._depth--;
  }
}
