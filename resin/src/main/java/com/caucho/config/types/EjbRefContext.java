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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.naming.*;
import com.caucho.loader.*;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.*;
import javax.rmi.PortableRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Configuration for the ejb-ref.
 *
 * An ejb-ref is used to make an ejb available within the environment
 * in which the ejb-ref is declared.
 */
public class EjbRefContext
{
  private static final EnvironmentLocal<EjbRefContext> _local
    = new EnvironmentLocal<EjbRefContext>();

  private ArrayList<EjbRef> _ejbRefList = new ArrayList<EjbRef>();

  private EjbRefContext()
  {
  }

  public static EjbRefContext getLocal()
  {
    return _local.get();
  }

  public static EjbRefContext createLocal()
  {
    EjbRefContext local = _local.getLevel();

    if (local == null) {
      local = new EjbRefContext();
      _local.set(local);
    }

    return local;
  }

  void add(EjbRef ejbRef)
  {
    _ejbRefList.add(ejbRef);
  }

  public Object findByType(Class type)
  {
    for (int i = 0; i < _ejbRefList.size(); i++) {
      Object value = _ejbRefList.get(i).getByType(type);

      if (value != null) {
        // XXX: should also save on success
        return value;
      }
    }

    return null;
  }

  public Object findByBeanName(String ownerClassName,
                               String fieldName,
                               Class fieldType)
    throws NamingException
  {
    // ejb/0fbe
    String expected = ownerClassName + "/" + fieldName;

    for (int i = 0; i < _ejbRefList.size(); i++) {
      String ejbRefName = _ejbRefList.get(i).getEjbRefName();

      if (expected.equals(ejbRefName))
        return _ejbRefList.get(i).createObject(null);
    }

    return null;
  }

  public String toString()
  {
    return "EjbRefContext[]";
  }
}
