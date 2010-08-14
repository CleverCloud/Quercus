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
public interface Callable {
  /**
   * Returns the callable name, needed as special case for ob_*
   */
  public String getCallbackName();
  
  /**
   * Checks for a valid callback.
   */
  public boolean isValid(Env env);
  
  /**
   * Evaluates the callback with no arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env);

  /**
   * Evaluates the callback with 1 arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value a1);

  /**
   * Evaluates the callback with 2 arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value a1, Value a2);

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value a1, Value a2, Value a3);

  /**
   * Evaluates the callback with 4 arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value a1, Value a2, Value a3,
                             Value a4);

  /**
   * Evaluates the callback with 5 arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value a1, Value a2, Value a3,
                             Value a4, Value a5);

  /**
   * Evaluates the callback with variable arguments.
   *
   * @param env the calling environment
   */
  abstract public Value call(Env env, Value []args);
  
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
  abstract public Value callArray(Env env,
                                  ArrayValue array,
                                  Value key,
                                  Value a1);

  /**
   * Evaluates a callback where the first argument is from an array.
   * The callback may be modifying that array element.
   * For ArrayModule.
   * 
   * @param env
   * @param array from which a1 came from
   * @param key index of a1 in the array
   * @param a1 need to make a reference to this variable
   * @param a2 additional argument to pass to the callback
   */
  abstract public Value callArray(Env env,
                                  ArrayValue array,
                                  Value key,
                                  Value a1,
                                  Value a2);
  
  /**
   * Evaluates a callback where the first argument is from an array.
   * The callback may be modifying that array element.
   * For ArrayModule.
   * 
   * @param env
   * @param array from which a1 came from
   * @param key index of a1 in the array
   * @param a1 need to make a reference to this variable
   * @param a2 additional argument to pass to the callback
   * @param a3 additional argument to pass to the callback
   */
  abstract public Value callArray(Env env,
                                  ArrayValue array,
                                  Value key,
                                  Value a1,
                                  Value a2,
                                  Value a3);
}

