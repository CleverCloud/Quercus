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
 * @author Sam
 */

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.BeanNameLiteral;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the ejb-ref.
 *
 * An ejb-ref is used to make an ejb available within the environment
 * in which the ejb-ref is declared.
 */
public class EjbRef extends BaseRef {
  private static final L10N L = new L10N(EjbRef.class);
  private static final Logger log
    = Logger.getLogger(EjbRef.class.getName());

  private String _loc;
  
  private Context _context;

  private String _ejbRefName;
  private String _ejbRefType;
  private Class<?> _type;
  
  private Class<?> _home;
  private Class<?> _remote;

  private String _foreignName;
  private String _ejbLink;

  private Object _linkValue;

  private Object _target;

  private boolean _isInitBinding;

  private String _clientClassName;

  public EjbRef()
  {
  }

  public EjbRef(Context context)
  {
    _context = context;
  }

  public EjbRef(Path modulePath)
  {
    super(modulePath);
  }


  public EjbRef(Path modulePath, String sourceEjbName)
  {
    super(modulePath, sourceEjbName);
  }

  public void setConfigLocation(String loc)
  {
    _loc = loc;
  }

  public boolean isEjbLocalRef()
  {
    return false;
  }

  /**
   * Gets the injection-target
   */
  public InjectionTarget getInjectionTarget()
  {
    return _injectionTarget;
  }

