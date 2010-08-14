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

package com.caucho.quercus.page;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.Path;

import java.util.HashMap;

/**
 * Represents an interpreted Quercus program.
 */
public class InterpretedPage extends QuercusPage
{
  private final QuercusProgram _program;

  public InterpretedPage(QuercusProgram program)
  {
    _program = program;
  }
  
  /**
   * Returns true if the page is modified.
   */
  @Override
  public boolean isModified()
  {
    return _program.isModified();
  }

  /**
   * Returns any profile page.
   */
  @Override
  public QuercusPage getProfilePage()
  {
    return _program.getProfilePage();
  }

  /**
   * Returns any profile page.
   */
  @Override
  public QuercusPage getCompiledPage()
  {
    return _program.getCompiledPage();
  }
  
  /**
   * Execute the program
   *
   * @param env the calling environment
   */
  public Value execute(Env env)
  {
    Value result = _program.execute(env);
    
    if (result == null)
      result = LongValue.ONE;
    
    return result;
  }

  /**
   * Returns the pwd according to the source page.
   */
  public Path getPwd(Env env)
  {
    return getSelfPath(env).getParent();
  }

  /**
   * Returns the pwd according to the source page.
   */
  public Path getSelfPath(Env env)
  {
    return _program.getSourcePath();
  }
  
  /**
   * Imports the page definitions.
   */
  public void init(Env env)
  {
    _program.init(env);
  }

  /**
   * Imports the page definitions.
   */
  public void importDefinitions(Env env)
  {
    _program.importDefinitions(env);
  }

  /**
   * Finds the function
   */
  public AbstractFunction findFunction(String name)
  {
    return _program.findFunction(name);
  }

  /**
   * Finds the class
   */
  public InterpretedClassDef findClass(String name)
  {
    //return _program.findClass(name);
    return null;
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,ClassDef> getClassMap()
  {
    //return _program.getClassMap();
    return null;
  }

  // runtime function list for compilation
  private AbstractFunction []_runtimeFunList;

  /**
   * Sets a runtime function array after an env.
   */
  @Override
  public boolean setRuntimeFunction(AbstractFunction []funList)
  {
    return _program.setRuntimeFunction(funList);
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof InterpretedPage))
      return false;

    InterpretedPage page = (InterpretedPage) o;

    return _program == page._program;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" +  _program.getSourcePath() + "]";
  }
}

