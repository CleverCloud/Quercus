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

package com.caucho.jms.jca;

import com.caucho.services.message.MessageSender;
import com.caucho.services.message.MessageServiceException;
import com.caucho.util.L10N;

import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.resource.spi.ConnectionManager;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * The managed factory implementation.
 */
class MessageSenderImpl implements MessageSender {
  protected static final Logger log
    = Logger.getLogger(MessageSenderImpl.class.getName());
  private static final L10N L = new L10N(MessageSenderImpl.class);

  private MessageSenderManager _manager;
  private ConnectionManager _cm;

  MessageSenderImpl(MessageSenderManager manager, ConnectionManager cm)
  {
    _manager = manager;
    _cm = cm;
  }

  public void send(HashMap header, Object value)
    throws MessageServiceException
  {
    ManagedSessionImpl session = null;
    
    try {
      session = (ManagedSessionImpl) _cm.allocateConnection(_manager, null);

      Message message;
      
      if (value == null) {
        message = session.getSession().createMessage();
      }
      else if (value instanceof String) {
        message = session.getSession().createTextMessage((String) value);
      }
      else if (value instanceof java.io.Serializable) {
        ObjectMessage objMessage = session.getSession().createObjectMessage();
        objMessage.setObject((java.io.Serializable) value);
        message = objMessage;
      }
      else {
        throw new MessageServiceException(L.l("value '{0}' must be serializable",
                                              value));
      }

      session.send(message);
    } catch (MessageServiceException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new MessageServiceException(e);
    } finally {
      if (session != null)
        session.close();
    }
  }
}

