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

package com.caucho.amber.cfg;

import com.caucho.amber.entity.Listener;

import com.caucho.amber.field.*;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import javax.persistence.*;
import javax.persistence.EmbeddedId;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Base concrete introspector for orm.xml and annotations.
 */
public class BaseConfigIntrospector extends AbstractConfigIntrospector {
  private static final Logger log
    = Logger.getLogger(BaseConfigIntrospector.class.getName());
  private static final L10N L = new L10N(BaseConfigIntrospector.class);

  private static final Class []_annTypes = new Class[] {
    Basic.class, javax.persistence.Column.class, javax.persistence.Id.class,
    ElementCollection.class, EmbeddedId.class,
    ManyToOne.class, OneToMany.class, OneToOne.class, ManyToMany.class,
    Version.class, Transient.class
  };

  final AmberConfigManager _configManager;
  final AmberPersistenceUnit _persistenceUnit;

  ArrayList<Completion> _linkCompletions = new ArrayList<Completion>();
  ArrayList<Completion> _depCompletions = new ArrayList<Completion>();

  HashMap<String, EmbeddableConfig> _embeddableConfigMap
    = new HashMap<String, EmbeddableConfig>();

  ArrayList<EntityMappingsConfig> _entityMappingsList;

  // HashMap<String, EntityConfig> _entityConfigMap
  //   = new HashMap<String, EntityConfig>();
  //
  // HashMap<String, MappedSuperclassConfig> _mappedSuperclassConfigMap
  //   = new HashMap<String, MappedSuperclassConfig>();

  /**
   * Creates the introspector.
   */
  public BaseConfigIntrospector(AmberConfigManager manager)
  {
    _configManager = manager;
    _persistenceUnit = manager.getPersistenceUnit();
  }

  /**
   * Sets the entity mappings list.
   */
  public void setEntityMappingsList(ArrayList<EntityMappingsConfig> entityMappingsList)
  {
    _entityMappingsList = entityMappingsList;
  }

  /**
   * Returns the entity config for a class name.
   */
  public EntityConfig getEntityConfig(String className)
  {
    // jpa/0r41
    if (_entityMappingsList == null)
      return null;

    // jpa/0s2l: mapping-file.

    HashMap<String, EntityConfig> entityMap;
    EntityConfig entityConfig;

    for (EntityMappingsConfig entityMappings : _entityMappingsList) {
      entityMap = entityMappings.getEntityMap();

      if (entityMap != null) {
        entityConfig = entityMap.get(className);

        if (entityConfig != null)
          return entityConfig;
      }
    }

    return null;
  }

  /**
   * Returns the mapped superclass config for a class name.
   */
  public MappedSuperclassConfig getMappedSuperclassConfig(String className)
  {
    if (_entityMappingsList == null)
      return null;

    HashMap<String, MappedSuperclassConfig> superclassMap;
    MappedSuperclassConfig superclassConfig;

    for (EntityMappingsConfig entityMappings : _entityMappingsList) {
      superclassMap = entityMappings.getMappedSuperclassMap();

      if (superclassMap != null) {
        superclassConfig = superclassMap.get(className);

        if (superclassConfig != null)
          return superclassConfig;
      }
    }

    return null;
  }

  /**
   * Initializes the persistence unit meta data:
   * default listeners and so on.
   */
  public void initMetaData(ArrayList<EntityMappingsConfig> entityMappingsList,
                           AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    PersistenceUnitMetaDataConfig metaData = null;

    for (EntityMappingsConfig entityMappings : entityMappingsList) {
      metaData = entityMappings.getPersistenceUnitMetaData();

      // It is undefined if this element occurs in multiple mapping
      // files within the same persistence unit.
      if (metaData != null)
        break;
    }

    if (metaData == null)
      return;

    PersistenceUnitDefaultsConfig defaults;

    defaults = metaData.getPersistenceUnitDefaults();

    if (defaults == null)
      return;

    EntityListenersConfig entityListeners;
    entityListeners = defaults.getEntityListeners();

    if (entityListeners == null)
      return;

    ArrayList<EntityListenerConfig> listeners;
    listeners = entityListeners.getEntityListeners();

    for (EntityListenerConfig listener : listeners)
      introspectDefaultListener(listener, persistenceUnit);
  }

  public void introspectDefaultListener(EntityListenerConfig listener,
                                        AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    String className = listener.getClassName();

    Class type = persistenceUnit.loadTempClass(className);

    if (type == null)
      throw new ConfigException(L.l("'{0}' is an unknown type for <entity-listener> in orm.xml",
                                    className));

    ListenerType listenerType = persistenceUnit.addDefaultListener(type);

    introspectListener(type, listenerType);
  }

  public void introspectEntityListeners(Class type,
                                        EntityType entityType,
                                        AmberPersistenceUnit persistenceUnit)
    throws ConfigException
  {
    getInternalEntityListenersConfig(type, _annotationCfg);
    EntityListeners entityListenersAnn = (EntityListeners) _annotationCfg.getAnnotation();
    EntityListenersConfig entityListenersCfg
      = _annotationCfg.getEntityListenersConfig();

    Class listeners[] = null;
/*
    // XML mapping takes higher priority than annotations.
    if (entityListenersCfg != null)
      listeners = entityListenersCfg.getEntityListeners().toArray();
    else if (entityListenersAnn != null)
      listeners = entityListenersAnn.value();
    else
      return;
*/
    String entityTypeName = entityType.getBeanClass().getName();

    for (int i = 0; listeners != null && i < listeners.length; i++) {

      Class cl = null;

      // Introspects annotation or xml.
      if (listeners[i] instanceof Class)
        cl = (Class) listeners[i];
      else {
        /*
        EntityListenerConfig listenerConfig
          = (EntityListenerConfig) listeners[i];

        String className = listenerConfig.getClassName();

        cl = persistenceUnit.loadTempClass(className);

        if (cl == null)
          throw new ConfigException(L.l("'{0}' is an unknown type for <entity-listener> in orm.xml",
                                        className));
         */
      }

      if (persistenceUnit.getDefaultListener(cl.getName()) != null)
        continue;

      introspectEntityListener(cl,
                               persistenceUnit,
                               entityType,
                               entityTypeName);
    }
  }

  public void introspectEntityListener(Class type,
                                       AmberPersistenceUnit persistenceUnit,
                                       EntityType sourceType,
                                       String sourceClassName)
    throws ConfigException
  {
    if (type == null) {
      throw new ConfigException(L.l("'{0}' is an unknown type for @EntityListeners annotated at class '{1}'",
                                    type.getName(),
                                    sourceClassName));
    }

    Class parentClass = type.getSuperclass();

    if (parentClass == null) {
      // java.lang.Object
      return;
    }
    /*
    else {
      // XXX: entity listener super-classes in a hierarchy might
      // not be annotated as entity listeners but they might have
      // @PreXxx or @PostXxx annotated methods. On the other hand,
      // needs to filter regular classes out.

      introspectEntityListener(parentClass, persistenceUnit,
                               sourceType, sourceClassName);
    }
    */

    // jpa/0r42

    ListenerType listenerType
      = persistenceUnit.getEntityListener(type.getName());

    ListenerType newListenerType
      = persistenceUnit.addEntityListener(sourceClassName, type);

    if (listenerType == null) {
      listenerType = newListenerType;
      introspectListener(type, listenerType);
    }

    sourceType.addListener(listenerType);
  }

