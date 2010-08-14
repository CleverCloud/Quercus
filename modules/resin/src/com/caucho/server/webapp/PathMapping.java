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

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

/**
 * Configuration for a path-mapping.
 */
public class PathMapping {
  static final L10N L = new L10N(PathMapping.class);

  // The path-mapping pattern
  private String _urlPattern;
  // The path-mapping regexp pattern
  private String _urlRegexp;
  
  // The real path
  private String _realPath;

  /**
   * Creates the path mapping.
   */
  public PathMapping()
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
   * Gets the urlRegexp.
   */
  public String getUrlRegexp()
  {
    return _urlRegexp;
  }

  /**
   * Sets the real path
   */
  public void setRealPath(String realPath)
  {
    _realPath = realPath;
  }

  /**
   * Gets the real path
   */
  public String getRealPath()
  {
    return _realPath;
  }

  /**
   * Init
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_urlPattern != null) {
    }
    else if (_urlRegexp != null) {
    }
    else
      throw new ServletException(L.l("path-mapping needs 'url-pattern' attribute."));
    if (_realPath == null)
      throw new ServletException(L.l("path-mapping needs 'real-path' attribute."));
  }
}
