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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.annotation.Name;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Quercus functions to get information about the Quercus environment.
 */
public class QuercusModule extends AbstractQuercusModule
{
  /**
   * Returns the Quercus object.
   */
  @Name("quercus_get_quercus")
  public static QuercusContext get_quercus(Env env)
  {
    return env.getQuercus();
  }
  
  /**
   * Returns the Env object.
   */
  @Name("quercus_get_env")
  public static Env get_env(Env env)
  {
    return env;
  }
  
  /**
   * Returns the version of Quercus.
   */
  @Name("quercus_version")
  public static String version(Env env)
  {
    return env.getQuercus().getVersion();
  }
  
  /**
   * Returns true if this is the Professional version.
   */
  @Name("quercus_is_pro")
  public static boolean is_pro(Env env)
  {
    return env.getQuercus().isPro();
  }
  
  /**
   * Returns true if pages will be compiled.
   */
  @Name("quercus_is_compile")
  public static boolean is_compile(Env env)
  {
    return env.getQuercus().isCompile();
  }
  
  /**
   * Returns true if Quercus is running under Resin.
   */
  @Name("quercus_is_resin")
  public static boolean is_resin(Env env)
  {
    return env.getQuercus().isResin();
  }
  
  /**
   * Returns true if a JDBC database has been explicitly set.
   */
  @Name("quercus_has_database")
  public static boolean has_database(Env env)
  {
    return env.getQuercus().getDatabase() != null;
  }
  
  /**
   * Returns true if there is an HttpRequest associated with this Env.
   */
  @Name("quercus_has_request")
  public static boolean has_request(Env env)
  {
    return env.getRequest() != null;
  }
  
  /**
   * Returns the HttpServletRequest associated with this Env.
   */
  @Deprecated
  @Name("quercus_get_request")
  public static HttpServletRequest get_request(Env env)
  {
    return env.getRequest();
  }
  
  /**
   * Returns the HttpServletRequest associated with this Env.
   */
  @Name("quercus_servlet_request")
  public static HttpServletRequest get_servlet_request(Env env)
  {
    return env.getRequest();
  }
  
  /**
   * Returns the HttpServletResponse associated with this Env.
   */
  @Deprecated
  @Name("quercus_get_response")
  public static HttpServletResponse get_response(Env env)
  {
    return env.getResponse();
  }
  
  /**
   * Returns the HttpServletResponse associated with this Env.
   */
  @Name("quercus_servlet_response")
  public static HttpServletResponse get_servlet_response(Env env)
  {
    return env.getResponse();
  }
  
  /**
   * Returns the ServletContext.
   */
  @Name("quercus_get_servlet_context")
  public static ServletContext get_servlet_context(Env env)
  {
    return env.getServletContext();
  }
  
  /**
   * Special quercus-only import statements.
   */
  @Name("quercus_import")
  public static void q_import(Env env, String name)
  {
    if (name.endsWith("*"))
      env.addWildcardImport(name);
    else
      env.putQualifiedImport(name);
  }
}
