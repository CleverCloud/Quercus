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

import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public class BigDecimalValueExpression extends AbstractValueExpression
{
  protected static final Logger log
    = Logger.getLogger(BigDecimalValueExpression.class.getName());
  protected static final L10N L = new L10N(BigDecimalValueExpression.class);

  private Class _expectedType;

  public BigDecimalValueExpression(Expr expr,
                                   String expressionString,
                                   Class expectedType)
  {
    super(expr, expressionString);

    _expectedType = expectedType;
  }

  public BigDecimalValueExpression(Expr expr,
                                String expressionString)
  {
    super(expr, expressionString);
  }

  public BigDecimalValueExpression(Expr expr)
  {
    super(expr);
  }

  public BigDecimalValueExpression()
  {
  }

  public Class<?> getExpectedType()
  {
    if (_expectedType != null)
      return _expectedType;
    else
      return BigDecimal.class;
  }

  @Override
  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    return _expr.evalBigDecimal(context);
  }
}
