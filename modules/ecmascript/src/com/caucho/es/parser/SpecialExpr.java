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
class SpecialExpr extends Expr {
  final static int THIS = 't';
  final static int HAS_NEXT = 'm';
  final static int NEXT = 'n';
  final static int EXCEPTION = 'e';
  final static int ARRAY = 'a';
  
  private int code;
  private String str;
  private Expr expr;

  SpecialExpr(Block block, int code)
  {
    super(block);

    if (code == THIS)
      block.function.setThis();
    this.code = code;
  }

  SpecialExpr(Block block, int code, String str)
  {
    super(block);
    
    this.code = code;
    this.str = str;
  }

  SpecialExpr(Block block, int code, Expr expr)
  {
    super(block);
    
    this.code = code;
    this.expr = expr;
  }

  int getType()
  {
    switch (code) {
    case HAS_NEXT:
      return TYPE_BOOLEAN;
      
    case THIS:
    case NEXT:
    case EXCEPTION:
    case ARRAY:
      return TYPE_ES;
      
    default:
      throw new RuntimeException();
    }
  }

  void printBooleanImpl() throws IOException
  {
    switch (code) {
    case HAS_NEXT:
      cl.print(str + ".hasNext()");
      break;
      
    default:
      throw new RuntimeException();
    }
  }

  void printImpl() throws IOException
  {
    switch (code) {
    case THIS:
      cl.print("_this");
      break;
      
    case NEXT:
      cl.print("((ESBase) " + str + ".next())");
      break;
      
    case EXCEPTION:
      cl.print("_env.wrap(new ESWrapperException(" + str + "))");
      break;
      
    case ARRAY:
      cl.print("_env.array(");
      expr.print();
      cl.print(")");
      break;
      
    default:
      throw new RuntimeException();
    }
  }
}

