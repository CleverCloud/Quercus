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

package com.caucho.config.type;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.xml.XmlBeanAttribute;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.loader.*;
import com.caucho.make.*;
import com.caucho.el.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import org.w3c.dom.Node;

/**
 * Represents an introspected bean type for configuration.
 */
public class EnvironmentBeanType extends InlineBeanType
{
  private static final L10N L = new L10N(EnvironmentBeanType.class);
  private static final Logger log
    = Logger.getLogger(EnvironmentBeanType.class.getName());

  public EnvironmentBeanType(Class beanClass)
  {
    super(beanClass);

    setAddCustomBean(XmlBeanAttribute.ATTRIBUTE);
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(XmlConfigContext env, Object bean, Node node)
  {
    super.beforeConfigure(env, bean, node);
    
    EnvironmentBean envBean = (EnvironmentBean) bean;
    ClassLoader loader = envBean.getClassLoader();
    
    Thread thread = Thread.currentThread();
    
    thread.setContextClassLoader(loader);

    // XXX: builder.setClassLoader?

    ArrayList<Dependency> dependencyList = env.getDependencyList();
    if (dependencyList != null) {
      for (Dependency depend : dependencyList) {
        Environment.addDependency(depend);
      }
    }
    
    // XXX: addDependencies(builder);
  }
}
