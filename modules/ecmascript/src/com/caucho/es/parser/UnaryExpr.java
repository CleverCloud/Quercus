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

import com.caucho.es.ESException;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class UnaryExpr extends Expr {
  Expr term;
  int op;
  
  UnaryExpr(Block block, Expr term, int op)
  {
    super(block);
    
    this.term = term;
    this.op = op;

    if (term != null)
      term.setUsed();
  }

  void exprStatement(Function fun) throws ESException
  {
    if (op == 'v') {
      fun.addExpr(this);
      isTop = true;
    }
    else
      term.exprStatement(fun);
  }

  int getType()
  {
    switch (op) {
    case '~':
      return TYPE_INTEGER;

    case '-':
    case '+':
      if (term.getType() == TYPE_INTEGER)
        return TYPE_INTEGER;
      else
        return TYPE_NUMBER;

    case '!':
      return TYPE_BOOLEAN;

    case 't':
    case 'v':
      return TYPE_ES;

    default:
      return TYPE_ES;
    }
  }

  void printBooleanImpl() throws IOException
  {
    switch (op) {
    case '!':
      cl.print("(!");
      term.printBoolean();
      cl.print(")");
      break;

    default:
      throw new IOException("foo");
    }
  }
  
  void printInt32Impl() throws IOException
  {
    switch (op) {
    case '~':
      cl.print("(~");
      term.printInt32();
      cl.print(")");
      break;

    case '-':
      cl.print("(-");
      term.printInt32();
      cl.print(")");
      break;
      
    case '+':
      cl.print("(");
      term.printInt32();
      cl.print(")");
      break;
      
    default:
      throw new IOException("foo");
    }
  }

  void printNumImpl() throws IOException
  {
    switch (op) {
    case '-':
      cl.print("(-");
      term.printNum();
      cl.print(")");
      break;
      
    case '+':
      cl.print("(");
      term.printNum();
      cl.print(")");
      break;
      
    default:
      throw new IOException("foo");
    }
  }

  void printImpl() throws IOException
  {
    switch (op) {
    case 't':
      term.print();
      cl.print(".typeof()");
      break;

    case 'v':
      cl.print("_env.doVoid(");
      term.print();
      cl.print(")");
      break;

    default:
      throw new IOException("foo");
    }
  }
}
