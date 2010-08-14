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

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusLanguageException;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a compiled PHP program.
 */
abstract public class QuercusPage
{
  private static final L10N L = new L10N(QuercusPage.class);
  
  private HashMap<String,AbstractFunction> _funMap
    = new HashMap<String,AbstractFunction>();
  
  private HashMap<String,AbstractFunction> _funMapLowerCase
    = new HashMap<String,AbstractFunction>();
  
  private HashMap<String,ClassDef> _classMap
    = new HashMap<String,ClassDef>();

  private QuercusPage _profilePage;

  /**
   * Returns true if the page is modified.
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Returns the compiling page, if any
   */
  public QuercusPage getCompiledPage()
  {
    return null;
  }

  /**
   * Returns the user name for profiling, if any
   */
  public String getUserPath()
  {
    return null;
  }

  /**
   * Returns the profiling page, if any
   */
  public QuercusPage getProfilePage()
  {
    return _profilePage;
  }

  /**
   * Sets the profiling page, if any
   */
  public void setProfilePage(QuercusPage profilePage)
  {
    _profilePage = profilePage;
  }

  /**
   * Returns the page's path.
   */
  abstract public Path getSelfPath(Env env);
  
  /**
   * Finds a function.
   */
  public AbstractFunction findFunction(String name)
  {
    AbstractFunction fun = _funMap.get(name);

    if (fun != null)
      return fun;

    fun = _funMapLowerCase.get(name.toLowerCase());

    return fun;
  }

  /**
   * Finds a function.
   */
  public ClassDef findClass(String name)
  {
    return _classMap.get(name);
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,ClassDef> getClassMap()
  {
    return _classMap;
  }

  /**
   * Execute the program as top-level, i.e. not included.
   *
   * @param env the calling environment
   */
  public Value executeTop(Env env)
  {
    QuercusPage compile = getCompiledPage();

    Path oldPwd = env.getPwd();

    Path pwd = getPwd(env);

    env.setPwd(pwd);
    try {
      if (compile != null)
        return compile.executeTop(env);
      
      return execute(env);
    } catch (QuercusLanguageException e) {
      if (env.getExceptionHandler() != null) {
        try {
          env.getExceptionHandler().call(env, e.getValue());
        }
        catch (QuercusLanguageException e2) {
          uncaughtExceptionError(env, e2);
        }
      }
      else {
        uncaughtExceptionError(env, e);
      }
      
      return NullValue.NULL;
    } finally {
      env.setPwd(oldPwd);
    }
  }

  /*
   * Throws an error for this uncaught exception.
   */
  private void uncaughtExceptionError(Env env, QuercusLanguageException e)
  {
    Location location = e.getLocation(env);
    String type = e.getValue().getClassName();
    String message = e.getMessage(env);
    
    env.error(location,
              L.l(
                "Uncaught exception of type '{0}' with message '{1}'",
                type,
                message));
  }
  
  /**
   * Returns the pwd according to the source page.
   */
  public Path getPwd(Env env)
  {
    return getSelfPath(env).getParent();
  }


  /**
   * Execute the program
   *
   * @param env the calling environment
   */
  abstract public Value execute(Env env);

  /**
   * Initialize the program
   *
   * @param quercus the owning engine
   */
  public void init(QuercusContext quercus)
  {
  }

  /**
   * Initialize the environment
   *
   * @param quercus the owning engine
   */
  public void init(Env env)
  {
  }

  /**
   * Imports the page definitions.
   */
  public void importDefinitions(Env env)
  {
    for (Map.Entry<String,AbstractFunction> entry : _funMap.entrySet()) {
      AbstractFunction fun = entry.getValue();

      if (fun.isGlobal())
        env.addFunction(entry.getKey(), entry.getValue());
    }
    
    for (Map.Entry<String,ClassDef> entry : _classMap.entrySet()) {
      env.addClassDef(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Adds a function.
   */
  protected void addFunction(String name, AbstractFunction fun)
  {
    AbstractFunction oldFun = _funMap.put(name, fun);
    
    _funMapLowerCase.put(name.toLowerCase(), fun);
  }

  /**
   * Adds a class.
   */
  protected void addClass(String name, ClassDef cl)
  {
    _classMap.put(name, cl);
  }

  /**
   * Sets a runtime function array after an env.
   */
  public boolean setRuntimeFunction(AbstractFunction []funList)
  {
    return false;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