  public void introspectListener(Class type,
                                 ListenerType listenerType)
    throws ConfigException
  {
    listenerType.setInstanceClassName(listenerType.getName() + "__ResinExt");

    for (Method method : type.getDeclaredMethods()) {
      introspectCallbacks(listenerType, method);
    }
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(Class type,
                                  EntityType entityType)
    throws ConfigException
  {
    getInternalExcludeDefaultListenersConfig(type, _annotationCfg);

    if (! _annotationCfg.isNull())
      entityType.setExcludeDefaultListeners(true);

    getInternalExcludeSuperclassListenersConfig(type, _annotationCfg);

    if (! _annotationCfg.isNull())
      entityType.setExcludeSuperclassListeners(true);

    for (Method method : type.getDeclaredMethods()) {
      introspectCallbacks(entityType, method);
    }
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(AbstractEnhancedType type,
                                  Method method)
    throws ConfigException
  {
    Class []param = method.getParameterTypes();

    String methodName = method.getName();
    Class jClass = type.getBeanClass();

    boolean isListener = type instanceof ListenerType;

    int n = ListenerType.CALLBACK_CLASS.length;

    for (int i = 1; i < n; i++) {
      getInternalCallbackConfig(i, jClass, method, methodName,
                                _annotationCfg);

      if (! _annotationCfg.isNull()) {
        validateCallback(ListenerType.CALLBACK_CLASS[i].getName(),
                         method, isListener);

        type.addCallback(i, method);
      }
    }
  }

  /**
   * Introspects named queries.
   */
  void introspectNamedQueries(Class type, String typeName)
  {
    // jpa/0y0-

    getInternalNamedQueryConfig(type, _annotationCfg);
    NamedQuery namedQueryAnn = (NamedQuery) _annotationCfg.getAnnotation();
    NamedQueryConfig namedQueryConfig = _annotationCfg.getNamedQueryConfig();

    // getInternalNamedQueriesConfig(type);
    NamedQueries namedQueriesAnn = (NamedQueries) type.getAnnotation(NamedQueries.class);
    // NamedQueriesConfig namedQueriesConfig = _annotationCfg.getNamedQueriesConfig();

    if ((namedQueryAnn == null) && (namedQueriesAnn == null))
      return;

    NamedQuery namedQueryArray[];

    if ((namedQueryAnn != null) && (namedQueriesAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @NamedQuery and @NamedQueries",
                                    typeName));
    }
    else if (namedQueriesAnn != null) {
      namedQueryArray = namedQueriesAnn.value();
    }
    else {
      namedQueryArray = new NamedQuery[] { namedQueryAnn };
    }

    for (int i=0; i < namedQueryArray.length; i++) {
      namedQueryAnn = namedQueryArray[i];
      _persistenceUnit.addNamedQuery(namedQueryAnn.name(),
                                     namedQueryAnn.query());
    }
  }

  /**
   * Introspects named native queries.
   */
  void introspectNamedNativeQueries(Class type, String typeName)
  {
    // jpa/0y2-

    getInternalNamedNativeQueryConfig(type, _annotationCfg);
    NamedNativeQuery namedNativeQueryAnn = (NamedNativeQuery) _annotationCfg.getAnnotation();
    NamedNativeQueryConfig namedNativeQueryConfig = _annotationCfg.getNamedNativeQueryConfig();

    NamedNativeQueries namedNativeQueriesAnn = (NamedNativeQueries) type.getAnnotation(NamedNativeQueries.class);

    if ((namedNativeQueryAnn == null) && (namedNativeQueriesAnn == null))
      return;

    NamedNativeQuery namedNativeQueryArray[];

    if ((namedNativeQueryAnn != null) && (namedNativeQueriesAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @NamedNativeQuery and @NamedNativeQueries",
                                    typeName));
    }
    else if (namedNativeQueriesAnn != null) {
      namedNativeQueryArray = namedNativeQueriesAnn.value();
    }
    else {
      namedNativeQueryArray = new NamedNativeQuery[] { namedNativeQueryAnn };
    }

    for (int i=0; i < namedNativeQueryArray.length; i++) {
      namedNativeQueryAnn = namedNativeQueryArray[i];

      NamedNativeQueryConfig nativeQueryConfig = new NamedNativeQueryConfig();

      nativeQueryConfig.setQuery(namedNativeQueryAnn.query());
      nativeQueryConfig.setResultClass(namedNativeQueryAnn.resultClass());
      nativeQueryConfig.setResultSetMapping(namedNativeQueryAnn.resultSetMapping());
      _persistenceUnit.addNamedNativeQuery(namedNativeQueryAnn.name(),
                                           nativeQueryConfig);
    }
  }

  /**
   * Introspects sql result set mappings.
   */
  void introspectSqlResultSetMappings(Class type,
                                      EntityType entityType,
                                      String typeName)
  {
    // jpa/0y1-

    getInternalSqlResultSetMappingConfig(type, _annotationCfg);
    SqlResultSetMapping sqlResultSetMappingAnn = (SqlResultSetMapping) _annotationCfg.getAnnotation();
    SqlResultSetMappingConfig sqlResultSetMappingConfig
      = _annotationCfg.getSqlResultSetMappingConfig();

    SqlResultSetMappings sqlResultSetMappingsAnn
      = (SqlResultSetMappings) type.getAnnotation(SqlResultSetMappings.class);

    if ((sqlResultSetMappingAnn == null) && (sqlResultSetMappingsAnn == null))
      return;

    SqlResultSetMapping sqlResultSetMappingArray[];

    if ((sqlResultSetMappingAnn != null) && (sqlResultSetMappingsAnn != null)) {
      throw new ConfigException(L.l("{0} may not have both @SqlResultSetMapping and @SqlResultSetMappings",
                                    typeName));
    }
    else if (sqlResultSetMappingsAnn != null) {
      sqlResultSetMappingArray = sqlResultSetMappingsAnn.value();
    }
    else {
      sqlResultSetMappingArray = new SqlResultSetMapping[] { sqlResultSetMappingAnn };
    }

    if (sqlResultSetMappingConfig != null) {
      _persistenceUnit.addSqlResultSetMapping(sqlResultSetMappingConfig.getName(),
                                              sqlResultSetMappingConfig);
      return;
    }

    for (int i=0; i < sqlResultSetMappingArray.length; i++) {
      sqlResultSetMappingAnn = sqlResultSetMappingArray[i];

      String name = sqlResultSetMappingAnn.name();
      EntityResult entities[] = sqlResultSetMappingAnn.entities();
      ColumnResult columns[] = sqlResultSetMappingAnn.columns();

      SqlResultSetMappingCompletion completion
        = new SqlResultSetMappingCompletion(entityType, name,
                                            entities, columns);

      _depCompletions.add(completion);
    }
  }

