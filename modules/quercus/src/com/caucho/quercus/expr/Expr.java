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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.*;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.statement.Statement;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a PHP expression.
 */
abstract public class Expr {
  private static final L10N L = new L10N(Expr.class);
  private static final Logger log = Logger.getLogger(Expr.class.getName());

  public static final int COMPILE_ARG_MAX = 5;

  private final Location _location;

  public Expr(Location location)
  {
    _location = location;
  }

  public Expr()
  {
    _location = Location.UNKNOWN;
  }

  /**
   * Returns the location.
   */
  final public Location getLocation()
  {
    return _location;
  }

  /**
   * Returns the filename.
   */
  public String getFileName()
  {
    if (_location != Location.UNKNOWN)
      return _location.getFileName();
    else
      return null;
  }

  /**
   * Returns the line number in the file.
   */
  public int getLine()
  {
    return _location.getLineNumber();
  }

  /**
   * Returns the function name.
   */
  public String getFunctionLocation()
  {
    return "";
  }

  /**
   * Returns the file name and line number, if location is known.
   */
  public String getLocationLine()
  {
    if (_location != Location.UNKNOWN)
      return _location.getFileName() + ":" + getLine() + ": ";
    else
      return "";
  }

  /**
   * Returns true for a reference.
   */
  public boolean isRef()
  {
    return false;
  }

  /**
   * Returns true for a constant expression.
   */
  public boolean isConstant()
  {
    return isLiteral();
  }

  /**
   * Returns true for a literal expression.
   */
  public boolean isLiteral()
  {
    return false;
  }

  /**
   * Returns true if a static true value.
   */
  public boolean isTrue()
  {
    return false;
  }

  /**
   * Returns true if a static false value.
   */
  public boolean isFalse()
  {
    return false;
  }

  /*
   * Returns true if this is an assign expr.
   */
  public boolean isAssign()
  {
    return false;
  }

  /**
   * Returns true for an expression that can be read (only $a[] uses this)
   */
  public boolean canRead()
  {
    return true;
  }

  /**
   * Returns true if the expression evaluates to a boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a long.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a double.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a number.
   */
  public boolean isNumber()
  {
    return isLong() || isDouble();
  }

  /**
   * Returns true if the expression evaluates to a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to an array.
   */
  public boolean isArray()
  {
    return false;
  }
  
  /**
   * Returns true if the expression is a var/left-hand-side.
   */
  public boolean isVar()
  {
    return false;
  }
  
  //
  // expression creation functions
  //

  public Expr createAssign(QuercusParser parser, Expr value)
    throws IOException
  {
    String msg = (L.l("{0} is an invalid left-hand side of an assignment.",
                      this));

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }
  
