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
 * @author Emil Ong
 */

package com.caucho.resin.eclipse;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericPublisher;
import org.eclipse.wst.server.core.IModuleArtifact;

/**
 * This "publisher" doesn't actually publish anything, but instead makes sure
 * that the correct resin.xml file is available to the resin instance (it may 
 * need to be pulled out of a bundle .jar file into a temp dir) and initializes
 * the variables that point the resin to the workspace. 
 * 
 * @author Emil Ong
 *
 */
@SuppressWarnings("restriction")
public class ResinInPlacePublisher extends GenericPublisher
                                   implements ResinPropertyIds
{
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resininplacepublisher";

  @Override
  public IStatus[] publish(IModuleArtifact[] resource, 
                           IProgressMonitor monitor)
  {
    if (getModule().length > 1)
      return null;

    IWebModule webModule = 
      (IWebModule) getModule()[0].loadAdapter(IWebModule.class, null);
    
    IContainer[] folders = webModule.getResourceFolders();

    if (folders.length != 1) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 0, 
                             "Cannot find web content folder",
                             null);
      CorePlugin.getDefault().getLog().log(s);
      return new IStatus[] { s };
    }
    
    String webContentFolder = folders[0].getLocation().toString();
    String webappId = getModule()[0].getName();
    
    try {
      if (monitor.isCanceled())
        return null;

      ResinServer resinServer = (ResinServer) getServer();
      Map properties = resinServer.getServerInstanceProperties();
      String configLocation = (String) properties.get(CONFIG_FILE_NAME);
      
      VariableUtil.setVariable(CONFIG_FILE_NAME, configLocation);
      VariableUtil.setVariable("webapp.dir", webContentFolder);
      VariableUtil.setVariable("webapp.id", webappId);
    } 
    catch (CoreException e) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 0, 
                             "In place Resin publish failed",
                             e);
      CorePlugin.getDefault().getLog().log(s);
      return new IStatus[] { s };
    }
    
    return null;
  }

  @Override
  public IStatus[] unpublish(IProgressMonitor monitor)
  {
    return null;
  }
}
