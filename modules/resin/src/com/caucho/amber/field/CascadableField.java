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

package com.caucho.amber.field;

import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A child field that is cascadable from parent to child
 * on persist, merge, remove or update operations.
 */
abstract public class CascadableField extends AbstractField {
  private static final L10N L = new L10N(CascadableField.class);
  private static final Logger log
    = Logger.getLogger(CascadableField.class.getName());

  private CascadeType[] _cascadeTypes;

  CascadableField(EntityType sourceType)
  {
    super(sourceType);
  }

  CascadableField(EntityType sourceType,
                  String name,
                  CascadeType[] cascadeTypes)
    throws ConfigException
  {
    super(sourceType, name);

    _cascadeTypes = cascadeTypes;
  }

  /**
   * Returns true if this is cascadable
   * from parent to child.
   */
  public boolean isCascade(CascadeType cascade)
  {
    if (_cascadeTypes == null)
      return false;

    for (int i = 0; i < _cascadeTypes.length; i++) {
      if (_cascadeTypes[i] == CascadeType.ALL)
        return true;

      if (_cascadeTypes[i] == cascade)
        return true;
    }

    return false;
  }

  /**
   * Sets the cascade types for this field
   * from parent to child.
   */
  public void setCascadeType(CascadeType[] cascadeTypes)
  {
    _cascadeTypes = cascadeTypes;
  }

  /**
   * Sets the cascade types for this field
   * from parent to child.
   */
  public CascadeType []getCascadeType()
  {
    return _cascadeTypes;
  }

  /**
   * Generates the (pre) cascade operation from
   * parent to this child. This field will only
   * be cascaded first if the operation can be
   * performed with no risk to break FK constraints.
   *
   * Default is to pre-cascade the persist() operation only.
   *
   * Check subclasses for one-to-one, many-to-one,
   * one-to-many and many-to-many relationships.
   */
  public void generatePreCascade(JavaWriter out,
                                 String aConn,
                                 CascadeType cascadeType)
    throws IOException
  {
    if (cascadeType != CascadeType.PERSIST)
      return;

    generateInternalCascade(out, aConn, cascadeType);
  }

  /**
   * Generates the (post) cascade operation from
   * parent to this child. This field will be
   * (post) cascaded if the operation on the
   * parent is required to be performed first
   * to avoid breaking FK constraints.
   *
   * Default is to post-cascade all operations,
   * except the persist() operation.
   *
   * Check subclasses for one-to-one, many-to-one,
   * one-to-many and many-to-many relationships.
   */
  public void generatePostCascade(JavaWriter out,
                                  String aConn,
                                  CascadeType cascadeType)
    throws IOException
  {
    if (cascadeType == CascadeType.PERSIST)
      return;

    generateInternalCascade(out, aConn, cascadeType);
  }

  /**
   * Returns true if the field is cascadable.
   */
  public boolean isCascadable()
  {
    return true;
  }

  /**
   * Generates the flush check for this child.
   * See DependentEntityOneToOneField.
   */
  public boolean generateFlushCheck(JavaWriter out)
    throws IOException
  {
    return false;
  }

  protected void generateInternalCascade(JavaWriter out,
                                         String aConn,
                                         CascadeType cascadeType)
    throws IOException
  {
    if (isCascade(cascadeType)) {
      String getter = generateSuperGetter("this");

      out.println("if (" + getter + " != null) {");
      out.pushDepth();

      out.print(aConn + ".");

      switch (cascadeType) {
      case PERSIST:
        out.print("persistFromCascade");
        break;

      case MERGE:
        out.print("merge");
        break;

      case REMOVE:
        out.print("remove");
        break;

      case REFRESH:
        out.print("refresh");
        break;
      }

      out.println("("+ getter + ");");

      // XXX: jpa/0h27, jpa/0o33
      if (cascadeType == CascadeType.PERSIST
          && this instanceof ManyToOneField) {
        out.println("((com.caucho.amber.entity.Entity) " + getter + ").__caucho_flush();");
      }

      out.popDepth();
      out.println("}");
    }
  }
}
