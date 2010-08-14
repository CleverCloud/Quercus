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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.type;

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.AbstractConfigIntrospector;
import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.Listener;
import com.caucho.amber.field.*;
import com.caucho.amber.gen.EntityComponent;
import com.caucho.amber.idgen.AmberTableGenerator;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.config.ConfigException;
import com.caucho.jdbc.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Base for entity or mapped-superclass types.
 */
public class EntityType extends BeanType {
  private static final Logger log = Logger.getLogger(EntityType.class.getName());
  private static final L10N L = new L10N(EntityType.class);

  private EntityType _parentType;
  
  AmberTable _table;

  private String _rootTableName;

  private ArrayList<AmberTable> _secondaryTables
    = new ArrayList<AmberTable>();

  private ArrayList<ListenerType> _listeners
    = new ArrayList<ListenerType>();

  private ArrayList<ListenerCallback> _postLoadCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _prePersistCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _postPersistCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _preUpdateCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _postUpdateCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _preRemoveCallbacks
    = new ArrayList<ListenerCallback>();

  private ArrayList<ListenerCallback> _postRemoveCallbacks
    = new ArrayList<ListenerCallback>();

  private Id _id;

  private String _discriminatorValue;
  private boolean _isJoinedSubClass;

  private HashSet<String> _eagerFieldNames;

  private HashMap<String,EntityType> _subEntities;

  private ArrayList<AmberField> _mappedSuperclassFields
    = new ArrayList<AmberField>();

  private ArrayList<AmberField> _fields;

  private boolean _hasDependent;

  private Class _proxyClass;

  private AmberEntityHome _home;

  protected int _defaultLoadGroupIndex;
  protected int _loadGroupIndex;

  protected int _minDirtyIndex;
  protected int _dirtyIndex;

  private boolean _excludeDefaultListeners;
  private boolean _excludeSuperclassListeners;

  protected boolean _hasLoadCallback;

  private HashMap<String,IdGenerator> _idGenMap
    = new HashMap<String,IdGenerator>();

  private final Lifecycle _lifecycle = new Lifecycle();

  private VersionField _versionField;

  private int _flushPriority;

  private boolean _isIdentityGenerator;
  private boolean _isSequenceGenerator;


