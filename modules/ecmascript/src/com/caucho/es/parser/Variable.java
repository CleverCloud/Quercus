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

import com.caucho.es.ESId;

/**
 * Expr is an intermediate form representing an expression.
 */
class Variable {
  private Function function;
  private ESId id;
  private boolean isLocal;
  private boolean isScope;
  // Is the variable initialized before it's used?
  private boolean isInitialized;
  // Is the variable ever used?
  private boolean isUsed;
  // Is the variable used by a closure?
  private boolean isClosureVar;
  // True if the variable is a global represented as a java field.
  private boolean isJavaGlobal;
  int type;
  
  Expr fullType;
  // True if the type is declared.
  private boolean isDeclared;

  Variable(Block block, ESId id, Expr type, boolean isLocal)
  {
    this.function = block.function;
    this.id = id;
    this.isLocal = isLocal;
    this.isScope = block.getDepth() > 0 || ! function.isGlobalScope();
    this.type = Expr.TYPE_UNKNOWN;
    this.fullType = (TypeExpr) type;
    
    if (fullType != null) {
      this.type = fullType.getType();
      isDeclared = true;
    }
  }

  /**
   * Returns the variable's name.
   */
  ESId getId()
  {
    return id;
  }

  /**
   * Sets the JavaScript type.
   */
  void setType(int type)
  {
    if (! isUsed)
      this.isInitialized = true;

    if (fullType != null) {
      // XXX: check type
      // throw new RuntimeException("mismatch type");
    }
    else
      this.type = type;
  }

  /**
   * Declare the type of this variable.
   */
  void declare(int javaScriptType, Expr typeExpr)
  {
    if (isDeclared && ! typeExpr.equals(this.fullType))
      throw new IllegalStateException("can't declare " + this + " twice");
    
    setType(javaScriptType, typeExpr);

    isDeclared = true;
  }

  /**
   * Sets the type of this variable.
   */
  void setType(int newType, Expr expr)
  {
    if (! isUsed)
      this.isInitialized = true;

    if (isDeclared)
      return;

    if (newType != Expr.TYPE_UNKNOWN && newType != Expr.TYPE_JAVA) {
      this.type = Expr.TYPE_ES;
      fullType = null;
      return;
    }

    if (fullType == null) {
    }
    else if (! (fullType instanceof JavaTypeExpr) ||
             ! (expr instanceof JavaTypeExpr)) {
      this.type = Expr.TYPE_ES;
      fullType = null;
      return;
    }

    JavaTypeExpr typeExpr = (JavaTypeExpr) expr;
    

    if (fullType != null &&
        ! ((JavaTypeExpr) fullType).getJavaClass().equals(typeExpr.getJavaClass())) {
      this.type = Expr.TYPE_ES;
      fullType = null;
      return;
    }
    
    Class javaClass = typeExpr.getJavaClass();
    if (javaClass.equals(byte.class) ||
        javaClass.equals(short.class) ||
        javaClass.equals(int.class)) {
      this.type = Expr.TYPE_INTEGER;
    }
    else if (javaClass.equals(float.class) ||
             javaClass.equals(double.class)) {
      this.type = Expr.TYPE_NUMBER;
    }
    else if (javaClass.equals(boolean.class)) {
      this.type = Expr.TYPE_BOOLEAN;
    }
    else if (javaClass.isPrimitive()) {
      this.type = Expr.TYPE_ES;
      this.fullType = null;
      return;
    }
    else {
      this.type = newType;
      this.fullType = typeExpr;
    }
  }

  boolean isLocal()
  {
    return isLocal;
  }

  void killLocal()
  {
    isLocal = false;
  }

  /**
   * Returns true if this is a variable that can be used just like a
   * java variable.
   */
  boolean isJavaLocal()
  {
    return isLocal && ! isClosureVar;
  }

  /**
   * True if the variable is a global represented by a global field.
   */
  boolean isJavaGlobal()
  {
    return isJavaGlobal;
  }

  /**
   * Set the variable as a global.
   */
  void setJavaGlobal(boolean isGlobal)
  {
    isJavaGlobal = isGlobal;
    isLocal = false;
    if (type == Expr.TYPE_UNKNOWN)
      type = Expr.TYPE_ES;
  }

  /**
   * Returns the JavaScript type of the variable.  If the type hasn't
   * been inferred yet, the type must be ESBase.
   */
  int getType()
  {
    if (type == Expr.TYPE_UNKNOWN) {
      type = Expr.TYPE_ES;
      isInitialized = false;
    }
    
    return type;
  }

  Expr getTypeExpr()
  {
    return fullType;
  }

  boolean hasInit()
  {
    return type != Expr.TYPE_UNKNOWN && isLocal && isInitialized;
  }

  boolean isScope()
  {
    return isScope;
  }

  void setUsed()
  {
    isUsed = true;
    if (type == Expr.TYPE_UNKNOWN)
      type = Expr.TYPE_ES;
    isInitialized = false;
  }

  void setUsedByClosure()
  {
    isClosureVar = true;
    setUsed();
  }

  boolean isUsed()
  {
    return isUsed;
  }
  
  void setLocal()
  {
    isLocal = true;
  }

  public String toString()
  {
    return "[Variable " + id + " " + fullType + "]";
  }
}
