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
 * @author Sam
 */

package com.caucho.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.CronType;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;
import javax.annotation.PostConstruct;

/**
 * Match if the request occurs during enabled times.
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"&gt;
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:IfCron>
 *     &lt;enable-at>* 8 * * *</enable-at>
 *     &lt;disable-at>* 16 * * *</disable-at>
 *   &lt;/resin:IfCron>
 * &lt;/resin:Allow>
 * </pre>
 *
 * <p>RequestPredicates may be used for security and rewrite actions.
 */
@Configurable
public class IfCron implements RequestPredicate
{
  private static final L10N L = new L10N(IfCron.class);
  
  private CronType _enableAt;
  private CronType _disableAt;

  /**
   * Sets the cron enable times.
   */
  @Configurable
  public void setEnableAt(CronType enableAt)
  {
    _enableAt = enableAt;
  }

  /**
   * Sets the cron disable times.
   */
  @Configurable
  public void setDisableAt(CronType disableAt)
  {
    _disableAt = disableAt;
  }

  @PostConstruct
  public void init()
  {
    if (_enableAt == null)
      throw new ConfigException(L.l("{0} requires 'enable-at' attribute",
                                    getClass().getSimpleName()));
    
    if (_disableAt == null)
      throw new ConfigException(L.l("{0} requires 'disable-at' attribute",
                                    getClass().getSimpleName()));
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    long now = Alarm.getCurrentTime();

    long prevEnable = _enableAt.prevTime(now);
    long prevDisable = _disableAt.prevTime(now);
    
    return prevEnable > 0 && prevDisable <= prevEnable;
  }
}
