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
import com.caucho.es.ESString;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class PlusExpr extends Expr {
  Expr left;
  Expr right;
  int op;
  
  private PlusExpr(Block block, Expr left, Expr right)
  {
    super(block);
    
    this.left = left;
    this.right = right;
  }

  /**
   * When possible, reorder the arguments to avoid allocating an extra
   * char buffer.
   *
   * x + ("b" + y) -> (x + "b") + y
   */
  static Expr create(Block block, Expr left, Expr right)
    throws ESException
  {
    if (left instanceof LiteralExpr &&
        right instanceof LiteralExpr &&
        (left.getType() == TYPE_STRING || right.getType() == TYPE_STRING)) {
      LiteralExpr leftLit = (LiteralExpr) left;
      LiteralExpr rightLit = (LiteralExpr) right;
      
      String string = leftLit.getLiteral().toString() + rightLit.getLiteral();
      
      return new LiteralExpr(block, ESString.create(string));
    }
    
    if (! (right instanceof PlusExpr) ||
        right.getType() != TYPE_STRING)
      return new PlusExpr(block, left, right);

    PlusExpr pright = (PlusExpr) right;

    if (pright.left.getType() == TYPE_STRING)
      return create(block, create(block, left, pright.left), pright.right);
    else
      return new PlusExpr(block, left, right);
  }

  int getType()
  {
    int ltype = left.getType();
    int rtype = right.getType();

    if (ltype == TYPE_INTEGER && rtype == TYPE_INTEGER)
      return TYPE_INTEGER;
    else if (ltype == TYPE_STRING || rtype == TYPE_STRING)
      return TYPE_STRING;
    else if (left.isNumeric() && right.isNumeric()) {
      return TYPE_NUMBER;
    }
    else
      return TYPE_ES;
  }

  void exprStatement(Function fun) throws ESException
  {
    left.exprStatement(fun);
    right.exprStatement(fun);
  }

  void printInt32Impl() throws IOException
  {
    cl.print("(");
    left.printInt32();
    cl.print("+");
    right.printInt32();
    cl.print(")");
  }

  void printNumImpl() throws IOException
  {
    cl.print("(");
    left.printNum();
    cl.print("+");
    right.printNum();
    cl.print(")");
  }

  void printStringImpl() throws IOException
  {
    printCharBufferAppend();
    cl.print(".close()");
  }

  void printCharBufferAppend() throws IOException
  {
    if (left instanceof PlusExpr && left.getType() == TYPE_STRING) {
      ((PlusExpr) left).printCharBufferAppend();
    }
    else {
      cl.print("CharBuffer.allocate()");
      cl.print(".append(");
      left.printJavaString();
      cl.print(")");
    }

    cl.print(".append(");
    right.printJavaString();
    cl.print(")");
  }

  void printImpl() throws IOException
  {
    left.print();
    cl.print(".plus(");
    right.print();
    cl.print(")");
  }
}
