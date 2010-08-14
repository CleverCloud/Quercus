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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.naming;

import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object proxy for a foreign JNDI factory.
 */
public class LinkProxy implements ObjectProxy, java.io.Serializable {
  private static Logger log
    = Logger.getLogger(LinkProxy.class.getName());
  private static L10N L = new L10N(LinkProxy.class);

  // The foreign factory
  protected InitialContextFactory _factory;
  // The foreign factory
  protected Class _factoryClass;
  // Properties for the object
  protected Hashtable<String,String> _props = new Hashtable<String,String>();
  // The jndi-link path
  protected String _jndiName;
  // The jndi-link path
  protected String _jndiLocalName;
  // The foreign name
  protected String _foreignName;

  /**
   * Creates a new LinkProxy.
   */
  public LinkProxy()
    throws NamingException
  {
  }

  /**
   * Creates a new LinkProxy.
   *
   * @param factory the foreign factory
   * @param props the properties for the object
   * @param name the jndi-link path in the foreign namespace
   */
  public LinkProxy(InitialContextFactory factory,
                   Hashtable<String,String> props,
                   String name)
    throws NamingException
  {
    if (factory == null)
      throw new NullPointerException();
    
    _factory = factory;
    _props = props;
    _foreignName = name;
  }

  /**
   * Creates a new LinkProxy.
   *
   * @param name the jndi-link path in the foreign namespace
   */
  public LinkProxy(String name)
    throws NamingException
  {
    _foreignName = name;
  }

  /**
   * Sets the jndi name.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Sets the jndi name.
   */
  public void setJndiLocalName(String name)
  {
    _jndiLocalName = name;
  }

  /**
   * @deprecated Use {@link #setJndiName}
   */
  public void setName(String name)
  {
    setJndiName(name);
  }

  /**
   * Sets the factory
   */
  public void setFactory(Class factoryClass)
  {
    _factoryClass = factoryClass;
  }

  /**
   * @deprecated Use {@link #setFactory}
   */
  public void setJndiFactory(Class factoryClass)
  {
    setFactory(factoryClass);
  }

  /**
   * Sets the foreign-name
   */
  public void setForeignName(String name)
  {
    _foreignName = name;
  }

  /**
   * Adds init param.
   */
  public void addInitParam(InitParam initParam)
  {
    if (_props == null)
      _props = new Hashtable<String,String>();
    
    _props.putAll(initParam.getParameters());
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    Context context;
    Hashtable<String,String> mergeEnv;

    mergeEnv = new Hashtable<String,String>();

    if (env != null && env.size() > 0)
      mergeEnv.putAll(env);
    if (_props != null && _props.size() > 0)
      mergeEnv.putAll(_props);

    context = new InitialContext(mergeEnv);

    if (_foreignName != null) {
      String foreignName;

      if (_factoryClass != null)
        foreignName = _foreignName;
      else
        foreignName = Jndi.getFullName(_foreignName);

      try {
        Object value = context.lookup(foreignName);

        return value;
      } catch (RuntimeException e) {
        throw e;
      } catch (NamingException e) {
        throw e;
      }
    }
    else
      return context;
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_jndiName == null && _jndiLocalName == null)
      throw new ConfigException(L.l("<jndi-link> configuration needs a <jndi-name>.  The <jndi-name> is the JNDI name where the context will be linked."));
    
    Class factoryClass = _factoryClass;

    if (factoryClass != null) {
      if (_props == null)
        _props = new Hashtable<String,String>();

      _props.put("java.naming.factory.initial", factoryClass.getName());
    }

    if (log.isLoggable(Level.CONFIG)) {
      if (_foreignName != null)
        log.config("jndi-link[jndi-name=" + _jndiName
                   + ", foreign-name=" + _foreignName + "] configured");
      else if (_factoryClass != null)
        log.config("jndi-link[jndi-name=" + _jndiName
                   + ", factory=" + _factoryClass.getName() + "] configured");
    }

    if (_foreignName != null
        && Jndi.getFullName(_jndiName).equals(Jndi.getFullName(_foreignName)))
      return;

    // server/155a - not a short link since it needs to be able to bind
    // the jndi root
    if (_jndiLocalName != null)
      Jndi.rebindDeep(_jndiLocalName, this);
    else
      Jndi.rebindDeepShort(_jndiName, this);
  }

  public String toString()
  {
    if (_factoryClass != null)
      return "LinkProxy[name=" + _jndiName + ",factory=" + _factoryClass.getName() + "]";
    else
      return "LinkProxy[name=" + _jndiName + ",foreign=" + _foreignName + "]";
  }
}
