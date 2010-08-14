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
package com.caucho.amber.ejb3;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.UserQuery;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.util.L10N;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The EJB query
 */
public class QueryImpl implements Query {
  private static final L10N L = new L10N(QueryImpl.class);
  
  private AbstractQuery _query;
  private UserQuery _userQuery;
    
  private AmberConnection _aConn;
  private int _firstResult;
  private int _maxResults = Integer.MAX_VALUE / 2;

  /**
   * Creates a manager instance.
   */
  QueryImpl(AbstractQuery query, AmberConnection aConn)
  {
    _query = query;
    _aConn = aConn;

    _userQuery = new UserQuery(query);
    _userQuery.setSession(_aConn);
  }

  /**
   * Execute the query and return as a List.
   */
  public List getResultList()
  {
    try {
      ArrayList results = new ArrayList();
      
      ResultSet rs = executeQuery();

      while (rs.next())
        results.add(rs.getObject(1));

      rs.close();

      return results;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Returns a single result.
   */
  public Object getSingleResult()
  {
    try {
      ResultSet rs = executeQuery();

      Object value = null;

      if (rs.next())
        value = rs.getObject(1);

      rs.close();

      return value;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Execute an update or delete.
   */
  public int executeUpdate()
  {
    try {
      return _userQuery.executeUpdate();
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Executes the query returning a result set.
   */
  protected ResultSet executeQuery()
    throws SQLException
  {
    return _userQuery.executeQuery();
  }

  /**
   * Sets the maximum result returned.
   */
  public Query setMaxResults(int maxResult)
  {
    return this;
  }

  /**
   * Sets the position of the first result.
   */
  public Query setFirstResult(int startPosition)
  {
    return this;
  }

  /**
   * Sets a hint.
   */
  public Query setHint(String hintName, Object value)
  {
    return this;
  }

  /**
   * Sets a named parameter.
   */
  public Query setParameter(String name, Object value)
  {
    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(String name, Date value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(String name, Calendar value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets an indexed parameter.
   */
  public Query setParameter(int index, Object value)
  {
    _userQuery.setObject(index, value);
    
    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(int index, Date value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(int index, Calendar value, TemporalType type)
  {
    return this;
  }

  /**
   * Sets the flush mode type.
   */
  public Query setFlushMode(FlushModeType mode)
  {
    return this;
  }

  //
  // extensions

  /**
   * Sets an indexed parameter.
   */
  public Query setDouble(int index, double value)
  {
    _userQuery.setDouble(index, value);
    
    return this;
  }

  /**
   * The maximum number of results to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getMaxResults()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * The first to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getFirstResult()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the implementation-specific hints
   *
   * @Since JPA 2.0
   */
  public Map getHints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the supported hints
   *
   * @Since JPA 2.0
   */
  public Set<String> getSupportedHints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the named parameters as a map
   *
   * @since JPA 2.0
   */
  public Map getNamedParameters()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the positional parameters as a list
   *
   * @since JPA 2.0
   */
  public List getPositionalParameters()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the flush type.
   *
   * @since JPA 2.0
   */
  public FlushModeType getFlushMode()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the lock type.
   *
   * @since JPA 2.0
   */
  public Query setLockMode(LockModeType lockMode)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the lock type.
   *
   * @since JPA 2.0
   */
  public LockModeType getLockMode()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(java.lang.String)
   */
  @Override
  public Parameter<?> getParameter(String name)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> Parameter<T> getParameter(String name, Class<T> type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(int)
   */
  @Override
  public Parameter<?> getParameter(int pos)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(int, java.lang.Class)
   */
  @Override
  public <T> Parameter<T> getParameter(int position, Class<T> type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(javax.persistence.Parameter)
   */
  @Override
  public <T> T getParameterValue(Parameter<T> param)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(java.lang.String)
   */
  @Override
  public Object getParameterValue(String name)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(int)
   */
  @Override
  public Object getParameterValue(int position)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameters()
   */
  @Override
  public Set<Parameter<?>> getParameters()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#isBound(javax.persistence.Parameter)
   */
  @Override
  public boolean isBound(Parameter<?> param)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.lang.Object)
   */
  @Override
  public <T> Query setParameter(Parameter<T> param, T value)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Calendar, javax.persistence.TemporalType)
   */
  @Override
  public Query setParameter(Parameter<Calendar> param, Calendar date,
                            TemporalType type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Date, javax.persistence.TemporalType)
   */
  @Override
  public Query setParameter(Parameter<Calendar> param, Date date,
                            TemporalType type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#unwrap(java.lang.Class)
   */
  @Override
  public <T> T unwrap(Class<T> cl)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
