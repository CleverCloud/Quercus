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

import java.sql.*;
import java.util.*;

import com.caucho.util.*;

/**
 * Quercus-specific mysql driver
 */
public class QuercusMysqlDriver implements Driver
{
  private static final L10N L = new L10N(QuercusMysqlDriver.class);

  private String _url;
  private String _host = "localhost";
  private int _port = 3306;
  private String _database;

  public void setUrl(String url)
  {
    _url = url;

    parseUrl(url);
  }

  public String getHost()
  {
    return _host;
  }

  public int getPort()
  {
    return _port;
  }

  public String getDatabase()
  {
    return _database;
  }

  public boolean acceptsURL(String url)
  {
    return false;
  }

  public Connection connect(String url, Properties info)
    throws SQLException
  {
    return new MysqlConnectionImpl(this, url, info);
  }

  public int getMajorVersion()
  {
    return 5;
  }

  public int getMinorVersion()
  {
    return 0;
  }

  public DriverPropertyInfo []getPropertyInfo(String url, Properties info)
  {
    return new DriverPropertyInfo[0];
  }

  public boolean jdbcCompliant()
  {
    return false;
  }

  private void parseUrl(String url)
  {
    if (url.startsWith("jdbc:mysql://")) {
      parseUrlCompat(url);
      return;
    }
    
    int p = url.indexOf(':');
    String scheme = url.substring(0, p);

    if (! "quercus-mysql".equals(scheme))
      throw new IllegalArgumentException(L.l("'{0}' is an illegal mysql scheme",
                                             url));

    int q = url.indexOf(':', p + 1);
    int r = url.indexOf(':', q + 1);

    if (q < 0)
      throw new IllegalArgumentException(L.l("'{0}' is an illegal mysql URL",
                                             url));

    _host = url.substring(p + 1, q);

    if (r < 0)
      _port = Integer.parseInt(url.substring(q + 1));
    else {
      _port = Integer.parseInt(url.substring(q + 1, r));
      _database = url.substring(r + 1);
    }
  }

  private void parseUrlCompat(String url)
  {
    if (! url.startsWith("jdbc:mysql://")) {
      throw new IllegalArgumentException(L.l("'{0}' is an illegal mysql scheme",
                                             url));
    }
    
    int p = url.indexOf("://");

    int q = url.indexOf(':', p + 3);
    int r = url.indexOf('/', q + 1);

    if (q < 0)
      throw new IllegalArgumentException(L.l("'{0}' is an illegal mysql URL",
                                             url));

    _host = url.substring(p + 3, q);

    if (r < 0)
      _port = Integer.parseInt(url.substring(q + 1));
    else {
      _port = Integer.parseInt(url.substring(q + 1, r));
      _database = url.substring(r + 1);

      p = _database.indexOf('?');
      if (p == 0) {
        _database = null;
      }
      else if (p > 0) {
        _database = _database.substring(0, p);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _host + ":" + _port + "," + _database + "]";
  }
}

