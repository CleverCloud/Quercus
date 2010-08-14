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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.servertype.definition.Property;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

@SuppressWarnings("restriction")
public class ResinServer extends GenericServer
                         implements ResinPropertyIds
{
  static final String RESIN_CONF_TYPE = "resin.conf.type";
  static final String RESIN_CONF_BUNDLE = "resin.conf.bundle";
  static final String RESIN_CONF_RESIN_HOME = "resin.conf.resin.home";
  static final String RESIN_CONF_USER = "resin.conf.user";
  static final String RESIN_CONF_COPY = "resin.conf.copy";
  
  static final String RESIN_CONF_USER_LOCATION 
    = "resin.conf.user.location";
  static final String RESIN_CONF_BUNDLE_LOCATION 
    = "resin.conf.bundle.location";

  @Override
  public void saveConfiguration(IProgressMonitor monitor) 
    throws CoreException
  {
    super.saveConfiguration(monitor);
    
    Map instanceProperties = getServerInstanceProperties(); 
    String resinConfProjectLocation = 
      (String) instanceProperties.get(CONFIG_FILE_NAME);
    
    if (resinConfProjectLocation != null)
      return;

    IServer server = getServer();
    IRuntime runtime = server.getRuntime();
    GenericServerRuntime genericRuntime = 
      (GenericServerRuntime) runtime.loadAdapter(GenericServerRuntime.class, 
                                                 null);
    ServerRuntime typeDef = genericRuntime.getServerTypeDefinition();

    String confType = (String) instanceProperties.get(RESIN_CONF_TYPE);
    boolean copyConfig = 
      "true".equals(instanceProperties.get(RESIN_CONF_COPY));

    File configFile = null;
    IFile configIFile = null;
    IFolder configFolder = server.getServerConfiguration();
    
    if (RESIN_CONF_BUNDLE.equals(confType)) {
      copyConfig = true;
      
      String filename = 
        getPropertyDefault(typeDef, RESIN_CONF_BUNDLE_LOCATION);
      configFile = PublisherUtil.locateBundleFile(typeDef, filename);
      configIFile = configFolder.getFile(configFile.getName());
      
      copyFileToWorkspace(configFile, configIFile, monitor);
    }
    else if (RESIN_CONF_RESIN_HOME.equals(confType)) {
      String resinHome = 
        (String) instanceProperties.get(ResinPropertyIds.RESIN_HOME);
      IPath resinConfPath = new Path(resinHome).append("conf");
      
      IPath resinConfFilePath = resinConfPath.append("resin.xml");
      configFile = resinConfFilePath.toFile();
      
      if (! configFile.exists()) {
        resinConfFilePath = resinConfPath.append("resin.conf");
        configFile = resinConfFilePath.toFile();
        
        if (! configFile.exists())
          PublisherUtil.throwCoreException("Cannot find Resin configuration in Resin home directory");
      }
    
      configIFile = configFolder.getFile(configFile.getName());
      
      if (copyConfig)
        copyFileToWorkspace(configFile, configIFile, monitor);
      else
        configIFile.createLink(resinConfFilePath, IResource.NONE, monitor);
    }
    else if (RESIN_CONF_USER.equals(confType)) {
      String userConf = 
        (String) instanceProperties.get(RESIN_CONF_USER_LOCATION);
      IPath userConfPath = new Path(userConf);
      configFile = userConfPath.toFile(); 

      configIFile = configFolder.getFile(configFile.getName());
      
      if (copyConfig)
        copyFileToWorkspace(configFile, configIFile, monitor);
      else
        configIFile.createLink(userConfPath, IResource.NONE, monitor);
    }
    else {
      PublisherUtil.throwCoreException("Internal configuration error");
    }

    File appDefaultFile = 
      new Path(configFile.getParentFile().toString())
          .append("app-default.xml").toFile();
    IFile appDefaultIFile = configFolder.getFile("app-default.xml");
    
    if (appDefaultFile.exists()) {
      if (copyConfig)
        copyFileToWorkspace(appDefaultFile, appDefaultIFile, monitor);
      else
        appDefaultIFile.createLink(new Path(appDefaultFile.toString()),
                                   IResource.NONE, monitor);
    }
    
    instanceProperties.put(ResinPropertyIds.CONFIG_FILE_NAME,
                           configIFile.getLocation().toOSString());
    VariableUtil.setVariable(ResinPropertyIds.CONFIG_FILE_NAME,
                             configIFile.getLocation().toOSString());
  }
  
  private void copyFileToWorkspace(File source, IFile destination, 
                                   IProgressMonitor monitor)
    throws CoreException
  {
    FileInputStream fileContents = null;
    
    try {
      fileContents = new FileInputStream(source);
      destination.create(fileContents, true, monitor);
    }
    catch (IOException e) {
      PublisherUtil.throwCoreException("error copying file: " + source, e);
    }
    finally {
      try {
        if (fileContents != null)
          fileContents.close();
      }
      catch (IOException e) {
        PublisherUtil.throwCoreException("error closing file: " + source, e);
      }
    }
  }
  
  /**
   * This is a hack to let us store internal data in the serverdef file 
   * that's not exposed to the user.
   * @param runtime The server definition, created from the serverdef file
   * @param key     The internal data key to fetch
   * @return        The default value of the internal property
   */
  private String getPropertyDefault(ServerRuntime runtime, String key)
  {
    List properties = runtime.getProperty();
    Iterator iterator = properties.iterator();
    
    while (iterator.hasNext()) {
      Property property = (Property) iterator.next();
      
      if (key.equals(property.getId()))
        return property.getDefault();
    }
    
    return null;
  }
  
  String getPropertyDefault(String key)
  {
    IRuntime runtime = getServer().getRuntime();
    GenericServerRuntime genericRuntime = 
      (GenericServerRuntime) runtime.loadAdapter(GenericServerRuntime.class, 
                                                 null);
    ServerRuntime typeDef = genericRuntime.getServerTypeDefinition();
    
    return getPropertyDefault(typeDef, key);
  }
  
  GenericServerRuntime getRuntimeDelegate()
  {
    IRuntime runtime = getServer().getRuntime();
    return (GenericServerRuntime) runtime.loadAdapter(GenericServerRuntime.class,
                                                      null);
  } 
}
