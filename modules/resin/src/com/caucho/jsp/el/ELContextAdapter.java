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

package com.caucho.jsp.el;

import com.caucho.el.AbstractVariableResolver;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.servlet.jsp.el.VariableResolver;

/**
 * Implementation of the expression evaluator.
 */
public class ELContextAdapter extends ELContext {
  private VariableResolver _resolver;
  private ELResolver _elResolver = new ELResolverAdapter();

  /**
   * Implements the expression.
   */
  ELContextAdapter(VariableResolver resolver)
  {
    _resolver = resolver;
  }
  
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  public javax.el.FunctionMapper getFunctionMapper()
  {
    return null;
  }

  public javax.el.VariableMapper getVariableMapper()
  {
    return null;
  }

  class ELResolverAdapter extends AbstractVariableResolver
  {
    @Override
    public Object getValue(ELContext context, Object base, Object property)
    {
      if (base == null && property instanceof String) {
        if (context != null)
          context.setPropertyResolved(true);

        try {
          return _resolver.resolveVariable((String) property);
        } catch (Exception e) {
          throw new ELException(e);
        }
      }
      else
        return null;
    }
    
  }
}
