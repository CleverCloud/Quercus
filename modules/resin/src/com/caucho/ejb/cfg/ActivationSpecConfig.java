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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import javax.resource.spi.*;

import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.*;
import com.caucho.util.L10N;

/**
 * Configures an activation spec
 */
public class ActivationSpecConfig extends BeanConfig {
  private static final L10N L = new L10N(ActivationSpecConfig.class);

  public ActivationSpecConfig()
  {
    setScope("singleton");
  }

  @Override
  public Class<?> getBeanConfigClass()
  {
    return ActivationSpec.class;
  }

  /**
   * Override the old meaning of type for backward compat.
   */
  @Override
  public void setType(Class<?> cl)
  {
    setClass(cl);
  }

  /**
   * Check for correct type.
   */
  @Override
  public void setClass(Class<?> cl)
  {
    super.setClass(cl);

    if (! ActivationSpec.class.isAssignableFrom(cl))
      throw new ConfigException(L.l("<activation-spec> class '{0}' must implement javax.resource.spi.ActivationSpec",
                                    cl.getName()));
  }

  @Override
  protected void deploy()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

