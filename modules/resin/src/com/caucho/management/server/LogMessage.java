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

package com.caucho.management.server;

/**
 * Persistent logging.
 */
public class LogMessage implements java.io.Serializable
{
  private String _name;
  
  private String _server;
  private String _thread;
  
  private long _timestamp;
  
  private String _level;
  private String _message;

  public LogMessage()
  {
  }

  /**
   * The java.util.logging name of the log message
   */
  public String getName()
  {
    return _name;
  }

  /**
   * The java.util.logging name of the log message
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * The Resin server id sending the log message
   */
  public String getServer()
  {
    return _server;
  }

  /**
   * The Resin server id sending the log message
   */
  public void setServer(String server)
  {
    _server = server;
  }

  /**
   * The thread name sending the log message
   */
  public String getThread()
  {
    return _thread;
  }

  /**
   * The thread name sending the log message
   */
  public void setThread(String thread)
  {
    _thread = thread;
  }

  /**
   * The timestamp for the log message
   */
  public void setTimestamp(long timestamp)
  {
    _timestamp = timestamp;
  }

  /**
   * The timestamp for the log message
   */
  public long getTimestamp()
  {
    return _timestamp;
  }

  /**
   * The java.util.logging level name for the log message
   */
  public void setLevel(String level)
  {
    _level = level;
  }

  /**
   * The java.util.logging level name for the log message
   */
  public String getLevel()
  {
    return _level;
  }

  /**
   * The message text
   */
  public void setMessage(String message)
  {
    _message = message;
  }

  /**
   * The message text
   */
  public String getMessage()
  {
    return _message;
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _name + ", " + _level
            + ", " + _thread + ", " + _message + "]");
  }
}
