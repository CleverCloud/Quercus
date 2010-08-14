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
import java.util.Set;

import javax.enterprise.context.spi.Contextual;

/**
 * Internal implementation for a Bean
 */
public interface Bean<T> extends Contextual<T> {
  //
  // metadata for the bean
  //

  /**
   * True for a disabled alternative.
   */
  public boolean isAlternative();

  /**
   * Returns the bean class.
   */
  public Class<?> getBeanClass();

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints();

  /**
   * Returns the bean's name or null if the bean does not have a primary name.
   */
  public String getName();

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable();

  /**
   * Returns the bean's qualifier annotations.
   */
  public Set<Annotation> getQualifiers();

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope();

  /**
   * Returns the stereotypes that the bean uses for priority
   */
  public Set<Class<? extends Annotation>> getStereotypes();

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes();
}
