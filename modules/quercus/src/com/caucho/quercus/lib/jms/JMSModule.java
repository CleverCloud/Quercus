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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.jms;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

/**
 * JMS functions
 */
@ClassImplementation
public class JMSModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(JMSModule.class.getName());

  private static final L10N L = new L10N(JMSModule.class);

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  static JMSQueue message_get_queue(Env env, String queueName,
                                    ConnectionFactory connectionFactory)
  {
    if (connectionFactory == null)
      connectionFactory = getConnectionFactory(env);

    if (connectionFactory == null) {
      env.warning(L.l("No connection factory"));
      return null;
    }

    try {
      Destination queue = null;

      if (queueName != null && ! queueName.equals(""))
        queue = (Destination) new InitialContext().lookup(
            "java:comp/env/" + queueName);
      
      return new JMSQueue(connectionFactory, queue);
    } catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  private static ConnectionFactory getConnectionFactory(Env env)
  {
    StringValue factoryName = env.getIni("jms.connection_factory");

    if (factoryName == null)
      log.fine("jms.connection_factory not set");

    try {
      Context context = (Context) new InitialContext().lookup("java:comp/env");

      ConnectionFactory connectionFactory = 
        (ConnectionFactory) context.lookup(factoryName.toString());

      if (connectionFactory == null)
        log.warning("Couldn't find factory " + factoryName.toString());

      return connectionFactory;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  static final IniDefinition INI_JMS_CONNECTION_FACTORY
    = _iniDefinitions.add(
      "jms.connection_factory",  "jms/ConnectionFactory", PHP_INI_SYSTEM);
}

