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

import java.lang.annotation.Annotation;

/**
 * Represents a bean scope, storing the bean instances available to the
 * execution context, and corresponding to a
 * {@link javax.enterprise.context.ScopeType @ScopeType}.
 *
 * The Context is an SPI class, called by
 * the {@link javax.inject.manager.Manager inject
 * Manager}, but not normally by application code.  Applications will call
 * {@link javax.enterprise.inject.spi.BeanManager#newInstance BeanManager.newInstance} to
 * create a new instance of a bean.
 *
 * Example contexts include @ApplicationScoped for singleton objects,
 * @RequestScoped for servlet request-specific objects, and @SessionScoped
 * for HTTP session objects.
 *
 * Applications may create their own Contexts by creating an associated
 * @ScopeType annotation and registering their Context with the inject
 * manager.
 */
public interface Context
{
  /**
   * Returns the @ScopeType corresponding to the current context.
   */
  public Class<? extends Annotation> getScope();

  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive();

  /**
   * Returns a instance of a bean, creating if the bean is not already
   * available in the context.
   *
   * @param bean the Bean type to be created
   *
   * @return an injected and initialized instance
   */
  public <T> T get(Contextual<T> bean);

  /**
   * Internal SPI method to create a new instance of a bean, when given
   * a creational context.  This method is needed to handle circular
   * initialization of bean instances.  If the bean already exists
   * in the creationalContext, return the existing bean.
   *
   * @param bean the Bean type to be created
   * @param creationalContext - temporary context used for managing
   *  circular references
   *
   * @return the bean instance
   */
  public <T> T get(Contextual<T> bean,
                   CreationalContext<T> creationalContext);
}
