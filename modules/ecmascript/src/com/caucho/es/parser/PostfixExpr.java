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
class PostfixExpr extends Expr {
  final static int PREINC = 'i';
  final static int PREDEC = 'd';
  final static int POSTINC = 'I';
  final static int POSTDEC = 'D';
  
  private int code;
  private FieldExpr field;
  private IdExpr id;

  private AssignExpr cheat;

  PostfixExpr(Block block, int code, FieldExpr field)
  {
    super(block);
    
    this.code = code;
    this.field = field;

    if (field != null)
      field.setUsed();
  }

  PostfixExpr(Block block, int code, IdExpr id)
  {
    super(block);
    
    this.code = code;
    this.id = id;
    
    if (id != null)
      id.setUsed();
  }

  int getType()
  {
    if (id == null || ! id.isLocal())
      return TYPE_NUMBER;
    else if (id.getType() == TYPE_INTEGER)
      return TYPE_INTEGER;
    else if (id.getType() == TYPE_NUMBER)
      return TYPE_NUMBER;
    else
      return TYPE_ES;
  }

  void exprStatement(Function fun) throws ESException
  {
    setTop();
    fun.addExpr(this);
  }

  void printInt32Impl() throws IOException
  {
    switch (code) {
    case PREINC:
      if (! noValue)
        cl.print("(");
      cl.print(id.getId());
      cl.print(" = ");
      id.printInt32();
      cl.print(" + 1");
      if (! noValue)
        cl.print(")");
      return;
    case PREDEC:
      if (! noValue)
        cl.print("(");
      cl.print(id.getId());
      cl.print(" = ");
      id.printInt32();
      cl.print(" - 1");
      if (! noValue)
        cl.print(")");
      return;
    case POSTINC:
      if (! noValue) {
        cl.print("_env._first(");
        id.printInt32();
        cl.print(", ");
      }
      cl.print(id.getId());
      cl.print(" = ");
      id.printInt32();
      cl.print(" + 1");
      if (! noValue)
        cl.print(")");
      return;
    case POSTDEC:
      if (! noValue) {
        cl.print("_env._first(");
        id.printInt32();
        cl.print(", ");
      }
      cl.print(id.getId());
      cl.print(" = ");
      id.printInt32();
      cl.print(" - 1");
      if (! noValue)
        cl.print(")");
      return;
    }
  }

  void printNumImpl() throws IOException
  {
    if (id != null && id.isLocal()) {
      switch (code) {
      case PREINC:
        if (! noValue)
          cl.print("(");
        cl.print(id.getId());
        cl.print(" = ");
        id.printNum();
        cl.print(" + 1");
        if (! noValue)
          cl.print(")");
        return;
      case PREDEC:
        if (! noValue)
          cl.print("(");
        cl.print(id.getId());
        cl.print(" = ");
        id.printNum();
        cl.print(" - 1");
        if (! noValue)
          cl.print(")");
        return;
      case POSTINC:
        if (! noValue) {
          cl.print("_env._first(");
          id.printNum();
          cl.print(", ");
        }
        cl.print(id.getId());
        cl.print(" = ");
        id.printNum();
        cl.print(" + 1");
        if (! noValue)
          cl.print(")");
        return;
      case POSTDEC:
        if (! noValue) {
          cl.print("_env._first(");
          id.printNum();
          cl.print(", ");
        }
        cl.print(id.getId());
        cl.print(" = ");
        id.printNum();
        cl.print(" - 1");
        if (! noValue)
          cl.print(")");
        return;
      }
    }

    switch (code) {
    case PREINC:
    case PREDEC:
      cl.print("_env._pre(");
      break;

    case POSTINC:
    case POSTDEC:
      cl.print("_env._post(");
      break;
    }

    if (field != null) {
      field.getExpr().print();
      cl.print(", ");
      field.getField().printStr();
    } else if (function.isGlobalScope()) {
      cl.print("_env.global, ");
      printLiteral(id.getId());
    } else {
      printLiteral(id.getId());
    }

    if (code == PREINC || code == POSTINC)
      cl.print(", 1)");
    else
      cl.print(", -1)");
  }

  void printImpl() throws IOException
  {
    switch (code) {
    case PREINC:
      if (! noValue)
        cl.print("(");
      cl.print(id.getId());
      cl.print(" = ESNumber.create(");
      id.printNum();
      cl.print(" + 1)");
      if (! noValue)
        cl.print(")");
      return;
    case PREDEC:
      if (! noValue)
        cl.print(")");
      cl.print(id.getId());
      cl.print(" = ESNumber.create(");
      id.printNum();
      cl.print(" - 1)");
      if (! noValue)
        cl.print(")");
      return;
    case POSTINC:
      if (! noValue) {
        cl.print("_env._first(");
        id.print();
        cl.print(", ");
      }
      cl.print(id.getId());
      cl.print(" = ESNumber.create(");
      id.printNum();
      cl.print(" + 1)");
      if (! noValue)
        cl.print(")");
      return;
    case POSTDEC:
      if (! noValue) {
        cl.print("_env._first(");
        id.print();
        cl.print(", ");
      }
      cl.print(id.getId());
      cl.print(" = ESNumber.create(");
      id.printNum();
      cl.print(" - 1)");
      if (! noValue)
        cl.print(")");
      return;
    }
    return;
  }
}
