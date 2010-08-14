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

package com.caucho.server.security;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class ContainerConstraint extends AbstractConstraint {
  private boolean _needsAuthentication;
  
  private ArrayList<AbstractConstraint> _constraints =
    new ArrayList<AbstractConstraint>();

  /**
   * Adds a constraint.
   */
  public void addConstraint(AbstractConstraint constraint)
  {
    for (AbstractConstraint subConstraint : constraint.toArray()) {
      _constraints.add(subConstraint);

      if (subConstraint.needsAuthentication())
        _needsAuthentication = true;
    }
  }
  
  /**
   * Returns true if the constraint requires authentication.
   */
  public boolean needsAuthentication()
  {
    return _needsAuthentication;
  }
  
  /**
   * Returns true if the user is authorized for the resource.
   *
   * <p>isAuthorized must provide the response if the user is not
   * authorized.  Typically this will just call sendError.
   *
   * <p>isAuthorized will be called after all the other filters, but
   * before the servlet.service().
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @return true if the request is authorized.
   */
  public AuthorizationResult isAuthorized(HttpServletRequest request,
                                          HttpServletResponse response,
                                          ServletContext application)
    throws ServletException, IOException
  {
    AuthorizationResult result = AuthorizationResult.NONE;
    
    for (int i = 0; i < _constraints.size(); i++) {
      AbstractConstraint constraint = _constraints.get(i);

      result = constraint.isAuthorized(request, response, application);

      if (! result.isFallthrough())
        return result;
    }

    return result;
  }

  /**
   * converts the sub constraints to an array.
   */
  protected AbstractConstraint []toArray()
  {
    return _constraints.toArray(new AbstractConstraint[_constraints.size()]);
  }
}
