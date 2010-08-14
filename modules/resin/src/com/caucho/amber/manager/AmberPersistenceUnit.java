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

package com.caucho.amber.manager;

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.AmberConfigManager;
import com.caucho.amber.cfg.EmbeddableIntrospector;
import com.caucho.amber.cfg.EntityMappingsConfig;
import com.caucho.amber.cfg.MappedSuperIntrospector;
import com.caucho.amber.cfg.NamedNativeQueryConfig;
import com.caucho.amber.cfg.SqlResultSetMappingConfig;
import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.EntityKey;
import com.caucho.amber.entity.Listener;
import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.gen.AmberGeneratorImpl;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main interface between Resin and the connector.  It's the
 * top-level SPI class for creating the SPI ManagedConnections.
 *
 * The resource configuration in Resin's web.xml will use bean-style
 * configuration to configure the ManagecConnectionFactory.
 */
public class AmberPersistenceUnit {
  private static final L10N L = new L10N(AmberPersistenceUnit.class);
  private static final Logger log
    = Logger.getLogger(AmberPersistenceUnit.class.getName());

  private String _name;

  private boolean _isJPA;

  private AmberContainer _amberContainer;

  // Actual class is EntityManagerProxy, but EntityManager is JDK 1.5 dependent
  private Object _entityManagerProxy;

  // basic data source
  private DataSource _dataSource;

  // data source for read-only requests
  private DataSource _readDataSource;

  // data source for requests in a transaction
  private DataSource _xaDataSource;

  private String _jtaDataSourceName;
  private String _nonJtaDataSourceName;

  // persistence.xml jta-data-source
  private DataSource _jtaDataSource;
  // persistence.xml non-jta-data-source
  private DataSource _nonJtaDataSource;

  private JdbcMetaData _jdbcMetaData;

  private Boolean _createDatabaseTables;
  private boolean _validateDatabaseTables = true;

  // private long _tableCacheTimeout = 250;
  private long _tableCacheTimeout = 2000;

  private TypeManager _typeManager = new TypeManager();

  // loader override for ejb
  private ClassLoader _enhancedLoader;

  private HashMap<String,AmberTable> _tableMap
    = new HashMap<String,AmberTable>();

  private HashMap<String,AmberEntityHome> _entityHomeMap
    = new HashMap<String,AmberEntityHome>();

  private HashMap<String,IdGenerator> _tableGenMap
    = new HashMap<String,IdGenerator>();

  private HashMap<String,SequenceIdGenerator> _sequenceGenMap
    = new HashMap<String,SequenceIdGenerator>();

  private LruCache<String,AbstractQuery> _queryParseCache
    = new LruCache<String,AbstractQuery>(1024);

  private LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>> _queryCache
    = new LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>>(1024);

  private LruCache<QueryCacheKey,SoftReference<ResultSetMetaData>> _queryCacheMetaData
    = new LruCache<QueryCacheKey,SoftReference<ResultSetMetaData>>(16);

  private LruCache<EntityKey,SoftReference<EntityItem>> _entityCache
    = new LruCache<EntityKey,SoftReference<EntityItem>>(32 * 1024);

  private EntityKey _entityKey = new EntityKey();

  private ArrayList<EntityType> _lazyConfigure = new ArrayList<EntityType>();

  private ArrayList<EntityType> _lazyGenerate = new ArrayList<EntityType>();
  private ArrayList<AmberEntityHome> _lazyHomeInit
    = new ArrayList<AmberEntityHome>();
  private ArrayList<AmberTable> _lazyTable = new ArrayList<AmberTable>();

  private HashMap<String,String> _namedQueryMap
    = new HashMap<String,String>();

  private HashMap<String, SqlResultSetMappingConfig> _sqlResultSetMap
    = new HashMap<String, SqlResultSetMappingConfig>();

  private HashMap<String, NamedNativeQueryConfig> _namedNativeQueryMap
    = new HashMap<String, NamedNativeQueryConfig>();

  private ArrayList<EntityMappingsConfig> _entityMappingsList;

  private ArrayList<EmbeddableType> _embeddableTypes
    = new ArrayList<EmbeddableType>();

  private ArrayList<MappedSuperclassType> _mappedSuperclassTypes
    = new ArrayList<MappedSuperclassType>();

  private ArrayList<ListenerType> _defaultListeners
    = new ArrayList<ListenerType>();

  private AmberConfigManager _configManager;

  private EmbeddableIntrospector _embeddableIntrospector;
  private MappedSuperIntrospector _mappedSuperIntrospector;

  private AmberGenerator _generator;

  // private boolean _supportsGetGeneratedKeys;

  private ThreadLocal<AmberConnection> _threadConnection
    = new ThreadLocal<AmberConnection>();

  private volatile boolean _isInit;

  private long _xid = 1;

