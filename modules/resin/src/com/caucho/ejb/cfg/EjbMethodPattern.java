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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.config.ConfigException;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.util.L10N;

/**
 * Configuration for a method.
 */
public class EjbMethodPattern<X> {
  private static L10N L = new L10N(EjbMethodPattern.class);

  public final static int RESIN_DATABASE = 0;
  public final static int RESIN_READ_ONLY = 1;
  public final static int RESIN_ROW_LOCK = 2;

  private EjbBean<X> _bean;

  private String _location;

  private MethodSignature _signature;
  
  private int _resinIsolation = -1;
  private int _jdbcIsolation = -1;
  
  private boolean _queryLoadsBean = true;
  private boolean _relationLoadsBean;
  
  private String _query;
  private String _queryLocation;
  
  private TransactionAttributeType _transactionType = null;
  private ArrayList<String> _roles;

  /**
   * Creates a new method.
   */
  public EjbMethodPattern()
  {
  }

  /**
   * Creates a new method.
   *
   * @param entity the owning entity bean.
   * @param signature the method signature.
   */
  public EjbMethodPattern(EjbBean<X> bean, MethodSignature signature)
  {
    _bean = bean;
    _signature = signature;
  }

  /**
   * Sets the bean.
   */
  public void setBean(EjbBean<X> bean)
  {
    _bean = bean;
  }

  /**
   * Sets the config location.
   */
  public void setLocation(String location)
  {
    _location = location;
  }

  /**
   * Returns the config location.
   */
  public String getLocation()
  {
    return _location;
  }

  /**
   * Sets the method signature.
   */
  public void setSignature(MethodSignature sig)
  {
    _signature = sig;
  }

  /**
   * Returns the method signature.
   */
  public MethodSignature getSignature()
  {
    return _signature;
  }

  /**
   * Returns the method name.
   */
  public String getName()
  {
    return _signature.getName();
  }

  /**
   * Returns true if the method does not set any values.
   */
  public boolean isReadOnly()
  {
    return _resinIsolation == RESIN_READ_ONLY;
  }

  /**
   * Returns the Resin isolation.
   */
  public int getResinIsolation()
  {
    return _resinIsolation;
  }

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(String isolation)
    throws ConfigException
  {
    if (isolation.equals("read-only"))
      _resinIsolation = RESIN_READ_ONLY;
    else if (isolation.equals("database"))
      _resinIsolation = RESIN_DATABASE;
    else if (isolation.equals("row-locking"))
      _resinIsolation = RESIN_ROW_LOCK;
    else
      throw new ConfigException(L.l("'{0}' is an unknown value for resin-isolation.  Only 'read-only', 'database', and 'row-locking' are allowed.",
                                    isolation));
  }

  /**
   * Returns the JDBC isolation.
   */
  public int getJDBCIsolation()
  {
    return _jdbcIsolation;
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJDBCIsolation(int isolation)
  {
    _jdbcIsolation = isolation;
  }

  /**
   * Returns the method's query.
   */
  public String getQuery()
  {
    return _query;
  }

  /**
   * Sets the method's query.
   */
  public void setQuery(String query)
  {
    _query = query;
  }

  /**
   * Returns the query config location.
   */
  public String getQueryLocation()
  {
    return _queryLocation;
  }

  /**
   * Sets the query node.
   */
  public void setQueryLocation(String location)
  {
    _queryLocation = location;
  }

  /**
   * Returns the method's transaction type, e.g. Required.
   */
  public TransactionAttributeType getTransactionType()
  {
    if (_transactionType != null)
      return _transactionType;
    else if (isReadOnly())
      return TransactionAttributeType.SUPPORTS;
    else
      return TransactionAttributeType.REQUIRED;
  }

  public void setTransaction(TransactionAttributeType type)
    throws ConfigException
  {
    _transactionType = type;
  }
  
  /**
   * Sets the method's transaction type, e.g. Required
   */
  public void setTransAttribute(String type)
    throws ConfigException
  {
    if ("Required".equals(type))
      _transactionType = TransactionAttributeType.REQUIRED;
    else if ("RequiresNew".equals(type))
      _transactionType = TransactionAttributeType.REQUIRES_NEW;
    else if ("Mandatory".equals(type))
      _transactionType = TransactionAttributeType.MANDATORY;
    else if ("NotSupported".equals(type))
      _transactionType = TransactionAttributeType.NOT_SUPPORTED;
    else if ("Never".equals(type))
      _transactionType = TransactionAttributeType.NEVER;
    else if ("Supports".equals(type))
      _transactionType = TransactionAttributeType.SUPPORTS;
    else
      throw new ConfigException(L.l("'{0}' is an unknown transaction type.  The transaction types are:\n  Required - creates a new transaction if none is active.\n  RequiresNew - always creates a new transaction.\n  Mandatory - requires an active transaction.\n  NotSupported - suspends any active transaction.\n  Never - forbids any active transaction.\n  Supports - allows a transaction or no transaction.", type));
  }

  /**
   * Returns true if the query method should load bean values.
   */
  public boolean getQueryLoadsBean()
  {
    return _queryLoadsBean;
  }

  /**
   * Set true if the query method should load bean values.
   */
  public void setQueryLoadsBean(boolean loadBean)
  {
    _queryLoadsBean = loadBean;
  }

  /**
   * Returns true if the relation method should load bean values.
   */
  public boolean getRelationLoadsBean()
  {
    return _relationLoadsBean;
  }

  /**
   * Set true if the relation method should load bean values.
   */
  public void setRelationLoadsBean(boolean loadBean)
  {
    _relationLoadsBean = loadBean;
  }

  /**
   * Returns the roles allowed for the method.
   */
  public ArrayList<String> getRoles()
  {
    return _roles;
  }

  /**
   * Set the roles allowed for the method.
   */
  public void setRoles(ArrayList<String> roles)
  {
    _roles = roles;
  }

  /**
   * Set the roles allowed for the method.
   */
  public void setRoles(String []roles)
  {
    if (roles != null) {
      if (_roles == null)
        _roles = new ArrayList();
      
      for (String role : roles) {
        _roles.add(role);
      }
    }
  }

  /**
   * Configures the bean with the override values
   */
  public void configure(AnnotatedMethod<?> apiMethod)
  {
    if (_signature.isMatch(apiMethod.getJavaMember().getName(),
                           apiMethod.getJavaMember().getParameterTypes())) {
      // configureSecurity(apiMethod);
      configureXA(apiMethod);
    }
  }

  private void configureXA(AnnotatedMethod<?> apiMethod)
  {
    if (_transactionType == null)
      return;

    Annotation ann = new TransactionAttribute() {
        public Class annotationType() { return TransactionAttribute.class; }
        public TransactionAttributeType value() { return _transactionType; }
      };

    ((AnnotatedMethodImpl<?>) apiMethod).addAnnotation(ann);

    // XXX: need to modify ...
    
    // XaCallChain xa = bizMethod.getXa();

    // xa.setTransactionType(_transactionType);
  }

  /*
  private void configureSecurity(AspectGenerator bizMethod)
  {
    if (_roles == null)
      return;

    SecurityGenerator security = bizMethod.getSecurity();

    // security.setRoles(_roles);
  }
  */

  /**
   * Returns true if these are equivalent.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EjbMethodPattern<?>))
      return false;

    EjbMethodPattern<?> method = (EjbMethodPattern<?>) o;

    return _signature.equals(method.getSignature());
  }

  public String toString()
  {
    return "EJBMethodPattern[" + _signature.getName() + "]";
  }
}
