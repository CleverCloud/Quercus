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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-specific java.util.logging.Logger configuration.
 */
public class LoggerConfig {
  private static final L10N L = new L10N(LoggerConfig.class);

  private Logger _logger;
  private Level _level = Level.INFO;
  private Boolean _useParentHandlers;
  
  private Filter _filter;
  
  private ArrayList<Handler> _handlerList
    = new ArrayList<Handler>();

  /**
   * Sets the name of the logger to configure.
   */
  public void setName(String name)
  {
    _logger = Logger.getLogger(name);
  }

  /**
   * Sets the use-parent-handlers
   */
  public void setUseParentHandlers(boolean useParentHandlers)
  {
    _useParentHandlers = new Boolean(useParentHandlers);
  }
  
  /**
   * Sets the output level.
   */
  public void setLevel(String level)
    throws ConfigException
  {
    if (level.equals("off"))
      _level = Level.OFF;
    else if (level.equals("severe"))
      _level = Level.SEVERE;
    else if (level.equals("warning"))
      _level = Level.WARNING;
    else if (level.equals("info"))
      _level = Level.INFO;
    else if (level.equals("config"))
      _level = Level.CONFIG;
    else if (level.equals("fine"))
      _level = Level.FINE;
    else if (level.equals("finer"))
      _level = Level.FINER;
    else if (level.equals("finest"))
      _level = Level.FINEST;
    else if (level.equals("all"))
      _level = Level.ALL;
    else {
      try {
        _level = Level.parse(level);
      } catch (IllegalArgumentException e) {
        throw new ConfigException(L.l("`{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging\n[-]?[0-9]+ - custom level", level));
      }
    }
  }
  
  public void add(Handler handler)
  {
    _handlerList.add(handler);
  }
  
  public void add(Filter filter)
  {
    _filter = filter;
  }

  /**
   * Initialize the logger.
   */
  @PostConstruct
  public void init()
  {
    if (_logger == null)
      throw new ConfigException(L.l("<logger> requires a 'name' attribute."));
    
    if (_level != null)
      _logger.setLevel(_level);

    if (_useParentHandlers != null)
      _logger.setUseParentHandlers(_useParentHandlers.booleanValue());
    
    for (Handler handler : _handlerList) {
      _logger.addHandler(handler);
    }
    
    if (_filter != null)
      _logger.setFilter(_filter);
  }
}
