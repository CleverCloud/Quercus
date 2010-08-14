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
 * @goal copy-tag
 */
public class MavenCopyTag extends AbstractDeployMojo
{
  private String _tag;
  private String _sourceTag;

  private String _sourceStage = "default";
  private String _sourceVersion;
  private String _sourceVirtualHost = "default";
  private String _sourceContextRoot = null;

  /**
   * The stage of the source tag to copy 
   * @parameter
   */
  public void setSourceStage(String stage)
  {
    _sourceStage = stage;
  }

  /**
   * The explicit target tag of the copy
   * @parameter
   */
  public void setTag(String tag)
  {
    _tag = tag;
  }

  /**
   * The explicit source tag to copy 
   * @parameter
   */
  public void setSourceTag(String tag)
  {
    _sourceTag = tag;
  }

  /**
   * The context root of the source tag to copy 
   * @parameter
   */
  public void setSourceContextRoot(String contextRoot)
  {
    _sourceContextRoot = contextRoot;
  }

  /**
   * The version of the source tag to copy 
   * @parameter
   */
  public void setSourceVersion(String version)
  {
    _sourceVersion = version;
  }

  /**
   * The virtual host of the source tag to copy 
   * @parameter
   */
  public void setSourceVirtualHost(String virtualHost)
  {
    _sourceVirtualHost = virtualHost;
  }

  protected String getMojoName()
  {
    return "resin-copy-tag";
  }

  @Override
  protected void processSystemProperties()
    throws MojoExecutionException
  {
    super.processSystemProperties();

    Properties properties = System.getProperties();

    String sourceStage = 
      properties.getProperty("resin.sourceStage");
    String sourceVirtualHost = 
      properties.getProperty("resin.sourceVirtualHost");
    String sourceContextRoot = 
      properties.getProperty("resin.sourceContextRoot");
    String sourceVersion = 
      properties.getProperty("resin.sourceVersion");

    String tag = properties.getProperty("resin.tag");
    String sourceTag = properties.getProperty("resin.sourceTag");

    if (sourceStage != null)
      _sourceStage = sourceStage;

    if (sourceVirtualHost != null)
      _sourceVirtualHost = sourceVirtualHost;

    if (sourceContextRoot != null)
      _sourceContextRoot = sourceContextRoot;

    if (sourceVersion != null)
      _sourceVersion = sourceVersion;

    if (tag != null)
      _tag = tag;

    if (sourceTag != null) 
      _sourceTag = sourceTag;
  }

  @Override
  protected void printParameters()
  {
    super.printParameters();
    
    Log log = getLog();

    log.debug("  sourceStage = " + _sourceStage);
    log.debug("  sourceVirtualHost = " + _sourceVirtualHost);
    log.debug("  sourceContextRoot = " + _sourceContextRoot);
    log.debug("  sourceVersion = " + _sourceVersion);
    log.debug("  tag = " + _tag);
    log.debug("  sourceTag = " + _sourceTag);
  }

  @Override
  protected void validate()
    throws MojoExecutionException 
  {
    super.validate();

    if (_tag == null && getContextRoot() == null)
      throw new MojoExecutionException("tag or contextRoot is required by " + getMojoName());

    if (_sourceTag == null && _sourceContextRoot == null)
      throw new MojoExecutionException("sourceTag or sourceContextRoot is required by " + getMojoName());
  }

  /**
   * Executes the maven resin:run task
   */
  @Override
  protected void doTask(WebAppDeployClient client) 
    throws MojoExecutionException
  {
    Log log = getLog();

    String tag = _tag;
    String sourceTag = _sourceTag;

    if (tag == null)
      tag = buildVersionedWarTag();

    if (sourceTag == null) {
      sourceTag = WebAppDeployClient.createTag(_sourceStage, 
                                               _sourceVirtualHost,
                                               _sourceContextRoot,
                                               _sourceVersion);
    }

    log.info("Copying " + sourceTag + " to " + tag);

    boolean result = client.copyTag(tag, sourceTag, getCommitAttributes());

    if (! result)
      log.warn("Failed to copy " + sourceTag + " to " + tag);
  }
}
