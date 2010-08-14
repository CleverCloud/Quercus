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
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.*;

import javax.script.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * Script engine
 */
public class QuercusScriptEngine
  extends AbstractScriptEngine
  implements Compilable
{
  private QuercusScriptEngineFactory _factory;
  private final QuercusContext _quercus;

  QuercusScriptEngine(QuercusScriptEngineFactory factory)
  {
    this(factory, createQuercus());
  }

  public QuercusScriptEngine(QuercusScriptEngineFactory factory,
                             QuercusContext quercus)
  {
    _factory = factory;
    _quercus = quercus;
  }
  
  private static QuercusContext createQuercus()
  {
    QuercusContext quercus = new QuercusContext();
    
    quercus.init();
    quercus.start();
    
    return quercus;
  }

  /**
   * Returns the Quercus object.
   * php/214h
   */
  public QuercusContext getQuercus()
  {
    return _quercus;
  }

  /**
   * evaluates based on a reader.
   */
  public Object eval(Reader script, ScriptContext cxt)
    throws ScriptException
  {
    Env env = null;

    try {
      ReadStream reader = ReaderStream.open(script);
      
      QuercusProgram program = QuercusParser.parse(_quercus, null, reader);

      Writer writer = cxt.getWriter();
      
      WriteStream out;

      if (writer != null) {
        WriterStreamImpl s = new WriterStreamImpl();
        s.setWriter(writer);
        WriteStream os = new WriteStream(s);
        
        os.setNewlineString("\n");

        try {
          os.setEncoding("iso-8859-1");
        } catch (Exception e) {
        }

        out = os;
      }
      else
        out = new NullWriteStream();

      QuercusPage page = new InterpretedPage(program);

      env = new Env(_quercus, page, out, null, null);

      env.setScriptContext(cxt);

      // php/214c
      env.start();
      
      Object result = null;
      
      try {
        Value value = program.execute(env);
        
        if (value != null)
          result = value.toJavaObject();
      }
      catch (QuercusExitException e) {
        //php/2148
      }
      
      out.flushBuffer();
      out.free();

      // flush buffer just in case
      //
      // jrunscript in interactive mode does not automatically flush its
      // buffers after every input, so output to stdout will not be seen
      // until the output buffer is full
      //
      // http://bugs.caucho.com/view.php?id=1914
      writer.flush();
      
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
   * evaluates based on a script.
   */
  public Object eval(String script, ScriptContext cxt)
    throws ScriptException
  {
    return eval(new StringReader(script), cxt);
  }

  /**
   * compiles based on a reader.
   */
  public CompiledScript compile(Reader script)
    throws ScriptException
  {
    try {
      ReadStream reader = ReaderStream.open(script);
      
      QuercusProgram program = QuercusParser.parse(_quercus, null, reader);

      return new QuercusCompiledScript(this, program);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ScriptException(e);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * evaluates based on a script.
   */
  public CompiledScript compile(String script)
    throws ScriptException
  {
    return compile(new StringReader(script));
  }

  /**
   * Returns the engine's factory.
   */
  public QuercusScriptEngineFactory getFactory()
  {
    return _factory;
  }

  /**
   * Creates a bindings.
   */
  public Bindings createBindings()
  {
    return new SimpleBindings();
  }

  public String toString()
  {
    return "QuercusScriptEngine[]";
  }
}

