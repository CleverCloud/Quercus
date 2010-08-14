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

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.internal.ServerPlugin;

import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

@SuppressWarnings("restriction")
public class ResinGitPublisher extends ResinPublisher 
                               implements ResinPropertyIds 
{ 
  public static final String PUBLISHER_ID = 
    "org.eclipse.jst.server.generic.resin.resingitpublisher";
  
  private DeployClient _client = null;
  
  @Override
  public IStatus[] publish(IModuleArtifact[] resource, IProgressMonitor monitor)
  {
    if (getModule().length > 1)
      return null;
    
    // Have ant create the .war file
    IStatus[] result = super.publish(resource, monitor);
    
    // null means success... NOT Status.OK
    if (result != null)
      return result;
    
    ServerRuntime typeDef = getServerRuntime().getServerTypeDefinition();
    String host = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                 VIRTUAL_HOST);
    String user = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                 DEPLOY_USERNAME);
    
    String tag = 
      WebAppDeployClient.createTag("default", host, getModuleName());
    Path war = getWarPath();

    HashMap<String,String> attributes = new HashMap<String,String>();
    attributes.put(DeployClient.USER_ATTRIBUTE, user);
    attributes.put("user.name", 
      System.getProperties().getProperty("user.name"));
    attributes.put("client", "Eclipse Resin plugin");

    try {
      // XXX add support for message, version, and stage
      getDeployClient().deployJarContents(tag, war, attributes);
    }
    catch (IOException e) {
      IStatus s = new Status(IStatus.ERROR, 
                             CorePlugin.PLUGIN_ID, 
                             0, 
                             "Could not deploy war file to server", 
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
  
  private DeployClient getDeployClient()
  {
    if (_client == null) {
      ServerRuntime typeDef = getServerRuntime().getServerTypeDefinition();

      String user = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                   DEPLOY_USERNAME);
      String pass = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                   DEPLOY_PASSWORD);
      
      String server = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                     SERVER_ADDRESS);
      String portString = PublisherUtil.getPublisherData(typeDef, PUBLISHER_ID, 
                                                         HTTP_PORT);
 
      int port = Integer.valueOf(portString);

      _client = new DeployClient(server, port, user, pass);
    }

    return _client;
  }
  
  private Path getWarPath()
  {
    String serverId = getServer().getServer().getId();
    IPath projectWorkingDir = 
      ServerPlugin.getInstance().getTempDirectory(serverId);

    String moduleName = getModuleName();
    
    String prefix = 
      projectWorkingDir.append(moduleName).toPortableString();
          
    return Vfs.lookup(prefix + ".war");
  }
  
  private String getModuleName() 
  {
    IModule module = getModule()[0];
    String moduleName = module.getName();
    
    if ("jst.web".equals(module.getModuleType().getId())) {
      IWebModule webModule = 
        (IWebModule) module.loadAdapter(IWebModule.class, null);
      
      if (webModule != null) {
        String contextRoot = webModule.getURI(module);
        moduleName = contextRoot.substring(0, contextRoot.lastIndexOf('.'));
      }
    }
    
    return moduleName;
  }
}