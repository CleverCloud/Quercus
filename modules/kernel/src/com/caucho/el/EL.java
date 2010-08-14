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

package com.caucho.el;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public class EL {
  private static final Logger log = Logger.getLogger(EL.class.getName());
  private static final L10N L = new L10N(EL.class);
  
  private static EnvironmentLocal<ELContext> _elEnvironment
    = new EnvironmentLocal<ELContext>();
  
  private static EnvironmentLocal<HashMap<String,Object>> _envVar
    = new EnvironmentLocal<HashMap<String,Object>>();

  public final static Object NULL = new Object();

  public static ELContext getEnvironment()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return getEnvironment(loader);
  }

  public static ELContext getEnvironment(ClassLoader loader)
  {
    ELContext context = _elEnvironment.getLevel(loader);

    if (context == null) {
      context = new EnvironmentContext(loader);
      
      _elEnvironment.set(context, loader);
    }

    return context;
  }
  
  public static void setEnvironment(ELContext env)
  {
    _elEnvironment.set(env);
  }
  
  public static void setEnvironment(ELContext env,
                                    ClassLoader loader)
  {
    _elEnvironment.set(env, loader);
  }

  /**
   * Sets the environment map.
   */
  public static void setVariableMap(HashMap<String,Object> map,
                                    ClassLoader loader)
  {
    _envVar.set(map, loader);
  }

  /**
   * Gets a var from the specified level.
   */
  public static Object getLevelVar(String name, ClassLoader loader)
  {
    HashMap<String,Object> varMap = _envVar.getLevel(loader);

    if (varMap == null)
      return null;
    else
      return varMap.get(name);
  }

  /**
   * Puts an environment value.
   */
  public static Object putVar(String name, Object value)
  {
    return putVar(name, value, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Puts an environment value.
   */
  public static Object putVar(String name, Object value, ClassLoader loader)
  {
    HashMap<String,Object> varMap = _envVar.getLevel(loader);

    if (varMap == null) {
      varMap = new HashMap<String,Object>();
      _envVar.set(varMap, loader);
    }

    return varMap.put(name, value);
  }

  public static Object evalObject(String value)
    throws ELParseException, ELException
  {
    ELParser parser = new ELParser(getEnvironment(), value);

    Expr expr = parser.parse();

    return expr.getValue(getEnvironment());
  }

  public static String evalString(String value, ELContext env)
    throws ELParseException, ELException
  {
    ELParser parser = new ELParser(getEnvironment(), value);

    Expr expr = parser.parse();

    return expr.evalString(env);
  }
  
  public static boolean evalBoolean(String value, ELContext env)
    throws ELParseException, ELException
  {
    ELParser parser = new ELParser(getEnvironment(), value);

    Expr expr = parser.parse();

    return expr.evalBoolean(env);
  }
}
