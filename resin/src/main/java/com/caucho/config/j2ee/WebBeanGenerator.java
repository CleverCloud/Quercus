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

package com.caucho.config.j2ee;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.ConfigException;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;


abstract public class WebBeanGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(WebBeanGenerator.class.getName());
  private static final L10N L = new L10N(WebBeanGenerator.class);

  private InjectManager _beanManager = InjectManager.create();

  /**
   * Creates the value.
   */
  protected <T> T create(Class<T> type, Annotation...bindings)
  {
    Set<Bean<?>> beans = _beanManager.getBeans(type, bindings);

    Bean bean = _beanManager.resolve(beans);

    CreationalContext<?> env = _beanManager.createCreationalContext(bean);

    return (T) _beanManager.getReference(bean, type, env);
  }
}
