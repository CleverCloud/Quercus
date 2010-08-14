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
 * When an injection {@link javax.enterprise.inject.spi.BeanManager#getInstance
 * Manager.getInstance} call cannot return a unique bean, it throws
 * this exception.
 *
 * This ambiguity may occur when two subtypes match, e.g. <code>BeanA</code>
 * and <code>BeanB</code> both implementing <code>@Current BeanAPI</code>.
 *
 * The ambiguity may also occur when using more than
 * one {@link javax.enterprise.inject.BindingType @BindingType} on the same
 * bean or when using the special {@link javax.enterprise.inject.Any @Any}
 * bindings, for example:
 *
 * <pre><code>
 * @Foo @A public class Bean {}
 * @Foo @B public class Bean {}
 *
 * class MyClass {
 *   @Foo Bean _fooField
 *   @Any Bean _anyField
 * }
 * </code></pre>
 */
public class AmbiguousResolutionException extends ResolutionException
{
  private static final long serialVersionUID =  -2132733164534544788L;

  public AmbiguousResolutionException()
  {
  }

  public AmbiguousResolutionException(String message)
  {
    super(message);
  }

  public AmbiguousResolutionException(Throwable cause)
  {
    super(cause);
  }

  public AmbiguousResolutionException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
