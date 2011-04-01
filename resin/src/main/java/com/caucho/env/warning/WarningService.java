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

package com.caucho.env.warning;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.util.L10N;

/**
 * The WarningService is a general way to send warning and critical
 * system messages such as shutdown messages.
 */
public class WarningService extends AbstractResinService
{
  private static final L10N L = new L10N(WarningService.class);
  
  private static final Logger log
    = Logger.getLogger(WarningService.class.getName());
  
  private final CopyOnWriteArrayList<WarningHandler> _handlerList
    = new CopyOnWriteArrayList<WarningHandler>();

  /**
   * Creates a new resin server.
   */
  public WarningService()
  {
  }
  
  /**
   * Returns the warning service.
   */
  public static WarningService getCurrent()
  {
    return ResinSystem.getCurrentService(WarningService.class);
  }
  
  /**
   * Returns the warning service.
   */
  public static WarningService create()
  {
    return create(ResinSystem.getCurrent());
  }

  /**
   * Returns the warning service.
   */
  public static WarningService create(ResinSystem resinSystem)
  {
    if (resinSystem == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          WarningService.class.getSimpleName(),
                                          ResinSystem.class.getSimpleName()));
    }

    WarningService service = resinSystem.getService(WarningService.class);

    if (service == null) {
      service = new WarningService();
      
      resinSystem.addServiceIfAbsent(service);
      
      service = resinSystem.getService(WarningService.class);
    }
    
    return service;
  }
  
  /**
   * Sends a warning
   */
  public void warning(String msg)
  {
    log.warning("WarningService: " + msg);
    System.err.println("WarningService: " + msg);
    
    for (WarningHandler handler : _handlerList) {
      handler.warning(msg);
    }
  }
  
  /**
   * Sends a warning to the current service.
   */
  public static void sendCurrentWarning(String msg)
  {
    WarningService warning = getCurrent();
    
    if (warning != null)
      warning.warning(msg);
  }
  
  public void addHandler(WarningHandler handler)
  {
    _handlerList.add(handler);
  }
}
