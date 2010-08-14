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

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * A special persistence provider which will disable a persistence-unit.
 * Can be used when a persistence-unit should be enabled explicitly.
 */
public class DisabledPersistenceProvider
  implements PersistenceProvider
{
  @SuppressWarnings("unchecked")
  @Override
  public EntityManagerFactory 
  createContainerEntityManagerFactory(PersistenceUnitInfo info,
                                      Map map)
  {
    return new DisabledEntityManagerFactory(info.getPersistenceUnitName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public EntityManagerFactory createEntityManagerFactory(String name, Map map)
  {
    return new DisabledEntityManagerFactory(name);
  }

  @Override
  public ProviderUtil getProviderUtil()
  {
    return null;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }

  static class DisabledEntityManagerFactory implements EntityManagerFactory {
    private String _name;
    
    DisabledEntityManagerFactory(String name)
    {
      _name = name;
    }

    @Override
    public void close()
    {
    }

    @Override
    public EntityManager createEntityManager()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityManager createEntityManager(Map map)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Cache getCache()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Metamodel getMetamodel()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil()
    {
      return null;
    }

    @Override
    public Map<String, Object> getProperties()
    {
      return null;
    }

    @Override
    public boolean isOpen()
    {
      return false;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "]";
    }
  }
}
