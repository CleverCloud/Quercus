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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import javax.enterprise.inject.spi.Bean;

import com.caucho.util.*;
import com.caucho.config.cfg.*;

/**
 * Handle for webbeans serialization
 */
public class SingletonHandle implements Serializable
{
  private static final L10N L = new L10N(SingletonHandle.class);
  private static final Logger log
    = Logger.getLogger(SingletonHandle.class.getName());

  private final String _id;

  private SingletonHandle()
  {
    _id = null;
  }

  public SingletonHandle(String id)
  {
    _id = id;
  }

  /**
   * Deserialization resolution
   */
  public Object readResolve()
  {
    try {
      InjectManager inject = InjectManager.create();

      Bean bean = inject.getPassivationCapableBean(_id);

      if (bean == null)
        throw new IllegalStateException(L.l("'{0}' is an unknown SingletonHandle bean.  Unserializing this bean requires an equivalent Managed Bean to be registered.",
                                           _id));

      return inject.getReference(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
