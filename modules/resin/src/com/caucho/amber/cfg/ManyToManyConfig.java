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

import com.caucho.amber.field.ManyToManyField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JType;
import com.caucho.bytecode.JTypeWrapper;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;


/**
 * The <many-to-many> tag in orm.xml
 */
class ManyToManyConfig extends AbstractRelationConfig
{
  private static final L10N L = new L10N(ManyToManyConfig.class);

  private BaseConfigIntrospector _introspector;

  private EntityType _sourceType;
  private AccessibleObject _field;
  private String _fieldName;
  private Class _fieldType;

  // attributes
  private String _mappedBy;

  // elements
  private MapKeyConfig _mapKey;

  private String _orderBy;

  ManyToManyConfig(BaseConfigIntrospector introspector,
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

    setName(_fieldName);
    
    introspect();
  }


  public String getMappedBy()
  {
    return _mappedBy;
  }

  public void setMappedBy(String mappedBy)
  {
    _mappedBy = mappedBy;
  }

  public MapKeyConfig getMapKey()
  {
    return _mapKey;
  }

  public void setMapKey(MapKeyConfig mapKey)
  {
    _mapKey = mapKey;
  }

  public String getOrderBy()
  {
    return _orderBy;
  }

  public void setOrderBy(String orderBy)
  {
    _orderBy = orderBy;
  }
  
  public boolean isOwningSide()
  {
    return "".equals(_mappedBy);
  }
    
  private void introspect()
  {
    introspectTypes();
    
    ManyToMany manyToMany = _field.getAnnotation(ManyToMany.class);
    
    if (manyToMany != null)
      introspectManyToMany(manyToMany);
    
    JoinTable joinTableAnn = _field.getAnnotation(JoinTable.class);

    if (joinTableAnn != null)
      setJoinTable(new JoinTableConfig(joinTableAnn));
  }

  private void introspectTypes()
  {
    Type retType;

    if (_field instanceof Field)
      retType = ((Field) _field).getGenericType();
    else
      retType = ((Method) _field).getGenericReturnType();

    ClassLoader loader = _sourceType.getPersistenceUnit().getTempClassLoader();
    
    JType type = JTypeWrapper.create(retType, loader);

    JType []typeArgs = type.getActualTypeArguments();

    if (typeArgs.length > 0) {
      setTargetEntity(typeArgs[0].getRawType().getJavaClass());
    }
  }
  
  private void introspectManyToMany(ManyToMany manyToMany)
  {
    Class targetClass = manyToMany.targetEntity();

    if (! void.class.equals(targetClass))
      setTargetEntity(targetClass);
    
    setCascadeTypes(manyToMany.cascade());
    setFetch(manyToMany.fetch());
    
    _mappedBy = manyToMany.mappedBy();
  }
  
  private void introspectJoinColumns(JoinColumn []joinColumns)
  {
    for (JoinColumn joinColumn : joinColumns) {
      // addJoinColumn(new JoinColumnConfig(joinColumn));
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
    addManyToMany(_sourceType, _field, _fieldName, _fieldType);
  }
  
  void addManyToMany(EntityType sourceType,
                     AccessibleObject field,
                     String fieldName,
                     Class fieldType)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();
  
    Class targetClass = getTargetEntity();

    if (targetClass == null || void.class.equals(targetClass))
      throw error(field, L.l("Can't determine targetEntity for {0}.  @ManyToMany properties must target @Entity beans.",
                             _fieldName));

    EntityType targetType = persistenceUnit.getEntityType(targetClass);

    if (targetType == null)
      throw error(field,
                  L.l("targetEntity '{0}' is not an @Entity bean for {1}.  The targetEntity of a @ManyToMany collection must be an @Entity bean.",
                      targetClass.getName(),
                      _fieldName));


    if (! isOwningSide())
      addDependentSide(targetType);
    else
      addOwningSide(targetType);
  }
  
  private void addOwningSide(EntityType targetType)
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();
    ManyToManyField manyToManyField;

    manyToManyField
      = new ManyToManyField(_sourceType, _fieldName, getCascade());
    
