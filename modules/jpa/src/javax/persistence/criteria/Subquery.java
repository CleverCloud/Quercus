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

import java.util.List;
import java.util.Set;

/**
 * criteria subquery.
 *
 * @since JPA 2.0
 */
public interface Subquery<T> extends AbstractQuery<T>, Expression<T> {
  public Subquery<T> select(Expression<T> expression);
  
  public Subquery<T> where(Expression<Boolean> restriction);
  
  public Subquery<T> where(Predicate... restrictions);
  
  public Subquery<T> groupBy(Expression<?>... grouping);
  
  public Subquery<T> groupBy(List<Expression<?>> grouping);
  
  public Subquery<T> having(Expression<Boolean> having);
  
  public Subquery<T> having(Predicate... restrictions);
  
  public Subquery<T> distinct(boolean distinct);
  
  public <Y> Root<Y> correlate(Root<Y> parentRoot);
  
  public <X,Y> Join<X,Y> correlate(Join<X,Y> parentJoin);
  
  public <X,Y> CollectionJoin<X,Y> correlate(CollectionJoin<X,Y> parentCollection);
  
  public <X,Y> SetJoin<X,Y> correlate(SetJoin<X,Y> parentSet);
  
  public <X,Y> ListJoin<X,Y> correlate(ListJoin<X,Y> parentList);
  
  public <X,K,V> MapJoin<X,K,V> correlate(MapJoin<X,K,V> parentMap);
  
  public AbstractQuery<?> getParent();
  
  public Expression<T> getSelection();
  
  public Set<Join<?,?>> getCorrelatedJoins();
}
