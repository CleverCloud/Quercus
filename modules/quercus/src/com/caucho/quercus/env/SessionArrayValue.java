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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.lib.UnserializeReader;
import com.caucho.util.CacheListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Represents the $_SESSION
 */
public class SessionArrayValue extends ArrayValueWrapper
  implements CacheListener, Serializable
{
  static protected final Logger log
    = Logger.getLogger(SessionArrayValue.class.getName());

  private String _id;

  private AtomicInteger _useCount = new AtomicInteger();

  protected long _accessTime;
  private long _maxInactiveInterval;

  private boolean _isValid;

  public SessionArrayValue(String id, long now, 
                           long maxInactiveInterval)
  {
    this(id, now, maxInactiveInterval, new ArrayValueImpl());
  }
  
  public SessionArrayValue(String id, long now,
                           long maxInactiveInterval, ArrayValue array)
  {
    super(array);

    _id = id;
    _accessTime = now;
    _maxInactiveInterval = maxInactiveInterval;
  }

  /**
   * Returns the session id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Changes the session id.  Used by session_regenerate_id() where we want
   * to change the session id, but keep the rest of the session information.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    long accessTime = _accessTime;

    SessionArrayValue copy = 
      new SessionArrayValue(_id, accessTime, _maxInactiveInterval,
                            (ArrayValue) getArray().copy(env, map));

    return copy;
  }

  /**
   * Encoding for serialization.
   */
  public String encode(Env env)
  {
    StringBuilder sb = new StringBuilder();
    ArrayValue array = getArray();

    SerializeMap serializeMap = new SerializeMap();
    
    synchronized (array) {
      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        sb.append(entry.getKey().toString());
        sb.append("|");

        entry.getValue().serialize(env, sb, serializeMap);
      }
    }

    return sb.toString();
  }

  /**
   * Decodes encoded values, adding them to this object.
   */
  public boolean decode(Env env, StringValue encoded)
  {
    ArrayValue array = getArray();

    try {
      UnserializeReader is = new UnserializeReader(encoded);

      synchronized (array) {
        while (true) {
          int ch;

          StringValue sb = env.createUnicodeBuilder();

          while ((ch = is.read()) > 0 && ch != '|') {
            sb.append((char) ch);
          }

          if (sb.length() == 0)
            return true;

          array.put(sb, is.unserialize(env));
        }
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  public boolean inUse()
  {
    return _useCount.get() > 0;
  }

  public void addUse()
  {
    _useCount.incrementAndGet();
  }

  public boolean load()
  {
    return true;
  }

  /**
   * Saves the object to the output stream.
   */
  public void store(Env env, OutputStream out)
    throws IOException
  {
    String encode = encode(env);

    int len = encode.length();

    out.write(len >> 24);
    out.write(len >> 16);
    out.write(len >> 8);
    out.write(len);

    for (int i = 0; i < len; i++) {
      char ch = encode.charAt(i);

      out.write(ch >> 8);
      out.write(ch);
    }
  }

  public void load(Env env, InputStream in)
    throws IOException
  {
    int len = (((in.read() & 0xff) << 24)
               + ((in.read() & 0xff) << 16)
               + ((in.read() & 0xff) << 8)
               + ((in.read() & 0xff)));

    StringValue sb = env.createUnicodeBuilder();

    for (int i = 0; i < len; i++) {
      char ch = (char) (((in.read() & 0xff) << 8) + (in.read() & 0xff));

      sb.append(ch);
    }

    decode(env, sb);
  }

  /**
   * Cleaning up session stuff at the end of a request.
   *
   * <p>If the session data has changed and we have persistent sessions,
   * save the session.  However, if save-on-shutdown is true, only save
   * on a server shutdown.
   */
  public void finish()
  {
    int count;

    count = _useCount.decrementAndGet();

    if (count > 0)
      return;

    if (count < 0)
      throw new IllegalStateException();

    store();
  }

  /**
   * Store on shutdown.
   */
  public void storeOnShutdown()
  {
    store();
  }

  protected void store()
  {
  }

  public long getMaxInactiveInterval()
  {
    return _maxInactiveInterval;
  }

  /*
  public void setClusterObject(ClusterObject clusterObject)
  {
    _clusterObject = clusterObject;
  }
  */

  public void reset(long now)
  {
    setValid(true);
    setAccess(now);
    clear();
  }

  public long getAccessTime()
  {
    return _accessTime;
  }
  
  public void setAccess(long now)
  {
    _accessTime = now;
  }

  public boolean isValid()
  {
    return _isValid;
  }

  public void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  /**
   * Invalidates the session.
   */
  public void invalidate()
  {
    if (! _isValid)
      throw new IllegalStateException(L.l(
        "Can't call invalidate() when session is no longer valid."));

    try {
      remove();

      clear();
    } finally {
      _isValid = false;
    }
  }

  protected void remove()
  {
  }
  
  public boolean isEmpty()
  {
    return getSize() == 0;
  }

  /**
   * Callback when the session is removed from the session cache, generally
   * because the session cache is full.
   */
  public void removeEvent()
  {
    // XXX: logic doesn't make sense
    
    /*
    boolean isValid = _isValid;

    if (log.isLoggable(Level.FINE)) {
      log.fine("remove session " + _id);
    }

    long now = Alarm.getCurrentTime();

    ClusterObject clusterObject = _clusterObject;

    if (_isValid && clusterObject != null) {
      try {
        clusterObject.update();
        clusterObject.store(this);
      } catch (Throwable e) {
        log.log(Level.WARNING, "Can't serialize session", e);
      }
    }
    */
  }
  
  //
  // Java serialization code
  //
  
  private Object writeReplace()
  {
    return new ArrayValueImpl(this);
  }
}
