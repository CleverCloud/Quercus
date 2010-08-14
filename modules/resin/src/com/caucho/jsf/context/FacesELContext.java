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

package com.caucho.jsf.context;

import javax.el.*;
import javax.faces.context.*;
import javax.servlet.*;
import javax.servlet.http.*;

import java.lang.reflect.Method;

import com.caucho.util.*;
import com.caucho.jsp.el.*;
import com.caucho.jsf.el.*;

public class FacesELContext extends ServletELContext
{
  private static final javax.el.FunctionMapper NULL_FUNCTION_MAPPER
    = new NullFunctionMapper();
  
  private FacesContext _facesContext;
  private ELResolver _elResolver;
  
  public FacesELContext(FacesContext facesContext, ELResolver elResolver)
  {
    _facesContext = facesContext;
    _elResolver = elResolver;
  }
    
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  public javax.el.FunctionMapper getFunctionMapper()
  {
    return NULL_FUNCTION_MAPPER;
  }

  public javax.el.VariableMapper getVariableMapper()
  {
    return JsfImplicitVariableMapper.MAPPER;
  }

  // ServletELContext

  @Override
  public Object getRequestScope()
  {
    return _facesContext.getExternalContext().getRequestMap();
  }

  @Override
  public Object getSessionScope()
  {
    return _facesContext.getExternalContext().getSessionMap();
  }

  @Override
  public Object getApplicationScope()
  {
    return _facesContext.getExternalContext().getApplicationMap();
  }

  @Override
  public ServletContext getApplication()
  {
    return (ServletContext) _facesContext.getExternalContext().getContext();
  }

  public HttpServletRequest getRequest()
  {
    return (HttpServletRequest) _facesContext.getExternalContext().getRequest();
  }

  static class NullFunctionMapper extends javax.el.FunctionMapper {
    @Override
    public Method resolveFunction(String prefix, String localName)
    {
      return null;
    }
  }
}
