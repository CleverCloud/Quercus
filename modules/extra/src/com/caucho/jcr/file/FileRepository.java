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
 * @author Scott Ferguson
 */

package com.caucho.jcr.file;

import com.caucho.config.ConfigException;
import com.caucho.jcr.base.BaseRepository;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Represents a repository based on a filesystem.
 *
 * Configuration looks like:
 *
 * <pre>
 * &lt;resource jndi-name="crm/wiki"
 *           type="com.caucho.cms.file.FileRepository">
 *     &lt;init>
 *         &lt;root>wiki-data&lt;/root>
 *     &lt;/init>
 * &lt;/resource>
 * </pre>
 */
public class FileRepository extends BaseRepository {
  private static final L10N L = new L10N(FileRepository.class);
  
  private Path _root;

  /**
   * Sets the file root.
   */
  public void setRoot(Path root)
  {
    _root = root.createRoot();
  }

  /**
   * Returns the file root.
   */
  public Path getRoot()
  {
    return _root;
  }

  /**
   * Initialize the FileRespository.
   */
  public void init()
    throws ConfigException
  {
    if (_root == null)
      throw new ConfigException(L.l("'root' property is required for FileRepository"));
  }

  /**
   * Returns the value for Repository feature descriptor keys.
   */
  public String getDescriptor(String key)
  {
    if (REP_NAME_DESC.equals(key))
      return "Resin File Repository";
    else
      return super.getDescriptor(key);
  }

  /**
   * Opens a new session, specifying the security credentials
   * and a workspace.
   *
   * @param credentials the security credentials
   * @param workspaceName select an optional workspace
   */
  public Session login(Credentials credentials, String workspaceName)
    throws LoginException,
           NoSuchWorkspaceException,
           RepositoryException
  {
    return new FileSession(this);
  }
}
