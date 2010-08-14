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

package com.caucho.env.jpa;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;

/**
 * The @PersistenceUnit, container managed entity manager proxy, used
 * for third-party providers.
 */
@SuppressWarnings("serial")
public class EntityManagerFactoryProxy
  implements EntityManagerFactory, Serializable
{
  private transient final PersistenceUnitManager _persistenceUnit;
  
  private transient EntityManagerFactory _emfDelegate;

  public EntityManagerFactoryProxy(PersistenceUnitManager persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
  }

  /**
   * Create a new EntityManager with TRANSACTION type.
   */
  @Override
  public EntityManager createEntityManager()
  {
    return getDelegate().createEntityManager();
  }

  /**
   * Create a new EntityManager with the given properties.
   */
  @SuppressWarnings("unchecked")
  @Override
  public EntityManager createEntityManager(Map map)
  {
    return getDelegate().createEntityManager(map);
  }

  /**
   * Close the factory an any resources.
   */
  @Override
  public void close()
  {
  }
  
  void closeImpl()
  {
    _emfDelegate = null;
  }

  /**
   * Returns true if the factory is open.
   */
  @Override
  public boolean isOpen()
  {
    return _persistenceUnit.isOpen();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map getProperties()
  {
    return getDelegate().getProperties();
  }

  @Override
  public Cache getCache()
  {
    return getDelegate().getCache();
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder()
  {
    return getDelegate().getCriteriaBuilder();
  }

  @Override
  public Metamodel getMetamodel()
  {
    return getDelegate().getMetamodel();
  }

  @Override
  public PersistenceUnitUtil getPersistenceUnitUtil()
  {
    return getDelegate().getPersistenceUnitUtil();
  }

  private EntityManagerFactory getDelegate()
  {
    if (_emfDelegate == null)
      _emfDelegate = _persistenceUnit.getEntityManagerFactoryDelegate();

    return _emfDelegate;
  }
  
  private Object writeReplace()
  {
    return new Handle(_persistenceUnit.getName());
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _persistenceUnit.getName()
            + "," + _emfDelegate + "]");
  }
  
  public static class Handle implements Serializable {
    private String _name;
    
    private Handle()
    {
    }
    
    private Handle(String name)
    {
      _name = name;
    }
    
    private Object readResolve()
    {
      InjectManager beanManager = InjectManager.getCurrent();
      
      return beanManager.getReference(EntityManagerFactory.class, 
                                      Names.create(_name));
    }
  }
}
