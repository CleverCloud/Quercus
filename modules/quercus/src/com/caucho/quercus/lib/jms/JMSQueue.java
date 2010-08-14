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

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.vfs.TempBuffer;
import com.caucho.util.L10N;

import javax.jms.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JMS functions
 */
public class JMSQueue
{
  private static final L10N L = new L10N(JMSQueue.class);
  private static final Logger log = Logger.getLogger(JMSQueue.class.getName());

  private Connection _connection;
  private Session _session;
  private MessageConsumer _consumer;
  private MessageProducer _producer;
  private Destination _destination;

  /**
   * Connects to a named queue.
   */
  public JMSQueue(ConnectionFactory connectionFactory,
                  Destination queue)
    throws Exception
  {
    _connection = connectionFactory.createConnection();

    _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    if (queue == null)
      _destination = _session.createTemporaryQueue();
    else
      _destination = queue;

    _consumer = _session.createConsumer(_destination);
    _producer = _session.createProducer(_destination);

    _connection.start();
  }

  public static Value __construct(Env env, @Optional String queueName)
  {
    JMSQueue queue = JMSModule.message_get_queue(env, queueName, null);

    if (queue == null) {
      env.warning(L.l("'{0}' is an unknown JMSQueue", queueName));
      return NullValue.NULL;
    }

    return env.wrapJava(queue);
  }

  public boolean send(@NotNull Value value, @Optional JMSQueue replyTo)
    throws JMSException
  {
    Message message = null;

    if (value.isArray()) {
      message = _session.createMapMessage();

      ArrayValue array = (ArrayValue) value;

      Set<Map.Entry<Value,Value>> entrySet = array.entrySet();

      for (Map.Entry<Value,Value> entry : entrySet) {
        if (entry.getValue() instanceof BinaryValue) {
          byte []bytes = ((BinaryValue) entry.getValue()).toBytes();

          ((MapMessage) message).setBytes(entry.getKey().toString(), bytes);
        } else {
          // every primitive except for bytes can be translated from a string
          ((MapMessage) message).setString(entry.getKey().toString(),
                                           entry.getValue().toString());
        }
      }
    } else if (value instanceof BinaryValue) {
      message = _session.createBytesMessage();


      byte []bytes = ((BinaryValue) value).toBytes();

      ((BytesMessage) message).writeBytes(bytes);
    } else if (value.isLongConvertible()) {
      message = _session.createStreamMessage();

      ((StreamMessage) message).writeLong(value.toLong());
    } else if (value.isDoubleConvertible()) {
      message = _session.createStreamMessage();

      ((StreamMessage) message).writeDouble(value.toDouble());
    } else if (value.toJavaObject() instanceof String) {
      message = _session.createTextMessage();

      ((TextMessage) message).setText(value.toString());
    } else if (value.toJavaObject() instanceof Serializable) {
      message = _session.createObjectMessage();

      ((ObjectMessage) message).setObject((Serializable) value.toJavaObject());
    } else {
      return false;
    }

    if (replyTo != null)
      message.setJMSReplyTo(replyTo._destination);

    _producer.send(message);

    return true;
  }

  public Value receive(Env env, @Optional("1") long timeout)
    throws JMSException
  {
    Message message = _consumer.receive(timeout);

    if (message == null)
      return BooleanValue.FALSE;

    if (message instanceof ObjectMessage) {
      Object object = ((ObjectMessage) message).getObject();

      return env.wrapJava(object);
    } else if (message instanceof TextMessage) {
      return env.createString(((TextMessage) message).getText());
    } else if (message instanceof StreamMessage) {
      Object object = ((StreamMessage) message).readObject();

      return env.wrapJava(object);
    } else if (message instanceof BytesMessage) {
      BytesMessage bytesMessage = (BytesMessage) message;
      int length = (int) bytesMessage.getBodyLength();

      StringValue bb = env.createBinaryBuilder(length);

      TempBuffer tempBuffer = TempBuffer.allocate();
      int sublen;

      while (true) {
        sublen = bytesMessage.readBytes(tempBuffer.getBuffer());

        if (sublen > 0)
          bb.append(tempBuffer.getBuffer(), 0, sublen);
        else
          break;
      }

      TempBuffer.free(tempBuffer);

      return bb;
    } else if (message instanceof MapMessage) {
      MapMessage mapMessage = (MapMessage) message;

      Enumeration mapNames = mapMessage.getMapNames();

      ArrayValue array = new ArrayValueImpl();

      while (mapNames.hasMoreElements()) {
        String name = mapNames.nextElement().toString();

        Object object = mapMessage.getObject(name);

        array.put(env.createString(name), env.wrapJava(object));
      }

      return array;
    } else {
      return BooleanValue.FALSE;
    }
  }

  protected void finalize()
  {
    try {
      _connection.close();
    } catch (JMSException e) {
      // intentionally left empty
    }
  }
}

