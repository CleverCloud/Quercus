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

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * Represents a double literal expression.
 */
public class DoubleLiteral extends Expr {
  private double _value;
  private Double _objValue;

  /**
   * Create a new double literal.
   */
  public DoubleLiteral(double value)
  {
    _value = value;
    _objValue = new Double(value);
  }

  /**
   * Returns true if the expression is constant.
   */
  public boolean isConstant()
  {
    return true;
  }

  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the Double constant
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    return _objValue;
  }

  /**
   * Evaluate the expression as an object as a long.
   *
   * @param env the variable environment
   *
   * @return the double value casted to a long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    return (long) _value;
  }

  /**
   * Evaluate the expression as an object as a double
   *
   * @param env the variable environment
   *
   * @return the double value
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    return _value;
  }

  /**
   * Evaluates directly to the output.  The method returns true
   * if the default value should be printed instead.
   *
   * @param out the output writer
   * @param env the variable environment
   * @param escapeXml if true, escape reserved XML
   *
   * @return true if the object is null, otherwise false
   */
  @Override
  public boolean print(JspWriter out,
                       ELContext env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    out.print(_value);

    return false;
  }

  /**
   * Prints the *.java code to create an DoubleLiteral.
   *
   * @param os the stream to the generated *.java
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.DoubleLiteral(");
    os.print(_value);
    os.print(")");
  }

  /**
   * Returns true for equal longs.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof DoubleLiteral))
      return false;

    DoubleLiteral literal = (DoubleLiteral) o;

    return _value == literal._value;
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return String.valueOf(_value);
  }
}
