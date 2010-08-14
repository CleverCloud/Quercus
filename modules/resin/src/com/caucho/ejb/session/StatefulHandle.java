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

package com.caucho.ejb.session;

import java.io.Serializable;

import com.caucho.ejb.manager.EjbManager;
import com.caucho.util.L10N;

/**
 * Abstract base class for a stateful session object
 */
public class StatefulHandle
  implements Serializable
{
  private static final L10N L = new L10N(StatefulHandle.class);
  
  private String _ejbName;
  private String _primaryKey;
  
  public StatefulHandle(String ejbName, String primaryKey)
  {
    _ejbName = ejbName;
    _primaryKey = primaryKey;
  }
  
  private Object readResolve()
  {
    EjbManager manager = EjbManager.create();
    
    StatefulManager<?> ejbManager
      = (StatefulManager<?>) manager.getServerByEjbName(_ejbName);
    
    if (ejbManager == null)
      throw new IllegalStateException(L.l("'{0}' is an unknown @Stateful ejb-name for deserialization in {1}",
                                          _ejbName, manager));
    
    Object proxy = ejbManager.getStatefulProxy(_primaryKey);
    
    System.out.println("READ: " + _ejbName + " " + ejbManager + " " + proxy);
    
    return proxy;
  }
}