  protected String getTagName()
  {
    return "<ejb-ref>";
  }

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }

  public void setClientClassName(String clientClassName)
  {
    _clientClassName = clientClassName;
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
   * Sets the injection-target
   */
  public void setInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTarget = injectionTarget;
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
    _ejbRefType = type;
  }

  public void setHome(Class<?> home)
  {
    _home = home;
  }

  /**
   * Returns the home class.
   */
  public Class<?> getHome()
  {
    return _home;
  }

  public void setRemote(Class<?> remote)
  {
    _remote = remote;
  }

  /**
   * Returns the remote class.
   */
  public Class<?> getRemote()
  {
    // XXX: should distinguish
    return _remote;
  }
  
  public Class<?> getLocal()
  {
    return null;
  }

  /**
   * Sets the canonical jndi name to use to find the bean that
   * is the target of the reference.
   * For remote beans, a &lt;jndi-link> {@link com.caucho.naming.LinkProxy} is
   * used to link the local jndi context referred to in this name to
   * a remote context.
   */
  public void setForeignName(String foreignName)
  {
    _foreignName = foreignName;
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
  
  public String getEjbLink()
  {
    return _ejbLink;
  }

  /**
   * Merges duplicated information in application-client.xml / resin-application-client.xml
   */
  public void mergeFrom(EjbRef other)
  {
    if (_foreignName == null)
      _foreignName = other._foreignName;

    if (_ejbLink == null)
      _ejbLink = other._ejbLink;

    if (_type == null)
      _type = other._type;

    if (_ejbRefType == null)
      _ejbRefType = other._ejbRefType;

    if (_home == null)
      _home = other._home;

    if (_remote == null)
      _remote = other._remote;

    if (_injectionTarget == null)
      _injectionTarget = other._injectionTarget;
  }

  @Override
  public void deploy()
  {
    super.deploy();
    
    if (_ejbRefType == null)
      throw new ConfigException(L.l("<ejb-ref-type> is missing for <ejb-ref> {0}",
                                    _ejbRefName));
    
    _type = getLocal();
   
    try {
      Jndi.bindDeepShort(_ejbRefName, this);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    // new BeanJndiProxy(injectManager, bean));
  }

  @Override
  public void bind()
  {
    deploy();
  }

  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  @Override
  public Object getValue()
  {
    String lookup = getLookupName();
    
    if (lookup != null) {
      return Jndi.lookup(lookup);
    }
    
    if (_type == null)
      throw new IllegalStateException(String.valueOf(this));
    
    InjectManager injectManager = InjectManager.getCurrent();
    
    Set<Bean<?>> beans = null;
    
    if (getEjbLink() != null)
      beans = injectManager.getBeans(_type, new BeanNameLiteral(getEjbLink()));
    
    if (beans == null || beans.size() == 0)
      beans = injectManager.getBeans(_type);
    
    Bean<?> bean;
    
    try {
      bean = injectManager.resolve(beans);
    } catch (Exception e) {
      throw new ConfigException(L.l("{0} can't resolve a unique bean.\n  {1}",
                                    this, e.toString(),
                                    e));
    }
    
    if (bean == null)
      throw new ConfigException(L.l("ejb-ref '{0}' is an unknown bean", 
                                    _type));
    
    CreationalContext<?> cxt = injectManager.createCreationalContext(bean);
    
    return injectManager.getReference(bean, _type, cxt);
    
    /*
     if (_target == null) {
      // ejb/0f6g, TCK
      if (_foreignName != null)
        resolve(null);
      else if (_home != null)
        resolve(_home);
      else if (_remote != null)
        resolve(_remote);
      else if (getLocal() != null) // ejb/0f6g
        resolve(getLocal());
      else
        resolve(null);
    }

    return _target;
    */
  }

  public Object getByType(Class<?> type)
  {
    try {
      if (_home != null && type.isAssignableFrom(_home))
        return createObject(null);

      if (_remote != null && type.isAssignableFrom(_remote))
        return createObject(null);

      if (_foreignName != null) {
        int pos = _foreignName.indexOf("#");

        if (pos > 0) {
          String intf = _foreignName.substring(++pos).replace("_", ".");

          // TCK: application-client.xml with multiple business interfaces.
          if (! type.getName().equals(intf))
            return null;
        }

        Object target;

        // XXX: JDK's iiop lookup
        String foreignName = _foreignName.replace('.', '_');

        if (_context != null) {
          target = _context.lookup(foreignName);
        } else {
          target = Jndi.lookup(foreignName);
        }

        if (target != null && type != null)
          return PortableRemoteObject.narrow(target, type);
      }
    } catch (Exception e) {
      // log.log(Level.FINER, e.toString(), e);
    }

    return null;
  }

  private void resolve(Class<?> type)
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} resolving", this));

    if (_foreignName != null)
      _target = lookupByForeignJndi(_foreignName, type);
    else if (_ejbLink != null)
      _target = lookupByLink(_ejbLink, type);
    else
      _target = lookupLocal(type);

    if (log.isLoggable(Level.CONFIG))
      log.log(Level.CONFIG, L.l("{0} resolved", this));
  }

  private Object lookupByLink(String link, Class<?> type)
    throws NamingException
  {
    Object target = null;

    String archiveName;
    String ejbName;

    int hashIndex = link.indexOf('#');

    if (hashIndex < 0) {
      archiveName = null;
      ejbName = link;
    }
    else {
      archiveName = link.substring(0, hashIndex);
      ejbName = link.substring(hashIndex + 1);
    }

    try {
      Path path = archiveName == null ? _modulePath : _modulePath.lookup(archiveName);

      if (true)
        throw new IllegalStateException();
 
      if (false) throw new NamingException();
    }
    catch (NamingException e) {
      throw e;
    }
    catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      throw new NamingException(L.l("{0} '{1}'  ejb-link '{2}' invalid ",
                                    getTagName(), _ejbRefName, link));
    }

    return target;
  }

  private Object lookupByForeignJndi(String foreignName, Class type)
    throws NamingException
  {
    Object target = Jndi.lookup(foreignName);

    return target;
  }

  private Object lookupLocal(Class type)
  {
    return null;
  }

  public String toString()
  {
    return getClass().getSimpleName()
      +  "[" + _ejbRefName + ", " + _ejbLink + ", " +  _foreignName + "]";
  }
}
