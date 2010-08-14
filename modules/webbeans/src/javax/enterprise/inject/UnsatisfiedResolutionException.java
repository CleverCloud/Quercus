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

/**
 * When no matching bean can be found for an injection point or a
 * {@link javax.enterprise.inject.spi.BeanManager#getInstance BeanManager.getReference},
 * the BeanManager {@link javax.enterprise.inject.spi.BeanManager} will throw this exception.
 *
 * This exception may occur when no beans are registered, or are registered
 * with a different {@link javax.enterprise.inject.BindingType @BindingType}
 * than expected.
 *
 * <pre><code>
 * @A public class Bean {}
 *
 * class MyClass {
 *   @B Bean _bField
 *   @Current Bean _currentField
 * }
 * </code></pre>
 */
public class UnsatisfiedResolutionException extends ResolutionException
{
  private static final long serialVersionUID = 5350603312442756709L;

  public UnsatisfiedResolutionException()
  {
  }

  public UnsatisfiedResolutionException(String message)
  {
    super(message);
  }

  public UnsatisfiedResolutionException(Throwable cause)
  {
    super(cause);
  }

  public UnsatisfiedResolutionException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
