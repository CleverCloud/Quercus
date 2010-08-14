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

import com.caucho.amber.field.ManyToOneField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.MappedSuperclassType;
import com.caucho.bytecode.JClass;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.util.HashMap;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;


/**
 * <many-to-one> tag in orm.xml
 */
class ManyToOneConfig extends AbstractRelationConfig
{
  private static final L10N L = new L10N(ManyToOneConfig.class);
  
  private BaseConfigIntrospector _introspector;

  private EntityType _sourceType;
  private EntityType _targetType;
  private AccessibleObject _field;
  private String _fieldName;
  private Class _fieldType;
  
  // attributes
  private boolean _isOptional = true;

  // elements
  private HashMap<String,JoinColumnConfig> _joinColumnMap
    = new HashMap<String,JoinColumnConfig>();

  ManyToOneConfig(BaseConfigIntrospector introspector,
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

  public JoinColumnConfig getJoinColumn(String name)
  {
    return _joinColumnMap.get(name);
  }

  public void addJoinColumn(JoinColumnConfig joinColumn)
  {
    _joinColumnMap.put(joinColumn.getReferencedColumnName(),
                       joinColumn);
  }

  public HashMap<String, JoinColumnConfig> getJoinColumnMap()
  {
    return _joinColumnMap;
  }
  
  public EntityType getRelatedType()
  {
    return _sourceType;
  }

  public void complete()
    throws ConfigException
  {
    addManyToOne();
  }
    
  private void introspect()
  {
    ManyToOne manyToOne = _field.getAnnotation(ManyToOne.class);
    
    if (manyToOne != null)
      introspectManyToOne(manyToOne);
    
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
  
  private void introspectManyToOne(ManyToOne manyToOne)
  {
    Class targetClass = manyToOne.targetEntity();

    if (void.class.equals(targetClass))
      targetClass = _fieldType;
    
    setTargetEntity(targetClass);
    
    setCascadeTypes(manyToOne.cascade());
    setFetch(manyToOne.fetch());
    
    _isOptional = manyToOne.optional();
  }
  
  private void introspectJoinColumns(JoinColumn []joinColumns)
  {
    for (JoinColumn joinColumn : joinColumns) {
      addJoinColumn(new JoinColumnConfig(joinColumn));
    }
  }

  void addManyToOne()
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    String targetName = _fieldType.getName();

    ManyToOneField manyToOneField;
    manyToOneField = new ManyToOneField(_sourceType, _fieldName, getCascade(), true);

    EntityType targetType = persistenceUnit.createEntity(getTargetEntity());

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(isFetchLazy());

    if (_joinColumnMap.size() > 0)
      manyToOneField.setJoinColumnMap(_joinColumnMap);

    _sourceType.addField(manyToOneField);

    // jpa/0ge3
    if (_sourceType instanceof MappedSuperclassType)
      return;

    if (_joinColumnMap.size() > 0)
      validateJoinColumns(_field, _fieldName, _joinColumnMap, targetType);

    manyToOneField.init();
  }
}
