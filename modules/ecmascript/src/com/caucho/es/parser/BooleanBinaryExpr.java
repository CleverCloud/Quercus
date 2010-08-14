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

import com.caucho.es.ESBase;
import com.caucho.es.ESBoolean;
import com.caucho.es.ESException;
import com.caucho.es.ESWrapperException;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class BooleanBinaryExpr extends BinaryExpr {
  private BooleanBinaryExpr(Block block, Expr left, Expr right, int op)
  {
    super(block, left, right, op);
  }

  static Expr create(Block block, Expr left, Expr right, int op)
    throws ESException
  {
    if (! (left instanceof LiteralExpr) || ! (right instanceof LiteralExpr))
      return new BooleanBinaryExpr(block, left, right, op);
      
    ESBase lvalue = ((LiteralExpr) left).getLiteral();
    ESBase rvalue = ((LiteralExpr) right).getLiteral();
    boolean value;

    try {
    switch (op) {
    case '<':
      value = lvalue.lessThan(rvalue, false);
      break;

    case '>':
      value = rvalue.lessThan(lvalue, false);
      break;

    case Lexer.LEQ:
      value = rvalue.lessThan(lvalue, true);
      break;

    case Lexer.GEQ:
      value = lvalue.lessThan(rvalue, true);
      break;

    case Lexer.EQ:
      value = lvalue.ecmaEquals(rvalue);
      break;

    case Lexer.NEQ:
      value = ! lvalue.ecmaEquals(rvalue);
      break;

    case Lexer.STRICT_EQ:
      value = lvalue.equals(rvalue);
      break;

    case Lexer.STRICT_NEQ:
      value = ! lvalue.equals(rvalue);
      break;

    default:
      throw new RuntimeException("foo");
    }
    } catch (Throwable e) {
      throw new ESWrapperException(e);
    }

    return new LiteralExpr(block, ESBoolean.create(value));
  }

  int getType()
  {
    return TYPE_BOOLEAN;
  }

  void printBooleanImpl() throws IOException
  {
    switch (op) {
    case '<':
      if (left.getType() == TYPE_INTEGER && right.getType() == TYPE_INTEGER) {
        cl.print("(");
        left.printInt32();
        cl.print("<");
        right.printInt32();
        cl.print(")");
      } else if (left.isNumeric() || right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("<");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".lessThan(");
        right.print();
        cl.print(", false)");
      }
      break;

    case '>':
      if (left.isNumeric() || right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print(">");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".greaterThan(");
        right.print();
        cl.print(", false)");
      }
      break;

    case Lexer.LEQ:
      if (left.isNumeric() || right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("<=");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".greaterThan(");
        right.print();
        cl.print(", true)");
      }
      break;

    case Lexer.GEQ:
      if (left.isNumeric() || right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print(">=");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".lessThan(");
        right.print();
        cl.print(", true)");
      }
      break;

    case Lexer.EQ:
      if (left.isNumeric() && right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("==");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".ecmaEquals(");
        right.print();
        cl.print(")");
      }
      break;

    case Lexer.NEQ:
      if (left.isNumeric() && right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("!=");
        right.printNum();
        cl.print(")");
      } else {
        cl.print("!");
        left.print();
        cl.print(".ecmaEquals(");
        right.print();
        cl.print(")");
      }
      break;

    case Lexer.STRICT_EQ:
      if (left.isNumeric() && right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("==");
        right.printNum();
        cl.print(")");
      } else {
        left.print();
        cl.print(".equals(");
        right.print();
        cl.print(")");
      }
      break;

    case Lexer.STRICT_NEQ:
      if (left.isNumeric() && right.isNumeric()) {
        cl.print("(");
        left.printNum();
        cl.print("!=");
        right.printNum();
        cl.print(")");
      } else {
        cl.print("!");
        left.print();
        cl.print(".equals(");
        right.print();
        cl.print(")");
      }
      break;

    default:
      throw new IOException("foo");
    }
  }
}


