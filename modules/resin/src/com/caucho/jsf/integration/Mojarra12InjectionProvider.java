/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 *  This file is part of Resin(R) Open Source
 *
 *  Each copy or derived work must preserve the copyright notice and this
 *  notice unmodified.
 *
 *  Resin Open Source is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Resin Open Source is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 *  of NON-INFRINGEMENT.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Resin Open Source; if not, write to the
 *
 *    Free Software Foundation, Inc.
 *    59 Temple Place, Suite 330
 *    Boston, MA 02111-1307  USA
 *
 *  @author Alex Rojkov
 */

package com.caucho.jsf.integration;


import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.enterprise.inject.spi.*;
import javax.servlet.ServletContext;

import com.sun.faces.spi.*;

import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

public class Mojarra12InjectionProvider
  extends DiscoverableInjectionProvider
{
  private static final L10N L = new L10N(InjectManager.class);
  private static final Logger log
    = Logger.getLogger(InjectManager.class.getName());

  protected ServletContext _context;
  protected BeanManager _manager;

  private Map<Class, AnnotatedType> _types
    = new HashMap<Class, AnnotatedType>();

  private Map<AnnotatedType, InjectionTarget> _targets
    = new HashMap<AnnotatedType, InjectionTarget>();

  public Mojarra12InjectionProvider(ServletContext context)
  {
    _context = context;
    _manager = InjectManager.getCurrent();

    if (log.isLoggable(Level.FINEST))
      log.finest(L.l(
        "Created Mojarra12InjectionProvider using InjectManager {0} for context {1}",
        _manager,
        _context));
  }

  public void inject(Object o)
    throws com.sun.faces.spi.InjectionProviderException
  {
    Class cl = o.getClass();

    InjectionTarget target = getInjectionTarget(cl);

    if (log.isLoggable(Level.FINEST))
      log.fine(L.l("{0} injecting bean '{1}'", this, o));

    target.inject(o, _manager.createCreationalContext(null));
  }

  public void invokePreDestroy(Object o)
    throws com.sun.faces.spi.InjectionProviderException
  {
    Class cl = o.getClass();

    InjectionTarget target = getInjectionTarget(cl);

    if (log.isLoggable(Level.FINEST))
      log.fine(L.l("{0} PreDestroy bean '{1}'", this, o));

    target.preDestroy(o);
  }

  public void invokePostConstruct(Object o)
    throws com.sun.faces.spi.InjectionProviderException
  {
    Class cl = o.getClass();

    InjectionTarget target = getInjectionTarget(cl);

    if (log.isLoggable(Level.FINEST))
      log.fine(L.l("{0} PreDestroy bean '{1}'", this, o));

    target.postConstruct(o);
  }

  private InjectionTarget getInjectionTarget(Class cl)
  {
    AnnotatedType type = _types.get(cl);

    if (type == null) {
      type = _manager.createAnnotatedType(cl);
      _types.put(cl, type);
    }

    InjectionTarget target = _targets.get(type);

    if (target == null) {
      target = _manager.createInjectionTarget(type);
      _targets.put(type, target);
    }

    return target;
  }
}