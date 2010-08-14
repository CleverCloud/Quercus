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

package com.caucho.quercus.script;

import com.caucho.quercus.QuercusContext;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.*;

/**
 * Script engine factory
 */
public class QuercusScriptEngineFactory implements ScriptEngineFactory
{
  private Bindings _globalBindings = new SimpleBindings();
  
  /**
   * Returns the full name of the ScriptEngine.
   */
  public String getEngineName()
  {
    return "Caucho Quercus Script Engine";
  }

  /**
   * Returns the version of the ScriptEngine.
   */
  public String getEngineVersion()
  {
    try {
      //return com.caucho.Version.VERSION;

      Class cl = Class.forName("com.caucho.Version");
      Field version = cl.getField("VERSION");

      return (String) version.get(null);
    } catch (Exception e) {
    }

    return "Resin/3.1.0";
  }

  /**
   * Returns an array of filename extensions normally used by this
   * language.
   */
  public List<String> getExtensions()
  {
    ArrayList<String> ext = new ArrayList<String>();
    ext.add("php");
    return ext;
  }

  /**
   * Returns the mime-types for scripts for the engine.
   */
  public List<String> getMimeTypes()
  {
    return new ArrayList<String>();
  }

  /**
   * Returns the short names for the scripts for the engine,
   * e.g. {"javascript", "rhino"}
   */
  public List<String> getNames()
  {
    ArrayList<String> names = new ArrayList<String>();
    names.add("quercus");
    names.add("php");
    return names;
  }

  /**
   * Returns the name of the supported language.
   */
  public String getLanguageName()
  {
    return "php";
  }

  /**
   * Returns the version of the scripting language.
   */
  public String getLanguageVersion()
  {
    return "5.3.2";
  }

  /**
   * Returns engine-specific properties.
   *
   * Predefined keys include:
   * <ul>
   * <li>THREADING
   * </ul>
   */
  public Object getParameter(String key)
  {
    if ("THREADING".equals(key))
      return "THREAD-ISOLATED";
    else if (ScriptEngine.ENGINE.equals(key))
      return getEngineName();
    else if (ScriptEngine.ENGINE_VERSION.equals(key))
      return getEngineVersion();
    else if (ScriptEngine.NAME.equals(key))
      return getEngineName();
    else if (ScriptEngine.LANGUAGE.equals(key))
      return getLanguageName();
    else if (ScriptEngine.LANGUAGE_VERSION.equals(key))
      return getLanguageVersion();
    else
      return null;
  }

  /**
   * Returns a string which could invoke a method of a Java object.
   */
  public String getMethodCallSyntax(String obj, String m, String []args)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("$");
    sb.append(obj);
    sb.append("->");
    sb.append(m);
    sb.append("(");
    for (int i = 0; i < args.length; i++) {
      if (i != 0)
        sb.append(", ");

      sb.append("$");
      sb.append(args[i]);
    }
    sb.append(");");
    
    return sb.toString();
  }

  /**
   * Returns a string which generates an output statement.
   */
  public String getOutputStatement(String toDisplay)
  {
    return "echo(\'" + toDisplay.replace("\'", "\\\'") + "\');";
  }

  /**
   * Returns a string which generates a valid program.
   */
  public String getProgram(String []statements)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("<?php\n");

    for (int i = 0; i < statements.length; i++) {
      sb.append(statements[i]);
      sb.append(";\n");
    }
    
    sb.append("?>\n");
    
    return sb.toString();
  }
  
  /**
   * Returns a ScriptEngine instance.
   */
  public ScriptEngine getScriptEngine()
  {
    return new QuercusScriptEngine(this, createQuercus());
  }

  /**
   * Creates a new Quercus, which can be overridden for security issues.
   */
  protected QuercusContext createQuercus()
  {
    QuercusContext quercus = new QuercusContext();
    
    quercus.init();
    quercus.start();
    
    return quercus;
  }

  public String toString()
  {
    return "QuercusScriptEngineFactory[]";
  }
}

