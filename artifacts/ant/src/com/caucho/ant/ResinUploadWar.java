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

package com.caucho.ant;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.admin.TagResult;
import com.caucho.util.QDate;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to deploy war files to resin
 */
public class ResinUploadWar extends ResinDeployClientTask {
  private String _warFile;
  private String _archive;
  private boolean _writeHead = true;

  /**
   * For ant.
   **/
  public ResinUploadWar()
  {
  }

  public void setWarFile(String warFile)
    throws BuildException
  {
    if (! warFile.endsWith(".war"))
      throw new BuildException("war-file must have .war extension");

    _warFile = warFile;

    if (getContextRoot() == null) {
      int lastSlash = _warFile.lastIndexOf("/");

      if (lastSlash < 0)
        lastSlash = 0;

      setContextRoot(_warFile.substring(lastSlash, 
                                        _warFile.length() - ".war".length()));
    }
  }

  public void setArchive(String tag)
  {
    _archive = tag;
  }

  public void setWriteHead(boolean writeHead)
  {
    _writeHead = writeHead;
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_warFile == null)
      throw new BuildException("war-file is required by " + getTaskName());
  }

  @Override
  protected void doTask(WebAppDeployClient client)
    throws BuildException
  {
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

      log("Deployed " + path + " to tag " + tag);

      if (archiveTag != null) {
        client.copyTag(archiveTag, tag, attributes);

        log("Created archive tag " + archiveTag);
      }

      if (getVersion() != null && _writeHead) {
        String headTag = buildWarTag();

        client.copyTag(headTag, tag, attributes);

        log("Wrote head version tag " + headTag);
      }
    }
    catch (IOException e) {
      throw new BuildException(e);
    }
  }
}
