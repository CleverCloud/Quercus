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

package com.caucho.jms.message;

import java.io.*;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.vfs.*;

/**
 * A message factory
 */
public class MessageFactory
{
  private static final Logger log
    = Logger.getLogger(MessageFactory.class.getName());

  private static final MessageType []MESSAGE_TYPE = MessageType.values();

  /**
   * Creates a new JMS text message.
   */
  public TextMessage createTextMessage()
  {
    return new TextMessageImpl();
  }

  /**
   * Creates a new JMS text message.
   *
   * @param msg initial message text
   */
  public TextMessage createTextMessage(String msg)
    throws JMSException
  {
    TextMessage textMessage = new TextMessageImpl();
    
    textMessage.setText(msg);

    return textMessage;
  }

  /**
   * Returns the message type.
   */
  public MessageType getMessageType(Message msg)
  {
    if (msg instanceof TextMessage)
      return MessageType.TEXT;
    else if (msg instanceof BytesMessage)
      return MessageType.BYTES;
    else if (msg instanceof MapMessage)
      return MessageType.MAP;
    else if (msg instanceof ObjectMessage)
      return MessageType.OBJECT;
    else if (msg instanceof StreamMessage)
      return MessageType.STREAM;
    else
      return MessageType.NULL;
  }

  /**
   * Creates a message based on the type.
   */
  public MessageImpl createMessage(int type)
  {
    if (type < 0 && MESSAGE_TYPE.length <= type)
      return null;

    switch (MESSAGE_TYPE[type]) {
    case NULL:
      return new MessageImpl();

    case BYTES:
      return new BytesMessageImpl();

    case MAP:
      return new MapMessageImpl();

    case OBJECT:
      return new ObjectMessageImpl();
      
    case STREAM:
      return new StreamMessageImpl();

    case TEXT:
      return new TextMessageImpl();

    default:
      return new MessageImpl();
    }
  }

  /**
   * Copy the message.
   */
  public MessageImpl copy(Message msg)
    throws JMSException
  {
    if (msg instanceof MessageImpl)
      return ((MessageImpl) msg).copy();
    else if (msg instanceof TextMessage) {
      return new TextMessageImpl((TextMessage) msg);
    }
    else if (msg instanceof MapMessage) {
      return new MapMessageImpl((MapMessage) msg);
    }
    else if (msg instanceof BytesMessage) {
      return new BytesMessageImpl((BytesMessage) msg);
    }
    else if (msg instanceof StreamMessage) {
      return new StreamMessageImpl((StreamMessage) msg);
    }
    else if (msg instanceof ObjectMessage) {
      return new ObjectMessageImpl((ObjectMessage) msg);
    }
    else if (msg != null)
      return new MessageImpl(msg);
    else
      throw new NullPointerException();
  }

  /**
   * Creates an input stream from the header.
   */
  public InputStream headerToInputStream(Message msg)
  {
    try {
      TempStream ts = new TempStream();
      ts.openWrite();
      WriteStream out = new WriteStream(ts);

      writeHeader(out, msg);

      out.close();

      return ts.openRead();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void writeHeader(WriteStream out, Message msg)
  {
  }
}

