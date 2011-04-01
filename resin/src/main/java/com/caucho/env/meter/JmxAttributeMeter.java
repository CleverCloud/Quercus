/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.env.meter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;

public final class JmxAttributeMeter extends AbstractMeter {
    private static final Logger log
        = Logger.getLogger(JmxAttributeMeter.class.getName());

  private MBeanServer _server;
  private ObjectName _objectName;
  private String _attribute;

  public JmxAttributeMeter(String name, String objectName, String attribute)
  {
    super(name);

    try {
        _objectName = new ObjectName(objectName);
    } catch (Exception e) {
        throw ConfigException.create(e);
    }

    _attribute = attribute;
    _server = Jmx.getGlobalMBeanServer();
  }

  /**
   * Polls the statistics attribute.
   */
  public double sample()
  {
    try {
      Object value = _server.getAttribute(_objectName, _attribute);

      if (value == null)
        return 0;
      
      return ((Number) value).doubleValue();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return 0;
    }
  }
}
