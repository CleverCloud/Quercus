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
 * The Maven Upload War Mojo
 * @goal upload-war 
 */
public class MavenUploadWar extends AbstractDeployMojo
{
  private String _warFile;
  private String _archive;
  private boolean _writeHead = true;

  /**
   * Sets the path of the WAR file (defaults to target/${finalName}.war)
   * @parameter
   */
  public void setWarFile(String warFile)
    throws MojoExecutionException
  {
    if (! warFile.endsWith(".war"))
      throw new MojoExecutionException("war-file must have .war extension");

    _warFile = warFile;

    if (getContextRoot() == null) {
      int lastSlash = _warFile.lastIndexOf("/");

      if (lastSlash < 0)
        lastSlash = 0;

      setContextRoot(_warFile.substring(lastSlash, 
                                        _warFile.length() - ".war".length()));
    }
  }

  /**
   * Sets whether to add an archive tag for this war.  Can be "true",
   * "false", or an explicit archive tag.  If set to "true", a default
   * archive tag will be constructed for the war.
   * @parameter
   */
  public void setArchive(String tag)
  {
    _archive = tag;
  }

  /**
   * Sets whether to set the head tag for this war if a version is 
   * specified.
   * @parameter
   */
  public void setWriteHead(boolean writeHead)
  {
    _writeHead = writeHead;
  }

  protected String getMojoName()
  {
    return "resin-upload-war";
  }

  @Override
  protected void processSystemProperties()
    throws MojoExecutionException
  {
    super.processSystemProperties();

    Properties properties = System.getProperties();
    String archive = properties.getProperty("resin.archive");
    String writeHead = properties.getProperty("resin.writeHead");
    String warFile = properties.getProperty("resin.warFile");

    if (archive != null)
      _archive = archive;

    if (writeHead != null) {
      if ("true".equalsIgnoreCase(writeHead))
        _writeHead = true;
      else if ("false".equalsIgnoreCase(writeHead))
        _writeHead = false;
      else
        throw new MojoExecutionException("resin.writeHead must be a either 'true' or 'false'");
    }

    if (warFile != null)
      setWarFile(warFile);
  }

  @Override
  protected void printParameters()
  {
    super.printParameters();
    
    Log log = getLog();

    log.debug("  warFile = " + _warFile);
    log.debug("  archive = " + _archive);
    log.debug("  writeHead = " + _writeHead);
  }

  @Override
  protected void validate()
    throws MojoExecutionException
  {
    super.validate();

    if (_warFile == null)
      throw new MojoExecutionException("war-file is required by " + getMojoName());
  }

  /**
   * Executes the maven resin:run task
   */
  @Override
  protected void doTask(WebAppDeployClient client) 
    throws MojoExecutionException
  {
    Log log = getLog();

    try {
      // upload
      com.caucho.vfs.Path path = Vfs.lookup(_warFile);

      String archiveTag = _archive;

      if ("true".equals(archiveTag)) {
        archiveTag = client.createArchiveTag(getVirtualHost(), 
                                             getContextRoot(), 
                                             getVersion());
      }
      else if ("false".equals(archiveTag)) {
        archiveTag = null;
      }

      String tag = buildVersionedWarTag();

      HashMap<String,String> attributes = getCommitAttributes();

      client.deployJarContents(tag, path, attributes);

      log.info("Deployed " + path + " to tag " + tag);

      if (archiveTag != null) {
        client.copyTag(archiveTag, tag, attributes);

        log.info("Created archive tag " + archiveTag);
      }

      if (getVersion() != null && _writeHead) {
        String headTag = buildWarTag();

        client.copyTag(headTag, tag, attributes);

        log.info("Wrote head version tag " + headTag);
      }
    }
    catch (IOException e) {
      throw new MojoExecutionException("Resin upload war failed", e);
    }
  }
}
