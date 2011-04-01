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

package com.caucho.naming;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Memory based model for JNDI.
 */
public class MemoryModel extends AbstractModel
{
  private HashMap<String,Object> _children
    = new HashMap<String,Object>(8);

  /**
   * Creates a new instance of the memory model.
   */
  public MemoryModel()
  {
  }

  /**
   * Creates a new instance of MemoryModel.
   */
  protected AbstractModel create()
  {
    return new MemoryModel();
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
    return _children.get(name);
  }

  /**
   * Rebinds an object as a child to the model.
   */
  public void bind(String name, Object obj)
    throws NamingException
  {
    _children.put(name, obj);
  }

  /**
   * Unbinds an object as a child to the model.
   */
  public void unbind(String name)
    throws NamingException
  {
    _children.remove(name);
  }

  /**
   * Creates a subcontext.
   */
  public AbstractModel createSubcontext(String name)
    throws NamingException
  {
    if (_children.get(name) != null)
      throw new NamingException("can't create subcontext: " + name + " " + _children.get(name));

    MemoryModel model = new MemoryModel();
    
    _children.put(name, model);

    return model;
  }

  /**
   * Lists the child names.
   */
  public List list()
  {
    return new ArrayList(_children.keySet());
  }
}