  public AmberPersistenceUnit(AmberContainer container,
                              String name)
  {
    _amberContainer = container;
    _name = name;

    _dataSource = container.getDataSource();
    _xaDataSource = container.getXADataSource();
    _readDataSource = container.getReadDataSource();

    _configManager = new AmberConfigManager(this);

    // needed to support JDK 1.4 compatibility
    try {
      bindProxy();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  private void bindProxy()
    throws Exception
  {
    /*
    String name = getName();

    // XXX: is "default" appropriate?
    // jpa/0s2m <=> tck: ejb30/persistence/ee/packaging/ejb/resource_local/test1
    if (name == null || "".equals(name))
      name = "default";

    InjectManager manager
      = InjectManager.create(_amberContainer.getParentClassLoader());

    EntityManagerFactory factory = new AmberEntityManagerFactory(this);

    BeanFactory beanFactory
      = manager.createBeanFactory(EntityManagerFactory.class);

    beanFactory.binding(Names.create(name));
    manager.addBean(beanFactory.singleton(factory));

    beanFactory = manager.createBeanFactory(EntityManager.class);
    beanFactory.binding(Names.create(name));

    manager.addBean(beanFactory.singleton(new EntityManagerProxy(this)));

    String jndiName
      = AmberContainer.getPersistenceContextJndiPrefix() + '/' + name;

    Jndi.bindDeepShort(jndiName, factory);
    */
  }

  public EntityManager getEntityManager()
  {
    // return (EntityManager) _entityManagerProxy;

    return null;
  }

  public AmberContainer getAmberContainer()
  {
    return _amberContainer;
  }

  public ClassLoader getTempClassLoader()
  {
    return _amberContainer.getTempClassLoader();
  }

  public ClassLoader getEnhancedLoader()
  {
    if (_enhancedLoader != null)
      return _enhancedLoader;
    else
      return _amberContainer.getEnhancedLoader();
  }

  /**
   * EJB/CMP needs to set a special enhanced loader.
   */
  public void setEnhancedLoader(ClassLoader loader)
  {
    _enhancedLoader = loader;
  }

  /**
   * Sets the data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the data source.
   */
  public DataSource getDataSource()
  {
    if (_jtaDataSource != null)
      return _jtaDataSource;
    else if (_nonJtaDataSource != null)
      return _nonJtaDataSource;
    else if (_dataSource != null)
      return _dataSource;
    else {
      return _amberContainer.getDataSource();
    }
  }

  /**
   * Sets the read data source.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _readDataSource = dataSource;
  }

  /**
   * Gets the read data source.
   */
  public DataSource getReadDataSource()
  {
    return _readDataSource;
  }

  /**
   * Sets the XA data source.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _xaDataSource = dataSource;
  }

  /**
   * Gets the xa data source.
   */
  public DataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * Sets the persistence.xml jta data source.
   */
  public void setJtaDataSourceName(String name)
  {
    _jtaDataSourceName = name;
  }

  /**
   * Sets the persistence.xml non-jta data source.
   */
  public void setNonJtaDataSourceName(String name)
  {
    _nonJtaDataSourceName = name;
  }

  /**
   * Sets the persistence.xml jta data source.
   */
  public void setJtaDataSource(DataSource dataSource)
  {
    _jtaDataSource = dataSource;
  }

  /**
   * Sets the persistence.xml non-jta data source.
   */
  public void setNonJtaDataSource(DataSource dataSource)
  {
    _nonJtaDataSource = dataSource;
  }

  /**
   * Return true for a jta-managed persistence unit
   */
  public boolean isJta()
  {
    return _nonJtaDataSource == null || _jtaDataSource != null;
  }

  /**
   * Returns the jdbc meta data.
   */
  public JdbcMetaData getMetaData()
  {
    if (_jdbcMetaData == null) {
      if (getDataSource() == null)
        throw new ConfigException("No data-source specified for PersistenceUnit");

      _jdbcMetaData = JdbcMetaData.create(getDataSource());
    }

    return _jdbcMetaData;
  }

  /**
   * Set true if database tables should be created automatically.
   */
  public void setCreateDatabaseTables(boolean create)
  {
    _createDatabaseTables = create;
  }

  /**
   * Set true if database tables should be created automatically.
   */
  public boolean getCreateDatabaseTables()
  {
    if (_createDatabaseTables != null)
      return _createDatabaseTables;
    else
      return _amberContainer.getCreateDatabaseTables();
  }

  /**
   * Set true if database tables should be validated automatically.
   */
  public void setValidateDatabaseTables(boolean validate)
  {
    _validateDatabaseTables = validate;
  }

  /**
   * Set true if database tables should be validated automatically.
   */
  public boolean getValidateDatabaseTables()
  {
    return _validateDatabaseTables;
  }

  /**
   * Set the default table cache time.
   */
  public void setTableCacheTimeout(long timeout)
  {
    _tableCacheTimeout = timeout;
  }

  /**
   * Get the default table cache time.
   */
  public long getTableCacheTimeout()
  {
    return _tableCacheTimeout;
  }

  /**
   * Set false for EJB-style generation.
   */
  public void setBytecodeGenerator(boolean isBytecodeGenerator)
  {
    if (isBytecodeGenerator)
      _generator = _amberContainer.getGenerator();
    else
      _generator = new AmberGeneratorImpl(this);
  }

  /**
   * Returns a new xid.
   */
  public long getXid()
  {
    synchronized (this) {
      return _xid++;
    }
  }

  public Class loadTempClass(String className)
  {
    try {
      return Class.forName(className, false, getTempClassLoader());
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Creates a table.
   */
  public AmberTable createTable(String tableName)
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "AmberPersistenceUnit.createTable: " + tableName);

    AmberTable table = _tableMap.get(tableName);

    if (table == null) {
      table = new AmberTable(this, tableName);
      table.setCacheTimeout(getTableCacheTimeout());

      _tableMap.put(tableName, table);

      _lazyTable.add(table);
    }

    return table;
  }

  public Throwable getConfigException()
  {
    return _amberContainer.getConfigException();
  }

  /**
   * Add an entity.
   *
   * @param className the class name
   * @param type the JClass type if it is already verified as an
   *             Entity | Embeddable | MappedSuperclass
   */
  public void addEntityClass(String className,
                             Class type)
    throws ConfigException
  {
    if (type == null) {
      type = loadTempClass(className);

      if (type == null) {
        throw new ConfigException(L.l("'{0}' is an unknown type",
                                      className));
      }
    }

    BeanType beanType = _configManager.introspect(type);

    if (beanType instanceof EntityType)
      _amberContainer.addEntity(className, (EntityType) beanType);
  }

  /**
   * Adds a sql result set mapping.
   */
  public void addSqlResultSetMapping(String resultSetName,
                                     SqlResultSetMappingConfig resultSet)
    throws ConfigException
  {
    if (_sqlResultSetMap.containsKey(resultSetName)) {
      throw new ConfigException(L.l("SqlResultSetMapping '{0}' is already defined.",
                                    resultSetName));
    }

    _sqlResultSetMap.put(resultSetName, resultSet);
  }

  /**
   * Returns the sql result set mapping.
   */
  public SqlResultSetMappingConfig getSqlResultSetMapping(String resultSetName)
  {
    return _sqlResultSetMap.get(resultSetName);
  }

  /**
   * Adds a named query.
   */
  public void addNamedQuery(String name,
                            String query)
    throws ConfigException
  {
    if (_namedQueryMap.containsKey(name)) {
      throw new ConfigException(L.l("Named query '{0}': '{1}' is already defined.",
                                    name, query));
    }

    _namedQueryMap.put(name, query);
  }

  /**
   * Returns the named query statement.
   */
  public String getNamedQuery(String name)
  {
    return (String) _namedQueryMap.get(name);
  }

  /**
   * Adds a named native query.
   */
  public void addNamedNativeQuery(String name,
                                  NamedNativeQueryConfig queryConfig)
    throws ConfigException
  {
    if (_namedNativeQueryMap.containsKey(name)) {
      throw new ConfigException(L.l("NamedNativeQuery '{0}' is already defined.",
                                    name));
    }

    _namedNativeQueryMap.put(name, queryConfig);
  }

  /**
   * Returns the named native query.
   */
  public NamedNativeQueryConfig getNamedNativeQuery(String name)
  {
    return _namedNativeQueryMap.get(name);
  }

  /**
   * Adds an entity.
   */
  public EntityType createEntity(Class beanClass)
  {
    return createEntity(beanClass.getName(), beanClass);
  }

  /**
   * Adds an entity.
   */
  public EntityType createEntity(String name,
                                 Class beanClass)
  {
    EntityType entityType = (EntityType) _typeManager.get(name);

    if (entityType != null)
      return entityType;

    // ejb/0al2
    // entityType = (EntityType) _typeManager.get(beanClass.getName());

    if (entityType == null) {
      // The parent type can be a @MappedSuperclass or an @EntityType.
      EntityType parentType = null;

      for (Class parentClass = beanClass.getSuperclass();
           parentType == null && parentClass != null;
           parentClass = parentClass.getSuperclass()) {
        parentType = (EntityType) _typeManager.get(parentClass.getName());
      }

      if (parentType != null)
        entityType = new SubEntityType(this, parentType);
      else
        entityType = new EntityType(this);
    }

    // _typeManager.put(name, entityType);
    _typeManager.put(name, entityType);
    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), entityType);

    entityType.setName(name);
    entityType.setBeanClass(beanClass);

    _lazyConfigure.add(entityType);
    // getEnvManager().addLazyConfigure(entityType);

    AmberEntityHome entityHome = _entityHomeMap.get(beanClass.getName());

    if (entityHome == null) {
      entityHome = new AmberEntityHome(this, entityType);
      _lazyHomeInit.add(entityHome);
      _isInit = false;
    }

    addEntityHome(name, entityHome);
    // XXX: some confusion about the double entry, related to the EJB 3.0
    // confuction of named instances.
    addEntityHome(beanClass.getName(), entityHome);

    return entityType;
  }

