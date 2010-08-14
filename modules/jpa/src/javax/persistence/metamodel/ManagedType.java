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

package javax.persistence.metamodel;

import java.util.Set;

/**
 * Reflection model for a JPA entity.
 *
 * @since JPA 2.0
 */
public interface ManagedType<X> extends Type<X> {
  public Set<Attribute<? super X,?>> getAttributes();
  
  public Set<Attribute<X,?>> getDeclaredAttributes();
  
  public <Y> SingularAttribute<? super X,Y> getSingularAttribute(String name,
                                                                 Class<Y> type);
  
  public <Y> SingularAttribute<X,Y> getDeclaredSingularAttribute(String name,
                                                                 Class<Y> type);
  
  public Set<SingularAttribute<? super X,?>> getSingularAttributes();
  
  public Set<SingularAttribute<X,?>> getDeclaredSingularAttributes();
  
  public <E> CollectionAttribute<? super X,E> getCollection(String name,
                                                            Class<E> elementType);
  
  public <E> CollectionAttribute<X,E> getDeclaredCollection(String name,
                                                            Class<E> elementType);
  
  public <E> SetAttribute<? super X,E> getSet(String name,
                                              Class<E> elementType);
  
  public <E> SetAttribute<X,E> getDeclaredSet(String name,
                                              Class<E> elementType);
  
  public <E> ListAttribute<X,E> getDeclaredList(String name,
                                                Class<E> elementType);
  
  public <K,V> MapAttribute<? super X,K,V>
    getMap(String name,
           Class<K> keyType,
           Class<V> valueType);
           
  public <K,V> MapAttribute<X,K,V>
    getDeclaredMap(String name,
                   Class<K> keyType,
                   Class<V> valueType);
  
  public Set<PluralAttribute<? super X,?,?>> getPluralAttributes();
  
  public Set<PluralAttribute<X,?,?>> getDeclaredPluralAttributes();
  
  public Attribute<? super X,?> getAttribute(String name);
  
  public Attribute<X,?> getDeclaredAttribute(String name);
  
  public SingularAttribute<? super X,?> getSingularAttribute(String name);
  
  public SingularAttribute<X,?> getDeclaredSingularAttribute(String name);
  
  public CollectionAttribute<? super X,?> getCollection(String name);
  
  public CollectionAttribute<X,?> getDeclaredCollection(String name);
  
  public SetAttribute<? super X,?> getSet(String name);
  
  public SetAttribute<X,?> getDeclaredSet(String name);
  
  public ListAttribute<? super X,?> getList(String name);
  
  public ListAttribute<X,?> getDeclaredList();
  
  public MapAttribute<? super X,?,?> getMap(String name);
  
  public MapAttribute<X,?,?> getDeclaredMap(String name);
           
}
