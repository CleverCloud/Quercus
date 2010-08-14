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
import com.caucho.es.ESException;
import com.caucho.es.ESNumber;
import com.caucho.es.ESWrapperException;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class BinaryExpr extends Expr {
  Expr left;
  Expr right;
  int op;
  String temp;
  boolean isSimple;
  
  protected BinaryExpr(Block block, Expr left, Expr right, int op)
  {
    super(block);
    
    this.left = left;
    this.right = right;
    this.op = op;
    isSimple = left.isSimple();
    if ((op == Lexer.AND || op == Lexer.OR) && ! isSimple)
      temp = block.function.getTemp();
  }

  static Expr create(Block block, Expr left, Expr right, int op)
    throws ESException
  {
    if (left != null)
      left.setUsed();
    if (right != null)
      right.setUsed();
    
    if (! (left instanceof LiteralExpr)  || ! (right instanceof LiteralExpr))
      return new BinaryExpr(block, left, right, op);

    ESBase lvalue = ((LiteralExpr) left).getLiteral();
    ESBase rvalue = ((LiteralExpr) right).getLiteral();
    ESBase value;

    try {
      switch (op) {
      case '*':
        value = ESNumber.create(lvalue.toNum() * rvalue.toNum());
        break;
      
      case '/':
        value = ESNumber.create(lvalue.toNum() / rvalue.toNum());
        break;
      
      case '-':
        value = ESNumber.create(lvalue.toNum() - rvalue.toNum());
        break;
      
      case '%':
        value = ESNumber.create(lvalue.toNum() % rvalue.toNum());
        break;

      case Lexer.LSHIFT:
        value = ESNumber.create(lvalue.toInt32() << rvalue.toInt32());
        break;

      case Lexer.RSHIFT:
        value = ESNumber.create(lvalue.toInt32() >> rvalue.toInt32());
        break;

      case Lexer.URSHIFT:
        value = ESNumber.create(lvalue.toInt32() >>> rvalue.toInt32());
        break;

      case '&':
        value = ESNumber.create(lvalue.toInt32() & rvalue.toInt32());
        break;

      case '|':
        value = ESNumber.create(lvalue.toInt32() | rvalue.toInt32());
        break;

      case '^':
        value = ESNumber.create(lvalue.toInt32() ^ rvalue.toInt32());
        break;

      case Lexer.AND:
        value = lvalue.toBoolean() ? rvalue : lvalue;
        break;

      case Lexer.OR:
        value = lvalue.toBoolean() ? lvalue : rvalue;
        break;

      case ',':
        value = rvalue;
        break;

      default:
        throw new RuntimeException("" + (char) op);
      }
    } catch (Throwable e) {
      throw new ESWrapperException(e);
    }

    return new LiteralExpr(block, value);
  }
  
  int getType()
  {
    
    switch (op) {
    case '*':
    case '/':
    case '%':
      return TYPE_NUMBER;
      
    case '-':
      if (left.getType() == TYPE_INTEGER && right.getType() == TYPE_INTEGER)
        return TYPE_INTEGER;
      else
        return TYPE_NUMBER;

    case Lexer.LSHIFT:
    case Lexer.RSHIFT:
    case Lexer.URSHIFT:
    case '&':
    case '|':
    case '^':
      return TYPE_INTEGER;

    case Lexer.AND:
    case Lexer.OR:
      if (left.getType() == right.getType())
        return left.getType();
      else if (left.isNum() && right.isNum())
        return TYPE_NUMBER;
      else
        return TYPE_ES;

    case ',':
      return TYPE_ES;

    default:
      throw new RuntimeException("" + (char) op + " " + op);
    }
  }

  void exprStatement(Function fun) throws ESException
  {
    switch (op) {
    default:
      left.exprStatement(fun);
      right.exprStatement(fun);
      break;
    }
  }

  void printNumImpl() throws IOException
  {
    cl.print("(");
    
    switch (op) {
    case '*':
    case '/':
    case '-':
    case '%':
      left.printNum();
      cl.print(" " + (char) op + " ");
      right.printNum();
      break;

    case Lexer.AND:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        right.printNum();
        cl.print(":");
        left.printNum();
      }
      else {
        cl.print("(" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? ");
        right.printNum();
        cl.print(":" + temp + ".toNum()");
      }
      break;

    case Lexer.OR:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        left.printNum();
        cl.print(":");
        right.printNum();
      }
      else {
        cl.print("(" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? " + temp + ".toNum() : ");
        right.printNum();
      }
      break;

    default:
      throw new IOException("foo");
    }
    
    cl.print(")");
  }

  void printInt32Impl() throws IOException
  {
    cl.print("(");
    
    switch (op) {
    case '-':
      left.printInt32();
      cl.print(" " + (char) op + " ");
      right.printInt32();
      break;

    case Lexer.LSHIFT:
      left.printInt32();
      cl.print(" << ");
      right.printInt32();
      break;

    case Lexer.RSHIFT:
      left.printInt32();
      cl.print(" >> ");
      right.printInt32();
      break;

    case Lexer.URSHIFT:
      left.printInt32();
      cl.print(" >>> ");
      right.printInt32();
      break;

    case '&':
    case '|':
    case '^':
      left.printInt32();
      cl.print(" " + (char) op + " ");
      right.printInt32();
      break;

    case Lexer.AND:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        right.printInt32();
        cl.print(":");
        left.printInt32();
      }
      else {
        cl.print("(" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? ");
        right.printInt32();
        cl.print(":" + temp + ".toInt32()");
      }
      break;

    case Lexer.OR:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        left.printInt32();
        cl.print(":");
        right.printInt32();
      }
      else {
        cl.print("(" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? " + temp + ".toInt32() : ");
        right.printInt32();
      }
      break;

    default:
      throw new IOException("foo");
    }
    
    cl.print(")");
  }

  void printBoolean() throws IOException
  {
    switch (op) {
    case Lexer.AND:
      cl.print("(");
      left.printBoolean();
      cl.print(" && ");
      right.printBoolean();
      cl.print(")");
      break;

    case Lexer.OR:
      cl.print("(");
      left.printBoolean();
      cl.print(" || ");
      right.printBoolean();
      cl.print(")");
      break;

    default:
      super.printBoolean();
    }
  }

  void printBooleanImpl() throws IOException
  {
    cl.print("(");
    
    switch (op) {
    case Lexer.AND:
      left.printBoolean();
      cl.print(" && ");
      right.printBoolean();
      break;

    case Lexer.OR:
      left.printBoolean();
      cl.print(" || ");
      right.printBoolean();
      break;

    default:
      throw new IOException("foo");
    }
    
    cl.print(")");
  }

  void printImpl() throws IOException
  {
    switch (op) {
    case Lexer.AND:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        right.print();
        cl.print(":");
        left.print();
      }
      else {
        cl.print("((" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? ");
        right.print();
        cl.print(":" + temp + ")");
      }
      break;

    case Lexer.OR:
      if (isSimple) {
        left.printBoolean();
        cl.print(" ? ");
        left.print();
        cl.print(":");
        right.print();
      }
      else {
        cl.print("((" + temp + " = ");
        left.print();
        cl.print(").toBoolean() ? " + temp + " : ");
        right.print();
        cl.print(")");
      }
      break;

    case ',':
      cl.print("_env.comma(");
      left.print();
      cl.print(", ");
      right.print();
      cl.print(")");
      break;

    default:
      throw new IOException("foo");
    }
  }
}
