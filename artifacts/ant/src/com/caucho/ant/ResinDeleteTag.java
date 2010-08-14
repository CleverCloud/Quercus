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
import com.caucho.server.admin.TagResult;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to delete a tag in the repository.  The tag may be specified
 * explicitly by the "tag" attribute or constructed using the stage,
 * type, host, context root, and version attributes.
 */
public class ResinDeleteTag extends ResinDeployClientTask {
  private String _tag;

  /**
   * For ant.
   **/
  public ResinDeleteTag()
  {
  }

  public void setTag(String tag)
  {
    _tag = tag;
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_tag == null && getContextRoot() == null)
      throw new BuildException("tag or contextRoot is required by " + 
                               getTaskName());
  }

  @Override
  protected void doTask(WebAppDeployClient client)
    throws BuildException
  {
    String tag = _tag;

    if (tag == null)
      tag = buildVersionedWarTag();

    if (client.removeTag(tag, getCommitAttributes()))
      log("Deleted tag " + tag);
    else
      log("Failed to delete tag " + tag);
  }
}