  /**
   * Completion callback for sql result set mappings.
   */
  void addSqlResultSetMapping(String resultSetName,
                              EntityResult entities[],
                              ColumnResult columns[])
    throws ConfigException
  {
    // jpa/0y1-

    SqlResultSetMappingConfig sqlResultSetMapping
      = new SqlResultSetMappingConfig();

    // Adds @EntityResult.
    for (int i=0; i < entities.length; i++) {
      EntityResult entityResult = entities[i];

      String className = entityResult.entityClass().getName();

      EntityType resultType = _persistenceUnit.getEntityType(className);

      if (resultType == null)
        throw new ConfigException(L.l("entityClass '{0}' is not an @Entity bean for @SqlResultSetMapping '{1}'. The entityClass of an @EntityResult must be an @Entity bean.",
                                      className,
                                      resultSetName));

      EntityResultConfig entityResultConfig = new EntityResultConfig();

      entityResultConfig.setEntityClass(className);

      // @FieldResult annotations.
      FieldResult fields[] = entityResult.fields();

      for (int j=0; j < fields.length; j++) {
        FieldResult fieldResult = fields[j];

        String fieldName = fieldResult.name();

        AmberField field = resultType.getField(fieldName);

        if (field == null)
          throw new ConfigException(L.l("@FieldResult with field name '{0}' is not a field for @EntityResult bean '{1}' in @SqlResultSetMapping '{2}'",
                                        fieldName,
                                        className,
                                        resultSetName));

        String columnName = fieldResult.column();

        if (columnName == null || columnName.length() == 0)
          throw new ConfigException(L.l("@FieldResult must have a column name defined and it must not be empty for '{0}' in @EntityResult '{1}' @SqlResultSetMapping '{2}'",
                                        fieldName,
                                        className,
                                        resultSetName));

        FieldResultConfig fieldResultConfig = new FieldResultConfig();

        fieldResultConfig.setName(fieldName);
        fieldResultConfig.setColumn(columnName);

        entityResultConfig.addFieldResult(fieldResultConfig);
      }

      sqlResultSetMapping.addEntityResult(entityResultConfig);
    }

    // Adds @ColumnResult.
    for (int i=0; i < columns.length; i++) {
      ColumnResult columnResult = columns[i];

      String columnName = columnResult.name();

      if (columnName == null || columnName.length() == 0)
        throw new ConfigException(L.l("@ColumnResult must have a column name defined and it must not be empty in @SqlResultSetMapping '{0}'",
                                      resultSetName));

      ColumnResultConfig columnResultConfig = new ColumnResultConfig();

      columnResultConfig.setName(columnName);

      sqlResultSetMapping.addColumnResult(columnResultConfig);
    }

    // Adds a global sql result set mapping to the persistence unit.
    _persistenceUnit.addSqlResultSetMapping(resultSetName,
                                            sqlResultSetMapping);
  }

  /**
   * Completes all partial bean introspection.
   */
  public void configureLinks()
    throws ConfigException
  {
    RuntimeException exn = null;

    while (_linkCompletions.size() > 0) {
      Completion completion = _linkCompletions.remove(0);

      try {
        completion.complete();
      } catch (Exception e) {
        if (e instanceof ConfigException) {
          log.warning(e.getMessage());
          log.log(Level.FINEST, e.toString(), e);
        }
        else
          log.log(Level.WARNING, e.toString(), e);

        completion.getRelatedType().setConfigException(e);

        if (exn == null)
          exn = ConfigException.create(e);
      }
    }

    if (exn != null)
      throw exn;
  }

  /**
   * Completes all partial bean introspection.
   */
  public void configureDependencies()
    throws ConfigException
  {
    RuntimeException exn = null;

    while (_depCompletions.size() > 0) {
      Completion completion = _depCompletions.remove(0);

      try {
        completion.complete();
      } catch (Exception e) {
        if (e instanceof ConfigException) {
          log.warning(e.getMessage());
          log.log(Level.FINEST, e.toString(), e);
        }
        else
          log.log(Level.WARNING, e.toString(), e);

        completion.getRelatedType().setConfigException(e);

        if (exn == null)
          exn = ConfigException.create(e);
      }
    }

    if (exn != null)
      throw exn;
  }

  /**
   * Introspects the fields.
   */
  void introspectIdMethod(AmberPersistenceUnit persistenceUnit,
                          EntityType entityType,
                          EntityType parentType,
                          Class type,
                          Class idClass,
                          MappedSuperclassConfig config)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    IdField idField = null;

    AttributesConfig attributesConfig = null;

    if (config != null)
      attributesConfig = config.getAttributes();

    for (Method method : type.getDeclaredMethods()) {
      String methodName = method.getName();
      Class []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().equals(Object.class))
        continue;

      if (! methodName.startsWith("get") || paramTypes.length != 0) {
        continue;
      }

      String fieldName = toFieldName(methodName.substring(3));

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      getInternalIdConfig(type, method, fieldName, _annotationCfg);
      Annotation id = _annotationCfg.getAnnotation();
      IdConfig idConfig = _annotationCfg.getIdConfig();

