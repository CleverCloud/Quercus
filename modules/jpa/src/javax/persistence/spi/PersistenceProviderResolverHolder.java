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

package javax.persistence.spi;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;

/**
 * Holder for the available providers.
 */
public class PersistenceProviderResolverHolder {
  private static WeakHashMap<ClassLoader,SoftReference<PersistenceProviderResolver>> _map
    = new WeakHashMap<ClassLoader,SoftReference<PersistenceProviderResolver>>();
  
  public static PersistenceProviderResolver getPersistenceProviderResolver()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    SoftReference<PersistenceProviderResolver> ref;
    
    synchronized (_map) {
      ref = _map.get(loader);
    }
    
    if (ref != null)
      return ref.get();
    else
      return null;
  }
  
  public static void
  setPersistenceProviderResolver(PersistenceProviderResolver resolver)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    SoftReference<PersistenceProviderResolver> ref
      = new SoftReference<PersistenceProviderResolver>(resolver);
    
    synchronized (_map) {
      _map.put(loader, ref);
    }
  }
}
