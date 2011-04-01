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
import javax.el.MethodInfo;
import java.io.IOException;

/**
 * Represents a string literal expression
 */
public class StringLiteral extends Expr {
  private String _value;

  public StringLiteral(String value)
  {
    _value = value;
  }

  /**
   * Returns true if the expression is constant.
   */
  @Override
  public boolean isConstant()
  {
    return true;
  }

  /**
   * Returns true if the expression is literal text
   */
  @Override
  public boolean isLiteralText()
  {
    return true;
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public MethodInfo getMethodInfo(ELContext env,
                                  Class<?> returnType,
                                  Class<?> []argTypes)
    throws ELException
  {
    return new MethodInfo(_value, returnType, argTypes);
  }

  /**
   * Evaluates the expression, returning an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public Object invoke(ELContext env, Class<?> []argTypes, Object []args)
    throws ELException
  {
    return _value;
  }
  
  /**
   * Returns the value of the literal.
   */
  public String getValue()
  {
    return _value;
  }
  
  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    return _value;
  }
  
  /**
   * Evaluate the expr as a string
   *
   * @param env the variable environment
   */
  @Override
  public String evalString(ELContext env)
    throws ELException
  {
    return _value;
  }

  /**
   * Evalutes directly to the output.
   */
  @Override
  public boolean print(WriteStream out,
                       ELContext env,
                       boolean isEscape)
    throws IOException, ELException
  {
    if (isEscape)
      toStreamEscaped(out, _value);
    else
      out.print(_value);
    
    return false;
  }

  /**
   * Prints the code to create an LongLiteral.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.StringLiteral(\"");
    printEscapedString(os, _value);
    os.print("\")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof StringLiteral))
      return false;

    StringLiteral literal = (StringLiteral) o;

    return _value.equals(literal._value);
  }

  /**
   * Returns a printable version.
   */
  public String toString()
  {
    return "\"" + _value + "\"";
  }
}
