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

package com.caucho.config;

import com.caucho.util.*;

import java.lang.reflect.*;

/**
 * Thrown by the various Builders
 */
public class LineConfigException extends ConfigException
  implements LineCompileException, LineException
{
  private String _filename;
  private int _line = -1;

  /**
   * Create a null exception
   */
  public LineConfigException()
  {
  }

  /**
   * Creates an exception with a message
   */
  public LineConfigException(String msg)
  {
    super(msg);
  }

  /**
   * Creates an exception with a message
   */
  public LineConfigException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

  /**
   * Creates an exception with a message
   */
  /*
  public LineConfigException(Throwable cause)
  {
    super(cause);
  }
  */

  public LineConfigException(String filename, int line, String message)
  {
    super(filename + ":" + line + ": " + message);

    _filename = filename;
    _line = line;
  }

  public LineConfigException(String filename, int line, Throwable cause)
  {
    super(filename + ":" + line + ": " + cause.getMessage(), cause);

    _filename = filename;
    _line = line;
  }

  public LineConfigException(String filename, int line,
                             String message, Throwable cause)
  {
    super(filename + ":" + line + ": " + message, cause);

    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLineNumber()
  {
    return _line;
  }

  public String toString()
  {
    return getMessage();
  }

  public static RuntimeException create(String filename, int line, Throwable e)
  {
    String loc = filename + ": " + line + ": ";
    
    if (e instanceof LineException) {
      if (e instanceof RuntimeException)
        throw (RuntimeException) e;
      else
        return new LineConfigException(filename, line, e.getMessage(), e);
    }
    else if (e instanceof DisplayableException)
      return new LineConfigException(filename, line, e.getMessage(), e);
    else
      return new LineConfigException(filename, line, e.toString(), e);
  }

  public static RuntimeException create(Field field, Throwable e)
  {
    return create(loc(field), e);
  }

  public static RuntimeException create(Method method, Throwable e)
  {
    return create(loc(method), e);
  }

  public static RuntimeException create(String loc, Throwable e)
  {
    if (e instanceof LineException) {
      if (e instanceof RuntimeException)
        return (RuntimeException) e;
      else
        return new LineConfigException(e.getMessage(), e);
    }
    else if (e instanceof DisplayableException)
      return new LineConfigException(loc + e.getMessage(), e);
    else
      return new LineConfigException(loc + e, e);
  }

  public static RuntimeException create(Throwable e)
  {
    if (e instanceof LineCompileException)
      return new LineConfigException(e.getMessage(), e);
    else if (e instanceof DisplayableException)
      return new ConfigException(e.getMessage(), e);
    else
      return new ConfigException(e.toString(), e);
  }

  public static String loc(Field field)
  {
    return field.getDeclaringClass().getName() + "." + field.getName() + ": ";
  }

  public static String loc(Method method)
  {
    return method.getDeclaringClass().getName() + "." + method.getName() + "(): ";
  }
}
