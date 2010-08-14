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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es.parser;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class TopExpr extends Expr {
  Expr child;
  Expr type;
  
  TopExpr(Block block, Expr child, Expr type)
  {
    super(block);

    this.child = child;
    this.type = type;
  }

  int getType()
  {
    return type.getType();
  }

  Expr getTypeExpr()
  {
    return type;
  }

  void setUsed()
  {
    child.setUsed();
  }

  void print() throws IOException
  {
    if (type.getType() == TYPE_INTEGER)
      child.printInt32();
    else
      child.print();
  }
}
