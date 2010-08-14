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

package com.caucho.servlets.naming;

import com.caucho.naming.*;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Hessian based model for JNDI.
 */
public class HessianModel extends AbstractModel
{
  private final NamingProxy _proxy;
  private final String _path;
  
  /**
   * Creates a new instance of the hessian model.
   */
  public HessianModel(NamingProxy proxy)
  {
    _proxy = proxy;
    _path = "";
  }
  
  /**
   * Creates a new instance of the hessian model.
   */
  HessianModel(NamingProxy proxy, String path)
  {
    _proxy = proxy;
    _path = path;
  }

  /**
   * Creates a new instance of HessianModel.
   */
  protected AbstractModel create()
  {
    return new HessianModel(_proxy, _path);
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
    String path = _path + "/" + name;

    Object v = _proxy.lookup(path);

    if (v instanceof RemoteContext)
      return new HessianModel(_proxy, path);
    else
      return v;
  }

  /**
   * Rebinds an object as a child to the model.
   */
  public void bind(String name, Object obj)
    throws NamingException
  {
    throw new NamingException("can't bind: " + name);
  }

  /**
   * Unbinds an object as a child to the model.
   */
  public void unbind(String name)
    throws NamingException
  {
    throw new NamingException("can't unbind: " + name);
  }

  /**
   * Creates a subcontext.
   */
  public AbstractModel createSubcontext(String name)
    throws NamingException
  {
    throw new NamingException("can't create subcontext: " + name);
  }

  /**
   * Lists the child names.
   */
  public List list()
  {
    return new ArrayList();
  }
}
