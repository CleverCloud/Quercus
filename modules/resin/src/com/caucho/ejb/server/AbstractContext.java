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
package com.caucho.ejb.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBMetaData;
import javax.ejb.TimerService;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import com.caucho.config.gen.CandiInvocationContext;
import com.caucho.security.SecurityContext;
import com.caucho.security.SecurityContextException;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.util.L10N;

/**
 * Base class for an abstract context
 */
@SuppressWarnings("deprecation")
abstract public class AbstractContext<X> implements EJBContext {
  private static final L10N L = new L10N(AbstractContext.class);
  private static final Logger log
    = Logger.getLogger(AbstractContext.class.getName());

  private boolean _isDead;
  private String []_declaredRoles;

  private Class<?> _invokedBusinessInterface;

  public void setDeclaredRoles(String[] roles)
  {
    _declaredRoles = roles;
  }
  
  /**
   * Returns true if the context is dead.
   */
  public boolean isDead()
  {
    return _isDead;
  }

  /**
   * Returns the server which owns this bean.
   */
  public abstract AbstractEjbBeanManager<X> getServer();

  /**
   * Returns the EJB's meta data.
   */
  public EJBMetaData getEJBMetaData()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the EJBLocalHome stub for the container.
   */
  @Override
  public EJBLocalHome getEJBLocalHome()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Looks up an object in the current JNDI context.
   */
  @Override
  public Object lookup(String name)
  {
    return getServer().lookup(name);
  }

  /**
   * Obsolete method which returns the EJB 1.0 environment.
   */
  @Override
  public Properties getEnvironment()
  {
    return new Properties();
  }
  
  @Override
  public final Map<String,Object> getContextData()
  {
    return CandiInvocationContext.getCurrentContextData();
  }

  /**
   * Obsolete method returns null.
   */
  @Override
  public Identity getCallerIdentity()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the principal
   */
  @Override
  public Principal getCallerPrincipal()
  {
    try {
      return SecurityContext.getUserPrincipal();
    } catch (SecurityContextException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Obsolete method returns false.
   */
  @Override
  public boolean isCallerInRole(Identity role)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the caller is in the named role.
   */
  @Override
  public boolean isCallerInRole(String roleName)
  {
    for (String role : _declaredRoles) {
      if (roleName.equals(role))
        return SecurityContext.isUserInRole(roleName);
    }
    
    return false;
  }

  /**
   * Returns the current UserTransaction. Only Session beans with bean-managed
   * transactions may use this.
   */
  @Override
  public UserTransaction getUserTransaction() throws IllegalStateException
  {
    // TCK uses this method in CMT
    /*
    if (getServer().isContainerTransaction())
      throw new IllegalStateException(
          "getUserTransaction() is not allowed with container-managed transaction");
          */

    return getServer().getUserTransaction();
  }

  /**
   * Looks the timer service.
   */
  @Override
  public TimerService getTimerService() throws IllegalStateException
  {
    return getServer().getTimerService();
  }

  /**
   * Forces a rollback of the current transaction.
   */
  @Override
  public void setRollbackOnly() 
    throws IllegalStateException
  {
    if (! getServer().isContainerTransaction()) {
      throw new IllegalStateException(L.l("setRollbackOnly() is only allowed with container-managed transaction"));
    }

    try {
      TransactionImpl xa = TransactionManagerImpl.getLocal().getCurrent();

      if (xa != null && xa.getStatus() != Status.STATUS_NO_TRANSACTION)
        xa.setRollbackOnly();
      else
        throw new IllegalStateException(L.l("setRollbackOnly() called with no active transaction."));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns true if the current transaction will rollback.
   */
  @Override
  public boolean getRollbackOnly() 
    throws IllegalStateException
  {
    if (! getServer().isContainerTransaction())
      throw new IllegalStateException(L.l("getRollbackOnly() is only allowed with container-managed transaction"));

    TransactionImpl xa = TransactionManagerImpl.getLocal().getCurrent();

    if (xa != null && xa.getStatus() != Status.STATUS_NO_TRANSACTION)
      return xa.isRollbackOnly();
    else
      throw new IllegalStateException(L.l("getRollbackOnly() called with no active transaction."));
  }

  /**
   * Destroy the context.
   */
  public void destroy() throws Exception
  {
    _isDead = true;
  }

  public Class<?> getInvokedBusinessInterface() 
    throws IllegalStateException
  {
    if (_invokedBusinessInterface == null)
      throw new IllegalStateException(L.l(
          "SessionContext.getInvokedBusinessInterface() is only allowed through EJB 3.0 interfaces"));

    return _invokedBusinessInterface;
  }

  public void __caucho_setInvokedBusinessInterface(Class<?> invokedBusinessInterface)
  {
    _invokedBusinessInterface = invokedBusinessInterface;
  }

  /**
   * Runs the timeout callbacks.
   */
  public void __caucho_timeout_callback(javax.ejb.Timer timer)
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }

  /**
   * Runs the timeout callbacks
   */
  public void __caucho_timeout_callback(Method method)
    throws IllegalAccessException, InvocationTargetException
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }

  /**
   * Runs the timeout callbacks.
   */
  public void __caucho_timeout_callback(Method method,
                                        javax.ejb.Timer timer)
    throws IllegalAccessException, InvocationTargetException
  {
    throw new IllegalStateException(L.l(
        "'{0}' does not have a @Timeout callback", getClass().getName()));
  }
}
