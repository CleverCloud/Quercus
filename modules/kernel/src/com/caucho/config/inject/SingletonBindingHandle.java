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
public class SingletonBindingHandle implements Serializable
{
  private static final L10N L = new L10N(SingletonBindingHandle.class);
  private static final Logger log
    = Logger.getLogger(SingletonBindingHandle.class.getName());

  private final Class _type;
  private final Annotation []_binding;

  private SingletonBindingHandle()
  {
    _type = null;
    _binding = null;
  }

  public SingletonBindingHandle(Class type, Annotation ...binding)
  {
    _type = type;

    if (binding != null && binding.length == 0)
      binding = null;

    _binding = binding;
  }

  /**
   * Deserialization resolution
   */
  public Object readResolve()
  {
    try {
      InjectManager inject = InjectManager.create();

      Bean bean
        = inject.resolve(inject.getBeans(_type, _binding));

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
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
