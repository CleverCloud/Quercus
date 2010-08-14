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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Tuple;

/**
 * Interface to build criteria.
 *
 * @since JPA 2.0
 */
public interface CriteriaBuilder {
  public enum Trimspec {
    BOTH,
    LEADING,
    TRAILING
  }
  
  public CriteriaQuery<Object> createQuery();
  
  public <T> CriteriaQuery<T> createQuery(Class<T> resultClass);
  
  public CriteriaQuery<Tuple> createTupleQuery();
  
  public <Y> CompoundSelection<Y> construct(Class<Y> resultClass,
                                            Selection<?> ...selections);
  
  public CompoundSelection<Tuple> tuple(Selection<?>... selections);
  
  public CompoundSelection<Object[]> array(Selection<?>... selections);
  
  public Order asc(Expression<?> x);
  
  public Order desc(Expression<?> x);
  
  public <N extends Number> Expression<Double> avg(Expression<N> x);
  
  public <N extends Number> Expression<N> sum(Expression<N> x);
  
  public Expression<Long> sumAsLong(Expression<Integer> x);
  
  public Expression<Double> sumAsDouble(Expression<Float> x);
  
  public <N extends Number> Expression<N> max(Expression<N> x);
  
  public <N extends Number> Expression<N> min(Expression<N> x);
  
  public <X extends Comparable<? super X>> 
  Expression<X> greatest(Expression<X> x);
  
  public <X extends Comparable<? super X>> 
  Expression<X> least(Expression<X> x);
  
  public Expression<Long> count(Expression<?> x);
  
  public Expression<Long> countDistinct(Expression<?> x);
  
  public Predicate exists(Subquery<?> subquery);
  
  public <Y> Expression<Y> all(Subquery<Y> subquery);
  
  public <Y> Expression<Y> some(Subquery<Y> subquery);
  
  public <Y> Expression<Y> any(Subquery<Y> subquery);
  
  public Predicate and(Expression<Boolean> x,
                       Expression<Boolean> y);
  
  public Predicate and(Expression<Boolean>... restrictions);
  
  public Predicate or(Expression<Boolean> x,
                      Expression<Boolean> y);
  
  public Predicate or(Expression<Boolean>... restrictions);
  
  public Predicate not(Expression<Boolean> restriction);
  
  public Predicate conjunection();
  
  public Predicate disjunction();
  
  public Predicate isTrue(Expression<Boolean> x);
  
  public Predicate isFalse(Expression<Boolean> x);
  
  public Predicate isNull(Expression<?> x);
  
  public Predicate isNotNull(Expression<?> x);
  
  public Predicate equal(Expression<?> x, Expression<?> y);
  
  public Predicate equal(Expression<?> x, Object y);
  
  public Predicate notEqual(Expression<?> x, Expression<?> y);
  
  public Predicate notEqual(Expression<?> x, Object y);
  
  public <Y extends Comparable<? super Y>>
  Predicate greaterThan(Expression<? extends Y> x,
                        Expression<? extends Y> y);
  
  public <Y extends Comparable<? super Y>>
  Predicate greaterThan(Expression<? extends Y> x,
                        Y y);
  
  public <Y extends Comparable<? super Y>>
  Predicate greaterThanOrEqualTo(Expression<? extends Y> x,
                                 Expression<? extends Y> y);
  
  public <Y extends Comparable<? super Y>>
  Predicate greaterThanOrEqualTo(Expression<? extends Y> x,
                                 Y y);
  
  public <Y extends Comparable<? super Y>>
  Predicate lessThan(Expression<? extends Y> x,
                     Expression<? extends Y> y);
  
  public <Y extends Comparable<? super Y>>
  Predicate lessThan(Expression<? extends Y> x,
                     Y y);
  
  public <Y extends Comparable<? super Y>>
  Predicate lessThanOrEqualTo(Expression<? extends Y> x,
                              Expression<? extends Y> y);
  
  public <Y extends Comparable<? super Y>>
  Predicate lessThanOrEqualTo(Expression<? extends Y> x,
                              Y y);
  
  <Y extends Comparable<? super Y>>
  Predicate between(Expression<? extends Y> v,
                    Expression<? extends Y> x,
                    Expression<? extends Y> y);
  
  <Y extends Comparable<? super Y>>
  Predicate between(Expression<? extends Y> v,
                    Y x,
                    Y y);
  
  public Predicate gt(Expression<? extends Number> x,
                      Expression<? extends Number> y);
  
