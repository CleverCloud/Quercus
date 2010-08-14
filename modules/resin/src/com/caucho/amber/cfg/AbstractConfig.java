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

import com.caucho.amber.field.IdField;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JAccessibleObject;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The base class for properties
 */
abstract class AbstractConfig implements Completion
{
  private static final L10N L = new L10N(AbstractConfig.class);
  
  public EntityType getRelatedType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String getName()
  {
    return "unknown";
  }
  
  public Class getTargetClass()
  {
    return void.class;
  }

  public void complete()
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
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
                                    getTargetClass().getSimpleName(),
                                    getName()));

    throw new ConfigException(L.l("{0}: '{1}' is an unknown field for {2}",
                                  getTargetClass().getSimpleName(),
                                  name,
                                  getName()));
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
  
  static AmberColumn findColumn(ArrayList<AmberColumn> columns, String ref)
  {
    if (((ref == null) || ref.equals("")) && columns.size() == 1)
      return columns.get(0);

    for (AmberColumn column : columns) {
      if (column.getName().equals(ref))
        return column;
    }

    return null;
  }

  public static String toSqlName(String name)
  {
    return name; // name.toUpperCase();
  }

  static ConfigException error(AccessibleObject field, String msg)
  {
    if (field instanceof Field)
      return error((Field) field, msg);
    else
      return error((Method) field, msg);
  }
  
  static ConfigException error(Field field, String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = 0; //field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + "." + field.getName() + ": " + msg);
  }
 
  static ConfigException error(Method field, String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = 0; //field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + "." + field.getName() + ": " + msg);
  }
}
