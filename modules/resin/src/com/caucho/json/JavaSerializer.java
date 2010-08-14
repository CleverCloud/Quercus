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

package com.caucho.json;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;

public class JavaSerializer implements JsonSerializer {
  private static final Logger log
    = Logger.getLogger(JavaSerializer.class.getName());

  private Class _type;
  private Field []_fields;

  JavaSerializer(Class type)
  {
    _type = type;

    introspect();
  }

  void introspect()
  {
    ArrayList<Field> fields = new ArrayList<Field>();

    introspectFields(fields, _type);

    Collections.sort(fields, new FieldComparator());

    _fields = new Field[fields.size()];
    fields.toArray(_fields);
  }

  private void introspectFields(ArrayList<Field> fields,
                                Class type)
  {
    if (type == null)
      return;

    introspectFields(fields, type.getSuperclass());

    for (Field field : type.getDeclaredFields()) {
      if (Modifier.isTransient(field.getModifiers()))
        continue;
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      field.setAccessible(true);
      fields.add(field);
    }
  }

  public void write(JsonOutput out, Object value)
    throws IOException
  {
    int i = 0;
    out.writeMapBegin();
    for (Field field : _fields) {
      Object fieldValue = null;

      try {
        fieldValue = field.get(value);
      } catch (Exception e) {
        log.warning(out + " cannot get field " + field + " with value " + value);
      }

      if (fieldValue == null)
        continue;

      if (i++ > 0)
        out.writeMapComma();

      out.writeMapEntry(field.getName(), fieldValue);
    }
    out.writeMapEnd();
  }

  static class FieldComparator implements Comparator<Field> {
    public int compare(Field a, Field b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}
