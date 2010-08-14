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

package com.caucho.quercus.env;

/**
 * Represents a call to a function.
 */
abstract public class Callback extends Value implements Callable {
  
  @Override
  public Callable toCallable(Env env)
  {
    return this;
  }
  
  /**
   * Evaluates a callback where the first argument is from an array.
   * The callback may be modifying that array element.
   * For ArrayModule.
   * 
   * @param env
   * @param array from which a1 came from
   * @param key index of a1 in the array
   * @param a1 need to make a reference to this variable
   */
  @Override
  final public Value callArray(Env env,
                               ArrayValue array,
                               Value key,
                               Value a1)
  {
    // php/1740
    
    Value result;  

    if (a1 instanceof Var) {
      a1 = new ArgRef((Var) a1);

      result = call(env, a1);
    }
    else {
      Value aVar = new Var(a1);

      result = call(env, aVar);

      Value aNew = aVar.toValue();
      
      if (aNew != a1)
        array.put(key, aNew);
    }

    return result;
  }
  
  /**
   * Evaluates a callback where the first argument is from an array.
   * The callback may be modifying that array element.
   * For ArrayModule.
   * 
   * @param env
   * @param array from which a1 came from
   * @param key index of a1 in the array
   * @param a1 need to make a reference to this variable
   */
  @Override
  final public Value callArray(Env env,
                               ArrayValue array,
                               Value key,
                               Value a1,
                               Value a2)
  {
    // php/1740
    
    Value result;  

    if (a1 instanceof Var) {
      a1 = new ArgRef((Var) a1);

      result = call(env, a1, a2);
    }
    else {
      Value aVar = new Var(a1);

      result = call(env, aVar, a2);

      Value aNew = aVar.toValue();
      
      if (aNew != a1)
        array.put(key, aNew);
    }

    return result;
  }
  
  /**
   * Evaluates a callback where the first argument is from an array.
   * The callback may be modifying that array element.
   * For ArrayModule.
   * 
   * @param env
   * @param array from which a1 came from
   * @param key index of a1 in the array
   * @param a1 need to make a reference to this variable
   */
  @Override
  final public Value callArray(Env env,
                               ArrayValue array,
                               Value key,
                               Value a1,
                               Value a2,
                               Value a3)
  {
    // php/1740
    
    Value result;  

    if (a1 instanceof Var) {
      a1 = new ArgRef((Var) a1);

      result = call(env, a1, a2, a3);
    }
    else {
      Value aVar = new Var(a1);

      result = call(env, aVar, a2, a3);

      Value aNew = aVar.toValue();
      
      if (aNew != a1)
        array.put(key, aNew);
    }

    return result;
  }

  /**
   * Evaluates the callback with variable arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value []args);

  /**
   * 
   * @return true if this is an invalid callback reference
   */
  @Override
  abstract public boolean isValid(Env env);

  /**
   * Returns the name of the callback.
   * 
   */
  abstract public String getCallbackName();

  /**
   * Returns true if this callback is implemented internally (i.e. in Java).
   *
   */
  abstract public boolean isInternal(Env env);
  
  public String toString()
  {
    return "Callback" + getCallbackName() + "]";
  }
}

