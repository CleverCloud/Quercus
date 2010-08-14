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

import java.io.Reader;

/**
 * The main scripting engine support.
 *
 * All methods must be implemented.
 */
public interface ScriptEngine {
  public static final String ARGV   = "javax.script.argv";
  public static final String ENGINE = "javax.script.engine";
  public static final String ENGINE_VERSION = "javax.script.engine_version";
  public static final String FILENAME = "javax.script.filename";
  public static final String LANGUAGE = "javax.script.language";
  public static final String LANGUAGE_VERSION = "javax.script.language_version";
  public static final String NAME = "javax.script.name";

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param script path to the script
   * @param context the script context
   */
  public Object eval(String script, ScriptContext context)
    throws ScriptException;

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param reader reader to the script
   * @param context the script context
   */
  public Object eval(Reader reader, ScriptContext context)
    throws ScriptException;

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param string the script
   * @param bindings the single bindings
   */
  public Object eval(String script, Bindings bindings)
    throws ScriptException;

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param reader reader to the script
   * @param bindings the single bindings
   */
  public Object eval(Reader reader, Bindings bindings)
    throws ScriptException;

  /**
   * Evaluates the script with no extra context, returning any value.
   *
   * @param script path to the script
   */
  public Object eval(String script)
    throws ScriptException;

  /**
   * Evaluates the script with no extra context, returning any value.
   *
   * @param reader reader to the script
   */
  public Object eval(Reader reader)
    throws ScriptException;

  /**
   * Sets an engine scope value.
   */
  public void put(String key, Object value);

  /**
   * Gets an engine scope value.
   */
  public Object get(String key);

  /**
   * Creates a new bindings.
   */
  public Bindings createBindings();

  /**
   * Returns a scope of named values.
   */
  public Bindings getBindings(int scope);

  /**
   * Sets a scope of named values.
   */
  public void setBindings(Bindings bindings, int scope);

  /**
   * Returns the default script context.
   */
  public ScriptContext getContext();

  /**
   * Sets the default script context.
   */
  public void setContext(ScriptContext cxt);

  /**
   * Returns the factory which owns the script engine.
   */
  public ScriptEngineFactory getFactory();
}

