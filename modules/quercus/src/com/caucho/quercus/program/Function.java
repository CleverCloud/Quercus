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

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvVar;
import com.caucho.quercus.env.EnvVarImpl;
import com.caucho.quercus.env.NullThisValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.ParamRequiredExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.statement.*;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents sequence of statements.
 */
public class Function extends AbstractFunction {
  private static final Logger log = Logger.getLogger(Function.class.getName());
  private static final L10N L = new L10N(Function.class);

  protected final FunctionInfo _info;
  protected final boolean _isReturnsReference;

  protected final String _name;
  protected final Arg []_args;
  protected final Statement _statement;

  protected boolean _hasReturn;
  
  protected String _comment;
  
  protected Arg []_closureUseArgs;

  Function(Location location,
           String name,
           FunctionInfo info,
           Arg []args,
           Statement []statements)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();
    _args = args;
    _statement = new BlockStatement(location, statements);

    setGlobal(info.isPageStatic());
    setClosure(info.isClosure());
    
    _isStatic = true;
  }

  public Function(ExprFactory exprFactory,
                  Location location,
                  String name,
                  FunctionInfo info,
                  Arg []args,
                  Statement []statements)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();

    _args = new Arg[args.length];
    
    System.arraycopy(args, 0, _args, 0, args.length);

    _statement = exprFactory.createBlock(location, statements);

    setGlobal(info.isPageStatic());
    setClosure(info.isClosure());
    
    _isStatic = true;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }
  
  /*
   * Returns the declaring class
   */
  @Override
  public ClassDef getDeclaringClass()
  {
    return _info.getDeclaringClass();
  }
  
  public FunctionInfo getInfo()
  {
    return _info;
  }
  
  protected boolean isMethod()
  {
    return getDeclaringClassName() != null;
  }
  
  /*
   * Returns the declaring class
   */
  @Override
  public String getDeclaringClassName()
  {
    ClassDef declaringClass = _info.getDeclaringClass();
    
    if (declaringClass != null)
      return declaringClass.getName();
    else
      return null;
  }

  /**
   * Returns the args.
   */
  public Arg []getArgs()
  {
    return _args;
  }

  /**
   * Returns the args.
   */
  public Arg []getClosureUseArgs()
  {
    return _closureUseArgs;
  }

  /**
   * Returns the args.
   */
  public void setClosureUseArgs(Arg []useArgs)
  {
    _closureUseArgs = useArgs;
  }

  public boolean isObjectMethod()
  {
    return false;
  }

  /**
   * True for a returns reference.
   */
  public boolean isReturnsReference()
  {
    return _isReturnsReference;
  }
  
  /**
   * Sets the documentation for this function.
   */
  public void setComment(String comment)
  {
    _comment = comment;
  }
  
  /**
   * Returns the documentation for this function.
   */
  @Override
  public String getComment()
  {
    return _comment;
  }

  public Value execute(Env env)
  {
    return null;
  }

  /**
   * Evaluates a function's argument, handling ref vs non-ref
   */
  @Override
  public Value []evalArguments(Env env, Expr fun, Expr []args)
  {
    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length)
        arg = _args[i];

      if (arg == null)
        values[i] = args[i].eval(env).copy();
      else if (arg.isReference())
        values[i] = args[i].evalVar(env);
      else {
        // php/0d04
        values[i] = args[i].eval(env);
      }
    }

    return values;
  }

  public Value call(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callCopy(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callRef(Env env, Expr []args)
  {
    return callImpl(env, args, true);
  }

  private Value callImpl(Env env, Expr []args, boolean isRef)
  {
    HashMap<StringValue,EnvVar> map = new HashMap<StringValue,EnvVar>();

    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
        values[i] = args[i].eval(env).copy();
      }
      else if (arg.isReference()) {
        values[i] = args[i].evalVar(env);

        map.put(arg.getName(), new EnvVarImpl(values[i].toLocalVarDeclAsRef()));
      }
      else {
        // php/0d04
        values[i] = args[i].eval(env);

        Var var = values[i].toVar();

        map.put(arg.getName(), new EnvVarImpl(var));

        values[i] = var.toValue();
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(arg.getName(),
                new EnvVarImpl(defaultExpr.evalVar(env).toVar()));
      else {
        map.put(arg.getName(),
                new EnvVarImpl(defaultExpr.eval(env).copy().toVar()));
      }
    }

    Map<StringValue,EnvVar> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(values); // php/0476
    Value oldThis;

    if (isStatic()) {
      // php/0967
      oldThis = env.setThis(env.getCallingClass());
    }
    else
      oldThis = env.getThis();

    try {
      Value value = _statement.execute(env);

      if (value != null)
        return value;
      else if (_info.isReturnsReference())
        return new Var();
      else
        return NullValue.NULL;
      /*
      else if (_isReturnsReference && isRef)
        return value;
      else
        return value.copyReturn();
        */
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
      env.setThis(oldThis);
    }
  }

  @Override
  public Value call(Env env, Value []args)
  {
    return callImpl(env, args, false, null, null);
  }

  @Override
  public Value callCopy(Env env, Value []args)
  {
    return callImpl(env, args, false, null, null).copy();
  }

  @Override
  public Value callRef(Env env, Value []args)
  {
    return callImpl(env, args, true, null, null);
  }

  public Value callImpl(Env env, Value []args, boolean isRef,
                        Arg []useParams, Value []useArgs)
  {
    HashMap<StringValue,EnvVar> map = new HashMap<StringValue,EnvVar>(8);

    if (useParams != null) {
      for (int i = 0; i < useParams.length; i++) {
        map.put(useParams[i].getName(), new EnvVarImpl(useArgs[i].toVar()));
      }
    }
      
    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
      }
      else if (arg.isReference()) {
        map.put(arg.getName(), new EnvVarImpl(args[i].toLocalVarDeclAsRef()));
      }
      else {
        // XXX: php/1708, toVar() may be doing another copy()
        Var var = args[i].toLocalVar();

        if (arg.getExpectedClass() != null
            && arg.getDefault() instanceof ParamRequiredExpr) {
          env.checkTypeHint(var,
                            arg.getExpectedClass(),
                            arg.getName().toString(),
                            getName());
        }

        // quercus/0d04
        map.put(arg.getName(), new EnvVarImpl(var));
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(
          arg.getName(), new EnvVarImpl(defaultExpr.evalVar(env).toVar()));
      else {
        map.put(
          arg.getName(), new EnvVarImpl(defaultExpr.eval(env).toLocalVar()));
      }
    }

    Map<StringValue,EnvVar> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(args);
    Value oldThis;

    if (_info.isMethod()) {
      oldThis = env.getThis();
    }
    else {
      // php/0967, php/091i
      oldThis = env.setThis(NullThisValue.NULL);
    }

    try {
      Value value = _statement.execute(env);

      if (value == null) {
        if (_isReturnsReference)
          return new Var();
        else
          return NullValue.NULL;
      }
      else if (_isReturnsReference)
        return value;
      else
        return value.toValue().copy();
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
      env.setThis(oldThis);
    }
  }
  
  //
  // method
  //

  @Override
  public Value callMethod(Env env,
                          QuercusClass qClass,
                          Value qThis,
                          Value[] args)
  {
    if (isStatic())
      qThis = qClass;
    
    Value oldThis = env.setThis(qThis);
    QuercusClass oldClass = env.setCallingClass(qClass);
    
    try {
      return callImpl(env, args, false, null, null);
    } finally {
      env.setThis(oldThis);
      env.setCallingClass(oldClass);
    }
  }

  @Override
  public Value callMethodRef(Env env,
                             QuercusClass qClass,
                             Value qThis,
                             Value[] args)
  {
    Value oldThis = env.setThis(qThis);
    QuercusClass oldClass = env.setCallingClass(qClass);
    
    try {
      return callImpl(env, args, true, null, null);
    } finally {
      env.setThis(oldThis);
      env.setCallingClass(oldClass);
    }
  }


  private boolean isVariableArgs()
  {
    return _info.isVariableArgs() || _args.length > 5;
  }

  private boolean isVariableMap()
  {
    // return _info.isVariableVar();
    // php/3254
    return _info.isUsesSymbolTable() || _info.isVariableVar();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

