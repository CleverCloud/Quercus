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

package com.caucho.quercus.env;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a PHP default value.
 */
@SuppressWarnings("serial")
public class DefaultValue extends NullValue {
  public static final DefaultValue DEFAULT
    = new DefaultValue();

  private DefaultValue()
  {
  }

  /**
   * Returns the null value singleton.
   */
  public static DefaultValue create()
  {
    return DEFAULT;
  }
  
  /**
   * Returns true for a DefaultValue
   */
  @Override
  public boolean isDefault()
  {
    return true;
  }
  
  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return false;
  }
  
  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return 0;
  }
  
  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return 0;
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return "";
  }
  
  /**
   * Converts to a callable
   */
  @Override
  public Callable toCallable(Env env)
  {
    return null;
  }
  
  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public String toString()
  {
    return "";
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  @Override
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("DefaultValue.DEFAULT");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PrintWriter out)
    throws IOException
  {
    out.print("0");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PrintWriter out)
    throws IOException
  {
    out.print("\"\"");
  }
  
  //
  // Java Serialization
  //
  
  private Object readResolve()
  {
    return DEFAULT;
  }
}

