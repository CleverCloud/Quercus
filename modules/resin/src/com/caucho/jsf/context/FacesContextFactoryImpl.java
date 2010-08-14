/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.lifecycle.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class FacesContextFactoryImpl extends FacesContextFactory
{
  private ApplicationFactory _applicationFactory;
  
  public FacesContext getFacesContext(Object context,
                                      Object request,
                                      Object response,
                                      Lifecycle lifecycle)
    throws FacesException
  {
    if (context == null
        || request == null
        || response == null
        || lifecycle == null)
      throw new NullPointerException();

    FacesContext facesContext
      = new ServletFacesContextImpl(this,
                                       (ServletContext) context,
                                       (HttpServletRequest) request,
                                       (HttpServletResponse) response);

     return facesContext;
  }

  Application getApplication()
  {
    synchronized (this) {
      if (_applicationFactory == null) {
        _applicationFactory = (ApplicationFactory)
          FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
      }

      return _applicationFactory.getApplication();
    }
  }

  public String toString()
  {
    return "FacesContextFactoryImpl[]";
  }
}
