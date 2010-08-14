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

import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.DependentEntityOneToOneField;
import com.caucho.amber.field.ManyToOneField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.MappedSuperclassType;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;


/**
 * <one-to-one> tag in orm.xml
 */
class OneToOneConfig extends AbstractRelationConfig
{
  private static final L10N L = new L10N(OneToOneConfig.class);
  
  private BaseConfigIntrospector _introspector;

  private EntityType _sourceType;
  private EntityType _targetType;
  private AccessibleObject _field;
  private String _fieldName;
  private Class _fieldType;
  
  // attributes
  private boolean _isOptional;
  private String _mappedBy;

  // elements
  private HashMap<String,PrimaryKeyJoinColumnConfig> _primaryKeyJoinColumnMap
    = new HashMap<String,PrimaryKeyJoinColumnConfig>();

  private HashMap<String,JoinColumnConfig> _joinColumnMap
    = new HashMap<String,JoinColumnConfig>();

  OneToOneConfig(BaseConfigIntrospector introspector,
                  EntityType sourceType,
                  AccessibleObject field,
                  String fieldName,
                  Class fieldType)
  {
    _introspector = introspector;
    
    _sourceType = sourceType;
    
    _field = field;
    _fieldName = fieldName;
    _fieldType = fieldType;
    
    introspect();
  }

  public boolean getOptional()
  {
    return _isOptional;
  }

  public void setOptional(boolean isOptional)
  {
    _isOptional = isOptional;
  }

  public String getMappedBy()
  {
    return _mappedBy;
  }

  public void setMappedBy(String mappedBy)
  {
    _mappedBy = mappedBy;
  }
  
  public boolean isOwningSide()
  {
    return "".equals(_mappedBy);
  }

  public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumn(String columnName)
  {
    return _primaryKeyJoinColumnMap.get(columnName);
  }

  public void addPrimaryKeyJoinColumn(PrimaryKeyJoinColumnConfig primaryKeyJoinColumn)
  {
    _primaryKeyJoinColumnMap.put(primaryKeyJoinColumn.getName(),
                                 primaryKeyJoinColumn);
  }

  public HashMap<String, PrimaryKeyJoinColumnConfig> getPrimaryKeyJoinColumnMap()
  {
    return _primaryKeyJoinColumnMap;
  }

  public JoinColumnConfig getJoinColumn(String name)
  {
    return _joinColumnMap.get(name);
  }

  public void addJoinColumn(JoinColumnConfig joinColumn)
  {
    _joinColumnMap.put(joinColumn.getName(),
                       joinColumn);
  }

  public HashMap<String, JoinColumnConfig> getJoinColumnMap()
  {
    return _joinColumnMap;
  }
    
  private void introspect()
  {
    OneToOne oneToOne = _field.getAnnotation(OneToOne.class);
    
    if (oneToOne != null)
      introspectOneToOne(oneToOne);
    
    JoinColumn joinColumnAnn = _field.getAnnotation(JoinColumn.class);
    JoinColumns joinColumnsAnn = _field.getAnnotation(JoinColumns.class);

    if (joinColumnsAnn != null && joinColumnAnn != null) {
      throw error(_field, L.l("{0} may not have both @JoinColumn and @JoinColumns",
                             _fieldName));
    }

    if (joinColumnsAnn != null)
      introspectJoinColumns(joinColumnsAnn.value());
    else if (joinColumnAnn != null)
      introspectJoinColumns(new JoinColumn[] { joinColumnAnn });
  }
  
  private void introspectOneToOne(OneToOne oneToOne)
  {
    Class targetClass = oneToOne.targetEntity();

    if (void.class.equals(targetClass))
      targetClass = _fieldType;
    
    setTargetEntity(targetClass);
    
    setCascadeTypes(oneToOne.cascade());
    setFetch(oneToOne.fetch());
    
    _isOptional = oneToOne.optional();
    _mappedBy = oneToOne.mappedBy();
  }
  
  private void introspectJoinColumns(JoinColumn []joinColumns)
  {
    for (JoinColumn joinColumn : joinColumns) {
      addJoinColumn(new JoinColumnConfig(joinColumn));
    }
  }

  @Override
  public EntityType getRelatedType()
  {
    return _sourceType;
  }

  @Override
  public void complete()
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    String targetName = _fieldType.getName();
      
    EntityType targetType = persistenceUnit.createEntity(getTargetEntity());

    if (isOwningSide()) {
      addManyToOne();

      // XXX: set unique
    }
    else {
      addDependentOneToOne();
    }
  }

  private void addManyToOne()
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    EntityType targetType = persistenceUnit.createEntity(getTargetEntity());

    ManyToOneField manyToOneField;
    manyToOneField = new ManyToOneField(_sourceType, _fieldName,
                                        getCascade(), false);

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(isFetchLazy());

    manyToOneField.setJoinColumnMap(_joinColumnMap);

    _sourceType.addField(manyToOneField);

    // jpa/0ge3
    if (_sourceType instanceof MappedSuperclassType)
      return;

    validateJoinColumns(_field, _fieldName, _joinColumnMap, targetType);

    manyToOneField.init();
  }

  private void addDependentOneToOne()
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    EntityType targetType = persistenceUnit.createEntity(getTargetEntity());

    // Owner
    ManyToOneField sourceField
      = getSourceField(targetType, _mappedBy, _sourceType);

    if (sourceField == null) {
      throw error(_field, L.l("OneToOne target '{0}' does not have a matching ManyToOne relation.",
                              targetType.getName()));
    }

    DependentEntityOneToOneField oneToOne;

    oneToOne = new DependentEntityOneToOneField(_sourceType, _fieldName, getCascade());
    oneToOne.setTargetField(sourceField);
    sourceField.setTargetField(oneToOne);
    oneToOne.setLazy(isFetchLazy());

    _sourceType.addField(oneToOne);
  }

  private ManyToOneField getSourceField(EntityType targetType,
                                String mappedBy,
                                EntityType sourceType)
  {
    do {
      ArrayList<AmberField> fields = targetType.getFields();

      for (AmberField field : fields) {
        // jpa/0o07: there is no mappedBy at all on any sides.
        if ("".equals(mappedBy) || mappedBy == null) {
          if (field.getJavaType().isAssignableFrom(sourceType.getBeanClass()))
            return (ManyToOneField) field;
        }
        else if (field.getName().equals(mappedBy))
          return (ManyToOneField) field;
      }

      // jpa/0ge4
      targetType = targetType.getParentType();
    } while (targetType != null);

    return null;
  }
}
