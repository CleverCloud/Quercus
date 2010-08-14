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

package com.caucho.config.el;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.MethodInfo;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.servlet.jsp.JspWriter;

import com.caucho.el.Expr;
import com.caucho.vfs.WriteStream;

/**
 * Variable resolution for webbeans variables
 */
@SuppressWarnings("serial")
public class CandiExpr extends Expr {
  private final Expr _expr;
  
  public CandiExpr(Expr expr)
  {
    _expr = expr;
  }

  @Override
  public boolean isConstant()
  {
    return _expr.isConstant();
  }

  @Override
  public boolean isReadOnly(ELContext env)
  {
    return _expr.isReadOnly(env);
  }

  @Override
  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  @Override
  public Expr createField(Expr field)
  {
    return new CandiExpr(_expr.createField(field));
  }

  @Override
  public Expr createField(String field)
  {
    return new CandiExpr(_expr.createField(field));
  }

  @Override
  public Expr createMethod(Expr []args)
  {
    return new CandiExpr(_expr.createMethod(args));
  }

  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    CandiConfigResolver.startContext();
    
    try {
      return _expr.getValue(env);
    } finally {
      CandiConfigResolver.finishContext();
    }
  }

  @Override
  public MethodInfo getMethodInfo(ELContext env,
                                  Class<?> returnType,
                                  Class<?> []argTypes)
    throws ELException
  {
    return _expr.getMethodInfo(env, returnType, argTypes);
  }

  @Override
  public Object invoke(ELContext env, Class<?> []argTypes, Object []args)
    throws ELException
  {
    return _expr.invoke(env, argTypes, args);
  }

  @Override
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    return _expr.evalBoolean(env);
  }

  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    return _expr.evalDouble(env);
  }

  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    return _expr.evalLong(env);
  }

  @Override
  public String evalString(ELContext env)
    throws ELException
  {
    return _expr.evalString(env);
  }

  @Override
  public String evalStringWithNull(ELContext env)
    throws ELException
  {
    return _expr.evalStringWithNull(env);
  }

  @Override
  public char evalCharacter(ELContext env)
    throws ELException
  {
    return _expr.evalCharacter(env);
  }

  @Override
  public long evalPeriod(ELContext env)
    throws ELException
  {
    return _expr.evalPeriod(env);
  }

  @Override
  public BigInteger evalBigInteger(ELContext env)
    throws ELException
  {
    return _expr.evalBigInteger(env);
  }

  @Override
  public BigDecimal evalBigDecimal(ELContext env)
    throws ELException
  {
    return _expr.evalBigDecimal(env);
  }

  @Override
  public void setValue(ELContext env, Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
    _expr.setValue(env, value);
  }

  @Override
  public boolean print(WriteStream out,
                       ELContext env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    CandiConfigResolver.startContext();
    
    try {
      return _expr.print(out, env, escapeXml);
    } finally {
      CandiConfigResolver.finishContext();
    }
  }

  @Override
  public boolean print(JspWriter out,
                       ELContext env,
                       boolean escapeXml)
    throws IOException, ELException
  {
    CandiConfigResolver.startContext();
    
    try {
      return _expr.print(out, env, escapeXml);
    } finally {
      CandiConfigResolver.finishContext();
    }
  }

  public void printCreate(WriteStream os)
    throws IOException
  {
    _expr.printCreate(os);
  }

  //
  // EL methods
  //

  @Override
  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }

  @Override
  public Class<?> getExpectedType()
  {
    return _expr.getExpectedType();
  }

  @Override
  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    return _expr.getType(context);
  }

  @Override
  public String toString()
  {
    return _expr.toString();
  }
}
