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

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.caucho.amber.manager.AmberPersistenceProvider;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Manages a single persistence unit
 */
public class PersistenceUnitManager implements PersistenceUnitInfo {
  private static final L10N L = new L10N(PersistenceUnitManager.class);
  private static final Logger log 
    = Logger.getLogger(PersistenceUnitManager.class.getName());
  
  private final PersistenceManager _persistenceManager;
  
  private final String _name;
  private URL _root;
  
  private String _version = "2.0";
  private Class<?> _providerClass;
  
  private ArrayList<String> _managedClasses = new ArrayList<String>();
  private boolean _isExcludeUnlistedClasses;
  
  private ArrayList<String> _mappingFiles = new ArrayList<String>();
  private ArrayList<URL> _jarFiles = new ArrayList<URL>();
  
  private PersistenceUnitTransactionType _transactionType
    = PersistenceUnitTransactionType.JTA;
  private SharedCacheMode _sharedCacheMode = SharedCacheMode.UNSPECIFIED;
  private ValidationMode _validationMode = ValidationMode.NONE;
  
  private String _jtaDataSourceName;
  private DataSource _jtaDataSourceValue;
  
  private String _nonJtaDataSourceName;
  private DataSource _nonJtaDataSourceValue;
  
  private Properties _properties = new Properties();
  
  private String _location;
  
  // the configuration program from the persistence-xml
  private ConfigProgram _persistenceXmlProgram;
  // override programs from resin-web.xml
  private ArrayList<ConfigProgram> _overridePrograms 
    = new ArrayList<ConfigProgram>();
  
  private final Lifecycle _lifecycle;
  
  private final EntityManagerFactoryProxy _entityManagerFactoryProxy;
  private final EntityManagerJtaProxy _entityManagerJtaProxy;
  
  private EntityManagerFactory _emfDelegate;
  
  PersistenceUnitManager(PersistenceManager manager, String name)
  {
    _persistenceManager = manager;
    
    _name = name;
    
    _lifecycle = new Lifecycle(log, "PersistenceUnit[" + name + "]");
    
    _entityManagerFactoryProxy = new EntityManagerFactoryProxy(this);
    _entityManagerJtaProxy = new EntityManagerJtaProxy(this);
  }
  
  public String getName()
  {
    return _name;
  }
  
  public void setConfigLocation(String location)
  {
    if (_location == null)
      _location = location;
  }
  
  /**
   * Sets the schema version.
   */
  public void setVersion(String version)
  {
    _version = version;
  }

  /**
   * The root URL of the persistence .jar or classes directory.
   */
  public URL getRoot()
  {
    return _root;
  }

  /**
   * The root URL of the persistence .jar or classes directory.
   */
  public void setRoot(URL url)
  {
    if (_root == null)
      _root = url;
    else if (! _root.equals(url))
      throw new ConfigException(L.l("persistence-unit '{0}' may not change its root URL"
                                    + " from '{1}' to '{2}'",
                                    _name, _root, url));
  }
  
  public void setDescription(String description)
  {
  }
  
  public Class<?> getProvider()
  {
    return _providerClass;
  }
  
  public void setProvider(Class<PersistenceProvider> cl)
  {
    Config.validate(cl, PersistenceProvider.class);
    
    _providerClass = cl;
  }
  
  public void addClass(String className)
  {
    _managedClasses.add(className);
  }

  public void setExcludeUnlistedClasses(boolean isExclude)
  {
    _isExcludeUnlistedClasses = isExclude;
  }
  
  public void addMappingFile(String fileName)
  {
    _mappingFiles.add(fileName);
  }
  
  public void addJarFile(String jarFile)
  {
    boolean isMatch = false;
    
    String classPath = Environment.getLocalClassPath();
    
    for (String pathName : classPath.split("[" + File.pathSeparatorChar + "]")) {
      if (pathName.endsWith(jarFile)) {
        Path path = Vfs.lookup(pathName);
        
        try {
          URL url = new URL(path.getURL());
          
          isMatch = true;
          
          if (! _jarFiles.contains(url))
            _jarFiles.add(url);
        } catch (Exception e) {
          throw ConfigException.create(e);
        }
      }
    }
    
    if (! isMatch) {
      throw new ConfigException(L.l("jar-file {0} was not found on the classpath.",
                                    jarFile));
    }
  }
  
  public void setTransactionType(String type)
  {
    if ("JTA".equals(type))
      _transactionType = PersistenceUnitTransactionType.JTA;
    else if ("RESOURCE_LOCAL".equals(type))
      _transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
    else
      throw new ConfigException(L.l("'{0}' is an unknown JPA transaction-type.",
                                    type));
  }
  
