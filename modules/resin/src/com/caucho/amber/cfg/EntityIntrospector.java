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

package com.caucho.amber.cfg;

import com.caucho.amber.AmberTableCache;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.amber.field.SubId;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.InheritanceType;
import javax.persistence.AttributeOverrides;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.persistence.AttributeOverride;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EntityIntrospector.class.getName());

  /**
   * Creates the introspector.
   */
  EntityIntrospector(AmberConfigManager manager)
  {
    super(manager);
  }

  /**
   * Returns true for entity type.
   */
  public boolean isEntity(Class type)
  {
    getInternalEntityConfig(type, _annotationCfg);
 
    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  public BeanType introspect(Class type)
    throws ConfigException, SQLException
  {
    BeanType beanType = null;
    
    try {
      EntityType entityType = null;

      EntityType parentType = introspectParent(type.getSuperclass());

      entityType = introspectEntityType(type, parentType);

      if (entityType == null)
        entityType = introspectMappedType(type, parentType);

      if (entityType == null)
        return introspectEmbeddableType(type);
      
      beanType = entityType;
      
      // jpa/0ge2
      entityType.setInstanceClassName(type.getName() + "__ResinExt");
      entityType.setEnhanced(true);

      MappedSuperclassConfig mappedSuperOrEntityConfig
        = introspectEntityConfig(type);

      entityType.setParentType(parentType);

      introspectTable(type, entityType, parentType);

      // inheritance must be after table since it adds columns
      introspectInheritance(type, entityType, parentType);
      
      introspectTableCache(entityType, type);

      getInternalIdClassConfig(type, _annotationCfg);
      IdClass idClassAnn = (IdClass) _annotationCfg.getAnnotation();
      IdClassConfig idClassConfig = _annotationCfg.getIdClassConfig();

      Class idClass = null;
      if (! _annotationCfg.isNull()) {
        if (idClassAnn != null)
          idClass = idClassAnn.value();
        else {
          String s = idClassConfig.getClassName();
          idClass = _persistenceUnit.loadTempClass(s);
        }

        // XXX: temp. introspects idClass as an embeddable type.
        _persistenceUnit.addEntityClass(idClass.getName(), idClass);

        // jpa/0i49 vs jpa/0i40
        // embeddable.setFieldAccess(isField);
      }

      if (entityType.getId() != null) {
      }
      else if (entityType.isFieldAccess())
        introspectIdField(_persistenceUnit, entityType, parentType,
                          type, idClass, mappedSuperOrEntityConfig);
      else {
        introspectIdMethod(_persistenceUnit, entityType, parentType,
                           type, idClass, mappedSuperOrEntityConfig);
      }

      HashMap<String, IdConfig> idMap = null;

      AttributesConfig attributes = null;

      if (mappedSuperOrEntityConfig != null) {
        attributes = mappedSuperOrEntityConfig.getAttributes();

        if (attributes != null)
          idMap = attributes.getIdMap();
      }

      // if ((idMap == null) || (idMap.size() == 0)) {
      //   idMap = entityType.getSuperClass();
      // }

      if (entityType.isEntity() && (entityType.getId() == null) && ((idMap == null) || (idMap.size() == 0)))
        throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id or exactly one @EmbeddedId field.",
                                      entityType.getName()));

      // Introspect overridden attributes. (jpa/0ge2)
      introspectAttributeOverrides(entityType, type);

      introspectSecondaryTable(entityType, type);

      if (entityType.isFieldAccess())
        introspectFields(_persistenceUnit, entityType, parentType, type,
                         mappedSuperOrEntityConfig, false);
      else
        introspectMethods(_persistenceUnit, entityType, parentType, type,
                          mappedSuperOrEntityConfig);

      introspectCallbacks(type, entityType);
      

      // Adds entity listeners, if any.
      introspectEntityListeners(type, entityType, _persistenceUnit);

      // Adds sql result set mappings, if any.
      introspectSqlResultSetMappings(type, entityType, entityType.getName());

      // Adds named queries, if any.
      introspectNamedQueries(type, entityType.getName());
      introspectNamedNativeQueries(type, entityType.getName());
    } catch (ConfigException e) {
      if (beanType != null)
        beanType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      if (beanType != null)
        beanType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      if (beanType != null)
        beanType.setConfigException(e);

      throw e;
    }

    return beanType;
  }

  private EntityType introspectEntityType(Class type,
                                          EntityType parentType)
    throws SQLException
  {
    getInternalEntityConfig(type, _annotationCfg);
      
    Entity entityAnn = (Entity) _annotationCfg.getAnnotation();
    EntityConfig entityConfig = _annotationCfg.getEntityConfig();

    boolean isEntity = ! _annotationCfg.isNull();

    if (! isEntity)
      return null;
    
    EntityType entityType;
    String typeName;
    
    if (entityConfig != null)
      typeName = entityConfig.getClassName();
    else
      typeName = entityAnn.name();

    // Validates the type
    String entityName;
    Inheritance inheritanceAnn = null;
    InheritanceConfig inheritanceConfig = null;
    Class rootClass = type;
    Entity rootEntityAnn = null;
    EntityConfig rootEntityConfig = null;

    validateType(type, true);

      // jpa/0ge2
      // if (hasInheritance) {

    if (entityConfig == null)
      entityName = entityAnn.name();
    else {
      entityName = entityConfig.getClassName();

      int p = entityName.lastIndexOf('.');

      if (p > 0)
        entityName = entityName.substring(p + 1);
    }

    if ((entityName == null) || "".equals(entityName)) {
      entityName = type.getSimpleName();
    }

    entityType = _persistenceUnit.createEntity(entityName, type);

    _configManager.addType(type, new EntityConfig(type.getName(), this, entityType));

    boolean isField = isField(type, entityConfig, false);

    if (isField)
      entityType.setFieldAccess(true);

    return entityType;
  }

  private EntityType introspectMappedType(Class type, EntityType parentType)
    throws SQLException
  {
    EntityType entityType;

    boolean isMappedSuperclass = false;
    MappedSuperclass mappedSuperAnn = null;
    MappedSuperclassConfig mappedSuperConfig = null;

    String typeName;

    MappedSuperclassConfig mappedSuperOrEntityConfig = null;

    getInternalMappedSuperclassConfig(type, _annotationCfg);
    mappedSuperAnn = (MappedSuperclass) _annotationCfg.getAnnotation();
    mappedSuperConfig = _annotationCfg.getMappedSuperclassConfig();

    isMappedSuperclass = ! _annotationCfg.isNull();

    if (! isMappedSuperclass)
      return null;
    
    mappedSuperOrEntityConfig = mappedSuperConfig;

    if (mappedSuperConfig != null)
      typeName = mappedSuperConfig.getClassName();
/*    else
      typeName = mappedSuperAnn.name();
*/
    // Validates the type
    String entityName;
    Inheritance inheritanceAnn = null;
    InheritanceConfig inheritanceConfig = null;
    Class rootClass = type;
    Entity rootEntityAnn = null;
    EntityConfig rootEntityConfig = null;

    validateType(type, false);

    // jpa/0ge2
    // if (hasInheritance) {

    if (mappedSuperConfig == null)
      entityName = null;//mappedSuperAnn.name();
    else {
      entityName = mappedSuperConfig.getSimpleClassName();
    }

    if ((entityName == null) || "".equals(entityName)) {
      entityName = type.getSimpleName();
    }

    entityType = _persistenceUnit.createMappedSuperclass(entityName, type);

    _configManager.addType(type, new EntityConfig(type.getName(), this, entityType));

    boolean isField = isField(type, mappedSuperOrEntityConfig, false);

    if (isField)
      entityType.setFieldAccess(true);

    // jpa/0ge2
    entityType.setInstanceClassName(type.getName() + "__ResinExt");
    entityType.setEnhanced(true);

    return entityType;
  }

  /**
   * Introspects.
   */
  private EmbeddableType introspectEmbeddableType(Class type)
    throws ConfigException, SQLException
  {
    getInternalEmbeddableConfig(type, _annotationCfg);
    Embeddable embeddableAnn = (Embeddable) _annotationCfg.getAnnotation();
    EmbeddableConfig embeddableConfig = _annotationCfg.getEmbeddableConfig();

    /*
    if (_annotationCfg.isNull())
      return null;
    */

    String typeName = type.getName();
    EmbeddableType embeddableType
      = _persistenceUnit.createEmbeddable(typeName, type);

    _configManager.addType(type, new EmbeddableConfig(type.getName(), this, embeddableType));

    try {
      boolean isField = isField(type, embeddableConfig);

      if (isField)
        embeddableType.setFieldAccess(true);

      // XXX: jpa/0u21
      Embeddable ann = (Embeddable) type.getAnnotation(javax.persistence.Embeddable.class);

      if (ann == null) {
        isField = true;
        embeddableType.setIdClass(true);
      
        _persistenceUnit.getAmberContainer().addEmbeddable(typeName,
                                                           embeddableType);
      }

      embeddableType.setInstanceClassName(type.getName() +
                                          "__ResinExt");
      embeddableType.setEnhanced(true);

      if (isField)
        introspectFields(_persistenceUnit, embeddableType, null,
                         type, embeddableConfig, true);
      else
        introspectMethods(_persistenceUnit, embeddableType, null,
                          type, embeddableConfig);

    } catch (ConfigException e) {
      if (embeddableType != null)
        embeddableType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      if (embeddableType != null)
        embeddableType.setConfigException(e);

      throw e;
    }

    return embeddableType;
  }

  private MappedSuperclassConfig introspectEntityConfig(Class type)
    throws SQLException
  {
    getInternalEntityConfig(type, _annotationCfg);
      
    if (! _annotationCfg.isNull())
      return _annotationCfg.getEntityConfig();
    
    getInternalMappedSuperclassConfig(type, _annotationCfg);

    return _annotationCfg.getMappedSuperclassConfig();
  }

  private EntityType introspectParent(Class parentClass)
    throws SQLException
  {
    if (parentClass == null)
      return null;

    getInternalEntityConfig(parentClass, _annotationCfg);

    if (! _annotationCfg.isNull())
      return (EntityType) _configManager.introspect(parentClass);
    else if (parentClass.isAnnotationPresent(javax.persistence.Entity.class))
      return (EntityType) _configManager.introspect(parentClass);
    else if (parentClass.isAnnotationPresent(javax.persistence.MappedSuperclass.class))
      return (EntityType) _configManager.introspect(parentClass);
    else
      return null;
  }



  private void introspectInheritance(Class type,
                                     EntityType entityType,
                                     EntityType parentType)
    throws SQLException
  {
    // Inheritance annotation/configuration is specified
    // on the entity class that is the root of the entity
    // class hierarachy.
    InheritanceConfig inheritanceConfig;
    Inheritance inheritanceAnn;

    getInternalInheritanceConfig(type, _annotationCfg);
    inheritanceAnn = (Inheritance) _annotationCfg.getAnnotation();
    inheritanceConfig = _annotationCfg.getInheritanceConfig();

    boolean hasInheritance = ! _annotationCfg.isNull();

    if (! hasInheritance &&
        (parentType == null || ! parentType.isEntity()))
      return;

    introspectDiscriminatorValue(type, entityType);
    
    if (parentType != null) {
      if (hasInheritance)
        throw new ConfigException(L.l("'{0}' cannot have @Inheritance. It must be specified on the entity class that is the root of the entity class hierarchy.",
                                      type));
      
      EntityType rootType = entityType.getRootType();
      rootType.addSubClass(entityType);

      getInternalPrimaryKeyJoinColumnConfig(type, _annotationCfg);
      PrimaryKeyJoinColumn joinAnn
        = (PrimaryKeyJoinColumn) _annotationCfg.getAnnotation();
      PrimaryKeyJoinColumnConfig primaryKeyJoinColumnConfig
        = _annotationCfg.getPrimaryKeyJoinColumnConfig();

      if (rootType.isJoinedSubClass()) {
        linkInheritanceTable(rootType.getTable(),
                             entityType.getTable(),
                             joinAnn,
                             primaryKeyJoinColumnConfig);

        entityType.setId(new SubId(entityType, rootType));
      }

      return;
    }
    
    if (! hasInheritance)
      return;

    if ((inheritanceAnn != null) || (inheritanceConfig != null))
      introspectInheritance(_persistenceUnit, entityType, type,
                            inheritanceAnn, inheritanceConfig);
  }

  private void introspectDiscriminatorValue(Class type, EntityType entityType)
  {
    DiscriminatorValue discValueAnn = (DiscriminatorValue) type.getAnnotation(DiscriminatorValue.class);

    String discriminatorValue = null;

    if (discValueAnn != null)
      discriminatorValue = discValueAnn.value();

    if (discriminatorValue == null || discriminatorValue.equals("")) {
      String name = entityType.getBeanClass().getSimpleName();

      discriminatorValue = name;
    }

    entityType.setDiscriminatorValue(discriminatorValue);
  }

  /**
   * Introspects the Inheritance
   */
  void introspectInheritance(AmberPersistenceUnit persistenceUnit,
                             EntityType entityType,
                             Class type,
                             Inheritance inheritanceAnn,
                             InheritanceConfig inheritanceConfig)
    throws ConfigException, SQLException
  {
    InheritanceType strategy;

    if (inheritanceAnn != null)
      strategy = inheritanceAnn.strategy();
    else
      strategy = inheritanceConfig.getStrategy();

    switch (strategy) {
    case JOINED:
      entityType.setJoinedSubClass(true);
      break;
    }

    getInternalDiscriminatorColumnConfig(type, _annotationCfg);
    DiscriminatorColumn discriminatorAnn = (DiscriminatorColumn) _annotationCfg.getAnnotation();
    DiscriminatorColumnConfig discriminatorConfig = _annotationCfg.getDiscriminatorColumnConfig();

    String columnName = null;

    if (discriminatorAnn != null)
      columnName = discriminatorAnn.name();

    if (columnName == null || columnName.equals(""))
      columnName = "DTYPE";

    AmberType columnType = null;
    DiscriminatorType discType = DiscriminatorType.STRING;

    if (discriminatorAnn != null)
      discType = discriminatorAnn.discriminatorType();

    switch (discType) {
    case STRING:
      columnType = StringType.create();
      break;
    case CHAR:
      columnType = PrimitiveCharType.create();
      break;
    case INTEGER:
      columnType = PrimitiveIntType.create();
      break;
    default:
      throw new IllegalStateException();
    }

    AmberTable table = entityType.getTable();

    // jpa/0gg0
    if (table == null)
      return;

    com.caucho.amber.table.AmberColumn column
      = table.createColumn(columnName, columnType);

    if (discriminatorAnn != null) {
//      column.setNotNull(! discriminatorAnn.nullable());

      column.setLength(discriminatorAnn.length());

      if (! "".equals(discriminatorAnn.columnDefinition()))
        column.setSQLType(discriminatorAnn.columnDefinition());
    }
    else {
      column.setNotNull(true);
      column.setLength(10);
    }

    entityType.setDiscriminator(column);
  }

  private void introspectTable(Class type,
                               EntityType entityType,
                               EntityType parentType)
  {
    // XXX: need better test
    boolean isEntity = ! (entityType instanceof MappedSuperclassType);
    
    AmberTable table = null;

    getInternalTableConfig(type, _annotationCfg);
    Table tableAnn = (Table) _annotationCfg.getAnnotation();
    TableConfig tableConfig = _annotationCfg.getTableConfig();

    String tableName = null;

    if (tableAnn != null)
      tableName = tableAnn.name();
    else if (tableConfig != null)
      tableName = tableConfig.getName();

    // jpa/0gg0, jpa/0gg2
    if (isEntity) { // && ! type.isAbstract()) {

      boolean hasTableConfig = true;

      if (tableName == null || tableName.equals("")) {
        hasTableConfig = false;
        tableName = toSqlName(entityType.getName());
      }

      /*
      InheritanceType strategy = null;
      
      if (parentType != null)
        strategy = parentType.getInheritanceStrategy();

      if (inheritanceAnn != null)
        strategy = (InheritanceType) inheritanceAnn.get("strategy");
      else if (inheritanceConfig != null)
        strategy = inheritanceConfig.getStrategy();
       */

      // jpa/0gg2
      if (! entityType.isEntity())
        return;
      /*
      if (type.isAbstract()
          && strategy != InheritanceType.JOINED
          && ! hasTableConfig) {
        // jpa/0gg0
      }
       */
      else if (parentType == null || parentType.getTable() == null) {
        entityType.setTable(_persistenceUnit.createTable(tableName));
      }
      else if (parentType.isJoinedSubClass()) {
        entityType.setTable(_persistenceUnit.createTable(tableName));

        EntityType rootType = parentType.getRootType();

        Class rootClass = rootType.getBeanClass();
        getInternalTableConfig(rootClass, _annotationCfg);
        Table rootTableAnn = (Table) _annotationCfg.getAnnotation();
        TableConfig rootTableConfig = _annotationCfg.getTableConfig();

        String rootTableName = null;

        if (rootTableAnn != null)
          rootTableName = rootTableAnn.name();
        else if (rootTableConfig != null)
          rootTableName = rootTableConfig.getName();

        if (rootTableName == null || rootTableName.equals("")) {
          String rootEntityName = rootType.getName();

          rootTableName = toSqlName(rootEntityName);
        }

        entityType.setRootTableName(rootTableName);
      }
      else
        entityType.setTable(parentType.getTable());
    }
  }

  private void introspectSecondaryTable(EntityType entityType, Class type)
  {
    getInternalSecondaryTableConfig(type, _annotationCfg);
    SecondaryTable secondaryTableAnn = (SecondaryTable) _annotationCfg.getAnnotation();
    SecondaryTableConfig secondaryTableConfig = _annotationCfg.getSecondaryTableConfig();

    AmberTable secondaryTable = null;

    if ((secondaryTableAnn != null) || (secondaryTableConfig != null)) {
      String secondaryName;

      if (secondaryTableAnn != null)
        secondaryName = secondaryTableAnn.name();
      else
        secondaryName = secondaryTableConfig.getName();

      secondaryTable = _persistenceUnit.createTable(secondaryName);

      entityType.addSecondaryTable(secondaryTable);

      // XXX: pk
    }

    if (secondaryTableAnn != null) {
      PrimaryKeyJoinColumn[] joinAnn = secondaryTableAnn.pkJoinColumns();

      linkSecondaryTable(entityType.getTable(),
                         secondaryTable,
                         joinAnn);
    }
  }

  private void introspectTableCache(EntityType entityType, Class type)
  {
    AmberTableCache tableCache = (AmberTableCache) type.getAnnotation(AmberTableCache.class);

    if (tableCache != null) {
      entityType.getTable().setReadOnly(tableCache.readOnly());

      long cacheTimeout = Period.toPeriod(tableCache.timeout());
      entityType.getTable().setCacheTimeout(cacheTimeout);
    }
  }

  private void introspectAttributeOverrides(EntityType entityType,
                                            Class type)
  {
    EntityType parent = entityType.getParentType();

    if (parent == null)
      return;

    boolean isAbstract = Modifier.isAbstract(parent.getBeanClass().getModifiers());

    if (parent.isEntity() && ! isAbstract)
      return;

    HashMap<String,ColumnConfig> overrideMap
      = new HashMap<String,ColumnConfig>();
    
    getInternalAttributeOverrideConfig(type, _annotationCfg);
    AttributeOverride attributeOverrideAnn = (AttributeOverride) _annotationCfg.getAnnotation();

    boolean hasAttributeOverride = (attributeOverrideAnn != null);

    AttributeOverrides attributeOverridesAnn
      = (AttributeOverrides) type.getAnnotation(AttributeOverrides.class);

    ArrayList<AttributeOverrideConfig> attributeOverrideList = null;

    EntityConfig entityConfig = getEntityConfig(type.getName());

    if (entityConfig != null)
      attributeOverrideList = entityConfig.getAttributeOverrideList();

    boolean hasAttributeOverrides = false;

    if ((attributeOverrideList != null) &&
        (attributeOverrideList.size() > 0)) {
      hasAttributeOverrides = true;
    }
    else if (attributeOverridesAnn != null)
      hasAttributeOverrides = true;

    if (hasAttributeOverride && hasAttributeOverrides)
      throw new ConfigException(L.l("{0} may not have both @AttributeOverride and @AttributeOverrides",
                                    type));

    if (attributeOverrideList == null)
      attributeOverrideList = new ArrayList<AttributeOverrideConfig>();

    if (hasAttributeOverride) {
      // Convert annotation to configuration.

      AttributeOverrideConfig attOverrideConfig
        = convertAttributeOverrideAnnotationToConfig(attributeOverrideAnn);

      attributeOverrideList.add(attOverrideConfig);
    }
    else if (hasAttributeOverrides) {
      if (attributeOverrideList.size() > 0) {
        // OK: attributeOverrideList came from orm.xml
      }
      else {
        // Convert annotations to configurations.

        AttributeOverride attOverridesAnn[]
          = attributeOverridesAnn.value();

        AttributeOverrideConfig attOverrideConfig;

        /* XXX:
        for (int i = 0; i < attOverridesAnn.length; i++) {
          attOverrideConfig
            = convertAttributeOverrideAnnotationToConfig((JAnnotation) attOverridesAnn[i]);

          attributeOverrideList.add(attOverrideConfig);
        }
         * */
      }
    }

    for (AttributeOverrideConfig override : attributeOverrideList) {
      overrideMap.put(override.getName(), override.getColumn());
    }

    _depCompletions.add(new AttributeOverrideCompletion(this, entityType, type,
                                                        overrideMap));
  }

  boolean isField(Class type,
                  AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    for (Method method : type.getDeclaredMethods()) {
      Annotation ann[] = method.getDeclaredAnnotations();

      for (int i = 0; ann != null && i < ann.length; i++) {
        if (ann[i] instanceof Basic || ann[i] instanceof Column)
          return false;
      }
    }

    return true;
  }

  private boolean isPropertyAnnotation(Class cl)
  {
    return (Basic.class.equals(cl)
            || Column.class.equals(cl));
  }
}
