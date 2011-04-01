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

package com.caucho.config.xml;

import java.lang.reflect.Constructor;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class XmlInjectionTarget<X> implements InjectionTarget<X>
{
  private InjectionTarget<X> _nextInjectionTarget;
  private Constructor<X> _ctor;
  private Arg<X> []_newProgram;
  private ConfigProgram []_injectProgram;

  public XmlInjectionTarget(ManagedBeanImpl<X> bean,
                            Constructor<X> ctor,
                            Arg<X> []newProgram,
                            ConfigProgram []injectProgram)
  {
    _nextInjectionTarget = bean.getInjectionTarget();
    _ctor = ctor;
    _newProgram = newProgram;

    _injectProgram = injectProgram;
  }

  @Override
  public X produce(CreationalContext<X> context)
  {
    if (_ctor == null)
      return (X) _nextInjectionTarget.produce(context);
    else {
      Object []args = new Object[_newProgram.length];

      for (int i = 0; i < args.length; i++) {
        args[i] = _newProgram[i].eval(context);
      }

      try {
        return (X) _ctor.newInstance(args);
      } catch (IllegalArgumentException e) {
        StringBuilder sb = new StringBuilder(_ctor.getName() + ": " + e);
        
        for (Object arg : args) {
          sb.append("\n  " + arg
                    + " [" + (arg != null ? arg.getClass().getName() : "null") + "]");
        }
        
        throw new IllegalArgumentException(sb.toString(), e);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        // XXX: clean up exception type
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void inject(X instance, CreationalContext<X> env)
  {
    _nextInjectionTarget.inject(instance, env);

    if (_injectProgram.length > 0) {
      for (ConfigProgram program : _injectProgram) {
        program.inject(instance, env);
      }
    }
  }

  @Override
  public void postConstruct(X instance)
  {
    _nextInjectionTarget.postConstruct(instance);
  }

  /**
   * Call destroy
   */
  @Override
  public void preDestroy(X instance)
  {
    _nextInjectionTarget.preDestroy(instance);
  }

  /**
   * Call destroy
   */
  @Override
  public void dispose(X instance)
  {
    _nextInjectionTarget.dispose(instance);
  }

  /**
   * Returns the injection points.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _nextInjectionTarget.getInjectionPoints();
  }
}
