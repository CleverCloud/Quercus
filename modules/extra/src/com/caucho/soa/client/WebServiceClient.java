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
 * @author Emil Ong
 */

package com.caucho.soa.client;

import com.caucho.config.ConfigException;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;
import com.caucho.webbeans.cfg.AbstractBeanConfig;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 */
public class WebServiceClient extends AbstractBeanConfig
   implements ObjectProxy, java.io.Serializable
{
  private static final Logger log 
    = Logger.getLogger(WebServiceClient.class.getName());
  private static final L10N L = new L10N(WebServiceClient.class);

  private Class _serviceClass;
  private String _url;

  private ArrayList<Class> _jaxbClasses = null;
  private StringBuilder _jaxbPackages = null;

  public void setClass(Class serviceClass)
  {
    setInterface(serviceClass);
  }

  public void setInterface(Class serviceClass)
  {
    _serviceClass = serviceClass;
  }

  public void setUrl(String url)
  {
    _url = url;
  }

  public void addJaxbClass(Class jaxbClass)
    throws ConfigException
  {
    if (_jaxbPackages != null) {
      throw new ConfigException(L.l("cannot set <jaxb-class> and <jaxb-package> simultaneously"));
    }

    if (_jaxbClasses == null)
      _jaxbClasses = new ArrayList<Class>();

    _jaxbClasses.add(jaxbClass);
  }

  public void addJaxbPackage(String jaxbPackage)
    throws ConfigException
  {
    if (_jaxbClasses != null) {
      throw new ConfigException(L.l("cannot set <jaxb-class> and <jaxb-package> simultaneously"));
    }

    if (_jaxbPackages == null)
      _jaxbPackages = new StringBuilder();
    else
      _jaxbPackages.append(':');

    _jaxbPackages.append(jaxbPackage);
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
    try {
      Object proxy = null;

      if (_jaxbClasses != null) {
        Class[] jaxbClasses = new Class[_jaxbClasses.size()];
        _jaxbClasses.toArray(jaxbClasses);
        proxy = ProxyManager.getWebServiceProxy(_serviceClass, 
                                                _url, 
                                                jaxbClasses);
      }
      else if (_jaxbPackages != null) {
        String jaxbPackages = _jaxbPackages.toString();
        proxy = 
          ProxyManager.getWebServiceProxy(_serviceClass, _url, jaxbPackages);
      }
      else {
        proxy = ProxyManager.getWebServiceProxy(_serviceClass, _url);
      }

      return proxy;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  public Object createService(Constructor ctor)
    throws ConfigException
  {
    try {
      int p = _url.indexOf(':');

      String urlName = _url.substring(p + 1);
      
      URL url = new URL(urlName);
      String action = "dummy-action";
      
      QName name = new QName(urlName, "dummy-action");

      return ctor.newInstance(url, name);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  public Object createProxy(Class api)
    throws NamingException
  {
    try {
      Object proxy = null;

      if (_jaxbClasses != null) {
        Class[] jaxbClasses = _jaxbClasses.toArray(new Class[0]);
        proxy = ProxyManager.getWebServiceProxy(api, _url, jaxbClasses);
      }
      else if (_jaxbPackages != null) {
        String jaxbPackages = _jaxbPackages.toString();
        proxy = 
          ProxyManager.getWebServiceProxy(api, _url, jaxbPackages);
      }
      else {
        proxy = ProxyManager.getWebServiceProxy(api, _url);
      }

      return proxy;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @PostConstruct
  public void init()
    throws Throwable
  {
    register(createObject(new Hashtable()), _serviceClass);
  }
}

