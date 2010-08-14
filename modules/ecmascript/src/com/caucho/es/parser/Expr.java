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
import com.caucho.es.ESId;
import com.caucho.es.ESNumber;

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class Expr {
  protected final static int TYPE_UNKNOWN = 0;
  protected final static int TYPE_ES      = TYPE_UNKNOWN + 1;
  protected final static int TYPE_STRING  = TYPE_ES + 1;
  protected final static int TYPE_NUMBER  = TYPE_STRING + 1;
  protected final static int TYPE_LONG    = TYPE_NUMBER;
  protected final static int TYPE_INTEGER = TYPE_LONG + 1;
  protected final static int TYPE_BOOLEAN = TYPE_INTEGER + 1;
  
  protected final static int TYPE_JAVA    = TYPE_BOOLEAN + 1;
  protected final static int TYPE_VOID    = TYPE_JAVA + 1;

  protected ParseClass cl;
  protected Block block;
  protected Function function;
  protected int type;
  protected Class javaType;
  protected boolean isTop;
  protected boolean noValue;
  
  private String filename;
  private int line;
  protected int withDepth;

  Expr(Block block)
  {
    this.block = block;
    this.withDepth = block.getWithDepth();
    this.function = block.function;
    this.cl = function.cl;
    this.filename = block.getFilename();
    this.line = block.getLine();

    type = TYPE_UNKNOWN;
  }

  String getFilename()
  {
    return filename;
  }

  int getLine()
  {
    return line;
  }

  void killValue()
  {
    noValue = true;
  }

  void setUsed()
  {
    getType();
  }

  void setTop()
  {
    noValue = true;
    isTop = true;
  }

  /**
   * Returns the javascript type of the expression.
   */
  int getType()
  {
    return type;
  }

  Expr getTypeExpr()
  {
    return null;
  }

  /**
   * Returns the Java class representing this type.
   */
  Class getJavaClass()
  {
    if (javaType != null)
      return javaType;
    
    Expr type = getTypeExpr();

    if (! (type instanceof TypeExpr)) {
      switch (getType()) {
      case TYPE_STRING:
        return String.class;

      case TYPE_INTEGER:
        return int.class;

      case TYPE_NUMBER:
        return double.class;

      case TYPE_BOOLEAN:
        return boolean.class;

      default:
        return ESBase.class;
      }
    }

    TypeExpr javaType = (TypeExpr) type;

    return javaType.getJavaClass();
  }

  boolean isSimple()
  {
    return false;
  }

  /**
   * True if the type of this expression is easily converted to a number.
   */
  boolean isNumeric()
  {
    int type = getType();
    
    return type >= TYPE_NUMBER && type <= TYPE_BOOLEAN;
  }

  boolean isNum()
  {
    int type = getType();
    
    return type == TYPE_NUMBER || type == TYPE_INTEGER;
  }

  /**
   * This expression will be used in a boolean context.
   */
  Expr setBoolean()
  {
    return new BooleanExpr(block, this);
  }
  
  Expr next(String iter, Expr lhs) throws ESException
  {
    return lhs.assign(new SpecialExpr(block, SpecialExpr.NEXT, iter));
  }

  /**
   * Gets the field of the current expr
   */
  Expr fieldReference(Expr expr)
  {
    return new FieldExpr(block, this, expr);
  }

  /**
   * Gets the field of the current expr
   */
  Expr fieldReference(ESId id)
    throws ESException
  {
    return new FieldExpr(block, this, new LiteralExpr(block, id));
  }
  
  /**
   * A unary op
   */
  Expr unaryOp(int op)
  {
    return new UnaryExpr(block, this, op);
  }
  
  /**
   * A unary op
   */
  Expr doVoid()
  {
    return new UnaryExpr(block, this, 'v');
  }
  
  /**
   * The typeof operator
   */
  Expr typeof()
  {
    return new UnaryExpr(block, this, 't');
  }
  
  /**
   * The delete operator
   */
  Expr delete()
    throws ESException
  {
    return BinaryExpr.create(block, this,
                             new LiteralExpr(block, ESBoolean.TRUE),
                             ',');
  }

  /**
   * The assignment operator
   */
  Expr assign(Expr value)
    throws ESException
  {
    throw error("illegal left-hand-side of assignment");
  }

  CallExpr startCall()
    throws ESException
  {
    return new CallExpr(block, this, null, false);
  }

  CallExpr startNew()
    throws ESException
  {
    return new CallExpr(block, this, null, true);
  }
  
  /**
   * Handle autoincrement
   */
  Expr prefix(int op)
    throws ESException
  {
    return unaryOp('+').binaryOp(op, op,
                                 new LiteralExpr(block, ESNumber.create(1.0)));
  }
  
  /**
   * Handle autoincrement
   */
  Expr postfix(int op)
  {
    return unaryOp('+');
  }
  
  /**
   * A binary op
   */
  Expr binaryOp(int lex, int op, Expr rexpr)
    throws ESException
  {
    setUsed();
    rexpr.setUsed();
    
    if (lex != '=') {
      switch (op) {
      case '<':
      case '>':
      case Lexer.LEQ:
      case Lexer.GEQ:
      case Lexer.EQ:
      case Lexer.NEQ:
      case Lexer.STRICT_EQ:
      case Lexer.STRICT_NEQ:
        return BooleanBinaryExpr.create(block, this, rexpr, op);
        
      case '+':
        return PlusExpr.create(block, this, rexpr);
        
      default:
        return BinaryExpr.create(block, this, rexpr, op);
      }
    }
    else if (op == '=')
      return assign(rexpr);

    else
      return assign(binaryOp(op, op, rexpr));
  }

  Expr cast(Expr castType)
    throws ESException
  {
    return CastExpr.create(block, this, (TypeExpr) castType);
  }
        
  /**
   * The conditional ? :  operation
   */
  Expr conditional(Expr mexpr, Expr rexpr)
  {
    return new ConditionalExpr(block, this, mexpr, rexpr);
  }

  void printExpr() throws IOException
  {
    print();
  }

  void print() throws IOException
  {
    if (ESBase.class.isAssignableFrom(getJavaClass()) ||
        this instanceof LiteralExpr) {
      printImpl();

      if (isTop)
        cl.println(";");
      return;
    }
    
    switch (getType()) {
    case TYPE_NUMBER:
      if (! noValue)
        cl.print("ESNumber.create(");
      printNumImpl();
      if (! noValue)
        cl.print(")");
      break;
      
    case TYPE_INTEGER:
      if (! noValue)
        cl.print("ESNumber.create(");
      printInt32Impl();
      if (! noValue)
        cl.print(")");
      break;
      
    case TYPE_BOOLEAN:
      if (! noValue)
        cl.print("(");
      printBooleanImpl();
      if (! noValue)
        cl.print("?ESBoolean.TRUE:ESBoolean.FALSE)");
      break;

    case TYPE_STRING:
      if (ESBase.class.isAssignableFrom(getJavaClass()))
        printImpl();
      else {
        if (! noValue)
          cl.print("ESString.create(");
        printStringImpl();
        if (! noValue)
          cl.print(")");
      }
      break;

    case TYPE_JAVA:
      if (! noValue)
        cl.print("_env.wrap(");
      printJavaImpl();
      if (! noValue)
        cl.print(")");
      break;

    case TYPE_VOID:
      if (! noValue)
        cl.print("_env.wrap(");
      printJavaImpl();
      if (! noValue)
        cl.print(")");
      break;

    default:
      printImpl();
    }

    if (isTop)
      cl.println(";");
  }

  void printBoolean() throws IOException
  {
    switch (getType()) {
    case TYPE_NUMBER:
      cl.print("(");
      printNumImpl();
      cl.print("!=0.0)");
      break;
      
    case TYPE_INTEGER:
      cl.print("(");
      printInt32Impl();
      cl.print("!=0)");
      break;
      
    case TYPE_BOOLEAN:
      printBooleanImpl();
      break;

    case TYPE_JAVA:
      cl.print("(");
      printJava();
      cl.print("!=null)");
      break;
      
    default:
      print();
      cl.print(".toBoolean()");
    }
    
    if (isTop)
      cl.println(";");
  }

  void printInt32() throws IOException
  {
    switch (getType()) {
    case TYPE_INTEGER:
      printInt32Impl();
      break;
      
    case TYPE_NUMBER:
      cl.print("((int)");
      printNumImpl();
      cl.print(")");
      break;
      
    case TYPE_BOOLEAN:
      cl.print("(");
      printBooleanImpl();
      cl.print("?1:0)");
      break;
      
    default:
      printImpl();
      cl.print(".toInt32()");
    }
  }
  
  void printInt64() throws IOException
  {
    printInt32();
  }

  void printNum() throws IOException
  {
    switch (getType()) {
    case TYPE_NUMBER:
      printNumImpl();
      break;
      
    case TYPE_INTEGER:
      cl.print("((double)");
      printInt32Impl();
      cl.print(")");
      break;
      
    case TYPE_BOOLEAN:
      cl.print("(");
      printBooleanImpl();
      cl.print("?1.0:0.0)");
      break;
      
    default:
      printImpl();
      cl.print(".toNum()");
    }
  }

  /**
   * Prints the expression as a java object.
   */
  void printJava() throws IOException
  {
    switch (getType()) {
    case TYPE_INTEGER:
      printInt32Impl();
      break;

    case TYPE_BOOLEAN:
      printBooleanImpl();
      break;

    case TYPE_STRING:
      printStringImpl();
      break;

    case TYPE_NUMBER:
      printNumImpl();
      break;

    case TYPE_JAVA:
      printJavaImpl();
      break;
      
    default:
      print();
      cl.print(".toJavaObject()");
      break;
    }
  }

  /**
   * Prints a string value
   */
  void printStr() throws IOException
  {
    print();
    cl.print(".toStr()");
  }

  void printJavaString() throws IOException
  {
    switch (getType()) {
    case TYPE_STRING:
      if (this instanceof LiteralExpr) {
        printStringImpl();
      }
      else {
        cl.print("String.valueOf(");
        printStringImpl();
        cl.print(")");
      }
      break;

    case TYPE_JAVA:
      if (getJavaClass().equals(String.class))
        printJavaImpl();
      else {
        cl.print("String.valueOf(");
        printJavaImpl();
        cl.print(")");
      }
      break;

      // JavaScript's double printing differs from Java's, so 
      // we need to convert to the JavaScript object.
    default:
      print();
      cl.print(".toStr().toString()");
      break;
    }
  }

  void printJavaClass(Class type)
    throws IOException
  {
    if (type.isArray()) {
      printJavaClass(type.getComponentType());
      cl.print("[]");
    }
    else
      cl.print(type.getName());
  }

  /**
   * Print where the result is a string.
   */
  void printString() throws IOException
  {
    switch (getType()) {
    case TYPE_INTEGER:
      printInt32Impl();
      break;

    case TYPE_BOOLEAN:
      printBooleanImpl();
      break;

    case TYPE_STRING:
      printStringImpl();
      break;

    case TYPE_JAVA:
      printJavaImpl();
      break;

      // JavaScript's double printing differs from Java's, so 
      // we need to convert to the JavaScript object.
    case TYPE_NUMBER:
    default:
      print();
      cl.print(".valueOf()");
      break;
    }
  }

  void printImpl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printBooleanImpl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printNumImpl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printInt32Impl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printInt64Impl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printStringImpl() throws IOException
  {
    throw new RuntimeException("no string impl for " + getClass());
  }

  void printJavaImpl() throws IOException
  {
    throw new RuntimeException("" + this);
  }

  void printLiteral(ESBase literal) throws IOException
  {
    cl.printLiteral(literal);
  }

  void exprStatement(Function fun) throws ESException
  {
  }

  private ESException error(String msg)
  {
    return block.error(msg);
  }
}
