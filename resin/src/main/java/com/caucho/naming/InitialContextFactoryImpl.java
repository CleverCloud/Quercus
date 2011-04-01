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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.naming;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Returns the JNDI context for the current classloader.  Since each
 * thread will normally have its own current class loader, the
 * actual JNDI context is stored in the contextClassLoader.
 */
public class InitialContextFactoryImpl implements InitialContextFactory
{
  private static Logger log
    = Logger.getLogger(InitialContextFactoryImpl.class.getName());
  private static L10N L = new L10N(InitialContextFactoryImpl.class);

  private static EnvironmentLocal<AbstractModel> _rootModel
    = new EnvironmentLocal<AbstractModel>();

  /**
   * Constructor with an initial root.
   */
  public InitialContextFactoryImpl()
  {
  }

  /**
   * Sets the model for the current class loader.
   */
  public static AbstractModel getContextModel()
  {
    return _rootModel.get();
  }

  /**
   * Sets the model for the current class loader.
   */
  public static void setContextModel(AbstractModel model)
  {
    _rootModel.set(model);
  }

  public static AbstractModel createRoot()
  {
    synchronized (_rootModel) {
      AbstractModel model = _rootModel.getLevel();

      if (model == null) {
        EnvironmentModelRoot root = EnvironmentModelRoot.create();

        model = root.get("");

        _rootModel.set(model);

        try {
          AbstractModel javaComp = model.createSubcontext("java:comp");
          AbstractModel java = model.createSubcontext("java:");
          // env/0g8i
          java.bind("comp", javaComp);

          // #3486, support/1101
          AbstractModel env = javaComp.createSubcontext("env");
          javaComp.bind("env", env);
        } catch (NamingException e) {
          throw new RuntimeException(e);
        }
      }

      return model;
    }
  }

  /**
   * Returns the initial context for the current thread.
   */
  @Override
  public Context getInitialContext(Hashtable<?,?> env)
    throws NamingException
  {
    AbstractModel model = createRoot();

    return new ContextImpl(model, env);
  }
  
  public static Context createInitialContext()
  {
    return new ContextImpl(createRoot(), null);
  }
}
