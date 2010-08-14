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
 * @author Scott Ferguson
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.FromItem;
import com.caucho.amber.type.AmberType;
import com.caucho.amber.type.BeanType;
import com.caucho.util.CharBuffer;


/**
 * Bound identifier expression.
 */
public class ElementCollectionIdExpr extends CollectionIdExpr {
  private ElementCollectionExpr _path;
  
  /**
   * Creates a new unbound id expression.
   */
  public ElementCollectionIdExpr(FromItem fromItem, ElementCollectionExpr path)
  {
    super(fromItem, path);

    _path = path;
  }

  /**
   * Returns the entity class.
   */
  public BeanType getTargetType()
  {
    return _path.getTargetType();
  }

  /**
   * Creates a load expression.
   */
  @Override
  public LoadExpr createLoad()
  {
    return new LoadBasicExpr(this);
  }

  /**
   * Generates the select expression.
   */
  @Override
  public void generateSelect(CharBuffer cb)
  {
    _path.generateSelect(cb);
  }
}
