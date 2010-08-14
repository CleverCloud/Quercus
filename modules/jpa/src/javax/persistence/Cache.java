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

package javax.persistence;

/**
 * Cache for objects
 *
 * @since JPA 2.0
 */
public interface Cache {
  /**
   * Returns true if the cache contains data for the entity
   *
   * @since JPA 2.0
   */
  @SuppressWarnings("unchecked")
  public boolean contains(Class cl, Object key);
  
  /**
   * Evict the data
   *
   * @since JPA 2.0
   */
  @SuppressWarnings("unchecked")
  public void evict(Class cl, Object key);
  
  /**
   * Evict call members of the given class
   *
   * @since JPA 2.0
   */
  @SuppressWarnings("unchecked")
  public void evict(Class cl);
  
  /**
   * Clears the cache
   *
   * @since JPA 2.0
   */
  public void evictAll();
}
