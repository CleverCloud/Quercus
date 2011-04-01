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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

import javax.inject.Qualifier;

/**
 * JpaPersistenceContext is a binding type for the JPA EntityManager, letting
 * you select the context by its name and allowing the extended value to
 * be selected.
 */
@Qualifier
@Documented
@Target({TYPE,FIELD,METHOD,PARAMETER})
@Retention(RUNTIME)
public @interface JpaPersistenceContext {
  /**
   * The unitName of the JPA Persistence Context
   */
  public String value();

  /**
   * If true, return the extended persistence context, defaults to false.
   */
  public boolean extended() default false;
}
