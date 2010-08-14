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

import java.io.*;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import com.caucho.vfs.*;
import com.caucho.hessian.io.*;

/**
 * A text message.
 */
public class TextMessageImpl extends MessageImpl implements TextMessage {
  private String _text;

  public TextMessageImpl()
  {
  }

  public TextMessageImpl(String text)
  {
    _text = text;
  }

  public TextMessageImpl(TextMessage msg)
    throws JMSException
  {
    super(msg);

    _text = msg.getText();
  }

  public TextMessageImpl(TextMessageImpl msg)
  {
    super(msg);

    _text = msg._text;
  }

  /**
   * Returns the type enumeration.
   */
  @Override
  public MessageType getType()
  {
    return MessageType.TEXT;
  }

  /**
   * Returns the message text.
   */
  public String getText()
    throws JMSException
  {
    return _text;
  }

  /**
   * Returns the message text.
   */
  public void setText(String text)
    throws JMSException
  {
    checkBodyWriteable();
    
    _text = text;
  }

  /**
   * Clears the body.
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _text = null;
  }

  public MessageImpl copy()
  {
    return new TextMessageImpl(this);
  }

  protected void copy(TextMessageImpl newMsg)
  {
    super.copy(newMsg);

    newMsg._text = _text;
  }

  /**
   * Serialize the body to an input stream.
   */
  @Override
  public InputStream bodyToInputStream()
    throws IOException
  {
    if (_text == null)
      return null;
    
    TempOutputStream os = new TempOutputStream();

    writeBody(os);

    return os.openRead();
  }

  /**
   * Serialize the body to an output stream.
   */
  @Override
  public void writeBody(OutputStream os)
    throws IOException
  {
    if (_text == null)
      return;

    Hessian2Output out = new Hessian2Output(os);

    out.writeString(_text);

    out.close();
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

    Hessian2Input in = new Hessian2Input(is);

    _text = in.readString();

    in.close();
  }
}

