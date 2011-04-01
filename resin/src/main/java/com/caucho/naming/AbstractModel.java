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
import java.util.List;

/**
 * Abstract data model behind Resin's JNDI.
 */
abstract public class AbstractModel {
  protected AbstractModel create()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * This is a deep copy.
   *
   * @return a deep copy of the context
   */
  public AbstractModel copy()
    throws NamingException
  {
    AbstractModel copy = create();

    List names = list();
    for (int i = 0; i < names.size(); i++) {
      String name = (String) names.get(i);
      Object value = lookup(name);

      if (value instanceof AbstractModel)
        copy.bind(name, ((AbstractModel) value).copy());
      else
        copy.bind(name, value);
    }

    return copy;
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
    throw new UnsupportedOperationException();
  }

  /**
   * Binds an object as a child to the model.
   */
  public void bind(String name, Object obj)
    throws NamingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Unbinds an object as a child to the model.
   */
  public void unbind(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Creates a subcontext for the model.
   */
  public AbstractModel createSubcontext(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Renames a child.
   */
  public void rename(String newName)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Lists the child names.
   */
  public List list()
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }
}
