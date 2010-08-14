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

package com.caucho.resin.eclipse;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jst.server.generic.core.internal.GenericServerBehaviour;

@SuppressWarnings("restriction")
public class ResinServerBehaviour
  extends GenericServerBehaviour
  implements ResinPropertyIds
{
  @Override
  public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy config,
                                        IProgressMonitor monitor)
    throws CoreException
  {
    super.setupLaunchConfiguration(config, monitor);
  
    String vmArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
                                        (String) null);


    String resinHome = getServerDefinition().getResolver().resolveProperties("${resin.home}");

    if (isWin()) {
      if (is64())
        vmArgs += " -Djava.library.path=" + resinHome + "/win64";
      else
        vmArgs += " -Djava.library.path=" + resinHome + "/win32";
    }
    else {
      if (is64())
        vmArgs += " -Djava.library.path=" + resinHome + "/libexec64";
      else
        vmArgs += " -Djava.library.path=" + resinHome + "/libexec32";

    }

    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}
	
	private boolean is64()
  {
    return "64".equals(System.getProperty("sun.arch.data.model"));
  }
  
  private boolean isWin()
  {
    return System.getProperty("os.name").startsWith("Windows");
  }

  @Override
  public void stop(boolean force)
  {
    // change the default behaviour and always force the stop,
    // which causes eclipse to just terminate the process and
    // not run the <stop> defined in the serverdef file
    super.stop(true);
  }
}
