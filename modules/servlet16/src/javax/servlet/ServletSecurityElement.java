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
 * @author Alex Rojkov
 */
package javax.servlet;

import java.util.*;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.HttpMethodConstraint;

/**
 * @since Servlet 3.0
 */
public class ServletSecurityElement extends HttpConstraintElement {

  private Collection<String> _methodNames = new HashSet<String>(0);
  private Collection<HttpMethodConstraintElement> _httpMethodConstraints
    = new HashSet<HttpMethodConstraintElement>(0);

  public ServletSecurityElement()
  {
  }

  public ServletSecurityElement(HttpConstraintElement constraint)
  {
    super(constraint.getEmptyRoleSemantic(),
          constraint.getTransportGuarantee(),
          constraint.getRolesAllowed());
  }

  public ServletSecurityElement(
    Collection<HttpMethodConstraintElement> methodConstraints)
  {
    this(new HttpConstraintElement(), methodConstraints);
  }

  public ServletSecurityElement(HttpConstraintElement constraint,
                                Collection<HttpMethodConstraintElement> methodConstraints)
  {
    this(constraint);

    _methodNames = new HashSet<String>(methodConstraints.size());

    for (HttpMethodConstraintElement methodConstraint : methodConstraints) {
      String httpMethod = methodConstraint.getMethodName();

      if (_methodNames.contains(httpMethod))
        throw new IllegalArgumentException("Http method "
          + httpMethod
          + " was already used.");
      else
        _methodNames.add(httpMethod);
    }

    _httpMethodConstraints.addAll(methodConstraints);
  }

  public ServletSecurityElement(ServletSecurity annotation)
  {
    super(annotation.value().value(),
          annotation.value().transportGuarantee(),
          annotation.value().rolesAllowed());

    HttpMethodConstraint []methodConstraints
      = annotation.httpMethodConstraints();

    for (HttpMethodConstraint methodConstraint : methodConstraints) {
      String httpMethod = methodConstraint.value();

      if (_methodNames.contains(httpMethod)) {
        throw new IllegalArgumentException("Http method "
          + httpMethod
          + " was already used.");
      } else {
        _methodNames.add(httpMethod);

        HttpMethodConstraintElement methodConstraintElement
          = new HttpMethodConstraintElement(httpMethod,
                                            new HttpConstraintElement(
                                              methodConstraint.emptyRoleSemantic(),
                                              methodConstraint.transportGuarantee(),
                                              methodConstraint.rolesAllowed()));

        _httpMethodConstraints.add(methodConstraintElement);
      }
    }
  }

  public Collection<HttpMethodConstraintElement> getHttpMethodConstraints()
  {
    return _httpMethodConstraints;
  }

  public Collection<String> getMethodNames()
  {
    return _methodNames;
  }
}
