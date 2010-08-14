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

package javax.enterprise.context.spi;

/**
 * Contextual creates and destroys instances of a given type.  In particular,
 * the {@link javax.enterprise.inject.spi.Bean} interface extends Contextual.
 *
 * Applications will not use Contextual, because its internal SPI, called by
 * the {@link javax.enterprise.inject.spi.Manager} during bean creation.
 */
public interface Contextual<T>
{
  /**
   * Creates a new instance for the Contextual's type.  If the instance
   * already exists in the CreationalContext, create will return it instead
   * of creating a new instance.
   *
   * <ol>
   * <li>create an instance of the bean
   * <li>create interceptor and decorator stacks
   * <li>inject dependencies
   * <li>set any XML-configured values
   * <li>call @PostConstruct
   * </ol>
   *
   * @param creationalContext the creation context used to support circular
   * references.
   *
   * @return the new instance
   */
  public T create(CreationalContext<T> creationalContext);

  /**
   * Destroys an instance for the Contextual's type.
   *
   * <ol>
   * <li>Call any {@link javax.enterprise.inject.Disposal @Disposal} method
   * <li>Call {@link javax.annotation.PreDestroy @PreDestroy} methods
   * <li>Destroy dependent objects
   * </ol>
   *
   * @param instance the instance to destroy
   */
  public void destroy(T instance, CreationalContext<T> creationalContext);
}
