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

import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.EnvVar;
import com.caucho.util.IntMap;
import com.caucho.vfs.Path;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a saved copy of the Quercus environment.
 */
public class SaveState {
  private AbstractFunction []_fun;

  private ClassDef []_classDef;
  private QuercusClass []_qClass;

  private Value []_const;
  
  private IntMap _staticNameMap = new IntMap();
  private Value []_staticValues;

  private IntMap _globalNameMap = new IntMap();
  private Value []_globalValues;
  
  private Map<Path,QuercusPage> _includeMap;
  private ImportMap _importMap;

  /**
   * Creates a new save state.
   */
  SaveState(Env env,
            AbstractFunction []fun,
            ClassDef []classDef,
            QuercusClass []qClass,
            Value []constants,
            Map<StringValue,Var> staticMap,
            Map<StringValue,EnvVar> globalMap,
            HashMap<Path,QuercusPage> includeMap,
            ImportMap importMap)
  {
    _fun = new AbstractFunction[fun.length];
    System.arraycopy(fun, 0, _fun, 0, fun.length);
    
    _classDef = new ClassDef[classDef.length];
    System.arraycopy(classDef, 0, _classDef, 0, classDef.length);
    
    _qClass = new QuercusClass[qClass.length];
    System.arraycopy(qClass, 0, _qClass, 0, qClass.length);
    
    _const = new Value[constants.length];
    System.arraycopy(constants, 0, _const, 0, constants.length);
    
    saveStatics(env, staticMap);
    saveGlobals(env, globalMap);
    
    _includeMap = new HashMap<Path,QuercusPage>(includeMap);
    
    if (importMap != null)
      _importMap = importMap.copy();
  }

  /**
   * Returns the function list
   */
  public AbstractFunction []getFunctionList()
  {
    return _fun;
  }

  /**
   * Returns the class def
   */
  public ClassDef []getClassDefList()
  {
    return _classDef;
  }

  /**
   * Returns the quercus class
   */
  public QuercusClass []getQuercusClassList()
  {
    return _qClass;
  }

  /**
   * Returns the constant list
   */
  public Value []getConstantList()
  {
    return _const;
  }
  
  /**
   * Returns the global name map
   */
  public IntMap getStaticNameMap()
  {
    return _staticNameMap;
  }
  
  /**
   * Returns the constant list
   */
  public Value []getStaticList()
  {
    return _staticValues;
  }

  /**
   * Returns the global name map
   */
  public IntMap getGlobalNameMap()
  {
    return _globalNameMap;
  }

  /**
   * Returns the global values
   */
  public Value []getGlobalList()
  {
    return _globalValues;
  }
  
  /**
   * Returns the list of included pages.
   */
  public Map<Path,QuercusPage> getIncludeMap()
  {
    return _includeMap;
  }
  
  /**
   * Returns the import statements.
   */
  public ImportMap getImportMap()
  {
    return _importMap;
  }
  
  public boolean isModified()
  {
    return false;
  }
  
  private void saveStatics(Env env, Map<StringValue,Var> staticMap)
  {
    _staticValues = new Value[staticMap.size()];

    for (Map.Entry<StringValue,Var> entry : staticMap.entrySet()) {
      int id = addStaticName(entry.getKey());

      _staticValues[id] = entry.getValue().toValue().copy(env);
    }
  }

  private void saveGlobals(Env env, Map<StringValue,EnvVar> globalMap)
  {
    _globalValues = new Value[globalMap.size()];

    for (Map.Entry<StringValue,EnvVar> entry : globalMap.entrySet()) {
      if (env.isSpecialVar(entry.getKey()))
        continue;

      EnvVar envVar = entry.getValue();

      int id = addGlobalName(entry.getKey());

      _globalValues[id] = envVar.get().copy(env);
    }
  }
  
  private int addStaticName(StringValue name)
  {
    int id = _staticNameMap.get(name);

    if (id >= 0)
      return id;

    id = _staticNameMap.size();
    _staticNameMap.put(name, id);

    return id;
  }

  private int addGlobalName(StringValue name)
  {
    int id = _globalNameMap.get(name);

    if (id >= 0)
      return id;

    id = _globalNameMap.size();
    _globalNameMap.put(name, id);

    return id;
  }
}
