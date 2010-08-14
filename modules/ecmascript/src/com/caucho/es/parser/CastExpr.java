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
 * CastExpr represents casting.
 */
class CastExpr extends Expr {
  private Expr lhs;
  private TypeExpr typeExpr;

  private CastExpr(Block block, Expr lhs, TypeExpr typeExpr)
    throws ESException
  {
    super(block);
    
    this.lhs = lhs;
    this.typeExpr = typeExpr;
  }

  static CastExpr create(Block block, Expr lhs, TypeExpr typeExpr)
    throws ESException
  {
    return new CastExpr(block, lhs, typeExpr);
  }
    
  /**
   * Returns the JavaScript type class of the casted type.
   */
  int getType()
  {
    return typeExpr.getType();
  }

  /**
   * Returns the actual type expression.
   */
  Expr getTypeExpr()
  {
    return typeExpr;
  }

  /**
   * Prints the casted expression when the result is a JavaScript object.
   */
  void printImpl() throws IOException
  {
    lhs.print();
  }
  
  /**
   * Prints the casted expression when the result is a boolean
   */
  void printBooleanImpl() throws IOException
  {
    lhs.printBoolean();
  }
  
  /**
   * Prints the casted expression when the result is a 32-bit integer
   */
  void printInt32Impl() throws IOException
  {
    if (! typeExpr.getTypeName().equals("int"))
      cl.print("(" + typeExpr.getTypeName() + ") ");
    
    lhs.printInt32();
  }
  
  /**
   * Prints the casted expression when the result is a number (double)
   */
  void printNumImpl() throws IOException
  {
    if (! typeExpr.getTypeName().equals("double"))
      cl.print("(" + typeExpr.getTypeName() + ") ");
    
    lhs.printNum();
  }
  
  /**
   * Prints the casted expression when the result is a string
   */
  void printStringImpl() throws IOException
  {
    if (lhs.getJavaClass().equals(String.class))
      lhs.printStringImpl();
    else {
      cl.print("String.valueOf(");
      lhs.printString();
      cl.print(")");
    }
  }
  
  /**
   * Prints the casted expression when the result is a JavaExpression
   */
  void printJavaImpl() throws IOException
  {
    if (! typeExpr.getJavaClass().isAssignableFrom(lhs.getJavaClass())) {
      cl.print("(");
      printJavaClass(typeExpr.getJavaClass());
      cl.print(") ");
    }

    lhs.printJava();    
  }
    

  /**
   * Marks the value as being used.
   */
  void setUsed()
  {
    lhs.setUsed();
  }
}
