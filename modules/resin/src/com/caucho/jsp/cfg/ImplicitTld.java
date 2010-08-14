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

package com.caucho.jsp.cfg;

import com.caucho.config.DependencyBean;
import com.caucho.config.ConfigException;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import java.util.ArrayList;

/**
 * Configuration for the taglib in the .tld
 */
public class ImplicitTld
{
  private static final L10N L = new L10N(ImplicitTld.class);
  
  private String _tlibVersion;
  private String _jspVersion = "2.0";
  private String _shortName;

  public ImplicitTld()
  {
  }

  /**
   * Sets the tld version.
   */
  public void setVersion(String version)
  {
    _jspVersion = version;

    if (version.compareTo("2.0") < 0)
      throw new ConfigException(L.l("implicit.tld must have a 2.0 version or greater at '{0}'",
                                    version));
  }

  /**
   * Sets the schema location.
   */
  public void setSchemaLocation(String location)
  {
  }
  
  /**
   * Sets the taglib version.
   */
  public void setTlibVersion(String tlibVersion)
  {
    _tlibVersion = tlibVersion;
  }
  
  /**
   * Sets the taglib version (backwards compat).
   */
  public void setTlibversion(String tlibVersion)
  {
    setTlibVersion(tlibVersion);
  }

  /**
   * Gets the taglib version.
   */
  public String getTlibVersion()
  {
    return _tlibVersion;
  }

  /**
   * Sets the JSP version.
   */
  public void setJspVersion(String jspVersion)
  {
    _jspVersion = jspVersion;
  }

  /**
   * Sets the JSP version (backwards compat).
   */
  public void setJspversion(String jspVersion)
  {
    setJspVersion(jspVersion);
  }

  /**
   * Gets the jsp version.
   */
  public String getJspVersion()
  {
    return _jspVersion;
  }

  /**
   * Sets the short name (prefix)
   */
  public void setShortName(String shortName)
  {
    _shortName = shortName;
  }

  /**
   * Sets the short name (backwards compat)
   */
  public void setShortname(String shortName)
  {
    setShortName(shortName);
  }

  /**
   * Gets the short name (prefix)
   */
  public String getShortName()
  {
    return _shortName;
  }
}
