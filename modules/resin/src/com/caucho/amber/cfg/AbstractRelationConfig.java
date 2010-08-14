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

import com.caucho.amber.field.IdField;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;


/**
 * The base class for <one-to-one>, <one-to-many> and so on.
 */
abstract class AbstractRelationConfig extends AbstractConfig
{
  private static final L10N L = new L10N(AbstractRelationConfig.class);
  // attributes
  private String _name;
  private Class _targetEntity;
  private FetchType _fetch = FetchType.EAGER;

  // elements
  private JoinTableConfig _joinTable;
  private CascadeType []_cascade = new CascadeType[0];

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public Class getTargetEntity()
  {
    return _targetEntity;
  }

  public void setTargetEntity(Class targetEntity)
  {
    _targetEntity = targetEntity;
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

  public CascadeType []getCascade()
  {
    return _cascade;
  }

  protected void setCascadeTypes(CascadeType []cascade)
  {
    _cascade = cascade;
  }
  
  public void setCascade(CascadeConfig cascade)
  {
    _cascade = cascade.getCascadeTypes();
  }

  public JoinTableConfig getJoinTable()
  {
    return _joinTable;
  }

  public void setJoinTable(JoinTableConfig joinTable)
  {
    _joinTable = joinTable;
  }

  void validateJoinColumns(AccessibleObject field,
                           String fieldName,
                           HashMap<String,JoinColumnConfig> joinColumnMap,
                           EntityType targetType)
    throws ConfigException
  {
    if (joinColumnMap == null)
      return;

    com.caucho.amber.field.Id id = targetType.getId();

    EntityType parentType = targetType;

    int idCols;

    // XXX: jpa/0l48
    while ((idCols = id.getKeyCount()) == 0) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      id = parentType.getId();
    }

    int size;
    Object joinColumnCfg[] = null;

    size = joinColumnMap.size();
    joinColumnCfg = joinColumnMap.values().toArray();
 
    if (idCols != size) {
      throw error(field, L.l("Number of @JoinColumns for '{1}' ({0}) does not match the number of primary key columns for '{3}' ({2}).",
                             "" + size,
                             fieldName,
                             idCols,
                             targetType.getName()));
    }

    for (int i = 0; i < size; i++) {
      String ref;

      ref = ((JoinColumnConfig) joinColumnCfg[i]).getReferencedColumnName();
 
      if (((ref == null) || ref.equals("")) && size > 1)
        throw error(field, L.l("referencedColumnName is required when more than one @JoinColumn is specified."));

      IdField key = getKey(id.getKeys(), ref);

      if (key == null)
        throw error(field, L.l("referencedColumnName '{0}' does not match any key column in '{1}'.",
                               ref, targetType.getName()));
    }
  }

  ArrayList<ForeignColumn>
    calculateColumns(AccessibleObject field,
                     String fieldName,
                     AmberTable mapTable,
                     String prefix,
                     EntityType type,
                     HashMap<String, JoinColumnConfig> joinColumnsConfig)
    throws ConfigException
  {
    if (joinColumnsConfig == null || joinColumnsConfig.size() == 0)
      return calculateColumns(mapTable, prefix, type);

    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    // #1448 not reproduced.
    if (type.getId() == null)
      throw error(field, L.l("Entity {0} has no primary key defined.",
                             type.getName()));

    ArrayList<IdField> idFields = type.getId().getKeys();

    int len = joinColumnsConfig.size();

    if (len != idFields.size()) {
      throw error(field, L.l("@JoinColumns for {0} do not match number of the primary key columns in {1}.  The foreign key columns must match the primary key columns.",
                             fieldName,
                             type.getName()));
    }

    for (JoinColumnConfig joinColumn : joinColumnsConfig.values()) {
      ForeignColumn foreignColumn;

      String name = joinColumn.getName();

      String refName = joinColumn.getReferencedColumnName();

      IdField id = getField(idFields, refName);

      AmberColumn column = id.getColumns().get(0);

      foreignColumn = mapTable.createForeignColumn(name, column);

      columns.add(foreignColumn);
    }

    return columns;
  }

  IdField getField(ArrayList<IdField> fields, String name)
  {
    if (fields.size() == 1)
      return fields.get(0);

    for (IdField field : fields) {
      if (field.getName().equals(name))
        return field;
    }

    if (name == null || name.equals(""))
      throw new ConfigException(L.l("{0}: '{1}' requires a referencedColumnName value because it has multiple target keys.",
                                    getTargetEntity(), getName()));

    throw new ConfigException(L.l("{0}: '{1}' is an unknown field for {2}",
                                  getTargetEntity(), name, getName()));
  }

  IdField getKey(ArrayList<IdField> keys, String name)
  {
    if (keys.size() == 1)
      return keys.get(0);

    for (IdField key : keys) {
      if (key.getName().equals(name))
        return key;
    }

    return null;
  }
  

  static ArrayList<ForeignColumn> calculateColumns(com.caucho.amber.table.AmberTable mapTable,
                                                   String prefix,
                                                   EntityType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    EntityType parentType = type;

    ArrayList<com.caucho.amber.table.AmberColumn> targetIdColumns;

    targetIdColumns = type.getId().getColumns();

    while (targetIdColumns.size() == 0) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (AmberColumn key : targetIdColumns) {
      columns.add(mapTable.createForeignColumn(prefix + key.getName(), key));
    }

    return columns;
  }

  ArrayList<ForeignColumn> calculateColumns(AmberTable mapTable,
                                            EntityType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    EntityType parentType = type;

    ArrayList<AmberColumn> targetIdColumns;

    targetIdColumns = type.getId().getColumns();

    while (targetIdColumns.size() == 0) {
      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (AmberColumn key : targetIdColumns) {
      columns.add(mapTable.createForeignColumn(key.getName(), key));
    }

    return columns;
  }
}
