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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.server.deploy.DeployConfig;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The configuration for a web-app in the resin.conf
 */
public class WebAppConfig extends DeployConfig {
  private static final L10N L = new L10N(WebAppConfig.class);
  private static final Logger log
    = Logger.getLogger(WebAppConfig.class.getName());

  // Any regexp
  private Pattern _urlRegexp;

  // The context path
  private String _contextPath;

  private WebAppConfig _prologue;

  public WebAppConfig()
  {
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    String cp = _contextPath;

    if (cp == null)
      cp = getId();

    if (cp == null)
      return null;

    if (cp.endsWith("/"))
      return cp.substring(0, cp.length() - 1);
    else
      return cp;
  }

  /**
   * Sets the context path
   */
  public void setContextPath(String path)
    throws ConfigException
  {
    if (! path.startsWith("/"))
      throw new ConfigException(L.l("context-path '{0}' must start with '/'.",
                                    path));
    
    _contextPath = path;
  }

  /**
   * Sets the url-regexp
   */
  public void setURLRegexp(String pattern)
  {
    if (! pattern.endsWith("$"))
      pattern = pattern + "$";
    if (! pattern.startsWith("^"))
      pattern = "^" + pattern;

    if (CauchoSystem.isCaseInsensitive())
      _urlRegexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    else
      _urlRegexp = Pattern.compile(pattern);
  }

  /**
   * Gets the regexp.
   */
  public Pattern getURLRegexp()
  {
    return _urlRegexp;
  }

  /**
   * Sets the app-dir.
   */
  public void setAppDir(RawString appDir)
  {
    setRootDirectory(appDir);
  }

  /**
   * Gets the app-dir.
   */
  public String getAppDir()
  {
    return getRootDirectory();
  }

  /**
   * Sets the app-dir.
   */
  public String getDocumentDirectory()
  {
    return getAppDir();
  }

  /**
   * Sets the app-dir.
   */
  public void setDocumentDirectory(RawString dir)
  {
    setRootDirectory(dir);
  }

  /**
   * Sets the startup-mode
   */
  public void setLazyInit(boolean isLazy)
    throws ConfigException
  {
    log.config(L.l("lazy-init is deprecated.  Use <startup-mode>lazy</startup-mode> instead."));

    if (isLazy)
      setStartupMode("lazy");
    else
      setStartupMode("automatic");
  }

  /**
   * Sets the prologue.
   */
  public void setPrologue(WebAppConfig prologue)
  {
    _prologue = prologue;
  }

  /**
   * Gets the prologue.
   */
  public DeployConfig getPrologue()
  {
    return _prologue;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextPath + "]";
  }
}