  public Predicate gt(Expression<? extends Number> x,
                      Number y);
  
  public Predicate ge(Expression<? extends Number> x,
                      Expression<? extends Number> y);
  
  public Predicate ge(Expression<? extends Number> x,
                      Number y);
  
  public Predicate lt(Expression<? extends Number> x,
                      Expression<? extends Number> y);
  
  public Predicate lt(Expression<? extends Number> x,
                      Number y);
  
  public Predicate le(Expression<? extends Number> x,
                      Expression<? extends Number> y);
  
  public Predicate le(Expression<? extends Number> x,
                      Number y);
  
  public <N extends Number> Expression<N> neg(Expression<N> x);
  
  public <N extends Number> Expression<N> abs(Expression<N> x);
  
  public <N extends Number> Expression<N> sum(Expression<? extends N> x,
                                              Expression<? extends N> y);
  
  public <N extends Number> Expression<N> sum(Expression<? extends N> x,
                                              N y);
  
  public <N extends Number> Expression<N> sum(N x,
                                              Expression<? extends N> y);
  
  public <N extends Number> Expression<N> prod(Expression<? extends N> x,
                                               Expression<? extends N> y);
  
  public <N extends Number> Expression<N> prod(Expression<? extends N> x,
                                               N y);
  
  public <N extends Number> Expression<N> prod(N x,
                                               Expression<? extends N> y);
  
  public <N extends Number> Expression<N> diff(Expression<? extends N> x,
                                               Expression<? extends N> y);
  
  public <N extends Number> Expression<N> diff(Expression<? extends N> x,
                                               N y);
  
  public <N extends Number> Expression<N> diff(N x,
                                               Expression<? extends N> y);
  
  public <N extends Number> Expression<N> quot(Expression<? extends N> x,
                                               Expression<? extends N> y);
  
  public <N extends Number> Expression<N> quot(Expression<? extends N> x,
                                               N y);
  
  public <N extends Number> Expression<N> quot(N x,
                                               Expression<? extends N> y);
  
  public Expression<Integer> mod(Expression<Integer> x,
                                 Expression<Integer> y);
  
  public Expression<Integer> mod(Expression<Integer> x,
                                 Integer y);
  
  public Expression<Integer> mod(Integer x,
                                 Expression<Integer> y);
  
  public Expression<Double> sqrt(Expression<? extends Number> x);
  
  public Expression<Long> toLong(Expression<? extends Number> number);
  
  public Expression<Integer> toInteger(Expression<? extends Number> number);
  
  public Expression<Float> toFloat(Expression<? extends Number> number);
  
  public Expression<Double> toDouble(Expression<? extends Number> number);
  
