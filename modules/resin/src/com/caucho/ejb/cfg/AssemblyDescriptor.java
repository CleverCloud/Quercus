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
 * @author Sam
 */

package com.caucho.ejb.cfg;

import com.caucho.config.gen.ApplicationExceptionConfig;
import com.caucho.j2ee.cfg.J2eeSecurityRole;

public class AssemblyDescriptor {
  private EjbConfig _config;

  AssemblyDescriptor(EjbConfig config)
  {
    _config = config;
  }

  public ContainerTransaction createContainerTransaction()
  {
    return new ContainerTransaction(_config);
  }

  public EjbJar.MethodPermission createMethodPermission()
  {
    return new EjbJar.MethodPermission(_config);
  }

  public void addSecurityRole(J2eeSecurityRole securityRole)
  {
  }

  public void addInterceptorBinding(InterceptorBinding interceptorBinding)
  {
    _config.addInterceptorBinding(interceptorBinding);
  }

  public void addMessageDestination(MessageDestination messageDestination)
  {
    _config.addMessageDestination(messageDestination);
  }

  public void addApplicationException(ApplicationExceptionConfig applicationException)
  {
    _config.addApplicationException(applicationException);
  }
}
