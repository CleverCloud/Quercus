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

package com.caucho.log;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.LoggerMXBean;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-specific java.util.logging.Logger configuration.
 */
public class LoggerAdmin extends AbstractManagedObject implements LoggerMXBean
{
  private static final L10N L = new L10N(LoggerAdmin.class);

  private final Logger _logger;
  private final ClassLoader _loader;
  private Level _level;

  LoggerAdmin(Logger logger)
  {
    _logger = logger;

    _loader = Thread.currentThread().getContextClassLoader();
  }
  
  /**
   * Sets the name of the logger to configure.
   */
  public String getName()
  {
    return _logger.getName();
  }
  
  /**
   * Sets the output level.
   */
  public void setLevel(String levelName)
  {
    Level level;
    
    if (levelName.equals("off"))
      level = Level.OFF;
    else if (levelName.equals("severe"))
      level = Level.SEVERE;
    else if (levelName.equals("warning"))
      level = Level.WARNING;
    else if (levelName.equals("info"))
      level = Level.INFO;
    else if (levelName.equals("config"))
      level = Level.CONFIG;
    else if (levelName.equals("fine"))
      level = Level.FINE;
    else if (levelName.equals("finer"))
      level = Level.FINER;
    else if (levelName.equals("finest"))
      level = Level.FINEST;
    else if (levelName.equals("all"))
      level = Level.ALL;
    else
      throw new IllegalArgumentException(L.l("`{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging",
                                             levelName));
  }

  public String getLevel()
  {
    return null;
  }
}