  public void setSharedCacheMode(String mode)
  {
    if ("ALL".equals(mode))
      _sharedCacheMode = SharedCacheMode.ALL;
    else if ("NONE".equals(mode))
      _sharedCacheMode = SharedCacheMode.NONE;
    else if ("ENABLE_SELECTIVE".equals(mode))
      _sharedCacheMode = SharedCacheMode.ENABLE_SELECTIVE;
    else if ("DISABLE_SELECTIVE".equals(mode))
      _sharedCacheMode = SharedCacheMode.DISABLE_SELECTIVE;
    else if ("UNSPECIFIED".equals(mode))
      _sharedCacheMode = SharedCacheMode.UNSPECIFIED;
    else
      throw new ConfigException(L.l("'{0}' is an unknown JPA shared-cache-mode.",
                                    mode));
  }
  
  public void setValidationMode(String mode)
  {
    if ("AUTO".equals(mode))
      _validationMode = ValidationMode.AUTO;
    else if ("CALLBACK".equals(mode))
      _validationMode = ValidationMode.CALLBACK;
    else if ("NONE".equals(mode))
      _validationMode = ValidationMode.NONE;
    else
      throw new ConfigException(L.l("'{0}' is an unknown JPA validation-mode.",
                                    mode));
  }
  
  public void setJtaDataSource(String name)
  {
    _jtaDataSourceName = name;
  }
  
  public void setJtaDataSourceValue(DataSource dataSource)
  {
    _jtaDataSourceValue = dataSource;
  }
  
  public void setNonJtaDataSource(String name)
  {
    _nonJtaDataSourceName = name;
  }
  
  public void setNonJtaDataSourceValue(DataSource dataSource)
  {
    _nonJtaDataSourceValue = dataSource;
  }
  
  public PropertiesConfig createProperties()
  {
    return new PropertiesConfig();
  }

  /**
   * Sets the persistence.xml program
   */
  void setPersistenceXmlProgram(ConfigProgram program)
  {
    _persistenceXmlProgram = program;
  }
  
  /**
   * Adds a resin-web.xml override program.
   */ 
  void addOverrideProgram(ConfigProgram program)
  {
    _overridePrograms.add(program);
  }

  /**
   * Create or return the provider's EntityManagerFactory
   */
  public EntityManagerFactory getEntityManagerFactoryDelegate()
  {
    start();
    
    return _emfDelegate;
  }
  
  /**
   * Returns the EntityManagerFactory proxy for this persistence unit.
   */
  EntityManagerFactory getEntityManagerFactoryProxy()
  {
    return _entityManagerFactoryProxy;
  }
  
  /**
   * Returns the EntityManager transactional proxy for this persistence unit.
   */
  EntityManager getEntityManagerJtaProxy()
  {
    return _entityManagerJtaProxy;
  }
  
  /**
   * Starts the persistence unit.
   */
  void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    // ConfigContext env = new ConfigContext();
    
    for (ConfigProgram program
           : _persistenceManager.getPersistenceUnitDefaults()) {
      program.configure(this);
    }
    
    if (_persistenceXmlProgram != null)
      _persistenceXmlProgram.configure(this);
    
    for (ConfigProgram program : _overridePrograms) {
      program.configure(this);
    }
    
