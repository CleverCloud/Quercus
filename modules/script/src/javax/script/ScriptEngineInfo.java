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

package javax.script;

/**
 * Metadata about the script engine.
 */
public interface ScriptEngineInfo {
  /**
   * Returns the full name of the ScriptEngine.
   */
  public String getEngineName();

  /**
   * Returns the version of the ScriptEngine, e.g.
   * Rhino Mozilla Javascript Engine.
   */
  public String getEngineVersion();

  /**
   * Returns an array of filename extensions normally used by this
   * language.
   */
  public String []getExtensions();

  /**
   * Returns the mime-types for scripts for the engine.
   */
  public String []getMimeTypes();

  /**
   * Returns the short names for the scripts for the engine,
   * e.g. {"javascript", "rhino"}
   */
  public String []getNames();

  /**
   * Returns the name of the supported language.
   */
  public String getLanguageName();

  /**
   * Returns the version of the scripting language.
   */
  public String getLanguageVersion();

  /**
   * Returns engine-specific properties.
   *
   * Predefined keys include:
   * <ul>
   * <li>ENGINE
   * <li>ENGINE_VERSION
   * <li>NAME
   * <li>LANGUAGE
   * <li>LANGUAGE_VERSION
   * <li>THREADING
   * </ul>
   */
  public Object getParameter(String key);

  /**
   * Returns a string which could invoke a method of a Java object.
   */
  public String getMethodCallSyntax(String obj, String m, String []args);

  /**
   * Returns a string which generates an output statement.
   */
  public String getOutputStatement(String toDisplay);

  /**
   * Returns a string which generates a valid program.
   */
  public String getProgram(String []statements);
}

