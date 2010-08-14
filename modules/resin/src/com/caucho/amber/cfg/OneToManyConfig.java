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
import com.caucho.amber.field.ManyToManyField;
import com.caucho.amber.field.ManyToOneField;
import com.caucho.amber.field.OneToManyField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JType;
import com.caucho.bytecode.JTypeWrapper;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

/**
 * <one-to-many> tag in orm.xml
 */
class OneToManyConfig extends AbstractRelationConfig
{
  private static final L10N L = new L10N(OneToManyConfig.class);

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

  private HashMap<String, JoinColumnConfig> _joinColumnMap
    = new HashMap<String, JoinColumnConfig>();
  
  private ArrayList<String> _orderByFields = null;
  private ArrayList<Boolean> _orderByAscending = null;

  OneToManyConfig(BaseConfigIntrospector introspector,
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

    setName(fieldName);
    
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
    
    OneToMany oneToMany = _field.getAnnotation(OneToMany.class);
    
    if (oneToMany != null)
      introspectOneToMany(oneToMany);
    
    JoinTable joinTableAnn = _field.getAnnotation(JoinTable.class);

    if (joinTableAnn != null)
      setJoinTable(new JoinTableConfig(joinTableAnn));

    
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
    
    OrderBy orderByAnn = _field.getAnnotation(OrderBy.class);
    
    if (orderByAnn != null)
      _orderBy = orderByAnn.value();
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

    if (typeArgs.length > 0)
      setTargetEntity(typeArgs[0].getRawType().getJavaClass());
  }
  
  private void introspectOneToMany(OneToMany oneToMany)
  {
    Class targetClass = oneToMany.targetEntity();

    if (! void.class.equals(targetClass))
      setTargetEntity(targetClass);
    
    setCascadeTypes(oneToMany.cascade());
    setFetch(oneToMany.fetch());
    
    _mappedBy = oneToMany.mappedBy();
  }
  
  private void introspectJoinColumns(JoinColumn []joinColumns)
  {
    for (JoinColumn joinColumn : joinColumns) {
      addJoinColumn(new JoinColumnConfig(joinColumn));
    }
  }
  
