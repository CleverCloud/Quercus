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
 * Criteria query.
 *
 * @since JPA 2.0
 */
public interface CriteriaQuery<T> extends AbstractQuery<T> {
  public CriteriaQuery<T> select(Selection<? extends T> selection);
  
  public CriteriaQuery<T> multiselect(Selection<?>... selection);
  
  public CriteriaQuery<T> multiselect(List<Selection<?>> selectionList);
  
  public CriteriaQuery<T> where(Expression<Boolean> restriction);
  
  public CriteriaQuery<T> where(Predicate... restrictions);
  
  public CriteriaQuery<T> groupBy(Expression<?>... grouping);
  
  public CriteriaQuery<T> groupBy(List<Expression<?>> grouping);
  
  public CriteriaQuery<T> having(Expression<Boolean> restriction);
  
  public CriteriaQuery<T> having(Predicate... restrictions);
  
  public CriteriaQuery<T> orderBy(Order... o);
  
  public CriteriaQuery<T> orderBy(List<Order> o);
  
  public CriteriaQuery<T> distinct(boolean distinct);
  
  public List<Order> getOrderList();
  
  public Set<ParameterExpression<?>> getParameters();
}
