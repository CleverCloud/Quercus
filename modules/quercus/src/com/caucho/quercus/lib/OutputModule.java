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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamImplOutputStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * PHP output routines.
 */
public class OutputModule extends AbstractQuercusModule 
  implements ModuleStartupListener {
  private static final L10N L = new L10N(OutputModule.class);
  private static final Logger log = Logger.getLogger(
      OutputModule.class.getName());

  private static final StringValue HTTP_ACCEPT_ENCODING
    = new ConstStringValue("HTTP_ACCEPT_ENCODING");

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  // ob_gzhandler related variables/types
  private enum Encoding { NONE, GZIP, DEFLATE };

  private static class GZOutputPair {
    public StringBuilderOutputStream _tempStream;
    public OutputStream _outputStream;
  }

  private static HashMap<Env,GZOutputPair> _gzOutputPairs 
    = new HashMap<Env,GZOutputPair>();

  public static final int PHP_OUTPUT_HANDLER_START = 1;
  public static final int PHP_OUTPUT_HANDLER_CONT = 2;
  public static final int PHP_OUTPUT_HANDLER_END = 4;
  
  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  public void startup(Env env)
  {
    boolean isOutputBuffering = INI_OUTPUT_BUFFERING.getAsBoolean(env);
    String handlerName = INI_OUTPUT_HANDLER.getAsString(env);

    if (handlerName != null
        && ! "".equals(handlerName)
        && env.getFunction(handlerName) != null) {
      Callable callback = env.createString(handlerName).toCallable(env);

      ob_start(env, callback, 0, true);
    } else if (isOutputBuffering) {
      ob_start(env, null, 0, true);
    }

    ob_implicit_flush(env, isOutputBuffering);
  }

  /**
   * Flushes the original output buffer.
   */
  public Value flush(Env env)
  {
    try {
      // XXX: conflicts with dragonflycms install
      env.getOriginalOut().flush();
    } catch (IOException e) {
    }

    return NullValue.NULL;
  }

  /**
   * Clears the output buffer.
   */
  public static Value ob_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.clean();

      return BooleanValue.TRUE;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pops the output buffer, discarding the contents.
   */
  public static boolean ob_end_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.clean();

      Callable callback = ob.getCallback();

      if (callback != null) {
        ob.setCallback(null);
      }
    }

    return env.popOutputBuffer();
  }

  /**
   * Pops the output buffer.
   */
  public static boolean ob_end_flush(Env env)
  {
    return env.popOutputBuffer();
  }

  /**
   * Returns the contents of the output buffer, emptying it afterwards.
   */
  public static Value ob_get_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      Value result = ob.getContents();

      ob.clean();

      return result;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the contents of the current output buffer.
   */
  public static Value ob_get_contents(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return ob.getContents();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pops the output buffer and returns the contents.
   */
  public static Value ob_get_flush(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    Value result = BooleanValue.FALSE;
    if (ob != null) {
      result = ob.getContents();
    }

    env.popOutputBuffer();

    return result;
  }

  /**
   * Flushes this output buffer into the next one on the stack or
   * to the default "output buffer" if no next output buffer exists.
   * The callback associated with this buffer is also called with
   * appropriate parameters.
   */
  public static Value ob_flush(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.flush();

      return BooleanValue.TRUE;
    } 
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pushes the output buffer
   */
  public static Value ob_get_length(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return LongValue.create(ob.getLength());
    else
      return BooleanValue.FALSE;
  }

  /**
   * Gets the nesting level of the current output buffer
   */
  public static Value ob_get_level(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return LongValue.create(ob.getLevel());
    else
      return LongValue.ZERO;
  }

  /**
   * Helper recursive function that ensures the handlers are listed
   * in the correct order in the array.
   */
  private static void listHandlers(Env env,
                                   OutputBuffer ob,
                                   ArrayValue handlers)
  {
    if (ob == null)
      return;

    listHandlers(env, ob.getNext(), handlers);

    Callable callback = ob.getCallback();

    if (callback != null) 
      handlers.put(env.createString(callback.getCallbackName()));
    else
      handlers.put(env.createString("default output handler"));
  }
  
  /**
   * Returns a list of all the output handlers in use.
   */
  public static Value ob_list_handlers(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();
    ArrayValue handlers = new ArrayValueImpl();

    listHandlers(env, ob, handlers);

    return handlers;
  }

  /**
   * Inserts the common values for ob_get_status into an array.  Used
   * by getFullStatus() and ob_get_status().
   */
  private static void putCommonStatus(ArrayValue element, OutputBuffer ob,
                                      Env env, boolean fullStatus)
  {
    LongValue type = LongValue.ONE;
    Callable callback = ob.getCallback();

    // XXX: need to replace logic because isInternal appears to be
    // specific to ob_, not general to Callback
    /*
    if (callback != null && callback.isInternal())
      type = LongValue.ZERO;
      */

    String name;

    if (callback != null)
      name = callback.getCallbackName();
    else
      name = "default output handler".intern();
    
    // XXX: there appears to be only one "internal" callback
    if (name.equals("URL-Rewriter"))
      type = LongValue.ZERO;
    
    element.put(env.createString("type"), type);

    // the rewriter is a special case where it includes a field
    // "buffer_size" right in the middle of the common elements, 
    // but only when called with full status.  It appears always 
    // to be 0 and there is no interface to change this buffer_size
    // and no indication of its meaning.
    if (fullStatus && callback != null
        && callback == UrlRewriterCallback.getInstance(env))
      element.put(env.createString("buffer_size"), LongValue.ZERO);

    // Technically, there are supposed to be three possible values
    // for status: 
    //   0 if the stream has never been flushed (PHP_OUTPUT_HANDLER_START)
    //   1 if the stream has been flushed (PHP_OUTPUT_HANDLER_CONT)
    //   2 if the stream was flushed at the end (PHP_OUTPUT_HANDLER_END)
    // However, there is no way to access the buffer after it has ended, 
    // so the final case doesn't seem to be an issue!  (Even calling
    // ob_get_status() in the handler on a ob_end_flush() does not
    // invoke this state.)
    LongValue status = ob.haveFlushed() ? LongValue.ONE : LongValue.ZERO;
    element.put(env.createString("status"), status);

    StringValue nameV = env.createString(name);

    element.put(env.createString("name".intern()), nameV);

    Value del = ob.getEraseFlag() ? BooleanValue.TRUE
        : BooleanValue.FALSE;
    
    element.put(env.createString("del"), del);
  }

  /**
   * Gets the status for all the output buffers on the stack.
   * Recursion ensures the results are ordered correctly in the array.
   */
  private static void getFullStatus(OutputBuffer ob, Env env, ArrayValue result)
  {
    if (ob == null)
      return;

    getFullStatus(ob.getNext(), env, result);

    ArrayValue element = new ArrayValueImpl();

    element.put(env.createString("chunk_size"),
                LongValue.create(ob.getChunkSize()));
    
    // XXX: Not sure why we even need to list a size -- PHP doesn't 
    // even seem to respect it.  -1 => infinity?  
    // (Note: "size" == "capacity")
    element.put(env.createString("size"), LongValue.create(-1));
    element.put(env.createString("block_size"), LongValue.create(-1));

    putCommonStatus(element, ob, env, true);
   
    result.put(element);
  }

  /**
   * Gets the status of the current output buffer(s)
   */ 
  public static Value ob_get_status(Env env, @Optional boolean full_status)
  {
    if (full_status) {
      OutputBuffer ob = env.getOutputBuffer();
      ArrayValue result = new ArrayValueImpl();

      getFullStatus(ob, env, result);

      return result;
    }

    OutputBuffer ob = env.getOutputBuffer();
    ArrayValue result = new ArrayValueImpl();

    if (ob != null) {
      result.put(env.createString("level"),
                 LongValue.create(ob.getLevel()));

      putCommonStatus(result, ob, env, false);
    }

    // returns an empty array when no output buffer exists
    return result;
  }

  /**
   * Makes the original "output buffer" flush on every write.
   */
  public static Value ob_implicit_flush(Env env, @Optional("true") boolean flag)
  {
    if (env.getOriginalOut() != null)
      env.getOriginalOut().setImplicitFlush(flag);

    return NullValue.NULL;
  }

  /**
   * Pushes the output buffer
   */
  public static boolean ob_start(Env env,
                                 @Optional Callable callback,
                                 @Optional int chunkSize,
                                 @Optional("true") boolean erase)
  {
    if (callback != null
        && callback.getCallbackName().equals("ob_gzhandler")) {
      OutputBuffer ob = env.getOutputBuffer();

      for (; ob != null; ob = ob.getNext()) {
        Callable cb = ob.getCallback();

        if (cb.getCallbackName().equals("ob_gzhandler")) {
          env.warning(
              L.l("output handler 'ob_gzhandler' cannot be used twice"));
          return false;
        }
      }
    }
    
    env.pushOutputBuffer(callback, chunkSize, erase);

    return true;
  }

  /**
   * Pushes a new UrlRewriter callback onto the output buffer stack
   * if one does not already exist.
   */
  public static UrlRewriterCallback pushUrlRewriter(Env env)
  {
    UrlRewriterCallback rewriter = UrlRewriterCallback.getInstance(env);

    if (rewriter == null) {
      OutputBuffer ob = env.getOutputBuffer();
      rewriter = new UrlRewriterCallback(env);

      // PHP installs the URL rewriter into the top output buffer if
      // its callback is null
      if (ob != null && ob.getCallback() == null)
        ob.setCallback(rewriter);
      else 
        ob_start(env, rewriter, 0, true);
    }

    return rewriter;
  }

  /**
   * Adds a variable to the list for rewritten URLs.
   */
  public static boolean output_add_rewrite_var(Env env, 
                                               String name, String value)
  {
    UrlRewriterCallback rewriter = pushUrlRewriter(env);
   
    rewriter.addRewriterVar(name, value);

    return true;
  }

  /**
   * Clears the list of variables for rewritten URLs.
   */
  public static boolean output_reset_rewrite_vars(Env env)
  {
    UrlRewriterCallback rewriter = UrlRewriterCallback.getInstance(env); 

    rewriter.resetRewriterVars();

    return true;
  }

  /**
   * Output buffering compatible callback that automatically compresses
   * the output.  The output of this function depends on the value of 
   * state.  Specifically, if the PHP_OUTPUT_HANDLER_START bit is on
   * in the state field, the function supplies a header with the output
   * and initializes a gzip/deflate stream which will be used for 
   * subsequent calls.
   */
  public static Value ob_gzhandler(Env env, StringValue buffer, int state)
  {
    Encoding encoding = Encoding.NONE;
    Value _SERVER = env.getGlobalVar("_SERVER");

    String [] acceptedList
      = _SERVER.get(HTTP_ACCEPT_ENCODING).toString().split(",");

    for (String accepted : acceptedList) {
      accepted = accepted.trim();

      if (accepted.equalsIgnoreCase("gzip")) {
        encoding = Encoding.GZIP;
        break;
      } else if (accepted.equalsIgnoreCase("deflate")) {
        encoding = Encoding.DEFLATE;
        break;
      }
    }

    if (encoding == Encoding.NONE)
      return BooleanValue.FALSE;

    GZOutputPair pair = null;

    StringValue result = env.createBinaryBuilder();
    
    if ((state & (PHP_OUTPUT_HANDLER_START)) != 0) {
      HttpModule.header(
          env, env.createString("Vary: Accept-Encoding"), true, 0);

      int encodingFlag = 0;

      pair = new GZOutputPair();
      pair._tempStream = new StringBuilderOutputStream(result);
      pair._tempStream.setStringBuilder(result);

      try {
        if (encoding == Encoding.GZIP) {
          HttpModule.header(
              env, env.createString("Content-Encoding: gzip"), true, 0);

          pair._outputStream = new GZIPOutputStream(pair._tempStream);
        } else if (encoding == Encoding.DEFLATE) {
          HttpModule.header(
              env, env.createString("Content-Encoding: deflate"), true, 0);

          pair._outputStream = new DeflaterOutputStream(pair._tempStream);
        }
      } catch (IOException e) {
        return BooleanValue.FALSE;
      }

      env.setGzStream(pair);
    } else {
      pair = (GZOutputPair) env.getGzStream();
      
      if (pair == null)
        return BooleanValue.FALSE;
      
      pair._tempStream.setStringBuilder(result);
    }
    
    try {
      buffer.writeTo(pair._outputStream);
      pair._outputStream.flush();

      if ((state & (PHP_OUTPUT_HANDLER_END)) != 0) {
        pair._outputStream.close();
      }
    } catch (IOException e) {
      return BooleanValue.FALSE;
    }

    pair._tempStream.setStringBuilder(null);

    return result;
  }

  static final IniDefinition INI_OUTPUT_BUFFERING
    = _iniDefinitions.add("output_buffering", false, PHP_INI_PERDIR);
  static final IniDefinition INI_OUTPUT_HANDLER
    = _iniDefinitions.add("output_handler", "", PHP_INI_PERDIR);
  static final IniDefinition INI_IMPLICIT_FLUSH
    = _iniDefinitions.add("implicit_flush", false, PHP_INI_ALL);
}
