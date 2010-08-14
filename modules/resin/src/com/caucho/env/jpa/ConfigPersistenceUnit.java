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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.naming.Jndi;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class ConfigPersistenceUnit {
  private String _name;
  private String _version;
  private String _description;
  private Class<?> _provider;
  private String _jtaDataSourceName;
  private String _nonJtaDataSourceName;
  private DataSource _jtaDataSource;
  private DataSource _nonJtaDataSource;
  private boolean _isExcludeUnlistedClasses;

  private URL _rootUrl;
  private DynamicClassLoader _loader;
  
  private ContainerProgram _program = new ContainerProgram();

  private PersistenceUnitTransactionType _transactionType
    = PersistenceUnitTransactionType.JTA;

  private Properties _properties = new Properties();

  // className -> type
  private HashMap<String,Class<?>> _classMap
    = new HashMap<String,Class<?>>();

  private ArrayList<String> _mappingFiles
    = new ArrayList<String>();

  private ArrayList<String> _jarFiles
    = new ArrayList<String>();

  private ArrayList<URL> _jarFileUrls
    = new ArrayList<URL>();

  public ConfigPersistenceUnit(URL rootUrl)
  {
    _rootUrl = rootUrl;
  }

  /**
   * Returns the unit name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the unit name.
   */
  public void setName(String name)
  {
    _name = name;
  }
  
  public URL getRoot()
  {
    return _rootUrl;
  }

  public void setVersion(String version)
  {
    _version = version;
  }
  
  public String getVersion()
  {
    return _version;
  }
  /**
   * Sets the description.
   */
  /*
  public void setDescription(String description)
  {
    _description = description;
  }
  */

  /**
   * Sets the provider class name.
   */
  /*
  public void setProvider(Class<?> provider)
  {
    _provider = provider;

    Config.validate(provider, PersistenceProvider.class);
  }
  */

  /**
   * Sets the provider class name.
   */
  public Class<?> getProvider()
  {
    return _provider;
  }

  /**
   * Sets the transactional data source.
   */
  /*
  public void setJtaDataSource(String ds)
  {
    _jtaDataSourceName = ds;
  }
  */

  /**
   * Gets the transactional data source.
   */
  public DataSource getJtaDataSource()
  {
    if (_jtaDataSourceName == null)
      return null;
    
    if (_jtaDataSource == null)
      _jtaDataSource = loadDataSource(_jtaDataSourceName);
    
    return _jtaDataSource;
  }

  /**
   * Gets the transactional data source.
   */
  public String getJtaDataSourceName()
  {
    return _jtaDataSourceName;
  }

  /**
   * Sets the non-transactional data source.
   */
  /*
  public void setNonJtaDataSource(String ds)
  {
    _nonJtaDataSourceName = ds;
  }
  */

  /**
   * Sets the non-transactional data source.
   */
  public DataSource getNonJtaDataSource()
  {
    if (_nonJtaDataSourceName == null)
      return null;
    
    if (_nonJtaDataSource == null)
      _nonJtaDataSource = loadDataSource(_nonJtaDataSourceName);
    
    return _nonJtaDataSource;
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  ContainerProgram getProgram()
  {
    return _program;
  }

  @PostConstruct
  public void init()
  {
  }
  
  protected DataSource loadDataSource(String name)
  {
    DataSource ds = (DataSource) Jndi.lookup(name);

    if (ds != null)
      return ds;

    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
