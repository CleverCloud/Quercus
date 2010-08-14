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

package com.caucho.jms.services;

import com.caucho.jms.util.BytesMessageOutputStream;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

import javax.jms.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class JMSServlet extends HttpServlet {
  private static final Logger log =
    Logger.getLogger(JMSServlet.class.getName());

  private Connection _jmsConnection;
  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private MessageProducer _producer;
  private Session _jmsSession;

  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  public void init()
  {
    try {
      _jmsConnection = _connectionFactory.createConnection();
      _jmsSession = 
        _jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      _producer = _jmsSession.createProducer(_destination);
    } catch (Exception e) {
      log.fine(e.toString());
    }
  }

  public void service(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException
  {
    InputStream is = request.getInputStream();

    try {
      BytesMessage message = _jmsSession.createBytesMessage();

      BytesMessageOutputStream out = new BytesMessageOutputStream(message);
      WriteStream ws = VfsStream.openWrite(out);

      ws.writeStream(is);

      ws.flush();
      out.flush();

      _producer.send(message);
    } catch (JMSException e) {
      throw new ServletException(e);
    }
  }
}
