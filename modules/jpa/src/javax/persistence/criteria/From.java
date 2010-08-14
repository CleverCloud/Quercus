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

package javax.persistence.criteria;

import java.util.Set;

import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 * A root type from a FROM clause.
 *
 * @since JPA 2.0
 */
public interface From<Z,X> extends Path<X>, FetchParent<Z,X> {
  public Set<Join<X,?>> getJoins();
  
  public boolean isCorrelated();
  
  public From<Z,X> getCorrelationParent();
  
  public <Y> Join<X,Y> join(SingularAttribute<? super X,Y> attribute);
  
  public <Y> Join<X,Y> join(SingularAttribute<? super X,Y> attribute, 
                            JoinType joinType);
  
  public <Y> CollectionJoin<X,Y> join(CollectionAttribute<? super X,Y> collection);
  
  public <Y> SetJoin<X,Y> join(SetAttribute<? super X,Y> set);
  
  public <Y> ListJoin<X,Y> join(ListAttribute<? super X,Y> list);
  
  public <K,V> MapJoin<X,K,V> join(MapAttribute<? super X,K,V> map);
  
  public <Y> CollectionJoin<X,Y> join(CollectionAttribute<? super X,Y> collection,
                                      JoinType joinType);
  
  public <Y> SetJoin<X,Y> join(SetAttribute<? super X,Y> set,
                               JoinType joinType);
  
  public <Y> ListJoin<X,Y> join(ListAttribute<? super X,Y> list,
                                JoinType joinType);
  
  public <K,V> MapJoin<X,K,V> join(MapAttribute<? super X,K,V> map,
                                 JoinType joinType);
  
  public <Y> Join<X,Y> join(String attributeName);
  
  public <Y> CollectionJoin<X,Y> joinCollection(String attributeName);
  
  public <Y> SetJoin<X,Y> joinSet(String attributeName);
  
  public <Y> ListJoin<X,Y> joinList(String attributeName);
  
  public <K,V> MapJoin<X,K,V> joinMap(String attributeName);
  
  public <Y> Join<X,Y> join(String attributeName, JoinType joinType);
  
  public <Y> CollectionJoin<X,Y> joinCollection(String attributeName,
                                                  JoinType joinType);
  
  public <Y> SetJoin<X,Y> joinSet(String attributeName,
                                    JoinType joinType);
  
  public <Y> ListJoin<X,Y> joinList(String attributeName, JoinType joinType);
  
  public <K,V> MapJoin<X,K,V> joinMap(String attributeName, JoinType joinType);  
}
