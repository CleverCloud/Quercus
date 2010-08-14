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

package com.caucho.jsp.el;

import com.caucho.el.Expr;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.servlet.jsp.el.Expression;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.FunctionMapper;
import java.lang.reflect.Method;

/**
 * Implementation of the expression evaluator.
 */
public class ExpressionEvaluatorImpl extends ExpressionEvaluator {
  private ELContext _elContext;
  
  /**
   * Creates the expression evaluator.
   */
  public ExpressionEvaluatorImpl(ELContext elContext)
  {
    _elContext = elContext;
  }

  /**
   * Evaluates an expression.
   */
  public Object evaluate(String expression, Class expectedType,
                         javax.servlet.jsp.el.VariableResolver resolver,
                         FunctionMapper funMapper)
    throws javax.servlet.jsp.el.ELException
  {
    Expression expr = parseExpression(expression, expectedType, funMapper);
      
    return expr.evaluate(resolver);
  }

  /**
   * Parses an expression.
   */
  public Expression parseExpression(String expression,
                                    Class expectedType,
                                    FunctionMapper funMapper)
    throws javax.servlet.jsp.el.ELException
  {
    ELContext elContext;

    if (funMapper != null)
      elContext = new ParseELContext(funMapper);
    else
      elContext = _elContext;
    
    JspELParser parser = new JspELParser(elContext, expression);

    // parser.setFunctionMapper(funMapper);

    try {
      Expr expr = parser.parse();

      return new ExpressionImpl(expr);
    } catch (com.caucho.el.ELParseException e) {
      throw new javax.servlet.jsp.el.ELParseException(e.getMessage());
    }
  }

  public class ParseELContext extends ELContext
  {
    private javax.el.FunctionMapper _funMapper;

    ParseELContext(FunctionMapper funMapper)
    {
      _funMapper = new FunctionMapperAdapter(funMapper);
    }
    
    public ELResolver getELResolver()
    {
      return _elContext.getELResolver();
    }

    public javax.el.FunctionMapper getFunctionMapper()
    {
      return _funMapper;
    }

    public javax.el.VariableMapper getVariableMapper()
    {
      return _elContext.getVariableMapper();
    }
  }

  public class FunctionMapperAdapter extends javax.el.FunctionMapper
  {
    private FunctionMapper _funMap;

    FunctionMapperAdapter(FunctionMapper funMap)
    {
      _funMap = funMap;
    }
    
    public Method resolveFunction(String prefix, String localName)
    {
      return _funMap.resolveFunction(prefix, localName);
    }
  }
}
