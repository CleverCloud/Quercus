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

package com.caucho.spring;

import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;
import com.caucho.util.*;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.support.*;

/**
 * Factory adapter from WebBeans to Spring.  Returns WebBeans values
 * directly to Spring.
 */
public class ResinBeanFactory extends DefaultListableBeanFactory
{
  private static final L10N L = new L10N(ResinBeanFactory.class);
  
  private final WebBeansContainer _webBeans = WebBeansContainer.create();
  
  /**
   * Creates a new bean factory adapter
   */
  public ResinBeanFactory()
  {
  }

  /**
   * Returns true for defined beans
   */
  @Override
  public boolean containsBeanDefinition(String beanName)
  {
    ComponentImpl comp = _webBeans.findByName(beanName);

    return comp != null;
  }

  /**
   * Returns the named beans
   */
  @Override
  public String []getBeanDefinitionNames()
  {
    return super.getBeanDefinitionNames();
  }

  public Object getBean(String name, Class requiredType, Object []args)
  {
    ComponentImpl comp = _webBeans.findByName(name);

    if (comp != null)
      return comp.get();

    throw new NoSuchBeanDefinitionException(name);
  }
}
