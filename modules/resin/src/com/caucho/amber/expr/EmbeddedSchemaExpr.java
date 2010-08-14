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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.expr;

import com.caucho.amber.field.AmberField;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParseException;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.util.L10N;

/**
 * Represents an embedded from a from-item table.
 */
public class EmbeddedSchemaExpr extends SchemaExpr {
  private static final L10N L = new L10N(EmbeddedSchemaExpr.class);

  private EmbeddedExpr _expr;
  private String _name;

  /**
   * Creates the embedded schema.
   */
  public EmbeddedSchemaExpr(EmbeddedExpr expr,
                            String name)
  {
    _expr = expr;
    _name = name;
  }

  /**
   * Returns the tail name.
   */
  public String getTailName()
  {
    return _name;
  }

  /**
   * Returns the embedded expression.
   */
  public EmbeddedExpr getExpr()
  {
    return _expr;
  }

  /**
   * Creates a field-based schema.
   */
  public SchemaExpr createField(QueryParser parser, String name)
    throws QueryParseException
  {
    EmbeddableType type = _expr.getTargetType();

    AmberField field = type.getField(name);

    if (field == null)
      throw parser.error(L.l("{0}: '{1}' is an unknown field.",
                             type.getBeanClass().getName(),
                             name));

    AmberExpr fieldExpr = _expr.createField(parser, name);

    if (fieldExpr instanceof EmbeddedExpr)
      return new EmbeddedSchemaExpr((EmbeddedExpr) fieldExpr, name);

    throw parser.error(L.l("{0}: '{1}' must be a collection or one-to-one relationship.",
                           type.getBeanClass().getName(),
                           name));
  }

  /**
   * Adds the from item.
   */
  public FromItem addFromItem(QueryParser parser, String id)
    throws QueryParseException
  {
    _expr = (EmbeddedExpr) _expr.bindSelect(parser, id);

    FromItem fromItem = _expr.bindSubPath(parser);

    return fromItem;
  }

  public String toString()
  {
    return "EmbeddedSchemaExpr[" + _name + "]";
  }
}
