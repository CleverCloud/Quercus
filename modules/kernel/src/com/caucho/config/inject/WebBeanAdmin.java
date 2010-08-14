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


package com.caucho.config.inject;

import com.caucho.management.server.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.spi.Bean;

/**
 * Administration for a JMS queue
 */
public class WebBeanAdmin extends AbstractManagedObject
  implements WebBeanMXBean
{
  private final Bean _bean;
  private int _id;

  public WebBeanAdmin(Bean bean, int id)
  {
    _bean = bean;
    _id = id;
  }

  //
  // configuration attributes
  //

  /**
   * Returns the unique id
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Returns the bean's name
   */
  public String getName()
  {
    String name = _bean.getName();

    if (name != null)
      return name;
    else
      return getBeanSimpleType();
  }

  /**
   * Returns the bean type
   */
  public String getBeanSimpleType()
  {
    Set<Class<?>> types = _bean.getTypes();

    Iterator<Class<?>> iter = types.iterator();

    if (iter.hasNext())
      return iter.next().getSimpleName();
    else
      return null;
  }

  /**
   * Returns all the bean's types
   */
  public String []getBeanTypes()
  {
    Set<Class<?>> types = _bean.getTypes();

    String []names = new String[types.size()];

    int i = 0;
    for (Class<?> type : types) {
      names[i++] = type.getName();
    }

    return names;
  }

  /**
   * Returns all the bean's binding types
   */
  public String []getQualifiers()
  {
    Set<Annotation> types = _bean.getQualifiers();

    ArrayList<String> nameList = new ArrayList<String>();

    for (Annotation ann : types) {
      if (ann != null)
        nameList.add(ann.toString());
    }

    String []names = new String[nameList.size()];
    nameList.toArray(names);

    return names;
  }

  /**
   * Returns the @ScopeType attribute
   */
  public String getScope()
  {
    Class annType = _bean.getScope();

    if (annType != null)
      return annType.getName();
    else
      return null;
  }

  /**
   * Adds unique properties
   */
  @Override
  protected void addObjectNameProperties(Map<String,String> props)
  {
    props.put("wid", String.valueOf(_id));
  }

  void register()
  {
    registerSelf();
  }

  void unregister()
  {
    unregisterSelf();
  }
}
