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

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to copy a tag in the repository.  A tag can be copied by
 * specifying the source tag explicitly using the "sourceTag" attribute
 * or using the "sourceStage", "sourceType", "sourceVirtualHost", 
 * "sourceContextRoot", and "sourceVersion" attributes.  The target
 * tag can be specified explicitly using the "tag" attribute or by using
 * the "stage", "type", "virtualHost", "contextRoot", and "version" 
 * attributes.
 */
public class ResinCopyTag extends ResinDeployClientTask {
  private String _tag;
  private String _sourceTag;

  private String _sourceStage = "default";
  private String _sourceVersion;
  private String _sourceVirtualHost = "default";
  private String _sourceContextRoot = null;

  /**
   * For ant.
   **/
  public ResinCopyTag()
  {
  }

  public void setSourceStage(String stage)
  {
    _sourceStage = stage;
  }

  public void setTag(String tag)
  {
    _tag = tag;
  }

  public void setSourceTag(String tag)
  {
    _sourceTag = tag;
  }

  public void setSourceContextRoot(String contextRoot)
  {
    _sourceContextRoot = contextRoot;
  }

  public void setSourceVersion(String version)
  {
    _sourceVersion = version;
  }

  public void setSourceVirtualHost(String virtualHost)
  {
    _sourceVirtualHost = virtualHost;
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_tag == null && getContextRoot() == null)
      throw new BuildException("tag or contextRoot is required by " 
                               + getTaskName());

    if (_sourceTag == null && _sourceContextRoot == null)
      throw new BuildException("sourceTag or sourceContextRoot is required by " 
                               + getTaskName());
  }

  @Override
  protected void doTask(WebAppDeployClient client)
    throws BuildException
  {
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

    log("Copying " + sourceTag + " to " + tag);

    boolean result = client.copyTag(tag, sourceTag, getCommitAttributes());

    if (! result)
      log("Failed to copy " + sourceTag + " to " + tag);
  }
}
