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

package com.caucho.quercus.env;

import com.caucho.quercus.lib.OutputModule;
import com.caucho.vfs.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Represents a PHP output buffer
 */
public class OutputBuffer {
  private static final Logger log
    = Logger.getLogger(OutputBuffer.class.getName());

  private int _state;
  private boolean _haveFlushed;
  private Callable _callback;
  
  private final boolean _erase;
  private final int _chunkSize;
  private final int _level;

  private final OutputBuffer _next;

  private TempStream _tempStream;
  private WriteStream _out;

  private final Env _env;

  OutputBuffer(OutputBuffer next, Env env, Callable callback, 
               int chunkSize, boolean erase)
  {
    _next = next;

    if (_next != null)
      _level = _next._level + 1;
    else
      _level = 1;

    _erase = erase;
    _chunkSize = chunkSize;

    _env = env;
    _callback = callback;

    _tempStream = new TempStream();
    _out = new WriteStream(_tempStream);
    
    _out.setNewlineString("\n");

    String encoding = env.getOutputEncoding();

    if (encoding != null) {
      try {
        _out.setEncoding(encoding);
      }
      catch (UnsupportedEncodingException e) {
        if (log.isLoggable(Level.WARNING))
          log.log(Level.WARNING, e.toString(), e);
        try {
          _out.setEncoding("UTF-8");
        }
        catch (UnsupportedEncodingException e2) {
          if (log.isLoggable(Level.WARNING))
            log.log(Level.WARNING, e.toString(), e2);
        }
      }
    }

    _state = OutputModule.PHP_OUTPUT_HANDLER_START;
    _haveFlushed = false;
  }

  /**
   * Returns the next output buffer;
   */
  public OutputBuffer getNext()
  {
    return _next;
  }

  /**
   * Returns the writer.
   */
  public WriteStream getOut()
  {
    return _out;
  }

  /**
   * Returns the buffer contents.
   */
  public Value getContents()
  {
    try {
      _out.flush();

      StringValue bb = _env.createBinaryBuilder(_tempStream.getLength());

      for (TempBuffer ptr = _tempStream.getHead();
           ptr != null;
           ptr = ptr.getNext()) {
        bb.append(ptr.getBuffer(), 0, ptr.getLength());
      }

      return bb;
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the buffer length.
   */
  public long getLength()
  {
    try {
      _out.flush();

      return (long)_tempStream.getLength();
    } catch (IOException e) {
      _env.error(e.toString(), e);

      return -1L;
    }
  }

  /**
   * Returns the nesting level.
   */
  public int getLevel()
  {
    return _level;
  }

  /**
   * Returns true if this buffer has ever been flushed.
   */
  public boolean haveFlushed()
  {
    return _haveFlushed;
  }

  /**
   * Returns the erase flag.
   */
  public boolean getEraseFlag()
  {
    // XXX: Why would anyone need this?  If the erase flag is false,
    // that supposedly means that the buffer will not be destroyed 
    // until the script finishes, but you can't access the buffer 
    // after it has been popped anyway, so who cares if you delete 
    // it or not?  It is also confusingly named.  More research may 
    // be necessary...
    return _erase;
  }

  /**
   * Returns the chunk size.
   */
  public int getChunkSize()
  {
    return _chunkSize;
  }

  /**
   * Cleans (clears) the buffer.
   */
  public void clean()
  {
    try {
      _state |= OutputModule.PHP_OUTPUT_HANDLER_CONT;
      
      _out.flush();

      _tempStream.clearWrite();
      
      _state &= ~(OutputModule.PHP_OUTPUT_HANDLER_START);
      _state &= ~(OutputModule.PHP_OUTPUT_HANDLER_CONT);
      
    } catch (IOException e) {
      _env.error(e.toString(), e);
    }
  }

  /**
   * Flushs the data in the stream, calling the callback with appropriate
   * flags if necessary.
   */
  public void flush()
  {
    _state |= OutputModule.PHP_OUTPUT_HANDLER_CONT;

    if (! callCallback()) {
      // clear the start and cont flags
      doFlush();
    }
    
    _state &= ~(OutputModule.PHP_OUTPUT_HANDLER_START);
    _state &= ~(OutputModule.PHP_OUTPUT_HANDLER_CONT);
    _haveFlushed = true;
  }

  /**
   * Closes the output buffer.
   */
  public void close()
  {
    _state |= OutputModule.PHP_OUTPUT_HANDLER_END;

    if (! callCallback()) {
      // all data that has and ever will be written has now been processed
      _state = 0; 

      doFlush();
    }

    WriteStream out = _out;
    _out = null;

    TempStream tempStream = _tempStream;
    _tempStream = null;

    try {
      if (out != null)
        out.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (tempStream != null)
      tempStream.destroy();
  }

  /**
   * Invokes the callback using the data in the current buffer.
   */
  private boolean callCallback()
  {
    if (_callback == null || ! _callback.isValid(_env))
      return false;

    Value result = 
      _callback.call(_env, getContents(), LongValue.create(_state));

    // special code to do nothing to the buffer
    if (result.toValue() != BooleanValue.FALSE) {
      // php/1l11, php/1l13
      clean();

      result.print(_env, getNextOut());

      return true;
    }
    else
      return false;
  }

  /**
   * Flushes the data without calling the callback.
   */
  private void doFlush()
  {
    try {
      _out.flush();

      WriteStream out = getNextOut();

      _tempStream.writeToStream(out);

      _tempStream.clearWrite();
    } catch (IOException e) {
      _env.error(e.toString(), e);
    }
  }

  private WriteStream getNextOut()
  {
    if (_next != null)
      return _next.getOut();
    else
      return _env.getOriginalOut();
  }

  /**
   * Returns the callback for this output buffer.
   */
  public Callable getCallback()
  {
    return _callback;
  }

  /**
   * Sets the callback for this output buffer.
   */
  public void setCallback(Callback callback)
  {
    _callback = callback;
  }
}

