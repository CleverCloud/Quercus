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

package com.caucho.amber.field;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class EmbeddedId extends CompositeId {
  private static final L10N L = new L10N(EmbeddedId.class);
  protected static final Logger log
    = Logger.getLogger(EmbeddedId.class.getName());

  private EmbeddedIdField _embeddedIdField;

  public EmbeddedId(EntityType ownerType, EmbeddedIdField key)
  {
    super(ownerType);

    _embeddedIdField = key;

    for (EmbeddedSubField subField : key.getSubFields()) {
      IdField subKey = (IdField) subField;
      
      addKey(subKey);
    }
  }

  /**
   * Returns true for an identity key.
   */
  public boolean isIdentityGenerator()
  {
    return false;
  }

  /**
   * Returns the embedded id field
   */
  public EmbeddedIdField getEmbeddedIdField()
  {
    return _embeddedIdField;
  }

  /**
   * Returns true if this is an @EmbeddedId
   */
  public boolean isEmbeddedId()
  {
    return _embeddedIdField != null;
  }

  //
  // Java code generation
  //

  /**
   * Generates code to copy to an object.
   */
  public void generateCopy(JavaWriter out,
                           String dest,
                           String source)
    throws IOException
  {
    // XXX: how to make a new instance?

    String value = _embeddedIdField.generateGet(source);
    
    out.println(_embeddedIdField.generateSet(dest, value) + ";");
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    out.print(_embeddedIdField.getEmbeddableType().getJavaTypeName());
    out.print(".__caucho_make");
    out.print("(aConn, " + rs + ", " + indexVar + " + " + index + ")");

    ArrayList<IdField> keys = getKeys();

    index += keys.size();

    return index;
  }

  /**
   * Generates any class prologue.
   */
  @Override
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologueMake(JavaWriter out,
                                   HashSet<Object> completedSet)
    throws IOException
  {
  }

  /**
   * Returns the key for the value
   */
  @Override
  public String generateGet(String objThis)
  {
    return _embeddedIdField.generateGet(objThis);
  }
}
