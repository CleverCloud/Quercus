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

package com.caucho.servlet;

import java.io.IOException;

/**
 * The WebSocket bidirectional message listener receives events on each
 * new message in the WebSocket stream.
 *
 * To read a message, read the input stream until the end of the message,
 * when it returns an end of file.
 * 
 * To write a message, write the output stream and close it. Remember, the
 * output will be locked until the output stream completes.
 */
public interface JanusListener
{
  /**
   * Called when the connection is established, allowing for any initial
   * messages.
   * 
   * @param context the bidirectional message context for reading new messages. 
   */
  public void onStart(JanusContext context)
    throws IOException;

  /**
   * Called when a new message is available is available.
   */
  public void onMessage(JanusContext context)
    throws IOException;

  /**
   * Called when the connection closes
   */
  public void onComplete(JanusContext context)
    throws IOException;

  /**
   * Called when the connection times out
   */
  public void onTimeout(JanusContext context)
    throws IOException;
}