    manyToManyField.setType(targetType);

    String sqlTable = _sourceType.getTable().getName() + "_" +
      targetType.getTable().getName();

    JoinTableConfig joinTableConfig = getJoinTable();

    AmberTable mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;

    HashMap<String,JoinColumnConfig> joinColumnsConfig = null;
    HashMap<String,JoinColumnConfig> inverseJoinColumnsConfig = null;

    if (joinTableConfig != null) {
      String joinTableName = joinTableConfig.getName();

      joinColumnsConfig = joinTableConfig.getJoinColumnMap();
      inverseJoinColumnsConfig = joinTableConfig.getInverseJoinColumnMap();

      if (joinColumnsConfig != null 
          && joinColumnsConfig.size() > 0)
        manyToManyField.setJoinColumns(true);

      if (inverseJoinColumnsConfig != null
          && inverseJoinColumnsConfig.size() > 0)
        manyToManyField.setInverseJoinColumns(true);
 
      if (! joinTableName.equals(""))
        sqlTable = joinTableName;
    }

    mapTable = persistenceUnit.createTable(sqlTable);

    sourceColumns
      = calculateColumns(_field, _fieldName, mapTable,
                         _sourceType.getTable().getName() + "_",
                         _sourceType, joinColumnsConfig);

    targetColumns
      = calculateColumns(_field, _fieldName, mapTable,
                         targetType.getTable().getName() + "_",
                         targetType,
                         inverseJoinColumnsConfig);
    

    manyToManyField.setAssociationTable(mapTable);
    manyToManyField.setTable(sqlTable);
    manyToManyField.setLazy(isFetchLazy());

    manyToManyField.setSourceLink(new LinkColumns(mapTable,
                                                  _sourceType.getTable(),
                                                  sourceColumns));

    manyToManyField.setTargetLink(new LinkColumns(mapTable,
                                                  targetType.getTable(),
                                                  targetColumns));

    _sourceType.addField(manyToManyField);
  }
  
  private void addMapKey()
  {
    MapKeyConfig mapKeyConfig = _mapKey;
    
    String key = mapKeyConfig.getName();

    String getter = "get" +
      Character.toUpperCase(key.charAt(0)) + key.substring(1);

    /*
      Method method = targetType.getGetter(getter);

      if (method == null) {
      throw error(_field,
      L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @ManyToMany targetEntity is incorrect.",
      targetName, key));
      }

      // manyToManyField.setMapKey(key);
      */
  }

  private void addDependentSide(EntityType targetType)
  {
    ManyToManyField sourceField
      = (ManyToManyField) targetType.getField(getMappedBy());

    if (sourceField == null)
      throw error(_field, L.l("Unable to find the associated field in '{0}' for a @ManyToMany relationship from '{1}'",
                              targetType.getName(), _fieldName));

    ManyToManyField manyToManyField;

 
    manyToManyField = new ManyToManyField(_sourceType,
                                          _fieldName,
                                          sourceField,
                                          getCascade());
    manyToManyField.setType(targetType);
    _sourceType.addField(manyToManyField);

    // jpa/0i5-
    // Update column names for bidirectional many-to-many

    if (! sourceField.hasJoinColumns()) {
      LinkColumns sourceLink = sourceField.getSourceLink();
      ArrayList<ForeignColumn> columns = sourceLink.getColumns();
      for (ForeignColumn column : columns) {
        String columnName = column.getName();
        columnName = columnName.substring(columnName.indexOf('_'));
        columnName = toSqlName(manyToManyField.getName()) + columnName;
        column.setName(columnName);
      }
    }

    if (! sourceField.hasInverseJoinColumns()) {
      LinkColumns targetLink = sourceField.getTargetLink();
      ArrayList<ForeignColumn> columns = targetLink.getColumns();
      for (ForeignColumn column : columns) {
        String columnName = column.getName();
        columnName = columnName.substring(columnName.indexOf('_'));
        columnName = toSqlName(sourceField.getName()) + columnName;
        column.setName(columnName);
      }
    }

    return;
  }
}
