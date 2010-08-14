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

import com.caucho.jcr.base.BaseNode;
import com.caucho.jcr.base.BaseSession;
import com.caucho.jcr.base.BaseWorkspace;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

/**
 * Represents a open session for a file repository.
 */
public class FileSession extends BaseSession {
  private static final L10N L = new L10N(FileSession.class);
  
  private FileRepository _repository;
  private BaseWorkspace _workspace;
  private BaseNode _rootNode;

  /**
   * Creates the new session.
   */
  FileSession(FileRepository repository)
  {
    _repository = repository;

    Path root = repository.getRoot();

    if (root.isDirectory())
      _rootNode = new DirectoryNode(this, root);
    else
      _rootNode = new FileNode(this, root);

    _workspace = new BaseWorkspace("default", this);
  }
  
  /**
   * Returns the owning repository.
   */
  public Repository getRepository()
  {
    return _repository;
  }
  
  /**
   * Returns the workspace.
   */
  public Workspace getWorkspace()
  {
    return _workspace;
  }

  /**
   * Returns the session's root node.
   */
  public Node getRootNode()
    throws RepositoryException
  {
    return _rootNode;
  }
}
