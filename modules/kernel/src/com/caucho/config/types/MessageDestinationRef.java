/* * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.rmi.PortableRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Hashtable;

public class MessageDestinationRef
  implements ObjectProxy
{
  private final L10N L = new L10N(MessageDestinationRef.class);
  private final Logger log = Logger.getLogger(MessageDestinationRef.class.getName());

  private final Path _modulePath;
  private final Context _context;

  private String _refName;
  private Class _type;
  private String _link;
  private String _foreignName;

  private Object _target;

  private InjectionTarget _injectionTarget;

  public MessageDestinationRef()
  {
    _modulePath = Vfs.lookup();
    _context = null;
  }

  public MessageDestinationRef(Path modulePath)
  {
    _modulePath = modulePath;
    _context = null;
  }

  public MessageDestinationRef(Context context)
  {
    _context = context;
    _modulePath = Vfs.getPwd();
  }

  public void setDescription(String description)
  {
  }

  /**
   * Sets the injection-target
   */
  public void setInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTarget = injectionTarget;
  }

  public void setMessageDestinationRefName(String refName)
  {
    _refName = refName;
  }

  public void setMessageDestinationType(Class type)
  {
    _type = type;
  }

  public void setMessageDestinationUsage(String usage)
  {
  }

  public void setMessageDestinationLink(String link)
  {
    _link = link;
  }

  public void setForeignName(String foreignName)
  {
    _foreignName = foreignName;
  }

  @PostConstruct
  public void init()
    throws NamingException
  {
    boolean bind = false;

    if (_link == null && _foreignName == null) {
            /*
      EJBServer server = EJBServer.getLocal();

      if (server != null)
        _link = _refName;
      else
        _foreignName = _refName;
        */
    }

    String fullRefName = Jndi.getFullName(_refName);

    if (_link != null) {
      bind = true;
    }
    else if (_foreignName != null) {
      String fullForeignName = Jndi.getFullName(_foreignName);

      if (! fullForeignName.equals(fullRefName))
        bind = true;
    }

    if (bind) {
      Jndi.rebindDeep(fullRefName, this);
    }

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, L.l("{0} init", this));
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
    return "<message-destination-ref>";
  }

  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    if (_target == null) {
      resolve(_type);
    }

    return _target;
  }

  private void resolve(Class type)
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} resolving", this));

    if (_foreignName != null) {
      _target = lookupByForeignJndi(_foreignName, type);
    }
    else {
      _target = lookupByLink(_link, type);
    }

    if (log.isLoggable(Level.CONFIG))
      if (log.isLoggable(Level.CONFIG))
        log.log(Level.CONFIG, L.l("{0} resolved", this));
  }

  private Object lookupByLink(String link, Class type)
    throws NamingException
  {
        return null;
        /*
    Object target = null;

    String archiveName;
    String name;

    int hashIndex = link.indexOf('#');

    if (hashIndex < 0) {
      archiveName = null;
      name = link;
    }
    else {
      archiveName = link.substring(0, hashIndex);
      name = link.substring(hashIndex + 1);
    }

    try {
      Path path = archiveName == null ? _modulePath : _modulePath.lookup(archiveName);

      EjbContainer ejbContainer = EjbContainer.getCurrent();
      MessageDestination dest = null;

      if (ejbContainer != null) {

        if (true)
          throw new IllegalStateException();

        if (dest != null) {

          target = dest.getResolvedDestination();

          if (target == null)  {
            log.log(Level.FINE, L.l("no destination is available for '{0}'", dest));

            throw new NamingException(L.l("{0} '{1}' message-destination found with message-destination-link '{2}' has no valid destination",
                                          getTagName(), _refName, link));
          }
        }
      }
      else {
        target = lookupByForeignJndi(link, type);
      }
    }
    catch (NamingException e) {
      throw e;
    }
    catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      throw new NamingException(L.l("{0} '{1}'  message-destination-link '{2}' invalid ",
                                    getTagName(), _refName, link));
    }

    if (log.isLoggable(Level.CONFIG))
      log.log(Level.CONFIG, L.l("{0} resolved", this));

    return target;
    */
  }

  private Object lookupByForeignJndi(String foreignName, Class type)
    throws NamingException
  {
    Object target;

    if (_context != null) {
      target = _context.lookup(Jndi.getFullName(foreignName));
    }
    else
      target = Jndi.lookup(foreignName);

    if (target == null) {
      if (foreignName.equals(_refName))
        throw new NamingException(L.l("{0} '{1}' cannot be resolved",
                                      getTagName(), _refName));
      else
        throw new NamingException(L.l("{0} '{1}' foreign-name '{2}' not found",
                                      getTagName(), _refName, foreignName));
    }

    if (type != null)
      target = PortableRemoteObject.narrow(target, type);

    return target;
  }

  public String toString()
  {
    return getClass().getSimpleName()
           +  "[ref-name=" + _refName + ", link=" + _link + ", foreign-name=" +  _foreignName + "]";
  }
}
