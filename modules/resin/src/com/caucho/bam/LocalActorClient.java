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

package com.caucho.bam;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * LocalActorClient is a convenience class for the
 */
public class LocalActorClient implements ActorClient {
  private static final Logger log
    = Logger.getLogger(LocalActorClient.class.getName());

  private static final WeakHashMap<ClassLoader,SoftReference<ActorClientFactory>>
    _factoryMap = new WeakHashMap<ClassLoader,SoftReference<ActorClientFactory>>();

  private ActorClient _client;

  public LocalActorClient()
  {
    this(null, null);
  }

  public LocalActorClient(String uid)
  {
    this(uid, null);
  }

  public LocalActorClient(String uid, String password)
  {
    _client = getFactory().createClient(uid, getClass().getSimpleName());
  }

  /**
   * Returns the jid
   */
  public String getJid()
  {
    return _client.getJid();
  }

  /**
   * Sets the message handler
   */
  public void setClientStream(ActorStream stream)
  {
    _client.setClientStream(stream);
  }

  /**
   * Gets the message listener
   */
  public ActorStream getClientStream()
  {
    return _client.getClientStream();
  }
  
  /**
   * The stream to the ActorClient.
   */
  public ActorStream getActorStream()
  {
    return _client.getActorStream();
  }

  /**
   * The stream to the link.
   */
  public ActorStream getLinkStream()
  {
    return _client.getLinkStream();
  }

  /**
   * The stream to the link.
   */
  public void setLinkStream(ActorStream linkStream)
  {
    _client.setLinkStream(linkStream);
  }

  public void message(String to,
                      Serializable payload)
  {
    _client.message(to, payload);
  }

  //
  // RPC
  //

  public Serializable queryGet(String to,
                               Serializable payload)
  {
    return _client.queryGet(to, payload);
  }

  public Serializable queryGet(String to,
                               Serializable payload,
                               long timeout)
  {
    return _client.queryGet(to, payload, timeout);
  }

  public void queryGet(String to,
                       Serializable payload,
                       QueryCallback callback)
  {
    _client.queryGet(to, payload, callback);
  }

  public Serializable querySet(String to,
                               Serializable payload)
  {
    return _client.querySet(to, payload);
  }

  public Serializable querySet(String to,
                               Serializable payload,
                               long timeout)
  {
    return _client.querySet(to, payload, timeout);
  }

  public void querySet(String to,
                       Serializable payload,
                       QueryCallback callback)
  {
    _client.querySet(to, payload, callback);
  }

  public boolean isClosed()
  {
    return _client.isClosed();
  }

  public void close()
  {
    _client.close();
  }

  public final boolean onQueryResult(long id,
                                     String to,
                                     String from,
                                     Serializable payload)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public final boolean onQueryError(long id,
                                    String to,
                                    String from,
                                    Serializable payload,
                                    ActorError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private ActorClientFactory getFactory()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    ActorClientFactory factory = null;

    synchronized (_factoryMap) {
      SoftReference<ActorClientFactory> factoryRef
        = _factoryMap.get(loader);

      if (factoryRef != null) {
        factory = factoryRef.get();
      
        if (factory != null)
          return factory;
      }
    }

    try {
      String name = readFactoryClassName();

      if (name != null) {
        Class cl = Class.forName(name, false, loader);

        factory = (ActorClientFactory) cl.newInstance();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (factory == null)
      throw new IllegalStateException("Can't find a valid ActorClient");

    synchronized (_factoryMap) {
      _factoryMap.put(loader, new SoftReference<ActorClientFactory>(factory));
    }

    return factory;
  }

  private String readFactoryClassName()
  {
    InputStream is = null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      is = loader.getResourceAsStream("META-INF/services/com.caucho.bam.ClientActorFactory");

      if (is == null)
        return null;

      StringBuilder sb = new StringBuilder();
      int ch;

      while ((ch = is.read()) >= 0) {
        if (ch == '\r' || ch == '\n') {
          String line = sb.toString();

          int p = line.indexOf('#');
          if (p > 0)
            line = line.substring(0, p);

          line = line.trim();

          if (line.length() > 0)
            return line;

          sb = new StringBuilder();
        }
        else
          sb.append((char) ch);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }

    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client.getJid() + "]";
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    super.finalize();

    close();
  }
}
