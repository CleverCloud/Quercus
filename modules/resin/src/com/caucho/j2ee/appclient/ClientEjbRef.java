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

package com.caucho.j2ee.appclient;

import com.caucho.config.ConfigException;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.*;
import javax.rmi.PortableRemoteObject;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Configuration for the ejb-ref.
 *
 * An ejb-ref is used to make an ejb available within the environment
 * in which the ejb-ref is declared.
 */
public class ClientEjbRef implements ObjectProxy {
  private static final L10N L = new L10N(ClientEjbRef.class);
  private static final Logger log
    = Logger.getLogger(ClientEjbRef.class.getName());

  private Context _ic;
  
  private String _ejbRefName;
  private String _type;
  private Class _home;
  private Class _remote;
  private String _jndiName;
  private String _ejbLink;

  public ClientEjbRef(Context ic)
  {
    _ic = ic;
  }

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }

  /**
   * Sets the name to use in the local jndi context.
   * This is the jndi lookup name that code uses to obtain the home for
   * the bean when doing a jndi lookup.
   *
   * <pre>
   *   <ejb-ref-name>ejb/Gryffindor</ejb-ref-name>
   *   ...
   *   (new InitialContext()).lookup("java:comp/env/ejb/Gryffindor");
   * </pre>
   */
  public void setEjbRefName(String name)
  {
    _ejbRefName = name;
  }

  /**
   * Returns the ejb name.
   */
  public String getEjbRefName()
  {
    return _ejbRefName;
  }

  public void setEjbRefType(String type)
  {
    _type = type;
  }

  public void setHome(Class home)
  {
    _home = home;
  }

  /**
   * Returns the home class.
   */
  public Class getHome()
  {
    return _home;
  }

  public void setRemote(Class remote)
  {
    _remote = remote;
  }

  /**
   * Returns the remote class.
   */
  public Class getRemote()
  {
    return _remote;
  }

  /**
   * Sets the canonical jndi name to use to find the bean that
   * is the target of the reference.
   * For remote beans, a &lt;jndi-link> {@link com.caucho.naming.LinkProxy} is
   * used to link the local jndi context referred to in this name to
   * a remote context.
   */
  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  /**
   * Set the target of the reference, an alternative to {@link #setJndiName(String)}.
   * The format of the ejbLink is "bean", or "jarname#bean", where <i>bean</i> is the
   * ejb-name of a bean within the same enterprise application, and <i>jarname</i>
   * further qualifies the identity of the target.
   */
  public void setEjbLink(String ejbLink)
  {
    _ejbLink = ejbLink;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_ejbRefName == null)
      throw new ConfigException(L.l("{0} is required", "<ejb-ref-name>"));

    _ejbRefName = Jndi.getFullName(_ejbRefName);

    if (_ejbLink != null) {
      // TCK: ejb30.persistence.annotations.entity
      Jndi.bindDeepShort(_ejbRefName, this);
    }

    ClientEjbRefContext context = ClientEjbRefContext.createLocal();

    context.add(this);
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
    Context context = new InitialContext();

    Object value = context.lookup(Jndi.getFullName(_ejbLink));

    if (_home != null)
      return PortableRemoteObject.narrow(value, _home);
    else if (_remote != null)
      return PortableRemoteObject.narrow(value, _remote);
    else
      return value;
  }

  public String toString()
  {
    return "ClientEjbRef[" + _ejbRefName + ", " + _ejbLink + ", " +  _jndiName + "]";
  }
}
