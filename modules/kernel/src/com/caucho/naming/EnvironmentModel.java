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

import com.caucho.util.L10N;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Environment based model for JNDI.
 */
public class EnvironmentModel extends AbstractModel
{
  private static final L10N L = new L10N(EnvironmentModel.class);
  
  private final EnvironmentModelRoot _root;
  private String _name;
  
  private HashMap<String,Object> _children
    = new HashMap<String,Object>(8);

  /**
   * Creates a new instance of the environment model.
   */
  EnvironmentModel(EnvironmentModelRoot root, String name)
  {
    _root = root;
    _name = name;
  }

  /**
   * Creates a new instance of EnvironmentModel.
   */
  protected AbstractModel create()
  {
    return new EnvironmentModel(_root, _name);
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
    Object value = _children.get(name);

    if (value != null)
      return value;

    ClassLoader loader = _root.getClassLoader();

    if (loader == null)
      return null;
    
    EnvironmentModelRoot parentRoot
      = EnvironmentModelRoot.getCurrent(loader.getParent());

    if (parentRoot != null) {
      EnvironmentModel parentModel = parentRoot.get(_name);

      if (parentModel != null) {
        value = parentModel.lookup(name);

        if (value instanceof EnvironmentModel) {
          value = createSubcontext(name);
        }
      }
    }
      
    return value;
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
    Object oldValue = _children.remove(name);
    
    if (oldValue == null)
      throw new NameNotFoundException(name);
    else if (oldValue instanceof EnvironmentModel)
      _root.remove(_name + "/" + name);
  }

  /**
   * Creates a subcontext.
   */
  public AbstractModel createSubcontext(String name)
    throws NamingException
  {
    if (_children.get(name) != null) {
      throw new NamingException(L.l("can't create subcontext: {0} {1}",
                                    name, _children.get(name)));
    }

    String childName;

    if (_name.equals(""))
      childName = name;
    else
      childName = _name + "/" + name;
    
    EnvironmentModel model = new EnvironmentModel(_root, childName);
    
    _children.put(name, model);

    _root.put(childName, model);

    return model;
  }
  
  /**
   * Renames a child.
   */
  public void rename(String newName)
    throws NamingException
  {
    _name = newName;
    HashMap<String,Object> newChildren = new HashMap<String,Object>();
    
    for (Entry<String,Object> entry : _children.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      
      String name = newName + "/" + key;
      
      if (obj instanceof EnvironmentModel)
        ((EnvironmentModel) obj).rename(name);
      
      newChildren.put(name, obj);
    }
    
    _children = newChildren;
  }

  /**
   * Lists the child names.
   */
  public List list()
  {
    ArrayList values = new ArrayList();

    fillList(values);

    return values;
  }

  protected void fillList(ArrayList values)
  {
    for (String key : _children.keySet()) {
      if (! values.contains(key))
        values.add(key);
    }

    ClassLoader loader = _root.getClassLoader();

    if (loader == null)
      return;
    
    EnvironmentModelRoot parentRoot
      = EnvironmentModelRoot.getCurrent(loader.getParent());

    if (parentRoot != null) {
      EnvironmentModel parentModel = parentRoot.get(_name);

      if (parentModel != null) {
        parentModel.fillList(values);
      }
    }
  }
  
  public String toString()
  {
    return "EnvironmentModel[" + _name + "]";
  }
}
