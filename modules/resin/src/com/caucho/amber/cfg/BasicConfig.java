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

import com.caucho.amber.field.PropertyField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.AmberType;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.util.HashSet;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.TemporalType;


/**
 * <basic> tag in the orm.xml
 */
class BasicConfig extends AbstractConfig
{
  private static final L10N L = new L10N(BasicConfig.class);

  // types allowed with a @Basic annotation
  private static HashSet<String> _basicTypes = new HashSet<String>();
  
  private BaseConfigIntrospector _introspector;

  private BeanType _sourceType;
  private AccessibleObject _field;
  private String _fieldName;
  private Class _fieldType;

  // attributes
  private String _name;
  private FetchType _fetch = FetchType.EAGER;
  private boolean _isOptional = true;

  // elements
  private ColumnConfig _column;
  
  // XXX: lob type?
  private String _lob;
  private TemporalType _temporal;
  private EnumType _enumerated;

  BasicConfig(BaseConfigIntrospector introspector,
              BeanType sourceType,
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

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the fetch type.
   */
  public FetchType getFetch()
  {
    return _fetch;
  }

  /**
   * Sets the fetch type.
   */
  public void setFetch(String fetch)
  {
    _fetch = FetchType.valueOf(fetch);
  }
  
  public boolean isFetchLazy()
  {
    return _fetch == FetchType.LAZY;
  }

  /**
   * Returns the optional.
   */
  public boolean getOptional()
  {
    return _isOptional;
  }

  /**
   * Sets the optional.
   */
  public void setOptional(boolean optional)
  {
    _isOptional = optional;
  }

  /**
   * Returns the column.
   */
  public ColumnConfig getColumn()
  {
    return _column;
  }

  /**
   * Sets the column.
   */
  public void setColumn(ColumnConfig column)
  {
    _column = column;
  }

  /**
   * Returns the lob.
   */
  public String getLob()
  {
    return _lob;
  }

  /**
   * Sets the lob.
   */
  public void setLob(String lob)
  {
    _lob = lob;
  }

  /**
   * Returns the temporal.
   */
  public TemporalType getTemporal()
  {
    return _temporal;
  }

  /**
   * Sets the temporal.
   */
  public void setTemporal(String temporal)
  {
    _temporal = TemporalType.valueOf(temporal);
  }

  /**
   * Returns the enumerated.
   */
  public EnumType getEnumerated()
  {
    return _enumerated;
  }

  /**
   * Sets the enumerated.
   */
  public void setEnumerated(String enumerated)
  {
    _enumerated = EnumType.valueOf(enumerated);
  }

  private void introspect()
    throws ConfigException
  {
    if (_basicTypes.contains(_fieldType.getName())) {
    }
    else if (Serializable.class.isAssignableFrom(_fieldType)) {
    }
    else
      throw error(_field, L.l("{0} is an invalid @Basic type for {1}.",
                              _fieldType, _fieldName));

    Basic basicAnn = _field.getAnnotation(Basic.class);
 
    if (basicAnn != null) {
      _fetch = basicAnn.fetch();
      _isOptional = basicAnn.optional();
    }
   
    Column columnAnn = _field.getAnnotation(Column.class);
    
    if (columnAnn != null)
      _column = new ColumnConfig(columnAnn);
    else
      _column = new ColumnConfig();

    if (_column.getName().equals(""))
      _column.setName(toSqlName(_fieldName));
 
    Enumerated enumeratedAnn = _field.getAnnotation(Enumerated.class);

    if (enumeratedAnn != null)
      _enumerated = enumeratedAnn.value();
  }

  @Override
  public void complete()
  {
    PropertyField property = new PropertyField(_sourceType, _fieldName);
 
    AmberPersistenceUnit persistenceUnit = _sourceType.getPersistenceUnit();
    
    AmberType amberType;

    if (_enumerated == null)
      amberType = persistenceUnit.createType(_fieldType);
    else {
      com.caucho.amber.type.EnumType enumType;

      enumType = persistenceUnit.createEnum(_fieldType.getName(),
                                            _fieldType);

      enumType.setOrdinal(_enumerated == javax.persistence.EnumType.ORDINAL);

      amberType = enumType;
    }

    // jpa/0w24
    property.setType(amberType);

    property.setLazy(isFetchLazy());

    AmberColumn fieldColumn = createColumn(amberType);
    property.setColumn(fieldColumn);
 

    /*
      field.setInsertable(insertable);
      field.setUpdateable(updateable);
    */

    _sourceType.addField(property);
  }

  private AmberColumn createColumn(AmberType amberType)
    throws ConfigException
  {
    String name = _column.getName();

    AmberColumn column = null;

    if (_sourceType instanceof EntityType) {
      EntityType entityType = (EntityType) _sourceType;
      
      String tableName = _column.getTable();
      AmberTable table;

      if (tableName.equals("")) {
        table = entityType.getTable();

        if (table == null)
          throw error(_field, L.l("{0} @Column(name='{1}') is an unknown table.",
                                  _fieldName,
                                  name));
      }
      else {
        table = entityType.getSecondaryTable(tableName);

        if (table == null)
          throw error(_field, L.l("{0} @Column(table='{1}') is an unknown secondary table.",
                                  _fieldName,
                                  tableName));
      }

      column = table.createColumn(name, amberType);
    }
    else { // embeddable
      column = new AmberColumn(null, name, amberType);
    }

    // primaryKey = column.primaryKey();
    column.setUnique(_column.isUnique());
    column.setNotNull(! _column.isNullable());
    //insertable = column.insertable();
    //updateable = column.updatable();
    if (! "".equals(_column.getColumnDefinition()))
      column.setSQLType(_column.getColumnDefinition());
      
    column.setLength(_column.getLength());
    int precision = _column.getPrecision();
    if (precision < 0) {
      throw error(_field, L.l("{0} @Column precision cannot be less than 0.",
                             _fieldName));
    }

    int scale = _column.getScale();
    if (scale < 0) {
      throw error(_field, L.l("{0} @Column scale cannot be less than 0.",
                             _fieldName));
    }

    // this test implicitly works for case where
    // precision is not set explicitly (ie: set to 0 by default)
    // and scale is set
    if (precision < scale) {
      throw error(_field, L.l("{0} @Column scale cannot be greater than precision. Must set precision to a non-zero value before setting scale.",
                             _fieldName));
    }

    if (precision > 0) {
      column.setPrecision(precision);
      column.setScale(scale);
    }

    return column;
  }

  static {
    // non-serializable types allowed with a @Basic annotation
    _basicTypes.add("boolean");
    _basicTypes.add("byte");
    _basicTypes.add("char");
    _basicTypes.add("short");
    _basicTypes.add("int");
    _basicTypes.add("long");
    _basicTypes.add("float");
    _basicTypes.add("double");
    _basicTypes.add("[byte");
    _basicTypes.add("[char");
    _basicTypes.add("[java.lang.Byte");
    _basicTypes.add("[java.lang.Character");
  }
}