    createDelegate();
  }
  
  private void addDefaultProperty(String name, String value)
  {
    if (_properties.get(name) == null)
      _properties.put(name, value);
  }
  
  private void createDelegate()
  {
    Class<?> cl = getProvider();

    if (cl == null)
      cl = _persistenceManager.getServiceProvider();

    if (cl == null)
      cl = AmberPersistenceProvider.class;
    
    addProviderDefaultProperties(cl);

    if (log.isLoggable(Level.CONFIG)) {
      log.config("JPA PersistenceUnit[" + getName() + "] handled by "
                 + cl.getName());
    }

    try {
      PersistenceProvider provider = (PersistenceProvider) cl.newInstance();

      HashMap<String,Object> map = null;
      
      _emfDelegate = provider.createContainerEntityManagerFactory(this, map);

      if (log.isLoggable(Level.FINE)) {
        log.fine("JPA PersistenceUnit[" + getName() + "] EMF delegate is "
                   + _emfDelegate);
      }
      
      if (_emfDelegate == null)
        throw new IllegalStateException(L.l("{0} did not return an EntityManagerFactory",
                                            provider));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Adds default properties for known providers
   */
  private void addProviderDefaultProperties(Class<?> cl)
  {
    if (cl == null)
      return;
    
    String className = cl.getName();

    if ("org.eclipse.persistence.jpa.PersistenceProvider".equals(className)) {
      addDefaultProperty("eclipselink.target-server",
                         "org.eclipse.persistence.platform.server.resin.ResinPlatform");
    }
    else if ("org.hibernate.ejb.HibernatePersistence".equals(className)) {
      addDefaultProperty("hibernate.transaction.manager_lookup_class",
                         "org.hibernate.transaction.ResinTransactionManagerLookup");
    }
  }
  
  boolean isOpen()
  {
    return _lifecycle.isActive();
  }
  
  void close()
  {
    if (! _lifecycle.toDestroy())
      return;
    
    _entityManagerFactoryProxy.closeImpl();
    
    EntityManagerFactory emfDelegate = _emfDelegate;
    _emfDelegate = null;
    
    if (emfDelegate != null)
      emfDelegate.close();
  }
  
  //
  // PersistenceUnitInfo API
  //

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
   */
  @Override
  public String getPersistenceUnitName()
  {
    return _name;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
   */
  @Override
  public String getPersistenceXMLSchemaVersion()
  {
    return _version;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()
   */
  @Override
  public URL getPersistenceUnitRootUrl()
  {
    return _root;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName()
   */
  @Override
  public String getPersistenceProviderClassName()
  {
    if (_providerClass != null)
      return _providerClass.getName();
    else
      return null;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getClassLoader()
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return _persistenceManager.getClassLoader();
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
   */
  @Override
  public List<String> getManagedClassNames()
  {
    return _managedClasses;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses()
   */
  @Override
  public boolean excludeUnlistedClasses()
  {
    return _isExcludeUnlistedClasses;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getJarFileUrls()
   */
  @Override
  public List<URL> getJarFileUrls()
  {
    return _jarFiles;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
   */
  @Override
  public List<String> getMappingFileNames()
  {
    return _mappingFiles;
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
   */
  @Override
  public SharedCacheMode getSharedCacheMode()
  {
    return _sharedCacheMode;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
   */
  @Override
  public PersistenceUnitTransactionType getTransactionType()
  {
    return _transactionType;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
   */
  @Override
  public ValidationMode getValidationMode()
  {
    return _validationMode;
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
   */
  @Override
  public DataSource getJtaDataSource()
  {
    if (_jtaDataSourceValue != null)
      return _jtaDataSourceValue;
    else if (_jtaDataSourceName != null) {
      _jtaDataSourceValue = loadDataSource(_jtaDataSourceName);
      
      return _jtaDataSourceValue;
    }
    else
      return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
   */
  @Override
  public DataSource getNonJtaDataSource()
  {
    if (_nonJtaDataSourceValue != null)
      return _nonJtaDataSourceValue;
    else if (_nonJtaDataSourceName != null) {
      _nonJtaDataSourceValue = loadDataSource(_nonJtaDataSourceName);
      
      return _nonJtaDataSourceValue;
    }
    else
      return null;
  }

  /*
   * @see javax.persistence.spi.PersistenceUnitInfo#getProperties()
   */
  @Override
  public Properties getProperties()
  {
    return _properties;
  }

  /**
   * Adds a class transformer.
   */
  @Override
  public void addTransformer(ClassTransformer transformer)
  {
    EnvironmentClassLoader loader = _persistenceManager.getClassLoader();

    loader.addTransformer(new TransformerAdapter(transformer));
  }

  /**
   * Returns a temporary class loader.
   */
  @Override
  public ClassLoader getNewTempClassLoader()
  {
    EnvironmentClassLoader loader = _persistenceManager.getClassLoader();
    
    return loader.getNewTempClassLoader();
  }
  
  private DataSource loadDataSource(String name)
  {
    if (name == null)
      return null;
    
    Named named = Names.create(name);
    InjectManager beanManager = InjectManager.create();
    
    Set<Bean<?>> beans = beanManager.getBeans(DataSource.class, named);
    
    if (beans != null && beans.size() > 0) {
      return (DataSource) beanManager.getReference(beanManager.resolve(beans));
    }
    
    DataSource ds = (DataSource) Jndi.lookup(name);

    if (ds != null)
      return ds;

    throw new ConfigException(L.l("'{0}' is an unknown or unconfigured JDBC DataSource.",
                                  name));
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "," + _emfDelegate + "]";
  }

  public class PropertiesConfig {
    public void addProperty(PropertyConfig prop)
    {
      _properties.put(prop.getName(), prop.getValue());
    }
  }

  public static class PropertyConfig {
    private String _name;
    private String _value;
    
    public void setName(String name)
    {
      _name = name;
    }
    
    public String getName()
    {
      return _name;
    }

    public void setValue(String value)
    {
      _value = value;
    }

    public String getValue()
    {
      return _value;
    }
  }
  

  public static class TransformerAdapter implements ClassFileTransformer {
    private ClassTransformer _transformer;

    TransformerAdapter(ClassTransformer transformer)
    {
      _transformer = transformer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class redefineClass,
                            ProtectionDomain domain,
                            byte []classFileBuffer)
      throws IllegalClassFormatException
    {
      return _transformer.transform(loader,
                                    className,
                                    redefineClass,
                                    domain,
                                    classFileBuffer);
    }
  }
}
