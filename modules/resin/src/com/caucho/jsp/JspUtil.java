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

package com.caucho.jsp;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.el.CandiExpr;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.el.Expr;
import com.caucho.el.MethodExpressionImpl;
import com.caucho.inject.Module;
import com.caucho.jsp.el.JspApplicationContextImpl;
import com.caucho.jsp.el.JspELParser;

@Module
public class JspUtil
{
  public static ValueExpression createValueExpression(ELContext elContext,
                                                      Class<?> type,
                                                      String exprString)
  {
    JspApplicationContextImpl jspContext
      = JspApplicationContextImpl.getCurrent();
    
    ExpressionFactory factory = jspContext.getExpressionFactory();

    return factory.createValueExpression(elContext,
                                         exprString,
                                         type);
  }

  public static Expr createExpr(ELContext elContext,
                                String exprString)
  {
    JspELParser parser = new JspELParser(elContext, exprString);

    return new CandiExpr(parser.parse());
  }
  
  public static <T> ReferenceFactory<T> getInjectFactory(Class<T> cl)
  {
    InjectManager cdiManager = InjectManager.create();
    
    Bean<T> bean = cdiManager.createManagedBean(cl);
    
    return cdiManager.getReferenceFactory(bean);
  }

  public static MethodExpression createMethodExpression(ELContext elContext,
                                                        String exprString,
                                                        Class<?> type,
                                                        Class<?> []args)
  {
    JspELParser parser = new JspELParser(elContext, exprString);

    Expr expr = parser.parse();

    return new MethodExpressionImpl(expr, exprString, type, args);
  }
}
