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

import com.caucho.vfs.Path;

import java.util.HashMap;

/**
 * Top <entity-mappings> tag in the orm.xml
 */
public class EntityMappingsConfig {
  private Path _root;

  // attributes
  private String _version;

  // elements
  private String _description;
  private PersistenceUnitMetaDataConfig _persistenceUnitMetaData;
  private String _package;
  private String _schema;
  private String _catalog;
  private AccessType _access;

  private HashMap<String, SequenceGeneratorConfig> _sequenceGeneratorMap
    = new HashMap<String, SequenceGeneratorConfig>();

  private HashMap<String, TableGeneratorConfig> _tableGeneratorMap
    = new HashMap<String, TableGeneratorConfig>();

  private HashMap<String, NamedQueryConfig> _namedQueryMap
    = new HashMap<String, NamedQueryConfig>();

  private HashMap<String, NamedNativeQueryConfig> _namedNativeQueryMap
    = new HashMap<String, NamedNativeQueryConfig>();

  private HashMap<String, SqlResultSetMappingConfig> _sqlResultSetMappingMap
    = new HashMap<String, SqlResultSetMappingConfig>();

  private HashMap<String, MappedSuperclassConfig> _mappedSuperclassMap
    = new HashMap<String, MappedSuperclassConfig>();

  private HashMap<String, EntityConfig> _entityMap
    = new HashMap<String, EntityConfig>();

  private HashMap<String, EmbeddableConfig> _embeddableMap
    = new HashMap<String, EmbeddableConfig>();


  public AccessType getAccess()
  {
    return _access;
  }

  public String getCatalog()
  {
    return _catalog;
  }

  public String getDescription()
  {
    return _description;
  }

  public String getPackage()
  {
    return _package;
  }

  public PersistenceUnitMetaDataConfig getPersistenceUnitMetaData()
  {
    return _persistenceUnitMetaData;
  }

  public void setPersistenceUnitMetaData(PersistenceUnitMetaDataConfig persistenceUnitMetaData)
  {
    _persistenceUnitMetaData = persistenceUnitMetaData;
  }

  public Path getRoot()
  {
    return _root;
  }

  public String getSchema()
  {
    return _schema;
  }

  public String getVersion()
  {
    return _version;
  }

  public void setAccess(String access)
  {
    _access = AccessType.valueOf(access);
  }

  public void setCatalog(String catalog)
  {
    _catalog = catalog;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public void setPackage(String packageName)
  {
    _package = packageName;
  }

  public void setRoot(Path root)
  {
    _root = root;
  }

  public void setSchema(String schema)
  {
    _schema = schema;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  /**
   * Sets the schema location
   */
  public void setSchemaLocation(String schema)
  {
  }

  /**
   * Adds a new <entity>.
   */
  public void addEntity(EntityConfig entity)
  {
    String className = entity.getClassName();

    addInternalClass(_entityMap, className, entity);
  }

  /**
   * Returns an entity config.
   */
  public EntityConfig getEntityConfig(String name)
  {
    return _entityMap.get(name);
  }

  /**
   * Returns the entity map.
   */
  public HashMap<String, EntityConfig> getEntityMap()
  {
    return _entityMap;
  }

  public void addSequenceGenerator(SequenceGeneratorConfig sequenceGenerator)
  {
    _sequenceGeneratorMap.put(sequenceGenerator.getName(), sequenceGenerator);
  }

  public SequenceGeneratorConfig getSequenceGenerator(String name)
  {
    return _sequenceGeneratorMap.get(name);
  }

  public HashMap<String, SequenceGeneratorConfig> getSequenceGeneratorMap()
  {
    return _sequenceGeneratorMap;
  }

  public void addTableGenerator(TableGeneratorConfig tableGenerator)
  {
    _tableGeneratorMap.put(tableGenerator.getName(), tableGenerator);
  }

  public TableGeneratorConfig getTableGenerator(String name)
  {
    return _tableGeneratorMap.get(name);
  }

  public HashMap<String, TableGeneratorConfig> getTableGeneratorMap()
  {
    return _tableGeneratorMap;
  }

  public void addNamedQuery(NamedQueryConfig namedQuery)
  {
    _namedQueryMap.put(namedQuery.getName(), namedQuery);
  }

  public NamedQueryConfig getNamedQuery(String name)
  {
    return _namedQueryMap.get(name);
  }

  public HashMap<String, NamedQueryConfig> getNamedQueryMap()
  {
    return _namedQueryMap;
  }

  public void addNamedNativeQuery(NamedNativeQueryConfig namedNativeQuery)
  {
    _namedNativeQueryMap.put(namedNativeQuery.getName(), namedNativeQuery);
  }

  public NamedNativeQueryConfig getNamedNativeQuery(String name)
  {
    return _namedNativeQueryMap.get(name);
  }

  public HashMap<String, NamedNativeQueryConfig> getNamedNativeQueryMap()
  {
    return _namedNativeQueryMap;
  }

  public void addSqlResultSetMapping(SqlResultSetMappingConfig sqlResultSetMapping)
  {
    _sqlResultSetMappingMap.put(sqlResultSetMapping.getName(), sqlResultSetMapping);
  }

  public SqlResultSetMappingConfig getSqlResultSetMapping(String name)
  {
    return _sqlResultSetMappingMap.get(name);
  }

  public HashMap<String, SqlResultSetMappingConfig> getSqlResultSetMappingMap()
  {
    return _sqlResultSetMappingMap;
  }

  public void addMappedSuperclass(MappedSuperclassConfig mappedSuperclass)
  {
    String className = mappedSuperclass.getClassName();

    addInternalClass(_mappedSuperclassMap, className, mappedSuperclass);
  }

  public MappedSuperclassConfig getMappedSuperclass(String name)
  {
    return _mappedSuperclassMap.get(name);
  }

  public HashMap<String, MappedSuperclassConfig> getMappedSuperclassMap()
  {
    return _mappedSuperclassMap;
  }

  public void addEmbeddable(EmbeddableConfig embeddable)
  {
    String className = embeddable.getClassName();

    addInternalClass(_embeddableMap, className, embeddable);
  }

  public EmbeddableConfig getEmbeddable(String name)
  {
    return _embeddableMap.get(name);
  }

  public HashMap<String, EmbeddableConfig> getEmbeddableMap()
  {
    return _embeddableMap;
  }

  private void addInternalClass(HashMap map,
                                String className,
                                Object config)
  {
    // jpa/0s2d, jpa/0ge7
    if ((_package == null) || className.startsWith(_package + "."))
      map.put(className, config);
    else
      map.put(_package + "." + className, config);
  }

  public String toString()
  {
    return _entityMap.toString();
  }
}
