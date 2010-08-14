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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to query tags of Resin applications deployed via the 
 * ResinDeployWar task to production.
 */
public class ResinQueryTags extends ResinDeployClientTask {
  private String _pattern;
  private boolean _printValues = false;

  /**
   * For ant.
   **/
  public ResinQueryTags()
  {
  }

  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPrintValues(boolean printValues)
  {
    _printValues = printValues;
  }

  public boolean getPrintValues()
  {
    return _printValues;
  }
  
  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_pattern == null 
        && getStage() == null
        && getVirtualHost() == null
        && getContextRoot() == null
        && getVersion() == null)
      throw new BuildException("At least one of pattern, stage, virtualHost, contextRoot, or version is required by " + getTaskName());
  }

  @Override
  protected void doTask(WebAppDeployClient client)
    throws BuildException
  {
    String pattern = _pattern;

    if (pattern == null) {
      if (getContextRoot() == null)
        setContextRoot(".*");

      pattern = buildVersionedWarTag();
    }

    log("Query pattern = '" + pattern + "'", Project.MSG_DEBUG);

    TagResult []tags = client.queryTags(pattern);

    for (TagResult tag : tags) {
      if (_printValues) 
        log(tag.getTag() + " -> " + tag.getRoot());
      else
        log(tag.getTag());
    }
  }
}
