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

import java.util.ArrayList;

import com.caucho.amber.type.EntityType;

/**
 * <entity> tag in the orm.xml
 */
public class EntityConfig extends MappedSuperclassConfig {
  // attributes
  private String _name;

  // elements
  private TableConfig _table;
  private SecondaryTableConfig _secondaryTable;
  private PrimaryKeyJoinColumnConfig _primaryKeyJoinColumn;
  private InheritanceConfig _inheritance;
  private String _discriminatorValue;
  private DiscriminatorColumnConfig _discriminatorColumn;
  private SequenceGeneratorConfig _sequenceGenerator;
  private TableGeneratorConfig _tableGenerator;
  private NamedQueryConfig _namedQuery;
  private NamedNativeQueryConfig _namedNativeQuery;
  private SqlResultSetMappingConfig _sqlResultSetMapping;
  private ArrayList<AttributeOverrideConfig> _attributeOverrideList
    = new ArrayList<AttributeOverrideConfig>();
  private ArrayList<AssociationOverrideConfig> _associationOverrideList
    = new ArrayList<AssociationOverrideConfig>();

  private EntityType _entityType;
  private EntityIntrospector _introspector;

  public EntityConfig()
  {
  }
  
  EntityConfig(String name)
  {
    super(name);
  }

  EntityConfig(String name,
               EntityIntrospector introspector,
               EntityType entityType)
  {
    super(name);
    
    _entityType = entityType;
    _introspector = introspector;
  }

  BaseConfigIntrospector getIntrospector()
  {
    return _introspector;
  }

  /**
   * Returns the entity name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the entity name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the configured entity type
   */
  public EntityType getEntityType()
  {
    return _entityType;
  }

  /**
   * Returns the configured entity type
   */
  public void setEntityType(EntityType entityType)
  {
    _entityType = entityType;
  }

  public TableConfig getTable()
  {
    return _table;
  }

  public void setTable(TableConfig table)
  {
    _table = table;
  }

  public void addAssociationOverride(AssociationOverrideConfig associationOverride)
  {
    _associationOverrideList.add(associationOverride);
  }

  public ArrayList<AssociationOverrideConfig> getAssociationOverrideList()
  {
    return _associationOverrideList;
  }

  public void addAttributeOverride(AttributeOverrideConfig attributeOverride)
  {
    _attributeOverrideList.add(attributeOverride);
  }

  public ArrayList<AttributeOverrideConfig> getAttributeOverrideList()
  {
    return _attributeOverrideList;
  }

  public SecondaryTableConfig getSecondaryTable()
  {
    return _secondaryTable;
  }

  public void setSecondaryTable(SecondaryTableConfig secondaryTable)
  {
    _secondaryTable = secondaryTable;
  }

  public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumn()
  {
    return _primaryKeyJoinColumn;
  }

  public void setPrimaryKeyJoinColumn(PrimaryKeyJoinColumnConfig primaryKeyJoinColumn)
  {
    _primaryKeyJoinColumn = primaryKeyJoinColumn;
  }

  public InheritanceConfig getInheritance()
  {
    return _inheritance;
  }

  public void setInheritance(InheritanceConfig inheritance)
  {
    _inheritance = inheritance;
  }

  public String getDiscriminatorValue()
  {
    return _discriminatorValue;
  }

  public void setDiscriminatorValue(String discriminatorValue)
  {
    _discriminatorValue = discriminatorValue;
  }

  public DiscriminatorColumnConfig getDiscriminatorColumn()
  {
    return _discriminatorColumn;
  }

  public void setDiscriminatorColumn(DiscriminatorColumnConfig discriminatorColumn)
  {
    _discriminatorColumn = discriminatorColumn;
  }

  public SequenceGeneratorConfig getSequenceGenerator()
  {
    return _sequenceGenerator;
  }

  public void setSequenceGenerator(SequenceGeneratorConfig sequenceGenerator)
  {
    _sequenceGenerator = sequenceGenerator;
  }

  public TableGeneratorConfig getTableGenerator()
  {
    return _tableGenerator;
  }

  public void setTableGenerator(TableGeneratorConfig tableGenerator)
  {
    _tableGenerator = tableGenerator;
  }

  public NamedQueryConfig getNamedQuery()
  {
    return _namedQuery;
  }

  public void setNamedQuery(NamedQueryConfig namedQuery)
  {
    _namedQuery = namedQuery;
  }

  public NamedNativeQueryConfig getNamedNativeQuery()
  {
    return _namedNativeQuery;
  }

  public void setNamedNativeQuery(NamedNativeQueryConfig namedNativeQuery)
  {
    _namedNativeQuery = namedNativeQuery;
  }

  public SqlResultSetMappingConfig getSqlResultSetMapping()
  {
    return _sqlResultSetMapping;
  }

  public void setSqlResultSetMapping(SqlResultSetMappingConfig sqlResultSetMapping)
  {
    _sqlResultSetMapping = sqlResultSetMapping;
  }

  public String toString()
  {
    return "EntityConfig[" + _name + ", " + getClassName() + "]";
  }
}
