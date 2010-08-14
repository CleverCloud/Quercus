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

package com.caucho.naming.hessian;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.naming.AbstractModel;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory based model for JNDI.
 */
public class HessianModel extends AbstractModel {
  private HessianProxyFactory _proxyFactory = new HessianProxyFactory();
  
  /**
   * Creates a new instance of the memory model.
   */
  public HessianModel()
  {
  }

  /**
   * Creates a new instance of MemoryModel.
   */
  protected AbstractModel create()
  {
    return new HessianModel();
  }

  /**
   * Returns the object from looking up a single link.
   *
   * @param name the name segment.
   *
   * @return the object stored in the map.
   */
  public Object lookup(String name)
    throws NamingException
  {
    if (name == null)
      return null;
    
    if (name.startsWith("hessian:"))
      name = name.substring("hessian:".length());
    
    if (name.startsWith("//"))
      name = "http:" + name;

    try {
      return _proxyFactory.create(name);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      NamingException exn = new NamingException(e.toString());
      exn.initCause(e);
      throw exn;
    }
  }

  /**
   * Lists the child names.
   */
  public List list()
  {
    return new ArrayList();
  }
}