  /**
   * Creates an assignment using this value as the right hand side.
   */
  public Expr createAssignFrom(QuercusParser parser,
                               AbstractVarExpr leftHandSide)
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createAssign(leftHandSide, this);
  }

  /**
   * Mark as an assignment for a list()
   */
  public void assign(QuercusParser parser)
    throws IOException
  {
    String msg = L.l("{0} is an invalid left-hand side of an assignment.",
                     this);

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }

  public Expr createAssignRef(QuercusParser parser, Expr value)
    throws IOException
  {
    // XXX: need real exception
    String msg = L.l("{0} is an invalid left-hand side of an assignment.",
                     this);

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }

  /**
   * Creates a reference.
   * @param location
   */
  public Expr createRef(QuercusParser parser)
    throws IOException
  {
    return this;
  }

  public Expr createDeref(ExprFactory factory)
    throws IOException
  {
    return this;
  }

  /**
   * Creates a assignment
   * @param location
   */
  public Expr createCopy(ExprFactory factory)
  {
    return this;
  }

  /**
   * Copy for things like $a .= "test";
   * @param location
   */
  /*
  public Expr copy()
  {
    return this;
  }
  */

  /**
   * Creates a field ref
   */
  public Expr createFieldGet(ExprFactory factory,
                             StringValue name)
  {
    return factory.createFieldGet(this, name);
  }

  /**
   * Creates a field ref
   */
  public Expr createFieldGet(ExprFactory factory,
                             Expr name)
  {
    return factory.createFieldVarGet(this, name);
  }
  
  //
  // class field refs $class::$bar
  //
  
  /**
   * Creates a class field $class::foo
   */
  public Expr createClassConst(QuercusParser parser, String name)
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createClassConst(this, name);
  }
  
  /**
   * Creates a class field $class::$foo
   */
  public Expr createClassField(QuercusParser parser, String name)
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createClassField(this, name);
  }
  
  /**
   * Creates a class field $class::${foo}
   */
  public Expr createClassField(QuercusParser parser, Expr name)
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createClassField(this, name);
  }
  
  //
  // unary operations
  //

  /**
   * Creates a assignment
   */
  public Statement createUnset(ExprFactory factory, Location location)
    throws IOException
  {
    throw new IOException(L.l("{0} is an illegal value to unset",
                              this));
  }

  /**
   * Creates an isset expression
   */
  public Expr createIsset(ExprFactory factory)
    throws IOException
  {
    throw new IOException(L.l("{0} is an illegal value to isset",
                              this));
  }
  
  //
  // function call creation
  //

  /**
   * Creates a function call expression
   */
  public Expr createCall(QuercusParser parser,
                         Location location,
                         ArrayList<Expr> args)
    throws IOException
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createVarFunction(location, this, args);
  }
  
  //
  // evaluation
  //

  /**
   * Evaluates the expression as a constant.
   *
   * @return the expression value.
   */
  public Value evalConstant()
  {
    return null;
  }

  /**
   * Evaluates as a constant prefix.
   *
   * @return the expression value as a prefix.
   */
  public Value evalConstantPrefix()
  {
    return evalConstant();
  }

  /**
   * Evaluates as a constant prefix.
   *
   * @return the expression value as a suffix.
   */
  public Value evalConstantSuffix()
  {
    return evalConstant();
  }

  /**
   * Evaluates the expression, returning a Value, never a Var.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public Value eval(Env env);

  /**
   * Evaluates the expression, always returning a variable.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Var evalVar(Env env)
  {
    return eval(env).toVar();
  }

  /**
   * Evaluates the expression, returning a Value.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalValue(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression, returning a Var for variables, and a Value
   * for values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression as a copy.
   * 
   * The default is not to copy because the absence of copying is more
   * testable.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression as a function argument where it is unknown
   * if the value will be used as a reference.
   *
   * @param env the calling environment.
   * @param isTail true for the top expression
   *
   * @return the expression value.
   */
  public Value evalArg(Env env, boolean isTop)
  {
    return eval(env);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalTop(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression, with the object expected to be modified,
   * e.g. from an unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalDirty(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression, creating an array for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression, creating an object for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates an assignment. The value must not be a Var.
   */
  public Value evalAssignValue(Env env, Value value)
  {
    throw new RuntimeException(L.l(
      "{0} is an invalid left-hand side of an assignment.",
      this));
  }

  /**
   * Evaluates an assignment. If the value is a Var, it replaces the
   * current Var.
   */
  public Value evalAssignRef(Env env, Value value)
  {
    throw new RuntimeException(L.l(
      "{0} is an invalid left-hand side of an assignment.",
      this));
  }

  /**
   * Evaluates as an array index assign ($a[index] = value).
   * @return what was assigned
   */
  public Value evalArrayAssign(Env env, Value index, Value value)
  {
    Value var = evalVar(env);

    return var.put(index, value);
  }

  /**
   * Handles post increments.
   */
  public Value evalPostIncrement(Env env, int incr)
  {
    Value value = evalVar(env);

    return value.postincr(incr);
  }

  /**
   * Handles post increments.
   */
  public Value evalPreIncrement(Env env, int incr)
  {
    Value value = evalVar(env);

    return value.preincr(incr);
  }

  /**
   * Evaluates the expression as a string
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public String evalString(Env env)
  {
    Value value = eval(env);

    if (value.isObject())
      return value.toString(env).toString();
    else
      return value.toString();
  }

  /**
   * Evaluates the expression as a string value
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public StringValue evalStringValue(Env env)
  {
    return eval(env).toStringValue(env);
  }

  /**
   * Evaluates the expression as a string
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public char evalChar(Env env)
  {
    return eval(env).toChar();
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    return eval(env).toBoolean();
  }

  /**
   * Evaluates the expression as a long
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public long evalLong(Env env)
  {
    return eval(env).toLong();
  }

  /**
   * Evaluates the expression as a double
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public double evalDouble(Env env)
  {
    return eval(env).toDouble();
  }

  /**
   * Evaluates the expression as an isset() statement.
   */
  public boolean evalIsset(Env env)
  {
    return eval(env).isset();
  }

  /**
   * Evaluates the expression as an array index unset
   */
  public void evalUnsetArray(Env env, Value index)
  {
    Value array = evalDirty(env);

    array.remove(index);
  }
  
  /**
   * Evaluates arguments
   */
  public static Value []evalArgs(Env env, Expr []exprs)
  {
    Value []args = new Value[exprs.length];
    
    for (int i = 0; i < args.length; i++) {
      args[i] = exprs[i].evalArg(env, true);
    }
    
    return args;
  }

  /**
   * Prints to the output as an echo.
   */
  public void print(Env env)
    throws IOException
  {
    eval(env).print(env);
  }

  public String toString()
  {
    return "Expr[]";
  }
}

