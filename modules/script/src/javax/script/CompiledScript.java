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
 * Represents a compiled script.
 */
abstract public class CompiledScript {
  /**
   * Evaluates the script.
   */
  public Object eval()
    throws ScriptException
  {
    return eval(getEngine().getContext());
  }

  /**
   * Evaluates the script with the given bindings.
   */
  public Object eval(Bindings bindings)
    throws ScriptException
  {
    ScriptContext engineCxt = getEngine().getContext();
    ScriptContext cxt = new SimpleScriptContext();

    cxt.setReader(engineCxt.getReader());
    cxt.setWriter(engineCxt.getWriter());
    cxt.setErrorWriter(engineCxt.getErrorWriter());
    cxt.setBindings(engineCxt.getBindings(ScriptContext.GLOBAL_SCOPE),
                    ScriptContext.GLOBAL_SCOPE);
    cxt.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

    return eval(cxt);
  }

  /**
   * Evaluates the script with the given context.
   */
  abstract public Object eval(ScriptContext context)
    throws ScriptException;

  /**
   * Returns the owning engine.
   */
  abstract public ScriptEngine getEngine();
}