  public EntityType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
  }

  /**
   * returns true for a loadable entity 
   */
  @Override
  public boolean isEntity()
  {
    return ! Modifier.isAbstract(getBeanClass().getModifiers());
  }
  
  /**
   * Sets the table.
   */
  public void setTable(AmberTable table)
  {
    _table = table;

    // jpa/0gg0
    if (table == null)
      return;

    table.setType(this);

    if (_rootTableName == null)
      _rootTableName = table.getName();
  }

  /**
   * Returns the table.
   */
  public AmberTable getTable()
  {
    // jpa/0gg0
    if (_table == null && ! isAbstractClass()) {
      String sqlName = AbstractConfigIntrospector.toSqlName(getName());
      setTable(_amberPersistenceUnit.createTable(sqlName));
    }

    return _table;
  }

  /**
   * Gets the instance class.
   */
  public Class getInstanceClass()
  {
    return getInstanceClass(Entity.class);
  }

  /**
   * Returns the component interface name.
   */
  @Override
  public String getComponentInterfaceName()
  {
    return "com.caucho.amber.entity.Entity";
  }

  /**
   * Gets a component generator.
   */
  @Override
  public ClassComponent getComponentGenerator()
  {
    return new EntityComponent();
  }

  /**
   * Returns the flush priority.
   */
  public int getFlushPriority()
  {
    return _flushPriority;
  }

  /**
   * Adds a mapped superclass field.
   */
  public void addMappedSuperclassField(AmberField field)
  {
    if (_mappedSuperclassFields.contains(field))
      return;

    _mappedSuperclassFields.add(field);
    Collections.sort(_mappedSuperclassFields, new AmberFieldCompare());
  }

  /**
   * Returns the mapped superclass fields.
   */
  public ArrayList<AmberField> getMappedSuperclassFields()
  {
    return _mappedSuperclassFields;
  }

  /**
   * Returns the mapped superclass field with a given name.
   */
  public AmberField getMappedSuperclassField(String name)
  {
    for (int i = 0; i < _mappedSuperclassFields.size(); i++) {
      AmberField field = _mappedSuperclassFields.get(i);

      if (field.getName().equals(name))
        return field;
    }

    return null;
  }

  /**
   * returns the merged fields
   */
  @Override
  public ArrayList<AmberField> getFields()
  {
    if (_fields != null)
      return _fields;
    else
      return super.getFields();
  }

  /**
   * Returns the root table name.
   */
  public String getRootTableName()
  {
    return _rootTableName;
  }

  /**
   * Sets the root table name.
   */
  public void setRootTableName(String rootTableName)
  {
    _rootTableName = rootTableName;
  }

  /**
   * Returns the version field.
   */
  public VersionField getVersionField()
  {
    return _versionField;
  }

  /**
   * Sets the version field.
   */
  public void setVersionField(VersionField versionField)
  {
    addField(versionField);

    _versionField = versionField;
  }

  /**
   * Adds a secondary table.
   */
  public void addSecondaryTable(AmberTable table)
  {
    if (! _secondaryTables.contains(table)) {
      _secondaryTables.add(table);
    }

    table.setType(this);
  }

  /**
   * Gets the secondary tables.
   */
  public ArrayList<AmberTable> getSecondaryTables()
  {
    return _secondaryTables;
  }

  /**
   * Adds an entity listener.
   */
  public void addListener(ListenerType listener)
  {
    if (_listeners.contains(listener))
      return;

    _listeners.add(listener);
  }

  /**
   * Gets the entity listeners.
   */
  public ArrayList<ListenerType> getListeners()
  {
    return _listeners;
  }

  /**
   * Gets a secondary table.
   */
  public AmberTable getSecondaryTable(String name)
  {
    for (AmberTable table : _secondaryTables) {
      if (table.getName().equals(name))
        return table;
    }

    return null;
  }

  /**
   * Returns true if and only if it has a
   * many-to-one, one-to-one or embedded field/property.
   */
  public boolean hasDependent()
  {
    return _hasDependent;
  }

  /**
   * Sets true if and only if it has a
   * many-to-one, one-to-one or embedded field/property.
   */
  public void setHasDependent(boolean hasDependent)
  {
    _hasDependent = hasDependent;
  }

  /**
   * Returns the java type.
   */
  @Override
  public String getForeignTypeName()
  {
    return getId().getForeignTypeName();
  }

  /**
   * Gets the proxy class.
   */
  public Class getProxyClass()
  {
    if (_proxyClass != null)
      return _proxyClass;
    else
      return _tBeanClass;
  }

  /**
   * Gets the proxy class.
   */
  public void setProxyClass(Class proxyClass)
  {
    _proxyClass = proxyClass;
  }

  /**
   * Returns true if the corresponding class is abstract.
   */
  public boolean isAbstractClass()
  {
    // ejb/0600 - EJB 2.1 are not abstract in this sense
    return (Modifier.isAbstract(getBeanClass().getModifiers())
            && _proxyClass == null);
  }

  /**
   * Sets the id.
   */
  public void setId(Id id)
  {
    _id = id;
  }

  /**
   * Returns the id.
   */
  public Id getId()
  {
    return _id;
  }

  /**
   * Set true for joined-subclass
   */
  public void setJoinedSubClass(boolean isJoinedSubClass)
  {
    _isJoinedSubClass = isJoinedSubClass;
  }

  /**
   * Set true for joined-subclass
   */
  public boolean isJoinedSubClass()
  {
    if (getParentType() != null)
      return getParentType().isJoinedSubClass();
    else
      return _isJoinedSubClass;
  }

  /**
   * Sets the discriminator value.
   */
  public String getDiscriminatorValue()
  {
    if (_discriminatorValue != null)
      return _discriminatorValue;
    else {
      return getBeanClass().getSimpleName();
    }
  }

  /**
   * Sets the discriminator value.
   */
  public void setDiscriminatorValue(String value)
  {
    _discriminatorValue = value;
  }

  /**
   * Returns true if read-only
   */
  public boolean isReadOnly()
  {
    return getTable().isReadOnly();
  }

  /**
   * Sets true if read-only
   */
  public void setReadOnly(boolean isReadOnly)
  {
    getTable().setReadOnly(isReadOnly);
  }

  /**
   * Returns the cache timeout.
   */
  public long getCacheTimeout()
  {
    return getTable().getCacheTimeout();
  }

  /**
   * Sets the cache timeout.
   */
  public void setCacheTimeout(long timeout)
  {
    getTable().setCacheTimeout(timeout);
  }

  /**
   * Adds a new field.
   */
  @Override
  public void addField(AmberField field)
  {
    super.addField(field);

    if (! field.isLazy()) {
      if (_eagerFieldNames == null)
        _eagerFieldNames = new HashSet<String>();

      _eagerFieldNames.add(field.getName());
    }
  }

  /**
   * Gets the EAGER field names.
   */
  public HashSet<String> getEagerFieldNames()
  {
    return _eagerFieldNames;
  }

  /**
   * Returns the field with a given name.
   */
  @Override
  public AmberField getField(String name)
  {
    if (_id != null) {
      ArrayList<IdField> keys = _id.getKeys();

      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

        if (key.getName().equals(name))
          return key;
      }
    }

    return super.getField(name);
  }

  /**
   * Returns the columns.
   */
  public ArrayList<AmberColumn> getColumns()
  {
    // jpa/0gg0
    if (getTable() == null)
      return null;

    return getTable().getColumns();
  }

  /**
   * Gets the exclude default listeners flag.
   */
  public boolean getExcludeDefaultListeners()
  {
    return _excludeDefaultListeners;
  }

  /**
   * Sets the exclude default listeners flag.
   */
  public void setExcludeDefaultListeners(boolean b)
  {
    _excludeDefaultListeners = b;
  }

  /**
   * Gets the exclude superclass listeners flag.
   */
  public boolean getExcludeSuperclassListeners()
  {
    return _excludeSuperclassListeners;
  }

  /**
   * Sets the exclude superclass listeners flag.
   */
  public void setExcludeSuperclassListeners(boolean b)
  {
    _excludeSuperclassListeners = b;
  }

  /**
   * True if the load lifecycle callback should be generated.
   */
  public void setHasLoadCallback(boolean hasCallback)
  {
    _hasLoadCallback = hasCallback;
  }

  /**
   * True if the load lifecycle callback should be generated.
   */
  public boolean getHasLoadCallback()
  {
    return _hasLoadCallback;
  }

  /**
   * Returns the root type.
   */
  public EntityType getRootType()
  {
    EntityType parent = getParentType();

    if (parent != null)
      return parent.getRootType();
    else
      return this;
  }

  /**
   * Returns the parent type.
   */
  public EntityType getParentType()
  {
    return _parentType;
  }

  /**
   * Returns the parent type.
   */
  public void setParentType(EntityType parentType)
  {
    _parentType = parentType;
  }

  /**
   * Adds a sub-class.
   */
  public void addSubClass(EntityType type)
  {
    if (_subEntities == null)
      _subEntities = new HashMap<String,EntityType>();

    _subEntities.put(type.getDiscriminatorValue(), type);
  }

  /**
   * Gets a sub-class.
   */
  public EntityType getSubClass(String discriminator)
  {
    if (_subEntities == null)
      return this;

    EntityType subType = _subEntities.get(discriminator);

    if (subType != null)
      return subType;
    else {
      // jpa/0l15
      for (EntityType subEntity : _subEntities.values()) {
        subType = subEntity.getSubClass(discriminator);

        if (subType != subEntity)
          return subType;
      }

      return this;
    }
  }

  /**
   * Creates a new entity for this specific instance type.
   */
  public Entity createBean()
  {
    try {
      Entity entity = (Entity) getInstanceClass().newInstance();

      return entity;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Returns the home.
   */
  public AmberEntityHome getHome()
  {
    if (_home == null) {
      _home = getPersistenceUnit().getEntityHome(getName());
    }

    return _home;
  }

  /**
   * Returns the next load group.
   */
  public int nextLoadGroupIndex()
  {
    int nextLoadGroupIndex = getLoadGroupIndex() + 1;

    _loadGroupIndex = nextLoadGroupIndex;

    return nextLoadGroupIndex;
  }

  /**
   * Returns the current load group.
   */
  public int getLoadGroupIndex()
  {
    return _loadGroupIndex;
  }

  /**
   * Sets the next default loadGroupIndex
   */
  public void nextDefaultLoadGroupIndex()
  {
    _defaultLoadGroupIndex = nextLoadGroupIndex();
  }

  /**
   * Returns the current load group.
   */
  public int getDefaultLoadGroupIndex()
  {
    return _defaultLoadGroupIndex;
  }

  /**
   * Returns true if the load group is owned by this type (not a subtype).
   */
  public boolean isLoadGroupOwnedByType(int i)
  {
    return getDefaultLoadGroupIndex() <= i && i <= getLoadGroupIndex();
  }

  /**
   * Returns the next dirty index
   */
  public int nextDirtyIndex()
  {
    int dirtyIndex = getDirtyIndex();

    _dirtyIndex = dirtyIndex + 1;

    return dirtyIndex;
  }

  /**
   * Returns the current dirty group.
   */
  public int getDirtyIndex()
  {
    return _dirtyIndex;
  }

  /**
   * Returns the min dirty group.
   */
  public int getMinDirtyIndex()
  {
    return _minDirtyIndex;
  }

  /**
   * Returns true if the load group is owned by this type (not a subtype).
   */
  public boolean isDirtyIndexOwnedByType(int i)
  {
    return getMinDirtyIndex() <= i && i < getDirtyIndex();
  }

  /**
   * Initialize the entity.
   */
  @Override
  public void init()
    throws ConfigException
  {
    if (getConfigException() != null)
      return;

    if (! _lifecycle.toInit())
      return;

    super.init();

    // forces table lazy load
    getTable();

    initId();

    _fields = getMergedFields();

    for (AmberField field : getFields()) {
      if (field.isUpdateable())
        field.setIndex(nextDirtyIndex());

      field.init();
    }

    if (getMappedSuperclassFields() == null)
      return;

    for (AmberField field : getMappedSuperclassFields()) {
      if (field.isUpdateable())
        field.setIndex(nextDirtyIndex());

      field.init();
    }
  }

  protected void initId()
  {
    assert getId() != null : "null id for " + getName();

    getId().init();
  }

  protected ArrayList<AmberField> getMergedFields()
  {
    ArrayList<AmberField> mappedFields = getMappedSuperclassFields();
    
    if (mappedFields == null)
      return getFields();

    ArrayList<AmberField> resultFields = new ArrayList<AmberField>();

    resultFields.addAll(getFields());

    for (AmberField field : mappedFields) {
      resultFields.add(field);
    }

    Collections.sort(resultFields, new AmberFieldCompare());

    return resultFields;
  }

  /**
   * Start the entry.
   */
  public void start()
    throws ConfigException
  {
    init();

    startGenerator();

    startImpl();

    if (! _lifecycle.toActive())
      return;
  }

  private void startGenerator()
  {
    IdField idGenField = getId().getGeneratedIdField();

    if (idGenField == null)
      return;

    JdbcMetaData md = getPersistenceUnit().getMetaData();

    if ("sequence".equals(idGenField.getGenerator())) {
      _isIdentityGenerator = false;
      _isSequenceGenerator = true;

      if (! md.supportsSequences())
        throw new ConfigException(L.l("'{0}' does not support sequences",
                                      md.getDatabaseName()));
    }
    else if ("identity".equals(idGenField.getGenerator())) {
      _isIdentityGenerator = true;
      _isSequenceGenerator = false;

      if (! md.supportsIdentity())
        throw new ConfigException(L.l("'{0}' does not support identity",
                                      md.getDatabaseName()));
    }
    else if ("auto".equals(idGenField.getGenerator())) {
      if (md.supportsIdentity())
        _isIdentityGenerator = true;
      else if (md.supportsSequences())
        _isSequenceGenerator = true;
    }

    if (! _isIdentityGenerator
        && getGenerator(idGenField.getName()) == null) {
      IdGenerator gen;
      
      if (_isSequenceGenerator) {
        String name = getTable().getName() + "_cseq";

        gen = getPersistenceUnit().createSequenceGenerator(name, 1);
      }
      else
        gen = getPersistenceUnit().getTableGenerator("caucho");

      _idGenMap.put(idGenField.getName(), gen);
    }

    // XXX: really needs to be called from the table-init code
    for (IdGenerator idGen : _idGenMap.values()) {
      try {
        if (idGen instanceof SequenceIdGenerator) {
          ((SequenceIdGenerator) idGen).init(_amberPersistenceUnit);
        }
        else if (idGen instanceof AmberTableGenerator) {
          // jpa/0g60
          ((AmberTableGenerator) idGen).init(_amberPersistenceUnit);
        }
      } catch (SQLException e) {
        throw ConfigException.create(e);
      }
    }
  }

  private void startImpl()
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      for (ListenerType listenerType : getListeners()) {
        String listenerClass = listenerType.getBeanClass().getName();

        Class cl = Class.forName(listenerClass, false, loader);
        Object listener;

        try {
          listener = cl.newInstance();
        } catch (InstantiationException e) {
          throw new ConfigException(L.l("'{0}' could not be instantiated.",
                                        cl));
        }

        for (Method jMethod : listenerType.getCallbacks(Listener.PRE_PERSIST)) {
          Method method = getListenerMethod(cl, jMethod.getName());

          if (method != null)
            _prePersistCallbacks.add(new ListenerCallback(listener, method));
        }

        for (Method jMethod : listenerType.getCallbacks(Listener.POST_PERSIST)) {
          Method method = getListenerMethod(cl, jMethod.getName());

          if (method != null)
            _postPersistCallbacks.add(new ListenerCallback(listener, method));
        }

        for (Method jMethod : listenerType.getCallbacks(Listener.POST_LOAD)) {
          Method method = getListenerMethod(cl, jMethod.getName());

          if (method != null)
            _postLoadCallbacks.add(new ListenerCallback(listener, method));
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private Method getListenerMethod(Class cl, String methodName)
  {
    if (cl == null || cl.equals(Object.class))
      return null;

    Method []methods = cl.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Class []paramTypes = methods[i].getParameterTypes();
      
      if (methods[i].getName().equals(methodName)
          && paramTypes.length == 1
          && paramTypes[0].isAssignableFrom(getBeanClass())) {
        return methods[i];
      }
    }

    return getListenerMethod(cl.getSuperclass(), methodName);
  }

  /**
   * Generates a string to load the field.
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
     out.print("(" + getInstanceClassName() + ") ");

    index = getId().generateLoadForeign(out, rs, indexVar, index);

    return index;
  }

  /**
   * Returns true if there's a field with the matching load group.
   */
  public boolean hasLoadGroup(int loadGroupIndex)
  {
    if (loadGroupIndex == 0)
      return true;

    for (AmberField field : getFields()) {
      if (field.hasLoadGroup(loadGroupIndex))
        return true;
    }

    return false;
  }

  /**
   * Generates loading code after the basic fields.
   */
  public int generatePostLoadSelect(JavaWriter out, int index,
                                    int loadGroupIndex)
    throws IOException
  {
    if (loadGroupIndex == 0 && getDiscriminator() != null)
      index++;

    // jpa/0l40
    for (EntityType type = this; type != null; type = type.getParentType()) {
      index = generatePostLoadSelect(out, index,
                                     type.getMappedSuperclassFields());

      index = generatePostLoadSelect(out, index, type.getFields());
    }

    return index;
  }

  private int generatePostLoadSelect(JavaWriter out,
                                     int index,
                                     ArrayList<AmberField> fields)
    throws IOException
  {
    if (fields != null) {
      for (int i = 0; i < fields.size(); i++) {
        AmberField field = fields.get(i);

        // jpa/0l40 if (field.getLoadGroupIndex() == loadGroupIndex)
        index = field.generatePostLoadSelect(out, index);
      }
    }

    return index;
  }

  /**
   * Generates the load code for native fields
   */
  public void generateLoadNative(JavaWriter out)
    throws IOException
  {
    int index = 0;
    
    for (AmberField field : getFields()) {
      index = field.generateLoadNative(out, index);
    }
  }

  /**
   * Generates the load code for native fields
   */
  public void generateNativeColumnNames(ArrayList<String> names)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generateNativeColumnNames(names);
    }
  }

  /**
   * Generates a string to set the field.
   */
  @Override
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    if (getId() != null)
      getId().generateSet(out, pstmt, index, value);
  }

  /**
   * Gets the value.
   */
  @Override
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getHome().loadLazy(aConn, rs, index);
  }

  /**
   * Finds the object
   */
  @Override
  public EntityItem findItem(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getHome().findItem(aConn, rs, index);
  }

  /**
   * Gets the value.
   */
  public Object getLoadObject(AmberConnection aConn,
                              ResultSet rs, int index)
    throws SQLException
  {
    return getHome().loadFull(aConn, rs, index);
  }

  /**
   * Returns true for sequence generator
   */
  public boolean isSequenceGenerator()
  {
    return _isSequenceGenerator;
  }

  /**
   * Returns true for sequence generator
   */
  public boolean isIdentityGenerator()
  {
    return _isIdentityGenerator;
  }

  /**
   * Sets the named generator.
   */
  public void setGenerator(String name, IdGenerator gen)
  {
    _idGenMap.put(name, gen);
  }

  /**
   * Sets the named generator.
   */
  public IdGenerator getGenerator(String name)
  {
    return _idGenMap.get(name);
  }

  /**
   * Gets the named generator.
   */
  public long nextGeneratorId(AmberConnection aConn, String name)
    throws SQLException
  {
    IdGenerator idGen = _idGenMap.get(name);

    return idGen.allocate(aConn);
  }

  /**
   * Loads from an object.
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    getId().generateLoadFromObject(out, obj);

    for (AmberField field : getFields()) {
      field.generateLoadFromObject(out, obj);
    }
  }

  /**
   * Copy from an object.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadGroup)
    throws IOException
  {
    if (getParentType() != null) // jpa/0ge3
      getParentType().generateCopyLoadObject(out, dst, src, loadGroup);

    ArrayList<AmberField> fields = getFields();

    for (AmberField field : fields) {
      // XXX: setter issue, too

      field.generateCopyLoadObject(out, dst, src, loadGroup);
    }
  }

  /**
   * Copy from an object.
   */
  public void generateMergeFrom(JavaWriter out,
                               String dst, String src)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateMergeFrom(out, dst, src);

    ArrayList<AmberField> fields = getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateMergeFrom(out, dst, src);
    }
  }

  /**
   * Copy from an object.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateCopyUpdateObject(out, dst, src, updateIndex);

    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateCopyUpdateObject(out, dst, src, updateIndex);
    }
  }

  /**
   * Checks entity-relationships from an object.
   */
  public void generateDumpRelationships(JavaWriter out,
                                        int updateIndex)
    throws IOException
  {
    if (getParentType() != null) // jpa/0ge3
      getParentType().generateDumpRelationships(out, updateIndex);

    ArrayList<AmberField> fields = getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateDumpRelationships(out, updateIndex);
    }
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateKeyLoadSelect(String id)
  {
    String select = getId().generateLoadSelect(id);

    if (getDiscriminator() != null) {
      if (select != null && ! select.equals(""))
        select = select + ", ";

      select = select + getDiscriminator().getName();
    }

    return select;
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateFullLoadSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();

    String idSelect = getId().generateSelect(id);

    if (idSelect != null)
      cb.append(idSelect);

    String loadSelect = generateLoadSelect(id);

    if (! idSelect.equals("") && ! loadSelect.equals(""))
      cb.append(",");

    cb.append(loadSelect);

    return cb.close();
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(String id)
  {
    return generateLoadSelect(getTable(), id);
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(AmberTable table, String id)
  {
    StringBuilder sb = new StringBuilder();

    // jpa/0l11
    if (getTable() == table && getDiscriminator() != null) {
      if (id != null) {
        if (getDiscriminator().getTable() == getTable()) {
          sb.append(id + ".");
          sb.append(getDiscriminator().getName());
        }
        else {
          // jpa/0l4b
          sb.append("'" + getDiscriminatorValue() + "'");
        }
      }
    }

    generateLoadSelect(sb, table, id, 0);

    if (sb.length() > 0)
      return sb.toString();
    else
      return null;
  }

  /**
   * Generates the select clause for a load.
   */
  @Override
  public void generateLoadSelect(StringBuilder sb, AmberTable table,
                                 String id, int loadGroup)
  {
    if (_parentType != null)
      _parentType.generateLoadSelect(sb, table, id, loadGroup);
    
    super.generateLoadSelect(sb, table, id, loadGroup);
  }

  /**
   * Generates the auto insert sql.
   */
  public String generateAutoCreateSQL(AmberTable table)
  {
    return generateCreateSQL(table, true);
  }

  /**
   * Generates the insert sql.
   */
  public String generateCreateSQL(AmberTable table)
  {
    return generateCreateSQL(table, false);
  }

  /**
   * Generates the insert sql.
   */
  private String generateCreateSQL(AmberTable table, boolean isAuto)
  {
    CharBuffer sql = new CharBuffer();

    sql.append("insert into ");
    sql.append(JavaWriter.escapeJavaString(table.getName()) + " (");

    boolean isFirst = true;

    ArrayList<String> idColumns = new ArrayList<String>();

    for (IdField field : getId().getKeys()) {
      if (isAuto && field.getGenerator() != null)
        continue;
      
      for (AmberColumn key : field.getColumns()) {
        String name;

        if (table == key.getTable())
          name = key.getName();
        else
          name = table.getDependentIdLink().getSourceColumn(key).getName();

        idColumns.add(name);

        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        sql.append(name);
      }
    }

    if (table == getTable() && getDiscriminator() != null) {
      if (! isFirst)
        sql.append(", ");
      isFirst = false;

      sql.append(getDiscriminator().getName());
    }

    ArrayList<String> columns = new ArrayList<String>();
    generateInsertColumns(table, columns);

    for (String columnName : columns) {
      if (! isFirst)
        sql.append(", ");
      isFirst = false;

      sql.append(columnName);
    }

    sql.append(") values (");

    isFirst = true;
    for (int i = 0; i < idColumns.size(); i++) {
      if (! isFirst)
        sql.append(", ");
      isFirst = false;

      sql.append("?");
    }

    if (table == getTable() && getDiscriminator() != null) {
      if (! isFirst)
        sql.append(", ");
      isFirst = false;

      sql.append("'" + getDiscriminatorValue() + "'");
    }

    for (int i = 0; i < columns.size(); i++) {
      if (! isFirst)
        sql.append(", ");
      isFirst = false;

      sql.append("?");
    }

    sql.append(")");

    return sql.toString();
  }

  protected void generateInsertColumns(AmberTable table, ArrayList<String> columns)
  {
    if (getParentType() != null)
      getParentType().generateInsertColumns(table, columns);

    for (AmberField field : getFields()) {
      if (field.getTable() == table)
        field.generateInsertColumns(columns);
    }
  }

  /**
   * Generates the update sql.
   */
  public void generateInsertSet(JavaWriter out,
                                AmberTable table,
                                String pstmt,
                                String query,
                                String obj)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateInsertSet(out, table, pstmt, query, obj);

    for (AmberField field : getFields()) {
      if (field.getTable() == table)
        field.generateInsertSet(out, pstmt, query, obj);
    }
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateIdSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append(getId().generateSelect(id));

    if (getDiscriminator() != null) {
      cb.append(", ");
      cb.append(getDiscriminator().getName());
    }

    return cb.close();
  }

  /**
   * Generates the update sql.
   */
  public void generateUpdateSQLPrefix(CharBuffer sql)
  {
    sql.append("update " + getTable().getName() + " set ");
  }

  /**
   * Generates the update sql.
   *
   * @param sql the partially built sql
   * @param group the dirty group
   * @param mask the group's mask
   * @param isFirst marks the first set group
   */
  public boolean generateUpdateSQLComponent(CharBuffer sql,
                                            int group,
                                            long mask,
                                            boolean isFirst)
  {
    ArrayList<AmberField> fields = getFields();

    while (mask != 0) {
      int i = 0;
      for (i = 0; (mask & (1L << i)) == 0; i++) {
      }

      mask &= ~(1L << i);

      AmberField field = null;

      for (int j = 0; j < fields.size(); j++) {
        field = fields.get(j);

        if (field.getIndex() == i + group * 64)
          break;
        else
          field = null;
      }

      if (field != null) {

        // jpa/0x00
        if (field instanceof VersionField)
          continue;

        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        field.generateUpdate(sql);
      }
    }

    // jpa/0x00
    for (int j = 0; j < fields.size(); j++) {
      AmberField field = fields.get(j);

      if (field instanceof VersionField) {
        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        field.generateUpdate(sql);
        break;
      }
    }

    return isFirst;
  }

  /**
   * Generates the update sql.
   */
  public void generateUpdateSQLSuffix(CharBuffer sql)
  {
    sql.append(" where ");

    sql.append(getId().generateMatchArgWhere(null));

    // optimistic locking
    if (_versionField != null) {
      sql.append(" and ");
      sql.append(_versionField.generateMatchArgWhere(null));
    }
  }

  /**
   * Generates the update sql.
   */
  public String generateUpdateSQL(long mask)
  {
    if (mask == 0)
      return null;

    CharBuffer sql = CharBuffer.allocate();

    sql.append("update " + getTable().getName() + " set ");

    boolean isFirst = true;

    ArrayList<AmberField> fields = getFields();

    while (mask != 0) {
      int i = 0;
      for (i = 0; (mask & (1L << i)) == 0; i++) {
      }

      mask &= ~(1L << i);

      AmberField field = null;

      for (int j = 0; j < fields.size(); j++) {
        field = fields.get(j);

        if (field.getIndex() == i)
          break;
        else
          field = null;
      }

      if (field != null) {
        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        field.generateUpdate(sql);
      }
    }

    if (isFirst)
      return null;

    sql.append(" where ");

    sql.append(getId().generateMatchArgWhere(null));

    return sql.toString();
  }

  /**
   * Generates code after the remove.
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generatePreDelete(out);
    }
  }

  /**
   * Generates code after the remove.
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generatePostDelete(out);
    }
  }

  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, Object key)
    throws SQLException
  {
    getHome().delete(aConn, key);
  }

  /**
   * Deletes by the primary key.
   */
  public void update(Entity entity)
    throws SQLException
  {
    // aConn.addCompletion(_tableCompletion);
  }

  /**
   * Updates global (persistence unit) entity priorities
   * for flushing.
   */
  public int updateFlushPriority(ArrayList<EntityType> updatingEntities)
  {
    // jpa/0h25, jpa/0h26, jpa/0h29, jpa/0j67

    _flushPriority = 0;

    ArrayList<AmberField> fields = getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field instanceof ManyToOneField) {
        ManyToOneField manyToOne = (ManyToOneField) field;

        EntityType targetRelatedType = manyToOne.getEntityTargetType();

        if (targetRelatedType instanceof EntityType) {
          EntityType targetType = (EntityType) targetRelatedType;

          if (! updatingEntities.contains(targetType)) {
            updatingEntities.add(targetType);
            targetType.updateFlushPriority(updatingEntities);
          }

          int targetPriority = targetType.getFlushPriority();

          if (targetPriority >= _flushPriority) {
            EntityType type = null;

            // jpa/0j67
            if (! manyToOne.isAnnotatedManyToOne()) {
              for (AmberField targetField : targetType.getFields()) {
                if (targetField instanceof ManyToOneField) {
                  ManyToOneField targetManyToOne = (ManyToOneField) targetField;

                  type = targetManyToOne.getEntityTargetType();

                  if (this == type) {
                    if (targetManyToOne.isAnnotatedManyToOne()) {
                      break;
                    }
                  }
                }
              }
            }

            if (this == type)
              continue;

            _flushPriority = targetPriority + 1;
          }
        }
      }
    }

    return _flushPriority;
  }

  /**
   * Returns a completion for the given field.
   */
  public AmberCompletion createManyToOneCompletion(String name,
                                                   Entity source,
                                                   Object newTarget)
  {
    AmberField field = getField(name);

    EntityType parentType = this;

    // jpa/0l40
    while (field == null) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      field = parentType.getField(name);
    }

    if (field instanceof ManyToOneField) {
      ManyToOneField manyToOne = (ManyToOneField) field;

      return getTable().getInvalidateCompletion();
    }
    else
      throw new IllegalStateException();
  }

  /**
   * XXX: temp hack.
   */
  public boolean isEJBProxy(String typeName)
  {
    return (getBeanClass() != getProxyClass() &&
            getProxyClass().getName().equals(typeName));
  }

  //
  // callbacks
  //

  /**
   * Callbacks before an entity is persisted
   */
  public void prePersist(Entity entity)
  {
    for (int i = 0; i < _prePersistCallbacks.size(); i++)
      _prePersistCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks after an entity is persisted
   */
  public void postPersist(Entity entity)
  {
    for (int i = 0; i < _postPersistCallbacks.size(); i++)
      _postPersistCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks before an entity is updateed
   */
  public void preUpdate(Entity entity)
  {
    for (int i = 0; i < _preUpdateCallbacks.size(); i++)
      _preUpdateCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks after an entity is updated
   */
  public void postUpdate(Entity entity)
  {
    for (int i = 0; i < _postUpdateCallbacks.size(); i++)
      _postUpdateCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks before an entity is removeed
   */
  public void preRemove(Entity entity)
  {
    for (int i = 0; i < _preRemoveCallbacks.size(); i++)
      _preRemoveCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks after an entity is removeed
   */
  public void postRemove(Entity entity)
  {
    for (int i = 0; i < _postRemoveCallbacks.size(); i++)
      _postRemoveCallbacks.get(i).invoke(entity);
  }

  /**
   * Callbacks after an entity is loaded
   */
  public void postLoad(Entity entity)
  {
    for (int i = 0; i < _postLoadCallbacks.size(); i++)
      _postLoadCallbacks.get(i).invoke(entity);
  }
}
