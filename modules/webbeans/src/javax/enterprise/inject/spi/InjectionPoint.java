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

package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.Member;
import java.util.Set;

/**
 */
public interface InjectionPoint
{
  /**
   * Returns the declared type of the injection point, e.g. an
   * injected field's type.
   */
  public Type getType();

  /**
   * Returns the declared qualifiers on the injection point.
   */
  public Set<Annotation> getQualifiers();

  /**
   * Returns the owning bean for the injection point.
   */
  public Bean<?> getBean();

  /**
   * Returns the Field for field injection, the Method for method injection,
   * and Constructor for constructor injection.
   */
  public Member getMember();

  /**
   * Returns all annotations on the injection point.
   */
  public Annotated getAnnotated();

  /**
   * A delegate injection point is true for Decorators
   */
  public boolean isDelegate();

  /**
   * Test if the injection point is a java transient
   */
  public boolean isTransient();
}
