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

package com.caucho.config.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents the security interception
 */
@Module
public class SecurityGenerator<X> extends AbstractAspectGenerator<X> {
  private String []_roles;
  private String _roleVar;

  private String _runAs;
 
  public SecurityGenerator(SecurityFactory<X> factory,
                           AnnotatedMethod<? super X> method,
                           AspectGenerator<X> next,
                           String []roleNames,
                           String runAs)
  {
    super(factory, method, next);
    
    _roles = roleNames;
    _runAs = runAs;
  }

  /**
   * Introspect EJB security annotations:
   *   @RunAs
   *   @RolesAllowed
   *   @PermitAll
   *   @DenyAll
   */
  /*
  @Override
  public void introspect(AnnotatedMethod<? super T> apiMethod, 
                         AnnotatedMethod<? super X> implMethod)
  {
    AnnotatedType<? super T> apiClass = apiMethod.getDeclaringType();
    AnnotatedType<X> implClass = getImplType();

    RunAs runAs = getAnnotation(RunAs.class, apiClass, implClass);
    
    if (runAs != null)
      _runAs = runAs.value();
    
    RolesAllowed rolesAllowed = getAnnotation(RolesAllowed.class, 
                                              apiMethod, 
                                              apiClass,
                                              implMethod, 
                                              implClass);

    if (rolesAllowed != null)
      _roles = rolesAllowed.value();

    PermitAll permitAll = getAnnotation(PermitAll.class, 
                                        apiMethod, 
                                        apiClass,
                                        implMethod, 
                                        implClass);

    if (permitAll != null)
      _roles = null;
    
    DenyAll denyAll = getAnnotation(DenyAll.class,
                                    apiMethod,
                                    implMethod);

    if (denyAll != null)
      _roles = new String[0];
  }
  */
  
  //
  // business method interception
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, 
                                     HashMap<String,Object> map)
    throws IOException
  {
    if (_roles != null) {
      _roleVar = "_role_" + out.generateId();

      out.print("private static String []" + _roleVar + " = new String[] {");

      for (int i = 0; i < _roles.length; i++) {
        if (i != 0)
          out.print(", ");

        out.print("\"");
        out.printJavaString(_roles[i]);
        out.print("\"");
      }

      out.println("};");
    }

    super.generateMethodPrologue(out, map);
  }
  
  //
  // invocation aspect code

  /**
   * Generates the method interceptor code
   */
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    if (_roleVar != null) {
      out.println("com.caucho.security.SecurityContext.checkUserInRole(" + _roleVar + ");");
      out.println();
    }

    if (_runAs != null) {
      out.print("String oldRunAs ="
                + " com.caucho.security.SecurityContext.runAs(\"");
      out.printJavaString(_runAs);
      out.println("\");");
    }
    
    super.generatePreTry(out);
  }

  /**
   * Generates the method interceptor code
   */
  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    super.generateFinally(out);
    
    if (_runAs != null) {
      out.println();
      out.println("com.caucho.security.SecurityContext.runAs(oldRunAs);");
    }
  }
}
