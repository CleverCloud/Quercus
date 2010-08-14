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

import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.ManyToManyField;
import com.caucho.amber.field.ManyToOneField;
import com.caucho.amber.field.OneToManyField;
import com.caucho.amber.field.ElementCollectionField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JType;
import com.caucho.bytecode.JTypeWrapper;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.*;

/**
 * JPA 2.0 element collection
 */
class ElementCollectionConfig extends AbstractConfig
{
  private static final L10N L = new L10N(ElementCollectionConfig.class);

  private EntityType _sourceType;
  private AccessibleObject _field;
  private String _fieldName;
  private Class _fieldType;
  
  private Class _targetClass;
  private FetchType _fetch = FetchType.EAGER;

  private CollectionTableConfig _collectionTable;

  ElementCollectionConfig(EntityType sourceType,
                          AccessibleObject field,
                          String fieldName,
                          Class fieldType)
  {
    _sourceType = sourceType;
    
    _field = field;
    _fieldName = fieldName;
    _fieldType = fieldType;

    setFetch(FetchType.LAZY);
    
    introspect();
  }

  public Class getTargetClass()
  {
    return _targetClass;
  }

  public void setTargetClass(Class targetClass)
  {
    _targetClass = targetClass;
  }

  public FetchType getFetch()
  {
    return _fetch;
  }

  public void setFetch(FetchType fetch)
  {
    _fetch = fetch;
  }
  
  public boolean isFetchLazy()
  {
    return _fetch == FetchType.LAZY;
  }
    
  private void introspect()
  {
    introspectTypes();
    
    ElementCollection elementCollectionAnn
      = _field.getAnnotation(ElementCollection.class);
    
    if (elementCollectionAnn != null)
      introspectElementCollection(elementCollectionAnn);
    
    CollectionTable collectionTableAnn
      = _field.getAnnotation(CollectionTable.class);

    if (collectionTableAnn != null)
      _collectionTable = new CollectionTableConfig(collectionTableAnn);
    else {
      _collectionTable
        = new CollectionTableConfig(getRelatedType().getName(), _fieldName);
    }
    
    /*
    OrderBy orderByAnn = _field.getAnnotation(OrderBy.class);
    
    if (orderByAnn != null)
      _orderBy = orderByAnn.value();
    */
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
      setTargetClass(typeArgs[0].getRawType().getJavaClass());
  }
  
  private void introspectElementCollection(ElementCollection eltCollection)
  {
    Class targetClass = eltCollection.targetClass();

    if (! void.class.equals(targetClass))
      setTargetClass(targetClass);
    
    setFetch(eltCollection.fetch());
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

    Class targetClass = getTargetClass();

    if (targetClass == null || void.class.equals(targetClass))
      throw error(_field, L.l("Can't determine targetEntity for {0}.  @OneToMany properties must target @Entity beans.",
                              _fieldName));

    AmberType targetType = persistenceUnit.createType(targetClass);
      
    if (targetType == null) {
      throw error(_field,
                  L.l("targetClass '{0}' is not a known element collection class for {1}.  The targetClass of a @ElementCollection must be a basic class.",
                      targetClass.getName(),
                      _fieldName));
    }

    /*
    if (_orderBy != null)
      calculateOrderBy(_orderBy);
    */
 
    addCollection(targetType);
  }

  private void addCollection(AmberType targetType)
  {
    ElementCollectionField eltCollectionField;

    eltCollectionField
      = new ElementCollectionField(_sourceType, _fieldName);
    eltCollectionField.setType(targetType);
    eltCollectionField.setLazy(isFetchLazy());

    CollectionTableConfig collectionTableConfig = _collectionTable;
 
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();

    String sqlTable = collectionTableConfig.getName();
    AmberTable mapTable = persistenceUnit.createTable(sqlTable);

    HashMap<String, JoinColumnConfig> joinColumnsConfig
      = collectionTableConfig.getJoinColumnMap();

    ArrayList<ForeignColumn> sourceColumns = null;
    
    sourceColumns
      = calculateColumns(_field, _fieldName, mapTable,
                         _sourceType.getTable().getName() + "_",
                         _sourceType,
                         joinColumnsConfig);

    eltCollectionField.setAssociationTable(mapTable);
    eltCollectionField.setTable(sqlTable);

    eltCollectionField.setSourceLink(new LinkColumns(mapTable,
                                                     _sourceType.getTable(),
                                                     sourceColumns));
    
    _sourceType.addField(eltCollectionField);
  }
}
