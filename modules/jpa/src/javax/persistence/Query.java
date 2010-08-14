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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main application interface to the persistence context.
 */
public interface Query {
  /**
   * Executes a SELECT and return the results as a list.
   */
  public List getResultList();

  /**
   * Returns the single result of a query.
   */
  public Object getSingleResult();

  /**
   * An update or delete query.
   */
  public int executeUpdate();

  /**
   * The maximum number of results to retrieve.
   */
  public Query setMaxResults(int maxResult);

  /**
   * The maximum number of results to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getMaxResults();

  /**
   * Sets the first result.
   */
  public Query setFirstResult(int startPosition);

  /**
   * The first to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getFirstResult();

  /**
   * An implementation-specific hint.
   */
  public Query setHint(String hintName, Object value);

  /**
   * Returns the implementation-specific hints
   *
   * @Since JPA 2.0
   */
  public Map<String,Object> getHints();

  /**
   * Binds a named parameter.
   */
  public <T> Query setParameter(Parameter<T> param, T value);

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(Parameter<Calendar> param,
                            Calendar date, 
                            TemporalType type); 

  /**
   * Sets a date parameter.
   */
  public Query setParameter(Parameter<Calendar> param,
                            java.util.Date date,
                            TemporalType type); 

  /**
   * Binds a position parameter.
   */
  public Query setParameter(String name, Object value);

  /**
   * Binds a position parameter.
   */
  public Query setParameter(int pos, Object value);

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(String name,
                            Calendar date, 
                            TemporalType type); 

  /**
   * Sets a date parameter.
   */
  public Query setParameter(String name,
                            java.util.Date date,
                            TemporalType type); 

  /**
   * Sets a date parameter.
   */
  public Query setParameter(int pos, Date date, TemporalType type); 

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(int pos, Calendar date, TemporalType type);
  
  public Set<Parameter<?>> getParameters();
  
  public Parameter<?> getParameter(String name);
  
  public <T> Parameter<T> getParameter(String name, Class<T> type);
  
  public Parameter<?> getParameter(int pos);
  
  public <T> Parameter<T> getParameter(int position, Class<T> type);

  public boolean isBound(Parameter<?> param);
  
  public <T> T getParameterValue(Parameter<T> param);
  
  public Object getParameterValue(String name);
  
  public Object getParameterValue(int position);
  
  /**
   * Sets the flush type.
   */
  public Query setFlushMode(FlushModeType flushMode);

  /**
   * Gets the flush type.
   *
   * @since JPA 2.0
   */
  public FlushModeType getFlushMode();

  /**
   * Sets the lock type.
   *
   * @since JPA 2.0
   */
  public Query setLockMode(LockModeType lockMode);

  /**
   * Gets the lock type.
   *
   * @since JPA 2.0
   */
  public LockModeType getLockMode();
  
  public <T> T unwrap(Class<T> cl);
}
