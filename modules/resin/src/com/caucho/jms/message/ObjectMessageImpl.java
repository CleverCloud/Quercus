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

package com.caucho.jms.message;

import com.caucho.jms.JmsExceptionWrapper;
import com.caucho.vfs.*;
import com.caucho.hessian.io.*;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.*;

/**
 * An object message.
 */
public class ObjectMessageImpl extends MessageImpl implements ObjectMessage
{
  private TempStream _tempStream;

  public ObjectMessageImpl()
  {
  }

  public ObjectMessageImpl(ObjectMessage msg)
    throws JMSException
  {
    super(msg);

    setObject(msg.getObject());
  }

  public ObjectMessageImpl(ObjectMessageImpl msg)
  {
    super(msg);

    _tempStream = msg._tempStream;
  }

  public ObjectMessageImpl(Serializable value)
    throws JMSException
  {
    setObject(value);
  }

  /**
   * Returns the type enumeration.
   */
  @Override
  public MessageType getType()
  {
    return MessageType.OBJECT;
  }
  
  /**
   * Writes the object to the stream.
   */
  public void setObject(Serializable o)
    throws JMSException
  {
    checkBodyWriteable();
    
    _tempStream = new TempStream();
    
    try {
      OutputStream ws = new StreamImplOutputStream(_tempStream);
      Hessian2Output out = new Hessian2Output(ws);
      out.writeObject(o);
      out.close();
      ws.close();
    } catch (Exception e) {
      throw JmsExceptionWrapper.create(e);
    }
  }

  /**
   * Reads the object from the stream.
   */
  public Serializable getObject()
    throws JMSException
  {
    if (_tempStream == null)
      return null;
    
    try {
      ReadStream is = _tempStream.openReadAndSaveBuffer();
      Hessian2Input in = new Hessian2Input(is);
      Serializable object = (Serializable) in.readObject();
      in.close();
      is.close();

      return object;
    } catch (Exception e) {
      throw JmsExceptionWrapper.create(e);
    }
  }
  
  /**
   * Clears the body
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _tempStream = null;
  }

  public MessageImpl copy()
  {
    return new ObjectMessageImpl(this);
  }

  /**
   * Serialize the body to an input stream.
   */
  @Override
  public InputStream bodyToInputStream()
    throws IOException
  {
    if (_tempStream != null)
      return _tempStream.openReadAndSaveBuffer();
    else
      return null;
  }

  /**
   * Read the body from an input stream.
   */
  @Override
  public void readBody(InputStream is)
    throws IOException, JMSException
  {
    if (is == null)
      return;

    _tempStream = new TempStream();
    _tempStream.openWrite();

    WriteStream ws = new WriteStream(_tempStream);
    ws.writeStream(is);
    ws.close();
  }
}

