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

package com.caucho.config.program;

import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import java.util.logging.Logger;

/**
 * Generator for a component value.
 */
public class ComponentValueGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(ComponentValueGenerator.class.getName());
  private static final L10N L = new L10N(ComponentValueGenerator.class);

  private final InjectManager _beanManager;
  private final Bean _bean;

  private final String _location;

  public ComponentValueGenerator(String location, Bean bean)
  {
    if (bean == null)
      throw new NullPointerException();

    _location = location;

    _beanManager = InjectManager.create();
    _bean = bean;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    CreationalContext<?> env = _beanManager.createCreationalContext(_bean);
    Class type = _bean.getBeanClass();

    return _beanManager.getReference(_bean, type, env);
  }
}
