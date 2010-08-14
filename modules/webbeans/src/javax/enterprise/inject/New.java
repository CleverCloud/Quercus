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

package javax.enterprise.inject;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * New bean creation and injection uses the {@literal @New} annotation as the
 * {@link javax.inject.Qualifier @Qualifier} for the injection point.
 *
 * The {@literal @New} annotation injects a new instance of a bean to
 * the injection
 * point.  The configuration for the {@literal @New} instance is separate
 * from any
 * simple bean configuration.
 *
 * <ul>
 * <li>Initializer methods and injected fields are defined by annotations.
 * <li>Interceptor bindings are defined by annotations.
 * <li>Scope is {@link javax.enterprise.context.Dependent @Dependent}
 * <li>Deployment type is  {@link javax.enterprise.context.Standard @Standard}
 * <li>The binding is {@literal @New}
 * <li>No bean {@link javax.annotation.Named @Named}
 * <li>No {@link javax.annotation.Stereotype @Stereotypes}
 * <li>No {@link javax.event.Observer @Observer} methods,
 * {@link javax.enterprise.inject.Produces @Produces} methods, or
 * {@link javax.enterprise.inject.Disposes @Disposes} methods
 * </ul>
 *
 * <h3>example: {@literal @New} injection</h3>
 *
 * <code><pre>
 * class MyBean {
 *   {@literal @New}
 *   private SubBean _bean;
 * }
 * </pre></code>
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE})
public @interface New {
  public Class<?> value() default New.class;
}
