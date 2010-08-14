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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.*;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Writer;

/**
 * Script engine
 */
public class QuercusCompiledScript extends CompiledScript {
  private final QuercusScriptEngine _engine;
  private final QuercusProgram _program;

  QuercusCompiledScript(QuercusScriptEngine engine, QuercusProgram program)
  {
    _engine = engine;
    _program = program;
  }

  /**
   * evaluates based on a reader.
   */
  public Object eval(ScriptContext cxt)
    throws ScriptException
  {
    Env env = null;

    try {
      Writer writer = cxt.getWriter();

      WriteStream out;

      if (writer != null) {
        ReaderWriterStream s = new ReaderWriterStream(null, writer);
        WriteStream os = new WriteStream(s);

        os.setNewlineString("\n");
    
        try {
          os.setEncoding("utf-8");
        } catch (Exception e) {
        }

        out = os;
      }
      else
        out = new NullWriteStream();

      QuercusPage page = new InterpretedPage(_program);

      env = new Env(_engine.getQuercus(), page, out, null, null);

      env.setScriptContext(cxt);

      // php/214g
      env.start();

      Value resultV = _program.execute(env);
      
      Object result = null;
      if (resultV != null)
        result = resultV.toJavaObject();

      out.flushBuffer();
      out.free();

      return result;
      /*
    } catch (ScriptException e) {
      throw e;
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ScriptException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      if (env != null)
        env.close();
    }
  }

  /**
   * Returns the script engine.
   */
  public ScriptEngine getEngine()
  {
    return _engine;
  }

  public String toString()
  {
    return "QuercusCompiledScript[]";
  }
}

