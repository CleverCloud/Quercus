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

package com.caucho.log;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.vfs.Path;

/**
 * Configuration for the error-log pattern (backwards compat).
 */
public class ErrorLog {
  private LogConfig _logConfig = new LogConfig();
  public ErrorLog()
    throws ConfigException
  {
    _logConfig.setName("");
    _logConfig.setLevel("info");
    _logConfig.setTimestamp("[%Y/%m/%d %H:%M:%S.%s] ");
  }
  
  /**
   * Sets the error log path (compat).
   */
  public void setId(Path path)
  {
    _logConfig.setPath(path);
  }
  
  /**
   * Sets the error log path (compat).
   */
  public void setTimestamp(String timestamp)
  {
    _logConfig.setTimestamp(timestamp);
  }
  
  /**
   * Sets the rotate period (compat).
   */
  public void setRolloverPeriod(Period period)
  {
    _logConfig.setRolloverPeriod(period);
  }
  
  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "error-log";
  }
}

