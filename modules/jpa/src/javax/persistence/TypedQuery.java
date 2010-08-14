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

/**
 * Represents a typed SQL query.
 */
public interface TypedQuery<X> extends Query {
  public List<X> getResultQuery();
  
  public X getSingleResult();
  
  public TypedQuery<X> setMaxResults(int maxResult);
  
  public TypedQuery<X> setFirstResult(int startPosition);
  
  public TypedQuery<X> setHint(String hintName, Object value);
  
  public <T> TypedQuery<X> setParameter(Parameter<T> parameter, T value);
  
  public TypedQuery<X> setParameter(Parameter<Calendar> param,
                                    Calendar value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setParameter(Parameter<Date> param,
                                    Date value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setParameter(String name,
                                    Object value);
  
  public TypedQuery<X> setParameter(String name,
                                    Calendar value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setParameter(String name,
                                    Date value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setParameter(int position,
                                    Object value);
  
  public TypedQuery<X> setParameter(int position,
                                    Calendar value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setParameter(int position,
                                    Date value,
                                    TemporalType temporalType);
  
  public TypedQuery<X> setFlushMode(FlushModeType flushMode);
  
  public TypedQuery<X> setLockMode(LockModeType lockMode);
}
