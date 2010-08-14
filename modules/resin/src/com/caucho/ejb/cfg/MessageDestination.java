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
 * @author Sam
 */

package com.caucho.ejb.cfg;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.*;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.jms.Destination;
import javax.naming.NamingException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MessageDestination extends DescriptionGroupConfig {
  private final L10N L = new L10N(MessageDestination.class);
  private Logger log = Logger.getLogger(MessageDestination.class.getName());

  private String _messageDestinationName;
  private String _mappedName;

  private Destination _destination;

  public MessageDestination()
  {
  }

  public void setMessageDestinationName(String messageDestinationName)
  {
    _messageDestinationName = messageDestinationName;
  }

  public String getMessageDestinationName()
  {
    return _messageDestinationName;
  }

  public void setMappedName(String mappedName)
  {
    _mappedName = mappedName;
  }

  public Destination getResolvedDestination()
  {
    if (_destination == null)
      resolve();

    return _destination;
  }

  private void resolve()
  {
    Destination destination = null;

    InjectManager webBeans = InjectManager.create();

    String name = _mappedName;

    if (name == null)
      name = _messageDestinationName;

    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("resolving <message-destination> '{0}'", name));

    throw new UnsupportedOperationException(getClass().getName());
    /*
    destination = (Destination) webBeans.getObjectByName(name);

    if (destination == null) {
      throw new ConfigException(L.l("<message-destination> '{0}' could not be resolved",
                                    name));
    }

    _destination = destination;
    */
  }

  public String toString()
  {
    return getClass().getSimpleName() + " [" + _messageDestinationName + "]";
  }
}
