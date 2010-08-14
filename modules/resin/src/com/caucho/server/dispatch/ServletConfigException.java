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

package com.caucho.server.dispatch;

import com.caucho.util.*;

import java.io.*;
import javax.servlet.ServletException;

/**
 * Represents the final servlet in a filter chain.
 */
public class ServletConfigException extends ServletException
  implements CompileException, DisplayableException
{
  /**
   * Creates a servlet config exception.
   */
  public ServletConfigException()
  {
    super();
  }
  
  /**
   * Creates a servlet config exception.
   */
  public ServletConfigException(String msg)
  {
    super(msg);
  }
  
  /**
   * Creates a servlet config exception.
   */
  public ServletConfigException(String msg, Throwable e)
  {
    super(msg, e);
  }
  
  /**
   * Creates a servlet config exception.
   */
  public ServletConfigException(Throwable e)
  {
    super(e);
  }

  public void print(PrintWriter out)
  {
    if (getCause() instanceof DisplayableException)
      ((DisplayableException) getCause()).print(out);
    else
      out.println(Html.escapeHtml(getMessage()));
  }
}
