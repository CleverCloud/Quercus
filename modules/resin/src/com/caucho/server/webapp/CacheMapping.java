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

package com.caucho.server.webapp;

import com.caucho.config.types.Period;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

/**
 * Configuration for a cache-mapping.
 */
public class CacheMapping {
  static L10N L = new L10N(CacheMapping.class);

  // The path-mapping pattern
  private String _urlPattern;
  private String _urlRegexp;
  
  // The period
  private long _maxAge = Long.MIN_VALUE;
  private long _sMaxAge = Long.MIN_VALUE;

  /**
   * Creates the path mapping.
   */
  public CacheMapping()
  {
  }

  /**
   * Sets the urlPattern
   */
  public void setUrlPattern(String urlPattern)
  {
    _urlPattern = urlPattern;
  }

  /**
   * Gets the urlPattern.
   */
  public String getUrlPattern()
  {
    return _urlPattern;
  }

  /**
   * Sets the urlRegexp
   */
  public void setUrlRegexp(String urlRegexp)
  {
    _urlRegexp = urlRegexp;
  }

  /**
   * Sets the urlRegexp
   */
  public String getUrlRegexp()
  {
    return _urlRegexp;
  }

  /**
   * Sets the period
   */
  public void setExpires(Period period)
  {
    setMaxAge(period);
  }

  /**
   * Sets the period
   */
  public void setMaxAge(Period period)
  {
    _maxAge = period.getPeriod();
  }

  /**
   * Gets the expires period.
   */
  public long getMaxAge()
  {
    return _maxAge;
  }

  /**
   * Sets the period
   */
  public void setSMaxAge(Period period)
  {
    _sMaxAge = period.getPeriod();
  }

  /**
   * Gets the expires period.
   */
  public long getSMaxAge()
  {
    return _sMaxAge;
  }

  /**
   * Init
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_urlPattern == null && _urlRegexp == null)
      throw new ServletException(L.l("cache-mapping needs 'url-pattern' attribute."));
    if (_maxAge == Long.MIN_VALUE && _sMaxAge == Long.MIN_VALUE)
      throw new ServletException(L.l("cache-mapping needs 'max-age' attribute."));
  }
}
