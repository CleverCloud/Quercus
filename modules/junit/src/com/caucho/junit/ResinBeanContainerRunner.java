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

package com.caucho.junit;

import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

import com.caucho.resin.*;
import com.caucho.config.inject.*;
import com.caucho.vfs.*;

/**
 * ResinJUnit runner runs a JUnit within the context of Resin.
 */
public class ResinBeanContainerRunner extends BlockJUnit4ClassRunner {
  private Class<?> _testClass;

  private ResinBeanContainer _beanContainer;
  private ResinBeanConfiguration _beanConfiguration;

  public ResinBeanContainerRunner(Class<?> testClass)
    throws Throwable
  {
    super(testClass);

    _testClass = testClass;

    _beanConfiguration = testClass.getAnnotation(ResinBeanConfiguration.class);
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier)
  {
    ResinBeanContainer beanContainer = getResinContext();
    BeanContainerRequest request = beanContainer.beginRequest();

    try {
      super.runChild(method, notifier);
    } finally {
      request.close();
    }
  }

  @Override
  protected Object createTest()
    throws Exception
  {
    InjectManager manager = getResinContext().getInstance(InjectManager.class);

    return manager.createTransientObject(_testClass);
  }

  protected ResinBeanContainer getResinContext()
  {
    if (_beanContainer == null) {
      _beanContainer = new ResinBeanContainer();

      String userName = System.getProperty("user.name");
      String workDir = "file:/tmp/" + userName;

      _beanContainer.setWorkDirectory(workDir);

      if (_beanConfiguration != null) {
        for (String module : _beanConfiguration.modules()) {
          _beanContainer.addModule(module);
        }

        for (String conf : _beanConfiguration.beansXml()) {
          _beanContainer.addBeansXml(conf);
        }
      }

      _beanContainer.start();
    }

    return _beanContainer;
  }
}
