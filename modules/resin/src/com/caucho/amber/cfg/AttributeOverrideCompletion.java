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


import com.caucho.amber.field.*;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;

import com.caucho.util.L10N;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Completion for overrides based on a parent map
 */
class AttributeOverrideCompletion extends CompletionImpl
{
  private static final L10N L = new L10N(AttributeOverrideCompletion.class);
  private Class _type;
  private HashMap<String,ColumnConfig> _overrideMap;

  AttributeOverrideCompletion(BaseConfigIntrospector base,
                              EntityType entityType,
                              Class type,
                              HashMap<String,ColumnConfig> overrideMap)
  {
    super(base, entityType);

    _type = type;
    _overrideMap = overrideMap;
  }

  @Override
  public void complete()
    throws ConfigException
  {

    // jpa/0ge8, jpa/0ge9, jpa/0gea
    // Fields which have not been overridden are added to the
    // entity subclass. This makes the columns to be properly
    // created at each entity table -- not the mapped superclass
    // table, even because the parent might not have a valid table.

    EntityType parent = _entityType.getParentType();

    ArrayList<AmberField> fields = parent.getFields();

    for (AmberField field : fields) {
      _entityType.addField(overrideField(field));
    }

    com.caucho.amber.field.Id parentId = parent.getId();
    com.caucho.amber.field.Id id = _entityType.getId();

    // jpa/0ge6
    if (parentId != null) {
      ArrayList<IdField> keys = parentId.getKeys();
      ArrayList<IdField> mappedKeys = new ArrayList<IdField>();

      for (IdField key : keys) {
        mappedKeys.add((IdField) overrideField(key));
      }

      id = new com.caucho.amber.field.Id(_entityType, mappedKeys);

      _entityType.setId(id);
    }
  }

  private AmberField overrideField(AmberField field)
  {
    String fieldName = field.getName();

    ColumnConfig column = _overrideMap.get(fieldName);

    AmberColumn oldColumn = field.getColumn();
    // XXX: deal with types
    AbstractField newField = (AbstractField) field.override(_entityType);

    if (column != null) {
      AmberTable table = _entityType.getTable();
      AmberColumn newColumn = table.createColumn(column.getName(),
                                            oldColumn.getType());

      newField.setColumn(newColumn);

      newField.init();

      return newField;
    }
    else {
      AmberTable table = _entityType.getTable();
      AmberColumn newColumn = table.createColumn(oldColumn.getName(),
                                            oldColumn.getType());

      newField.setColumn(newColumn);

      newField.init();

      return newField;
    }
  }
}