      if (! _annotationCfg.isNull()) {
        idField = introspectId(persistenceUnit,
                               entityType,
                               method,
                               fieldName,
                               method.getReturnType(),
                               idConfig);

        if (idField != null)
          keys.add(idField);
      }
      else {
        getInternalEmbeddedIdConfig(type, method, fieldName, _annotationCfg);
        Annotation embeddedId = _annotationCfg.getAnnotation();
        EmbeddedIdConfig embeddedIdConfig = _annotationCfg.getEmbeddedIdConfig();

        if (! _annotationCfg.isNull()) {
          idField = introspectEmbeddedId(persistenceUnit,
                                         entityType,
                                         method,
                                         fieldName,
                                         method.getReturnType());
          break;
        }
        else {
          continue;
        }
      }
    }

    if (keys.size() == 0) {
      if (idField != null) {
        // @EmbeddedId was used.
        com.caucho.amber.field.EmbeddedId id
          = new com.caucho.amber.field.EmbeddedId(entityType, (EmbeddedIdField) idField);

        entityType.setId(id);
      }
    }
    else if (keys.size() == 1) {
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    }
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id methods, but no @IdClass.  Compound primary keys require either an @IdClass or exactly one @EmbeddedId field or property.",
                                    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);

      entityType.setId(id);
    }
  }

  /**
   * Introspects the fields.
   */
  void introspectIdField(AmberPersistenceUnit persistenceUnit,
                         EntityType entityType,
                         EntityType parentType,
                         Class type,
                         Class idClass,
                         MappedSuperclassConfig config)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    AttributesConfig attributesConfig = null;

    if (config != null)
      attributesConfig = config.getAttributes();

    for (Field field : type.getDeclaredFields()) {
      String fieldName = field.getName();

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      getInternalIdConfig(type, field, fieldName, _annotationCfg);
      Annotation id = _annotationCfg.getAnnotation();
      IdConfig idConfig = _annotationCfg.getIdConfig();

      if (_annotationCfg.isNull()) {
        getInternalEmbeddedIdConfig(type, field, fieldName, _annotationCfg);
        Annotation embeddedId = _annotationCfg.getAnnotation();
        EmbeddedIdConfig embeddedIdConfig = _annotationCfg.getEmbeddedIdConfig();

        if (_annotationCfg.isNull())
          continue;
      }

      IdField idField = introspectId(persistenceUnit,
                                     entityType,
                                     field,
                                     fieldName,
                                     field.getType(),
                                     idConfig);

      if (idField != null)
        keys.add(idField);
    }

    if (keys.size() == 0) {
    }
    else if (keys.size() == 1)
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id fields, but no @IdClass.  Compound primary keys require an @IdClass.",
                                    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);

      entityType.setId(id);

      _configManager.introspect(idClass);
    }
  }

  /**
   * Check if it's field
   */
  boolean isField(Class type,
                  AbstractEnhancedConfig typeConfig,
                  boolean isEmbeddable)
    throws ConfigException
  {
    if (type == null)
      return false;

    if (typeConfig != null) {
      String access = typeConfig.getAccess();

      if (access != null)
        return access.equals("FIELD");

      Class parentClass = type.getSuperclass();

      if (parentClass == null)
        return false;
      else {
        getInternalEntityConfig(parentClass, _annotationCfg);
        EntityConfig superEntityConfig = _annotationCfg.getEntityConfig();

        if (superEntityConfig == null)
          return false;

        return isField(parentClass, superEntityConfig, false);
      }
    }

    for (Field field : type.getDeclaredFields()) {
      for (Class annType : _annTypes) {
        if (field.getAnnotation(annType) != null) {
          return true;
        }
      }
    }

    return isField(type.getSuperclass(), null, false);
  }

  private IdField introspectId(AmberPersistenceUnit persistenceUnit,
                               EntityType entityType,
                               AccessibleObject field,
                               String fieldName,
                               Class fieldType,
                               IdConfig idConfig)
    throws ConfigException, SQLException
  {
    javax.persistence.Id id = field.getAnnotation(javax.persistence.Id.class);
    Column column = field.getAnnotation(javax.persistence.Column.class);

    ColumnConfig columnConfig = null;
    GeneratedValueConfig generatedValueConfig = null;

    if (idConfig != null) {
      columnConfig = idConfig.getColumn();
      generatedValueConfig = idConfig.getGeneratedValue();
    }

    GeneratedValue gen = field.getAnnotation(GeneratedValue.class);

    AmberType amberType = persistenceUnit.createType(fieldType);

    KeyPropertyField idField;

    AmberColumn keyColumn = null;
    keyColumn = createColumn(entityType,
                             field,
                             fieldName,
                             column,
                             amberType,
                             columnConfig);


    if (entityType.getTable() != null) {
      idField = new KeyPropertyField(entityType, fieldName, keyColumn);
    }
    else {
      idField = new KeyPropertyField(entityType, fieldName, keyColumn);
      return idField;
    }

    if (gen == null) {
    }
    else {
      JdbcMetaData metaData = null;

      /* XXX: validation needs to occur later
      try {
        metaData = persistenceUnit.getMetaData();
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException(L.l("Unable to get meta data for database. Meta data is needed for generated values."), e);
      }
      */

      if (GenerationType.IDENTITY.equals(gen.strategy())) {
        /* XXX: validation later
        if (! metaData.supportsIdentity())
          throw new ConfigException(L.l("'{0}' does not support identity.",
                                        metaData.getDatabaseName()));
        */

        keyColumn.setGeneratorType("identity");
        idField.setGenerator("identity");
      }
      else if (GenerationType.SEQUENCE.equals(gen.strategy())) {
        /* XXX: validation later
        if (! metaData.supportsSequences())
          throw new ConfigException(L.l("'{0}' does not support sequence.",
                                        metaData.getDatabaseName()));
        */

        addSequenceIdGenerator(persistenceUnit, idField, gen);
      }
      else if (GenerationType.TABLE.equals(gen.strategy())) {
        addTableIdGenerator(persistenceUnit, idField, id);
      }
      else if (GenerationType.AUTO.equals(gen.strategy())) {
        keyColumn.setGeneratorType("auto");
        idField.setGenerator("auto");

        /* XXX: validation later
        if (metaData.supportsIdentity()) {
          keyColumn.setGeneratorType("identity");
          idField.setGenerator("identity");
        }
        else if (metaData.supportsSequences()) {
          addSequenceIdGenerator(persistenceUnit, idField, gen);
        }
        else {
          addTableIdGenerator(persistenceUnit, idField, id);
        }
        */
      }
    }

    return idField;
  }

  private IdField introspectEmbeddedId(AmberPersistenceUnit persistenceUnit,
                                       EntityType ownerType,
                                       AccessibleObject field,
                                       String fieldName,
                                       Class fieldType)
    throws ConfigException, SQLException
  {
    IdField idField;

    EmbeddableType embeddableType
      = (EmbeddableType) _configManager.introspect(fieldType);

    if (embeddableType == null)
      throw new IllegalStateException("" + fieldType + " is an unsupported embeddable type");

    idField = new EmbeddedIdField(ownerType, embeddableType, fieldName);

    return idField;
  }

  void addSequenceIdGenerator(AmberPersistenceUnit persistenceUnit,
                              KeyPropertyField idField,
                              GeneratedValue genAnn)
    throws ConfigException
  {
    idField.setGenerator("sequence");
    idField.getColumn().setGeneratorType("sequence");

    String name = genAnn.generator();

    if (name == null || "".equals(name))
      name = idField.getEntitySourceType().getTable().getName() + "_cseq";

    IdGenerator gen = persistenceUnit.createSequenceGenerator(name, 1);

    idField.getEntitySourceType().setGenerator(idField.getName(), gen);
  }

  void addTableIdGenerator(AmberPersistenceUnit persistenceUnit,
                           KeyPropertyField idField,
                           javax.persistence.Id idAnn)
    throws ConfigException
  {
    idField.setGenerator("table");
    idField.getColumn().setGeneratorType("table");

    String name = null;// XXX: idAnn.name();
    if (name == null || "".equals(name))
      name = "caucho";

    IdGenerator gen = persistenceUnit.getTableGenerator(name);

    if (gen == null) {
      String genName = "GEN_TABLE";

      GeneratorTableType genTable;
      genTable = persistenceUnit.createGeneratorTable(genName);

      gen = genTable.createGenerator(name);

      // jpa/0g60
      genTable.init();

      persistenceUnit.putTableGenerator(name, gen);
    }

    idField.getEntitySourceType().setGenerator(idField.getName(), gen);
  }

  /**
   * Links a secondary table.
   */
  void linkSecondaryTable(AmberTable primaryTable,
                          AmberTable secondaryTable,
                          PrimaryKeyJoinColumn []joinColumnsAnn)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (AmberColumn column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      PrimaryKeyJoinColumn joinAnn
        = getJoinColumn(joinColumnsAnn, column.getName());
      String name;

      if (joinAnn == null)
        name = column.getName();
      else
        name = joinAnn.name();

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
                                       primaryTable,
                                       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);
  }

  /**
   * Links a secondary table.
   */
  void linkInheritanceTable(AmberTable primaryTable,
                            AmberTable secondaryTable,
                            PrimaryKeyJoinColumn joinAnn,
                            PrimaryKeyJoinColumnConfig pkJoinColumnCfg)
    throws ConfigException
  {
    PrimaryKeyJoinColumn joinAnns[] = null;

    if (joinAnn != null)
      joinAnns = new PrimaryKeyJoinColumn[] { joinAnn };

    linkInheritanceTable(primaryTable,
                         secondaryTable,
                         joinAnns,
                         pkJoinColumnCfg);
  }

  /**
   * Links a secondary table.
   */
  void linkInheritanceTable(AmberTable primaryTable,
                            AmberTable secondaryTable,
                            PrimaryKeyJoinColumn []joinColumnsAnn,
                            PrimaryKeyJoinColumnConfig pkJoinColumnCfg)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (AmberColumn column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      String name;

      if (joinColumnsAnn == null) {

        if (pkJoinColumnCfg == null)
          name = column.getName();
        else
          name = pkJoinColumnCfg.getName();
      }
      else {
        PrimaryKeyJoinColumn join;

        join = getJoinColumn(joinColumnsAnn, column.getName());

        if (join == null)
          name = column.getName();
        else
          name = join.name();
      }

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
                                       primaryTable,
                                       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);

    // jpa/0l48
    //    link = new LinkColumns(primaryTable,
    //                           secondaryTable,
    //                           linkColumns);
    //
    //    link.setSourceCascadeDelete(true);
    //
    //    primaryTable.setDependentIdLink(link);
  }

  /**
   * Introspects the methods.
   */
  void introspectMethods(AmberPersistenceUnit persistenceUnit,
                         BeanType entityType,
                         BeanType parentType,
                         Class type,
                         AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    for (Method method : type.getDeclaredMethods()) {
      String methodName = method.getName();
      Class []paramTypes = method.getParameterTypes();

      if (method.getDeclaringClass().equals(Object.class))
        continue;

      // jpa/0r38
      // Callbacks are introspected in the main introspect() block.
      // introspectCallbacks(entityType, method);

      String propName;

      if (paramTypes.length != 0) {
        validateNonGetter(method);
        continue;
      }
      else if (methodName.startsWith("get")) {
        propName = methodName.substring(3);
      }
      else if (methodName.startsWith("is")
               && (method.getReturnType().equals(boolean.class)
                   || method.getReturnType().equals(Boolean.class))) {
        propName = methodName.substring(2);
      }
      else {
        validateNonGetter(method);
        continue;
      }

      getInternalVersionConfig(type, method, propName, _annotationCfg);
      Annotation versionAnn = _annotationCfg.getAnnotation();
      VersionConfig versionConfig = _annotationCfg.getVersionConfig();

      if (! _annotationCfg.isNull()) {
        validateNonGetter(method);
      }
      else {
        Method setter = null;
        
        try {
          setter = getMethod(type,
                             "set" + propName,
                             new Class[] { method.getReturnType() });
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
        }
        
        if (Modifier.isPrivate(method.getModifiers())
            || setter == null
            || Modifier.isPrivate(setter.getModifiers())) {
          Annotation ann = isAnnotatedMethod(method);

          if (ann == null) {
            if (setter != null)
              ann = isAnnotatedMethod(setter);
          }
          else if (ann instanceof Transient)
            continue;

          if (ann != null) {
            throw error(method, L.l("'{0}' is not a valid annotation for {1}.  Only public persistent property getters with matching setters may have property annotations.",
                                          ann.getClass(), getFullName(method)));
          }

          continue;
        }

        // ejb/0g03 for private
        if (Modifier.isStatic(method.getModifiers())) { // || ! method.isPublic()) {
          validateNonGetter(method);
          continue;
        }
      }

      String fieldName = toFieldName(propName);

      if (containsFieldOrCompletion(parentType, fieldName))
        continue;

      Class fieldType = method.getReturnType();

      introspectField(persistenceUnit, entityType, method,
                      fieldName, fieldType, typeConfig);
    }
  }

  private Method getMethod(Class cl, String name, Class []param)
  {
    if (cl == null)
      return null;

    loop:
    for (Method method : cl.getDeclaredMethods()) {
      if (! method.getName().equals(name))
        continue;

      Class []types = method.getParameterTypes();

      if (types.length != param.length)
        continue;
      
      for (int i = 0; i < types.length; i++) {
        if (! param[i].equals(types[i]))
          continue loop;
      }

      return method;
    }

    return getMethod(cl.getSuperclass(), name, param);
  }

  /**
   * Introspects the fields.
   */
  void introspectFields(AmberPersistenceUnit persistenceUnit,
                        BeanType entityType,
                        BeanType parentType,
                        Class type,
                        AbstractEnhancedConfig typeConfig,
                        boolean isEmbeddable)
    throws ConfigException
  {
    if (entityType.isEntity() && ((EntityType) entityType).getId() == null)
      throw new ConfigException(L.l("{0} has no key", entityType));

    for (Field field : type.getDeclaredFields()) {
      String fieldName = field.getName();

      if (containsFieldOrCompletion(parentType, fieldName)) {
        continue;
      }

      if (Modifier.isStatic(field.getModifiers())
          || Modifier.isTransient(field.getModifiers()))
        continue;

      Class fieldType = field.getType();
      introspectField(persistenceUnit, entityType, field,
                      fieldName, fieldType, typeConfig);
    }
  }

  void introspectField(AmberPersistenceUnit persistenceUnit,
                       BeanType sourceType,
                       AccessibleObject field,
                       String fieldName,
                       Class fieldType,
                       AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    EmbeddableConfig embeddableConfig = null;
    MappedSuperclassConfig mappedSuperOrEntityConfig = null;

    if (typeConfig instanceof EmbeddableConfig)
      embeddableConfig = (EmbeddableConfig) typeConfig;
    else if (typeConfig instanceof MappedSuperclassConfig)
      mappedSuperOrEntityConfig = (MappedSuperclassConfig) typeConfig;

    // jpa/0r37: interface fields must not be considered.

    Class jClass;
    
    if (field instanceof Field)
      jClass = ((Field) field).getDeclaringClass();
    else
      jClass = ((Method) field).getDeclaringClass();

    if (jClass.isInterface())
      return;

    // jpa/0r37: fields declared in non-entity superclasses
    // must not be considered.

    BeanType declaringType;

    declaringType = _persistenceUnit.getEntityType(jClass.getName());

    if (declaringType == null)
      declaringType = _persistenceUnit.getEmbeddable(jClass.getName());

    if (declaringType == null)
      declaringType = _persistenceUnit.getMappedSuperclass(jClass.getName());

    if (declaringType == null)
      return;

    AttributesConfig attributesConfig = null;
    IdConfig idConfig = null;
    BasicConfig basicConfig = null;
    OneToOneConfig oneToOneConfig = null;
    OneToManyConfig oneToManyConfig = null;
    ManyToOneConfig manyToOneConfig = null;
    ManyToManyConfig manyToManyConfig = null;
    VersionConfig versionConfig = null;
    ElementCollectionConfig elementCollectionConfig = null;

    if (mappedSuperOrEntityConfig != null) {
      attributesConfig = mappedSuperOrEntityConfig.getAttributes();

      if (attributesConfig != null) {
        idConfig = attributesConfig.getId(fieldName);

        basicConfig = attributesConfig.getBasic(fieldName);

        oneToOneConfig = attributesConfig.getOneToOne(fieldName);

        oneToManyConfig = attributesConfig.getOneToMany(fieldName);

        elementCollectionConfig = null; // attributesConfig.getOneToMany(fieldName);

        manyToOneConfig = attributesConfig.getManyToOne(fieldName);

        manyToManyConfig = attributesConfig.getManyToMany(fieldName);

        versionConfig = attributesConfig.getVersion(fieldName);
      }
    }

    if (idConfig != null
        || field.isAnnotationPresent(javax.persistence.Id.class)) {
      validateAnnotations(field, fieldName, "@Id", _idAnnotations);

      if (! _idTypes.contains(fieldType.getName())) {
        throw error(field, L.l("{0} is an invalid @Id type for {1}.",
                               fieldType.getName(), fieldName));
      }
    }
    else if (basicConfig != null
             || field.isAnnotationPresent(javax.persistence.Basic.class)) {
      validateAnnotations(field, fieldName, "@Basic", _basicAnnotations);

      BasicConfig basic
        = new BasicConfig(this, sourceType, field, fieldName, fieldType);
      
      basic.complete();
    }
    else if ((versionConfig != null)
             || field.isAnnotationPresent(javax.persistence.Version.class)) {
      validateAnnotations(field, fieldName, "@Version", _versionAnnotations);

      addVersion((EntityType) sourceType, field,
                 fieldName, fieldType, versionConfig);
    }
    else if (field.isAnnotationPresent(javax.persistence.ManyToOne.class)) {
      validateAnnotations(field, fieldName, "@ManyToOne", _manyToOneAnnotations);

      ManyToOne ann = field.getAnnotation(ManyToOne.class);

      Class targetEntity = null;

      if (ann != null)
        targetEntity = ann.targetEntity();
      else {
        targetEntity = manyToOneConfig.getTargetEntity();
     }

      if (targetEntity == null ||
          targetEntity.getName().equals("void")) {
        targetEntity = fieldType;
      }

      getInternalEntityConfig(targetEntity, _annotationCfg);
      Annotation targetEntityAnn = _annotationCfg.getAnnotation();
      EntityConfig targetEntityConfig = _annotationCfg.getEntityConfig();

      if (_annotationCfg.isNull()) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne relations must target a valid @Entity.",
                               targetEntity.getName(), fieldName));
      }

      if (! fieldType.isAssignableFrom(targetEntity)) {
        throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne targetEntity must be assignable to the field type '{2}'.",
                               targetEntity.getName(),
                               fieldName,
                               fieldType.getName()));
      }

      EntityType entityType = (EntityType) sourceType;

      entityType.setHasDependent(true);

      _linkCompletions.add(new ManyToOneConfig(this,
                                               entityType,
                                               field,
                                               fieldName,
                                               fieldType));
    }
    else if (oneToManyConfig != null
             || field.isAnnotationPresent(javax.persistence.OneToMany.class)) {
      validateAnnotations(field, fieldName, "@OneToMany", _oneToManyAnnotations);

      if (field.isAnnotationPresent(javax.persistence.MapKey.class)) {
        if (!fieldType.getName().equals("java.util.Map")) {
          throw error(field, L.l("'{0}' is an illegal @OneToMany/@MapKey type for {1}. @MapKey must be a java.util.Map",
                                 fieldType.getName(),
                                 fieldName));
        }
      }
      else if (! _oneToManyTypes.contains(fieldType.getName())) {
        throw error(field, L.l("'{0}' is an illegal @OneToMany type for {1}.  @OneToMany must be a java.util.Collection, java.util.List or java.util.Map",
                               fieldType.getName(),
                               fieldName));
      }

      EntityType entityType = (EntityType) sourceType;

      _depCompletions.add(new OneToManyConfig(this,
                                              entityType,
                                              field,
                                              fieldName,
                                              fieldType));
    }
    else if ((oneToOneConfig != null)
             || field.isAnnotationPresent(javax.persistence.OneToOne.class)) {
      validateAnnotations(field, fieldName, "@OneToOne", _oneToOneAnnotations);

      EntityType entityType = (EntityType) sourceType;

      OneToOneConfig oneToOne
        = new OneToOneConfig(this, entityType, field, fieldName, fieldType);
      
      if (oneToOne.isOwningSide())
        _linkCompletions.add(oneToOne);
      else {
        _depCompletions.add(0, oneToOne);
        entityType.setHasDependent(true);
      }
    }
    else if ((manyToManyConfig != null)
             || field.isAnnotationPresent(javax.persistence.ManyToMany.class)) {

      if (field.isAnnotationPresent(javax.persistence.MapKey.class)) {
        if (! fieldType.getName().equals("java.util.Map")) {
          throw error(field, L.l("'{0}' is an illegal @ManyToMany/@MapKey type for {1}. @MapKey must be a java.util.Map",
                                 fieldType.getName(),
                                 fieldName));
        }
      }

      ManyToManyConfig manyToMany
        = new ManyToManyConfig(this, (EntityType) sourceType, field, fieldName, fieldType);

      if (manyToMany.isOwningSide())
        _linkCompletions.add(manyToMany);
      else
        _depCompletions.add(manyToMany);
    }
    else if (elementCollectionConfig != null
             || field.isAnnotationPresent(javax.persistence.ElementCollection.class)) {
      validateAnnotations(field, fieldName, "@ElementCollection", _elementCollectionAnnotations);
      if (! _elementCollectionTypes.contains(fieldType.getName())) {
        throw error(field, L.l("'{0}' is an illegal @ElementCollection type for {1}.  @ElementCollection must be a java.util.Collection, java.util.List or java.util.Map",
                               fieldType.getName(),
                               fieldName));
      }

      EntityType entityType = (EntityType) sourceType;

      ElementCollectionConfig comp
        = new ElementCollectionConfig(entityType,
                                      field,
                                      fieldName,
                                      fieldType);

      _depCompletions.add(comp);
    }
    else if (field.isAnnotationPresent(javax.persistence.Embedded.class)) {
      validateAnnotations(field, fieldName, "@Embedded", _embeddedAnnotations);

      EntityType entityType = (EntityType) sourceType;

      entityType.setHasDependent(true);

      _depCompletions.add(new EmbeddedCompletion(entityType,
                                                 field,
                                                 fieldName,
                                                 fieldType,
                                                 false));
    }
    else if (field.isAnnotationPresent(javax.persistence.EmbeddedId.class)) {
      validateAnnotations(field, fieldName, "@EmbeddedId", _embeddedIdAnnotations);

      _depCompletions.add(new EmbeddedCompletion((EntityType) sourceType,
                                                 field,
                                                 fieldName,
                                                 fieldType,
                                                 true));
    }
    else if (field.isAnnotationPresent(javax.persistence.Transient.class)) {
    }
    else {
      BasicConfig basic
        = new BasicConfig(this, sourceType, field, fieldName, fieldType);
      
      basic.complete();
    }
  }

  void addVersion(EntityType sourceType,
                  AccessibleObject field,
                  String fieldName,
                  Class fieldType,
                  VersionConfig versionConfig)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    Column columnAnn = field.getAnnotation(Column.class);

    ColumnConfig columnConfig = null;

    if (versionConfig != null)
      columnConfig = versionConfig.getColumn();

    if (! _versionTypes.contains(fieldType.getName())) {
      throw error(field, L.l("{0} is an invalid @Version type for {1}.",
                             fieldType.getName(), fieldName));
    }

    AmberType amberType = persistenceUnit.createType(fieldType);

    AmberColumn fieldColumn = createColumn(sourceType, field, fieldName,
                                      columnAnn, amberType, columnConfig);

    VersionField version = new VersionField(sourceType, fieldName);
    version.setColumn(fieldColumn);

    sourceType.setVersionField(version);
  }

  private AmberColumn createColumn(BeanType beanType,
                              AccessibleObject field,
                              String fieldName,
                              Column columnAnn,
                              AmberType amberType,
                              ColumnConfig columnConfig)
    throws ConfigException
  {
    EntityType entityType = null;

    if (beanType instanceof EntityType)
      entityType = (EntityType) beanType;
    
    String name;

    if (columnAnn != null && ! columnAnn.name().equals(""))
      name = (String) columnAnn.name();
    else if (columnConfig != null && ! columnConfig.getName().equals(""))
      name = columnConfig.getName();
    else
      name = toSqlName(fieldName);

    AmberColumn column = null;

    if (entityType == null) { // embeddable
      column = new AmberColumn(null, name, amberType);
    }
    else if (columnAnn != null && ! columnAnn.table().equals("")) {
      String tableName = columnAnn.table();
      AmberTable table;

      table = entityType.getSecondaryTable(tableName);

      if (table == null)
        throw error(field, L.l("{0} @Column(table='{1}') is an unknown secondary table.",
                               fieldName,
                               tableName));

      column = table.createColumn(name, amberType);
    }
    else if (entityType.getTable() != null)
      column = entityType.getTable().createColumn(name, amberType);
    else { // jpa/0ge2: MappedSuperclassType
      column = new AmberColumn(null, name, amberType);
    }

    if (column != null && columnAnn != null) {
      // primaryKey = column.primaryKey();
      column.setUnique(columnAnn.unique());
      column.setNotNull(! columnAnn.nullable());
      //insertable = column.insertable();
      //updateable = column.updatable();
      if (! "".equals(columnAnn.columnDefinition()))
        column.setSQLType(columnAnn.columnDefinition());
      column.setLength(columnAnn.length());
      int precision = columnAnn.precision();
      if (precision < 0) {
        throw error(field, L.l("{0} @Column precision cannot be less than 0.",
                               fieldName));
      }

      int scale = columnAnn.scale();
      if (scale < 0) {
        throw error(field, L.l("{0} @Column scale cannot be less than 0.",
                               fieldName));
      }

      // this test implicitly works for case where
      // precision is not set explicitly (ie: set to 0 by default)
      // and scale is set
      if (scale > precision) {
        throw error(field, L.l("{0} @Column scale cannot be greater than precision. Must set precision to a non-zero value before setting scale.",
                               fieldName));
      }

      if (precision > 0) {
        column.setPrecision(precision);
        column.setScale(scale);
      }
    }

    return column;
  }

  public static JoinColumn getJoinColumn(JoinColumns joinColumns,
                                         String keyName)
  {
    if (joinColumns == null)
      return null;

    return getJoinColumn(joinColumns.value(), keyName);
  }

  private AmberColumn findColumn(ArrayList<AmberColumn> columns, String ref)
  {
    if (((ref == null) || ref.equals("")) && columns.size() == 1)
      return columns.get(0);

    for (AmberColumn column : columns) {
      if (column.getName().equals(ref))
        return column;
    }

    return null;
  }

  public static JoinColumn getJoinColumn(JoinColumn []columnsAnn,
                                          String keyName)
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return null;

    for (int i = 0; i < columnsAnn.length; i++) {
      String ref = columnsAnn[i].referencedColumnName();

      if (ref.equals("") || ref.equals(keyName))
        return columnsAnn[i];
    }

    return null;
  }

  public static PrimaryKeyJoinColumn getJoinColumn(PrimaryKeyJoinColumn []columnsAnn,
                                                   String keyName)
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return null;

    for (int i = 0; i < columnsAnn.length; i++) {
      String ref = columnsAnn[i].referencedColumnName();

      if (ref.equals("") || ref.equals(keyName))
        return columnsAnn[i];
    }

    return null;
  }

  /**
   * completes for dependent
   */
  class EmbeddedCompletion extends CompletionImpl {
    private AccessibleObject _field;
    private String _fieldName;
    private Class _fieldType;
    // The same completion is used for both:
    // @Embedded or @EmbeddedId
    private boolean _embeddedId;

    EmbeddedCompletion(EntityType type,
                       AccessibleObject field,
                       String fieldName,
                       Class fieldType,
                       boolean embeddedId)
    {
      super(BaseConfigIntrospector.this, type, fieldName);

      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
      _embeddedId = embeddedId;
    }

    @Override
    public void complete()
      throws ConfigException
    {
      getInternalAttributeOverrideConfig(_entityType.getBeanClass(),
                                         _annotationCfg);
      AttributeOverride attributeOverrideAnn = (AttributeOverride) _annotationCfg.getAnnotation();
      AttributeOverrideConfig attributeOverrideConfig = _annotationCfg.getAttributeOverrideConfig();

      boolean hasAttributeOverride = ! _annotationCfg.isNull();

      AttributeOverrides attributeOverridesAnn = _field.getAnnotation(AttributeOverrides.class);

      boolean hasAttributeOverrides = (attributeOverridesAnn != null);

      if (hasAttributeOverride && hasAttributeOverrides) {
        throw error(_field, L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
                                _fieldName));
      }

      AttributeOverride attOverridesAnn[] = null;

      if (attributeOverrideAnn != null) {
        attOverridesAnn = new AttributeOverride[] { attributeOverrideAnn };
      }
      else if (attributeOverridesAnn != null) {
        attOverridesAnn = attributeOverridesAnn.value();
      }

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      EmbeddableType type = persistenceUnit.createEmbeddable(_fieldType);

      EntityEmbeddedField embeddedField;

      if (_embeddedId) {
        embeddedField = _entityType.getId().getEmbeddedIdField();
      } else {
        embeddedField = new EntityEmbeddedField(_entityType, type, _fieldName);
      }

      // embeddedField.setEmbeddedId(_embeddedId);

      embeddedField.setLazy(false);

      _entityType.addField(embeddedField);

      // XXX: todo ...
      // validateAttributeOverrides(_field, attributeOverridesAnn, type);

      AmberTable sourceTable = _entityType.getTable();

      HashMap<String, AmberColumn> embeddedColumns = new HashMap<String, AmberColumn>();
      HashMap<String, String> fieldNameByColumn = new HashMap<String, String>();

      for (EmbeddedSubField subField : embeddedField.getSubFields()) {
        String embeddedFieldName = subField.getName();

        String columnName = toSqlName(embeddedFieldName);
        boolean notNull = false;
        boolean unique = false;

        if (attOverridesAnn != null) {
          for (int j= 0; j < attOverridesAnn.length; j++) {

            if (embeddedFieldName.equals(attOverridesAnn[j].name())) {

              Column columnAnn = attOverridesAnn[j].column();

              if (columnAnn != null) {
                columnName = columnAnn.name();
                notNull = ! columnAnn.nullable();
                unique = columnAnn.unique();

                subField.getColumn().setName(columnName);
                subField.getColumn().setNotNull(notNull);
                subField.getColumn().setUnique(unique);
              }
            }
          }
        }

        /*
        AmberType amberType = _persistenceUnit.createType(fields.get(i).getJavaType().getName());

        AmberColumn column = sourceTable.createColumn(columnName, amberType);

        column.setNotNull(notNull);
        column.setUnique(unique);

        embeddedColumns.put(columnName, column);
        fieldNameByColumn.put(columnName, embeddedFieldName);
        */
      }

      /*
      embeddedField.setEmbeddedColumns(embeddedColumns);
      embeddedField.setFieldNameByColumn(fieldNameByColumn);
      */

      embeddedField.init();
    }
  }

  /**
   * completes for dependent
   */
  class SqlResultSetMappingCompletion extends CompletionImpl {
    private String _name;
    private EntityResult _entities[];
    private ColumnResult _columns[];

    SqlResultSetMappingCompletion(EntityType type,
                                  String name,
                                  EntityResult entities[],
                                  ColumnResult columns[])
    {
      super(BaseConfigIntrospector.this, type);

      _name = name;
      _entities = entities;
      _columns = columns;
    }

    @Override
    public void complete()
      throws ConfigException
    {
      addSqlResultSetMapping(_name,
                             _entities,
                             _columns);
    }
  }

  void getInternalEmbeddableConfig(Class type,
                                   AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, Embeddable.class);

    EmbeddableConfig embeddableConfig = null;

    if (_embeddableConfigMap != null)
      embeddableConfig = _embeddableConfigMap.get(type.getName());

    annotationCfg.setConfig(embeddableConfig);
  }

  void getInternalEntityConfig(Class type, AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, Entity.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    annotationCfg.setConfig(entityConfig);
  }

  void getInternalMappedSuperclassConfig(Class type,
                                         AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, MappedSuperclass.class);

    MappedSuperclassConfig mappedSuperConfig
      = getMappedSuperclassConfig(type.getName());

    annotationCfg.setConfig(mappedSuperConfig);
  }

  void getInternalEntityListenersConfig(Class type,
                                        AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, EntityListeners.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    annotationCfg.setConfig(entityConfig.getEntityListeners());
  }

  void getInternalExcludeDefaultListenersConfig(Class type,
                                                AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, ExcludeDefaultListeners.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    if (entityConfig.getExcludeDefaultListeners())
      annotationCfg.setConfig(entityConfig.getExcludeDefaultListeners());
  }

  void getInternalExcludeSuperclassListenersConfig(Class type,
                                                   AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, ExcludeSuperclassListeners.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    if (entityConfig.getExcludeSuperclassListeners())
      annotationCfg.setConfig(entityConfig.getExcludeSuperclassListeners());
  }

  void getInternalInheritanceConfig(Class type,
                                    AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, Inheritance.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getInheritance());
    }
  }

  void getInternalNamedQueryConfig(Class type, AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, NamedQuery.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getNamedQuery());
    }
  }

  void getInternalNamedNativeQueryConfig(Class type,
                                         AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, NamedNativeQuery.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getNamedNativeQuery());
    }
  }

  void getInternalSqlResultSetMappingConfig(Class type,
                                            AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, SqlResultSetMapping.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getSqlResultSetMapping());
    }
  }

  void getInternalTableConfig(Class type,
                              AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, javax.persistence.Table.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getTable());
    }
  }

  void getInternalSecondaryTableConfig(Class type,
                                       AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, SecondaryTable.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getSecondaryTable());
    }
  }

  void getInternalIdClassConfig(Class type,
                                AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, IdClass.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    annotationCfg.setConfig(entityConfig.getIdClass());
  }

  void getInternalPrimaryKeyJoinColumnConfig(Class type,
                                             AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, PrimaryKeyJoinColumn.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getPrimaryKeyJoinColumn());
    }
  }

  void getInternalDiscriminatorColumnConfig(Class type,
                                            AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, DiscriminatorColumn.class);

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null) {
      annotationCfg.setConfig(entityConfig.getDiscriminatorColumn());
    }
  }

  void getInternalOneToOneConfig(Class type,
                                 AccessibleObject field,
                                 String fieldName,
                                 AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, OneToOne.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      OneToOneConfig oneToOne = attributes.getOneToOne(fieldName);

      annotationCfg.setConfig(oneToOne);
    }
  }

  void getInternalOneToManyConfig(Class type,
                                  AccessibleObject field,
                                  String fieldName,
                                  AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, OneToMany.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      OneToManyConfig oneToMany = attributes.getOneToMany(fieldName);

      annotationCfg.setConfig(oneToMany);
    }
  }

  void getInternalManyToOneConfig(Class type,
                                  AccessibleObject field,
                                  String fieldName,
                                  AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, ManyToOne.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      ManyToOneConfig manyToOne = attributes.getManyToOne(fieldName);

      annotationCfg.setConfig(manyToOne);
    }
  }

  void getInternalManyToManyConfig(Class type,
                                   AccessibleObject field,
                                   String fieldName,
                                   AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, ManyToMany.class);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AttributesConfig attributes = entityConfig.getAttributes();

    if (attributes != null) {
      ManyToManyConfig manyToMany = attributes.getManyToMany(fieldName);

      annotationCfg.setConfig(manyToMany);
    }
  }

  void getInternalIdConfig(Class type,
                           AccessibleObject method,
                           String fieldName,
                           AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(method, javax.persistence.Id.class);

    MappedSuperclassConfig mappedSuperclassOrEntityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (mappedSuperclassOrEntityConfig == null)
      return;

    AttributesConfig attributes
      = mappedSuperclassOrEntityConfig.getAttributes();

    if (attributes != null) {
      IdConfig id = attributes.getId(fieldName);

      annotationCfg.setConfig(id);
    }
  }

  void getInternalCallbackConfig(int callback,
                                 Class type,
                                 AccessibleObject method,
                                 String fieldName,
                                 AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(method, ListenerType.CALLBACK_CLASS[callback]);

    MappedSuperclassConfig entityConfig
      = getInternalMappedSuperclassOrEntityConfig(type.getName());

    if (entityConfig == null)
      return;

    AbstractListenerConfig callbackConfig;

    switch (callback) {
    case Listener.PRE_PERSIST:
      callbackConfig = entityConfig.getPrePersist();
      break;
    case Listener.POST_PERSIST:
      callbackConfig = entityConfig.getPostPersist();
      break;
    case Listener.PRE_REMOVE:
      callbackConfig = entityConfig.getPreRemove();
      break;
    case Listener.POST_REMOVE:
      callbackConfig = entityConfig.getPostRemove();
      break;
    case Listener.PRE_UPDATE:
      callbackConfig = entityConfig.getPreUpdate();
      break;
    case Listener.POST_UPDATE:
      callbackConfig = entityConfig.getPostUpdate();
      break;
    case Listener.POST_LOAD:
      callbackConfig = entityConfig.getPostLoad();
      break;
    default:
      return;
    }

    if (callbackConfig == null)
      return;

    if (callbackConfig.getMethodName().equals(((Method) method).getName()))
      annotationCfg.setConfig(callbackConfig);
  }

  void getInternalEmbeddedIdConfig(Class type,
                                   AccessibleObject method,
                                   String fieldName,
                                   AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(method, EmbeddedId.class);
  }

  void getInternalVersionConfig(Class type,
                                AccessibleObject method,
                                String fieldName,
                                AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(method, Version.class);
  }

  void getInternalJoinColumnConfig(Class type,
                                   AccessibleObject field,
                                   String fieldName,
                                   AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, JoinColumn.class);
  }

  void getInternalJoinTableConfig(Class type,
                                  AccessibleObject field,
                                  String fieldName,
                                  AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, JoinTable.class);
  }

  void getInternalMapKeyConfig(Class type,
                               AccessibleObject field,
                               String fieldName,
                               AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(field, MapKey.class);
  }

  void getInternalAttributeOverrideConfig(Class type,
                                          AnnotationConfig annotationCfg)
  {
    annotationCfg.reset(type, AttributeOverride.class);
  }

  private MappedSuperclassConfig getInternalMappedSuperclassOrEntityConfig(String name)
  {
    MappedSuperclassConfig mappedSuperclassConfig = null;

    mappedSuperclassConfig = getEntityConfig(name);

    if (mappedSuperclassConfig != null)
      return mappedSuperclassConfig;

    mappedSuperclassConfig = getMappedSuperclassConfig(name);

    return mappedSuperclassConfig;
  }

  static AttributeOverrideConfig convertAttributeOverrideAnnotationToConfig(Annotation attOverrideAnn)
  {
    /*
    Column columnAnn = attOverrideAnn.getAnnotation("column");

    return createAttributeOverrideConfig(attOverrideAnn.getString("name"),
                                         columnAnn.getString("name"),
                                         columnAnn.getBoolean("nullable"),
                                         columnAnn.getBoolean("unique"));
     */
    throw new UnsupportedOperationException();
  }

  static AttributeOverrideConfig createAttributeOverrideConfig(String name,
                                                                       String columnName,
                                                                       boolean isNullable,
                                                                       boolean isUnique)
  {
    AttributeOverrideConfig attOverrideConfig
      = new AttributeOverrideConfig();

    attOverrideConfig.setName(name);

    ColumnConfig columnConfig = new ColumnConfig();

    columnConfig.setName(columnName);
    columnConfig.setNullable(isNullable);
    columnConfig.setUnique(isUnique);

    attOverrideConfig.setColumn(columnConfig);

    return attOverrideConfig;
  }
}