  private void calculateOrderBy(String orderBy)
  {

    _orderByFields = new ArrayList<String>();
    _orderByAscending = new ArrayList<Boolean>();

    int len = orderBy.length();

    int i = 0;

    while (i < len) {

      int index = orderBy.indexOf(",", i);

      if (index < 0)
        index = len;

      String orderByField = orderBy.substring(i, index);

      i += index;

      // ASC or DESC
      index = orderByField.toUpperCase().lastIndexOf("SC");

      Boolean asc = Boolean.TRUE;

      if (index > 1) {
        if (orderByField.charAt(index - 1) != 'E') {
          // field ASC or default
          if (orderByField.charAt(index - 1) == 'A' &&
              Character.isSpaceChar(orderByField.charAt(index - 2))) {
            index -= 2;
          }
        }
        else if (index > 2 &&
                 orderByField.charAt(index - 2) == 'D' &&
                 Character.isSpaceChar(orderByField.charAt(index - 3))) {

          asc = Boolean.FALSE;
          index -= 3;
        }
      }

      if (index > 0)
        orderByField = orderByField.substring(0, index).trim();

      _orderByFields.add(orderByField);
      _orderByAscending.add(asc);
    }
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

  @Override
  public EntityType getRelatedType()
  {
    return _sourceType;
  }
  
  @Override
  public void complete()
  {
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    Class targetEntity = getTargetEntity();

    if (targetEntity == null)
      throw error(_field, L.l("Can't determine targetEntity for {0}.  @OneToMany properties must target @Entity beans.",
                              _fieldName));

    EntityType targetType = persistenceUnit.getEntityType(targetEntity);
      
    if (targetType == null) {
      throw error(_field,
                  L.l("targetEntity '{0}' is not an @Entity bean for {1}.  The targetEntity of a @OneToMany collection must be an @Entity bean.",
                      targetEntity.getName(),
                      _fieldName));
    }

    if (_orderBy != null)
      calculateOrderBy(_orderBy);
 
    if (! isOwningSide()) {
      oneToManyBidirectional(targetType);
    }
    else {
      oneToManyUnidirectional(targetType);
    }
  }

  private void oneToManyBidirectional(EntityType targetType)
  {
    JoinTableConfig joinTableConfig = getJoinTable();

    if (joinTableConfig != null) {
      throw error(_field,
                  L.l("Bidirectional @ManyToOne property {0} may not have a @JoinTable annotation.",
                      _fieldName));
    }
      
    String mappedBy = getMappedBy();

    ManyToOneField sourceField = getSourceField(targetType,
                                                mappedBy,
                                                null);

    if (sourceField == null)
      throw error(_field, L.l("'{1}' is an unknown column in '{0}' for @ManyToOne(mappedBy={1}).",
                              targetType.getName(),
                              mappedBy));

    OneToManyField oneToMany;

    oneToMany = new OneToManyField(_sourceType, _fieldName, getCascade());
    oneToMany.setSourceField(sourceField);
    oneToMany.setOrderBy(_orderByFields, _orderByAscending);
    oneToMany.setLazy(isFetchLazy());

      /*
      if (! _annotationCfg.isNull()) {
        String key = mapKeyAnn.name();

        String getter = "get" +
          Character.toUpperCase(key.charAt(0)) + key.substring(1);

        Method method = targetType.getGetter(getter);

        if (method == null) {
          throw error(_field,
                      L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @OneToMany targetEntity is incorrect.",
                          targetName, key));
        }

        oneToMany.setMapKey(key);
      }
       */

    _sourceType.addField(oneToMany);
  }

  private void oneToManyUnidirectional(EntityType targetType)
  {
    ManyToManyField manyToManyField;

    manyToManyField = new ManyToManyField(_sourceType, _fieldName, 
                                          getCascade());
    manyToManyField.setType(targetType);

    String sqlTable = _sourceType.getTable().getName() + "_" + targetType.getTable().getName();

    JoinTable joinTableAnn = _field.getAnnotation(javax.persistence.JoinTable.class);

    JoinTableConfig joinTableConfig = getJoinTable();

    AmberTable mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;
 
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    if (joinTableConfig != null) {
      HashMap<String, JoinColumnConfig> joinColumnsConfig = null;
      HashMap<String, JoinColumnConfig> inverseJoinColumnsConfig = null;

      if (! joinTableConfig.getName().equals(""))
        sqlTable = joinTableConfig.getName();

      joinColumnsConfig = joinTableConfig.getJoinColumnMap();
      inverseJoinColumnsConfig = joinTableConfig.getInverseJoinColumnMap();
 
      mapTable = persistenceUnit.createTable(sqlTable);

      sourceColumns
        = calculateColumns(_field, _fieldName, mapTable,
                           _sourceType.getTable().getName() + "_",
                           _sourceType,
                           joinColumnsConfig);

      targetColumns = calculateColumns(_field, _fieldName, mapTable,
                                       targetType.getTable().getName() + "_",
                                       targetType,
                                       inverseJoinColumnsConfig);
    }
    else {
      mapTable = persistenceUnit.createTable(sqlTable);

      sourceColumns = calculateColumns(mapTable,
                                       _sourceType.getTable().getName() + "_",
                                       _sourceType);

      targetColumns
        = calculateColumns(mapTable,
                           // jpa/0j40
                           toSqlName(_fieldName) + "_",
                           targetType);
    }

    manyToManyField.setAssociationTable(mapTable);
    manyToManyField.setTable(sqlTable);

    manyToManyField.setSourceLink(new LinkColumns(mapTable,
                                                  _sourceType.getTable(),
                                                  sourceColumns));

    manyToManyField.setTargetLink(new LinkColumns(mapTable,
                                                  targetType.getTable(),
                                                  targetColumns));
      /*
      if (mapKey != null) {

        String key;

        if (mapKeyAnn != null)
          key = mapKeyAnn.name();
        else
          key = mapKeyConfig.getName();

        String getter = "get" +
          Character.toUpperCase(key.charAt(0)) + key.substring(1);

        Method method = targetType.getGetter(getter);

        if (method == null) {
          throw error(_field,
                      L.l("targetEntity '{0}' has no getter for field named '{1}'. Either the @MapKey name or the @ManyToMany targetEntity is incorrect.",
                          targetName, key));
        }

        manyToManyField.setMapKey(key);
      }
      */
    _sourceType.addField(manyToManyField);
  }

  ManyToOneField getSourceField(EntityType targetType,
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
    }
    while (targetType != null);

    return null;
  }
}
