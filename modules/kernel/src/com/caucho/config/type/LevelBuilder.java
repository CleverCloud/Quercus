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
package com.caucho.config.type;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements a java.util.logging levels
 */
public class LevelBuilder extends ConfigType {
  private static final L10N L = new L10N(LevelBuilder.class);
  
  public static final LevelBuilder TYPE = new LevelBuilder();

  private static final HashMap<String,Level> _levelMap
    = new HashMap<String,Level>();

  private String _text;

  private LevelBuilder()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return Level.class;
  }

  /**
   * Replace with the real path.
   */
  public Object valueOf(String text)
  {
    if ("all".equals(text))
      return Level.ALL;
    else if ("finest".equals(text))
      return Level.FINEST;
    else if ("finer".equals(text))
      return Level.FINER;
    else if ("fine".equals(text))
      return Level.FINE;
    else if ("config".equals(text))
      return Level.CONFIG;
    else if ("info".equals(text))
      return Level.INFO;
    else if ("warning".equals(text))
      return Level.WARNING;
    else if ("severe".equals(text))
      return Level.SEVERE;
    else if ("off".equals(text))
      return Level.OFF;
    else {
      try {
        return Level.parse(text);
      } catch (IllegalArgumentException e) {
        throw new ConfigException(L.l("'{0}' is an unknown java.util.logging.Level",
                                      text));
      }
    }
  }
}
