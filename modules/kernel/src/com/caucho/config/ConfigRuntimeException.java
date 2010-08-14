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

import com.caucho.util.CompileException;
import com.caucho.util.DisplayableException;
import com.caucho.util.Html;

import java.io.PrintWriter;

/**
 * Thrown by the various Builders
 */
public class ConfigRuntimeException
  extends RuntimeException
{
  /**
   * Create a null exception
   */
  public ConfigRuntimeException()
  {
  }

  /**
   * Creates an exception with a message
   */
  public ConfigRuntimeException(String msg)
  {
    super(msg);
  }

  /**
   * Creates an exception with a message and throwable
   */
  public ConfigRuntimeException(String msg, Throwable e)
  {
    super(msg, e);
  }

  /**
   * Creates an exception with a throwable
   */
  public ConfigRuntimeException(Throwable e)
  {
    super(getMessage(e), e);
  }

  private static String getMessage(Throwable e)
  {
    if (e instanceof DisplayableException || e instanceof CompileException)
      return e.getMessage();
    else
      return e.toString();
  }

  public void print(PrintWriter out)
  {
    out.println(Html.escapeHtml(getMessage()));
  }
}