  public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> number);
  
  public Expression<BigInteger> toBigInteger(Expression<? extends Number> number);
  
  public Expression<String> toString(Expression<Character> expr);
  
  public <T> Expression<T> literal(T value);
  
  public <T> Expression<T> nullLiteral(Class<T> resultClass);
  
  public <T> ParameterExpression<T> parameter(Class<T> paramClass);
  
  public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name);
  
  public <C extends Collection<?>>
  Predicate isEmpty(Expression<C> collection);
  
  public <C extends Collection<?>>
  Predicate isNotEmpty(Expression<C> collection);

  public <C extends Collection<?>>
  Expression<Integer> size(Expression<C> collection);
  
  public <C extends Collection<?>>
  Expression<Integer> size(C collection);
 
  public <E,C extends Collection<E>>
  Predicate isMember(Expression<E> elem,
                     Expression<C> collection);
  
  public <E,C extends Collection<E>>
  Predicate isMember(E elem,
                     Expression<C> collection);
  
  public <E,C extends Collection<E>>
  Predicate isNotMember(Expression<E> elem,
                        Expression<C> collection);
  
  public <E,C extends Collection<E>>
  Predicate isNotMember(E elem,
                        Expression<C> collection);
  
  public <V,M extends Map<?,V>>
  Expression<Collection<V>> values(M map);
  
  public <K,M extends Map<K,?>>
  Expression<Set<K>> keys(M map);
  
  public Predicate like(Expression<String> x,
                        Expression<String> pattern);
  
  public Predicate like(Expression<String> x,
                        String pattern);
  
  public Predicate like(Expression<String> x,
                        Expression<String> pattern,
                        Expression<Character> escapeChar);
  
  public Predicate like(Expression<String> x,
                        Expression<String> pattern,
                        char escapeChar);
  
  public Predicate like(Expression<String> x,
                        String pattern,
                        Expression<Character> escapeChar);
  
  public Predicate like(Expression<String> x,
                        String pattern,
                        char escapeChar);
  
  public Predicate notLike(Expression<String> x,
                           Expression<String> pattern);
  
  public Predicate notLike(Expression<String> x,
                           String pattern);
  
  public Predicate notLike(Expression<String> x,
                           Expression<String> pattern,
                           Expression<Character> escapeChar);
  
  public Predicate notLike(Expression<String> x,
                           Expression<String> pattern,
                           char escapeChar);
  
  public Predicate notLike(Expression<String> x,
                           String pattern,
                           Expression<Character> escapeChar);
  
  public Predicate notLike(Expression<String> x,
                           String pattern,
                           char escapeChar);
  
  public Expression<String> concat(Expression<String> x,
                                   Expression<String> y);
  
  public Expression<String> concat(Expression<String> x,
                                   String y);
  
  public Expression<String> concat(String x,
                                   Expression<String> y);
  
  public Expression<String> substring(Expression<String> x,
                                      Expression<Integer> from);
  
  public Expression<String> substring(Expression<String> x,
                                      int from);
  
  public Expression<String> substring(Expression<String> x,
                                      Expression<Integer> from,
                                      Expression<Integer> len);
  
  public Expression<String> substring(Expression<String> x,
                                      int from,
                                      int len);
  
  public Expression<String> trim(Expression<String> x);
  
  public Expression<String> trim(Trimspec trim,
                                 Expression<String> x);
  
  public Expression<String> trim(Expression<Character> t,
                                 Expression<String> x);
  
  public Expression<String> trim(Trimspec trimSpec,
                                 Expression<Character> t,
                                 Expression<String> x);
  
  public Expression<String> trim(char t,
                                 Expression<String> x);
  
  public Expression<String> trim(Trimspec trimSpec,
                                 char t,
                                 Expression<String> x);
  
  public Expression<String> lower(Expression<String> x);
  
  public Expression<String> upper(Expression<String> x);
  
  public Expression<Integer> length(Expression<String> x);
  
  public Expression<Integer> locate(Expression<String> x,
                                    Expression<String> pattern);
  
  public Expression<Integer> locate(Expression<String> x,
                                    String pattern);
  
  public Expression<Integer> locate(Expression<String> x,
                                    Expression<String> pattern,
                                    Expression<Integer> from);
  
  public Expression<Integer> locate(Expression<String> x,
                                    String pattern,
                                    int from);
  
  public Expression<Date> currentDate();
  
  public Expression<Timestamp> currentTimestamp();
  
  public Expression<Time> currentTime();
  
  public <T> In<T> in(Expression<? extends T> expression);
  
  public <Y> Expression<Y> coalesce(Expression<? extends Y> x,
                                    Expression<? extends Y> y);
  
  public <Y> Expression<Y> coalesce(Expression<? extends Y> x,
                                    Y y);
  
  public <Y> Expression<Y> nullif(Expression<Y> x,
                                  Expression<?> y);
  
  public <Y> Expression<Y> nullif(Expression<Y> x,
                                  Y y);
  
  public <T> Coalesce<T> coalesce();
  
  public <C,R> SimpleCase<C,R> selectCase(Expression<? extends C> expression);
  
  public <R> Case<R> selectCase();
  
  public <T> Expression<T> function(String name,
                                    Class<T> type,
                                    Expression<?>... args);
  
  public interface Case<R> extends Expression<R> {
    public Case<R> when(Expression<Boolean> condition,
                        R result);
    
    public Case<R> when(Expression<Boolean> condition,
                        Expression<? extends R> result);
    
    public Expression<R> otherwise(R result);
    
    public Expression<R> otherwise(Expression<? extends R> result);
  }
  
  public interface SimpleCase<C,R> extends Expression<R> {
    public Expression<C> getExpression();
    
    public SimpleCase<C,R> when(C condition, R result);
    
    public SimpleCase<C,R> when(C condition, Expression<? extends R> result);
    
    public Expression<R> otherwise(R result);
    
    public Expression<R> otherwise(Expression<? extends R> result);
  }
  
  public interface Coalesce<T> extends Expression<T> {
    public Coalesce<T> value(T value);
    
    public Coalesce<T> value(Expression<? extends T> value);
  }
  
  public interface In<T> extends Predicate {
    public Expression<T> getExpression();
    
    public In<T> value(T value);
    
    public In<T> value(Expression<? extends T> value);
  }
  
}
