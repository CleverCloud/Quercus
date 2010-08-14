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
 * @goal delete-tag
 */
public class MavenDeleteTag extends AbstractDeployMojo
{
  private String _tag;

  public void setTag(String tag)
  {
    _tag = tag;
  }

  protected String getMojoName()
  {
    return "resin-delete-tag";
  }

  @Override
  protected void processSystemProperties()
    throws MojoExecutionException
  {
    super.processSystemProperties();

    Properties properties = System.getProperties();

    String tag = properties.getProperty("resin.tag");

    if (tag != null)
      _tag = tag;
  }

  @Override
  protected void printParameters()
  {
    super.printParameters();
    
    Log log = getLog();

    log.debug("  tag = " + _tag);
  }

  @Override
  protected void validate()
    throws MojoExecutionException 
  {
    super.validate();

    if (_tag == null && getContextRoot() == null)
      throw new  MojoExecutionException("tag or contextRoot is required by " + getMojoName());
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

    if (tag == null)
      tag = buildVersionedWarTag();

    log.info("Deleting tag " + tag);

    if (! client.removeTag(tag, getCommitAttributes()))
      log.warn("Failed to delete tag " + tag);
  }
}
