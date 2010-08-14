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

package com.caucho.maven;

import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.admin.StatusQuery;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * The MavenDeploy
 */
abstract public class AbstractDeployMojo extends AbstractMojo
{
  private String _server;
  private int _port = -1;
  private String _user;
  private String _message;
  private String _password;

  private String _stage = "default";
  private String _virtualHost = "default";
  private String _contextRoot;
  private String _version;

  /**
   * Sets the ip address of the host that resin is running on
   * @parameter
   */
  public void setServer(String server)
  {
    _server = server;
  }

  public String getServer()
  {
    return _server;
  }

  /**
   * Sets the HTTP port that the resin instance is listening to
   * @parameter
   */
  public void setPort(int port)
  {
    _port = port;
  }

  public int getPort()
  {
    return _port;
  }

  /**
   * Sets the user name for the deployment service
   * @parameter
   */
  public void setUser(String user)
  {
    _user = user;
  }

  public String getUser()
  {
    return _user;
  }

  /**
   * Sets the password for the deployment service
   * @parameter
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the commit message for the deploy
   * @parameter
   */
  public void setCommitMessage(String message)
  {
    _message = message;
  }

  public String getCommitMessage()
  {
    return _message;
  }

  /**
   * Sets the stage in which to deploy the webapp (defaults to default)
   * @parameter
   */
  public void setStage(String stage)
  {
    _stage = stage;
  }

  public String getStage()
  {
    return _stage;
  }

  /**
   * Sets the virtual host to which to deploy the webapp (defaults to default)
   * @parameter
   */
  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  public String getVirtualHost()
  {
    return _virtualHost;
  }

  /**
   * Sets the context path of the webapp (defaults to /${finalName})
   * @parameter
   */
  public void setContextRoot(String contextRoot)
  {
    _contextRoot = contextRoot;
  }

  public String getContextRoot()
  {
    return _contextRoot;
  }

  /**
   * Sets the version for the deploy
   * @parameter
   */
  public void setVersion(String version)
  {
    _version = version;
  }

  public String getVersion()
  {
    return _version;
  }

  abstract protected String getMojoName();

  protected void validate()
    throws MojoExecutionException
  {
    if (_server == null)
      throw new MojoExecutionException("server is required by " + getMojoName());

    if (_port == -1)
      throw new MojoExecutionException("port is required by " + getMojoName());

    if (_user == null)
      throw new MojoExecutionException("user is required by " + getMojoName());
  }

  protected String buildWarTag()
  {
    return WebAppDeployClient.createTag(_stage, 
                                        _virtualHost, 
                                        _contextRoot);
  }

  protected String buildVersionedWarTag()
  {
    return WebAppDeployClient.createTag(_stage, 
                                        _virtualHost, 
                                        _contextRoot, 
                                        _version);
  }

  protected HashMap<String,String> getCommitAttributes()
  {
    HashMap<String,String> attributes = new HashMap<String,String>();

    attributes.put(DeployClient.USER_ATTRIBUTE, _user);
    attributes.put(DeployClient.MESSAGE_ATTRIBUTE, _message);
    attributes.put(DeployClient.VERSION_ATTRIBUTE, _version);

    attributes.put("user.name", 
                   System.getProperties().getProperty("user.name"));
    attributes.put("client", "maven (" + getClass().getSimpleName() + ")");

    return attributes;
  }

  /**
    * Set parameter values with system properties to support 
    * command line overrides.
   **/
  protected void processSystemProperties()
    throws MojoExecutionException
  {
    Properties properties = System.getProperties();
    String server = properties.getProperty("resin.server");
    String port = properties.getProperty("resin.port");

    String user = properties.getProperty("resin.user");
    String password = properties.getProperty("resin.password");
    String message = properties.getProperty("resin.commitMessage");

    String stage = properties.getProperty("resin.stage");
    String virtualHost = properties.getProperty("resin.virtualHost");
    String contextRoot = properties.getProperty("resin.contextRoot");
    String version = properties.getProperty("resin.version");

    if (server != null) 
      _server = server;
    
    if (port != null)
      _port = Integer.parseInt(port);

    if (user != null)
      _user = user;

    if (password != null)
      _password = password;

    if (message != null)
      _message = message;

    if (stage != null)
      _stage = stage;

    if (virtualHost != null)
      _virtualHost = virtualHost;

    if (contextRoot != null)
      _contextRoot = contextRoot;

    if (version != null)
      _version = version;
  }

  protected void printParameters()
  {
    Log log = getLog();
    
    log.debug(getMojoName() + " parameters");
    log.debug("  server = " + _server);
    log.debug("  port = " + _port);
    log.debug("  user = " + _user);
    log.debug("  password = " + _password);
    log.debug("  commitMessage = " + _message);
    log.debug("  stage = " + _stage);
    log.debug("  virtualHost = " + _virtualHost);
    log.debug("  contextRoot = " + _contextRoot);
    log.debug("  version = " + _version);
  }

  protected abstract void doTask(WebAppDeployClient client)
    throws MojoExecutionException;

  /**
   * Executes the task
   */

  public void execute() 
    throws MojoExecutionException
  {
    processSystemProperties();

    if (getLog().isDebugEnabled())
      printParameters();

    validate();

    doTask(new WebAppDeployClient(_server, _port, _user, _password));
  }
}
