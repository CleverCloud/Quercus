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

package com.caucho.mule;

import java.io.Reader;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.webbeans.ComponentFactory;

import com.caucho.util.L10N;
import com.caucho.webbeans.manager.*;

import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

public class ResinContainerContext implements UMOContainerContext
{
  private static final L10N L = new L10N(ResinContainerContext.class);
  private static final Logger log = 
    Logger.getLogger(ResinContainerContext.class.getName());

  private final WeakHashMap<Class,ComponentFactory> _componentMap
    = new WeakHashMap<Class,ComponentFactory>();

  private final WebBeansContainer _webBeans = WebBeansContainer.create();

  private String _name = "resin";

  public ResinContainerContext()
  {
  }

  public void initialise()
    throws InitialisationException
  {
  }

  public void dispose()
  {
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public Object getComponent(Object key) 
    throws ObjectNotFoundException
  {
    if (key == null)
      throw new ObjectNotFoundException("Component key is null");

    if (log.isLoggable(Level.FINE)) {
      log.fine(L.l("ResinContainerContext.getComponent({0} with type {1})",
                   key, key.getClass().getName()));
    }

    if (key instanceof ContainerKeyPair) {
      ContainerKeyPair pair = (ContainerKeyPair) key;

      // XXX pair.getContainerName() appears to be null most of the time
      // ignore it?

      key = pair.getKey();
    }

    if (key instanceof Class) {
      Class clazz = (Class) key;
      ComponentFactory component = null;

      if (log.isLoggable(Level.FINE))
        log.fine("Creating new instance from " + clazz);

      synchronized (_componentMap) {
        component = _componentMap.get(clazz);

        if (component == null) {
          component = _webBeans.resolveByType(clazz);

          if (component == null)
            component = _webBeans.createTransient(clazz);

          _componentMap.put(clazz, component);
        }
      }

      return component.get();
    }
    else if (key instanceof String) {
      ComponentFactory component = _webBeans.findByName((String) key);

      if (component == null) {
        throw new ObjectNotFoundException(L.l("Cannot find component with name '{0}'", key));
      }

      return component.get();
    }
    else {
      throw new ObjectNotFoundException(L.l("Component keys of type {0} are not understood", key.getClass().getName()));
    }
  }

  public void configure(Reader configuration, String doctype, String encoding) 
    throws ContainerException
  {
    // Resin beans are configured from 
    // resin.conf, web.xml, resin-web.xml, and/or web-beans.xml
  }
}
