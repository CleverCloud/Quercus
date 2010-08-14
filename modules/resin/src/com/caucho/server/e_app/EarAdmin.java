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


package com.caucho.server.e_app;

import com.caucho.management.server.EAppMXBean;
import com.caucho.server.deploy.DeployControllerAdmin;

public class EarAdmin
  extends DeployControllerAdmin<EarDeployController>
  implements EAppMXBean
{
  private EarDeployController _eAppController;
  
  public EarAdmin(EarDeployController earDeployController)
  {
    super(earDeployController);

    _eAppController = earDeployController;
  }

  public String getClientRefs()
  {
    EnterpriseApplication eApp = _eAppController.getDeployInstance();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(eApp.getClassLoader());

      return eApp.getClientRefs();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

}
