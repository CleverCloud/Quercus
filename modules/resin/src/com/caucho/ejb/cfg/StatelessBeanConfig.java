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

package com.caucho.ejb.cfg;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.Bean;
import javax.jms.*;

import com.caucho.config.*;
import com.caucho.config.cfg.AbstractBeanConfig;
import com.caucho.config.inject.CauchoBean;
import com.caucho.config.types.*;
import com.caucho.ejb.manager.*;

import com.caucho.util.*;

/**
 * ejb-stateless-bean configuration
 */
public class StatelessBeanConfig extends AbstractBeanConfig
{
  private static final L10N L = new L10N(StatelessBeanConfig.class);
  private static final Logger log
    = Logger.getLogger(StatelessBeanConfig.class.getName());

  public StatelessBeanConfig()
  {
  }

  @Override
  protected void initImpl()
  {
    if (getInstanceClass() == null)
      throw new ConfigException(L.l("ejb-stateless-bean requires a 'class' attribute"));

    final String name = getName();

    Annotation ann = new Stateless() {
        public Class annotationType() { return Stateless.class; }
        public String name() { return name; }
        public String mappedName() { return name; }
        public String description() { return ""; }
    };

    add(ann);
    
    EjbManager ejbContainer = EjbManager.create();
    ejbContainer.createBean(buildAnnotatedType(), null);
  }
  
  @Override
  protected void deploy()
  {
  }
}