  /**
   * Adds an entity.
   */
  public MappedSuperclassType createMappedSuperclass(String name,
                                                     Class beanClass)
  {
    MappedSuperclassType mappedSuperType
      = (MappedSuperclassType) _typeManager.get(name);

    if (mappedSuperType != null)
      return mappedSuperType;

    mappedSuperType = new MappedSuperclassType(this);

    _typeManager.put(name, mappedSuperType);
    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), mappedSuperType);

    _mappedSuperclassTypes.add(mappedSuperType);

    mappedSuperType.setName(name);
    mappedSuperType.setBeanClass(beanClass);

    return mappedSuperType;
  }

  /**
   * Adds an embeddable type.
   */
  public EmbeddableType createEmbeddable(Class beanClass)
  {
    return createEmbeddable(beanClass.getName(), beanClass);
  }

  /**
   * Adds an embeddable type.
   */
  public EmbeddableType createEmbeddable(String name,
                                         Class beanClass)
  {
    AmberType type = _typeManager.get(name);

    if (type != null && ! (type instanceof EmbeddableType))
      throw new ConfigException(L.l("'{0}' is not a valid embeddable type",
                                    name));

    EmbeddableType embeddableType;

    embeddableType = (EmbeddableType) type;

    if (embeddableType != null)
      return embeddableType;

    embeddableType = new EmbeddableType(this);

    _typeManager.put(name, embeddableType);

    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), embeddableType);

    embeddableType.setName(name);
    embeddableType.setBeanClass(beanClass);

    _embeddableTypes.add(embeddableType);

    _amberContainer.addEmbeddable(beanClass.getName(), embeddableType);

    return embeddableType;
  }

  /**
   * Adds an enumerated type.
   */
  public EnumType createEnum(String name,
                             Class beanClass)
  {
    EnumType enumType = (EnumType) _typeManager.get(name);

    if (enumType != null)
      return enumType;

    enumType = new EnumType();

    _typeManager.put(name, enumType);

    // XXX: some confusion about the double entry
    if (_typeManager.get(beanClass.getName()) == null)
      _typeManager.put(beanClass.getName(), enumType);

    enumType.setName(name);
    enumType.setBeanClass(beanClass);

    return enumType;
  }

  /**
   * Gets a default listener.
   */
  public ListenerType getDefaultListener(String className)
  {
    return _amberContainer.getDefaultListener(className);
  }

  /**
   * Adds a default listener.
   */
  public ListenerType addDefaultListener(Class beanClass)
  {
    ListenerType listenerType = getListener(beanClass);

    if (! _defaultListeners.contains(listenerType)) {
      _defaultListeners.add(listenerType);

      _amberContainer.addDefaultListener(beanClass.getName(),
                                         listenerType);
    }

    return listenerType;
  }

  /**
   * Gets an entity listener.
   */
  public ListenerType getEntityListener(String className)
  {
    return _amberContainer.getEntityListener(className);
  }

  /**
   * Adds an entity listener.
   */
  public ListenerType addEntityListener(String entityName,
                                        Class listenerClass)
  {
    ListenerType listenerType = getListener(listenerClass);

    _amberContainer.addEntityListener(entityName,
                                      listenerType);

    return listenerType;
  }

  private ListenerType getListener(Class beanClass)
  {
    String name = beanClass.getName();

    ListenerType listenerType = (ListenerType) _typeManager.get(name);

    if (listenerType != null)
      return listenerType;

    listenerType = new ListenerType(this);

    ListenerType parentType = null;

    for (Class parentClass = beanClass.getSuperclass();
         parentType == null && parentClass != null;
         parentClass = parentClass.getSuperclass()) {
      parentType = (ListenerType) _typeManager.get(parentClass.getName());
    }

    if (parentType != null)
      listenerType = new SubListenerType(this, parentType);
    else
      listenerType = new ListenerType(this);

    _typeManager.put(name, listenerType);

    listenerType.setName(name);
    listenerType.setBeanClass(beanClass);

    return listenerType;
  }

  /**
   * Adds a new home bean.
   */
  private void addEntityHome(String name, AmberEntityHome home)
  {
    _entityHomeMap.put(name, home);

    // getEnvManager().addEntityHome(name, home);
  }

  /**
   * Returns a table generator.
   */
  public IdGenerator getTableGenerator(String name)
  {
    return _tableGenMap.get(name);
  }

  /**
   * Sets a table generator.
   */
  public IdGenerator putTableGenerator(String name, IdGenerator gen)
  {
    synchronized (_tableGenMap) {
      IdGenerator oldGen = _tableGenMap.get(name);

      if (oldGen != null)
        return oldGen;
      else {
        _tableGenMap.put(name, gen);
        return gen;
      }
    }
  }

  /**
   * Adds a generator table.
   */
  public GeneratorTableType createGeneratorTable(String name)
  {
    AmberType type = _typeManager.get(name);

    if (type instanceof GeneratorTableType)
      return (GeneratorTableType) type;

    if (type != null)
      throw new RuntimeException(L.l("'{0}' is a duplicate generator table.",
                                     type));

    GeneratorTableType genType = new GeneratorTableType(this, name);

    _typeManager.put(name, genType);

    // _lazyGenerate.add(genType);

    return genType;
  }

  /**
   * Returns a sequence generator.
   */
  public SequenceIdGenerator createSequenceGenerator(String name, int size)
    throws ConfigException
  {
    synchronized (_sequenceGenMap) {
      SequenceIdGenerator gen = _sequenceGenMap.get(name);

      if (gen == null) {
        gen = new SequenceIdGenerator(this, name, size);

        _sequenceGenMap.put(name, gen);
      }

      return gen;
    }
  }

  /**
   * Configures a type.
   */
  public void initType(AbstractEnhancedType type)
    throws Exception
  {
    type.init();

    getGenerator().generate(type);
  }

  /**
   * Configure lazy.
   */
  public void generate()
    throws Exception
  {
    configure();

    AbstractEnhancedType type = null;

    try {
      for (MappedSuperclassType mappedType : _mappedSuperclassTypes) {
        type = mappedType;

        initType(mappedType);
      }

      while (_lazyGenerate.size() > 0) {
        EntityType entityType = _lazyGenerate.remove(0);

        type = entityType;

        // Entity
        initType(entityType);

        ArrayList<ListenerType> listeners;

        String className = entityType.getBeanClass().getName();

        listeners = _amberContainer.getEntityListeners(className);

        if (listeners == null)
          continue;

        // Entity Listeners
        for (ListenerType listenerType : listeners) {
          type = listenerType;

          initType(listenerType);
        }
      }

      // Embeddable
      for (EmbeddableType embeddableType : _embeddableTypes) {
        type = embeddableType;

        initType(embeddableType);
      }

      // Default Listeners
      for (ListenerType listenerType : _defaultListeners) {
        type = listenerType;

        initType(listenerType);
      }
    } catch (Exception e) {
      if (type != null) {
        type.setConfigException(e);

        _amberContainer.addEntityException(type.getBeanClass().getName(), e);
      }

      throw e;
    }

    try {
      getGenerator().compile();
    } catch (Exception e) {
      _amberContainer.addException(e);

      throw e;
    }
  }

  /**
   * Gets the JPA flag.
   */
  public boolean isJPA()
  {
    return _isJPA;
  }

  /**
   * Sets the JPA flag.
   */
  public void setJPA(boolean isJPA)
  {
    _isJPA = isJPA;
  }

  /**
   * Configure lazy.
   */
  public void generate(JavaClassGenerator javaGen)
    throws Exception
  {
    configure();

    while (_lazyGenerate.size() > 0) {
      EntityType type = _lazyGenerate.remove(0);

      type.init();

      if (type instanceof EntityType) {
        EntityType entityType = (EntityType) type;

        if (! entityType.isGenerated()) {
          if (entityType.getInstanceClassName() == null)
            throw new ConfigException(L.l("'{0}' does not have a configured instance class.",
                                          entityType));

          entityType.setGenerated(true);

          try {
            getGenerator().generateJava(javaGen, entityType);
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }

      configure();
    }

    for (EmbeddableType embeddableType : _embeddableTypes) {

      embeddableType.init();

      if (! embeddableType.isGenerated()) {
        if (embeddableType.getInstanceClassName() == null)
          throw new ConfigException(L.l("'{0}' does not have a configured instance class.",
                                        embeddableType));

        embeddableType.setGenerated(true);

        try {
          getGenerator().generateJava(javaGen, embeddableType);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }

    for (SequenceIdGenerator gen : _sequenceGenMap.values())
      gen.init(this);

    while (_defaultListeners.size() > 0) {
      ListenerType type = _defaultListeners.remove(0);

      type.init();

      if (! type.isGenerated()) {
        if (type.getInstanceClassName() == null)
          throw new ConfigException(L.l("'{0}' does not have a configured instance class.",
                                        type));

        type.setGenerated(true);

        try {
          getGenerator().generateJava(javaGen, type);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }
  }

  /**
   * Returns the @Embeddable introspector.
   */
  public EmbeddableIntrospector getEmbeddableIntrospector()
  {
    return _embeddableIntrospector;
  }

  /**
   * Configure lazy.
   */
  public void configure()
    throws Exception
  {
    //_embeddableIntrospector.configure();
    //_mappedSuperIntrospector.configure();

    _configManager.configure();

    while (_lazyConfigure.size() > 0) {
      EntityType type = _lazyConfigure.remove(0);

      if (type.startConfigure()) {
        // getEnvManager().getGenerator().configure(type);
      }

      _configManager.configure();

      if (! _lazyGenerate.contains(type))
        _lazyGenerate.add(type);
    }

    updateFlushPriority();
  }

  /**
   * Returns the entity home.
   */
  public AmberEntityHome getEntityHome(String name)
  {
    if (! _isInit) {
      try {
        initEntityHomes();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new AmberRuntimeException(e);
      }
    }

    return _entityHomeMap.get(name);
  }

  /**
   * Returns the entity home by the schema name.
   */
  public AmberEntityHome getHomeBySchema(String name)
  {
    for (AmberEntityHome home : _entityHomeMap.values()) {
      if (name.equals(home.getEntityType().getName()))
        return home;
    }

    try {
      createType(name);
    } catch (Exception e) {
    }

    return _entityHomeMap.get(name);
  }

  /**
   * Returns a matching embeddable type.
   */
  public EmbeddableType getEmbeddable(String className)
  {
    AmberType type = _typeManager.get(className);

    if (type instanceof EmbeddableType)
      return (EmbeddableType) type;
    else
      return null;
  }

  public EntityType getEntityType(Class cl)
  {
    return getEntityType(cl.getName());
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntityType(String className)
  {
    AmberType type = _typeManager.get(className);

    if (type instanceof EntityType)
      return (EntityType) type;
    else
      return null;
  }

  /**
   * Returns a matching mapped superclass.
   */
  public MappedSuperclassType getMappedSuperclass(String className)
  {
    AmberType type = _typeManager.get(className);

    if (type instanceof MappedSuperclassType)
      return (MappedSuperclassType) type;

    return null;
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntityByInstanceClass(String className)
  {
    return _typeManager.getEntityByInstanceClass(className);
  }

  /**
   * Updates global entity priorities for flushing.
   */
  public void updateFlushPriority()
  {
    ArrayList<EntityType> updatingEntities
      = new ArrayList<EntityType>();

    try {
      HashMap<String,AmberType> typeMap = _typeManager.getTypeMap();

      Collection<AmberType> types = typeMap.values();

      Iterator it = types.iterator();

      while (it.hasNext()) {
        AmberType type = (AmberType) it.next();

        if (type instanceof EntityType) {
          EntityType entityType = (EntityType) type;

          if (updatingEntities.contains(entityType))
            continue;

          updatingEntities.add(entityType);

          entityType.updateFlushPriority(updatingEntities);
        }
      }
    } finally {
      updatingEntities = null;
    }
  }

  /**
   * Creates a type.
   */
  public AmberType createType(String typeName)
    throws ConfigException
  {
    AmberType type = _typeManager.get(typeName);

    if (type != null)
      return type;

    Class cl = loadTempClass(typeName);

    if (cl == null)
      throw new ConfigException(L.l("'{0}' is an unknown type", typeName));

    return createType(cl);
  }

  /**
   * Creates a type.
   */
  public AmberType createType(Class javaType)
    throws ConfigException
  {
    AmberType type = _typeManager.create(javaType);

    if (type != null)
      return type;

    return createEntity(javaType);
  }

  /**
   * Sets the generator.
   */
  public AmberGenerator getGenerator()
  {
    if (_generator != null)
      return _generator;
    /*
      else if (_enhancer != null)
      return _enhancer;
    */
    else {
      _generator = _amberContainer.getGenerator();

      return _generator;
    }
  }

  /**
   * Returns the FALSE SQL literal, i.e., either "false" or "0".
   */
  public String getFalseLiteral()
  {
    return getMetaData().getFalseLiteral();
  }

  /**
   * Returns true if POSITION SQL function is allowed.
   */
  public boolean hasPositionFunction()
  {
    return getMetaData().supportsPositionFunction();
  }

  /**
   * Returns true if generated keys are allowed.
   */
  public boolean hasReturnGeneratedKeys()
  {
    return getMetaData().supportsGetGeneratedKeys();
  }

  /**
   * Sets the entity mappings config.
   */
  public void setEntityMappingsList(ArrayList<EntityMappingsConfig> entityMappingsList)
  {
    //_entityMappingsList = entityMappingsList;

    //_entityIntrospector.setEntityMappingsList(_entityMappingsList);

    //_mappedSuperIntrospector.setEntityMappingsList(_entityMappingsList);
  }

  /**
   * Initialize the resource.
   */
  public void init()
    throws ConfigException, IOException
  {
    initLoaders();

    /*
    if (_entityMappingsList != null) {
      BaseConfigIntrospector introspector = new BaseConfigIntrospector();

      introspector.initMetaData(_entityMappingsList, this);
    }
    */

    if (_dataSource == null)
      return;

    /*
    try {
      Connection conn = _dataSource.getConnection();

      try {
        DatabaseMetaData metaData = conn.getMetaData();

        try {
          _supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();
        } catch (Throwable e) {
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new ConfigException(e);
    }
    */
  }

  /**
   * Initialize the resource.
   */
  public void initLoaders()
    throws ConfigException, IOException
  {
    // getEnvManager().initLoaders();
  }

  public void initEntityHomes()
    throws AmberRuntimeException, ConfigException
  {
    ArrayList<AmberEntityHome> homeList;

    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;

      homeList = new ArrayList<AmberEntityHome>(_lazyHomeInit);

      _lazyHomeInit.clear();
    }

    if (_jtaDataSourceName != null && _jtaDataSource == null)
      _jtaDataSource = (DataSource) Jndi.lookup(_jtaDataSourceName);
    if (_nonJtaDataSourceName != null && _nonJtaDataSource == null)
      _nonJtaDataSource = (DataSource) Jndi.lookup(_nonJtaDataSourceName);

    initTables();

    // for QA consistency
    Collections.sort(homeList);

    for (AmberEntityHome home : homeList) {
      home.init();
    }
  }

  /**
   * Configure lazy.
   */
  public void initTables()
    throws ConfigException
  {
    for (IdGenerator gen : _tableGenMap.values())
      gen.start();

    while (_lazyTable.size() > 0) {
      AmberTable table = _lazyTable.remove(0);

      if (getDataSource() == null)
        throw new ConfigException(L.l("{0}: No configured data-source found.",
                                      this));

      if (getCreateDatabaseTables())
        table.createDatabaseTable(this);

      if (getValidateDatabaseTables())
        table.validateDatabaseTable(this);
    }
  }

  /**
   * Returns the cache connection.
   */
  public CacheConnection getCacheConnection()
  {
    // cache connection cannot be reused (#998)

    CacheConnection cacheConnection = new CacheConnection(this);

    // ejb/0a0b - avoid dangling connections.
    cacheConnection.register();

    return cacheConnection;
  }

  /**
   * Returns the cache connection.
   */
  public AmberConnection createAmberConnection(boolean isExtended)
  {
    return new AmberConnection(this, isExtended);
  }

  /**
   * Returns the thread's amber connection.
   */
  public AmberConnection getThreadConnection(boolean isExtended)
  {
    AmberConnection aConn = _threadConnection.get();

    if (aConn == null) {
      aConn = new AmberConnection(this, isExtended);
      aConn.initThreadConnection();

      _threadConnection.set(aConn);
    }

    return aConn;
  }

  /**
   * Unset the thread's amber connection.
   */
  public void removeThreadConnection()
  {
    _threadConnection.set(null);
  }

  /**
   * Returns an EntityHome.
   */
  public AmberEntityHome getHome(Class cl)
  {
    return getEntityHome(cl.getName());
  }

  /**
   * Returns the query cache.
   */
  public AbstractQuery getQueryParseCache(String sql)
  {
    return _queryParseCache.get(sql);
  }

  /**
   * Returns the query cache.
   */
  public void putQueryParseCache(String sql, AbstractQuery query)
  {
    _queryParseCache.put(sql, query);
  }

  /**
   * Returns the query result.
   */
  public ResultSetCacheChunk getQueryChunk(QueryCacheKey key)
  {
    SoftReference<ResultSetCacheChunk> ref = _queryCache.get(key);

    if (ref == null)
      return null;
    else {
      ResultSetCacheChunk chunk = ref.get();

      if (chunk != null && chunk.isValid())
        return chunk;
      else
        return null;
    }
  }

  /**
   * Returns the query meta data.
   */
  public ResultSetMetaData getQueryMetaData(QueryCacheKey key)
  {
    SoftReference<ResultSetMetaData> ref = _queryCacheMetaData.get(key);

    if (ref == null)
      return null;
    else
      return ref.get();
  }

  /**
   * Applies persistence unit default and entity listeners
   * for @PreXxx, @PostXxx callbacks.
   */
  protected void callListeners(int callbackIndex,
                               Entity entity)
  {
    // ejb/0g22
    if (! isJPA())
      return;

    String className = entity.getClass().getName();

    EntityType entityType = (EntityType) _typeManager.get(className);

    if (! entityType.getExcludeDefaultListeners()) {
      for (ListenerType listenerType : _defaultListeners) {
        for (Method m : listenerType.getCallbacks(callbackIndex)) {
          Listener listener = (Listener) listenerType.getInstance();
          listener.__caucho_callback(callbackIndex, entity);
        }
      }
    }

    ArrayList<ListenerType> listeners;

    listeners = _amberContainer.getEntityListeners(className);

    if (listeners == null)
      return;

    for (ListenerType listenerType : listeners) {

      if ((! entityType.getExcludeDefaultListeners()) &&
          _defaultListeners.contains(listenerType))
        continue;

      for (Method m : listenerType.getCallbacks(callbackIndex)) {
        Listener listener = (Listener) listenerType.getInstance();
        listener.__caucho_callback(callbackIndex, entity);
      }
    }
  }

  /**
   * Sets the query result.
   */
  public void putQueryChunk(QueryCacheKey key, ResultSetCacheChunk chunk)
  {
    _queryCache.put(key, new SoftReference<ResultSetCacheChunk>(chunk));
  }

  /**
   * Sets the query meta data.
   */
  public void putQueryMetaData(QueryCacheKey key, ResultSetMetaData metaData)
  {
    _queryCacheMetaData.put(key, new SoftReference<ResultSetMetaData>(metaData));
  }

  /**
   * Returns the entity item.
   */
  public EntityItem getEntityItem(String homeName, Object key)
    throws AmberException
  {
    AmberEntityHome home = getEntityHome(homeName);

    // XXX: needs refactoring
    throw new IllegalStateException("XXXX:");

    // return home.findEntityItem(getCacheConnection(), key, false);
  }

  /**
   * Returns the entity with the given key.
   */
  public EntityItem getEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType.getInstanceClass(), key);
      ref = _entityCache.get(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Returns the entity with the given key.
   */
  public EntityItem getEntity(EntityKey entityKey)
  {
    SoftReference<EntityItem> ref;

    ref = _entityCache.get(entityKey);

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Sets the entity result.
   */
  public EntityItem putEntity(EntityType rootType,
                              Object key,
                              EntityItem entity)
  {
    if (entity == null)
      throw new IllegalStateException(L.l("Null entity item cannot be added to the persistence unit cache"));

    SoftReference<EntityItem> ref = new SoftReference<EntityItem>(entity);
    EntityKey entityKey = new EntityKey(rootType.getInstanceClass(), key);

    // can't use putIfNew because the SoftReference might be empty, i.e.
    // not "new" but in need of replacement
    ref = _entityCache.put(entityKey, ref);

    return entity;
  }

  /**
   * Sets the entity result.
   */
  public EntityItem putEntity(Class cl,
                              Object key,
                              EntityItem entity)
  {
    if (entity == null)
      throw new IllegalStateException(L.l("Null entity item cannot be added to the persistence unit cache"));

    SoftReference<EntityItem> ref = new SoftReference<EntityItem>(entity);
    EntityKey entityKey = new EntityKey(cl, key);

    // can't use putIfNew because the SoftReference might be empty, i.e.
    // not "new" but in need of replacement
    ref = _entityCache.put(entityKey, ref);

    return entity;
  }

  /**
   * Remove the entity result.
   */
  public EntityItem removeEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType.getInstanceClass(), key);
      ref = _entityCache.remove(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Updates the cache item after commit.
   */
  public void updateCacheItem(EntityType rootType,
                              Object key,
                              EntityItem cacheItem)
  {
    if (cacheItem == null)
      throw new IllegalStateException(L.l("Null entity item cannot be used to update the persistence unit cache"));

    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType.getInstanceClass(), key);

      // jpa/0q00
      ref = new SoftReference<EntityItem>(cacheItem);
      EntityKey entityKey = new EntityKey(rootType.getInstanceClass(), key);

      // ejb/0628, ejb/06d0
      _entityCache.put(entityKey, ref);
    }
  }

  /**
   * Completions affecting the cache.
   */
  public void complete(ArrayList<AmberCompletion> completions)
  {
    int size = completions.size();
    if (size == 0)
      return;

    synchronized (_entityCache) {
      Iterator<LruCache.Entry<EntityKey,SoftReference<EntityItem>>> iter;

      iter = _entityCache.iterator();
      while (iter.hasNext()) {
        LruCache.Entry<EntityKey,SoftReference<EntityItem>> entry;
        entry = iter.next();

        EntityKey key = entry.getKey();
        SoftReference<EntityItem> valueRef = entry.getValue();
        EntityItem value = valueRef.get();

        if (value == null)
          continue;

        AmberEntityHome entityHome = value.getEntityHome();
        EntityType entityRoot = entityHome.getEntityType();
        Object entityKey = key.getKey();

        for (int i = 0; i < size; i++) {
          if (completions.get(i).complete(entityRoot, entityKey, value)) {
            // XXX: delete
          }
        }
      }
    }

    synchronized (_queryCache) {
      Iterator<SoftReference<ResultSetCacheChunk>> iter;

      iter = _queryCache.values();
      while (iter.hasNext()) {
        SoftReference<ResultSetCacheChunk> ref = iter.next();

        ResultSetCacheChunk chunk = ref.get();

        if (chunk != null) {
          for (int i = 0; i < size; i++) {
            if (completions.get(i).complete(chunk)) {
              // XXX: delete
            }
          }
        }
      }
    }
  }

  /**
   * destroys the manager.
   */
  public void destroy()
  {
    _typeManager = null;
    _queryCache = null;
    _entityCache = null;
  }

  /**
   * New Version of getCreateTableSQL which returns
   * the SQL for the table with the given SQL type
   * but takes sqlType, length, precision, and scale.
   */
  public String getCreateColumnSQL(int sqlType, int length, int precision, int scale) {
    return getMetaData().getCreateColumnSQL(sqlType, length, precision, scale);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
